package com.boot.cleanhub.biz.supply.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
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
 *   약품/소모품 품목 마스터 — 락스·왁스·세제 등 창고에 두고 쓰는 자재의 정의.
 *
 *   현재 재고 수량은 이 테이블에 두지 않는다. 입출고 이력(SupplyTransaction)의 합계로 도출한다.
 *   수량을 컬럼에 두고 직접 갱신하면 숫자가 틀어졌을 때 원인을 추적할 수 없다.
 *
 *   ※ 스키마는 Flyway(V21)가 소스이며, 엔티티와 일치해야 한다(ddl-auto=validate).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Entity
@Table(name = "supply_item")
@Getter
@Setter
public class SupplyItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 품목명(예: 락스) */
    @Column(nullable = false, length = 100)
    private String name;

    /** 규격(예: 20L 말통) */
    @Column(length = 50)
    private String spec;

    /** 단위(예: 통, 개, 박스) */
    @Column(nullable = false, length = 20)
    private String unit;

    /** 최근 구매 단가(원) — 선택 */
    @Column(name = "unit_price")
    private Long unitPrice;

    /** 안전재고 — 현재 재고가 이 값 아래로 떨어지면 목록에서 경고 표시 */
    @Column(name = "safety_qty", nullable = false)
    private Integer safetyQty;

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
