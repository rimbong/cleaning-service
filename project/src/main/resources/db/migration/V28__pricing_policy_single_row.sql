-- =====================================================================
-- V28 단가 정책 단일 행 보장
--
--   pricing_policy 는 설정 테이블이라 행이 하나여야 한다. 그런데 제약이 없어서
--   실수로 두 행이 생기면 어느 것이 적용되는지 정해지지 않는다.
--   (조회를 findTopByOrderByIdAsc 로 바꿔 순서는 고정했지만, 애초에 두 행이
--    생기지 않게 막는 편이 확실하다)
--
--   상수식 유니크 인덱스로 "행은 최대 하나" 를 표현한다.
--   두 번째 INSERT 는 유니크 위반으로 거부되고, GlobalExceptionHandler 가 409 로 바꾼다.
-- =====================================================================

-- 혹시 여러 행이 있으면 가장 먼저 만든 것만 남긴다(제약을 걸려면 먼저 정리해야 한다).
DELETE FROM pricing_policy
 WHERE id <> (SELECT MIN(id) FROM pricing_policy);

CREATE UNIQUE INDEX uk_pricing_policy_single_row ON pricing_policy ((true));
