-- =====================================================================
-- V22 계단·공용부 정기청소 권장가(적정가) 산정
--
--   1) pricing_policy — 단가 정책. 회사 정보처럼 <b>한 행만</b> 쓰는 설정 테이블이다.
--      단가를 코드 상수로 두면 최저임금이 오를 때마다 소스 수정·재빌드·재배포가 필요해서
--      배포된 PC 에서 손댈 수가 없다. 화면에서 고칠 수 있도록 DB 에 둔다.
--
--   2) client 건물 규모 — 층수·세대수·공용화장실 등. 권장가 산정의 입력값이며,
--      한 번 넣어두면 견적뿐 아니라 기존 계약의 적정가 재산정(인상 검토)에도 쓴다.
--
--   ※ 엔티티(biz/pricing/domain/PricingPolicy, biz/client/domain/Client)와 일치해야 한다
--     (ddl-auto=validate).
-- =====================================================================

CREATE TABLE pricing_policy (
    id              BIGSERIAL   PRIMARY KEY,
    base_fee        BIGINT      NOT NULL,   -- 기본 출동료(원/월) — 이동·준비·소모품·관리비·최소마진
    per_floor       BIGINT      NOT NULL,   -- 층당 단가(원) — 계단참·난간·창틀. 작업량의 핵심
    per_household   BIGINT      NOT NULL,   -- 세대당 단가(원) — 세대 많을수록 오염·쓰레기 증가분
    per_toilet      BIGINT      NOT NULL,   -- 공용 화장실 1개당(원)
    elevator_fee    BIGINT      NOT NULL,   -- 엘리베이터 있을 때 가산(원)
    -- 주기 계수 — 횟수를 늘려도 단순 배수가 아니다(한 번 갈 때 몰아 하는 동선·준비 효율 반영).
    --   기준은 월 2회(격주) = 1.0 이며 위 단가들이 그 기준에 맞춰져 있다.
    coef_monthly_1  NUMERIC(4,2) NOT NULL,  -- 월 1회
    coef_monthly_2  NUMERIC(4,2) NOT NULL,  -- 월 2회(격주) — 기준
    coef_monthly_3  NUMERIC(4,2) NOT NULL,  -- 월 3회
    coef_weekly_1   NUMERIC(4,2) NOT NULL,  -- 주 1회(월 4회)
    coef_weekly_2   NUMERIC(4,2) NOT NULL,  -- 주 2회
    coef_weekly_3   NUMERIC(4,2) NOT NULL,  -- 주 3회
    rounding_unit   BIGINT      NOT NULL,   -- 최종 금액 반올림 단위(원). 1000 이면 천원 단위
    memo            VARCHAR(255),
    created_at      TIMESTAMP   NOT NULL,
    updated_at      TIMESTAMP   NOT NULL
);

-- 초기값 — 2026년 최저임금 시급 10,320원 기준으로 잡은 단가표.
--   (소규모 빌라 밀집형 운영 전제. 건물이 멀어 단독 출동해야 하면 base_fee 를 올린다)
INSERT INTO pricing_policy (
    base_fee, per_floor, per_household, per_toilet, elevator_fee,
    coef_monthly_1, coef_monthly_2, coef_monthly_3, coef_weekly_1, coef_weekly_2, coef_weekly_3,
    rounding_unit, memo, created_at, updated_at
) VALUES (
    20000, 6000, 1500, 15000, 5000,
    0.60, 1.00, 1.40, 1.70, 2.60, 3.40,
    1000, '2026년 최저임금(시급 10,320원) 기준 초기 단가', NOW(), NOW()
);

-- ── 거래처(건물) 규모 ────────────────────────────────────────────────
--   전부 NULL 허용이다. 기존 거래처 23건은 아직 실측 전이라 값이 없고,
--   값이 없는 거래처는 권장가 산정 대상에서 제외한다(억지로 0 을 넣으면 엉뚱한 금액이 나온다).
ALTER TABLE client ADD COLUMN floors          INTEGER;   -- 지상 층수(계단 청소 대상 층)
ALTER TABLE client ADD COLUMN household_count INTEGER;   -- 세대수(호실 수)
ALTER TABLE client ADD COLUMN shared_toilets  INTEGER;   -- 공용 화장실 개수
ALTER TABLE client ADD COLUMN extra_floors    INTEGER;   -- 지하·옥상 등 추가 청소 층
ALTER TABLE client ADD COLUMN has_elevator    BOOLEAN;   -- 엘리베이터 유무
