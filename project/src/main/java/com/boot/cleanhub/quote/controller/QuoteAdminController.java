package com.boot.cleanhub.quote.controller;

import java.util.List;

import javax.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boot.cleanhub.common.api.ApiResponse;
import com.boot.cleanhub.quote.dto.QuoteRequest;
import com.boot.cleanhub.quote.dto.QuoteResponse;
import com.boot.cleanhub.quote.service.QuoteService;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   견적 관리 API — 관리자 전용.
 *
 *   경로가 /api/admin/** 이므로 JwtApiSecurityConfig 의 hasRole("ADMIN") 규칙으로 보호된다.
 *   (유효한 Bearer 토큰 + roles 클레임에 ROLE_ADMIN 이 있어야 호출 가능)
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin/quotes")
@RequiredArgsConstructor
public class QuoteAdminController {

    private final QuoteService quoteService;

    /** 견적 목록(서비스 내용/고객명 검색 지원) */
    @GetMapping
    public ApiResponse<List<QuoteResponse>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.ok(quoteService.list(keyword));
    }

    /** 견적 단건 조회 */
    @GetMapping("/{id}")
    public ApiResponse<QuoteResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(quoteService.get(id));
    }

    /** 견적 등록 */
    @PostMapping
    public ApiResponse<QuoteResponse> create(@Valid @RequestBody QuoteRequest request) {
        return ApiResponse.ok(quoteService.create(request), "견적이 등록되었습니다.");
    }

    /** 견적 수정 */
    @PutMapping("/{id}")
    public ApiResponse<QuoteResponse> update(@PathVariable Long id, @Valid @RequestBody QuoteRequest request) {
        return ApiResponse.ok(quoteService.update(id, request), "견적이 수정되었습니다.");
    }

    /** 견적 삭제 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        quoteService.delete(id);
        return ApiResponse.ok(null, "견적이 삭제되었습니다.");
    }
}
