package com.boot.cleanhub.biz.settlement.dto;

import lombok.Getter;

/**
 * <pre>
 *   세금계산서 집계 행 — 거래처별 기간 공급가액·세액.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
public class TaxInvoiceAggRow {

    private final Long clientId;        // null = 거래처 미연결(견적 등)
    private final String clientName;
    private final String businessNumber;
    private final long supplyAmount;    // 공급가액(부가세 기준으로 산출한 합)
    private final long taxAmount;       // 세액(부가세 기준으로 산출한 합)
    private final int count;            // 청구 건수

    public TaxInvoiceAggRow(Long clientId, String clientName, String businessNumber,
            long supplyAmount, long taxAmount, int count) {
        this.clientId = clientId;
        this.clientName = clientName;
        this.businessNumber = businessNumber;
        this.supplyAmount = supplyAmount;
        this.taxAmount = taxAmount;
        this.count = count;
    }
}
