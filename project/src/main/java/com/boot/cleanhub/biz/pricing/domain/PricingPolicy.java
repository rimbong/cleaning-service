package com.boot.cleanhub.biz.pricing.domain;

import java.math.BigDecimal;
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
 *   계단·공용부 정기청소 단가 정책 — 회사 정보처럼 <b>한 행만</b> 쓰는 설정.
 *
 *   단가를 코드 상수로 두면 최저임금이 오를 때마다 소스 수정·재빌드·재배포가 필요해서
 *   이미 배포된 PC 에서는 손댈 수가 없다. 화면에서 고칠 수 있도록 DB 에 둔다.
 *
 *   주기 계수의 기준은 <b>월 2회(격주) = 1.0</b> 이며, 위 단가들이 그 기준에 맞춰져 있다.
 *   횟수를 늘려도 단순 배수가 아닌 이유는 한 번 갈 때 몰아 하는 동선·준비 시간의 효율 때문이다.
 *
 *   ※ 스키마는 Flyway(V22)가 소스이며, 엔티티와 일치해야 한다(ddl-auto=validate).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Entity
@Table(name = "pricing_policy")
@Getter
@Setter
public class PricingPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 기본 출동료(원/월) — 이동·준비·소모품·관리비·최소 마진 */
    @Column(name = "base_fee", nullable = false)
    private Long baseFee;

    /** 층당 단가(원) — 계단참·난간·창틀. 작업량의 핵심 */
    @Column(name = "per_floor", nullable = false)
    private Long perFloor;

    /** 세대당 단가(원) — 세대가 많을수록 늘어나는 오염·쓰레기 몫 */
    @Column(name = "per_household", nullable = false)
    private Long perHousehold;

    /** 공용 화장실 1개당 단가(원) */
    @Column(name = "per_toilet", nullable = false)
    private Long perToilet;

    /** 엘리베이터 가산(원) */
    @Column(name = "elevator_fee", nullable = false)
    private Long elevatorFee;

    /** 주기 계수 — 월 1회 */
    @Column(name = "coef_monthly_1", nullable = false)
    private BigDecimal coefMonthly1;

    /** 주기 계수 — 월 2회(격주). 기준값 1.00 */
    @Column(name = "coef_monthly_2", nullable = false)
    private BigDecimal coefMonthly2;

    /** 주기 계수 — 월 3회 */
    @Column(name = "coef_monthly_3", nullable = false)
    private BigDecimal coefMonthly3;

    /** 주기 계수 — 주 1회(월 4회) */
    @Column(name = "coef_weekly_1", nullable = false)
    private BigDecimal coefWeekly1;

    /** 주기 계수 — 주 2회 */
    @Column(name = "coef_weekly_2", nullable = false)
    private BigDecimal coefWeekly2;

    /** 주기 계수 — 주 3회 */
    @Column(name = "coef_weekly_3", nullable = false)
    private BigDecimal coefWeekly3;

    /** 최종 금액 반올림 단위(원). 1000 이면 천원 단위로 맞춘다 */
    @Column(name = "rounding_unit", nullable = false)
    private Long roundingUnit;

    /** 메모 — 단가 근거(예: 기준 최저임금) */
    @Column(length = 255)
    private String memo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 주기에 해당하는 계수를 돌려준다.
     *
     * @param cycle 산정 주기
     * @return 계수
     */
    public BigDecimal coefficientOf(PricingCycle cycle) {
        switch (cycle) {
            case MONTHLY_1:
                return coefMonthly1;
            case MONTHLY_2:
                return coefMonthly2;
            case MONTHLY_3:
                return coefMonthly3;
            case WEEKLY_1:
                return coefWeekly1;
            case WEEKLY_2:
                return coefWeekly2;
            case WEEKLY_3:
                return coefWeekly3;
            default:
                // enum 에 값을 추가하고 여기를 빠뜨리면 조용히 틀린 금액이 나온다. 그래서 예외로 알린다.
                throw new IllegalArgumentException("주기 계수가 정의되지 않았습니다: " + cycle);
        }
    }

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
