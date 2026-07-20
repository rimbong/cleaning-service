package com.boot.cleanhub.biz.pricing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /** 주기 계수 공식의 기준값 — 월 1회일 때의 계수 */
    @Column(name = "coef_base", nullable = false)
    private BigDecimal coefBase;

    /**
     * 주기 계수 공식의 할인 지수.
     * 1 이면 방문 횟수에 정비례(할인 없음), 1 보다 작을수록 자주 갈 때 1회당 단가가 더 내려간다.
     */
    @Column(name = "coef_exponent", nullable = false)
    private BigDecimal coefExponent;

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
     * 월 방문 횟수에 해당하는 주기 계수를 계산한다.
     *
     * <pre>
     *   계수 = coefBase x 방문횟수 ^ coefExponent
     * </pre>
     *
     * 표(6단계)를 쓰지 않는 이유: 표에 없는 횟수(주 4회 등)를 계산할 수 없어
     * 조용히 낮은 금액이 나오기 때문이다. 공식은 어떤 횟수든 받으면서
     * 자주 갈수록 1회당 단가가 내려가는 볼륨 할인도 유지한다.
     *
     * @param visitsPerMonth 월 방문 횟수(1 이상)
     * @return 주기 계수(소수 넷째 자리)
     */
    public BigDecimal coefficientFor(int visitsPerMonth) {
        if (visitsPerMonth < 1) {
            // 0 이하가 들어오면 금액이 0 이나 음수가 된다. 조용히 넘기지 않는다.
            throw new IllegalArgumentException("월 방문 횟수는 1 이상이어야 합니다: " + visitsPerMonth);
        }
        double value = coefBase.doubleValue() * Math.pow(visitsPerMonth, coefExponent.doubleValue());
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
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
