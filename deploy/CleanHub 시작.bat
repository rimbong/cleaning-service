@echo off
title CleanHub 서버 - 이 창을 닫으면 프로그램이 종료됩니다
cd /d "%~dp0"

rem ============================================================
rem  CleanHub 실행 - 이 파일의 바로가기를 바탕화면에 두고 더블클릭하세요.
rem ============================================================

if not exist "%~dp0cleanhub-env.bat" (
    echo.
    echo  [설정 파일이 없습니다]
    echo  cleanhub-env.example.bat 를 cleanhub-env.bat 로 복사한 뒤 안의 값을 채워 주세요.
    echo.
    pause
    exit /b 1
)
call "%~dp0cleanhub-env.bat"

if not exist "%~dp0cleanhub.war" (
    echo.
    echo  [프로그램 파일이 없습니다]  cleanhub.war 가 이 폴더에 있어야 합니다.
    echo.
    pause
    exit /b 1
)

rem ---- 실행할 자바 찾기 ----
rem  1) 설정 파일에 JAVA_CMD 를 적어 두었으면 그것을 쓴다
rem  2) JAVA_HOME 이 잡혀 있으면 그 안의 java.exe
rem  3) PATH 에 java 가 있으면 그것
if not defined JAVA_CMD (
    if defined JAVA_HOME (
        if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
    )
)
if not defined JAVA_CMD (
    where java > nul 2>&1
    if not errorlevel 1 set "JAVA_CMD=java"
)
if not defined JAVA_CMD (
    echo.
    echo  [자바를 찾을 수 없습니다]
    echo.
    echo  Java 8 ^(JRE^) 을 설치해 주세요.
    echo    https://adoptium.net/temurin/releases/?version=8
    echo  설치할 때 "Set JAVA_HOME variable" 과 "Add to PATH" 를 켜세요.
    echo.
    echo  이미 설치돼 있는데도 이 메시지가 나오면
    echo  cleanhub-env.bat 에 아래 한 줄을 추가하세요. 경로는 실제 설치 위치로 바꾸세요.
    echo      set JAVA_CMD=C:\Program Files\Eclipse Adoptium\jre-8\bin\java.exe
    echo.
    pause
    exit /b 1
)

rem 업로드/로그 폴더가 없으면 만든다
if not exist "%FILE_UPLOAD_DIR%" mkdir "%FILE_UPLOAD_DIR%"
if not exist "%APP_LOG_PATH%"    mkdir "%APP_LOG_PATH%"

echo.
echo   CleanHub 를 시작합니다. 잠시만 기다려 주세요...
echo   준비되면 브라우저가 자동으로 열립니다.
echo.
echo   [주의] 이 검은 창을 닫으면 프로그램이 꺼집니다. 쓰는 동안은 그대로 두세요.
echo.

rem 서버가 응답할 때까지 기다렸다가 브라우저를 여는 도우미(별도 창에서 조용히 대기)
start "" /min cmd /c ""%~dp0wait-and-open.bat" %SERVER_PORT%"

"%JAVA_CMD%" -jar "%~dp0cleanhub.war"

rem 서버가 멈추면(창을 닫거나 오류로 죽으면) 여기로 온다
echo.
echo   CleanHub 가 종료되었습니다.
echo   오류로 꺼진 것 같으면 로그를 확인하세요: %APP_LOG_PATH%
echo.
pause
