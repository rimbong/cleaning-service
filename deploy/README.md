# CleanHub 설치 안내 (윈도우 PC)

한 대의 윈도우 PC에서 CleanHub 를 돌리기 위한 준비물과 순서입니다.
**설치하는 사람(개발자) 기준**으로 적었습니다. 실제 사용자는 바탕화면 아이콘만 누르면 됩니다.

구성: 그 PC에서만 접속(localhost) / 바탕화면 아이콘으로 실행 / 매일 자동 백업

---

## 1. 준비물 설치

### 1-1. Java 8 (JRE)

WAR 를 실행하려면 필요합니다.

- [Eclipse Temurin 8 (JRE)](https://adoptium.net/temurin/releases/?version=8) 에서 Windows x64 `.msi` 를 받아 설치
- 설치할 때 **"Set JAVA_HOME variable"** 과 **"Add to PATH"** 를 켜 주세요.
- 확인: 명령 프롬프트에서 `java -version` → `1.8.0_xxx` 가 나오면 됩니다.

### 1-2. PostgreSQL 18

- [PostgreSQL 다운로드](https://www.postgresql.org/download/windows/) → Windows x86-64 설치본
- 설치 중 물어보는 **postgres 계정 비밀번호를 꼭 적어두세요.** (DB 를 만들 때 씁니다)
- 포트는 기본값 `5432` 그대로.
- 설치 경로는 기본값(`C:\Program Files\PostgreSQL\18`) 을 권장합니다.
  다른 곳에 설치하면 `CleanHub 백업.bat` 안의 `PG_DUMP` 경로를 고쳐야 합니다.

---

## 2. 데이터베이스 만들기

`deploy\sql\01_create_db.sql` 을 열어 **`CHANGE_ME` 를 실제 비밀번호로 바꾼 뒤** 실행합니다.

명령 프롬프트에서:

```
cd C:\CleanHub\sql
"C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -f 01_create_db.sql
```

postgres 비밀번호를 물어보면 1-2 에서 정한 값을 넣으세요.

이 스크립트가 하는 일:
- `cleanhub_user` 계정 생성
- `cleanhub` 데이터베이스 생성
- **pgcrypto 확장 설치** (최초 관리자 계정의 비밀번호 해시를 만드는 데 필요합니다)

> 표(테이블)는 만들지 않습니다. 프로그램을 처음 켤 때 Flyway 가 알아서 만듭니다.

---

## 3. 프로그램 파일 준비

개발 PC에서 빌드합니다. **프론트엔드를 먼저 빌드**해야 화면이 WAR 에 들어갑니다.

```
cd frontend\vue
npm install
npm run build

cd ..\..\project
mvnw clean package -DskipTests
```

산출물: `project\target\cleanhub-0.0.1-SNAPSHOT.war`

아버님 PC 에 `C:\CleanHub` 폴더를 만들고 아래를 복사합니다.

```
C:\CleanHub\
    cleanhub.war                <- 위 WAR 를 이 이름으로 바꿔서
    CleanHub 시작.bat
    CleanHub 백업.bat
    백업 자동실행 등록.bat
    wait-and-open.bat
    cleanhub-env.example.bat
    sql\01_create_db.sql
```

---

## 4. 설정 파일 만들기

`cleanhub-env.example.bat` 를 **`cleanhub-env.bat`** 로 복사한 뒤 값을 채웁니다.

| 항목 | 설명 |
|---|---|
| `SERVER_PORT` | 기본 `8080`. 다른 프로그램이 쓰고 있으면 `8081`, `9090` 등으로 |
| `DB_PASSWORD` | 2번에서 정한 `cleanhub_user` 비밀번호와 **똑같이** |
| `JWT_SECRET` | **반드시 바꾸세요.** 로그인 토큰 서명 키입니다. 영문/숫자 섞어 32자 이상 |
| `FILE_UPLOAD_DIR` | 계약서 첨부·도장 이미지 저장 위치. 기본 `C:\CleanHub\data\uploads` |
| `APP_LOG_PATH` | 로그 위치. 기본 `C:\CleanHub\logs` |

> `cleanhub-env.bat` 에는 비밀번호가 들어갑니다. 이 파일은 git 에 올라가지 않습니다.

---

## 5. 실행

`CleanHub 시작.bat` 을 더블클릭합니다.

- 검은 콘솔 창이 뜨고, 준비되면 **브라우저가 자동으로 열립니다** (`http://localhost:8080/admin`).
- **이 검은 창을 닫으면 프로그램이 꺼집니다.** 쓰는 동안은 그대로 두세요.
- 바탕화면에서 쓰시려면: `CleanHub 시작.bat` 오른쪽 클릭 → **바로 가기 만들기** → 바탕화면으로 옮기기

### 첫 로그인

- 아이디 `admin` / 비밀번호 `admin1234`
- **반드시 비밀번호를 바꾸세요.** 이 값은 소스에 적혀 있는 초기값입니다.

---

## 6. 자동 백업 등록

`백업 자동실행 등록.bat` 을 **오른쪽 클릭 → 관리자 권한으로 실행** (한 번만).

- 매일 밤 11시에 자동으로 백업합니다.
- 백업 위치: `C:\CleanHub\backup`
- 30일 지난 백업은 자동으로 지웁니다.
- 백업 내용: **DB 전체**(`.dump`) + **업로드 파일**(`.zip` — 계약서 첨부, 도장 이미지)

바로 한 번 돌려보려면 `CleanHub 백업.bat` 을 더블클릭하세요.

> PC 가 고장나면 백업 파일만으로 복구합니다. **백업 폴더를 가끔 USB 나 클라우드에 복사해 두세요.**
> PC 안에만 있으면 그 PC 가 죽을 때 백업도 같이 죽습니다.

---

## 7. 복구 방법 (PC 고장 등)

새 PC 에 1~4 번을 똑같이 한 뒤, **프로그램을 켜기 전에** 백업을 되돌립니다.

```
rem 1) DB 복원 (postgres 슈퍼유저로)
"C:\Program Files\PostgreSQL\18\bin\pg_restore.exe" -U postgres -d cleanhub --clean --if-exists "C:\CleanHub\backup\cleanhub_db_YYYYMMDD_HHMMSS.dump"

rem 2) 업로드 파일 복원
rem    cleanhub_files_YYYYMMDD_HHMMSS.zip 을 풀어서 FILE_UPLOAD_DIR 위치에 덮어쓰기
```

그 다음 `CleanHub 시작.bat` 을 실행합니다.

---

## 문제가 생기면

| 증상 | 확인할 것 |
|---|---|
| 브라우저가 안 열림 | 콘솔 창에 빨간 오류가 있는지. 로그: `C:\CleanHub\logs` |
| `Connection refused` / DB 오류 | PostgreSQL 서비스가 떠 있는지 (services.msc → `postgresql-x64-18`) |
| 포트 충돌 | `cleanhub-env.bat` 의 `SERVER_PORT` 를 다른 번호로 |
| 로그인 안 됨 | 초기 계정 `admin` / `admin1234`. 비밀번호를 잊으면 DB 를 직접 손봐야 합니다 |
