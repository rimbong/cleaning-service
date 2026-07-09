-- =====================================================================
-- V11 정산 — 청구(billing) + 입금(payment)
--   청구: 계약(월정액) 또는 견적(일회성)의 특정 연월 청구 1건. 입금: 청구 1:N(분할 커버).
--   ※ 엔티티(settlement/domain/{Billing,Payment})와 일치해야 한다(ddl-auto=validate).
-- =====================================================================

CREATE TABLE billing (
    id          BIGSERIAL   PRIMARY KEY,
    contract_id BIGINT      REFERENCES contract(id) ON DELETE CASCADE,  -- 계약 청구(견적이면 NULL)
    quote_id    BIGINT      REFERENCES quote(id)    ON DELETE CASCADE,  -- 견적 청구(계약이면 NULL)
    bill_year   INTEGER     NOT NULL,
    bill_month  INTEGER     NOT NULL,
    amount      BIGINT      NOT NULL,   -- 청구액(생성 시 복사, 편집 가능)
    memo        TEXT,
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL,
    -- 계약 청구는 (계약,연,월) 중복 방지. quote_id 쪽은 contract_id 가 NULL 이라 제약 영향 없음.
    CONSTRAINT uq_billing_contract_month UNIQUE (contract_id, bill_year, bill_month)
);
CREATE INDEX idx_billing_year_month ON billing (bill_year, bill_month);
CREATE INDEX idx_billing_contract_id ON billing (contract_id);
CREATE INDEX idx_billing_quote_id ON billing (quote_id);

CREATE TABLE payment (
    id         BIGSERIAL   PRIMARY KEY,
    billing_id BIGINT      NOT NULL REFERENCES billing(id) ON DELETE CASCADE,
    amount     BIGINT      NOT NULL,   -- 입금액
    paid_date  DATE        NOT NULL,   -- 입금일
    method     VARCHAR(30),            -- 수금방법
    memo       VARCHAR(255),
    created_at TIMESTAMP   NOT NULL
);
CREATE INDEX idx_payment_billing_id ON payment (billing_id);
