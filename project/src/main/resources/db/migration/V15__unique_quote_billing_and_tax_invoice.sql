-- 멱등성 보강(코드리뷰 P1-2/P1-3): 견적 1회성 청구·세금계산서 발행 기록의 중복을 DB에서도 막는다.
--   서비스 계층 existsBy 체크에 더해, 동시 요청(경쟁) 시에도 중복 행이 생기지 않도록 유니크 제약을 둔다.
--   유니크를 걸기 전에, 그동안 쌓였을 수 있는 기존 중복을 먼저 정리한다(그룹당 가장 오래된 id 하나만 남김) —
--   기존 데이터가 있어도 마이그레이션이 안전하게 통과하도록.

-- 세금계산서 발행 기록: 기존 중복 제거 후 (거래처, 연, 시작월, 종료월, 기준) 유니크.
DELETE FROM tax_invoice t
    USING tax_invoice d
 WHERE t.client_id   = d.client_id
   AND t.period_year = d.period_year
   AND t.from_month  = d.from_month
   AND t.to_month    = d.to_month
   AND t.basis       = d.basis
   AND t.id > d.id;

ALTER TABLE tax_invoice
    ADD CONSTRAINT uq_tax_invoice_client_period_basis
    UNIQUE (client_id, period_year, from_month, to_month, basis);

-- 견적 청구: 기존 중복 제거 후 (견적, 연, 월) 부분 유니크(계약 청구는 quote_id NULL 이라 제외).
DELETE FROM billing t
    USING billing d
 WHERE t.quote_id IS NOT NULL AND d.quote_id IS NOT NULL
   AND t.quote_id   = d.quote_id
   AND t.bill_year  = d.bill_year
   AND t.bill_month = d.bill_month
   AND t.id > d.id;

CREATE UNIQUE INDEX uq_billing_quote_month
    ON billing (quote_id, bill_year, bill_month)
    WHERE quote_id IS NOT NULL;
