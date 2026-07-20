package com.boot.cleanhub.biz.pricing.dto;

import java.util.List;

import lombok.Getter;

/**
 * <pre>
 *   적정가 재산정 결과.
 *
 *   skipped* 는 "왜 이 계약이 목록에 없는지"를 알려주기 위한 값이다.
 *   조용히 빼버리면 전체를 검토한 것으로 오해하게 된다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Getter
public class PriceReviewResponse {

    /** 검토 대상(건물 규모가 입력된 진행중 계약) */
    private final List<PriceReviewRow> rows;
    /** 검토한 계약 수 */
    private final int reviewedCount;
    /** 건물 규모 미입력으로 제외된 계약 수 */
    private final int skippedNoBuilding;
    /** 청소 주기 미입력으로 제외된 계약 수 */
    private final int skippedNoCycle;
    /** 현재액 합계 */
    private final long totalCurrent;
    /** 권장가 합계 */
    private final long totalRecommended;

    public PriceReviewResponse(List<PriceReviewRow> rows, int skippedNoBuilding, int skippedNoCycle) {
        this.rows = rows;
        this.reviewedCount = rows.size();
        this.skippedNoBuilding = skippedNoBuilding;
        this.skippedNoCycle = skippedNoCycle;
        this.totalCurrent = rows.stream().mapToLong(PriceReviewRow::getCurrentAmount).sum();
        this.totalRecommended = rows.stream().mapToLong(PriceReviewRow::getRecommendedAmount).sum();
    }
}
