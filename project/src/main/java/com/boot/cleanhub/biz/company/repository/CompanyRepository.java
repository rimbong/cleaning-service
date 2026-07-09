package com.boot.cleanhub.biz.company.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.boot.cleanhub.biz.company.domain.Company;

/**
 * <pre>
 *   회사(공급자) 프로필 저장소. 단일 행(설정)이라 기본 CRUD 만 사용한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
public interface CompanyRepository extends JpaRepository<Company, Long> {
}
