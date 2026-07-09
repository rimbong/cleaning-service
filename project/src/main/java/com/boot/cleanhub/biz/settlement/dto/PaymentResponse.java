package com.boot.cleanhub.biz.settlement.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.boot.cleanhub.biz.settlement.domain.Payment;

import lombok.Getter;

/**
 * <pre>
 *   입금 응답.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
public class PaymentResponse {

    private final Long id;
    private final Long billingId;
    private final Long amount;
    private final LocalDate paidDate;
    private final String method;
    private final String memo;
    private final LocalDateTime createdAt;

    private PaymentResponse(Payment p) {
        this.id = p.getId();
        this.billingId = p.getBilling() != null ? p.getBilling().getId() : null;
        this.amount = p.getAmount();
        this.paidDate = p.getPaidDate();
        this.method = p.getMethod();
        this.memo = p.getMemo();
        this.createdAt = p.getCreatedAt();
    }

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment);
    }
}
