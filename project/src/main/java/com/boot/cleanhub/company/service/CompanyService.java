package com.boot.cleanhub.company.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boot.cleanhub.company.domain.Company;
import com.boot.cleanhub.company.dto.CompanyRequest;
import com.boot.cleanhub.company.dto.CompanyResponse;
import com.boot.cleanhub.company.repository.CompanyRepository;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   회사(공급자) 프로필 서비스 — 단일 행(설정) 조회/수정.
 *   Flyway 가 빈 1행을 시드하지만, 없을 경우에도 기본 행을 만들어 항상 1개를 보장한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private final CompanyRepository companyRepository;

    /** 회사 프로필(단일 행) 조회. 없으면 기본 행 생성. */
    public CompanyResponse get() {
        return CompanyResponse.from(getSingleton());
    }

    /** 회사 프로필 수정. */
    @Transactional
    public CompanyResponse update(CompanyRequest request) {
        Company company = getSingleton();
        company.setBusinessNumber(request.getBusinessNumber());
        company.setCompanyName(request.getCompanyName());
        company.setOwnerName(request.getOwnerName());
        company.setAddress(request.getAddress());
        company.setBusinessType(request.getBusinessType());
        company.setBusinessItem(request.getBusinessItem());
        company.setPhone(request.getPhone());
        return CompanyResponse.from(companyRepository.saveAndFlush(company));
    }

    /** 단일 행 획득(없으면 생성). */
    @Transactional
    Company getSingleton() {
        return companyRepository.findAll().stream().findFirst().orElseGet(() -> {
            Company c = new Company();
            c.setUpdatedAt(LocalDateTime.now());
            return companyRepository.save(c);
        });
    }
}
