package com.boot.cleanhub.biz.pricing.dto;

import java.math.BigDecimal;
import java.util.List;

import com.boot.cleanhub.biz.pricing.domain.VisitFrequency;

import lombok.Getter;

/**
 * <pre>
 *   적정가 재산정 한 줄 — 계약 하나의 현재 월정액과 권장가를 나란히 놓는다.
 *
 *   거래처 단가가 몇 년 전에 정해진 채 그대로인 경우가 많아, 지금 기준으로 얼마가
 *   적정인지 한눈에 보려는 화면이다. 금액을 자동으로 바꾸지는 않는다(인상은 협상이다).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Getter
public class PriceReviewRow {

    private final Long contractId;
    private final Long clientId;
    private final String clientName;
    private final String contractTitle;

    /** 건물 규모 요약(예: 5층 10세대) */
    private final String buildingSummary;
    /** 산정에 쓴 월 방문 횟수 */
    private final int visitsPerMonth;
    /** 표시용 주기 문구 */
    private final String cycleLabel;

    /** 현재 계약 월정액 */
    private final long currentAmount;
    /** 지금 기준 권장가 */
    private final long recommendedAmount;
    /** 권장가 - 현재액(양수면 올려야 할 금액) */
    private final long difference;
    /** 인상률(%). 소수 첫째 자리까지 */
    private final double differenceRate;

    /** 주기 계수 적용 전 소계 */
    private final long subtotal;
    /** 적용된 주기 계수 */
    private final BigDecimal coefficient;
    /**
     * 산출 근거 — 항목별 금액.
     * 거래처에 인상을 요청할 때 "왜 이 금액인지"를 그대로 읽어줄 수 있어야 해서 같이 내려준다.
     */
    private final List<PriceEstimateLine> breakdown;

    /**
     * @param weeklyRepeated 매주 반복되는 계약인지 — 주기 표시를 "주 N회" 로 할지 판단한다.
     *                       매월 첫째주·넷째주 같은 계약은 월 2회지만 격주가 아니므로 구분이 필요하다.
     */
    public PriceReviewRow(Long contractId, Long clientId, String clientName, String contractTitle,
            String buildingSummary, long currentAmount, PriceEstimateResponse estimate,
            boolean weeklyRepeated) {
        this.contractId = contractId;
        this.clientId = clientId;
        this.clientName = clientName;
        this.contractTitle = contractTitle;
        this.buildingSummary = buildingSummary;
        this.visitsPerMonth = estimate.getVisitsPerMonth();
        this.cycleLabel = VisitFrequency.label(estimate.getVisitsPerMonth(), weeklyRepeated);
        this.currentAmount = currentAmount;
        this.recommendedAmount = estimate.getRecommendedAmount();
        this.subtotal = estimate.getSubtotal();
        this.coefficient = estimate.getCoefficient();
        this.breakdown = estimate.getBreakdown();
        this.difference = estimate.getRecommendedAmount() - currentAmount;
        // 현재액이 0 이면 인상률을 낼 수 없다(0 으로 나눔). 그때는 0 으로 두고 화면에서 금액만 본다.
        this.differenceRate = currentAmount > 0
                ? Math.round(((double) this.difference / currentAmount) * 1000d) / 10d
                : 0d;
    }
}
