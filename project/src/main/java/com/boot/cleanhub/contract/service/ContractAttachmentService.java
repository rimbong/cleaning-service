package com.boot.cleanhub.contract.service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.boot.cleanhub.contract.domain.Contract;
import com.boot.cleanhub.contract.domain.ContractAttachment;
import com.boot.cleanhub.contract.dto.ContractAttachmentResponse;
import com.boot.cleanhub.contract.repository.ContractAttachmentRepository;
import com.boot.cleanhub.contract.repository.ContractRepository;
import com.boot.cleanhub.error.BizException;
import com.boot.cleanhub.error.ErrorCode;
import com.boot.cleanhub.util.file.FileUtillMo;

import lombok.RequiredArgsConstructor;

/**
 * <pre>
 *   계약서 첨부 파일 서비스 — 업로드/목록/다운로드/삭제.
 *   파일 본문은 파일시스템(file.upload-dir 아래)에 저장하고, 실제 파일 I/O 는 FileUtillMo 로 일원화한다.
 *   DB(contract_attachment)에는 메타데이터 + 저장 상대경로(storedPath)만 보관한다.
 *
 *   허용 확장자만 받는다(계약서 스캔·문서 위주). 크기 상한은 multipart 설정(application.yml)이 담당하고,
 *   초과 시 MaxUploadSizeExceededException 은 GlobalExceptionHandler 가 처리한다.
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.1 (2026.07.08 — DB bytea → 파일시스템 저장 전환, FileUtillMo 사용)
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContractAttachmentService {

    /** 허용 확장자(소문자) — 계약서 스캔(PDF/이미지)·한글/오피스 문서 */
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            "pdf", "png", "jpg", "jpeg", "gif", "webp",
            "hwp", "hwpx", "doc", "docx", "xls", "xlsx"));

    private final ContractAttachmentRepository attachmentRepository;
    private final ContractRepository contractRepository;

    /** 업로드 저장 루트(설정 file.upload-dir) */
    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * 첨부 목록(메타데이터).
     *
     * @param contractId 계약 ID
     * @return 업로드순 첨부 메타 목록
     */
    public List<ContractAttachmentResponse> list(Long contractId) {
        return attachmentRepository.findMetaByContractId(contractId);
    }

    /**
     * 첨부 업로드 — 파일시스템에 저장(FileUtillMo)하고 메타데이터를 DB 에 기록.
     *
     * @param contractId 계약 ID
     * @param file       업로드 파일
     * @return 저장된 첨부 메타데이터
     * @throws BizException 계약 없음(CONTRACT_NOT_FOUND)/빈 파일·저장 실패(FILE_UPLOAD_FAILED)/확장자 불허(INVALID_FILE_FORMAT)
     */
    @Transactional
    public ContractAttachmentResponse upload(Long contractId, MultipartFile file) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new BizException(ErrorCode.CONTRACT_NOT_FOUND));

        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.FILE_UPLOAD_FAILED);
        }
        String originalFilename = StringUtils.hasText(file.getOriginalFilename())
                ? file.getOriginalFilename() : "unnamed";
        validateExtension(originalFilename);

        String storedPath;
        try {
            // 계약별 하위 경로에 고유 파일명으로 저장(FileUtillMo). 저장 상대경로만 DB 에 보관한다.
            storedPath = FileUtillMo.uploadSingleFile(file, uploadDir, "contract/" + contractId);
        } catch (IOException e) {
            throw new BizException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        ContractAttachment attachment = new ContractAttachment();
        attachment.setContract(contract);
        attachment.setOriginalFilename(originalFilename);
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setStoredPath(storedPath);

        ContractAttachment saved = attachmentRepository.save(attachment);
        return new ContractAttachmentResponse(saved.getId(), contractId, saved.getOriginalFilename(),
                saved.getContentType(), saved.getFileSize(), saved.getCreatedAt());
    }

    /**
     * 첨부 다운로드 응답 — 해당 계약의 첨부인지 확인 후 파일을 읽어 표준 다운로드 응답을 만든다(FileUtillMo).
     *
     * @param contractId   계약 ID(경로)
     * @param attachmentId 첨부 ID
     * @return 다운로드용 ResponseEntity
     * @throws BizException 해당 계약의 첨부가 아니면 CONTRACT_ATTACHMENT_NOT_FOUND, 파일 읽기 실패면 FILE_UPLOAD_FAILED
     */
    public ResponseEntity<byte[]> download(Long contractId, Long attachmentId) {
        ContractAttachment attachment = attachmentRepository.findByIdAndContract_Id(attachmentId, contractId)
                .orElseThrow(() -> new BizException(ErrorCode.CONTRACT_ATTACHMENT_NOT_FOUND));
        try {
            // baseDir + 저장 상대경로로 파일을 읽어 응답 구성(uploadSingleFile 과 인자 스타일 일치)
            return FileUtillMo.downloadResponse(uploadDir, attachment.getStoredPath(),
                    attachment.getOriginalFilename(), attachment.getContentType());
        } catch (IOException e) {
            throw new BizException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 첨부 삭제 — DB 행 + 파일시스템 파일 함께 제거.
     *
     * @param contractId   계약 ID(경로)
     * @param attachmentId 첨부 ID
     * @throws BizException 해당 계약의 첨부가 아니면 CONTRACT_ATTACHMENT_NOT_FOUND
     */
    @Transactional
    public void delete(Long contractId, Long attachmentId) {
        ContractAttachment attachment = attachmentRepository.findByIdAndContract_Id(attachmentId, contractId)
                .orElseThrow(() -> new BizException(ErrorCode.CONTRACT_ATTACHMENT_NOT_FOUND));
        FileUtillMo.deleteFile(fullPath(attachment)); // 디스크 파일 정리(best-effort)
        attachmentRepository.delete(attachment);
    }

    /**
     * 특정 계약의 첨부 전체 삭제(DB 행 + 파일) — 계약 삭제 시 호출.
     * 파일시스템은 DB FK cascade 로 지워지지 않으므로 물리 파일을 여기서 함께 정리한다.
     *
     * @param contractId 계약 ID
     */
    @Transactional
    public void deleteAllByContract(Long contractId) {
        List<ContractAttachment> attachments = attachmentRepository.findByContract_Id(contractId);
        for (ContractAttachment a : attachments) {
            FileUtillMo.deleteFile(fullPath(a));
        }
        attachmentRepository.deleteAll(attachments);
    }

    /** 저장 루트 + 상대경로 → 전체 경로 */
    private String fullPath(ContractAttachment attachment) {
        return new File(uploadDir, attachment.getStoredPath()).getPath();
    }

    /** 파일명 확장자 화이트리스트 검증 */
    private void validateExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        String ext = dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BizException(ErrorCode.INVALID_FILE_FORMAT);
        }
    }
}
