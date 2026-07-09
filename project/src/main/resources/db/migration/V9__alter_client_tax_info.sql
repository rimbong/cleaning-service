-- =====================================================================
-- V9 거래처(client) 세금계산서/사업자 정보 컬럼 추가(선택)
--   세금계산서 공급받는자란에 사용. 전부 nullable.
--   ※ 엔티티(client/domain/Client)와 일치해야 한다(ddl-auto=validate).
-- =====================================================================

ALTER TABLE client ADD COLUMN business_number     VARCHAR(20);   -- 사업자번호
ALTER TABLE client ADD COLUMN representative_name VARCHAR(50);   -- 대표자/성명
ALTER TABLE client ADD COLUMN business_type       VARCHAR(50);   -- 업태
ALTER TABLE client ADD COLUMN business_item       VARCHAR(50);   -- 종목
ALTER TABLE client ADD COLUMN tax_invoice_type    VARCHAR(20);   -- ELECTRONIC/EMAIL/LABOR/NONE
