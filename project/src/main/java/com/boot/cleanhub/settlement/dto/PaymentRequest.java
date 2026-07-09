package com.boot.cleanhub.settlement.dto;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   입금 등록 요청.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
@Setter
public class PaymentRequest {

    @NotNull(message = "입금액은 필수입니다.")
    @Positive(message = "입금액은 0보다 커야 합니다.")
    private Long amount;

    @NotNull(message = "입금일은 필수입니다.")
    private LocalDate paidDate;

    @Size(max = 30)
    private String method;

    @Size(max = 255)
    private String memo;
}
