package com.boot.cleanhub.biz.supply.domain;

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

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   약품/소모품 입출고 이력 — 재고의 유일한 근거.
 *
 *   quantity 는 화면에 입력한 수량이 아니라 <b>부호 있는 증감</b>이다(입고 +, 사용 -).
 *   현재 재고 = 해당 품목 quantity 의 단순 합계. 이렇게 두면 재고가 이상할 때
 *   이력만 훑어도 어느 건에서 틀어졌는지 바로 보인다.
 *
 *   ※ 스키마는 Flyway(V21)가 소스이며, 엔티티와 일치해야 한다(ddl-auto=validate).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Entity
@Table(name = "supply_transaction")
@Getter
@Setter
public class SupplyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대상 품목 — 다대일 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private SupplyItem item;

    /** 입출고 구분(입고/사용/조정) */
    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false, length = 20)
    private SupplyTxType txType;

    /** 부호 있는 증감 수량(입고 +, 사용 -, 조정은 실사수량 - 당시재고) */
    @Column(nullable = false)
    private Integer quantity;

    /** 입고일/사용일 */
    @Column(name = "tx_date", nullable = false)
    private LocalDate txDate;

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
