package com.boot.cleanhub.client.dto;

import java.time.LocalDate;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import com.boot.cleanhub.client.domain.CleaningType;
import com.boot.cleanhub.client.domain.TaxInvoiceType;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   거래처 등록/수정 요청 바디.
 *   화면(관리자)에서 입력한 값을 받는다. 검증은 @Valid 로 컨트롤러에서 수행.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.07
 * @version 1.0
 */
@Getter
@Setter
public class ClientRequest {

    /** 건물명(필수) */
    @NotBlank(message = "건물명은 필수입니다.")
    @Size(max = 100, message = "건물명은 100자 이하로 입력하세요.")
    private String name;

    /** 주소 */
    @Size(max = 255)
    private String address;

    /** 담당자명 */
    @Size(max = 50)
    private String managerName;

    /** 담당자 연락처 */
    @Size(max = 30)
    private String managerPhone;

    /** 청소 종류(REGULAR/SPECIAL) */
    private CleaningType cleaningType;

    /** 계약 시작일 */
    private LocalDate contractStartDate;

    /** 메모 */
    private String memo;

    // ── 세금계산서/사업자 정보(선택) ──

    /** 사업자번호 */
    @Size(max = 20)
    private String businessNumber;

    /** 대표자/성명 */
    @Size(max = 50)
    private String representativeName;

    /** 업태 */
    @Size(max = 50)
    private String businessType;

    /** 종목 */
    @Size(max = 50)
    private String businessItem;

    /** 세금계산서 발행 방식 */
    private TaxInvoiceType taxInvoiceType;
}
