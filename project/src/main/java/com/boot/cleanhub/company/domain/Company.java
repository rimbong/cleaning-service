package com.boot.cleanhub.company.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PreUpdate;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   회사(공급자) 프로필 — 세금계산서 발행 주체(운영 회사) 정보.
 *   단일 운영자라 1개 행만 유지한다(설정 성격). 세금계산서의 "공급자"란에 사용.
 *
 *   ※ 스키마는 Flyway(V8__create_company.sql)가 소스이며, 엔티티와 일치해야 한다(ddl-auto=validate).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Entity
@Table(name = "company")
@Getter
@Setter
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 등록번호(사업자번호) */
    @Column(name = "business_number", length = 20)
    private String businessNumber;

    /** 상호 */
    @Column(name = "company_name", length = 100)
    private String companyName;

    /** 대표자/성명 */
    @Column(name = "owner_name", length = 50)
    private String ownerName;

    /** 사업장 주소 */
    @Column(length = 255)
    private String address;

    /** 업태 */
    @Column(name = "business_type", length = 50)
    private String businessType;

    /** 종목 */
    @Column(name = "business_item", length = 50)
    private String businessItem;

    /** 연락처 */
    @Column(length = 30)
    private String phone;

    /** 수정 시각 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
