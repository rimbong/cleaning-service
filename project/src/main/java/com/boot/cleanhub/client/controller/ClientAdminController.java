package com.boot.cleanhub.client.controller;

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

import com.boot.cleanhub.client.dto.ClientRequest;
import com.boot.cleanhub.client.dto.ClientResponse;
import com.boot.cleanhub.client.service.ClientService;
import com.boot.cleanhub.common.api.ApiResponse;
import com.boot.cleanhub.common.dto.PageRequestFactory;
import com.boot.cleanhub.common.dto.PageResponse;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   거래처(건물) 관리 API — 관리자 전용.
 *
 *   경로가 /api/admin/** 이므로 JwtApiSecurityConfig 의 hasRole("ADMIN") 규칙으로 보호된다.
 *   (유효한 Bearer 토큰 + roles 클레임에 ROLE_ADMIN 이 있어야 호출 가능)
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.07
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin/clients")
@RequiredArgsConstructor
public class ClientAdminController {

    private final ClientService clientService;

    /** 거래처 목록(건물명 검색 지원, 페이징) */
    @GetMapping
    public ApiResponse<PageResponse<ClientResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(clientService.list(keyword, PageRequestFactory.of(page, size)));
    }

    /** 거래처 단건 조회 */
    @GetMapping("/{id}")
    public ApiResponse<ClientResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(clientService.get(id));
    }

    /** 거래처 등록 */
    @PostMapping
    public ApiResponse<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
        return ApiResponse.ok(clientService.create(request), "거래처가 등록되었습니다.");
    }

    /** 거래처 수정 */
    @PutMapping("/{id}")
    public ApiResponse<ClientResponse> update(@PathVariable Long id, @Valid @RequestBody ClientRequest request) {
        return ApiResponse.ok(clientService.update(id, request), "거래처가 수정되었습니다.");
    }

    /** 거래처 삭제 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        clientService.delete(id);
        return ApiResponse.ok(null, "거래처가 삭제되었습니다.");
    }
}
