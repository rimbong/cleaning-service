package com.boot.cleanhub.biz.supply.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.boot.cleanhub.biz.supply.domain.SupplyTransaction;
import com.boot.cleanhub.biz.supply.domain.SupplyTxType;

import lombok.Getter;

/**
 * <pre>
 *   약품/소모품 입출고 이력 응답.
 *   quantity 는 부호 있는 증감이다(입고 +, 사용 -). 화면에서 부호 그대로 보여주면
 *   재고가 어디서 줄고 늘었는지 훑기 쉽다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Getter
public class SupplyTransactionResponse {

    private final Long id;
    private final Long itemId;
    private final String itemName;
    private final SupplyTxType txType;
    private final String txTypeLabel;
    private final Integer quantity;
    private final LocalDate txDate;
    private final String memo;
    private final LocalDateTime createdAt;

    private SupplyTransactionResponse(SupplyTransaction t) {
        this.id = t.getId();
        this.itemId = t.getItem() != null ? t.getItem().getId() : null;
        this.itemName = t.getItem() != null ? t.getItem().getName() : null;
        this.txType = t.getTxType();
        this.txTypeLabel = t.getTxType() != null ? t.getTxType().getLabel() : null;
        this.quantity = t.getQuantity();
        this.txDate = t.getTxDate();
        this.memo = t.getMemo();
        this.createdAt = t.getCreatedAt();
    }

    public static SupplyTransactionResponse from(SupplyTransaction transaction) {
        return new SupplyTransactionResponse(transaction);
    }
}
