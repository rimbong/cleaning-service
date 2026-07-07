package com.boot.cleanhub.client.domain;

/**
 * <pre>
 *   거래처(건물)의 청소 종류.
 *   - REGULAR : 정기 청소(건물 계단 청소 등 월정액 계약)
 *   - SPECIAL : 특수 청소(입주청소·물탱크청소 등 일회성)
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.07
 * @version 1.0
 */
public enum CleaningType {

    /** 정기 청소(월정액 계약) */
    REGULAR("정기 청소"),

    /** 특수 청소(일회성) */
    SPECIAL("특수 청소");

    private final String label;

    CleaningType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
