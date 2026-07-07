---
name: build
description: Build THIS single-module Spring Boot project by running .claude/scripts/build.ps1 (sets JAVA_HOME, uses configured Maven + settings.xml, runs goals; default 'clean package'). Machine-specific paths and any SSL workaround come from .claude/scripts/build.env.ps1 (gitignored). Use to compile/package or verify the build. - 이 프로젝트를 Maven으로 빌드(clean package)/컴파일할 때. "빌드", "패키지", "clean package", "컴파일 검증".
when_to_use: 프로젝트를 컴파일/패키징(clean package)하거나 빌드가 통과하는지 검증할 때.
allowed-tools: PowerShell, Bash, Read, Edit
argument-hint: [maven goals — 기본 clean package]
---

# 빌드 (/build)

이 **단일모듈 Spring Boot** 프로젝트를 `.claude/scripts/build.ps1` 래퍼로 빌드한다.
JAVA_HOME·Maven 실행파일·settings.xml·(사내망)SSL 우회 등 **머신 전용값은 `.claude/scripts/build.env.ps1`**(gitignore)에서 읽는다.

## 실행
```
powershell -NoProfile -ExecutionPolicy Bypass -File .claude/scripts/build.ps1 [goals...]
```
- goals 생략 → 기본 `clean package`.
- 예: `clean compile`(빠른 컴파일 검증), `-DskipTests package`(테스트 스킵), `spring-boot:run`(실행).

## 절차
1. **env 확인**: `.claude/scripts/build.env.ps1` 존재 확인.
   - 없으면 → `build.env.example.ps1` 를 복사해 `build.env.ps1` 생성하도록 안내(최소 `$BUILD_JAVA_HOME` 필요).
2. **빌드 실행**: 위 명령으로 `build.ps1 [goals]` 실행.
3. **결과 요약**: `[build] SUCCESS` 또는 실패 시 로그의 **첫 번째 에러**를 짚어 원인·해결 안내.
   - 산출물(성공 시): `project/target/cleanhub-0.0.1-SNAPSHOT.war`

## 자주 나는 이슈
- **PKIX / SSL 인증서 실패**(사내망): `build.env.ps1` 의 `$BUILD_MVN_EXTRA_ARGS = @('-Daether.connector.https.securityMode=insecure')` 로 우회.
  근본해결은 프록시 CA 인증서를 JDK cacerts 에 등록(그 후 EXTRA_ARGS 비우기).
- **JAVA_HOME 미설정**: `build.env.ps1` 의 `$BUILD_JAVA_HOME` 설정.
- **mvnw 가 기본 .m2/central 로만 붙어 실패**: `build.env.ps1` 의 `$BUILD_MVN_CMD`(실제 mvn)·`$BUILD_MVN_SETTINGS` 지정.

## 비고
- **절대경로·SSL 우회 플래그는 `build.env.ps1`(gitignore)에만.** 커밋되는 `build.ps1`·`build.env.example.ps1` 에는 넣지 않는다(다른 사람에게 SSL 강제 안 함).
- 이 프로젝트는 단일모듈이라 의존순서/멀티모듈 빌드 로직은 없다(그건 과거 IB20 툴킷의 build-all — 제거됨).
