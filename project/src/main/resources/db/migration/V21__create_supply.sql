-- =====================================================================
-- V21 약품/소모품 재고 — 품목 마스터(supply_item) + 입출고 이력(supply_transaction)
--
--   현재 재고는 컬럼으로 저장하지 않는다. supply_transaction.quantity(부호 있는 증감)의
--   합계로 도출한다. 수량 컬럼을 직접 UPDATE 하면 숫자가 틀어졌을 때 언제/왜 틀어졌는지
--   추적할 방법이 없어진다. 이력만 쌓으면 실사 조정도 한 줄로 남는다.
--
--   ※ 엔티티(supply/domain/SupplyItem·SupplyTransaction)와 일치해야 한다(ddl-auto=validate).
-- =====================================================================

CREATE TABLE supply_item (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,          -- 약품/소모품명 (예: 락스)
    spec        VARCHAR(50),                    -- 규격 (예: 20L 말통)
    unit        VARCHAR(20)  NOT NULL,          -- 단위 (예: 통, 개, 박스)
    unit_price  BIGINT,                         -- 최근 구매 단가(원, 선택)
    safety_qty  INTEGER      NOT NULL,          -- 안전재고 — 이 아래로 떨어지면 목록에서 경고 표시
    memo        VARCHAR(255),
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL
);

-- 같은 품목을 두 번 등록하면 재고가 두 행으로 쪼개져 조용히 틀어진다. 이름+규격으로 막는다.
--   (spec 은 NULL 허용이라 NULL 끼리도 중복으로 잡히도록 COALESCE 로 빈 문자열 취급)
CREATE UNIQUE INDEX uk_supply_item_name_spec ON supply_item (name, COALESCE(spec, ''));

CREATE TABLE supply_transaction (
    id          BIGSERIAL   PRIMARY KEY,
    item_id     BIGINT      NOT NULL REFERENCES supply_item (id),
    tx_type     VARCHAR(20) NOT NULL,   -- IN(입고) / OUT(사용) / ADJUST(실사조정)
    quantity    INTEGER     NOT NULL,   -- 부호 있는 증감. 입고 +, 사용 -, 조정 = 실사수량 - 당시재고
    tx_date     DATE        NOT NULL,   -- 입고일 / 사용일
    memo        VARCHAR(255),
    created_at  TIMESTAMP   NOT NULL,
    updated_at  TIMESTAMP   NOT NULL
);

CREATE INDEX idx_supply_tx_item ON supply_transaction (item_id);
CREATE INDEX idx_supply_tx_date ON supply_transaction (tx_date);
