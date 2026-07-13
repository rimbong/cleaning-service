-- CleanHub 운영 DB 초기 생성 스크립트
--
-- 실행 방법 (PostgreSQL 설치 후 한 번만):
--   psql -U postgres -f 01_create_db.sql
--   (postgres 슈퍼유저 비밀번호를 물어봅니다 — PostgreSQL 설치할 때 정한 비밀번호)
--
-- ※ 아래 비밀번호는 반드시 바꾸세요. 여기 값과 cleanhub-env.bat 의 DB_PASSWORD 가 같아야 합니다.

-- 1) 앱 전용 계정
CREATE USER cleanhub_user WITH PASSWORD 'CHANGE_ME';

-- 2) 데이터베이스 (소유자 = 앱 계정)
CREATE DATABASE cleanhub OWNER cleanhub_user ENCODING 'UTF8';

-- 3) pgcrypto 확장 — 최초 관리자 계정의 비밀번호 해시(BCrypt)를 만들 때 필요하다.
--    슈퍼유저 권한이 있어야 해서 여기서 미리 만들어 둔다.
\connect cleanhub
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 4) 앱 계정에 스키마 권한
GRANT ALL ON SCHEMA public TO cleanhub_user;
