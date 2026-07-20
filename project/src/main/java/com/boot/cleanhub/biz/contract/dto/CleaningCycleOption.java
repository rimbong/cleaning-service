package com.boot.cleanhub.biz.contract.dto;

import com.boot.cleanhub.biz.contract.domain.CleaningCycle;

import lombok.Getter;

/**
 * <pre>
 *   청소 주기 선택 항목 — 화면의 주기 목록과 월 방문 횟수 미리보기에 쓴다.
 *
 *   배수를 화면에 하드코딩하지 않고 여기서 내려주는 이유:
 *   같은 규칙이 서버와 화면 두 곳에 있으면 한쪽만 바뀌어 어긋난다.
 *   실제로 격주를 화면이 "주 배수 / 2" 로 유도하고 있어, 서버 배수를 바꾸면
 *   저장되는 값과 화면에 보이는 값이 달라지는 상태였다.
 *
 *   monthlyMultiplier 가 null 이면 요일로 계산할 수 없는 주기이며(매월),
 *   화면은 월 방문 횟수를 직접 입력받아야 한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Getter
public class CleaningCycleOption {

    private final String value;
    private final String label;
    /** 청소 요일 하나가 한 달에 몇 번이 되는지(null 이면 직접 입력 대상) */
    private final Integer monthlyMultiplier;

    private CleaningCycleOption(CleaningCycle cycle) {
        this.value = cycle.name();
        this.label = cycle.getLabel();
        this.monthlyMultiplier = cycle.getMonthlyMultiplier();
    }

    public static CleaningCycleOption from(CleaningCycle cycle) {
        return new CleaningCycleOption(cycle);
    }
}
