-- =====================================================================
-- V27 매월 계약의 월 방문 횟수 보정
--
--   V26 의 백필이 매월(MONTHLY)만 요일 개수를 무시하고 1 로 고정했다.
--   매주는 요일수 x 4, 격주는 요일수 x 2 로 계산하면서 매월만 빠뜨린 것이다.
--   요일이 여럿인 매월 계약은 실제보다 낮게 잡힌다
--   (매월 + 월·목 = 매달 각 요일 1번씩 = 월 2회인데 1회로 저장).
--
--   이 저장소의 개발 DB 에는 매월 계약이 없어 지금은 바뀌는 행이 없다.
--   다만 V26 이 이미 돌아간 다른 DB(운영 PC 등)에서는 보정이 필요하므로 남긴다.
--
--   직접 고친 값까지 되돌리면 안 되므로, V26 이 넣었을 값(1)과 같은 행만 손댄다.
-- =====================================================================

UPDATE contract
   SET visits_per_month = COALESCE(array_length(string_to_array(NULLIF(cleaning_weekdays, ''), ','), 1), 1),
       updated_at       = NOW()
 WHERE cleaning_cycle = 'MONTHLY'
   AND visits_per_month = 1
   AND COALESCE(array_length(string_to_array(NULLIF(cleaning_weekdays, ''), ','), 1), 1) > 1;
