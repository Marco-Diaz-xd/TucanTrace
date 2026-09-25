@echo off
REM ============================================================
REM  TucanTrace - Visor UML en vivo (dos pestanas en el navegador)
REM  Ejecuta el programa objetivo y lo visualiza en vivo.
REM  Boton "Ejecutar de nuevo" en la pestana Terminal para re-correrlo.
REM ============================================================
setlocal

if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"

cd /d "%~dp0.."

REM Compilar TucanTrace si hace falta
if not exist "build\classes\tucantrace\Main.class" (
  echo Compilando TucanTrace...
  for /f "delims=" %%f in ('dir /b /s "src\*.java"') do echo "%%f" >> "%TEMP%\tt-sources.txt"
  for /f "delims=" %%f in ('type "%TEMP%\tt-sources.txt"') do set JAVAC_SOURCES=!JAVAC_SOURCES! %%f
  "%JAVA_HOME%\bin\javac.exe" -encoding UTF-8 -cp "lib\*" -d "build\classes" @%TEMP%\tt-sources.txt
)

REM Compilar el caso de prueba si hace falta
if not exist "build\case-study-classes\co\edu\uniamazonia\logica2\Main.class" (
  echo Compilando caso de prueba...
  if not exist "build\case-study-classes" mkdir "build\case-study-classes"
  for /f "delims=" %%f in ('dir /b /s "case-study\tucango-model\src\*.java"') do echo "%%f" >> "%TEMP%\tt-case.txt"
  "%JAVA_HOME%\bin\javac.exe" -encoding UTF-8 -d "build\case-study-classes" @%TEMP%\tt-case.txt
)

echo ============================================================
echo   TucanTrace - Visor en vivo
echo   Visor UML : http://127.0.0.1:8077/
echo   Terminal  : http://127.0.0.1:8077/terminal
echo ============================================================
echo.

"%JAVA_HOME%\bin\java.exe" -Dfile.encoding=UTF-8 -cp "build\classes;lib\*" ^
  tucantrace.Main --live --delay 150 --http-port 8077 --port 5005 ^
  --exec co.edu.uniamazonia.logica2.Main --exec-cp build\case-study-classes

echo.
echo (TucanTrace finalizo. Presiona una tecla para cerrar.)
pause >nul
