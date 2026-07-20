package com.boot.cleanhub.biz.contract.dto;

import java.time.LocalDate;
import java.util.List;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;

import com.boot.cleanhub.biz.contract.domain.CleaningCycle;
import com.boot.cleanhub.biz.contract.domain.ContractStatus;
import com.boot.cleanhub.biz.contract.domain.VatType;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   계약 등록/수정 요청 바디.
 *   화면(관리자)에서 입력한 값을 받는다. 검증은 @Valid 로 컨트롤러에서 수행.
 *   status 가 비어 있으면 서비스에서 ACTIVE(진행 중)로 기본 설정한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.0
 */
@Getter
@Setter
public class ContractRequest {

    /** 대상 거래처 ID(필수) */
    @NotNull(message = "거래처는 필수입니다.")
    private Long clientId;

    /** 계약명(필수) */
    @NotBlank(message = "계약명은 필수입니다.")
    @Size(max = 100, message = "계약명은 100자 이하로 입력하세요.")
    private String title;

    /** 월 청구금액(원, 필수) */
    @NotNull(message = "월 청구금액은 필수입니다.")
    @PositiveOrZero(message = "월 청구금액은 0 이상이어야 합니다.")
    private Long monthlyFee;

    /** 청구일(매월 N일, 1~31) */
    @Min(value = 1, message = "청구일은 1~31 사이여야 합니다.")
    @Max(value = 31, message = "청구일은 1~31 사이여야 합니다.")
    private Integer billingDay;

    /** 계약 시작일(필수) */
    @NotNull(message = "계약 시작일은 필수입니다.")
    private LocalDate startDate;

    /** 계약 종료일(무기한이면 비움) */
    private LocalDate endDate;

    /** 계약 상태(비우면 ACTIVE) */
    private ContractStatus status;

    /** 메모 */
    private String memo;

    /** 계약서 원본 보관 위치/비고 */
    @Size(max = 255, message = "보관 위치는 255자 이하로 입력하세요.")
    private String documentLocation;

    /** 수금 방법/계좌(선택) */
    @Size(max = 30)
    private String paymentMethod;

    /** 출입문 비밀번호(선택) */
    @Size(max = 50)
    private String doorCode;

    /** 청소 요일(다중, 선택) — 요일 코드 목록 예: ["MON","WED","FRI"] */
    private List<String> cleaningWeekdays;

    /** 청소 주기(비우면 매주 WEEKLY) */
    private CleaningCycle cleaningCycle;

    /**
     * 월 방문 횟수 — 권장가 산정용. 비우면 요일·주기로 환산해서 쓴다.
     * 요일·주기로 표현 못 하는 패턴(월 3회 등)은 여기에 직접 넣는다.
     */
    @Min(value = 1, message = "월 방문 횟수는 1 이상이어야 합니다.")
    @Max(value = 31, message = "월 방문 횟수가 너무 큽니다. 값을 확인하세요.")
    private Integer visitsPerMonth;

    /** 부가세 기준(비우면 부가세 별도 EXCLUSIVE) */
    private VatType vatType;

    /** 초도(최초 1회) 청소비(원, 선택) */
    @PositiveOrZero(message = "초도청소비는 0 이상이어야 합니다.")
    private Long initialFee;

    /** 청소 범위(선택) — 예: "지하1층~지상4층 건물내부" */
    @Size(max = 255, message = "청소 범위는 255자 이하로 입력하세요.")
    private String cleaningScope;

    /** 기본 서비스 항목(선택) — 예: "현관,계단,화장실" */
    @Size(max = 255, message = "서비스 항목은 255자 이하로 입력하세요.")
    private String serviceItems;

    /** 추가 서비스 항목(선택) — 기본 서비스 외에 따로 합의한 작업 */
    @Size(max = 255, message = "추가 서비스는 255자 이하로 입력하세요.")
    private String extraServices;

    /** 계약서에 적을 추가사항(선택) — 내부 메모와 달리 계약서에 인쇄된다 */
    @Size(max = 255, message = "추가사항은 255자 이하로 입력하세요.")
    private String extraNotes;

    /**
     * 종료일-시작일 교차검증 — 둘 다 있을 때만, 종료일이 시작일과 같거나 이후여야 한다.
     * (무기한 계약은 종료일을 비우므로 통과) @AssertTrue 는 isXxx() getter 형태로 인식된다.
     *
     * @return 유효하면 true
     */
    @AssertTrue(message = "계약 종료일은 시작일과 같거나 이후여야 합니다.")
    public boolean isEndDateValid() {
        return endDate == null || startDate == null || !endDate.isBefore(startDate);
    }
}
