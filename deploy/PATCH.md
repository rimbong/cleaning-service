# CleanHub 패치 가이드 (운영 중 수정 반영)

이미 아버지 PC 에서 돌고 있는 CleanHub 를 고칠 때 보는 문서입니다.
설치가 아니라 **이미 설치된 것을 새 버전으로 바꾸는** 절차입니다.

---

## 먼저 — 예전 방식은 이제 안 됩니다

| | 예전 (별도 톰캣) | 지금 (내장 톰캣) |
|---|---|---|
| 코드 수정 | `.class` 파일만 교체 | **WAR 파일 통째로 교체** |
| DB 수정 | 운영 DB 에 직접 `ALTER` | **새 마이그레이션 파일 추가 → 자동 적용** |

**왜 바뀌었나**
- WAR 를 압축 해제하지 않고 `java -jar` 로 통째로 실행한다. 그래서 안의 `.class` 하나만
  갈아끼우는 게 불가능하다. WAR 전체를 새로 만드는 게 유일하고 안전한 방법이다.
- 스키마는 Flyway 가 관리한다(`db/migration/V*.sql`). 운영 DB 에 손으로 `ALTER` 를 치면
  Flyway 기록과 실제 DB 가 어긋나, 다음에 앱을 켤 때 `ddl-auto=validate` 가 막아 **기동이 안 된다**.

**딱 두 규칙만 지키면 된다**
1. 패치는 항상 **WAR 전체 재빌드·교체** (부분 교체 금지)
2. DB 변경은 항상 **새 V*.sql 마이그레이션** (운영 DB 직접 수정 금지)

---

## A. 코드만 고칠 때 (화면·로직 수정, DB 변경 없음)

### 개발 PC 에서 — WAR 새로 빌드

```
rem 1) 프론트 먼저 (화면을 안 바꿨어도 안전하게 같이 빌드한다)
cd D:\private\cleaning-service\frontend\vue
npm run build

rem 2) WAR 빌드
cd D:\private\cleaning-service\project
mvn -Daether.connector.https.securityMode=insecure clean package -DskipTests
```

> **이 개발 PC 는 사내망이라 `mvnw` 가 아니라 설치된 `mvn` 을 쓴다.**
> `-Daether...insecure` 는 사내망 SSL(PKIX) 우회라 **빼면 빌드가 실패한다.**
> (settings.xml 은 Maven 설치 폴더의 conf 를 자동으로 읽으므로 `-s` 는 생략 가능.
>  단, `~/.m2/settings.xml` 이 생기면 그게 우선하니 그때는 `-s` 를 다시 붙인다.)

산출물: `project\target\cleanhub-0.0.1-SNAPSHOT.war` (약 85MB)

### 아버지 PC 에서 — 교체

```
rem 0) (권장) 먼저 백업
"CleanHub 백업.bat" 더블클릭

rem 1) 앱 종료
검은 콘솔 창을 닫는다 (또는 CleanHub 시작.bat 로 뜬 창을 닫는다)

rem 2) 옛 WAR 를 이름 바꿔 보관하고 새 WAR 로 교체
ren C:\CleanHub\cleanhub.war cleanhub_backup.war
copy <새로 빌드한 WAR> C:\CleanHub\cleanhub.war
       (파일명을 반드시 cleanhub.war 로 — 배치파일이 이 이름을 찾는다)

rem 3) 다시 실행
"CleanHub 시작.bat" 더블클릭
```

브라우저가 열리고 로그인되면 끝이다. 이상하면 4번(되돌리기)으로.

---

## B. DB 스키마도 바꿀 때 (컬럼 추가 등)

**운영 DB 에 `ALTER` 를 직접 치지 않는다.** 대신 새 마이그레이션 파일을 만든다.

### 개발 PC 에서

```
rem 1) 새 마이그레이션 파일 생성 (번호는 마지막 다음 번호)
   resources\db\migration\V29__무엇무엇.sql
   예)  ALTER TABLE client ADD COLUMN sample VARCHAR(50);

rem 2) 엔티티(Java)도 같이 수정
   ddl-auto=validate 라 SQL 과 엔티티가 일치해야 앱이 뜬다.
   컬럼을 추가했으면 해당 도메인 클래스에 필드를 추가한다.

rem 3) 로컬에서 한 번 띄워 확인
   Flyway 가 V29 를 적용하고 validate 를 통과하는지 본다.

rem 4) WAR 빌드 (A 와 동일)
```

### 아버지 PC 에서

```
A 와 똑같다. WAR 교체 후 CleanHub 시작.bat.
앱이 켜질 때 Flyway 가 "아직 V29 를 안 돌렸네" 하고 자동으로 적용한다.
아버지 PC 에서 SQL 을 직접 칠 일은 없다.
```

> **마이그레이션 규칙**
> - 파일명: `V<번호>__<설명>.sql` — 번호는 항상 마지막보다 커야 한다(현재 V28 까지 있음).
> - 이미 배포·적용된 마이그레이션 파일은 **절대 고치지 않는다.** 내용을 바꾸면 체크섬이 달라져
>   다음 기동 때 Flyway 가 막는다. 수정이 필요하면 새 번호로 또 만든다(V24 가 그런 복구 파일이다).
> - 마이그레이션이 넣는 **값(데이터)에는 한글을 직접 쓰지 않는다.** 주석의 한글은 무해하다.
>   (인코딩 환경에 따라 한글이 물음표로 저장된 적이 있어 V24 로 복구했다. 값에 한글이 필요하면
>    PostgreSQL 유니코드 이스케이프 `U&'...'` 를 쓴다 — V24 참고.)

---

## 되돌리기 (패치가 잘못됐을 때)

### 코드만 바꿨으면 (DB 안 건드림)

```
rem 앱 종료 후
del C:\CleanHub\cleanhub.war
ren C:\CleanHub\cleanhub_backup.war cleanhub.war
"CleanHub 시작.bat"
```
옛 WAR 로 돌아간다. DB 를 안 건드렸으니 이걸로 끝.

### DB 도 바꿨으면

마이그레이션은 되돌리기가 까다롭다. 그래서 **패치 전 백업이 중요하다.**
```
rem 1) 앱 종료
rem 2) DB 복원 (백업 시점으로)
"C:\Program Files\PostgreSQL\18\bin\pg_restore.exe" -U postgres -d cleanhub --clean --if-exists "C:\CleanHub\backup\cleanhub_db_<시각>.dump"
rem 3) 옛 WAR 로 교체 후 실행
```
백업이 없으면 스키마를 손으로 되돌려야 하므로, **DB 를 바꾸는 패치는 반드시 백업 먼저.**

---

## 요약

| 상황 | 개발 PC | 아버지 PC |
|---|---|---|
| 코드만 | 프론트 빌드 → WAR 빌드 | 백업 → 앱 종료 → WAR 교체 → 시작 |
| DB 도 | V*.sql 추가 + 엔티티 수정 → WAR 빌드 | (같음) 시작하면 Flyway 자동 적용 |

어느 쪽이든 아버지 PC 에서 하는 일은 **"백업 → WAR 교체 → 재시작"** 하나로 같다.
