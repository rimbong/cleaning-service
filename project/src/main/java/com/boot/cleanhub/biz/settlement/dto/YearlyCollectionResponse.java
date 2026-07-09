package com.boot.cleanhub.biz.settlement.dto;

import java.util.List;

import lombok.Getter;

/**
 * <pre>
 *   연간 수금 현황 응답 — 한 해(year)의 거래처별 월별 수금일 매트릭스.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
public class YearlyCollectionResponse {

    private final int year;
    private final int count;
    private final List<YearlyCollectionRow> rows;

    public YearlyCollectionResponse(int year, List<YearlyCollectionRow> rows) {
        this.year = year;
        this.rows = rows;
        this.count = rows.size();
    }
}
