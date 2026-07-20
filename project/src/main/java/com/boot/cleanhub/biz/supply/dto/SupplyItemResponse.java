package com.boot.cleanhub.biz.supply.dto;

import java.time.LocalDateTime;

import com.boot.cleanhub.biz.supply.domain.PhType;
import com.boot.cleanhub.biz.supply.domain.SupplyItem;

import lombok.Getter;

/**
 * <pre>
 *   약품/소모품 품목 응답.
 *   currentQuantity 는 엔티티에 저장된 값이 아니라 입출고 이력 합계로 계산해 넘겨받는다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Getter
public class SupplyItemResponse {

    private final Long id;
    private final String name;
    private final String spec;
    /** pH 구분 — 미분류면 null */
    private final PhType phType;
    private final String phTypeLabel;
    /** 취급 시 주의·혼합 금지 사항(미분류면 null) */
    private final String phMixWarning;
    private final String unit;
    private final Long unitPrice;
    private final Integer safetyQty;
    private final String memo;
    /** 현재 재고(입출고 증감 합계) */
    private final int currentQuantity;
    /** 안전재고 미만 여부 — 목록에서 경고 표시용 */
    private final boolean belowSafety;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private SupplyItemResponse(SupplyItem item, int currentQuantity) {
        this.id = item.getId();
        this.name = item.getName();
        this.spec = item.getSpec();
        this.phType = item.getPhType();
        this.phTypeLabel = item.getPhType() != null ? item.getPhType().getLabel() : null;
        this.phMixWarning = item.getPhType() != null ? item.getPhType().getMixWarning() : null;
        this.unit = item.getUnit();
        this.unitPrice = item.getUnitPrice();
        this.safetyQty = item.getSafetyQty();
        this.memo = item.getMemo();
        this.currentQuantity = currentQuantity;
        this.belowSafety = item.getSafetyQty() != null && currentQuantity < item.getSafetyQty();
        this.createdAt = item.getCreatedAt();
        this.updatedAt = item.getUpdatedAt();
    }

    public static SupplyItemResponse from(SupplyItem item, int currentQuantity) {
        return new SupplyItemResponse(item, currentQuantity);
    }
}
