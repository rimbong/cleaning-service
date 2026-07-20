package com.boot.cleanhub.biz.supply.dto;

import java.time.LocalDate;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import com.boot.cleanhub.biz.supply.domain.SupplyTxType;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   약품/소모품 입출고 등록 요청. 대상 품목은 경로변수로 받으므로 본문에 없다.
 *
 *   quantity 는 <b>화면에 적는 양수</b>다(부호는 서버가 구분에 따라 붙인다).
 *   - IN     : 들여온 수량
 *   - OUT    : 쓴 수량
 *   - ADJUST : 창고를 세어본 <b>실제 수량</b>(차이만큼만 이력에 남는다)
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.20
 * @version 1.0
 */
@Getter
@Setter
public class SupplyTransactionRequest {

    @NotNull(message = "구분은 필수입니다.")
    private SupplyTxType txType;

    @NotNull(message = "수량은 필수입니다.")
    @PositiveOrZero(message = "수량은 0 이상이어야 합니다.")
    private Integer quantity;

    @NotNull(message = "일자는 필수입니다.")
    private LocalDate txDate;

    @Size(max = 255)
    private String memo;
}
