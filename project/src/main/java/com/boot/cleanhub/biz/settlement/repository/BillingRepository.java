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

    /** 특정 연월에 이미 청구가 있는 계약 id 목록 — 월청구 자동생성 시 계약마다 existsBy 하는 N+1 회피 */
    @Query("select b.contract.id from Billing b"
            + " where b.billYear = :year and b.billMonth = :month and b.contract.id is not null")
    List<Long> findContractIdsWithBilling(@Param("year") int year, @Param("month") int month);

    /** 견적 1회성 청구 중복 방지 — 그 견적·연월 청구가 이미 있나 */
    boolean existsByQuote_IdAndBillYearAndBillMonth(Long quoteId, Integer billYear, Integer billMonth);

    /**
     * 기간 청구(계약·거래처·견적 포함) — 세금계산서 집계용.
     * 연·월을 하나의 키(year*100+month)로 비교해 연도 경계(예: 2025-11 ~ 2026-02) 기간도 지원한다.
     *
     * @param fromKey 시작 키 = 시작연*100 + 시작월
     * @param toKey   종료 키 = 종료연*100 + 종료월
     */
    @Query("select b from Billing b"
            + " left join fetch b.contract c left join fetch c.client"
            + " left join fetch b.quote q left join fetch q.client"
            + " where (b.billYear * 100 + b.billMonth) between :fromKey and :toKey")
    List<Billing> findByPeriodWithRefs(@Param("fromKey") int fromKey, @Param("toKey") int toKey);
}
