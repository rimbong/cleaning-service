package com.boot.cleanhub.expense.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.cleanhub.expense.domain.Expense;

/**
 * <pre>
 *   지출 저장소. 목록은 지출일 최신순.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /** 전체(지출일 최신순) */
    List<Expense> findAllByOrderByExpenseDateDescIdDesc();

    /** 거래처/주유소명 검색(지출일 최신순) */
    List<Expense> findByVendorNameContainingIgnoreCaseOrderByExpenseDateDescIdDesc(String vendorName);
}
