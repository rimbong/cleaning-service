-- =====================================================================
-- V10 계약(contract) 수금방법·출입문 비번 컬럼 추가(선택)
--   수금대장의 수금방법·출입문 정보 반영. 전부 nullable.
--   ※ 엔티티(contract/domain/Contract)와 일치해야 한다(ddl-auto=validate).
-- =====================================================================

ALTER TABLE contract ADD COLUMN payment_method VARCHAR(30);  -- 수금 방법/계좌
ALTER TABLE contract ADD COLUMN door_code      VARCHAR(50);  -- 출입문 비번(운영 메모)
