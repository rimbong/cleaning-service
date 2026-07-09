package com.boot.cleanhub.biz.contract.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.boot.cleanhub.biz.contract.domain.ContractAttachment;
import com.boot.cleanhub.biz.contract.dto.ContractAttachmentResponse;

/**
 * <pre>
 *   계약서 첨부 파일 저장소.
 *   목록은 파일 메타데이터 프로젝션으로만 조회하고, 실제 파일은 파일시스템에 저장한다
 *   (엔티티에는 저장 경로 storedPath 만 있음 — DB 에 파일 본문은 없다).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.1 (2026.07.08 — DB bytea → 파일시스템 저장 전환)
 */
public interface ContractAttachmentRepository extends JpaRepository<ContractAttachment, Long> {

    /** 특정 계약의 첨부 목록(메타데이터, 업로드순) */
    @Query("select new com.boot.cleanhub.biz.contract.dto.ContractAttachmentResponse("
            + "a.id, a.contract.id, a.originalFilename, a.contentType, a.fileSize, a.createdAt) "
            + "from ContractAttachment a where a.contract.id = :contractId order by a.id")
    List<ContractAttachmentResponse> findMetaByContractId(@Param("contractId") Long contractId);

    /** 단건 조회(메타 + storedPath) — 경로의 계약에 실제로 속한 첨부만 매칭(다운로드/삭제) */
    Optional<ContractAttachment> findByIdAndContract_Id(Long id, Long contractId);

    /** 특정 계약의 첨부 전체(계약 삭제 시 파일 함께 정리용) */
    List<ContractAttachment> findByContract_Id(Long contractId);
}
