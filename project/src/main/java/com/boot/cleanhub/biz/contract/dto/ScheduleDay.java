package com.boot.cleanhub.biz.contract.dto;

import java.util.List;

import lombok.Getter;

/**
 * <pre>
 *   요일 하나의 청소 스케줄 — 요일 코드·라벨 + 그 요일 청소할 거래처 목록.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
public class ScheduleDay {

    private final String weekday;   // MON~SUN
    private final String label;     // 월~일
    private final int count;
    private final List<ScheduleItem> items;

    public ScheduleDay(String weekday, String label, List<ScheduleItem> items) {
        this.weekday = weekday;
        this.label = label;
        this.items = items;
        this.count = items.size();
    }
}
