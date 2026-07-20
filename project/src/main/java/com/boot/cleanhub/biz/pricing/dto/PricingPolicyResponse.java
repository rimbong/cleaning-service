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
    private final BigDecimal coefBase;
    private final BigDecimal coefExponent;
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
        this.coefBase = p.getCoefBase();
        this.coefExponent = p.getCoefExponent();
        this.roundingUnit = p.getRoundingUnit();
        this.memo = p.getMemo();
        this.updatedAt = p.getUpdatedAt();
    }

    public static PricingPolicyResponse from(PricingPolicy policy) {
        return new PricingPolicyResponse(policy);
    }
}
