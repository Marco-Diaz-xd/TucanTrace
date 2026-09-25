@echo off
REM ============================================================
REM  TucanTrace - Visor UML en vivo (dos pestanas en el navegador)
REM  Ejecuta el programa objetivo y lo visualiza en vivo.
REM ============================================================
setlocal enabledelayedexpansion

REM Buscar JDK 21 o superior en rutas comunes de Windows
set "JAVA_CMD="
set "JAVAC_CMD="

if defined JAVA_HOME (
  if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_CMD=%JAVA_HOME%\bin\java.exe"
  if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC_CMD=%JAVA_HOME%\bin\javac.exe"
)

if not defined JAVA_CMD (
  for %%P in (
    "C:\Program Files\Java\jdk-21*\bin"
    "C:\Program Files\Eclipse Adoptium\jdk-21*\bin"
    "C:\Program Files\Semeru\jdk-21*\bin"
    "C:\Program Files\Amazon Corretto\jdk21*\bin"
    "C:\Program Files\Microsoft\jdk-21*\bin"
    "C:\Program Files\BellSoft\LibericaJDK-21*\bin"
    "C:\Program Files\Zulu\zulu-21*\bin"
    "C:\Program Files\Java\jdk-22*\bin"
    "C:\Program Files\Java\jdk-23*\bin"
  ) do (
    if not defined JAVA_CMD (
      if exist "%%~P\java.exe" (
        set "JAVA_CMD=%%~P\java.exe"
        set "JAVAC_CMD=%%~P\javac.exe"
      )
    )
  )
)

REM Si aun no se encuentra, usar el del PATH
if not defined JAVA_CMD set "JAVA_CMD=java"
if not defined JAVAC_CMD set "JAVAC_CMD=javac"

cd /d "%~dp0.."

REM 1. Compilar TucanTrace si hace falta
if not exist "build\classes\tucantrace\Main.class" (
  echo Compilando TucanTrace...
  if not exist "build\classes" mkdir "build\classes"
  if exist "%TEMP%\tt-sources.txt" del "%TEMP%\tt-sources.txt"
  for /f "delims=" %%f in ('dir /b /s "src\*.java"') do echo %%f>> "%TEMP%\tt-sources.txt"
  "%JAVAC_CMD%" -encoding UTF-8 -cp "lib\*" -d "build\classes" @"%TEMP%\tt-sources.txt"
  if errorlevel 1 (
    echo Error al compilar TucanTrace. Verifica que tengas JDK 21 instalado.
    pause
    exit /b 1
  )
)

REM 2. Compilar el caso de prueba si hace falta
if not exist "build\case-study-classes\co\edu\uniamazonia\logica2\Main.class" (
  echo Compilando caso de prueba...
  if not exist "build\case-study-classes" mkdir "build\case-study-classes"
  if exist "%TEMP%\tt-case.txt" del "%TEMP%\tt-case.txt"
  for /f "delims=" %%f in ('dir /b /s "case-study\tucango-model\src\*.java"') do echo %%f>> "%TEMP%\tt-case.txt"
  "%JAVAC_CMD%" -encoding UTF-8 -d "build\case-study-classes" @"%TEMP%\tt-case.txt"
  if errorlevel 1 (
    echo Error al compilar caso de prueba.
    pause
    exit /b 1
  )
)

echo ============================================================
echo   TucanTrace - Visor en vivo
echo   Visor UML : http://127.0.0.1:8077/
echo   Terminal  : http://127.0.0.1:8077/terminal
echo ============================================================
echo.

"%JAVA_CMD%" -Dfile.encoding=UTF-8 -cp "build\classes;lib\*" ^
  tucantrace.Main --live --delay 150 --http-port 8077 --port 5005 ^
  --exec co.edu.uniamazonia.logica2.Main --exec-cp build\case-study-classes

echo.
echo (TucanTrace finalizo. Presiona una tecla para cerrar.)
pause >nul
