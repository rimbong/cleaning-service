-- 세금계산서 집계 기간을 "연도 1개 + 월범위"에서 "(시작연,시작월)~(종료연,종료월)"로 확장(코드리뷰 P2-4).
--   기존: period_year + from_month + to_month  (한 해 안의 기간만 표현 가능)
--   변경: from_year + from_month + to_year + to_month  (연말~연초 등 연도 경계 기간 표현 가능)
--
--   엔티티(settlement/domain/TaxInvoice)와 일치해야 한다(ddl-auto=validate).

-- 1) period_year -> from_year 로 이름 변경(기존 값 보존).
ALTER TABLE tax_invoice RENAME COLUMN period_year TO from_year;

-- 2) to_year 추가 후, 기존 행은 단일 연도였으므로 from_year 로 채우고 NOT NULL 확정.
ALTER TABLE tax_invoice ADD COLUMN to_year INTEGER;
UPDATE tax_invoice SET to_year = from_year WHERE to_year IS NULL;
ALTER TABLE tax_invoice ALTER COLUMN to_year SET NOT NULL;

-- 3) 중복 발행 방지 유니크 제약을 새 기간 컬럼 구성으로 교체.
ALTER TABLE tax_invoice DROP CONSTRAINT uq_tax_invoice_client_period_basis;
ALTER TABLE tax_invoice
    ADD CONSTRAINT uq_tax_invoice_client_period_basis
    UNIQUE (client_id, from_year, from_month, to_year, to_month, basis);
