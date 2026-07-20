package com.boot.cleanhub.biz.pricing.repository;

import java.util.Optional;

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

    /**
     * 단일 행을 가져온다.
     *
     * findAll() 로 받아 첫 번째를 쓰면 안 된다. ORDER BY 가 없으면 순서가 정해지지 않고,
     * PostgreSQL 은 UPDATE 후 행의 물리 위치가 바뀌어 단가를 수정한 뒤 다른 행이 선택될 수 있다.
     * (V28 이 단일 행 제약을 걸지만, 정렬을 명시해 어떤 상황에서도 같은 행이 나오게 한다)
     *
     * @return 단가 정책(없으면 empty)
     */
    Optional<PricingPolicy> findTopByOrderByIdAsc();
}
