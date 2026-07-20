package com.boot.cleanhub.biz.supply.dto;

import com.boot.cleanhub.biz.supply.domain.HazardousMix;

import lombok.Getter;

/**
 * <pre>
 *   보유 약품에서 성립하는 위험 조합 경고 한 건.
 *   창고 전체를 기준으로 판정한 결과다(현재 보고 있는 페이지가 아니다).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Getter
public class SupplyHazardResponse {

    private final String firstLabel;
    private final String secondLabel;
    /** 생성되는 유독 물질(예: 염소가스) */
    private final String gas;
    /** 왜 위험한지 */
    private final String detail;

    private SupplyHazardResponse(HazardousMix mix) {
        this.firstLabel = mix.getFirst().getLabel();
        this.secondLabel = mix.getSecond().getLabel();
        this.gas = mix.getGas();
        this.detail = mix.getDetail();
    }

    public static SupplyHazardResponse from(HazardousMix mix) {
        return new SupplyHazardResponse(mix);
    }
}
