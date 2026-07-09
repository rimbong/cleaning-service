package com.boot.cleanhub.biz.settlement.repository;

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
}
