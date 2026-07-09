package com.boot.cleanhub.biz.expense.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.boot.cleanhub.common.dto.PageResponse;
import com.boot.cleanhub.error.BizException;
import com.boot.cleanhub.error.ErrorCode;
import com.boot.cleanhub.biz.expense.domain.Expense;
import com.boot.cleanhub.biz.expense.dto.ExpenseRequest;
import com.boot.cleanhub.biz.expense.dto.ExpenseResponse;
import com.boot.cleanhub.biz.expense.repository.ExpenseRepository;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   지출 도메인 서비스 — 등록/조회/수정/삭제.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public PageResponse<ExpenseResponse> list(String keyword, Pageable pageable) {
        Page<Expense> expenses;
        if (StringUtils.hasText(keyword)) {
            expenses = expenseRepository
                    .findByVendorNameContainingIgnoreCaseOrderByExpenseDateDescIdDesc(keyword.trim(), pageable);
        } else {
            expenses = expenseRepository.findAllByOrderByExpenseDateDescIdDesc(pageable);
        }
        return PageResponse.from(expenses.map(ExpenseResponse::from));
    }

    public ExpenseResponse get(Long id) {
        return ExpenseResponse.from(findOrThrow(id));
    }

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        Expense expense = new Expense();
        apply(expense, request);
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense expense = findOrThrow(id);
        apply(expense, request);
        expenseRepository.saveAndFlush(expense);
        return ExpenseResponse.from(expense);
    }

    @Transactional
    public void delete(Long id) {
        expenseRepository.delete(findOrThrow(id));
    }

    private void apply(Expense expense, ExpenseRequest request) {
        expense.setCategory(request.getCategory());
        expense.setVendorName(request.getVendorName());
        expense.setBusinessNumber(request.getBusinessNumber());
        expense.setAmount(request.getAmount());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setMemo(request.getMemo());
    }

    private Expense findOrThrow(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new BizException(ErrorCode.EXPENSE_NOT_FOUND));
    }
}
