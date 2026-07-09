-- 계약(contract)에 정기 청소 스케줄 필드 추가.
--  - cleaning_weekdays: 청소 요일 다중(요일 코드 쉼표구분, 예 "MON,WED,FRI")
--  - cleaning_cycle:    청소 주기(WEEKLY 매주 / BIWEEKLY 격주 / MONTHLY 매월)
ALTER TABLE contract ADD COLUMN cleaning_weekdays VARCHAR(30);
ALTER TABLE contract ADD COLUMN cleaning_cycle VARCHAR(20);

-- 기존 계약은 주기를 매주(WEEKLY)로 기본 설정(요일은 미지정 상태로 두어 이후 화면에서 지정).
UPDATE contract SET cleaning_cycle = 'WEEKLY' WHERE cleaning_cycle IS NULL;
