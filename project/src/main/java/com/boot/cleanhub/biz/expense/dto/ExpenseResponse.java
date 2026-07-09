package com.boot.cleanhub.biz.expense.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.boot.cleanhub.biz.expense.domain.Expense;
import com.boot.cleanhub.biz.expense.domain.ExpenseCategory;

import lombok.Getter;

/**
 * <pre>
 *   지출 응답.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
public class ExpenseResponse {

    private final Long id;
    private final ExpenseCategory category;
    private final String categoryLabel;
    private final String vendorName;
    private final String businessNumber;
    private final Long amount;
    private final LocalDate expenseDate;
    private final String memo;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private ExpenseResponse(Expense e) {
        this.id = e.getId();
        this.category = e.getCategory();
        this.categoryLabel = e.getCategory() != null ? e.getCategory().getLabel() : null;
        this.vendorName = e.getVendorName();
        this.businessNumber = e.getBusinessNumber();
        this.amount = e.getAmount();
        this.expenseDate = e.getExpenseDate();
        this.memo = e.getMemo();
        this.createdAt = e.getCreatedAt();
        this.updatedAt = e.getUpdatedAt();
    }

    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(expense);
    }
}
