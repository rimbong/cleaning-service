-- =====================================================================
-- V5 계약서 관리 — 보관 위치 필드 + 첨부 파일 테이블
--   실무의 계약은 수기 계약서(종이)나 파일로 관리한다. 이를 반영:
--     1) contract.document_location : 종이 원본 등 보관 위치/비고
--     2) contract_attachment       : 계약서 스캔·별첨 파일(본문은 bytea 로 DB 저장)
--   ※ 엔티티(contract/domain/Contract, ContractAttachment)와 일치해야 한다(ddl-auto=validate).
-- =====================================================================

-- 1) 계약서 원본 보관 위치/비고
ALTER TABLE contract ADD COLUMN document_location VARCHAR(255);

-- 2) 계약서 첨부 파일
--    계약 삭제 시 첨부도 함께 삭제되도록 ON DELETE CASCADE (계약 1:N 첨부).
CREATE TABLE contract_attachment (
    id                BIGSERIAL     PRIMARY KEY,
    contract_id       BIGINT        NOT NULL REFERENCES contract (id) ON DELETE CASCADE,
    original_filename VARCHAR(255)  NOT NULL,          -- 업로드 원본 파일명
    content_type      VARCHAR(100),                    -- MIME 타입
    file_size         BIGINT        NOT NULL,          -- 파일 크기(byte)
    data              BYTEA         NOT NULL,          -- 파일 본문
    created_at        TIMESTAMP     NOT NULL
);

-- 계약별 첨부 목록 조회에 쓰는 FK 인덱스
CREATE INDEX idx_contract_attachment_contract_id ON contract_attachment (contract_id);
