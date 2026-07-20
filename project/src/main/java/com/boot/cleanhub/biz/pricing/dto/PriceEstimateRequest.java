package com.boot.cleanhub.biz.pricing.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   권장가 산정 요청 — 건물 규모와 청소 주기.
 *
 *   상한(@Max)은 오타로 0 을 더 붙였을 때 말도 안 되는 금액이 그대로 견적에 들어가는 것을 막는다.
 *   (실무상 계단청소 대상 건물은 이 범위를 넘지 않는다)
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Getter
@Setter
public class PriceEstimateRequest {

    @NotNull(message = "지상 층수는 필수입니다.")
    @PositiveOrZero(message = "층수는 0 이상이어야 합니다.")
    @Max(value = 100, message = "층수가 너무 큽니다. 값을 확인하세요.")
    private Integer floors;

    @NotNull(message = "세대수는 필수입니다.")
    @PositiveOrZero(message = "세대수는 0 이상이어야 합니다.")
    @Max(value = 1000, message = "세대수가 너무 큽니다. 값을 확인하세요.")
    private Integer householdCount;

    @PositiveOrZero(message = "공용 화장실 수는 0 이상이어야 합니다.")
    @Max(value = 100, message = "공용 화장실 수가 너무 큽니다. 값을 확인하세요.")
    private Integer sharedToilets;

    @PositiveOrZero(message = "추가 층수는 0 이상이어야 합니다.")
    @Max(value = 20, message = "추가 층수가 너무 큽니다. 값을 확인하세요.")
    private Integer extraFloors;

    private Boolean hasElevator;

    /**
     * 월 방문 횟수. 주 단위로 생각하면 주1회=4, 주2회=8, 주3회=12, 주4회=16 이다.
     * 예전처럼 정해진 몇 단계 중에서만 고르지 않으므로 어떤 횟수든 산정된다.
     */
    @NotNull(message = "월 방문 횟수는 필수입니다.")
    @Min(value = 1, message = "월 방문 횟수는 1 이상이어야 합니다.")
    @Max(value = 31, message = "월 방문 횟수가 너무 큽니다. 값을 확인하세요.")
    private Integer visitsPerMonth;
}
