-- 계약(contract)에 계단청소 계약서(계단청소계약서.hwp) 기준 데이터 필드 추가.
--   initial_fee    : 초도(최초 1회) 청소비
--   cleaning_scope : 청소 범위(예: "지하1층~지상4층 건물내부")
--   service_items  : 기본 서비스 항목(예: "현관,계단창틀,계단손잡이,우편함,화장실")
ALTER TABLE contract ADD COLUMN initial_fee    BIGINT;
ALTER TABLE contract ADD COLUMN cleaning_scope VARCHAR(255);
ALTER TABLE contract ADD COLUMN service_items  VARCHAR(255);
