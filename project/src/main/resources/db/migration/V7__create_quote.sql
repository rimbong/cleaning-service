-- =====================================================================
-- V7 견적 테이블
--   일회성 특수청소(입주·물탱크청소 등) 견적. 거래처(client) 연결은 선택(nullable)이며,
--   신규 일회성 고객이면 고객 정보(customer_*)를 직접 담는다.
--   거래처 삭제 시 견적은 남기고 연결만 해제(ON DELETE SET NULL) — 고객 정보는 견적에 보존됨.
--   ※ 엔티티(quote/domain/Quote)와 일치해야 한다(ddl-auto=validate).
-- =====================================================================

CREATE TABLE quote (
    id             BIGSERIAL     PRIMARY KEY,
    client_id      BIGINT        REFERENCES client (id) ON DELETE SET NULL,  -- 대상 거래처(선택)
    customer_name  VARCHAR(50),                                             -- 고객명
    customer_phone VARCHAR(30),                                             -- 고객 연락처
    address        VARCHAR(255),                                            -- 현장 주소
    title          VARCHAR(100)  NOT NULL,                                  -- 서비스 내용(입주청소 등)
    amount         BIGINT        NOT NULL,                                  -- 견적 금액(원)
    quote_date     DATE          NOT NULL,                                  -- 견적일
    valid_until    DATE,                                                    -- 유효기간
    status         VARCHAR(20)   NOT NULL,                                  -- PENDING / ACCEPTED / REJECTED
    memo           TEXT,                                                    -- 메모
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL
);

-- 거래처별 견적 조회용 FK 인덱스
CREATE INDEX idx_quote_client_id ON quote (client_id);
-- 서비스 내용 검색/정렬용 인덱스
CREATE INDEX idx_quote_title ON quote (title);
