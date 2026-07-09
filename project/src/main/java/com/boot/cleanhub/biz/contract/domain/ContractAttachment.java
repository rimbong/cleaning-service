package com.boot.cleanhub.biz.contract.domain;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * <pre>
 *   계약서 첨부 파일 — 계약 하나에 여러 개(계약서 스캔·별첨 등). 계약(Contract)을 참조한다.
 *
 *   파일 본문은 파일시스템(설정 file.upload-dir 아래)에 저장하고, DB 에는 메타데이터와
 *   저장 상대경로(storedPath)만 보관한다. 실제 파일 I/O 는 util/file/FileUtillMo 로 일원화한다.
 *   ※ 스키마는 Flyway(V5 생성 + V6 파일시스템 전환)가 소스이며, 이 엔티티와 일치해야 한다(ddl-auto=validate).
 * </pre>
 *
 * @author In-seong Hwang
 * @since 2026.07.08
 * @version 1.0
 */
@Entity
@Table(name = "contract_attachment")
@Getter
@Setter
public class ContractAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 계약 — 다대일 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    /** 업로드 당시 원본 파일명(다운로드 시 그대로 내려줌) */
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    /** MIME 타입(예: application/pdf, image/png) */
    @Column(name = "content_type", length = 100)
    private String contentType;

    /** 파일 크기(byte) */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /** 저장 상대경로(업로드 루트 file.upload-dir 기준, 예: "contract/10/2026..._ab12.pdf") */
    @Column(name = "stored_path", nullable = false, length = 500)
    private String storedPath;

    /** 업로드 시각 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 최초 저장 직전: 업로드 시각 기록 */
    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
