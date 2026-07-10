package com.boot.cleanhub.biz.settlement.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.boot.cleanhub.biz.settlement.domain.TaxInvoice;

/**
 * <pre>
 *   세금계산서 발행 기록 저장소.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
public interface TaxInvoiceRepository extends JpaRepository<TaxInvoice, Long> {

    /** 발행 기록 목록(거래처 포함, 최신순) */
    @Query("select t from TaxInvoice t join fetch t.client order by t.id desc")
    List<TaxInvoice> findAllWithClient();

    /** 단건(거래처 포함) */
    @Query("select t from TaxInvoice t join fetch t.client where t.id = :id")
    Optional<TaxInvoice> findByIdWithClient(Long id);

    /** 중복 발행 방지 — 그 거래처·기간(시작연월~종료연월)·기준으로 이미 발행 기록이 있나 */
    boolean existsByClient_IdAndFromYearAndFromMonthAndToYearAndToMonthAndBasis(
            Long clientId, Integer fromYear, Integer fromMonth, Integer toYear, Integer toMonth, String basis);
}
