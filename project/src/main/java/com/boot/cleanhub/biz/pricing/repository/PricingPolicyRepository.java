package com.boot.cleanhub.biz.pricing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.cleanhub.biz.pricing.domain.PricingPolicy;

/**
 * <pre>
 *   단가 정책 저장소. 단일 행(설정)이라 기본 CRUD 만 사용한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
public interface PricingPolicyRepository extends JpaRepository<PricingPolicy, Long> {
}
