-- 회사(공급자)에 도장(인장) 이미지 경로 추가.
--   세금계산서 양식 출력 시 "도장 포함"을 선택하면 이 이미지를 공급자 도장/영수 도장 자리에 찍는다.
--   이미지 본문은 파일시스템(file.upload-dir/company/stamp)에 저장하고 DB에는 상대경로만 보관.
ALTER TABLE company ADD COLUMN stamp_image_path VARCHAR(255);
