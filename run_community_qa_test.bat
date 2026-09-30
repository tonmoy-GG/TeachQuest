@echo off
setlocal
cd /d "%~dp0frontend"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\run-selenium-tests.ps1" -TestScript "test:community-qa" %*
set "testExitCode=%ERRORLEVEL%"
echo.
pause
exit /b %testExitCode%