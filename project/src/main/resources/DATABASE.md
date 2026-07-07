# 데이터베이스 & 마이그레이션 가이드

이 문서는 현재 DB 구성과, 운영용 **스키마 버전 관리(Flyway)** 채택 방법을 설명합니다.

## 1. 현재 구성 (개발 기본)

| 항목 | 값 | 설명 |
|---|---|---|
| DB | **H2 (인메모리)** `jdbc:h2:mem:testdb` | 재기동 시 초기화. 개발/데모용 |
| 스키마 생성 | `spring.jpa.hibernate.ddl-auto=update` | JPA 엔티티로 테이블 자동 생성/수정 |
| 초기 데이터 | `schema.sql` / `data.sql` | 기동 시 실행 (`spring.sql.init`) |
| Batch 메타테이블 | `spring.batch.jdbc.initialize-schema=always` | BATCH_* 자동 생성 |
| **Flyway** | **비활성** (`spring.flyway.enabled=false`) | 아래 절차로 채택 |

> 개발은 이대로 두면 됩니다. 아래는 **운영에서 영속 DB + 스키마 버전 관리**가 필요할 때의 절차입니다.

## 2. Flyway 채택 절차 (운영/영속 DB)

Flyway 는 `db/migration/V{버전}__{설명}.sql` 스크립트를 순서대로 적용하고 이력을 관리합니다.
JPA 자동 생성(ddl-auto)과 **동시에 쓰면 충돌**하므로, 채택 시 아래처럼 역할을 넘깁니다.

### 2-1. 설정 변경 (예: application-prod.yml)
```yaml
spring:
  flyway:
    enabled: true
  jpa:
    hibernate:
      ddl-auto: validate      # 스키마 생성은 Flyway 가 담당, JPA 는 검증만
  sql:
    init:
      mode: never             # schema.sql/data.sql 자동 실행 중지 (Flyway 로 이관)
  datasource:                 # 영속 DB 로 교체 (예: MySQL/PostgreSQL)
    url: jdbc:mysql://... 
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

### 2-2. 마이그레이션 스크립트 작성
`src/main/resources/db/migration/` 에 버전 순으로 작성합니다.
- `V1__baseline.sql` — 최초 전체 스키마 (현재 JPA 엔티티에 대응하는 CREATE TABLE 들)
- `V2__add_xxx.sql` — 이후 변경마다 새 파일 추가 (기존 파일 수정 금지)

> 팁: 최초 V1 은 개발 H2 에서 `ddl-auto` 로 생성된 스키마를 export 해서 시작하면 편합니다.
> (H2 콘솔에서 `SCRIPT TO 'schema.sql'` 또는 Hibernate `hibernate.hbm2ddl.auto=create` + SQL 로그)

### 2-3. 기존 DB 에 처음 도입할 때
이미 테이블이 있는 DB 라면:
```yaml
spring:
  flyway:
    baseline-on-migrate: true
    baseline-version: 1
```

## 3. 요약
- **개발**: 지금 그대로(H2 + ddl-auto). Flyway 신경 쓸 것 없음.
- **운영 채택**: `flyway.enabled=true` + `ddl-auto=validate` + `sql.init.mode=never` + `V*.sql` 작성.
- 원칙: **한 번 적용된 마이그레이션 파일은 절대 수정하지 않고, 변경은 새 버전 파일로.**
