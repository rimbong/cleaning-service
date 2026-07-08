package com.boot.cleanhub.quote.dto;

import java.time.LocalDate;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import com.boot.cleanhub.quote.domain.QuoteStatus;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   견적 등록/수정 요청 바디.
 *   화면(관리자)에서 입력한 값을 받는다. 검증은 @Valid 로 컨트롤러에서 수행.
 *   거래처(clientId) 연결은 선택이며, 미연결이면 고객명(customerName)을 요구한다(고객 식별 최소 보장).
 *   status 가 비어 있으면 서비스에서 PENDING(대기)으로 기본 설정한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.0
 */
@Getter
@Setter
public class QuoteRequest {

    /** 대상 거래처 ID(선택 — 신규 일회성 고객이면 비움) */
    private Long clientId;

    /** 고객명 */
    @Size(max = 50, message = "고객명은 50자 이하로 입력하세요.")
    private String customerName;

    /** 고객 연락처 */
    @Size(max = 30)
    private String customerPhone;

    /** 현장 주소 */
    @Size(max = 255)
    private String address;

    /** 서비스 내용(필수, 예: 입주청소) */
    @NotBlank(message = "서비스 내용은 필수입니다.")
    @Size(max = 100, message = "서비스 내용은 100자 이하로 입력하세요.")
    private String title;

    /** 견적 금액(원, 필수) */
    @NotNull(message = "견적 금액은 필수입니다.")
    @PositiveOrZero(message = "견적 금액은 0 이상이어야 합니다.")
    private Long amount;

    /** 견적일(필수) */
    @NotNull(message = "견적일은 필수입니다.")
    private LocalDate quoteDate;

    /** 유효기간(없으면 비움) */
    private LocalDate validUntil;

    /** 견적 상태(비우면 PENDING) */
    private QuoteStatus status;

    /** 메모 */
    private String memo;

    /**
     * 고객 식별 최소 보장 — 거래처 연결 또는 고객명 중 하나는 있어야 한다.
     *
     * @return 유효하면 true
     */
    @AssertTrue(message = "거래처를 선택하거나 고객명을 입력하세요.")
    public boolean isCustomerProvided() {
        return clientId != null || (customerName != null && !customerName.trim().isEmpty());
    }

    /**
     * 유효기간-견적일 교차검증 — 둘 다 있을 때만, 유효기간이 견적일과 같거나 이후여야 한다.
     *
     * @return 유효하면 true
     */
    @AssertTrue(message = "유효기간은 견적일과 같거나 이후여야 합니다.")
    public boolean isValidUntilValid() {
        return validUntil == null || quoteDate == null || !validUntil.isBefore(quoteDate);
    }
}
