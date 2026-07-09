package com.boot.cleanhub.expense.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.cleanhub.expense.domain.Expense;

/**
 * <pre>
 *   지출 저장소. 목록은 지출일 최신순, 페이징(Pageable)으로 조회한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.1
 */
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    /** 전체(지출일 최신순) 페이지 */
    Page<Expense> findAllByOrderByExpenseDateDescIdDesc(Pageable pageable);

    /** 거래처/주유소명 검색(지출일 최신순) 페이지 */
    Page<Expense> findByVendorNameContainingIgnoreCaseOrderByExpenseDateDescIdDesc(String vendorName, Pageable pageable);
}
