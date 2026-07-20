package com.boot.cleanhub.biz.pricing.dto;

import lombok.Getter;

/**
 * <pre>
 *   권장가 산출 근거 한 줄 — "층 5개 x 6,000 = 30,000원" 같은 항목.
 *   고객에게 금액 근거를 설명할 때 그대로 읽어주면 된다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Getter
public class PriceEstimateLine {

    /** 항목명(예: 층당) */
    private final String label;
    /** 계산식 설명(예: 5개 x 6,000원) — 수량이 없는 항목은 빈 값 */
    private final String detail;
    /** 금액(원) */
    private final long amount;

    public PriceEstimateLine(String label, String detail, long amount) {
        this.label = label;
        this.detail = detail;
        this.amount = amount;
    }
}
