-- =====================================================================
-- V26 계약에 월 방문 횟수 추가
--
--   [문제] 계약은 청소 요일(월~일) + 주기(매주/격주/매월)로만 표현할 수 있어서
--          "월 3회" 같은 패턴을 만들 방법이 없었다. 권장가는 월 방문 횟수로
--          계산하는데 계약이 그 값을 표현하지 못하니 둘이 어긋난다.
--
--          환산 규칙도 취약했다. 격주는 요일 개수를 무시하고 무조건 월 2회로 봤다.
--          격주 + 월·목(요일 2개)이면 실제로는 월 4회인데 월 2회로 계산된다.
--          (지금 격주 계약 2건이 모두 요일 1개라 우연히 맞고 있었을 뿐이다)
--
--   [정리] 요일·주기는 "언제 가는지"(스케줄용)로 두고,
--          "한 달에 몇 번"은 visits_per_month 로 따로 저장한다.
--          화면에서는 요일·주기로 자동 계산해 채워주되 직접 고칠 수 있게 한다.
--
--   NULL 을 허용한다. 값이 없으면 예전처럼 요일·주기로 환산해 쓰므로 기존 동작이 깨지지 않는다.
--
--   ※ 엔티티(biz/contract/domain/Contract)와 일치해야 한다(ddl-auto=validate).
-- =====================================================================

ALTER TABLE contract ADD COLUMN visits_per_month INTEGER;

-- 기존 계약 채우기 — 요일 개수 x 주기 배수.
--   매주 = 한 달 4주 기준(원 단가표가 주1회=월4회로 잡은 것과 맞춘다)
--   청소 주기가 없는 계약은 근거가 없으므로 NULL 로 남긴다(재산정에서 제외되고 그 건수가 표시된다).
UPDATE contract
   SET visits_per_month = CASE cleaning_cycle
         WHEN 'MONTHLY'  THEN 1
         WHEN 'BIWEEKLY' THEN COALESCE(array_length(string_to_array(NULLIF(cleaning_weekdays, ''), ','), 1), 1) * 2
         WHEN 'WEEKLY'   THEN COALESCE(array_length(string_to_array(NULLIF(cleaning_weekdays, ''), ','), 1), 1) * 4
         ELSE NULL
       END
 WHERE visits_per_month IS NULL;
