@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "BUNDLE_JAR_PATH=%SCRIPT_DIR%snippet-tray-manager.jar"
set "DEV_JAR_PATH=%SCRIPT_DIR%build\libs\snippet-tray-manager-1.0.0-all.jar"
set "JAR_PATH="

set "MSG_MISSING=Java is not installed or not on your PATH."
set "MSG_INSTALL=Please install Java 17 or newer, then run this script again."
set "MSG_DOWNLOAD=Download: https://adoptium.net/"

where java >nul 2>&1
if errorlevel 1 (
  powershell -NoProfile -Command "Add-Type -AssemblyName PresentationFramework; [System.Windows.MessageBox]::Show('%MSG_MISSING%`n`n%MSG_INSTALL%`n`n%MSG_DOWNLOAD%','Snippet Tray Manager','OK','Error')" >nul 2>&1
  echo Error: %MSG_MISSING%
  echo %MSG_INSTALL%
  echo %MSG_DOWNLOAD%
  exit /b 1
)

set "JAVA_VERSION_RAW="
for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
  set "JAVA_VERSION_RAW=%%~v"
  goto :version_found
)

:version_found
if "%JAVA_VERSION_RAW%"=="" (
  powershell -NoProfile -Command "Add-Type -AssemblyName PresentationFramework; [System.Windows.MessageBox]::Show('Could not determine Java version.`n`nPlease install Java 17 or newer and try again.','Snippet Tray Manager','OK','Error')" >nul 2>&1
  echo Error: Could not determine Java version.
  exit /b 1
)

set "JAVA_VERSION_RAW=%JAVA_VERSION_RAW:"=%"
set "JAVA_MAJOR=%JAVA_VERSION_RAW%"
set "JAVA_MAJOR_INVALID="

for /f "tokens=1,2 delims=." %%a in ("%JAVA_VERSION_RAW%") do (
  if "%%a"=="1" (
    set "JAVA_MAJOR=%%b"
  ) else (
    set "JAVA_MAJOR=%%a"
  )
)

for /f "delims=0123456789" %%x in ("%JAVA_MAJOR%") do set "JAVA_MAJOR_INVALID=1"
if defined JAVA_MAJOR_INVALID (
  powershell -NoProfile -Command "Add-Type -AssemblyName PresentationFramework; [System.Windows.MessageBox]::Show('Could not determine Java major version from: %JAVA_VERSION_RAW%`n`nPlease install Java 17 or newer and try again.','Snippet Tray Manager','OK','Error')" >nul 2>&1
  echo Error: Could not determine Java major version from: %JAVA_VERSION_RAW%
  exit /b 1
)

if %JAVA_MAJOR% LSS 17 (
  powershell -NoProfile -Command "Add-Type -AssemblyName PresentationFramework; [System.Windows.MessageBox]::Show('Detected Java version: %JAVA_VERSION_RAW%`n`nJava 17 or newer is required.`nPlease upgrade Java and run this script again.','Snippet Tray Manager','OK','Error')" >nul 2>&1
  echo Error: Detected Java version %JAVA_VERSION_RAW%. Java 17 or newer is required.
  exit /b 1
)

if exist "%BUNDLE_JAR_PATH%" (
  set "JAR_PATH=%BUNDLE_JAR_PATH%"
) else if exist "%DEV_JAR_PATH%" (
  set "JAR_PATH=%DEV_JAR_PATH%"
) else (
  if exist "%SCRIPT_DIR%gradlew.bat" (
    echo Jar not found. Building first...
    call "%SCRIPT_DIR%gradlew.bat" -p "%SCRIPT_DIR%" shadowJar
    if errorlevel 1 exit /b 1
    set "JAR_PATH=%DEV_JAR_PATH%"
  ) else (
    powershell -NoProfile -Command "Add-Type -AssemblyName PresentationFramework; [System.Windows.MessageBox]::Show('Could not find application jar.`n`nExpected either:`n- %BUNDLE_JAR_PATH%`n- %DEV_JAR_PATH%`n`nAlso, Gradle wrapper was not found in this folder.','Snippet Tray Manager','OK','Error')" >nul 2>&1
    echo Error: Could not find application jar and Gradle wrapper was not found in this folder.
    exit /b 1
  )
)

java -jar "%JAR_PATH%"

endlocal
