package com.boot.cleanhub.biz.supply.repository;

/**
 * <pre>
 *   품목별 현재 재고 조회 결과(Spring Data 프로젝션).
 *   재고는 저장된 값이 아니라 입출고 이력 합계라서 별도 컬럼이 아닌 집계 결과로 받는다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
public interface SupplyStockView {

    /** 품목 id */
    Long getItemId();

    /** 현재 재고(입출고 증감 합계) */
    Long getQuantity();
}
