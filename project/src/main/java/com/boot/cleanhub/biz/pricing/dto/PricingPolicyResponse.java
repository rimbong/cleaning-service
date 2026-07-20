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
    /**
     * 저장된 정책인지. false 면 아직 저장 전이라 화면에 채워 보여주는 기본값이다.
     * 이 상태에서는 권장가 계산이 거부되므로 화면이 저장을 유도해야 한다.
     */
    private final boolean saved;

    private PricingPolicyResponse(PricingPolicy p, boolean saved) {
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
        this.saved = saved;
    }

    /**
     * @param policy 단가 정책
     * @param saved  저장된 값이면 true, 화면에 보여줄 기본값이면 false
     */
    public static PricingPolicyResponse from(PricingPolicy policy, boolean saved) {
        return new PricingPolicyResponse(policy, saved);
    }
}
