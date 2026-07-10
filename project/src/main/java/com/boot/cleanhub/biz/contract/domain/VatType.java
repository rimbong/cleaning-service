package com.boot.cleanhub.biz.contract.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   부가세 기준 — 계약의 "월 청구금액"이 부가세와 어떤 관계인지.
 *   세금계산서 집계에서 공급가액·세액을 어떻게 나눌지 결정한다(거래처마다 다를 수 있어 계약별로 둔다).
 *
 *     EXCLUSIVE(부가세 별도): 청구액 = 공급가액(net).  세액 = 청구액 * 10%,  합계 = 청구액 * 1.1
 *     INCLUSIVE(부가세 포함): 청구액 = 합계(gross).    공급가액 = 청구액 / 1.1,  세액 = 청구액 - 공급가액
 *     FREE(면세)          : 세액 없음.               공급가액 = 청구액,      세액 = 0
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.10
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor
public enum VatType {

    EXCLUSIVE("부가세 별도"),
    INCLUSIVE("부가세 포함"),
    FREE("면세");

    private final String label;
}
