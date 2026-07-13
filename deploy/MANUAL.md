# CleanHub 수동 실행 가이드 (배치 파일 없이 손으로)

`.bat` 파일이 안 먹히거나, 무슨 일이 일어나는지 직접 보고 싶을 때 쓰는 문서입니다.
**모든 명령은 명령 프롬프트(cmd)** 에서 실행합니다. (시작 → `cmd` 입력)

> 이 문서의 경로·비밀번호는 예시입니다. 본인 환경에 맞게 바꿔서 쓰세요.

---

## 0. 준비물이 제대로 깔렸는지 확인

```
java -version
```
→ `openjdk version "1.8.0_xxx"` 같은 게 나와야 합니다.

**안 나오면**: Java 가 없거나 PATH 에 없습니다. 설치 위치를 직접 찾아 쓰면 됩니다.
```
"C:\Program Files\Eclipse Adoptium\jre-8.0.xxx\bin\java.exe" -version
```
앞으로 나오는 `java` 를 전부 이 전체 경로로 바꿔 쓰면 됩니다.

```
"C:\Program Files\PostgreSQL\18\bin\psql.exe" --version
```
→ `psql (PostgreSQL) 18.x` 가 나와야 합니다.

PostgreSQL 이 **켜져 있는지** 확인:
```
sc query postgresql-x64-18
```
→ `STATE : 4 RUNNING` 이어야 합니다. 멈춰 있으면:
```
net start postgresql-x64-18
```

---

## 1. 데이터베이스 만들기 (한 번만)

`psql` 로 postgres 관리자 접속. (설치할 때 정한 비밀번호를 물어봅니다)

```
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres
```

접속되면 `postgres=#` 프롬프트가 뜹니다. 아래를 **한 줄씩** 입력합니다.
(`여기에_원하는_비밀번호` 는 본인이 정하세요. 나중에 앱 설정에 똑같이 넣어야 합니다.)

```sql
CREATE USER cleanhub_user WITH PASSWORD '여기에_원하는_비밀번호';

CREATE DATABASE cleanhub OWNER cleanhub_user ENCODING 'UTF8';

\connect cleanhub

CREATE EXTENSION IF NOT EXISTS pgcrypto;

GRANT ALL ON SCHEMA public TO cleanhub_user;

\q
```

각 명령 뒤에 `CREATE USER`, `CREATE DATABASE` 같은 응답이 나오면 성공입니다.

> **`pgcrypto` 는 꼭 필요합니다.** 최초 관리자 계정(`admin`)의 비밀번호를 암호화해서 넣을 때 씁니다.
> 이게 없으면 앱을 처음 켤 때 마이그레이션이 실패합니다.

**잘 됐는지 확인:**
```
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U cleanhub_user -d cleanhub -c "SELECT current_database();"
```
비밀번호를 물어보고 `cleanhub` 가 나오면 성공입니다.

> 표(테이블)는 여기서 안 만듭니다. 앱을 처음 켤 때 Flyway 가 알아서 만듭니다.

---

## 2. 프로그램 파일 준비 (개발 PC에서)

**프론트엔드를 먼저** 빌드해야 화면이 WAR 에 들어갑니다. 순서를 바꾸면 빈 화면이 나옵니다.

```
cd frontend\vue
npm install
npm run build

cd ..\..\project
mvnw clean package -DskipTests
```

결과물: `project\target\cleanhub-0.0.1-SNAPSHOT.war` (약 80MB)

이 파일을 아버님 PC 의 `C:\CleanHub\cleanhub.war` 로 복사합니다.

> **톰캣은 설치 안 해도 됩니다.** WAR 안에 톰캣이 들어 있습니다.

---

## 3. 실행 (손으로)

`C:\CleanHub` 로 이동한 뒤, **아래를 한 줄씩** 입력합니다.
(`set` 으로 넣은 값은 그 cmd 창에서만 유효합니다. 창을 닫으면 사라집니다.)

```
cd /d C:\CleanHub

set SPRING_PROFILES_ACTIVE=prod
set SERVER_PORT=8080
set DB_URL=jdbc:postgresql://localhost:5432/cleanhub
set DB_USERNAME=cleanhub_user
set DB_PASSWORD=1번에서_정한_비밀번호
set JWT_SECRET=아무렇게나_길게_32자_이상_영문숫자
set FILE_UPLOAD_DIR=C:\CleanHub\data\uploads
set APP_LOG_PATH=C:\CleanHub\logs
set REFRESH_COOKIE_SECURE=false

java -jar cleanhub.war
```

### 잘 떴는지 보는 법

로그가 주르륵 지나가다가 아래 두 줄이 나오면 성공입니다.

```
Tomcat started on port(s): 8080 (http)
Started CleanhubApplication in 9.xxx seconds
```

그 다음 브라우저에서 **http://localhost:8080/admin** 을 엽니다.

- 아이디 `admin` / 비밀번호 `admin1234`
- **첫 로그인 후 반드시 비밀번호를 바꾸세요.**

### 끄는 법

실행 중인 cmd 창에서 **Ctrl + C**. 또는 그 창을 닫습니다.

### 각 설정값이 무슨 뜻인지

| 값 | 뜻 | 안 넣으면 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE=prod` | 운영 모드로 켜기 | 개발 모드(`dev`)로 떠서 DB 접속값이 달라짐 |
| `SERVER_PORT` | 접속 포트 | 70번 포트로 뜸 |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | DB 접속 | **기동 실패** (필수) |
| `JWT_SECRET` | 로그인 토큰 서명 키 | 소스에 적힌 개발용 기본값이 쓰임 (위험) |
| `FILE_UPLOAD_DIR` | 계약서 첨부·도장 저장 위치 | 실행한 폴더 아래 `.\uploads` |
| `APP_LOG_PATH` | 로그 위치 | `/var/log/cleanhub` (리눅스 경로라 윈도우에서 이상해짐) |

> **`application-prod.yml` 은 고칠 필요 없습니다.** 그 파일에는 값이 아니라 `${DB_URL}` 같은
> 자리만 있고, 위의 `set` 으로 넣은 값이 거기 들어갑니다. WAR 를 다시 빌드할 필요도 없습니다.

---

## 4. 백업 (손으로)

### DB 백업

```
set PGPASSWORD=1번에서_정한_비밀번호
"C:\Program Files\PostgreSQL\18\bin\pg_dump.exe" -h localhost -U cleanhub_user -d cleanhub -F c -f "C:\CleanHub\backup\cleanhub_db_20260713.dump"
set PGPASSWORD=
```

- `-F c` : 압축된 형식. `pg_restore` 로 되돌립니다.
- 파일명의 날짜는 아무렇게나 붙여도 됩니다. 언제 받은 건지 알아보기 쉽게만 하세요.

### 업로드 파일 백업

계약서 첨부와 회사 도장 이미지가 들어 있습니다. **DB만 백업하면 이건 안 살아납니다.**

```
powershell -Command "Compress-Archive -Path 'C:\CleanHub\data\uploads\*' -DestinationPath 'C:\CleanHub\backup\cleanhub_files_20260713.zip' -Force"
```

### 백업이 제대로 됐는지 확인

```
"C:\Program Files\PostgreSQL\18\bin\pg_restore.exe" -l "C:\CleanHub\backup\cleanhub_db_20260713.dump"
```
표 목록(`client`, `contract`, `billing` 등)이 주르륵 나오면 정상입니다.
오류가 나면 그 백업 파일은 못 씁니다.

> **백업 파일을 그 PC 안에만 두지 마세요.** PC 가 고장나면 백업도 같이 죽습니다.
> USB 나 클라우드(구글 드라이브 등)에 주기적으로 복사해 두세요.

---

## 5. 복구 (PC 고장 등)

새 PC 에 **0~2 번을 먼저** 하고, **프로그램을 켜기 전에** 복구합니다.

### DB 복구

```
set PGPASSWORD=postgres_비밀번호
"C:\Program Files\PostgreSQL\18\bin\pg_restore.exe" -h localhost -U postgres -d cleanhub --clean --if-exists "C:\CleanHub\backup\cleanhub_db_20260713.dump"
set PGPASSWORD=
```

- `--clean --if-exists` : 기존 표가 있으면 지우고 덮어씁니다.
- **postgres 관리자 계정으로** 하는 게 안전합니다 (`cleanhub_user` 는 권한이 모자랄 수 있음).

### 업로드 파일 복구

`cleanhub_files_20260713.zip` 을 풀어서 `FILE_UPLOAD_DIR`(예: `C:\CleanHub\data\uploads`) 에 덮어씁니다.

그 다음 3번(실행)을 하면 됩니다.

---

## 6. 문제가 생겼을 때

### "Port 8080 was already in use" — 포트를 다른 프로그램이 쓰고 있음

누가 쓰는지 찾기:
```
netstat -ano | findstr :8080
```
맨 오른쪽 숫자가 PID 입니다. 그 프로그램이 뭔지 보려면:
```
tasklist /fi "pid eq 그PID"
```

**해결**: `set SERVER_PORT=8081` 처럼 다른 번호를 쓰면 됩니다.

### "Connection refused" / DB 접속 실패

PostgreSQL 이 떠 있는지:
```
sc query postgresql-x64-18
```
멈춰 있으면 `net start postgresql-x64-18`

접속이 되는지 직접 확인:
```
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U cleanhub_user -d cleanhub -c "SELECT 1;"
```
여기서 안 되면 앱도 절대 안 됩니다. 비밀번호가 `DB_PASSWORD` 와 같은지 보세요.

### "password authentication failed"

`set DB_PASSWORD=...` 값이 1번에서 `CREATE USER` 할 때 정한 비밀번호와 다릅니다.

비밀번호를 다시 정하려면:
```
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -c "ALTER USER cleanhub_user WITH PASSWORD '새비밀번호';"
```

### "Migration failed" / "function crypt does not exist"

`pgcrypto` 확장이 안 깔렸습니다. 1번의 `CREATE EXTENSION` 을 빠뜨린 겁니다.

```
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -d cleanhub -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;"
```

그 다음 실패한 마이그레이션 기록을 지우고 다시 켜야 합니다:
```
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U cleanhub_user -d cleanhub -c "DELETE FROM flyway_schema_history WHERE success = false;"
```

### 화면이 하얗게만 나옴

프론트엔드 빌드를 빼먹고 WAR 를 만든 겁니다. 2번을 **npm run build 부터** 다시 하세요.

### 관리자 비밀번호를 잊음

DB 에서 직접 되돌립니다. (`admin1234` 로 초기화)

```
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U cleanhub_user -d cleanhub -c "UPDATE auth_user SET password = crypt('admin1234', gen_salt('bf', 10)) WHERE username = 'admin';"
```

그 다음 `admin` / `admin1234` 로 로그인하고 바로 바꾸세요.

### 로그 보기

```
type C:\CleanHub\logs\project-error.log
```
최근 것만 보려면:
```
powershell -Command "Get-Content 'C:\CleanHub\logs\project-error.log' -Tail 50"
```

---

## 7. 자주 쓰는 확인 명령 모음

| 하고 싶은 것 | 명령 |
|---|---|
| 자바 버전 | `java -version` |
| DB 켜져 있나 | `sc query postgresql-x64-18` |
| DB 접속되나 | `psql -U cleanhub_user -d cleanhub -c "SELECT 1;"` |
| 거래처 몇 개인가 | `psql -U cleanhub_user -d cleanhub -c "SELECT count(*) FROM client;"` |
| 앱이 떠 있나 | `netstat -ano \| findstr :8080` |
| 백업 파일 성한가 | `pg_restore -l 백업파일.dump` |

(`psql`, `pg_dump`, `pg_restore` 는 `C:\Program Files\PostgreSQL\18\bin\` 안에 있습니다)
