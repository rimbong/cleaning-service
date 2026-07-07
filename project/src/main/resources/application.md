# Spring Boot 설정 문서 (`application*.yml`)

이 문서는 이 프로젝트의 설정 파일 구조와 각 설정 항목의 의미, 개발(dev)/운영(prod) 환경에서의 값을 설명합니다.

> 관련 문서: 프로파일 실행법은 [`PROFILES.md`](PROFILES.md), DB/마이그레이션은 [`DATABASE.md`](DATABASE.md) 참고.

## 0. 먼저 — 스프링 부트 자동 설정
- **과거 방식**: `ViewResolver`, `DataSource`, `TransactionManager` 등 모든 Bean을 XML/Java로 직접 등록.
- **부트 방식**: `spring-boot-starter-*` 의존성을 추가하면, 스프링 부트가 라이브러리를 감지해 관련 Bean을 **합리적 기본값으로 자동 등록**.
- 개발자는 `application.yml`에 **바꾸고 싶은 값만 명시**해 덮어씁니다. 이 문서의 대부분은 그 "덮어쓰기"입니다.

---

## 1. 파일 구조 (프로파일 분리) ⭐

설정은 **환경 공통 / 개발 / 운영** 3개 파일로 나뉩니다. 같은 폴더(`src/main/resources/`)에 있습니다.

| 파일 | 역할 | 적용 시점 |
|---|---|---|
| `application.yml` | **모든 환경 공통** + 기본 프로파일 지정 | 항상 |
| `application-dev.yml` | **개발** 전용 오버라이드 (H2, 상세 로그 등) | 프로파일 `dev` |
| `application-prod.yml` | **운영** 전용 오버라이드 (실 DB, 최소 로그 등) | 프로파일 `prod` |

동작: 부트가 `application.yml`을 읽고 **활성 프로파일에 맞는 `application-{프로파일}.yml`을 자동 병합**(겹치면 프로파일 값이 우선).
- 기본 프로파일은 **dev** (`spring.profiles.active: ${SPRING_PROFILES_ACTIVE:dev}`) → 아무 설정 없이 실행하면 dev.
- 운영: `SPRING_PROFILES_ACTIVE=prod` (또는 `-Dspring.profiles.active=prod`).

### DB·Actuator·로그(path/level)는 프로파일별에만 있음
현재 DB는 **개발 기능 테스트용 H2**라 `application.yml`(공통)엔 datasource가 없고, **dev/prod 파일에 각자** 둡니다. (나중에 개발/운영 DB를 분리하기 위함)

---

## 2. 설정값의 출처 — `${...}` 플레이스홀더

값이 `${...}`로 되어 있으면 **외부에서 주입**됩니다. 문법은 `${키:기본값}`(키가 없으면 기본값).

| 출처 | 예 | 로드 방식 |
|---|---|---|
| **환경변수** | `${SPRING_PROFILES_ACTIVE:dev}`, `${DB_URL}`, `${APP_LOG_PATH}`, `${CORS_ALLOWED_ORIGINS:...}` | OS env / `-D` 옵션 |
| **`db.properties`** | `${url}`, `${username}`, `${maximum-pool-size}` 등 (H2 접속·풀 값) | `PropertySourceConfig`가 로드 |
| **`etc.properties`** | `${mail.host}`, `${mail.password}`, `${jwt.secret}` 등 | `PropertySourceConfig`가 로드 |

> `etc.properties` 안에서도 다시 `${ENV:개발기본}` 형태로 **환경변수 오버라이드**를 씁니다.
> 예: `jwt.secret=${JWT_SECRET:개발용키}`, `mail.password=${MAIL_PASSWORD:}` → 운영은 env로 주입, 소스엔 평문 없음.

---

## 3. 공통 설정 — `application.yml`

### `spring.messages`
- **`basename: i18n/messages`, `encoding: UTF-8`**: i18n 메시지 번들 위치/인코딩. Thymeleaf `#{...}`와 **API 에러/검증 메시지**(`messages_ko/en.properties`의 `error.*`, `validation.*`)를 서버에서 해석하는 `MessageSource`.

### `spring.application.name: project`
- 애플리케이션 식별자(로깅/모니터링용).

### `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:dev}`
- **활성 프로파일**. 기본 dev, 운영은 env로 prod.

### `spring.batch.job.enabled: false`
- 기동 시 Batch Job 자동 실행 방지(엔드포인트로 수동 실행). ※ Batch 메타테이블 생성은 프로파일별(`spring.batch.jdbc.initialize-schema`).

### `spring.mail.*`
- `spring-boot-starter-mail`의 SMTP 설정. 값은 `etc.properties`의 `mail.*`(=env 오버라이드)에서 옴.
- `host/port/username/password`, `properties.mail.smtp.auth`, `...starttls.enable`, `mail.mime.charset=UTF-8`.
- **실제 발송하려면** 운영에서 `MAIL_HOST`/`MAIL_USERNAME`/`MAIL_PASSWORD` 등을 env로 주입.

### `spring.devtools`
- `livereload`/`restart` 활성. 개발 편의(운영 jar엔 devtools가 포함되지 않아 무영향).

### `spring.jpa` (DB 무관 공통 동작)
- **`open-in-view: false`**: OSIV 비활성. 영속성 컨텍스트를 트랜잭션 범위로 한정(성능·안전). dev/prod 모두 false 권장.
- **`show-sql: false`**: SQL 콘솔 직접 출력은 끄고, SQL 로그는 `logging.level`(hibernate)로 제어.
- **`properties.hibernate.*`**:
  - `format-sql: true`: SQL 보기 좋게 정렬.
  - `use-new-id-generator-mappings: false`: ID 생성 전략 하위호환.
  - `'[jdbc.batch_size]': 10`: INSERT/UPDATE/DELETE를 10개씩 모아 전송(쓰기 지연). 키에 `.`이 있어 `'[...]'`로 감쌈.
- ※ **연결/방언(dialect)/`ddl-auto`는 DB에 종속**이라 프로파일별(dev/prod)에 있음.

### `spring.servlet.multipart`
- 파일 업로드 제한: `max-file-size: 20MB`(단일), `max-request-size: 40MB`(요청 총합), `file-size-threshold: 20MB`(이 크기 넘으면 디스크에 기록).

### `spring.web.resources.add-mappings: false`
- 부트 **기본 정적 리소스 매핑을 끔**. 정적 경로는 `WebConfig.addResourceHandlers`에 **명시 등록한 것만** 접근 가능(`/app/**`(SPA), `/static/**`). 보안상 경로 통제 목적.
- ⚠️ 이 설정 때문에 **Swagger UI 정적 리소스**가 안 뜰 수 있음(springdoc이 자체 핸들러를 등록하지만 확인 필요).

### `spring.mvc`
- **`throw-exception-if-no-handler-found: true`**: 매칭 핸들러가 없으면 `NoHandlerFoundException` 발생 → `add-mappings:false`와 함께 **미정의 URL을 명확히 404**로. (`CustomErrorController`가 API면 JSON, 페이지면 HTML로 응답)
- **`view.prefix: /app/`, `view.suffix: .html`**: (SPA 정적 뷰 연동용) 컨트롤러가 뷰 이름 반환 시 접두/접미사. 실제 SPA 서빙·딥링크 폴백은 `WebConfig`의 `/app/**` 리소스 핸들러가 담당(Thymeleaf 뷰는 별도 resolver 우선).

### `spring.thymeleaf`
- **`prefix: classpath:/templates/`, `suffix: .html`**: 템플릿 위치/확장자.
- ※ **`cache`는 프로파일별**(dev: false 즉시반영 / prod: true 성능).

### `mybatis`
- **`config-location: classpath:mybatis/context/sql-config.xml`**: MyBatis 전역 설정.
- **`mapper-locations: classpath:mybatis/mapper/**/*.xml`**: Mapper XML 위치(하위 전체).
- **`type-aliases-package: com.boot.cleanhub.common.dto`**: Mapper XML에서 클래스명(camelCase)만으로 참조 가능한 DTO 패키지.

### `server`
- **`port: 70`**: 내장 톰캣 포트. (운영은 보통 80/443 또는 리버스 프록시 뒤)
- **`servlet.context-path: /`**, **`session.timeout: 30m`**.
- **`servlet.encoding.*`**: 요청/응답 UTF-8 강제(`enabled/charset/force`) → 한글 깨짐 방지.

### `cors.allowed-origins` (커스텀)
- **CORS 허용 오리진**(콤마 구분). `CorsProperties`(@ConfigurationProperties)가 바인딩 → `CorsConfig`가 Security와 연동.
- 기본값은 개발용 로컬 프론트(`localhost:5173`, `localhost:3000`). 운영은 `CORS_ALLOWED_ORIGINS` env로 오버라이드.

### `logging`
- **파일 분리·롤링·보관정책·패턴은 `logback-spring.xml`(운영용)에서 설정**. yml 은 레벨/경로만 담당.
- **`file.path`(경로)와 `level`(레벨)은 프로파일별**(application-dev / application-prod).
  - `logging.file.path` → logback 이 `<springProperty>` 로 읽어 로그 디렉터리로 사용.
  - `logging.level.*` → logback 설정과 무관하게 Spring 이 최종 적용.

#### `logback-spring.xml` (로그 파일 구성)
- 파일명이 `-spring` 접미사인 이유: Spring 확장 태그(`<springProfile>`, `<springProperty>`)를 쓰기 위함.
- 생성 파일(`logging.file.path` 아래):
  - **`project-info.log`**: INFO 이상 전체(운영 상시 확인).
  - **`project-error.log`**: ERROR 만 격리(장애 추적 — 이 파일만 열면 에러만).
  - **`project-debug.log`**: DEBUG 이상(**dev 프로파일에서만**).
  - 각 파일은 날짜+크기(100MB) 롤링, `archive/` 로 이동, `maxHistory`/`totalSizeCap` 으로 자동 삭제.
- 프로파일 배선: **dev** = 콘솔+info+error+debug / **prod** = 콘솔+info+error / 그 외 = 콘솔만.

---

## 4. 개발 설정 — `application-dev.yml`

| 항목 | 값 | 의미 |
|---|---|---|
| `spring.datasource.*` | **H2 인메모리** (`${url}`=`jdbc:h2:mem:testdb`, `sa`) + Hikari 풀 | 기능 테스트용 DB (값은 `db.properties`) |
| `spring.jpa.database-platform` | `H2Dialect` | H2 방언 |
| `spring.jpa.defer-datasource-initialization` | `true` | 엔티티 생성 **후** `schema.sql`/`data.sql` 실행 |
| `spring.jpa.hibernate.ddl-auto` | **`update`** | 엔티티로 스키마 자동 생성/수정(개발 편의) |
| `spring.sql.init.encoding` | `utf-8` | 초기 스크립트 인코딩 |
| `spring.batch.jdbc.initialize-schema` | `always` | H2에 `BATCH_*` 메타테이블 자동 생성 |
| `spring.flyway.enabled` | `false` | 개발은 Flyway 미사용(ddl-auto로 충분) |
| `spring.thymeleaf.cache` | `false` | 템플릿 수정 즉시 반영 |
| `spring.h2.console.enabled` | `true` | `/h2-console` 사용 |
| `logging.file.path` | `./logs` | 실행 폴더 기준(하드코딩) |
| `logging.level.*` | debug / `org.hibernate.type: trace` | 상세 로그(SQL·바인딩 값까지) |
| `management` | `health,info,metrics,env,beans,mappings` + `show-details: always` | Actuator 넓게 노출(디버깅) |

### `ddl-auto` 값 참고
`none`(작업안함) / `update`(변경 반영·데이터 보존) / `create`(재생성) / `create-drop`(종료 시 삭제) / `validate`(스키마 검증만).
→ **개발 `update`, 운영 `validate`(또는 none)**.

---

## 5. 운영 설정 — `application-prod.yml`

> ⚠️ prod는 **실제 운영 인프라 전제**. `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` 환경변수와 `db/migration/V*.sql`이 있어야 정상 기동(→ `DATABASE.md`).

| 항목 | 값 | 의미 |
|---|---|---|
| `spring.thymeleaf.cache` | `true` | 템플릿 캐시(성능) |
| `spring.h2.console.enabled` | `false` | H2 콘솔 차단(보안) |
| `spring.datasource.*` | `${DB_URL}` 등 | **실 운영 DB** (env 주입, MySQL/PostgreSQL 등) |
| `spring.jpa.hibernate.ddl-auto` | **`validate`** | 스키마는 Flyway가 관리, JPA는 검증만 |
| `spring.sql.init.mode` | `never` | `schema.sql`/`data.sql` 자동 실행 중지 |
| `spring.flyway.enabled` | `true` | `db/migration/V*.sql` 적용 |
| `logging.file.path` | `${APP_LOG_PATH:/var/log/spring-boot-head}` | 로그 경로 env 주입 |
| `logging.level.*` | 전부 `info` | 개발용 debug/trace 낮춤 |
| `management` | `health,info` + `show-details: when-authorized` | Actuator 최소 노출(보안) |

---

## 6. yml에 없지만 관련된 설정
- **springdoc(Swagger UI)**: yml 설정 없이 `config/OpenApiConfig`(애너테이션)로 구성. `/swagger-ui.html`, `/v3/api-docs`.
- **JWT / 시크릿**: `etc.properties`의 `jwt.*`, `mail.*` (env 오버라이드).
- **Actuator 상세**: [Spring Boot Actuator 문서] 참고. 현재는 `health`,`info`만 노출.

---

## 7. 요약 — "어디를 바꿔야 하나"
| 바꾸려는 것 | 위치 |
|---|---|
| 모든 환경 공통 (포트, mybatis, multipart, cors 기본…) | `application.yml` |
| 개발 DB/로그/캐시/Actuator | `application-dev.yml` |
| 운영 DB/로그/캐시/Actuator/Flyway | `application-prod.yml` |
| H2 접속·풀 값 | `db.properties` |
| 메일/JWT/시크릿 (+env 오버라이드) | `etc.properties` |
| 운영 비밀·경로 (DB_URL, APP_LOG_PATH, JWT_SECRET, CORS…) | **환경변수** |
