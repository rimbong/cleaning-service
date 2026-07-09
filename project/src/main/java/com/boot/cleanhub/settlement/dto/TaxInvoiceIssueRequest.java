package com.boot.cleanhub.settlement.dto;

import java.time.LocalDate;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   세금계산서 발행 기록 요청 — 거래처·기간·기준으로 집계해 발행 기록을 저장한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
@Setter
public class TaxInvoiceIssueRequest {

    @NotNull(message = "거래처는 필수입니다.")
    private Long clientId;

    @NotNull(message = "연도는 필수입니다.")
    private Integer year;

    @NotNull
    @Min(1) @Max(12)
    private Integer fromMonth;

    @NotNull
    @Min(1) @Max(12)
    private Integer toMonth;

    /** 집계 기준 BILLED(청구) / PAID(수금). 비면 BILLED */
    private String basis;

    @NotNull(message = "발행일은 필수입니다.")
    private LocalDate issueDate;
}
