package com.boot.cleanhub.biz.settlement.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.CellStyle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boot.cleanhub.biz.client.domain.Client;
import com.boot.cleanhub.biz.client.repository.ClientRepository;
import com.boot.cleanhub.error.BizException;
import com.boot.cleanhub.error.ErrorCode;
import com.boot.cleanhub.biz.settlement.domain.Billing;
import com.boot.cleanhub.biz.settlement.domain.TaxInvoice;
import com.boot.cleanhub.biz.settlement.dto.TaxInvoiceAggResponse;
import com.boot.cleanhub.biz.settlement.dto.TaxInvoiceAggRow;
import com.boot.cleanhub.biz.settlement.dto.TaxInvoiceIssueRequest;
import com.boot.cleanhub.biz.settlement.dto.TaxInvoiceResponse;
import com.boot.cleanhub.biz.settlement.repository.BillingRepository;
import com.boot.cleanhub.biz.settlement.repository.PaymentRepository;
import com.boot.cleanhub.biz.settlement.repository.TaxInvoiceRepository;
import com.boot.cleanhub.biz.company.dto.CompanyResponse;
import com.boot.cleanhub.biz.company.service.CompanyService;
import com.boot.cleanhub.util.excel.PoiMo;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   세금계산서 — 기간·거래처별 집계(청구/수금 기준) + 발행 기록 + 집계표 엑셀 출력.
 *   집계는 정산(billing/payment)에서 계산한다. 세액 = 공급가액 * 10%.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxInvoiceService {

    private static final String BASIS_PAID = "PAID";

    private final BillingRepository billingRepository;
    private final PaymentRepository paymentRepository;
    private final TaxInvoiceRepository taxInvoiceRepository;
    private final ClientRepository clientRepository;
    private final CompanyService companyService;

    /** 거래처별 기간 집계(BILLED=청구합 / PAID=수금합). */
    public TaxInvoiceAggResponse aggregate(int year, int fromMonth, int toMonth, String basis) {
        String b = BASIS_PAID.equalsIgnoreCase(basis) ? BASIS_PAID : "BILLED";
        List<Billing> billings = billingRepository.findByPeriodWithRefs(year, fromMonth, toMonth);

        Map<Long, Long> paidMap = new HashMap<>();
        if (BASIS_PAID.equals(b) && !billings.isEmpty()) {
            List<Long> ids = billings.stream().map(Billing::getId).collect(Collectors.toList());
            for (Object[] row : paymentRepository.sumGroupedByBillingIds(ids)) {
                paidMap.put((Long) row[0], ((Number) row[1]).longValue());
            }
        }

        Map<Long, Acc> byClient = new HashMap<>();
        for (Billing bill : billings) {
            Client cl = clientOf(bill);
            Long key = cl != null ? cl.getId() : null;
            long amount = BASIS_PAID.equals(b)
                    ? paidMap.getOrDefault(bill.getId(), 0L)
                    : (bill.getAmount() != null ? bill.getAmount() : 0L);
            Acc acc = byClient.computeIfAbsent(key, k -> new Acc());
            if (acc.name == null) {
                acc.name = cl != null ? cl.getName() : "(거래처 미연결)";
                acc.businessNumber = cl != null ? cl.getBusinessNumber() : null;
            }
            acc.supply += amount;
            acc.count += 1;
        }

        List<TaxInvoiceAggRow> rows = new ArrayList<>();
        for (Map.Entry<Long, Acc> e : byClient.entrySet()) {
            Acc a = e.getValue();
            rows.add(new TaxInvoiceAggRow(e.getKey(), a.name, a.businessNumber, a.supply, a.count));
        }
        rows.sort(Comparator.comparing(TaxInvoiceAggRow::getClientName, Comparator.nullsLast(Comparator.naturalOrder())));
        return new TaxInvoiceAggResponse(year, fromMonth, toMonth, b, rows);
    }

    /** 발행 기록 저장 — 그 거래처·기간 집계액으로. */
    @Transactional
    public TaxInvoiceResponse issue(TaxInvoiceIssueRequest req) {
        TaxInvoiceAggResponse agg = aggregate(req.getYear(), req.getFromMonth(), req.getToMonth(), req.getBasis());
        TaxInvoiceAggRow row = agg.getRows().stream()
                .filter(r -> Objects.equals(r.getClientId(), req.getClientId()))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.BILLING_NOT_FOUND));
        Client client = clientRepository.findById(req.getClientId())
                .orElseThrow(() -> new BizException(ErrorCode.CLIENT_NOT_FOUND));

        TaxInvoice t = new TaxInvoice();
        t.setClient(client);
        t.setPeriodYear(req.getYear());
        t.setFromMonth(req.getFromMonth());
        t.setToMonth(req.getToMonth());
        t.setSupplyAmount(row.getSupplyAmount());
        t.setTaxAmount(row.getTaxAmount());
        t.setBasis(agg.getBasis());
        t.setIssueDate(req.getIssueDate());
        return TaxInvoiceResponse.from(taxInvoiceRepository.save(t));
    }

    /** 발행 기록 목록. */
    public List<TaxInvoiceResponse> list() {
        return taxInvoiceRepository.findAllWithClient().stream()
                .map(TaxInvoiceResponse::from)
                .collect(Collectors.toList());
    }

    /** 발행 기록 삭제. */
    @Transactional
    public void delete(Long id) {
        TaxInvoice t = taxInvoiceRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.TAX_INVOICE_NOT_FOUND));
        taxInvoiceRepository.delete(t);
    }

    /**
     * 집계표 엑셀(xlsx) 바이트 — 세금계산서 발행/신고용 거래처별 공급가액·세액 목록.
     * 공용 유틸 {@link PoiMo} 로 생성한다(setData 가 한글 폭을 반영해 열 너비를 자동 조정).
     * 데이터 정렬은 값 뒤 "$c"(가운데)/"$r"(오른쪽) 수식어로 지정한다.
     * 제목은 setMergedData 로 병합해 넣어(열 너비 영향 없음) 순번 열이 넓어지지 않게 한다.
     * 구성: 0행 제목(병합) / 1행 공백 / 2행 헤더 / 3행~ 데이터 / 마지막 합계.
     */
    public byte[] buildSummaryExcel(int year, int fromMonth, int toMonth, String basis) {
        TaxInvoiceAggResponse agg = aggregate(year, fromMonth, toMonth, basis);
        String basisLabel = BASIS_PAID.equals(agg.getBasis()) ? "수금 기준" : "청구 기준";
        String titleText = year + "년 " + fromMonth + "~" + toMonth + "월 세금계산서 집계 (" + basisLabel + ")";
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PoiMo poi = PoiMo.create("세금계산서집계.xlsx");
            try {
                // 스타일: 제목(굵게 큰 글씨), 헤더(굵게+연노랑+테두리), 본문(테두리), 합계(굵게+테두리)
                CellStyle titleStyle = poi.createNewStyle();
                poi.setFontStyle(titleStyle, "맑은 고딕", (short) 14, "bold", false, false);

                CellStyle head = poi.createNewStyle();
                poi.setFontStyle(head, "맑은 고딕", (short) 11, "bold", false, false);
                poi.setBackgroundColor(head, "light-yellow");
                poi.setLineBorder(head, "thin");

                CellStyle body = poi.createNewStyle();
                poi.setLineBorder(body, "thin");

                CellStyle total = poi.createNewStyle();
                poi.setFontStyle(total, "맑은 고딕", (short) 11, "bold", false, false);
                poi.setLineBorder(total, "thin");

                // 제목(0행) — 0~5열 병합, 가운데 정렬(열 너비에 영향 주지 않음)
                poi.setMergedData(titleStyle, 0, 0, 5, titleText + "$c");

                // 헤더(2행) — "$c" 로 가운데 정렬 (1행은 공백)
                String[] cols = { "순번", "사업자번호", "상호", "공급가액", "세액", "건수" };
                for (int i = 0; i < cols.length; i++) {
                    poi.setData(head, 2, i, cols[i] + "$c");
                }

                // 데이터(3행~)
                int r = 3;
                int no = 1;
                for (TaxInvoiceAggRow row : agg.getRows()) {
                    poi.setData(body, r, 0, no++ + "$c");
                    poi.setData(body, r, 1, row.getBusinessNumber() != null ? row.getBusinessNumber() : "");
                    poi.setData(body, r, 2, row.getClientName() != null ? row.getClientName() : "");
                    poi.setData(body, r, 3, String.format("%,d", row.getSupplyAmount()) + "$r");
                    poi.setData(body, r, 4, String.format("%,d", row.getTaxAmount()) + "$r");
                    poi.setData(body, r, 5, row.getCount() + "$c");
                    r++;
                }

                // 합계 행
                poi.setData(total, r, 0, "");
                poi.setData(total, r, 1, "");
                poi.setData(total, r, 2, "합계$c");
                poi.setData(total, r, 3, String.format("%,d", agg.getTotalSupply()) + "$r");
                poi.setData(total, r, 4, String.format("%,d", agg.getTotalTax()) + "$r");
                poi.setData(total, r, 5, "");

                poi.write(out);
            } finally {
                poi.close();
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new BizException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 개별 세금계산서(별지 제11호 서식) 엑셀 — 발행 기록 하나를 그 양식으로 출력.
     * 공급자는 회사정보(Company), 공급받는자는 거래처(Client), 금액은 발행 기록에서.
     * 공급가액·세액은 자릿수 칸(억·천·백·십·만…)에 한 자리씩 넣는다(원본 양식 재현). 공용 PoiMo 로 그린다.
     *
     * @param id 발행 기록(TaxInvoice) id
     * @return 별지11호 양식 xlsx 바이트
     */
    public byte[] buildInvoiceForm(Long id) {
        TaxInvoice ti = taxInvoiceRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.TAX_INVOICE_NOT_FOUND));
        CompanyResponse supplier = companyService.get();
        Client buyer = ti.getClient();
        long supply = ti.getSupplyAmount() != null ? ti.getSupplyAmount() : 0L;
        long tax = ti.getTaxAmount() != null ? ti.getTaxAmount() : 0L;

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PoiMo poi = PoiMo.create("세금계산서.xlsx");
            try {
                for (int c = 0; c <= 32; c++) {
                    poi.setColumnWidth(c, 3);
                }
                CellStyle title = poi.createNewStyle();
                poi.setFontStyle(title, "맑은 고딕", (short) 16, "bold", false, false);
                CellStyle sub = poi.createNewStyle();
                poi.setFontStyle(sub, "맑은 고딕", (short) 9, "normal", false, false);
                CellStyle label = poi.createNewStyle();
                poi.setFontStyle(label, "맑은 고딕", (short) 9, "bold", false, false);
                poi.setBackgroundColor(label, "light-yellow");
                poi.setLineBorder(label, "thin");
                CellStyle cell = poi.createNewStyle();
                poi.setFontStyle(cell, "맑은 고딕", (short) 9, "normal", false, false);
                poi.setLineBorder(cell, "thin");

                // 제목
                poi.setMergedData(title, 0, 0, 32, "세  금  계  산  서$c");
                poi.setMergedData(sub, 1, 0, 32, "( 공급받는자 보관용 )$c");

                // 블록 헤더(공급자 / 공급받는자)
                poi.setMergedData(label, 2, 0, 15, "공 급 자$c");
                poi.setMergedData(label, 2, 16, 32, "공 급 받 는 자$c");

                // 등록번호
                poi.setMergedData(label, 3, 0, 2, "등록번호$c");
                poi.setMergedData(cell, 3, 3, 15, nvl(supplier.getBusinessNumber()) + "$c");
                poi.setMergedData(label, 3, 16, 18, "등록번호$c");
                poi.setMergedData(cell, 3, 19, 32, nvl(buyer.getBusinessNumber()) + "$c");

                // 상호 / 성명
                poi.setMergedData(label, 4, 0, 2, "상호$c");
                poi.setMergedData(cell, 4, 3, 8, nvl(supplier.getCompanyName()) + "$c");
                poi.setMergedData(label, 4, 9, 10, "성명$c");
                poi.setMergedData(cell, 4, 11, 15, nvl(supplier.getOwnerName()) + "$c");
                poi.setMergedData(label, 4, 16, 18, "상호$c");
                poi.setMergedData(cell, 4, 19, 25, nvl(buyer.getName()) + "$c");
                poi.setMergedData(label, 4, 26, 27, "성명$c");
                poi.setMergedData(cell, 4, 28, 32, nvl(buyer.getRepresentativeName()) + "$c");

                // 사업장 주소
                poi.setMergedData(label, 5, 0, 2, "사업장$c");
                poi.setMergedData(cell, 5, 3, 15, nvl(supplier.getAddress()) + "$l");
                poi.setMergedData(label, 5, 16, 18, "사업장$c");
                poi.setMergedData(cell, 5, 19, 32, nvl(buyer.getAddress()) + "$l");

                // 업태 / 종목
                poi.setMergedData(label, 6, 0, 2, "업태$c");
                poi.setMergedData(cell, 6, 3, 8, nvl(supplier.getBusinessType()) + "$c");
                poi.setMergedData(label, 6, 9, 10, "종목$c");
                poi.setMergedData(cell, 6, 11, 15, nvl(supplier.getBusinessItem()) + "$c");
                poi.setMergedData(label, 6, 16, 18, "업태$c");
                poi.setMergedData(cell, 6, 19, 25, nvl(buyer.getBusinessType()) + "$c");
                poi.setMergedData(label, 6, 26, 27, "종목$c");
                poi.setMergedData(cell, 6, 28, 32, nvl(buyer.getBusinessItem()) + "$c");

                // 작성 / 공급가액 / 세액 / 비고 헤더
                poi.setMergedData(label, 7, 0, 6, "작성$c");
                poi.setMergedData(label, 7, 7, 17, "공 급 가 액$c");
                poi.setMergedData(label, 7, 18, 27, "세 액$c");
                poi.setMergedData(label, 7, 28, 32, "비고$c");

                // 자릿수 라벨(8행)
                poi.setMergedData(label, 8, 0, 1, "년$c");
                poi.setMergedData(label, 8, 2, 3, "월$c");
                poi.setMergedData(label, 8, 4, 5, "일$c");
                poi.setData(label, 8, 6, "공란$c");
                String[] supplyLabels = { "백", "십", "억", "천", "백", "십", "만", "천", "백", "십", "일" };
                for (int i = 0; i < supplyLabels.length; i++) {
                    poi.setData(label, 8, 7 + i, supplyLabels[i] + "$c");
                }
                String[] taxLabels = { "십", "억", "천", "백", "십", "만", "천", "백", "십", "일" };
                for (int i = 0; i < taxLabels.length; i++) {
                    poi.setData(label, 8, 18 + i, taxLabels[i] + "$c");
                }
                poi.setMergedData(cell, 8, 28, 32, "");

                // 자릿수 값(9행)
                poi.setMergedData(cell, 9, 0, 1, ti.getPeriodYear() + "$c");
                poi.setMergedData(cell, 9, 2, 3, ti.getIssueDate().getMonthValue() + "$c");
                poi.setMergedData(cell, 9, 4, 5, ti.getIssueDate().getDayOfMonth() + "$c");
                poi.setData(cell, 9, 6, "");
                placeDigits(poi, cell, 9, 7, 17, supply);
                placeDigits(poi, cell, 9, 18, 27, tax);
                poi.setMergedData(cell, 9, 28, 32, "");

                // 품목 헤더(10행)
                poi.setData(label, 10, 0, "월$c");
                poi.setData(label, 10, 1, "일$c");
                poi.setMergedData(label, 10, 2, 8, "품목$c");
                poi.setMergedData(label, 10, 9, 11, "규격$c");
                poi.setMergedData(label, 10, 12, 14, "수량$c");
                poi.setMergedData(label, 10, 15, 19, "단가$c");
                poi.setMergedData(label, 10, 20, 25, "공급가액$c");
                poi.setMergedData(label, 10, 26, 30, "세액$c");
                poi.setMergedData(label, 10, 31, 32, "비고$c");

                // 품목 값(11행)
                poi.setData(cell, 11, 0, ti.getIssueDate().getMonthValue() + "$c");
                poi.setData(cell, 11, 1, ti.getIssueDate().getDayOfMonth() + "$c");
                poi.setMergedData(cell, 11, 2, 8, "청소비$c");
                poi.setMergedData(cell, 11, 9, 11, "");
                poi.setMergedData(cell, 11, 12, 14, "1$c");
                poi.setMergedData(cell, 11, 15, 19, String.format("%,d", supply) + "$r");
                poi.setMergedData(cell, 11, 20, 25, String.format("%,d", supply) + "$r");
                poi.setMergedData(cell, 11, 26, 30, String.format("%,d", tax) + "$r");
                poi.setMergedData(cell, 11, 31, 32, "");

                // 합계 헤더(12행)
                poi.setMergedData(label, 12, 0, 5, "합계금액$c");
                poi.setMergedData(label, 12, 6, 10, "현금$c");
                poi.setMergedData(label, 12, 11, 15, "수표$c");
                poi.setMergedData(label, 12, 16, 20, "어음$c");
                poi.setMergedData(label, 12, 21, 25, "외상미수금$c");
                poi.setMergedData(label, 12, 26, 32, "이 금액을 (청구) 함$c");

                // 합계 값(13행)
                poi.setMergedData(cell, 13, 0, 5, String.format("%,d", supply + tax) + "$r");
                poi.setMergedData(cell, 13, 6, 10, "");
                poi.setMergedData(cell, 13, 11, 15, "");
                poi.setMergedData(cell, 13, 16, 20, "");
                poi.setMergedData(cell, 13, 21, 25, "");
                poi.setMergedData(cell, 13, 26, 32, "");

                poi.write(out);
            } finally {
                poi.close();
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new BizException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /** 금액을 자릿수 칸(startCol~endCol)에 한 자리씩 오른쪽 정렬로 채운다. */
    private static void placeDigits(PoiMo poi, CellStyle style, int row, int startCol, int endCol, long amount) {
        String s = String.valueOf(Math.max(0L, amount));
        int di = s.length() - 1;
        for (int col = endCol; col >= startCol; col--) {
            String ch = di >= 0 ? String.valueOf(s.charAt(di)) : "";
            poi.setData(style, row, col, ch.isEmpty() ? "" : (ch + "$c"));
            di--;
        }
    }

    /** null → 빈 문자열. */
    private static String nvl(String s) {
        return s != null ? s : "";
    }

    // ===== helpers =====

    private static Client clientOf(Billing b) {
        if (b.getContract() != null) {
            return b.getContract().getClient();
        }
        if (b.getQuote() != null) {
            return b.getQuote().getClient();
        }
        return null;
    }

    /** 거래처별 누적기(집계용) */
    private static final class Acc {
        private String name;
        private String businessNumber;
        private long supply;
        private int count;
    }
}
