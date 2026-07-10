package com.boot.cleanhub.biz.contract.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import com.boot.cleanhub.biz.client.domain.Client;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   계약 — 거래처(건물)의 정기 청소 월정액 계약.
 *   한 거래처에 여러 계약을 둘 수 있다(재계약 이력 등). 거래처(Client)를 참조한다.
 *
 *   ※ 스키마는 Flyway(V4__create_contract.sql)가 소스이며, 이 엔티티와 일치해야 한다(ddl-auto=validate).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.0
 */
@Entity
@Table(name = "contract")
@Getter
@Setter
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대상 거래처(건물) — 다대일. 목록/상세에서만 지연 로딩(fetch join 사용) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    /** 계약명(예: 2026년 정기 계단청소) */
    @Column(nullable = false, length = 100)
    private String title;

    /** 월 청구금액(원) */
    @Column(name = "monthly_fee", nullable = false)
    private Long monthlyFee;

    /** 청구일(매월 N일, 1~31). 미정이면 null */
    @Column(name = "billing_day")
    private Integer billingDay;

    /** 계약 시작일 */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** 계약 종료일(무기한이면 null) */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** 계약 상태(진행/종료/중지) */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ContractStatus status;

    /** 메모(자유 기재) */
    @Column(columnDefinition = "TEXT")
    private String memo;

    /** 계약서 원본(종이 등) 보관 위치/비고 — 예: "캐비닛 A-3", "구글드라이브 링크" */
    @Column(name = "document_location", length = 255)
    private String documentLocation;

    /** 수금 방법/계좌(선택) — 예: 현금, 신한, 국민 */
    @Column(name = "payment_method", length = 30)
    private String paymentMethod;

    /** 출입문 비밀번호(운영 메모, 선택) */
    @Column(name = "door_code", length = 50)
    private String doorCode;

    /** 청소 요일(다중) — 요일 코드 쉼표구분 저장, 예: "MON,WED,FRI". 정기 청소 실행 요일. */
    @Column(name = "cleaning_weekdays", length = 30)
    private String cleaningWeekdays;

    /** 청소 주기(매주/격주/매월) */
    @Enumerated(EnumType.STRING)
    @Column(name = "cleaning_cycle", length = 20)
    private CleaningCycle cleaningCycle;

    /** 부가세 기준(별도/포함/면세) — 세금계산서 공급가액·세액 계산에 사용 */
    @Enumerated(EnumType.STRING)
    @Column(name = "vat_type", length = 20)
    private VatType vatType;

    /** 초도(최초 1회) 청소비(원, 선택) — 계약서의 "초도청소비" */
    @Column(name = "initial_fee")
    private Long initialFee;

    /** 청소 범위(선택) — 예: "지하1층~지상4층 건물내부" */
    @Column(name = "cleaning_scope", length = 255)
    private String cleaningScope;

    /** 기본 서비스 항목(선택) — 예: "현관,계단창틀,계단손잡이,우편함,화장실" */
    @Column(name = "service_items", length = 255)
    private String serviceItems;

    /** 등록 시각 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 수정 시각 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 최초 저장 직전: 생성/수정 시각 기록 */
    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 수정 저장 직전: 수정 시각 갱신 */
    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
