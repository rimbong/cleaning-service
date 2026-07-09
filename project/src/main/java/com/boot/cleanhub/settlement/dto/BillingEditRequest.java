package com.boot.cleanhub.settlement.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   청구 수정 요청 — 청구액·메모만 수정(그 달만 할인/추가작업 등).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
@Setter
public class BillingEditRequest {

    @NotNull(message = "청구액은 필수입니다.")
    @PositiveOrZero(message = "청구액은 0 이상이어야 합니다.")
    private Long amount;

    private String memo;
}
