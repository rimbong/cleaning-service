package com.boot.cleanhub.biz.quote.domain;

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

import com.boot.cleanhub.biz.client.domain.Client;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   견적 — 일회성 특수청소(입주청소·물탱크청소 등)의 견적.
 *   기존 거래처(건물)를 대상으로 할 수도 있고, 아직 거래처가 아닌 신규 일회성 고객일 수도 있다.
 *   그래서 거래처(client) 연결은 선택(nullable)이고, 고객 정보(customerName 등)를 직접 담는다.
 *
 *   ※ 스키마는 Flyway(V7__create_quote.sql)가 소스이며, 이 엔티티와 일치해야 한다(ddl-auto=validate).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.0
 */
@Entity
@Table(name = "quote")
@Getter
@Setter
public class Quote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대상 거래처(건물) — 기존 거래처면 연결, 신규 일회성 고객이면 null. 삭제 시 DB 에서 SET NULL */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    /** 고객명(일회성 고객 등 — 거래처 미연결 시 특히 필요) */
    @Column(name = "customer_name", length = 50)
    private String customerName;

    /** 고객 연락처 */
    @Column(name = "customer_phone", length = 30)
    private String customerPhone;

    /** 현장 주소 */
    @Column(length = 255)
    private String address;

    /** 서비스 내용(예: 입주청소, 물탱크청소) */
    @Column(nullable = false, length = 100)
    private String title;

    /** 견적 금액(원) */
    @Column(nullable = false)
    private Long amount;

    /** 견적일 */
    @Column(name = "quote_date", nullable = false)
    private LocalDate quoteDate;

    /** 유효기간(없으면 null) */
    @Column(name = "valid_until")
    private LocalDate validUntil;

    /** 견적 상태(대기/수락/거절) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuoteStatus status;

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
