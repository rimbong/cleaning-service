package com.boot.cleanhub.biz.supply.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   약품/소모품 품목 등록/수정 요청.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Getter
@Setter
public class SupplyItemRequest {

    @NotBlank(message = "품목명은 필수입니다.")
    @Size(max = 100)
    private String name;

    @Size(max = 50)
    private String spec;

    @NotBlank(message = "단위는 필수입니다.")
    @Size(max = 20)
    private String unit;

    @PositiveOrZero(message = "단가는 0 이상이어야 합니다.")
    private Long unitPrice;

    @NotNull(message = "안전재고는 필수입니다.")
    @PositiveOrZero(message = "안전재고는 0 이상이어야 합니다.")
    private Integer safetyQty;

    @Size(max = 255)
    private String memo;
}
