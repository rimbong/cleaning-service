package com.boot.cleanhub.biz.supply.domain;

/**
 * <pre>
 *   약품/소모품 입출고 구분.
 *   - IN     : 입고(구매해서 창고에 넣음)      — 재고 증가
 *   - OUT    : 사용(현장에서 씀)              — 재고 감소
 *   - ADJUST : 실사 조정(창고를 세어보니 다름) — 실사 수량과 장부 수량의 차이만큼 증감
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
public enum SupplyTxType {

    IN("입고"),
    OUT("사용"),
    ADJUST("조정");

    private final String label;

    SupplyTxType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
