-- =====================================================================
-- V3 거래처(건물) 테이블
--   청소 서비스 대상 건물/고객. 계약·견적·정산이 이 거래처를 참조한다.
--   ※ 엔티티(client/domain/Client)와 일치해야 한다(ddl-auto=validate).
-- =====================================================================

CREATE TABLE client (
    id                  BIGSERIAL     PRIMARY KEY,
    name                VARCHAR(100)  NOT NULL,          -- 건물명
    address             VARCHAR(255),                    -- 주소
    manager_name        VARCHAR(50),                     -- 담당자명
    manager_phone       VARCHAR(30),                     -- 담당자 연락처
    cleaning_type       VARCHAR(20),                     -- REGULAR(정기) / SPECIAL(특수)
    contract_start_date DATE,                            -- 계약 시작일(정기 계약 시)
    memo                TEXT,                            -- 메모
    created_at          TIMESTAMP     NOT NULL,
    updated_at          TIMESTAMP     NOT NULL
);

-- 목록 검색/정렬에 자주 쓰는 건물명 인덱스
CREATE INDEX idx_client_name ON client (name);
