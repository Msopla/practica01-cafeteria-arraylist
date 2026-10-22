@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
call compilar.cmd
if errorlevel 1 exit /b 1
call scripts\localizar-java.cmd
if errorlevel 1 exit /b 1
"%JAVA_BIN%\java.exe" -Dfile.encoding=UTF-8 -jar build\cafeteria.jar %*
exit /b %errorlevel%
