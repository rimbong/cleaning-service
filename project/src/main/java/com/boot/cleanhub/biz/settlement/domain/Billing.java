package com.boot.cleanhub.biz.settlement.domain;

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
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import com.boot.cleanhub.biz.contract.domain.Contract;
import com.boot.cleanhub.biz.quote.domain.Quote;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   청구서(billing) — 정산의 본체. 계약(월정액) 또는 견적(일회성)의 특정 연월 청구 1건.
 *   실제 입금은 payment(청구 1:N)에 기록한다. 수금액=입금 합, 상태(미수/부분/완납)는 파생 계산.
 *
 *   ※ contract 와 quote 는 정확히 하나만 설정(계약 청구 vs 견적 청구).
 *   ※ 스키마는 Flyway(V11)가 소스이며, 엔티티와 일치해야 한다(ddl-auto=validate).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Entity
@Table(name = "billing")
@Getter
@Setter
public class Billing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 계약 청구(월정액) — 견적 청구면 null */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private Contract contract;

    /** 견적 청구(일회성) — 계약 청구면 null */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id")
    private Quote quote;

    /** 청구 연 */
    @Column(name = "bill_year", nullable = false)
    private Integer billYear;

    /** 청구 월(1~12) */
    @Column(name = "bill_month", nullable = false)
    private Integer billMonth;

    /** 청구액(생성 시 monthly_fee/quote.amount 를 복사; 이후 편집 가능) */
    @Column(nullable = false)
    private Long amount;

    /** 메모(신규/해약/유예 등 상태 메모) */
    @Column(columnDefinition = "TEXT")
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
