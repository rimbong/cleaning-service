package com.boot.cleanhub.biz.settlement.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.boot.cleanhub.biz.settlement.domain.Payment;

/**
 * <pre>
 *   입금(payment) 저장소. 청구별 입금 조회 + 입금합 집계(수금액 계산).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** 특정 청구의 입금 목록 */
    List<Payment> findByBilling_IdOrderById(Long billingId);

    /** 특정 청구의 입금 합(수금액). 없으면 0 */
    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.billing.id = :billingId")
    long sumByBillingId(@Param("billingId") Long billingId);

    /** 여러 청구의 입금 합을 한 번에 (billingId, sum) 목록으로 — 목록 화면 N+1 회피 */
    @Query("select p.billing.id, coalesce(sum(p.amount), 0) from Payment p"
            + " where p.billing.id in :billingIds group by p.billing.id")
    List<Object[]> sumGroupedByBillingIds(@Param("billingIds") List<Long> billingIds);

    /**
     * 여러 청구의 입금 합을 (billingId, sum) 로 — 단, 입금일(paidDate)이 기간 내인 입금만 합산.
     * 세금계산서 "수금 기준" 집계에서 기간 밖 입금을 제외하기 위함.
     */
    @Query("select p.billing.id, coalesce(sum(p.amount), 0) from Payment p"
            + " where p.billing.id in :billingIds and p.paidDate between :fromDate and :toDate"
            + " group by p.billing.id")
    List<Object[]> sumGroupedByBillingIdsInPeriod(@Param("billingIds") List<Long> billingIds,
            @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    /** 여러 청구의 최종 수금일(max paidDate)을 (billingId, date) 목록으로 — 연간 수금 현황용 */
    @Query("select p.billing.id, max(p.paidDate) from Payment p"
            + " where p.billing.id in :billingIds group by p.billing.id")
    List<Object[]> findLatestPaidDateByBillingIds(@Param("billingIds") List<Long> billingIds);
}
