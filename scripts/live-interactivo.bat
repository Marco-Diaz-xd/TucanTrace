@echo off
REM ============================================================
REM  TucanTrace + Prototipo Interactivo de TucanGo
REM  Escribi en ESTA ventana. El navegador muestra el UML en vivo.
REM ============================================================
setlocal

if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
title TucanTrace - ESCRIBI ACA
cd /d "%~dp0.."
set "TUCANGO=%~dp0..\..\PROYECTO LOGICA II"

REM --- Compilar TucanTrace si hace falta ---
if not exist "build\classes\tucantrace\Main.class" (
  echo Compilando TucanTrace...
  if not exist "build\classes" mkdir "build\classes"
  if exist "%TEMP%\tt-src.txt" del "%TEMP%\tt-src.txt"
  for /f "delims=" %%f in ('dir /b /s "src\*.java"') do echo "%%f" >> "%TEMP%\tt-src.txt"
  "%JAVA_HOME%\bin\javac.exe" -encoding UTF-8 -cp "lib\*" -d "build\classes" @"%TEMP%\tt-src.txt"
)

REM --- Compilar TucanGo (proyecto principal) si hace falta ---
if not exist "%TUCANGO%\build\co\edu\uniamazonia\logica2\PrototipoInteractivo.class" (
  echo Compilando TucanGo...
  if not exist "%TUCANGO%\build" mkdir "%TUCANGO%\build"
  if exist "%TEMP%\tt-tg.txt" del "%TEMP%\tt-tg.txt"
  for /f "delims=" %%f in ('dir /b /s "%TUCANGO%\src\*.java"') do echo "%%f" >> "%TEMP%\tt-tg.txt"
  "%JAVA_HOME%\bin\javac.exe" -encoding UTF-8 -d "%TUCANGO%\build" @"%TEMP%\tt-tg.txt"
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

"%JAVA_HOME%\bin\java.exe" -Dfile.encoding=UTF-8 -cp "build\classes;lib\*" ^
  tucantrace.Main --live --quiet --delay 0 --http-port 8077 --port 5005 ^
  --exec co.edu.uniamazonia.logica2.PrototipoInteractivo ^
  --exec-cp "%TUCANGO%\build" "%TUCANGO%\src"

echo.
echo (TucanTrace finalizo. Presiona una tecla para cerrar.)
pause >nul
