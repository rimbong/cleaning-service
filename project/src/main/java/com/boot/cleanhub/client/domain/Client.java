package com.boot.cleanhub.client.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   거래처(건물) — 청소 서비스를 제공하는 대상 건물/고객.
 *   계약·견적·정산 등 다른 도메인이 이 거래처를 참조한다(가장 기반이 되는 도메인).
 *
 *   ※ 스키마는 Flyway(V3__create_client.sql)가 소스이며, 이 엔티티와 일치해야 한다(ddl-auto=validate).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.07
 * @version 1.0
 */
@Entity
@Table(name = "client")
@Getter
@Setter
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 건물명(예: 00빌라) */
    @Column(nullable = false, length = 100)
    private String name;

    /** 주소 */
    @Column(length = 255)
    private String address;

    /** 담당자명 */
    @Column(name = "manager_name", length = 50)
    private String managerName;

    /** 담당자 연락처 */
    @Column(name = "manager_phone", length = 30)
    private String managerPhone;

    /** 청소 종류(정기/특수) */
    @Enumerated(EnumType.STRING)
    @Column(name = "cleaning_type", length = 20)
    private CleaningType cleaningType;

    /** 계약 시작일(정기 계약인 경우) */
    @Column(name = "contract_start_date")
    private LocalDate contractStartDate;

    /** 메모(자유 기재) */
    @Column(columnDefinition = "TEXT")
    private String memo;

    // ── 세금계산서/사업자 정보(선택) — 세금계산서 공급받는자란에 사용 ──

    /** 사업자번호 */
    @Column(name = "business_number", length = 20)
    private String businessNumber;

    /** 대표자/성명 */
    @Column(name = "representative_name", length = 50)
    private String representativeName;

    /** 업태 */
    @Column(name = "business_type", length = 50)
    private String businessType;

    /** 종목 */
    @Column(name = "business_item", length = 50)
    private String businessItem;

    /** 세금계산서 발행 방식 */
    @Enumerated(EnumType.STRING)
    @Column(name = "tax_invoice_type", length = 20)
    private TaxInvoiceType taxInvoiceType;

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
