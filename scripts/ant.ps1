# =====================================================================
#  ant.ps1 - Ejecuta Apache Ant sin instalación global
#  Descarga los jars de Ant (Maven Central) la primera vez y los cachea.
#
#  Uso:
#     .\scripts\ant.ps1 compile
#     .\scripts\ant.ps1 interactive
#     .\scripts\ant.ps1 jar
#     .\scripts\ant.ps1 clean compile
# =====================================================================

$ErrorActionPreference = 'Stop'

$antLauncherVersion = '1.10.15'
$antVersion = '1.10.15'
$cacheDir = Join-Path $env:LOCALAPPDATA 'Programs\ant-jars'
$launcherJar = Join-Path $cacheDir "ant-launcher-$antLauncherVersion.jar"
$antJar = Join-Path $cacheDir "ant-$antVersion.jar"

# JDK 21 (ajusta si cambia la ruta)
$javaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME.TrimEnd('\') } else { 'C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot' }
$java = Join-Path $javaHome 'bin\java.exe'

if (-not (Test-Path $java)) {
    throw "No se encontró java.exe en '$javaHome'. Define JAVA_HOME."
}

# Descargar jars si faltan
if (-not (Test-Path $launcherJar) -or -not (Test-Path $antJar)) {
    New-Item -ItemType Directory -Force -Path $cacheDir | Out-Null
    $ProgressPreference = 'SilentlyContinue'
    if (-not (Test-Path $launcherJar)) {
        Write-Host "Descargando ant-launcher..." 
        Invoke-WebRequest -UseBasicParsing -OutFile $launcherJar `
            "https://repo1.maven.org/maven2/org/apache/ant/ant-launcher/$antLauncherVersion/ant-launcher-$antLauncherVersion.jar"
    }
    if (-not (Test-Path $antJar)) {
        Write-Host "Descargando ant..."
        Invoke-WebRequest -UseBasicParsing -OutFile $antJar `
            "https://repo1.maven.org/maven2/org/apache/ant/ant/$antVersion/ant-$antVersion.jar"
    }
}

$antClasspath = "$launcherJar;$antJar"
$buildFile = Join-Path (Split-Path $PSScriptRoot -Parent) 'build.xml'

& $java "-cp" $antClasspath "org.apache.tools.ant.launch.Launcher" "-f" $buildFile @args
exit $LASTEXITCODE
