-- =====================================================================
-- V1 초기 스키마 (PostgreSQL)
--   인증 기반 테이블. cleanhub 도메인 테이블은 이후 마이그레이션(V3, V4 …)으로 추가한다.
--   ※ ddl-auto=validate 이므로 이 스키마는 엔티티(AuthUser, RefreshToken)와 일치해야 한다.
-- =====================================================================

-- 세션 로그인 사용자 (auth/domain/AuthUser)
CREATE TABLE auth_user (
    id        BIGSERIAL     PRIMARY KEY,
    username  VARCHAR(255)  NOT NULL UNIQUE,
    password  VARCHAR(255)  NOT NULL,          -- BCrypt 해시
    role      VARCHAR(255)                     -- 예: ROLE_ADMIN, ROLE_USER
);

-- JWT refresh 토큰 (auth/domain/RefreshToken) — 사용자당 1행(username unique)
CREATE TABLE jwt_refresh_token (
    id        BIGSERIAL     PRIMARY KEY,
    username  VARCHAR(255)  NOT NULL UNIQUE,
    token     VARCHAR(512)  NOT NULL
);
