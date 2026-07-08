# CleanHub 현황·핸드오프 (STATUS)

> 새 세션이 맥락을 빠르게 잡기 위한 요약. 상세는 git 커밋 메시지 + `worklog/2026-07-HIS.md` 참고.
> 최종 갱신: 2026-07-08 (계약 도메인 추가)

## 프로젝트 한 줄
청소업(계단·입주·물탱크 청소) **견적·정산 관리 시스템**. 대민(고객) + 관리자(아버님 실무).
Spring Boot 2.7(Java 8, WAR) + Vue3 + PostgreSQL 18(Flyway). 패키지 `com.boot.cleanhub`. 포트 70.

## 지금까지 완료 (커밋 순)
1. **초기 세팅**(3a110c5) — head 파생, 데모 전부 제거, PostgreSQL+Flyway, admin 계정 시드.
2. **거래처 도메인**(711ad82) — 첫 도메인 수직완성(백→프론트). 대민/관리자 영역 분리.
3. **자동로그인**(e6c450e) — refreshToken HttpOnly 쿠키, 새로고침·재시작 시 로그인 유지.
4. **계약 도메인**(미커밋) — 거래처 1:N 참조 정기 월정액 계약. 관리자 CRUD 수직완성. Flyway V4. (worklog 2026-07-08 참고)

## 확정된 설계 결정 (바꾸지 말 것 — 이미 합의·구현됨)

### 인증 구조 (세션 앵커 + JWT API + refresh 쿠키)
| 자산 | 저장 | 수명 | 역할 |
|---|---|---|---|
| 세션(JSESSIONID) | HttpOnly 쿠키 | 브라우저 세션 | 브라우저 열린 동안 로그인 상태·사용자정보 |
| access(JWT) | 프론트 메모리 | 1시간 | `/api/**` Bearer 인증 |
| refresh(JWT) | HttpOnly 쿠키 + DB | 7일 | 자동로그인, access 재발급 |
- 로그인: `/auth/login`(세션) → `/auth/api/token`(JWT 발급, refresh는 쿠키). "로그인 유지" 체크박스=rememberMe.
- 자동로그인: 앱 시작 시 `main.js`가 `auth.restore()` → `/api/auth/refresh`(쿠키) → 세션 없어도 복원.
- 401 시: axios 응답 인터셉터가 refresh로 자동 갱신+재시도(single-flight).
- **상세 흐름: [auth-flow.md](auth-flow.md)** — 코드레벨 5시나리오(로그인/API호출/자동갱신/복원/로그아웃).

### 영역 분리 (프론트)
- 대민: `PublicLayout` 아래 `/`, `/auth`(연습 데모) — 로그인 불필요.
- 관리자: `AdminLayout`(사이드바) 아래 `/admin/**` — ROLE_ADMIN 라우터 가드. 로그인 `/admin/login`.
- UI 톤: teal(청록) 브랜드색, 폼/상세는 가운데정렬·목록은 full-width. 디자인토큰 `style.css`.

### 도메인 컨벤션
- 백엔드: `com.boot.cleanhub.<도메인>/`(domain·repository·service·dto·controller). 관리자 API는 `/api/admin/<도메인>` (자동으로 ROLE_ADMIN 보호). 스키마는 Flyway V*(엔티티와 일치, ddl-auto=validate).
- 프론트: `views/admin/<도메인>/`, `services/<도메인>/`. 목록은 vue-query 캐싱.
- 참고 템플릿: **거래처(client)** 도메인 전체가 이후 도메인의 본보기.

## 도메인 로드맵 (MVP 기준)
```
[완료]  거래처(client) — 건물·고객
[완료]  계약(contract) — 거래처 1:N, 정기 월정액 계약(계약명·월금액·청구일·기간·상태)
[다음]  견적(quote) — 일회성 특수청소(입주·물탱크 등) 견적
[이후]  정산(settlement) — 계약+견적 집계, 월 청구, 엑셀 출력
[2단계] 대민 견적요청, 후기·건의/문의(게시판 계열)
[3단계] 대시보드 통계, PDF 견적서, 이메일 알림
```
- 엑셀 정산 양식: 아버님 실제 양식 **미수령**(추후 받아서 정산화면에 반영).
- 거래처 필드는 초안 상태 — 실무 피드백으로 조정 예정.

## 로컬 개발 환경
- DB: PostgreSQL 18, `cleanhub` DB / `cleanhub_user`(비번은 db.properties) / pgcrypto 확장. 셋업은 README 참고.
- 빌드/실행: `/build` 스킬(`clean compile`·`spring-boot:run`). 로컬 검증 포트 오버라이드 `SERVER_PORT=7071`.
  - 머신 설정은 `.claude/scripts/build.env.ps1`(gitignore, 이 PC엔 이미 있음).
- 프론트: `frontend/vue`에서 `npm run dev`(:5173, /api·/auth 프록시) 또는 `npm run build`(백엔드 :70 서빙 반영).
- admin 계정: `admin` / `admin1234` (Flyway V2 시드).

## 알아둘 것 (트랩)
- `${username}` 등 일반 플레이스홀더는 Windows USERNAME 환경변수와 충돌 → db.properties는 `db.` 접두사.
- 커밋 시 `.vscode/launch.json`은 제외(IDE 설정). node_modules·static/app는 gitignore.
- Git Bash curl로 한글 JSON 보내면 CP949로 깨짐 → 테스트는 UTF-8 파일(`--data-binary @file`)로.
- refresh 쿠키 Secure는 개발 false(http localhost), 운영은 env `REFRESH_COOKIE_SECURE=true`.

## 다음 세션 시작하는 법
1. `D:\private\cleaning-service`에서 `claude` 새로 실행(가벼운 새 세션).
2. 첫 메시지: "이 문서(.claude/docs/STATUS.md)와 worklog 최신 것 읽고 이어서 작업하자. 다음은 <계약/견적/...> 하고 싶어."
3. CLAUDE.md는 자동 로드됨. 코드 히스토리는 `git log`, 작업 이력은 worklog.
