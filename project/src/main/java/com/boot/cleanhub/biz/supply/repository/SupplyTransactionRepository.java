package com.boot.cleanhub.biz.supply.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.boot.cleanhub.biz.supply.domain.SupplyTransaction;

/**
 * <pre>
 *   약품/소모품 입출고 이력 저장소.
 *   현재 재고는 저장된 컬럼이 아니라 여기 quantity(부호 있는 증감)의 합계로 구한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
public interface SupplyTransactionRepository extends JpaRepository<SupplyTransaction, Long> {

    /** 품목별 이력(최신순) 페이지 */
    Page<SupplyTransaction> findByItemIdOrderByTxDateDescIdDesc(Long itemId, Pageable pageable);

    /** 품목에 이력이 하나라도 있는지 — 품목 삭제 가능 여부 판단용 */
    boolean existsByItemId(Long itemId);

    /**
     * 여러 품목의 현재 재고를 한 번에 구한다(목록 화면에서 N+1 방지).
     * 이력이 하나도 없는 품목은 결과에 나오지 않으므로 호출 측에서 0 으로 취급한다.
     *
     * @param itemIds 품목 id 목록
     * @return 품목별 재고 합계
     */
    @Query("SELECT t.item.id AS itemId, SUM(t.quantity) AS quantity "
            + "FROM SupplyTransaction t WHERE t.item.id IN :itemIds GROUP BY t.item.id")
    List<SupplyStockView> findStockByItemIds(@Param("itemIds") Collection<Long> itemIds);

    /**
     * 단일 품목의 현재 재고.
     *
     * @param itemId 품목 id
     * @return 재고 합계(이력이 없으면 0)
     */
    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM SupplyTransaction t WHERE t.item.id = :itemId")
    Long findStockByItemId(@Param("itemId") Long itemId);
}
