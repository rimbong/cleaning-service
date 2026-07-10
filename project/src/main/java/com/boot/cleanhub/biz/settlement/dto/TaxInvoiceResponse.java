package com.boot.cleanhub.biz.settlement.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.boot.cleanhub.biz.settlement.domain.TaxInvoice;

import lombok.Getter;

/**
 * <pre>
 *   세금계산서 발행 기록 응답.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
public class TaxInvoiceResponse {

    private final Long id;
    private final Long clientId;
    private final String clientName;
    private final Integer fromYear;
    private final Integer fromMonth;
    private final Integer toYear;
    private final Integer toMonth;
    private final Long supplyAmount;
    private final Long taxAmount;
    private final String basis;
    private final LocalDate issueDate;
    private final LocalDateTime createdAt;

    private TaxInvoiceResponse(TaxInvoice t) {
        this.id = t.getId();
        this.clientId = t.getClient() != null ? t.getClient().getId() : null;
        this.clientName = t.getClient() != null ? t.getClient().getName() : null;
        this.fromYear = t.getFromYear();
        this.fromMonth = t.getFromMonth();
        this.toYear = t.getToYear();
        this.toMonth = t.getToMonth();
        this.supplyAmount = t.getSupplyAmount();
        this.taxAmount = t.getTaxAmount();
        this.basis = t.getBasis();
        this.issueDate = t.getIssueDate();
        this.createdAt = t.getCreatedAt();
    }

    public static TaxInvoiceResponse from(TaxInvoice t) {
        return new TaxInvoiceResponse(t);
    }
}
