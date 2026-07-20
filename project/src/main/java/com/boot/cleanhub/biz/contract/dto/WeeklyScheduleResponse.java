package com.boot.cleanhub.biz.contract.dto;

import java.util.List;

import lombok.Getter;

/**
 * <pre>
 *   주간 청소 스케줄 응답 — 월~일 각 요일의 청소 대상 거래처 목록.
 *
 *   청소 요일이 지정된 계약은 요일별로 분류하고, 요일이 없는 계약은 unscheduled 로 따로 담는다.
 *   "매월 첫째주 수요일" 처럼 요일 하나로 적을 수 없는 계약이 있는데, 요일이 없다고 빼버리면
 *   "오늘 어디 가지?" 화면에서 사라져 청소를 빠뜨리게 된다. 그래서 목록에서 지우지 않고
 *   별도 구역에 모아 보여준다(상세 일정은 계약 메모에 적는다).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.1
 */
@Getter
public class WeeklyScheduleResponse {

    private final List<ScheduleDay> days;

    /** 청소 요일이 지정되지 않은 진행중 계약(매월 계약 등) */
    private final List<ScheduleItem> unscheduled;

    public WeeklyScheduleResponse(List<ScheduleDay> days, List<ScheduleItem> unscheduled) {
        this.days = days;
        this.unscheduled = unscheduled;
    }
}
