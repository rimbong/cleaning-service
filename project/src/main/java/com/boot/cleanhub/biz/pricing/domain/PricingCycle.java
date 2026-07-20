package com.boot.cleanhub.biz.pricing.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   권장가 산정용 청소 주기 — 단가표의 6단계.
 *
 *   계약의 {@code CleaningCycle}(매주/격주/매월) 은 3단계뿐이라 주2회·주3회·월3회를 표현할 수 없다.
 *   그렇다고 CleaningCycle 을 늘리면 이미 쌓인 계약 데이터와 스케줄 화면까지 영향을 받으므로,
 *   산정 전용으로 이 enum 을 따로 둔다. 계약에서 넘어올 때의 환산은 PricingService 가 한다.
 *
 *   visitsPerMonth 는 "1회 방문 환산 금액"을 보여주기 위한 값이다(주 단위는 4주로 계산).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor
public enum PricingCycle {

    MONTHLY_1("월 1회", 1),
    MONTHLY_2("월 2회(격주)", 2),
    MONTHLY_3("월 3회", 3),
    WEEKLY_1("주 1회", 4),
    WEEKLY_2("주 2회", 8),
    WEEKLY_3("주 3회", 12);

    private final String label;
    private final int visitsPerMonth;
}
