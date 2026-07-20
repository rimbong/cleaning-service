-- =====================================================================
-- V25 주기 계수를 표(6단계) 대신 공식으로 전환
--
--   [문제] V22 는 주기 계수를 월1회~주3회 6칸에 하나씩 적어두는 방식이었다.
--          표에 없는 횟수(주 4회 등)는 계산할 수 없어 주 3회로 깎여 산정됐다.
--          실제로 진행중 계약 중 주 4회(MON,TUE,WED,THU) 건이 낮게 잡히고 있었다.
--
--   [전환] 계수 = coef_base x (월 방문횟수) ^ coef_exponent
--
--          기존 6개 값에 곡선을 맞춘 결과가 base 0.6224 / exponent 0.6949 이고,
--          6단계 모두 오차 5% 안에서 재현된다. 방문이 늘수록 1회당 단가가 내려가는
--          볼륨 할인(동선·준비 효율)도 그대로 유지된다.
--            월1회 0.60->0.62  월2회 1.00->1.01  월3회 1.40->1.34
--            주1회 1.70->1.63  주2회 2.60->2.64  주3회 3.40->3.50
--
--          이제 어떤 방문 횟수든 계산되므로 주 4회(월 16회)도 정상 산정된다.
--
--   ※ 엔티티(biz/pricing/domain/PricingPolicy)와 일치해야 한다(ddl-auto=validate).
-- =====================================================================

ALTER TABLE pricing_policy ADD COLUMN coef_base     NUMERIC(6,4);
ALTER TABLE pricing_policy ADD COLUMN coef_exponent NUMERIC(6,4);

UPDATE pricing_policy
   SET coef_base     = 0.6224,
       coef_exponent = 0.6949,
       updated_at    = NOW()
 WHERE coef_base IS NULL;

ALTER TABLE pricing_policy ALTER COLUMN coef_base     SET NOT NULL;
ALTER TABLE pricing_policy ALTER COLUMN coef_exponent SET NOT NULL;

-- 6단계 표는 더 이상 쓰지 않는다.
ALTER TABLE pricing_policy DROP COLUMN coef_monthly_1;
ALTER TABLE pricing_policy DROP COLUMN coef_monthly_2;
ALTER TABLE pricing_policy DROP COLUMN coef_monthly_3;
ALTER TABLE pricing_policy DROP COLUMN coef_weekly_1;
ALTER TABLE pricing_policy DROP COLUMN coef_weekly_2;
ALTER TABLE pricing_policy DROP COLUMN coef_weekly_3;
