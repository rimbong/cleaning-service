@echo off
title CleanHub 백업 자동실행 등록
cd /d "%~dp0"

rem ============================================================
rem  매일 밤 11시에 백업이 자동으로 돌도록 작업 스케줄러에 등록한다.
rem  ※ 이 파일은 "관리자 권한으로 실행" 해야 합니다. (오른쪽 클릭 -> 관리자 권한으로 실행)
rem  ※ 한 번만 등록하면 됩니다.
rem ============================================================

net session > nul 2>&1
if errorlevel 1 (
    echo.
    echo  [관리자 권한이 필요합니다]
    echo  이 파일을 오른쪽 클릭 -^> "관리자 권한으로 실행" 으로 다시 실행해 주세요.
    echo.
    pause
    exit /b 1
)

set TASK_NAME=CleanHub 백업
set BACKUP_BAT=%~dp0CleanHub 백업.bat

schtasks /create /tn "%TASK_NAME%" /tr "\"%BACKUP_BAT%\"" /sc daily /st 23:00 /rl highest /f
if errorlevel 1 (
    echo.
    echo  [등록 실패]
    pause
    exit /b 1
)

echo.
echo  등록되었습니다. 매일 밤 11시에 자동으로 백업합니다.
echo.
echo  - 지금 바로 한 번 돌려보려면:  schtasks /run /tn "%TASK_NAME%"
echo  - 등록을 취소하려면:           schtasks /delete /tn "%TASK_NAME%" /f
echo.
pause
