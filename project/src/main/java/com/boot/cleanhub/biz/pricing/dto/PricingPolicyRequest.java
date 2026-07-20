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

    /** 주기 계수 공식의 기준값 — 월 1회일 때의 계수 */
    @NotNull(message = "계수 기준값은 필수입니다.")
    @DecimalMin(value = "0.01", message = "계수 기준값은 0.01 이상이어야 합니다.")
    @DecimalMax(value = "99.99", message = "계수 기준값이 너무 큽니다.")
    private BigDecimal coefBase;

    /**
     * 할인 지수. 1 이면 방문 횟수에 정비례(할인 없음), 작을수록 자주 갈 때 1회당 단가가 더 내려간다.
     * 1 을 넘으면 자주 갈수록 1회당 단가가 <b>올라가</b> 상식과 반대가 되므로 막는다.
     */
    @NotNull(message = "할인 지수는 필수입니다.")
    @DecimalMin(value = "0.1", message = "할인 지수는 0.1 이상이어야 합니다.")
    @DecimalMax(value = "1.0", message = "할인 지수는 1.0 을 넘을 수 없습니다(자주 갈수록 1회당 단가가 올라갑니다).")
    private BigDecimal coefExponent;

    @NotNull(message = "반올림 단위는 필수입니다.")
    @Positive(message = "반올림 단위는 1 이상이어야 합니다.")
    private Long roundingUnit;

    @Size(max = 255)
    private String memo;
}
