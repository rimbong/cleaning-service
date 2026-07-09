package com.boot.cleanhub.biz.expense.domain;

/**
 * <pre>
 *   지출 분류.
 *   - FUEL : 주유(연료)
 *   - ETC  : 기타
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
public enum ExpenseCategory {

    FUEL("주유"),
    ETC("기타");

    private final String label;

    ExpenseCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
