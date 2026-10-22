@echo off
setlocal
cd /d "%~dp0"
call scripts\localizar-java.cmd
if errorlevel 1 exit /b 1
if not exist build\classes mkdir build\classes
set "JAVA_RELEASE=-source 8 -target 8"
"%JAVA_BIN%\javac.exe" --help 2>&1 | findstr /c:"--release" >nul
if not errorlevel 1 set "JAVA_RELEASE=--release 8"
"%JAVA_BIN%\javac.exe" %JAVA_RELEASE% -Xlint:all,-options -encoding UTF-8 -d build\classes src\main\java\ec\edu\ups\cafeteria\*.java
if errorlevel 1 exit /b 1
"%JAVA_BIN%\jar.exe" cfe build\cafeteria.jar ec.edu.ups.cafeteria.Main -C build\classes .
if errorlevel 1 exit /b 1
echo Compilacion correcta: build\cafeteria.jar
exit /b 0
