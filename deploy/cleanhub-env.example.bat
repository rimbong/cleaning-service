@echo off
rem ============================================================
rem  CleanHub 환경설정 (이 파일을 cleanhub-env.bat 로 복사해서 값을 채우세요)
rem  ※ cleanhub-env.bat 에는 비밀번호가 들어갑니다. 다른 사람에게 주지 마세요.
rem ============================================================

rem ---- 실행 프로파일 (운영) ----
set SPRING_PROFILES_ACTIVE=prod

rem ---- 접속 포트 ----
rem  브라우저 주소창에 http://localhost:8080 으로 접속하게 됩니다.
rem  다른 프로그램이 8080 을 쓰고 있으면 8081, 9090 등으로 바꾸세요.
set SERVER_PORT=8080

rem ---- 데이터베이스 ----
rem  01_create_db.sql 에서 정한 비밀번호와 똑같이 맞춰야 합니다.
set DB_URL=jdbc:postgresql://localhost:5432/cleanhub
set DB_USERNAME=cleanhub_user
set DB_PASSWORD=CHANGE_ME

rem ---- 로그인 토큰 서명 키 ----
rem  아무나 못 만들게 하는 열쇠입니다. 반드시 바꾸세요.
rem  영문/숫자 섞어서 최소 32자 이상. (예: 키보드로 아무렇게나 길게)
set JWT_SECRET=CHANGE_ME_TO_A_LONG_RANDOM_STRING_AT_LEAST_32_CHARS

rem ---- 파일 저장 위치 ----
rem  계약서 첨부, 회사 도장 이미지가 여기 쌓입니다. 백업 대상입니다.
set FILE_UPLOAD_DIR=C:\CleanHub\data\uploads

rem ---- 로그 저장 위치 ----
set APP_LOG_PATH=C:\CleanHub\logs

rem ---- HTTPS 를 쓰지 않으므로 꺼 둡니다 (그 PC에서만 접속하는 구성) ----
set REFRESH_COOKIE_SECURE=false

rem ---- 자바 위치 (선택) ----
rem  보통은 비워 두면 됩니다. 자바를 못 찾는다는 오류가 나면 아래 주석을 풀고
rem  실제 설치 경로로 고치세요.
rem set JAVA_CMD=C:\Program Files\Eclipse Adoptium\jre-8in\java.exe
