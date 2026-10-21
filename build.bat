@echo off
setlocal enabledelayedexpansion
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

echo Step 1 of 3: compiling the source code...
if exist out rmdir /s /q out
mkdir out
"%JDKBIN%javac" -encoding UTF-8 -cp "lib\*" -d out src\*.java src\dao\*.java src\exception\*.java src\model\*.java src\report\*.java src\ui\*.java
if errorlevel 1 (
    echo.
    echo Compile failed. Fix the errors shown above, then run build.bat again.
    pause
    exit /b 1
)

echo Step 2 of 3: listing the libraries in lib...
set LIBS=
for %%f in (lib\*.jar) do set LIBS=!LIBS! lib/%%~nxf
> manifest.txt echo Main-Class: Main
>> manifest.txt echo Class-Path:!LIBS!

echo Step 3 of 3: creating dist\WarehouseSystem.jar...
if exist dist rmdir /s /q dist
mkdir dist
"%JDKBIN%jar" cfm dist\WarehouseSystem.jar manifest.txt -C out .
if errorlevel 1 (
    echo The jar could not be created.
    pause
    exit /b 1
)
del manifest.txt
xcopy /e /i /y /q lib dist\lib >nul
if exist db.properties (
    copy /y db.properties dist\ >nul
) else (
    echo WARNING: db.properties was not found in this folder, so it was not copied to dist.
    echo Create it first, or copy it into dist yourself.
)

echo.
echo Done. The program is in the dist folder.
echo Start it with run.bat
pause
