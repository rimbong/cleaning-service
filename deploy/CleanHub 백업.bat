@echo off
title CleanHub 백업
cd /d "%~dp0"

rem ============================================================
rem  CleanHub 백업 - 데이터베이스 + 업로드 파일(계약서 첨부, 도장 이미지)
rem  작업 스케줄러가 매일 자동 실행합니다. 손으로 돌려도 됩니다.
rem ============================================================

if not exist "cleanhub-env.bat" (
    echo [설정 파일 cleanhub-env.bat 이 없습니다]
    exit /b 1
)
call "%~dp0cleanhub-env.bat"

rem ---- 백업 위치와 보관 기간 ----
set BACKUP_DIR=C:\CleanHub\backup
set KEEP_DAYS=30

set PG_DUMP=C:\Program Files\PostgreSQL\18\bin\pg_dump.exe
if not exist "%PG_DUMP%" (
    echo [pg_dump 를 찾을 수 없습니다] %PG_DUMP%
    echo PostgreSQL 설치 경로가 다르면 이 파일의 PG_DUMP 값을 고치세요.
    exit /b 1
)

if not exist "%BACKUP_DIR%" mkdir "%BACKUP_DIR%"

rem ---- 파일명에 쓸 날짜. 지역설정과 무관하게 얻는다 ----
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd_HHmmss"') do set STAMP=%%i

set DB_FILE=%BACKUP_DIR%\cleanhub_db_%STAMP%.dump
set FILES_ZIP=%BACKUP_DIR%\cleanhub_files_%STAMP%.zip

echo.
echo  [1/3] 데이터베이스 백업 중...
set PGPASSWORD=%DB_PASSWORD%
"%PG_DUMP%" -h localhost -p 5432 -U %DB_USERNAME% -d cleanhub -F c -f "%DB_FILE%"
set PG_RESULT=%errorlevel%
set PGPASSWORD=
if not "%PG_RESULT%"=="0" (
    echo        [실패] 데이터베이스 백업 실패. 오류 코드 %PG_RESULT%
    exit /b 1
)
echo        완료: %DB_FILE%

echo  [2/3] 업로드 파일 백업 중...
if exist "%FILE_UPLOAD_DIR%" (
    powershell -NoProfile -Command "if (Get-ChildItem -Path '%FILE_UPLOAD_DIR%' -Force | Select-Object -First 1) { Compress-Archive -Path '%FILE_UPLOAD_DIR%\*' -DestinationPath '%FILES_ZIP%' -Force }"
    if exist "%FILES_ZIP%" (
        echo        완료: %FILES_ZIP%
    ) else (
        echo        건너뜀: 업로드된 파일이 없습니다.
    )
) else (
    echo        건너뜀: 업로드 폴더가 없습니다.
)

echo  [3/3] %KEEP_DAYS%일 지난 오래된 백업 정리 중...
forfiles /p "%BACKUP_DIR%" /m cleanhub_*.* /d -%KEEP_DAYS% /c "cmd /c del @path" > nul 2>&1
echo        완료.

echo.
echo  백업이 끝났습니다. 보관 위치: %BACKUP_DIR%
echo.
exit /b 0
