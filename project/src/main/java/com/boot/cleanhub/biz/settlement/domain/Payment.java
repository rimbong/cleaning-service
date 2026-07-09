package com.boot.cleanhub.biz.settlement.domain;

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

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   입금(payment) — 청구(billing)에 대한 실제 입금 1건. 보통 청구당 1건이나, 분할 입금이면 여러 건.
 *   수금액 = 그 청구의 payment 합.
 *
 *   ※ 스키마는 Flyway(V11)가 소스이며, 엔티티와 일치해야 한다(ddl-auto=validate).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Entity
@Table(name = "payment")
@Getter
@Setter
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 청구 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_id", nullable = false)
    private Billing billing;

    /** 입금액 */
    @Column(nullable = false)
    private Long amount;

    /** 입금일 */
    @Column(name = "paid_date", nullable = false)
    private LocalDate paidDate;

    /** 수금 방법(현금·신한·국민 등) */
    @Column(length = 30)
    private String method;

    /** 메모 */
    @Column(length = 255)
    private String memo;

    /** 등록 시각 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
