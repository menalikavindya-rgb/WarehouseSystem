@echo off
cd /d "%~dp0"
where javaw >nul 2>nul
if errorlevel 1 (
    echo Java was not found. Install the JDK, then try again.
    pause
    exit /b 1
)
if not exist dist\WarehouseSystem.jar (
    echo The program is not built yet. Run build.bat first.
    pause
    exit /b 1
)
start "" javaw -jar dist\WarehouseSystem.jar
