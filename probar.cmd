@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0"
call compilar.cmd
if errorlevel 1 exit /b 1
call scripts\localizar-java.cmd
if errorlevel 1 exit /b 1
if not exist build\test-classes mkdir build\test-classes
set "JAVA_RELEASE=-source 8 -target 8"
"%JAVA_BIN%\javac.exe" --help 2>&1 | findstr /c:"--release" >nul
if not errorlevel 1 set "JAVA_RELEASE=--release 8"
"%JAVA_BIN%\javac.exe" %JAVA_RELEASE% -Xlint:all,-options -encoding UTF-8 -cp build\classes -d build\test-classes src\test\java\ec\edu\ups\cafeteria\PruebasCafeteria.java
if errorlevel 1 exit /b 1
"%JAVA_BIN%\java.exe" -Dfile.encoding=UTF-8 -cp "build\classes;build\test-classes" ec.edu.ups.cafeteria.PruebasCafeteria
exit /b %errorlevel%
