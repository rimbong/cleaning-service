package com.boot.cleanhub.biz.settlement.dto;

import java.util.List;

import lombok.Getter;

/**
 * <pre>
 *   연간 수금 현황의 요일 블록 — 청소 요일 하나(월~일 또는 미지정)에 속한 거래처 목록.
 *   수정환경.xls 의 요일 블록(월/화/수/목/금)에 대응한다.
 *   한 거래처가 여러 요일(월수금 등)이면 각 요일 블록에 모두 들어간다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
public class YearlyCollectionGroup {

    private final String weekday;   // MON~SUN 또는 NONE(요일 미지정)
    private final String label;     // 월~일 또는 "요일 미지정"
    private final int count;
    private final List<YearlyCollectionRow> rows;

    public YearlyCollectionGroup(String weekday, String label, List<YearlyCollectionRow> rows) {
        this.weekday = weekday;
        this.label = label;
        this.rows = rows;
        this.count = rows.size();
    }
}
