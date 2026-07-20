package com.boot.cleanhub.biz.pricing.dto;

import java.util.List;

import lombok.Getter;

/**
 * <pre>
 *   적정가 재산정 결과.
 *
 *   skipped 는 "왜 이 계약이 목록에 없는지"를 알려주기 위한 값이다.
 *   조용히 빼버리면 전체를 검토한 것으로 오해하게 되고, 건수만 알려주면
 *   어디를 고쳐야 할지 찾을 수 없어 손을 못 댄다. 그래서 목록으로 내려준다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.1
 */
@Getter
public class PriceReviewResponse {

    /** 검토 대상(건물 규모와 방문 횟수가 모두 있는 진행중 계약) */
    private final List<PriceReviewRow> rows;
    /** 검토한 계약 수 */
    private final int reviewedCount;
    /** 빠진 계약 목록(이유 포함) */
    private final List<PriceReviewSkipped> skipped;
    /** 건물 규모 미입력으로 제외된 계약 수 */
    private final int skippedNoBuilding;
    /** 방문 횟수를 확인할 수 없어 제외된 계약 수 */
    private final int skippedNoCycle;
    /** 현재액 합계 */
    private final long totalCurrent;
    /** 권장가 합계 */
    private final long totalRecommended;

    public PriceReviewResponse(List<PriceReviewRow> rows, List<PriceReviewSkipped> skipped) {
        this.rows = rows;
        this.reviewedCount = rows.size();
        this.skipped = skipped;
        this.skippedNoBuilding = (int) skipped.stream()
                .filter(s -> s.getReason() == PriceReviewSkipped.Reason.NO_BUILDING)
                .count();
        this.skippedNoCycle = (int) skipped.stream()
                .filter(s -> s.getReason() == PriceReviewSkipped.Reason.NO_VISITS)
                .count();
        this.totalCurrent = rows.stream().mapToLong(PriceReviewRow::getCurrentAmount).sum();
        this.totalRecommended = rows.stream().mapToLong(PriceReviewRow::getRecommendedAmount).sum();
    }
}
