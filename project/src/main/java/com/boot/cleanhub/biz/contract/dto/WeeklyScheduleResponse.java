package com.boot.cleanhub.biz.contract.dto;

import java.util.List;

import lombok.Getter;

/**
 * <pre>
 *   주간 청소 스케줄 응답 — 월~일 각 요일의 청소 대상 거래처 목록.
 *   진행 중(ACTIVE) 계약 중 청소 요일이 지정된 것만 요일별로 분류한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
public class WeeklyScheduleResponse {

    private final List<ScheduleDay> days;

    public WeeklyScheduleResponse(List<ScheduleDay> days) {
        this.days = days;
    }
}
