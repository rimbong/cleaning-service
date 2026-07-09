package com.boot.cleanhub.settlement.dto;

import java.util.List;

import lombok.Getter;

/**
 * <pre>
 *   월 정산 응답 — 그 달의 청구 목록 + 합계(청구·수금·미수·건수).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
public class SettlementMonthResponse {

    private final int year;
    private final int month;
    private final long totalBilled;   // 청구 총액
    private final long totalPaid;     // 수금 총액
    private final long totalUnpaid;   // 미수 총액(청구-수금)
    private final int count;          // 청구 건수
    private final List<BillingResponse> items;

    public SettlementMonthResponse(int year, int month, List<BillingResponse> items) {
        this.year = year;
        this.month = month;
        this.items = items;
        long billed = 0;
        long paid = 0;
        for (BillingResponse b : items) {
            billed += b.getAmount() != null ? b.getAmount() : 0L;
            paid += b.getPaidAmount() != null ? b.getPaidAmount() : 0L;
        }
        this.totalBilled = billed;
        this.totalPaid = paid;
        this.totalUnpaid = billed - paid;
        this.count = items.size();
    }
}
