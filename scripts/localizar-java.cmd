@echo off
rem Localiza un JDK sin cambiar la configuracion del sistema.
set "JAVA_BIN="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\javac.exe" set "JAVA_BIN=%JAVA_HOME%\bin"
if defined JAVA_BIN exit /b 0
for %%J in (javac.exe) do if not "%%~$PATH:J"=="" set "JAVA_BIN=%%~dp$PATH:J"
if defined JAVA_BIN exit /b 0
for /d %%D in ("%ProgramFiles%\Java\*" "%ProgramFiles%\Eclipse Adoptium\*" "%USERPROFILE%\.jdks\*") do if exist "%%~D\bin\javac.exe" set "JAVA_BIN=%%~D\bin"
if defined JAVA_BIN exit /b 0
for /d %%D in ("%USERPROFILE%\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_*") do if exist "%%~D\jre\bin\javac.exe" set "JAVA_BIN=%%~D\jre\bin"
if defined JAVA_BIN exit /b 0
echo No se encontro un JDK. Configure JAVA_HOME o agregue javac al PATH.
exit /b 1
