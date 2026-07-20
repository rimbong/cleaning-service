package com.boot.cleanhub.biz.pricing.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.boot.cleanhub.biz.pricing.domain.PricingPolicy;

import lombok.Getter;

/**
 * <pre>
 *   단가 정책 응답.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Getter
public class PricingPolicyResponse {

    private final Long id;
    private final Long baseFee;
    private final Long perFloor;
    private final Long perHousehold;
    private final Long perToilet;
    private final Long elevatorFee;
    private final BigDecimal coefMonthly1;
    private final BigDecimal coefMonthly2;
    private final BigDecimal coefMonthly3;
    private final BigDecimal coefWeekly1;
    private final BigDecimal coefWeekly2;
    private final BigDecimal coefWeekly3;
    private final Long roundingUnit;
    private final String memo;
    private final LocalDateTime updatedAt;

    private PricingPolicyResponse(PricingPolicy p) {
        this.id = p.getId();
        this.baseFee = p.getBaseFee();
        this.perFloor = p.getPerFloor();
        this.perHousehold = p.getPerHousehold();
        this.perToilet = p.getPerToilet();
        this.elevatorFee = p.getElevatorFee();
        this.coefMonthly1 = p.getCoefMonthly1();
        this.coefMonthly2 = p.getCoefMonthly2();
        this.coefMonthly3 = p.getCoefMonthly3();
        this.coefWeekly1 = p.getCoefWeekly1();
        this.coefWeekly2 = p.getCoefWeekly2();
        this.coefWeekly3 = p.getCoefWeekly3();
        this.roundingUnit = p.getRoundingUnit();
        this.memo = p.getMemo();
        this.updatedAt = p.getUpdatedAt();
    }

    public static PricingPolicyResponse from(PricingPolicy policy) {
        return new PricingPolicyResponse(policy);
    }
}
