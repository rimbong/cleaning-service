# 설정 프로파일 가이드 (dev / prod)

이 프로젝트는 Spring Boot **프로파일(profile)** 로 개발/운영 환경 설정을 분리합니다.
처음 보는 분도 이 문서만 보면 어떤 파일이 무엇이고, 어떻게 실행하는지 알 수 있습니다.

## 1. 설정 파일 구조

같은 폴더(`src/main/resources/`)에 3개의 YAML 이 있습니다.

| 파일 | 역할 | 언제 적용되나 |
|---|---|---|
| `application.yml` | **공통 설정** (모든 환경 공유) + 기본 프로파일 지정 | 항상 |
| `application-dev.yml` | **개발** 전용 오버라이드 | 프로파일이 `dev` 일 때 |
| `application-prod.yml` | **운영** 전용 오버라이드 | 프로파일이 `prod` 일 때 |

동작 원리: Spring Boot 는 `application.yml` 을 먼저 읽고, **활성 프로파일에 해당하는 `application-{프로파일}.yml` 을 자동으로 찾아 덮어씁니다.**
- 예) 프로파일이 `prod` → `application.yml` + `application-prod.yml` 병합 (겹치는 값은 prod 가 이김)

> 기본 프로파일은 `dev` 입니다. (`application.yml` 의 `spring.profiles.active: ${SPRING_PROFILES_ACTIVE:dev}`)
> → **아무 설정도 안 하면 자동으로 `dev` 로 실행됩니다.**

## 2. 환경별 차이

| 항목 | dev (개발) | prod (운영) |
|---|---|---|
| 로그 저장 경로 | `./logs` (실행 폴더 기준, 하드코딩) | 환경변수 **`APP_LOG_PATH`** (미지정 시 `/var/log/spring-boot-head`) |
| 로그 레벨 | debug / trace (상세) | info (간결) |
| Thymeleaf 캐시 | OFF (수정 즉시 반영) | ON (성능) |
| H2 콘솔(`/h2-console`) | 사용 가능 | 비활성화 |

## 3. 실행 방법

### 3-1. 개발(dev) 으로 실행 — 그냥 실행
별도 설정이 필요 없습니다. 기본이 dev 입니다.

```bash
# 프로젝트 루트(project/)에서
mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run          # Mac/Linux
```

### 3-2. 운영(prod) 으로 실행 — 프로파일만 바꾸면 됨

아래 중 **한 가지** 방법만 쓰면 됩니다.

**① Maven 으로 실행할 때**
```bash
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=prod
```

**② 패키징된 war/jar 를 실행할 때 (실행 인자)**
```bash
java -jar project-0.0.1-SNAPSHOT.war --spring.profiles.active=prod
```

**③ 환경변수로 (서버 배포 시 권장)**
```bash
# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:APP_LOG_PATH = "D:/logs/app"     # 운영은 로그 경로도 함께 지정
java -jar project-0.0.1-SNAPSHOT.war

# Windows cmd
set SPRING_PROFILES_ACTIVE=prod
set APP_LOG_PATH=D:/logs/app
java -jar project-0.0.1-SNAPSHOT.war

# Mac/Linux
export SPRING_PROFILES_ACTIVE=prod
export APP_LOG_PATH=/var/log/app
java -jar project-0.0.1-SNAPSHOT.war
```

**④ JVM 옵션(-D) 으로**
```bash
java -Dspring.profiles.active=prod -DAPP_LOG_PATH=/var/log/app -jar project.war
```

**⑤ IDE (IntelliJ / STS) 에서**
- Run/Debug Configuration →
  - **VM options**: `-Dspring.profiles.active=prod`
  - 또는 **Environment variables**: `SPRING_PROFILES_ACTIVE=prod;APP_LOG_PATH=D:/logs/app`

**⑥ 외장 톰캣(WAR 배포) 에서**
- `bin/setenv.bat`(Windows) 또는 `bin/setenv.sh`(Linux) 에 추가:
```
set CATALINA_OPTS=-Dspring.profiles.active=prod -DAPP_LOG_PATH=D:/logs/app   # Windows
export CATALINA_OPTS="-Dspring.profiles.active=prod -DAPP_LOG_PATH=/var/log/app"  # Linux
```

### 3-3. 우선순위
여러 방법을 동시에 지정하면 아래 순서로 **위쪽이 우선**합니다.
```
실행 인자(--spring.profiles.active)  >  JVM 옵션(-D)  >  환경변수  >  application.yml 기본값(dev)
```

## 4. 어떤 프로파일로 떴는지 확인
애플리케이션 시작 로그에 다음 줄이 출력됩니다.
```
The following 1 profile is active: "prod"
```
(dev 이면 `"dev"` 로 표시)

## 5. 새 환경(예: staging) 추가하려면
1. `application-staging.yml` 파일을 이 폴더에 만들고, 바꿀 값만 작성
2. 실행 시 프로파일을 `staging` 으로 지정 (`SPRING_PROFILES_ACTIVE=staging` 등)

끝. 공통 값은 `application.yml`, 환경별로 다른 값만 `application-{프로파일}.yml` 에 두면 됩니다.
