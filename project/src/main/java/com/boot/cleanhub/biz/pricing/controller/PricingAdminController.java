package com.boot.cleanhub.biz.pricing.controller;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boot.cleanhub.biz.pricing.dto.PriceEstimateRequest;
import com.boot.cleanhub.biz.pricing.dto.PriceEstimateResponse;
import com.boot.cleanhub.biz.pricing.dto.PriceReviewResponse;
import com.boot.cleanhub.biz.pricing.dto.PricingPolicyRequest;
import com.boot.cleanhub.biz.pricing.dto.PricingPolicyResponse;
import com.boot.cleanhub.biz.pricing.service.PricingService;
import com.boot.cleanhub.common.api.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   권장가 산정 API — 관리자 전용. /api/admin/** 이므로 ROLE_ADMIN 보호.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin/pricing")
@RequiredArgsConstructor
public class PricingAdminController {

    private final PricingService pricingService;

    /** 단가 정책 조회 */
    @GetMapping("/policy")
    public ApiResponse<PricingPolicyResponse> getPolicy() {
        return ApiResponse.ok(pricingService.getPolicy());
    }

    /** 단가 정책 수정 — 최저임금 인상 등으로 단가를 조정할 때 */
    @PutMapping("/policy")
    public ApiResponse<PricingPolicyResponse> updatePolicy(@Valid @RequestBody PricingPolicyRequest request) {
        return ApiResponse.ok(pricingService.updatePolicy(request), "단가 정책이 저장되었습니다.");
    }

    /**
     * 권장가 산정 — 조회지만 입력 항목이 많아 POST 로 받는다.
     * 결과는 참고용 권장가이며 계약 금액을 바꾸지 않는다.
     */
    @PostMapping("/estimate")
    public ApiResponse<PriceEstimateResponse> estimate(@Valid @RequestBody PriceEstimateRequest request) {
        return ApiResponse.ok(pricingService.estimate(request));
    }

    /** 적정가 재산정 — 진행 중 계약의 현재 월정액 vs 지금 기준 권장가 (인상 검토용) */
    @GetMapping("/review")
    public ApiResponse<PriceReviewResponse> review() {
        return ApiResponse.ok(pricingService.review());
    }
}
