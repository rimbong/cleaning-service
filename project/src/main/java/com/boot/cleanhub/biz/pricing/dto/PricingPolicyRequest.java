package com.boot.cleanhub.biz.pricing.dto;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   단가 정책 수정 요청.
 *   계수는 0 이면 금액이 0 이 되어버리므로 최소 0.1 로 막는다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Getter
@Setter
public class PricingPolicyRequest {

    @NotNull(message = "기본 출동료는 필수입니다.")
    @PositiveOrZero(message = "기본 출동료는 0 이상이어야 합니다.")
    private Long baseFee;

    @NotNull(message = "층당 단가는 필수입니다.")
    @PositiveOrZero(message = "층당 단가는 0 이상이어야 합니다.")
    private Long perFloor;

    @NotNull(message = "세대당 단가는 필수입니다.")
    @PositiveOrZero(message = "세대당 단가는 0 이상이어야 합니다.")
    private Long perHousehold;

    @NotNull(message = "공용 화장실 단가는 필수입니다.")
    @PositiveOrZero(message = "공용 화장실 단가는 0 이상이어야 합니다.")
    private Long perToilet;

    @NotNull(message = "엘리베이터 가산은 필수입니다.")
    @PositiveOrZero(message = "엘리베이터 가산은 0 이상이어야 합니다.")
    private Long elevatorFee;

    @NotNull(message = "월 1회 계수는 필수입니다.")
    @DecimalMin(value = "0.1", message = "주기 계수는 0.1 이상이어야 합니다.")
    @DecimalMax(value = "99.99", message = "주기 계수가 너무 큽니다.")
    private BigDecimal coefMonthly1;

    @NotNull(message = "월 2회 계수는 필수입니다.")
    @DecimalMin(value = "0.1", message = "주기 계수는 0.1 이상이어야 합니다.")
    @DecimalMax(value = "99.99", message = "주기 계수가 너무 큽니다.")
    private BigDecimal coefMonthly2;

    @NotNull(message = "월 3회 계수는 필수입니다.")
    @DecimalMin(value = "0.1", message = "주기 계수는 0.1 이상이어야 합니다.")
    @DecimalMax(value = "99.99", message = "주기 계수가 너무 큽니다.")
    private BigDecimal coefMonthly3;

    @NotNull(message = "주 1회 계수는 필수입니다.")
    @DecimalMin(value = "0.1", message = "주기 계수는 0.1 이상이어야 합니다.")
    @DecimalMax(value = "99.99", message = "주기 계수가 너무 큽니다.")
    private BigDecimal coefWeekly1;

    @NotNull(message = "주 2회 계수는 필수입니다.")
    @DecimalMin(value = "0.1", message = "주기 계수는 0.1 이상이어야 합니다.")
    @DecimalMax(value = "99.99", message = "주기 계수가 너무 큽니다.")
    private BigDecimal coefWeekly2;

    @NotNull(message = "주 3회 계수는 필수입니다.")
    @DecimalMin(value = "0.1", message = "주기 계수는 0.1 이상이어야 합니다.")
    @DecimalMax(value = "99.99", message = "주기 계수가 너무 큽니다.")
    private BigDecimal coefWeekly3;

    @NotNull(message = "반올림 단위는 필수입니다.")
    @Positive(message = "반올림 단위는 1 이상이어야 합니다.")
    private Long roundingUnit;

    @Size(max = 255)
    private String memo;
}
