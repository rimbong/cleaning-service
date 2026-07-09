package com.boot.cleanhub.biz.settlement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.boot.cleanhub.biz.settlement.domain.Billing;

/**
 * <pre>
 *   청구서(billing) 저장소.
 *   목록/단건은 계약·거래처·견적을 left join fetch 로 함께 로딩(대상명 표시·N+1 회피).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
public interface BillingRepository extends JpaRepository<Billing, Long> {

    /** 특정 연월의 청구 목록(계약·거래처·견적 포함) */
    @Query("select b from Billing b"
            + " left join fetch b.contract c left join fetch c.client"
            + " left join fetch b.quote q left join fetch q.client"
            + " where b.billYear = :year and b.billMonth = :month order by b.id")
    List<Billing> findByMonthWithRefs(@Param("year") int year, @Param("month") int month);

    /** 단건(계약·거래처·견적 포함) */
    @Query("select b from Billing b"
            + " left join fetch b.contract c left join fetch c.client"
            + " left join fetch b.quote q left join fetch q.client"
            + " where b.id = :id")
    Optional<Billing> findByIdWithRefs(@Param("id") Long id);

    /** 자동 생성 시 중복 방지 — 그 계약·연월 청구가 이미 있나 */
    boolean existsByContract_IdAndBillYearAndBillMonth(Long contractId, Integer billYear, Integer billMonth);

    /** 기간(연·월범위) 청구(계약·거래처·견적 포함) — 세금계산서 집계용 */
    @Query("select b from Billing b"
            + " left join fetch b.contract c left join fetch c.client"
            + " left join fetch b.quote q left join fetch q.client"
            + " where b.billYear = :year and b.billMonth between :fromMonth and :toMonth")
    List<Billing> findByPeriodWithRefs(@Param("year") int year,
            @Param("fromMonth") int fromMonth, @Param("toMonth") int toMonth);
}
