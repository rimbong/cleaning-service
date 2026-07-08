package com.boot.cleanhub.contract.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import com.boot.cleanhub.client.domain.Client;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   계약 — 거래처(건물)의 정기 청소 월정액 계약.
 *   한 거래처에 여러 계약을 둘 수 있다(재계약 이력 등). 거래처(Client)를 참조한다.
 *
 *   ※ 스키마는 Flyway(V4__create_contract.sql)가 소스이며, 이 엔티티와 일치해야 한다(ddl-auto=validate).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.0
 */
@Entity
@Table(name = "contract")
@Getter
@Setter
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대상 거래처(건물) — 다대일. 목록/상세에서만 지연 로딩(fetch join 사용) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** 계약명(예: 2026년 정기 계단청소) */
    @Column(nullable = false, length = 100)
    private String title;

    /** 월 청구금액(원) */
    @Column(name = "monthly_fee", nullable = false)
    private Long monthlyFee;

    /** 청구일(매월 N일, 1~31). 미정이면 null */
    @Column(name = "billing_day")
    private Integer billingDay;

    /** 계약 시작일 */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** 계약 종료일(무기한이면 null) */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** 계약 상태(진행/종료/중지) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContractStatus status;

    /** 메모(자유 기재) */
    @Column(columnDefinition = "TEXT")
    private String memo;

    /** 등록 시각 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 수정 시각 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 최초 저장 직전: 생성/수정 시각 기록 */
    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 수정 저장 직전: 수정 시각 갱신 */
    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
