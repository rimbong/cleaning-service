package com.boot.cleanhub.biz.settlement.dto;

import java.util.List;

import lombok.Getter;

/**
 * <pre>
 *   세금계산서 집계 응답 — 기간·기준 + 거래처별 행 + 합계.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
public class TaxInvoiceAggResponse {

    private final int fromYear;
    private final int fromMonth;
    private final int toYear;
    private final int toMonth;
    private final String basis;        // BILLED / PAID
    private final long totalSupply;
    private final long totalTax;
    private final List<TaxInvoiceAggRow> rows;

    public TaxInvoiceAggResponse(int fromYear, int fromMonth, int toYear, int toMonth, String basis, List<TaxInvoiceAggRow> rows) {
        this.fromYear = fromYear;
        this.fromMonth = fromMonth;
        this.toYear = toYear;
        this.toMonth = toMonth;
        this.basis = basis;
        this.rows = rows;
        long supply = 0;
        long tax = 0;
        for (TaxInvoiceAggRow r : rows) {
            supply += r.getSupplyAmount();
            tax += r.getTaxAmount();
        }
        this.totalSupply = supply;
        this.totalTax = tax;
    }
}
