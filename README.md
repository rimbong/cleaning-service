# CleanHub

청소업(건물 계단 청소·입주청소·물탱크청소 등) **견적·정산 관리 시스템**.

- **대민(고객)**: 견적 요청 · 진행 조회 · 후기 · 건의
- **관리자**: 견적 처리 · 거래처/계약 관리 · 월 청구/정산(엑셀) · 후기/문의 관리 · 대시보드

## 기술 스택

| 영역 | 스택 |
|---|---|
| 백엔드 | Java 8, Spring Boot 2.7.18 (Security, Data JPA, Thymeleaf), WAR |
| DB | PostgreSQL 18 (스키마: Flyway) |
| 프론트 | Vue 3, Vite, Pinia, vue-router, vue-i18n, axios, vue-query |
| 인증 | 세션 + JWT 이중 SecurityFilterChain |

## 로컬 실행

### 1. DB 준비 (PostgreSQL 18)
```sql
CREATE USER cleanhub_user WITH PASSWORD '1234';
CREATE DATABASE cleanhub OWNER cleanhub_user;
-- cleanhub DB 에서:
CREATE EXTENSION IF NOT EXISTS pgcrypto;
GRANT ALL ON SCHEMA public TO cleanhub_user;
```
스키마·초기 admin 계정은 앱 기동 시 **Flyway**(`resources/db/migration/`)가 자동 생성.
- 초기 관리자: `admin` / `admin1234` (ROLE_ADMIN) — 최초 로그인 후 변경 권장.

### 2. 백엔드
```
cd project
mvnw.cmd spring-boot:run        # http://localhost:70
```

### 3. 프론트 (개발 서버)
```
cd frontend/vue
npm install
npm run dev                     # http://localhost:5173/app/  (API 는 :70 프록시)
```

## 구조

```
project/          Spring Boot 백엔드 (com.boot.cleanhub)
frontend/vue/     Vue 3 SPA (빌드 산출물 → project/.../static/app)
.claude/          작업 가이드(CLAUDE.md)·스킬·worklog
```

자세한 개발 가이드는 [.claude/CLAUDE.md](.claude/CLAUDE.md) 참고.
