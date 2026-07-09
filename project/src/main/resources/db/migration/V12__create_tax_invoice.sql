-- =====================================================================
-- V12 세금계산서 발행 기록
--   거래처·기간의 공급가액·세액을 집계해 "발행" 이력으로 보관. 집계 자체는 billing/payment 로 계산.
--   ※ 엔티티(settlement/domain/TaxInvoice)와 일치해야 한다(ddl-auto=validate).
-- =====================================================================

CREATE TABLE tax_invoice (
    id            BIGSERIAL   PRIMARY KEY,
    client_id     BIGINT      NOT NULL REFERENCES client(id),  -- 공급받는자(거래처)
    period_year   INTEGER     NOT NULL,
    from_month    INTEGER     NOT NULL,
    to_month      INTEGER     NOT NULL,
    supply_amount BIGINT      NOT NULL,   -- 공급가액
    tax_amount    BIGINT      NOT NULL,   -- 세액(공급가액*10%)
    basis         VARCHAR(10) NOT NULL,   -- BILLED / PAID
    issue_date    DATE        NOT NULL,   -- 발행일
    created_at    TIMESTAMP   NOT NULL
);
CREATE INDEX idx_tax_invoice_client_id ON tax_invoice (client_id);
