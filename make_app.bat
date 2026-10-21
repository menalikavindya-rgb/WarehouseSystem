@echo off
cd /d "%~dp0"

rem --- find the JDK tools (jar, javac, jpackage) even when only some of them are on the PATH ---
set "JDKBIN="
if defined JAVA_HOME if exist "%JAVA_HOME%\bin\jar.exe" set "JDKBIN=%JAVA_HOME%\bin\"
if not defined JDKBIN (
    for /d %%d in ("%ProgramFiles%\Java\jdk*" "%ProgramFiles%\Eclipse Adoptium\jdk*" "%ProgramFiles%\Microsoft\jdk*" "%ProgramFiles%\Zulu\zulu*" "%ProgramFiles%\Amazon Corretto\jdk*" "%ProgramFiles%\BellSoft\LibericaJDK*") do (
        if exist "%%~d\bin\jar.exe" set "JDKBIN=%%~d\bin\"
    )
)
if not defined JDKBIN (
    where jar >nul 2>nul
    if errorlevel 1 (
        echo The JDK tools were not found. Install the full JDK, or set the JAVA_HOME variable to the JDK folder.
        pause
        exit /b 1
    )
)
if defined JDKBIN echo Using the JDK in %JDKBIN%
if not exist dist\WarehouseSystem.jar (
    echo Run build.bat first.
    pause
    exit /b 1
)
if exist installer rmdir /s /q installer
"%JDKBIN%jpackage" --type app-image --name WarehouseSystem --input dist --main-jar WarehouseSystem.jar --main-class Main --dest installer
if errorlevel 1 (
    echo jpackage failed. Make sure the JDK is installed and its bin folder is on the PATH.
    pause
    exit /b 1
)
copy /y db.properties installer\WarehouseSystem\ >nul 2>nul
echo.
echo Done. Open the folder installer\WarehouseSystem and double-click WarehouseSystem.exe
echo The database settings are in installer\WarehouseSystem\db.properties (edit that file on each computer).
echo To get a desktop icon: right-click WarehouseSystem.exe, Send to, Desktop (create shortcut).
pause
