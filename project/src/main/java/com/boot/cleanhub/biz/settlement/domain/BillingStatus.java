package com.boot.cleanhub.biz.settlement.domain;

/**
 * <pre>
 *   청구 수금 상태 — 저장하지 않고 입금 합으로 파생 계산한다.
 *   - UNPAID  : 미수(입금 0)
 *   - PARTIAL : 부분수금(0 < 입금 < 청구액)
 *   - PAID    : 완납(입금 >= 청구액)
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
public enum BillingStatus {

    UNPAID("미수"),
    PARTIAL("부분수금"),
    PAID("완납");

    private final String label;

    BillingStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** 청구액·입금합으로 상태 계산 */
    public static BillingStatus of(long amount, long paid) {
        if (paid <= 0) {
            return UNPAID;
        }
        if (paid < amount) {
            return PARTIAL;
        }
        return PAID;
    }
}
