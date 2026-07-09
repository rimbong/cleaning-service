package com.boot.cleanhub.biz.settlement.dto;

import com.boot.cleanhub.biz.contract.domain.Contract;
import com.boot.cleanhub.biz.quote.domain.Quote;
import com.boot.cleanhub.biz.settlement.domain.Billing;
import com.boot.cleanhub.biz.settlement.domain.BillingStatus;

import lombok.Getter;

/**
 * <pre>
 *   청구 응답 DTO — 대상(계약/견적)·대상명·청구액·수금액·파생상태를 평면화.
 *   수금액(paidAmount)은 payment 합으로 서비스에서 계산해 넘긴다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
public class BillingResponse {

    private final Long id;
    private final String sourceType;   // CONTRACT / QUOTE
    private final Long contractId;
    private final Long quoteId;
    private final String targetName;   // 거래처/고객명
    private final String title;        // 계약명/서비스 내용
    private final Integer billYear;
    private final Integer billMonth;
    private final Long amount;         // 청구액
    private final Long paidAmount;     // 수금액(입금 합)
    private final BillingStatus status;
    private final String statusLabel;
    private final String memo;

    private BillingResponse(Billing b, long paidAmount) {
        this.id = b.getId();
        Contract c = b.getContract();
        Quote q = b.getQuote();
        if (c != null) {
            this.sourceType = "CONTRACT";
            this.contractId = c.getId();
            this.quoteId = null;
            this.targetName = c.getClient() != null ? c.getClient().getName() : null;
            this.title = c.getTitle();
        } else {
            this.sourceType = "QUOTE";
            this.contractId = null;
            this.quoteId = q != null ? q.getId() : null;
            this.targetName = quoteTarget(q);
            this.title = q != null ? q.getTitle() : null;
        }
        this.billYear = b.getBillYear();
        this.billMonth = b.getBillMonth();
        this.amount = b.getAmount();
        this.paidAmount = paidAmount;
        long amt = b.getAmount() != null ? b.getAmount() : 0L;
        this.status = BillingStatus.of(amt, paidAmount);
        this.statusLabel = this.status.getLabel();
        this.memo = b.getMemo();
    }

    private static String quoteTarget(Quote q) {
        if (q == null) {
            return null;
        }
        if (q.getClient() != null) {
            return q.getClient().getName();
        }
        return q.getCustomerName();
    }

    public static BillingResponse of(Billing billing, long paidAmount) {
        return new BillingResponse(billing, paidAmount);
    }
}
