package com.boot.cleanhub.settlement.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import com.boot.cleanhub.client.domain.Client;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   세금계산서 발행 기록 — 특정 거래처·기간의 공급가액·세액을 집계해 "발행했다"고 기록.
 *   집계는 정산(billing/payment)에서 계산하고, 이 엔티티는 발행 이력 보관용.
 *
 *   ※ 스키마는 Flyway(V12)가 소스이며, 엔티티와 일치해야 한다(ddl-auto=validate).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Entity
@Table(name = "tax_invoice")
@Getter
@Setter
public class TaxInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 공급받는자(거래처) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** 대상 연 */
    @Column(name = "period_year", nullable = false)
    private Integer periodYear;

    /** 대상 시작 월 */
    @Column(name = "from_month", nullable = false)
    private Integer fromMonth;

    /** 대상 종료 월 */
    @Column(name = "to_month", nullable = false)
    private Integer toMonth;

    /** 공급가액 */
    @Column(name = "supply_amount", nullable = false)
    private Long supplyAmount;

    /** 세액(공급가액 * 10%) */
    @Column(name = "tax_amount", nullable = false)
    private Long taxAmount;

    /** 집계 기준(BILLED=청구 / PAID=수금) */
    @Column(nullable = false, length = 10)
    private String basis;

    /** 발행일 */
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    /** 등록 시각 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
