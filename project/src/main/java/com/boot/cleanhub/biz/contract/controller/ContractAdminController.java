package com.boot.cleanhub.biz.contract.controller;

import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
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
import com.boot.cleanhub.common.dto.PageRequestFactory;
import com.boot.cleanhub.common.dto.PageResponse;
import com.boot.cleanhub.biz.contract.dto.ContractRequest;
import com.boot.cleanhub.biz.contract.dto.ContractResponse;
import com.boot.cleanhub.biz.contract.service.ContractDocumentService;
import com.boot.cleanhub.biz.contract.service.ContractService;
import com.boot.cleanhub.util.file.FileUtillMo;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   계약 관리 API — 관리자 전용.
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
@RequestMapping("/api/admin/contracts")
@RequiredArgsConstructor
public class ContractAdminController {

    /** 계약서(HWP) MIME 타입 — 한글 문서 */
    private static final String HWP_CONTENT_TYPE = "application/x-hwp";

    private final ContractService contractService;
    private final ContractDocumentService contractDocumentService;

    /** 계약 목록(계약명 검색 또는 거래처 필터 지원, 페이징) */
    @GetMapping
    public ApiResponse<PageResponse<ContractResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.ok(contractService.list(keyword, clientId, PageRequestFactory.of(page, size)));
    }

    /** 계약 단건 조회 */
    @GetMapping("/{id}")
    public ApiResponse<ContractResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(contractService.get(id));
    }

    /** 계약 등록 */
    @PostMapping
    public ApiResponse<ContractResponse> create(@Valid @RequestBody ContractRequest request) {
        return ApiResponse.ok(contractService.create(request), "계약이 등록되었습니다.");
    }

    /** 계약 수정 */
    @PutMapping("/{id}")
    public ApiResponse<ContractResponse> update(@PathVariable Long id, @Valid @RequestBody ContractRequest request) {
        return ApiResponse.ok(contractService.update(id, request), "계약이 수정되었습니다.");
    }

    /** 계약서(HWP) 다운로드 — 빈 양식을 계약/거래처/회사정보로 채워 생성. withStamp=true 면 회사 도장 포함 */
    @GetMapping("/{id}/document")
    public ResponseEntity<byte[]> document(@PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean withStamp) {
        byte[] bytes = contractDocumentService.buildContractDocument(id, withStamp);
        return FileUtillMo.downloadResponse(bytes, contractDocumentService.buildFileName(id), HWP_CONTENT_TYPE);
    }

    /** 계약 삭제 */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        contractService.delete(id);
        return ApiResponse.ok(null, "계약이 삭제되었습니다.");
    }
}
