-- =====================================================================
-- V4 계약 테이블
--   거래처(건물)의 정기 청소 월정액 계약. 한 거래처에 계약이 여러 개일 수 있다(1:N).
--   ※ 엔티티(contract/domain/Contract)와 일치해야 한다(ddl-auto=validate).
-- =====================================================================

CREATE TABLE contract (
    id            BIGSERIAL     PRIMARY KEY,
    client_id     BIGINT        NOT NULL REFERENCES client (id),  -- 대상 거래처(건물)
    title         VARCHAR(100)  NOT NULL,                         -- 계약명
    monthly_fee   BIGINT        NOT NULL,                         -- 월 청구금액(원)
    billing_day   INTEGER,                                        -- 청구일(매월 N일, 1~31)
    start_date    DATE          NOT NULL,                         -- 계약 시작일
    end_date      DATE,                                           -- 계약 종료일(무기한이면 NULL)
    status        VARCHAR(20)   NOT NULL,                         -- ACTIVE / ENDED / SUSPENDED
    memo          TEXT,                                           -- 메모
    created_at    TIMESTAMP     NOT NULL,
    updated_at    TIMESTAMP     NOT NULL
);

-- 거래처별 계약 조회(거래처 상세의 계약 목록)에 자주 쓰는 FK 인덱스
CREATE INDEX idx_contract_client_id ON contract (client_id);
-- 계약명 검색/정렬용 인덱스
CREATE INDEX idx_contract_title ON contract (title);
