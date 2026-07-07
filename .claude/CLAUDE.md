# CLAUDE.md

이 파일은 이 저장소에서 작업하는 Claude Code(및 기타 AI 에이전트)를 위한 가이드입니다.

## 프로젝트 개요

**CleanHub** — 청소업(건물 계단 청소·입주청소·물탱크청소 등) **견적·정산 관리 시스템**.
- **대민(고객)**: 견적 요청·진행 조회·후기·건의
- **관리자**: 견적 처리·거래처/계약 관리·월 청구/정산(+엑셀)·후기/문의 관리·대시보드

`spring_boot_head_mvn`(범용 프레임워크 헤드)에서 파생해 시작했으며, 데모(testcase)는 전부 제거하고
공용 인프라(인증·알림·엑셀·메일 등)만 남긴 상태에서 실제 도메인을 얹어간다.

- **언어/런타임**: Java 1.8
- **프레임워크**: Spring Boot 2.7.18 (Spring Security, Spring Data JPA, Thymeleaf)
- **빌드**: Maven (Maven Wrapper 포함), 패키징은 **WAR**
- **DB**: **PostgreSQL 18** (개발: `localhost:5432/cleanhub`, 스키마는 **Flyway**로 관리)
- **영속성**: JPA(Hibernate) 주력 + MyBatis 설정 유지(필요 시 사용)
- **인증**: 세션 기반 + JWT 기반 두 가지 SecurityFilterChain 공존
- **기본 패키지**: `com.boot.cleanhub`

> ⚠️ 빌드/실행 명령은 모두 **`project/` 하위**에서 실행. `pom.xml`이 `project/`에 있음.
> 빌드는 `/build` 스킬(`.claude/scripts/build.ps1`, 머신설정은 gitignore된 `build.env.ps1`) 사용.

## 빌드 / 실행

```bash
# 빌드/실행은 /build 스킬 사용 (권장)
#   clean compile  /  clean package  /  spring-boot:run
# 산출물: project/target/cleanhub-0.0.1-SNAPSHOT.war
```
- **서버 포트**: `70` (application.yml). 로컬 검증 시 `SERVER_PORT=7071` 등으로 오버라이드.
- **프로파일**: 기본 `dev`(PostgreSQL 로컬). 운영 `prod`(env로 DB 주입).

## DB / 마이그레이션 (PostgreSQL + Flyway)

- 개발 접속값: `db.properties`(평문, `db.*` 접두사). ⚠️ 키에 `db.` 접두사 필수 —
  `${username}` 같은 일반명은 **Windows 환경변수 USERNAME 과 충돌**해 OS 사용자로 접속됨.
- 스키마 소스 = **Flyway**(`resources/db/migration/V*.sql`), `ddl-auto: validate`(JPA는 검증만).
  스키마 변경 = 새 마이그레이션(V3, V4 …) 추가. 엔티티와 SQL이 일치해야 기동됨.
- 최초 admin 계정은 `V2__seed_admin.sql`이 pgcrypto `crypt()`로 BCrypt 해시 생성(`admin/admin1234`, ROLE_ADMIN).
  → DB 생성 시 `CREATE EXTENSION pgcrypto` 필요(슈퍼유저).
- 로컬 DB 셋업: `cleanhub` DB + `cleanhub_user`(1234) + pgcrypto 확장.

## 디렉토리 구조

소스 루트: `project/src/main/java/com/boot/cleanhub/`

```
auth/          # 정식 인증 모듈 — 세션 로그인(auth_user)·JWT 발급/갱신/폐기 (domain/repository/service/controller/dto)
common/        # 공통 인프라
  api/         # ApiResponse (표준 응답 envelope)
  controller/  # SuperController (레거시 스타일 부모)
  dao/         # CommonDaoIF/Impl, SuperDao — MyBatis 공통 CRUD
  dto/         # PBox, PBoxList, PLog, PSession — 데이터 컨테이너
  handler/     # JwtAuthenticationEntryPoint(401), JwtAccessDeniedHandler(403)
  mail/        # MailService, MailTemplateRenderer (이메일)
  notification/# SSE 알림 인프라
  aop/         # 실행시간 추적(TimeTrace)
config/        # @Configuration — Security 2체인, Cors, Web, DataBase, PropertySource, OpenApi 등
filter/        # JwtRequestFilter (Bearer 검증)
interceptor/   # EventCheckInterceptor — 모든 요청 전처리
error/         # 전역 예외 처리 (BizException, GlobalExceptionHandler, ErrorCode)
util/          # 정적 유틸 (date, excel(POI), http, json, jwt, security(RSA/TEA), xml, file, image)
```
※ **도메인 코드(견적·거래처·계약·정산·후기 등)는 아직 없음** — 여기에 새로 추가한다.
   패키지는 `com.boot.cleanhub.<도메인>` (예: `quote`, `client`, `contract`, `settlement`, `review`).

리소스: `project/src/main/resources/`
- `application.yml`(공통) + `application-dev.yml`/`application-prod.yml`(프로파일별)
- `db.properties`·`etc.properties` — `PropertySourceConfig`로 로드, yml `${...}` 치환
- `db/migration/` — Flyway 마이그레이션
- `logback-spring.xml` — 운영용 로깅(레벨별 파일 분리·롤링·보관)
- `i18n/messages*.properties` — 다국어 메시지
- `mybatis/context/sql-config.xml` — MyBatis 설정(매퍼는 필요 시 추가)

## 프론트엔드 (frontend/vue)

- **Vue 3 + Vite + Pinia + vue-router + vue-i18n + axios + vue-query**
- 백엔드 `/app` 경로로 서빙(`base:'/app/'`, 빌드 산출물 → `project/.../static/app`).
  dev 서버(:5173)는 `/api`·`/auth` 를 백엔드(:70)로 프록시.
- 공용 인프라(유지): `stores/auth`·`stores/notify`, `services/auth`, `composables/http/useRequest`,
  `plugins/http/axios`(Bearer 자동첨부 + 401 자동갱신 인터셉터), `components/notify/NotifyHost`, i18n.
- 도메인 화면은 `views/<도메인>/`, 서비스는 `services/<도메인>/`, 스토어는 `stores/<도메인>/`.

## 핵심 아키텍처 패턴

### 이중 Spring Security 체인 (config/)
- `JwtApiSecurityConfig`(@Order 1, `/api/**`): STATELESS, `JwtRequestFilter`가 Bearer 검증(클레임 기반).
  미인증=401 JSON(EntryPoint), 권한부족=403 JSON(AccessDeniedHandler). `/api/admin/**`=hasRole ADMIN.
- `SessionSecurityConfig`(@Order 2, 그 외): 폼 로그인(`POST /auth/login`, JSON 응답), 세션 기반, BCrypt.
  `/admin` 페이지는 hasRole ADMIN(미인증→로그인 리다이렉트, 권한부족→403 페이지). `GET /login`→구성값 리다이렉트.
- 인증 흐름: 세션 로그인 → `POST /auth/api/token`으로 JWT 발급 → `/api/**` Bearer 호출.
  토큰: access(짧음)+refresh(김, DB저장). 갱신 `/api/auth/refresh`(회전 옵션 `jwt.refresh-token-rotation`).
- 신규 보안 규칙 추가 시 어느 체인(`/api/**` 여부)인지 먼저 확인.

### PBox — 범용 데이터 컨테이너 (레거시)
`common/dto/PBox`는 `HashMap<String,Object>` 상속 만능 객체. MyBatis 경로에서 DTO 대신 사용 가능.
> 방침: 신규 도메인은 **JPA + DTO(모던 스타일)** 우선. 간단한 것만 PBox 허용.

## 코딩 컨벤션

- **주석/문서는 한국어.** 클래스 상단 `<pre>` Javadoc + `@author`, `@since`, `@version` 관례.
- **Lombok**: `@RequiredArgsConstructor` 생성자 주입 선호. 빌드 산출물에는 lombok 제외.
- **컨트롤러**: 클래스 `@RequestMapping`, 메서드 `@GetMapping/@PostMapping`. API는 `ApiResponse` envelope.
- **Java 8 문법까지만** (`javax.*`, Jakarta 아님).
- 명명: 변수·메서드 camelCase, 상수 SCREAMING_SNAKE_CASE, 클래스 PascalCase, 패키지 소문자.
- K&R 중괄호, 한 줄=한 문장, 빈 catch 금지, 와일드카드 import 금지, `===`/`const`(JS).

## 작업 시 주의사항

- **새 도메인 추가**: `com.boot.cleanhub.<도메인>/`(controller/service/repository/domain/dto),
  스키마는 **Flyway 마이그레이션(V*.sql)** 으로 추가(엔티티와 일치 필수), 프론트는 `views/<도메인>/`.
- 설정값은 `application.yml`(공통)/프로파일 yml, 상세는 `config/`의 `@Configuration`.
- `util/`에 직접 구현 정적 유틸 다수(POI·RSA/TEA·HTTP 등) — 새 유틸 전 중복 확인.
- 로그: `logback-spring.xml`(파일 구성) + yml `logging.level.*`(레벨). 경로는 프로파일별.
- 작업 이력은 `/worklog` 스킬로 `.claude/docs/worklog/`에 기록.
