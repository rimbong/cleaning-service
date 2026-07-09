package com.boot.cleanhub.settlement.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boot.cleanhub.client.domain.Client;
import com.boot.cleanhub.client.repository.ClientRepository;
import com.boot.cleanhub.error.BizException;
import com.boot.cleanhub.error.ErrorCode;
import com.boot.cleanhub.settlement.domain.Billing;
import com.boot.cleanhub.settlement.domain.TaxInvoice;
import com.boot.cleanhub.settlement.dto.TaxInvoiceAggResponse;
import com.boot.cleanhub.settlement.dto.TaxInvoiceAggRow;
import com.boot.cleanhub.settlement.dto.TaxInvoiceIssueRequest;
import com.boot.cleanhub.settlement.dto.TaxInvoiceResponse;
import com.boot.cleanhub.settlement.repository.BillingRepository;
import com.boot.cleanhub.settlement.repository.PaymentRepository;
import com.boot.cleanhub.settlement.repository.TaxInvoiceRepository;

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

    /** 집계표 엑셀(xlsx) 바이트 — 세금계산서 발행/신고용 거래처별 공급가액·세액 목록. */
    public byte[] buildSummaryExcel(int year, int fromMonth, int toMonth, String basis) {
        TaxInvoiceAggResponse agg = aggregate(year, fromMonth, toMonth, basis);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("세금계산서 집계");
            CellStyle header = headerStyle(wb);
            CellStyle numeric = numericStyle(wb);

            String basisLabel = BASIS_PAID.equals(agg.getBasis()) ? "수금 기준" : "청구 기준";
            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue(year + "년 " + fromMonth + "~" + toMonth + "월 세금계산서 집계 (" + basisLabel + ")");

            String[] cols = { "순번", "사업자번호", "상호", "공급가액", "세액", "건수" };
            Row head = sheet.createRow(2);
            for (int i = 0; i < cols.length; i++) {
                Cell c = head.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(header);
            }

            int r = 3;
            int no = 1;
            for (TaxInvoiceAggRow row : agg.getRows()) {
                Row dr = sheet.createRow(r++);
                dr.createCell(0).setCellValue(no++);
                dr.createCell(1).setCellValue(row.getBusinessNumber() != null ? row.getBusinessNumber() : "");
                dr.createCell(2).setCellValue(row.getClientName() != null ? row.getClientName() : "");
                setNum(dr.createCell(3), row.getSupplyAmount(), numeric);
                setNum(dr.createCell(4), row.getTaxAmount(), numeric);
                dr.createCell(5).setCellValue(row.getCount());
            }
            Row total = sheet.createRow(r);
            Cell tl = total.createCell(2);
            tl.setCellValue("합계");
            tl.setCellStyle(header);
            setNum(total.createCell(3), agg.getTotalSupply(), header);
            setNum(total.createCell(4), agg.getTotalTax(), header);

            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }
            wb.write(out);
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

    private static void setNum(Cell cell, long value, CellStyle style) {
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static CellStyle headerStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        return s;
    }

    private static CellStyle numericStyle(Workbook wb) {
        CellStyle s = wb.createCellStyle();
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
        return s;
    }

    /** 거래처별 누적기(집계용) */
    private static final class Acc {
        private String name;
        private String businessNumber;
        private long supply;
        private int count;
    }
}
