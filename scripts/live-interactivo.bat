@echo off
REM ============================================================
REM  TucanTrace + Prototipo Interactivo de TucanGo
REM  Escribi en ESTA ventana. El navegador muestra el UML en vivo.
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

title TucanTrace - ESCRIBI ACA
cd /d "%~dp0.."
set "TUCANGO=%~dp0..\..\PROYECTO LOGICA II"

REM --- Compilar TucanTrace si hace falta ---
if not exist "build\classes\tucantrace\Main.class" (
  echo Compilando TucanTrace...
  if not exist "build\classes" mkdir "build\classes"
  if exist "%TEMP%\tt-src.txt" del "%TEMP%\tt-src.txt"
  for /f "delims=" %%f in ('dir /b /s "src\*.java"') do echo %%f>> "%TEMP%\tt-src.txt"
  "%JAVAC_CMD%" -encoding UTF-8 -cp "lib\*" -d "build\classes" @"%TEMP%\tt-src.txt"
)

REM --- Compilar TucanGo (proyecto principal) si hace falta ---
if exist "%TUCANGO%\src" (
  if not exist "%TUCANGO%\build\co\edu\uniamazonia\logica2\PrototipoInteractivo.class" (
    echo Compilando TucanGo...
    if not exist "%TUCANGO%\build" mkdir "%TUCANGO%\build"
    if exist "%TEMP%\tt-tg.txt" del "%TEMP%\tt-tg.txt"
    for /f "delims=" %%f in ('dir /b /s "%TUCANGO%\src\*.java"') do echo %%f>> "%TEMP%\tt-tg.txt"
    "%JAVAC_CMD%" -encoding UTF-8 -d "%TUCANGO%\build" @"%TEMP%\tt-tg.txt"
  )
)

cls
echo ========================================================================
echo     TUCANTRACE  +  Prototipo Interactivo de TUCANGO
echo ========================================================================
echo.
echo     NAVEGADOR (2 pestanas):
echo        Visor UML : http://127.0.0.1:8077/
echo        Terminal  : http://127.0.0.1:8077/terminal
echo.
echo ========================================================================
echo     Mira hacia ABAJO. Cuando aparezca el cartel
echo        ">>> ESCRIBI EN ESTA VENTANA <<<"
echo     escribi ahi mismo:  1  y presiona ENTER  para empezar.
echo ========================================================================
echo.

"%JAVA_CMD%" -Dfile.encoding=UTF-8 -cp "build\classes;lib\*" ^
  tucantrace.Main --live --quiet --delay 0 --http-port 8077 --port 5005 ^
  --exec co.edu.uniamazonia.logica2.PrototipoInteractivo ^
  --exec-cp "%TUCANGO%\build" "%TUCANGO%\src"

echo.
echo (TucanTrace finalizo. Presiona una tecla para cerrar.)
pause >nul
