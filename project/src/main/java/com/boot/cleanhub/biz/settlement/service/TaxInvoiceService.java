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
