package com.boot.cleanhub.client.domain;

/**
 * <pre>
 *   거래처 세금계산서 발행 방식.
 *   - ELECTRONIC : 전자세금계산서
 *   - EMAIL      : 이메일 발송
 *   - LABOR      : 인건비 처리(세금계산서 대신)
 *   - NONE       : 발행 안 함
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
public enum TaxInvoiceType {

    /** 전자세금계산서 */
    ELECTRONIC("전자세금계산서"),

    /** 이메일 발송 */
    EMAIL("이메일 발송"),

    /** 인건비 처리 */
    LABOR("인건비 처리"),

    /** 발행 안 함 */
    NONE("발행 안 함");

    private final String label;

    TaxInvoiceType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
