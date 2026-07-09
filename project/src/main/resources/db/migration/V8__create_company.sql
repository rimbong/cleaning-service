-- =====================================================================
-- V8 회사(공급자) 프로필 — 세금계산서 발행 주체(운영 회사) 정보. 단일 행(설정).
--   ※ 실제 회사정보(상호·사업자번호 등)는 커밋 금지 → 빈 행만 시드하고 관리자 화면에서 입력.
--   ※ 엔티티(company/domain/Company)와 일치해야 한다(ddl-auto=validate).
-- =====================================================================

CREATE TABLE company (
    id              BIGSERIAL     PRIMARY KEY,
    business_number VARCHAR(20),                 -- 등록번호
    company_name    VARCHAR(100),                -- 상호
    owner_name      VARCHAR(50),                 -- 대표자/성명
    address         VARCHAR(255),                -- 사업장 주소
    business_type   VARCHAR(50),                 -- 업태
    business_item   VARCHAR(50),                 -- 종목
    phone           VARCHAR(30),
    updated_at      TIMESTAMP     NOT NULL
);

-- 빈 프로필 1행 시드(관리자 화면에서 채움)
INSERT INTO company (updated_at) VALUES (now());
