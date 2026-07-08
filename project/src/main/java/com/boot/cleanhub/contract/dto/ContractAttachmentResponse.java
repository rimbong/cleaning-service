package com.boot.cleanhub.contract.dto;

import java.time.LocalDateTime;

import lombok.Getter;

/**
 * <pre>
 *   계약서 첨부 파일 응답 DTO(메타데이터 전용 — 파일 본문 bytea 는 제외).
 *   목록 조회 시 파일 본문을 메모리에 올리지 않도록, Repository 의 JPQL 생성자 표현식으로
 *   이 DTO 를 직접 만들어 반환한다(그래서 아래 생성자 시그니처가 그 쿼리와 정확히 일치해야 한다).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.0
 */
@Getter
public class ContractAttachmentResponse {

    private final Long id;
    private final Long contractId;
    private final String originalFilename;
    private final String contentType;
    private final Long fileSize;
    private final LocalDateTime createdAt;

    /** JPQL "new ..." 생성자 표현식 전용(필드 순서 고정). */
    public ContractAttachmentResponse(Long id, Long contractId, String originalFilename,
            String contentType, Long fileSize, LocalDateTime createdAt) {
        this.id = id;
        this.contractId = contractId;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.createdAt = createdAt;
    }
}
