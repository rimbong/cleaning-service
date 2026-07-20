package com.boot.cleanhub.biz.supply.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.util.CellReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boot.cleanhub.biz.supply.domain.HazardousMix;
import com.boot.cleanhub.biz.supply.domain.PhType;
import com.boot.cleanhub.biz.supply.domain.SupplyItem;
import com.boot.cleanhub.biz.supply.domain.SupplyTransaction;
import com.boot.cleanhub.biz.supply.domain.SupplyTxType;
import com.boot.cleanhub.biz.supply.dto.SupplyHazardResponse;
import com.boot.cleanhub.biz.supply.dto.SupplyItemRequest;
import com.boot.cleanhub.biz.supply.dto.SupplyItemResponse;
import com.boot.cleanhub.biz.supply.dto.SupplyTransactionRequest;
import com.boot.cleanhub.biz.supply.dto.SupplyTransactionResponse;
import com.boot.cleanhub.biz.supply.repository.SupplyItemRepository;
import com.boot.cleanhub.biz.supply.repository.SupplyStockView;
import com.boot.cleanhub.biz.supply.repository.SupplyTransactionRepository;
import com.boot.cleanhub.common.dto.PageResponse;
import com.boot.cleanhub.error.BizException;
import com.boot.cleanhub.error.ErrorCode;
import com.boot.cleanhub.util.excel.PoiMo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <pre>
 *   약품/소모품 재고 서비스 — 품목 관리 + 입출고 등록 + 현재 재고 계산.
 *
 *   현재 재고는 어디에도 저장하지 않는다. 입출고 이력(quantity, 부호 있는 증감)의 합계다.
 *   따라서 재고를 바꾸는 유일한 방법은 이력 한 줄을 남기는 것이고, 숫자가 이상하면
 *   그 품목의 이력만 보면 원인이 드러난다.
 *
 *   재고가 음수가 되는 것은 막지 않는다. 현장에서 사용을 먼저 찍고 입고를 나중에 올리는 일이
 *   흔한데, 그때마다 저장을 거부하면 기록 자체를 포기하게 된다. 대신 음수/안전재고 미만은
 *   목록에서 눈에 띄게 표시해 "입고를 안 찍었다"는 신호로 쓴다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplyService {

    /** 신규 등록 시 중복 검사에서 제외할 id 가 없음을 나타내는 값(존재할 수 없는 id) */
    private static final long NO_EXCLUDE_ID = -1L;

    private final SupplyItemRepository supplyItemRepository;
    private final SupplyTransactionRepository supplyTransactionRepository;

    // ===================== 품목 =====================

    public PageResponse<SupplyItemResponse> list(String keyword, Pageable pageable) {
        Page<SupplyItem> items;
        if (StringUtils.hasText(keyword)) {
            items = supplyItemRepository.findByNameContainingIgnoreCaseOrderByNameAscIdAsc(keyword.trim(), pageable);
        } else {
            items = supplyItemRepository.findAllByOrderByNameAscIdAsc(pageable);
        }
        Map<Long, Integer> stocks = stockMap(items.getContent());
        return PageResponse.from(items.map(item -> SupplyItemResponse.from(item, stockOf(stocks, item))));
    }

    public SupplyItemResponse get(Long id) {
        SupplyItem item = findItemOrThrow(id);
        return SupplyItemResponse.from(item, currentStock(id));
    }

    @Transactional
    public SupplyItemResponse create(SupplyItemRequest request) {
        checkDuplicate(request, NO_EXCLUDE_ID);
        SupplyItem item = new SupplyItem();
        apply(item, request);
        return SupplyItemResponse.from(supplyItemRepository.save(item), 0);
    }

    @Transactional
    public SupplyItemResponse update(Long id, SupplyItemRequest request) {
        SupplyItem item = findItemOrThrow(id);
        // 검사를 apply 보다 먼저 한다 — 이유는 checkDuplicate 주석 참고.
        checkDuplicate(request, id);
        apply(item, request);
        supplyItemRepository.saveAndFlush(item);
        return SupplyItemResponse.from(item, currentStock(id));
    }

    /**
     * 품목 삭제. 입출고 이력이 있으면 거부한다 —
     * 이력을 같이 지우면 과거 재고 흐름이 통째로 사라지기 때문이다.
     *
     * @param id 품목 id
     */
    @Transactional
    public void delete(Long id) {
        SupplyItem item = findItemOrThrow(id);
        if (supplyTransactionRepository.existsByItemId(id)) {
            throw new BizException(ErrorCode.SUPPLY_ITEM_HAS_HISTORY);
        }
        supplyItemRepository.delete(item);
    }

    /**
     * 보유 약품에서 성립하는 위험 조합 경고.
     *
     * 창고 전체를 기준으로 판정한다. 화면에 보이는 목록(한 페이지)으로 하면
     * 락스와 산성 세제가 다른 페이지에 있을 때 어느 페이지에서도 경고가 뜨지 않는다.
     * 안전 정보라 "경고 없음"이 "안전"으로 읽히므로 범위를 좁히면 안 된다.
     *
     * @return 성립하는 조합 목록(없으면 빈 목록)
     */
    public List<SupplyHazardResponse> hazards() {
        Set<PhType> inStock = new HashSet<>(supplyItemRepository.findPhTypesInStock());
        List<SupplyHazardResponse> warnings = new ArrayList<>();
        for (HazardousMix mix : HazardousMix.values()) {
            if (mix.matches(inStock)) {
                warnings.add(SupplyHazardResponse.from(mix));
            }
        }
        return warnings;
    }

    // ===================== 입출고 =====================

    public PageResponse<SupplyTransactionResponse> history(Long itemId, Pageable pageable) {
        findItemOrThrow(itemId);
        Page<SupplyTransaction> page = supplyTransactionRepository
                .findByItemIdOrderByTxDateDescIdDesc(itemId, pageable);
        return PageResponse.from(page.map(SupplyTransactionResponse::from));
    }

    /**
     * 입출고 등록. 요청의 quantity 는 항상 양수이며, 부호와 실제 증감은 구분에 따라 여기서 정한다.
     *
     * @param itemId  품목 id
     * @param request 구분·수량·일자·메모
     * @return 저장된 이력
     */
    @Transactional
    public SupplyTransactionResponse addTransaction(Long itemId, SupplyTransactionRequest request) {
        SupplyItem item = findItemOrThrow(itemId);

        SupplyTransaction transaction = new SupplyTransaction();
        transaction.setItem(item);
        transaction.setTxType(request.getTxType());
        transaction.setQuantity(toSignedDelta(itemId, request));
        transaction.setTxDate(request.getTxDate());
        transaction.setMemo(request.getMemo());

        return SupplyTransactionResponse.from(supplyTransactionRepository.save(transaction));
    }

    /**
     * 입출고 이력 삭제(잘못 찍은 건 정정용). 이력이 빠지면 재고 합계도 그만큼 되돌아간다.
     *
     * @param itemId        품목 id
     * @param transactionId 이력 id
     */
    @Transactional
    public void deleteTransaction(Long itemId, Long transactionId) {
        SupplyTransaction transaction = supplyTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new BizException(ErrorCode.SUPPLY_TRANSACTION_NOT_FOUND));
        // 다른 품목의 이력 id 를 넘겨 지우는 일이 없도록 소속을 확인한다.
        if (transaction.getItem() == null || !transaction.getItem().getId().equals(itemId)) {
            throw new BizException(ErrorCode.SUPPLY_TRANSACTION_NOT_FOUND);
        }
        supplyTransactionRepository.delete(transaction);
    }

    // ===================== 엑셀 =====================

    /**
     * 재고 현황 엑셀(xlsx) — 품목·규격·단위·현재재고·안전재고·단가·재고금액.
     * 재고금액은 단가 x 현재재고이며 합계는 SUM 수식으로 넣는다. 공용 PoiMo 로 생성.
     *
     * @param keyword 품목명 검색어(비면 전체)
     * @return xlsx 바이트
     */
    public byte[] buildExcel(String keyword) {
        List<SupplyItem> list = StringUtils.hasText(keyword)
                ? supplyItemRepository.findByNameContainingIgnoreCaseOrderByNameAscIdAsc(keyword.trim())
                : supplyItemRepository.findAllByOrderByNameAscIdAsc();
        Map<Long, Integer> stocks = stockMap(list);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PoiMo poi = PoiMo.create("약품재고현황.xlsx");
            try {
                CellStyle title = poi.createNewStyle();
                poi.setFontStyle(title, "맑은 고딕", (short) 14, "bold", false, false);
                CellStyle head = poi.createNewStyle();
                poi.setFontStyle(head, "맑은 고딕", (short) 11, "bold", false, false);
                poi.setBackgroundColor(head, "light-yellow");
                poi.setLineBorder(head, "thin");
                CellStyle body = poi.createNewStyle();
                poi.setLineBorder(body, "thin");
                CellStyle num = poi.createNewStyle();
                poi.setLineBorder(num, "thin");
                poi.setAlign(num, "r");
                poi.setNumberFormat(num, "#,##0");
                CellStyle total = poi.createNewStyle();
                poi.setFontStyle(total, "맑은 고딕", (short) 11, "bold", false, false);
                poi.setLineBorder(total, "thin");
                CellStyle numTotal = poi.createNewStyle();
                poi.setFontStyle(numTotal, "맑은 고딕", (short) 11, "bold", false, false);
                poi.setLineBorder(numTotal, "thin");
                poi.setAlign(numTotal, "r");
                poi.setNumberFormat(numTotal, "#,##0");

                // 컬럼: 순번 | 품목 | 규격 | 단위 | 현재재고 | 안전재고 | 단가 | 재고금액 | 메모
                String[] cols = { "순번", "품목", "규격", "단위", "현재재고", "안전재고", "단가", "재고금액", "메모" };
                int amountCol = 7;

                poi.setMergedData(title, 0, 0, cols.length - 1, "약품/소모품 재고 현황$c");
                for (int i = 0; i < cols.length; i++) {
                    poi.setData(head, 2, i, cols[i] + "$c");
                }

                final int firstDataRow = 3;
                int r = firstDataRow;
                int no = 1;
                for (SupplyItem item : list) {
                    int stock = stockOf(stocks, item);
                    poi.setData(body, r, 0, no++ + "$c");
                    poi.setData(body, r, 1, item.getName() != null ? item.getName() : "");
                    poi.setData(body, r, 2, item.getSpec() != null ? item.getSpec() : "");
                    poi.setData(body, r, 3, item.getUnit() != null ? item.getUnit() + "$c" : "");
                    poi.setNumber(num, r, 4, stock);
                    poi.setNumber(num, r, 5, item.getSafetyQty() != null ? item.getSafetyQty() : 0);
                    // 단가 미입력은 0 이 아니라 빈칸으로 둔다 — "0원"과 "안 넣음"이 구분되게.
                    // 재고금액도 단가를 모르면 낼 수 없으므로 함께 비운다(SUM 수식은 텍스트 칸을 건너뛴다).
                    if (item.getUnitPrice() != null) {
                        poi.setNumber(num, r, 6, item.getUnitPrice());
                        poi.setNumber(num, r, amountCol, item.getUnitPrice() * stock);
                    } else {
                        poi.setData(body, r, 6, "");
                        poi.setData(body, r, amountCol, "");
                    }
                    poi.setData(body, r, 8, item.getMemo() != null ? item.getMemo() : "");
                    r++;
                }

                // 합계 행 — 재고금액 SUM 수식
                for (int i = 0; i <= 5; i++) {
                    poi.setData(total, r, i, "");
                }
                poi.setData(total, r, 6, "합계$c");
                if (!list.isEmpty()) {
                    int lastDataRow = r - 1;
                    poi.setFormula(numTotal, r, amountCol,
                            "SUM(" + new CellReference(firstDataRow, amountCol).formatAsString()
                                    + ":" + new CellReference(lastDataRow, amountCol).formatAsString() + ")");
                } else {
                    poi.setNumber(numTotal, r, amountCol, 0L);
                }
                poi.setData(total, r, 8, "");

                poi.evaluateAllFormulas();
                poi.write(out);
            } finally {
                poi.close();
            }
            return out.toByteArray();
        } catch (IOException ex) {
            log.error("엑셀 생성 실패", ex);
            throw new BizException(ErrorCode.EXCEL_GENERATION_FAILED);
        }
    }

    // ===================== 내부 =====================

    /**
     * 요청 수량을 재고 증감(부호 있는 값)으로 바꾼다.
     * 조정(ADJUST)은 입력값이 "창고를 세어본 실제 수량"이므로 현재 재고와의 차이만 남긴다.
     *
     * @param itemId  품목 id
     * @param request 입출고 요청
     * @return 이력에 저장할 증감 수량
     */
    private int toSignedDelta(Long itemId, SupplyTransactionRequest request) {
        int quantity = request.getQuantity();
        if (request.getTxType() == SupplyTxType.ADJUST) {
            return quantity - currentStock(itemId);
        }
        if (quantity < 1) {
            throw new BizException(ErrorCode.SUPPLY_QUANTITY_REQUIRED);
        }
        return request.getTxType() == SupplyTxType.IN ? quantity : -quantity;
    }

    /** 단일 품목의 현재 재고(이력 없으면 0) */
    private int currentStock(Long itemId) {
        Long sum = supplyTransactionRepository.findStockByItemId(itemId);
        return sum != null ? sum.intValue() : 0;
    }

    /** 여러 품목의 재고를 한 번에 조회해 map 으로 만든다(목록 N+1 방지) */
    private Map<Long, Integer> stockMap(List<SupplyItem> items) {
        if (items.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> ids = items.stream()
                .map(SupplyItem::getId)
                .collect(Collectors.toList());
        Map<Long, Integer> stocks = new HashMap<>();
        for (SupplyStockView view : supplyTransactionRepository.findStockByItemIds(ids)) {
            stocks.put(view.getItemId(), view.getQuantity() != null ? view.getQuantity().intValue() : 0);
        }
        return stocks;
    }

    /** 이력이 한 건도 없는 품목은 집계 결과에 없으므로 0 으로 본다 */
    private int stockOf(Map<Long, Integer> stocks, SupplyItem item) {
        Integer stock = stocks.get(item.getId());
        return stock != null ? stock : 0;
    }

    private void apply(SupplyItem item, SupplyItemRequest request) {
        item.setName(request.getName().trim());
        item.setSpec(trimToNull(request.getSpec()));
        item.setPhType(request.getPhType());
        item.setUnit(request.getUnit().trim());
        item.setUnitPrice(request.getUnitPrice());
        item.setSafetyQty(request.getSafetyQty());
        item.setMemo(request.getMemo());
    }

    /**
     * 같은 이름·규격의 품목이 이미 있는지 <b>엔티티에 값을 넣기 전에</b> 검사한다.
     *
     * 엔티티를 먼저 고치면 안 되는 이유: 영속 상태 엔티티를 수정한 뒤 이 검사 쿼리를 실행하면
     * Hibernate 가 auto-flush 로 UPDATE 를 먼저 내보낸다. 그러면 검사가 돌기도 전에
     * DB 의 유니크 인덱스(V21 uk_supply_item_name_spec)가 거부해 500 이 나간다.
     * 등록은 409 인데 수정만 500 이 되는 문제가 실제로 있었다.
     *
     * @param request   요청 값(apply 와 같은 방식으로 다듬어서 비교한다)
     * @param excludeId 검사에서 제외할 id — 신규 등록이면 NO_EXCLUDE_ID
     */
    private void checkDuplicate(SupplyItemRequest request, long excludeId) {
        String name = request.getName() != null ? request.getName().trim() : "";
        String spec = trimToNull(request.getSpec());
        // 규격 없음(null)을 그대로 넘기면 JPQL 파라미터 타입이 정해지지 않는다. 빈 문자열로 맞춰서 넘긴다.
        String specKey = spec != null ? spec : "";
        if (supplyItemRepository.existsDuplicate(name, specKey, excludeId)) {
            throw new BizException(ErrorCode.SUPPLY_ITEM_DUPLICATE);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private SupplyItem findItemOrThrow(Long id) {
        return supplyItemRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.SUPPLY_ITEM_NOT_FOUND));
    }
}
