-- 계약(contract)에 계약서 양식의 "추가서비스"·"추가사항" 칸에 들어갈 데이터 필드 추가.
--   extra_services : 추가 서비스 항목(기본 서비스 외 별도 합의한 작업)
--   extra_notes    : 계약서에 적을 추가사항(특약 등)
--
-- 기존 memo 는 관리자 내부용 메모라 계약서에 인쇄하면 안 된다(고객에게 나가는 문서).
-- 그래서 계약서에 찍을 문구는 extra_notes 로 따로 받는다.
ALTER TABLE contract ADD COLUMN extra_services VARCHAR(255);
ALTER TABLE contract ADD COLUMN extra_notes    VARCHAR(255);
