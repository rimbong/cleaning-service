-- =====================================================================
-- V2 최초 관리자 계정 시드
--   "닭이 먼저냐 달걀이 먼저냐" — 로그인하려면 계정이 있어야 하므로,
--   최초 admin 1명을 마이그레이션으로 넣는다. 이후 계정 관리는 앱(관리자 화면)에서.
--
--   비밀번호는 pgcrypto 의 crypt()+gen_salt('bf') 로 BCrypt 해시 생성.
--   (Spring Security BCryptPasswordEncoder 와 호환되는 $2a$ 형식)
--   ※ pgcrypto 확장은 DB 생성 시 슈퍼유저가 설치해 둔다: CREATE EXTENSION pgcrypto;
--
--   초기 계정:  admin / admin1234  (ROLE_ADMIN)
--   ⚠ 운영 배포 시 최초 로그인 후 반드시 비밀번호를 변경할 것.
-- =====================================================================

INSERT INTO auth_user (username, password, role)
VALUES ('admin', crypt('admin1234', gen_salt('bf', 10)), 'ROLE_ADMIN');
