package com.boot.cleanhub.biz.contract.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.boot.cleanhub.common.api.ApiResponse;
import com.boot.cleanhub.biz.contract.dto.ContractAttachmentResponse;
import com.boot.cleanhub.biz.contract.service.ContractAttachmentService;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   계약서 첨부 파일 API — 관리자 전용.
 *   경로가 /api/admin/** 이므로 JwtApiSecurityConfig 의 hasRole("ADMIN") 규칙으로 보호된다.
 *
 *   다운로드는 표준 ApiResponse envelope 가 아니라 파일 바이트를 그대로 내려준다
 *   (프론트 axios downloadGet 이 Bearer 를 붙여 호출 → blob 다운로드).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin/contracts/{contractId}/attachments")
@RequiredArgsConstructor
public class ContractAttachmentController {

    private final ContractAttachmentService attachmentService;

    /** 첨부 목록(메타데이터) */
    @GetMapping
    public ApiResponse<List<ContractAttachmentResponse>> list(@PathVariable Long contractId) {
        return ApiResponse.ok(attachmentService.list(contractId));
    }

    /** 첨부 업로드(multipart, 파라미터명 file) */
    @PostMapping
    public ApiResponse<ContractAttachmentResponse> upload(
            @PathVariable Long contractId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.ok(attachmentService.upload(contractId, file), "파일이 첨부되었습니다.");
    }

    /** 첨부 다운로드(파일 바이트) — 읽기·응답구성 모두 서비스+FileUtillMo 로 일원화 */
    @GetMapping("/{attachmentId}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable Long contractId,
            @PathVariable Long attachmentId) {
        return attachmentService.download(contractId, attachmentId);
    }

    /** 첨부 삭제 */
    @DeleteMapping("/{attachmentId}")
    public ApiResponse<Void> delete(
            @PathVariable Long contractId,
            @PathVariable Long attachmentId) {
        attachmentService.delete(contractId, attachmentId);
        return ApiResponse.ok(null, "첨부가 삭제되었습니다.");
    }
}
