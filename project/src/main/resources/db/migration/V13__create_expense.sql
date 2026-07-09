-- =====================================================================
-- V13 지출(expense) — 주유 등 경비. 정산과 독립.
--   ※ 엔티티(expense/domain/Expense)와 일치해야 한다(ddl-auto=validate).
-- =====================================================================

CREATE TABLE expense (
    id              BIGSERIAL   PRIMARY KEY,
    category        VARCHAR(20) NOT NULL,   -- FUEL(주유) / ETC(기타)
    vendor_name     VARCHAR(100),           -- 거래처/주유소명
    business_number VARCHAR(20),            -- 사업자번호(선택)
    amount          BIGINT      NOT NULL,   -- 금액
    expense_date    DATE        NOT NULL,   -- 지출일
    memo            VARCHAR(255),
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP   NOT NULL
);
CREATE INDEX idx_expense_date ON expense (expense_date);
