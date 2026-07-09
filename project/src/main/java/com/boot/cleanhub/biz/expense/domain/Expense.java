package com.boot.cleanhub.biz.expense.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   지출 — 주유(연료) 등 경비. 정산과 독립된 경비 관리 트랙(엑셀의 지출·주유 내역 반영).
 *
 *   ※ 스키마는 Flyway(V13)가 소스이며, 엔티티와 일치해야 한다(ddl-auto=validate).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Entity
@Table(name = "expense")
@Getter
@Setter
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 지출 분류(주유/기타) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseCategory category;

    /** 거래처/주유소명 */
    @Column(name = "vendor_name", length = 100)
    private String vendorName;

    /** 사업자번호(선택) */
    @Column(name = "business_number", length = 20)
    private String businessNumber;

    /** 금액(원) */
    @Column(nullable = false)
    private Long amount;

    /** 지출일 */
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    /** 메모 */
    @Column(length = 255)
    private String memo;

    /** 등록 시각 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 수정 시각 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
