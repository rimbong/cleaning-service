package com.boot.cleanhub.biz.client.dto;

import java.time.LocalDate;

import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import com.boot.cleanhub.biz.client.domain.CleaningType;
import com.boot.cleanhub.biz.client.domain.TaxInvoiceType;

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

    // ── 건물 규모(선택) — 계단청소 권장가 산정용. 실측 전이면 비워 둔다 ──
    //   상한은 0 을 하나 더 붙이는 오타로 엉뚱한 권장가가 나오는 것을 막는다.

    @PositiveOrZero(message = "층수는 0 이상이어야 합니다.")
    @Max(value = 100, message = "층수가 너무 큽니다. 값을 확인하세요.")
    private Integer floors;

    @PositiveOrZero(message = "세대수는 0 이상이어야 합니다.")
    @Max(value = 1000, message = "세대수가 너무 큽니다. 값을 확인하세요.")
    private Integer householdCount;

    @PositiveOrZero(message = "공용 화장실 수는 0 이상이어야 합니다.")
    @Max(value = 100, message = "공용 화장실 수가 너무 큽니다. 값을 확인하세요.")
    private Integer sharedToilets;

    @PositiveOrZero(message = "추가 층수는 0 이상이어야 합니다.")
    @Max(value = 20, message = "추가 층수가 너무 큽니다. 값을 확인하세요.")
    private Integer extraFloors;

    private Boolean hasElevator;

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
