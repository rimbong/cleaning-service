package com.boot.cleanhub.biz.settlement.dto;

import java.util.List;

import lombok.Getter;

/**
 * <pre>
 *   연간 수금 현황 응답 — 한 해(year)의 거래처별 월별 수금일 매트릭스.
 *   거래처는 청소 요일 블록(월~일, 미지정)으로 묶는다(수정환경.xls 요일 블록 구조).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 2.0
 */
@Getter
public class YearlyCollectionResponse {

    private final int year;
    private final List<YearlyCollectionGroup> groups;

    public YearlyCollectionResponse(int year, List<YearlyCollectionGroup> groups) {
        this.year = year;
        this.groups = groups;
    }
}
