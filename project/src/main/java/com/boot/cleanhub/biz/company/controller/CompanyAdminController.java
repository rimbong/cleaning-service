package com.boot.cleanhub.biz.company.controller;

import javax.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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

    /** 도장(인장) 이미지 등록(multipart) */
    @PostMapping("/stamp")
    public ApiResponse<CompanyResponse> uploadStamp(@RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(companyService.uploadStamp(file), "도장 이미지가 등록되었습니다.");
    }

    /** 도장 이미지 삭제 */
    @DeleteMapping("/stamp")
    public ApiResponse<CompanyResponse> removeStamp() {
        return ApiResponse.ok(companyService.removeStamp(), "도장 이미지가 삭제되었습니다.");
    }

    /** 도장 이미지 미리보기(바이트) — 없으면 404 */
    @GetMapping("/stamp")
    public ResponseEntity<byte[]> stamp() {
        byte[] bytes = companyService.getStampBytes();
        if (bytes == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(bytes);
    }
}
