package com.boot.cleanhub.biz.company.controller;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boot.cleanhub.common.api.ApiResponse;
import com.boot.cleanhub.biz.company.dto.CompanyRequest;
import com.boot.cleanhub.biz.company.dto.CompanyResponse;
import com.boot.cleanhub.biz.company.service.CompanyService;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   회사(공급자) 프로필 API — 관리자 전용. 단일 설정이라 조회/수정만.
 *   /api/admin/** 이므로 ROLE_ADMIN 보호.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.09
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin/company")
@RequiredArgsConstructor
public class CompanyAdminController {

    private final CompanyService companyService;

    /** 회사 프로필 조회 */
    @GetMapping
    public ApiResponse<CompanyResponse> get() {
        return ApiResponse.ok(companyService.get());
    }

    /** 회사 프로필 수정 */
    @PutMapping
    public ApiResponse<CompanyResponse> update(@Valid @RequestBody CompanyRequest request) {
        return ApiResponse.ok(companyService.update(request), "회사 정보가 저장되었습니다.");
    }
}
