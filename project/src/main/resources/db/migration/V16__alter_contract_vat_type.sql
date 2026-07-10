-- 계약(contract)에 부가세 기준 필드 추가(코드리뷰 P1-1).
--   EXCLUSIVE(부가세 별도, 청구액=공급가액) / INCLUSIVE(부가세 포함) / FREE(면세)
--   세금계산서 집계에서 청구액을 공급가액·세액으로 나눌 때 사용. 거래처마다 다를 수 있어 계약별.
ALTER TABLE contract ADD COLUMN vat_type VARCHAR(20);

-- 기존 계약은 종전 동작(청구액=공급가액, 세액=청구액*10%)과 같은 EXCLUSIVE 로 기본 설정.
UPDATE contract SET vat_type = 'EXCLUSIVE' WHERE vat_type IS NULL;
