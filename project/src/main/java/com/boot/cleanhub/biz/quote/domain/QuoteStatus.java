package com.boot.cleanhub.biz.quote.domain;

/**
 * <pre>
 *   견적 상태.
 *   - PENDING  : 대기(견적 제시 후 고객 응답 전)
 *   - ACCEPTED : 수락(고객이 견적을 받아들임 → 작업 진행)
 *   - REJECTED : 거절(고객이 견적을 받아들이지 않음)
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.0
 */
public enum QuoteStatus {

    /** 대기 */
    PENDING("대기"),

    /** 수락 */
    ACCEPTED("수락"),

    /** 거절 */
    REJECTED("거절");

    private final String label;

    QuoteStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
