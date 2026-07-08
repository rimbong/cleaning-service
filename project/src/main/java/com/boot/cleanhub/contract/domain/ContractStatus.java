package com.boot.cleanhub.contract.domain;

/**
 * <pre>
 *   계약 상태.
 *   - ACTIVE    : 진행 중(현재 유효한 정기 계약)
 *   - ENDED     : 종료(계약 만료·해지)
 *   - SUSPENDED : 중지(일시 중단 — 추후 재개 가능)
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.0
 */
public enum ContractStatus {

    /** 진행 중 */
    ACTIVE("진행 중"),

    /** 종료 */
    ENDED("종료"),

    /** 중지 */
    SUSPENDED("중지");

    private final String label;

    ContractStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
