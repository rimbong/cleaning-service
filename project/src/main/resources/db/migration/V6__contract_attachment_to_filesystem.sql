-- =====================================================================
-- V6 계약서 첨부 저장 방식 전환: DB(bytea) -> 파일시스템
--   파일 본문(data bytea)을 제거하고, 파일시스템 저장 상대경로(stored_path)를 둔다.
--   실제 파일은 설정 file.upload-dir 아래(예: <루트>/contract/<계약ID>/...)에 저장한다.
--   ※ 엔티티(contract/domain/ContractAttachment)와 일치해야 한다(ddl-auto=validate).
-- =====================================================================

-- 파일 본문 컬럼 제거(이후 파일시스템에 저장)
ALTER TABLE contract_attachment DROP COLUMN data;

-- 저장 상대경로 컬럼 추가(빈 값 방지 후 NOT NULL 확정 — 기존 행이 있어도 안전)
ALTER TABLE contract_attachment ADD COLUMN stored_path VARCHAR(500);
UPDATE contract_attachment SET stored_path = '' WHERE stored_path IS NULL;
ALTER TABLE contract_attachment ALTER COLUMN stored_path SET NOT NULL;
