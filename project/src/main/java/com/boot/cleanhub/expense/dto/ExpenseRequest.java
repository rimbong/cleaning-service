package com.boot.cleanhub.expense.dto;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import com.boot.cleanhub.expense.domain.ExpenseCategory;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   지출 등록/수정 요청.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Getter
@Setter
public class ExpenseRequest {

    @NotNull(message = "분류는 필수입니다.")
    private ExpenseCategory category;

    @Size(max = 100)
    private String vendorName;

    @Size(max = 20)
    private String businessNumber;

    @NotNull(message = "금액은 필수입니다.")
    @PositiveOrZero(message = "금액은 0 이상이어야 합니다.")
    private Long amount;

    @NotNull(message = "지출일은 필수입니다.")
    private LocalDate expenseDate;

    @Size(max = 255)
    private String memo;
}
