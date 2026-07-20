package com.boot.cleanhub.biz.pricing.dto;

import java.math.BigDecimal;
import java.util.List;

import com.boot.cleanhub.biz.pricing.domain.VisitFrequency;

import lombok.Getter;

/**
 * <pre>
 *   권장가 산정 결과.
 *
 *   이 금액은 <b>참고용 권장가</b>이지 확정가가 아니다. 실제 계약은 흥정·경쟁 상황에 따라
 *   달라지므로 화면에서도 실제 금액 입력란을 따로 두고, 이 값은 '적용' 버튼으로만 옮긴다.
 *
 *   breakdown 은 "왜 이 금액인지"를 고객에게 설명하기 위한 산출 근거다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Getter
public class PriceEstimateResponse {

    /** 권장 월 청소비(부가세 별도, 반올림 적용) */
    private final long recommendedAmount;
    /** 주기 계수 적용 전 소계 */
    private final long subtotal;
    /** 1회 방문 환산 금액 */
    private final long perVisitAmount;
    /** 월 방문 횟수 */
    private final int visitsPerMonth;
    /** 표시용 주기 문구(예: "월 16회") — 견적 화면은 요일 정보가 없어 "월 N회"로만 적는다 */
    private final String cycleLabel;
    /** 적용된 주기 계수 */
    private final BigDecimal coefficient;
    /** 산출 근거 — 항목별 금액 */
    private final List<PriceEstimateLine> breakdown;

    public PriceEstimateResponse(long recommendedAmount, long subtotal, long perVisitAmount,
            int visitsPerMonth, BigDecimal coefficient, List<PriceEstimateLine> breakdown) {
        this.recommendedAmount = recommendedAmount;
        this.subtotal = subtotal;
        this.perVisitAmount = perVisitAmount;
        this.visitsPerMonth = visitsPerMonth;
        this.cycleLabel = VisitFrequency.label(visitsPerMonth);
        this.coefficient = coefficient;
        this.breakdown = breakdown;
    }
}
