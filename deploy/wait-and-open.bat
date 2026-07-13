@echo off
rem ============================================================
rem  서버가 뜰 때까지 기다렸다가 브라우저를 연다. ("CleanHub 시작.bat" 이 호출)
rem  인자: %1 = 포트
rem ============================================================
setlocal
set PORT=%1
if "%PORT%"=="" set PORT=8080

rem 최대 90초 동안 1초 간격으로 포트가 열렸는지 확인한다.
for /l %%i in (1,1,90) do (
    powershell -NoProfile -Command "try { $c = New-Object Net.Sockets.TcpClient('localhost', %PORT%); $c.Close(); exit 0 } catch { exit 1 }" > nul 2>&1
    if not errorlevel 1 (
        start "" http://localhost:%PORT%/admin
        exit /b 0
    )
    timeout /t 1 /nobreak > nul
)
exit /b 1
