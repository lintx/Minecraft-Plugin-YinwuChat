@echo off
echo ========================================
echo YinwuChat Multi-Platform Build Script
echo ========================================
echo.

if "%1"=="" goto :help
if "%1"=="all" goto :build_all
if "%1"=="velocity" goto :build_velocity
if "%1"=="bukkit" goto :build_bukkit
goto :help

:build_all
echo Building all platforms...
call mvn clean package -q
echo.
echo Build completed! Check target/ directory for JAR files.
goto :end

:build_velocity
echo Building Velocity platform...
call mvn clean package -P velocity -q
echo.
echo Velocity JAR created: target/YinwuChat-Velocity-2.12.jar
goto :end

:build_bukkit
echo Building Bukkit platform...
call mvn clean package -P bukkit -q
echo.
echo Bukkit JAR created: target/YinwuChat-Bukkit-2.12.jar
goto :end

:help
echo Usage: build.bat [platform]
echo.
echo Platforms:
echo   all       - Build all platforms (default)
echo   velocity  - Build only Velocity proxy plugin
echo   bukkit    - Build only Bukkit/Spigot plugin
echo.
echo Examples:
echo   build.bat all        - Build both Velocity and Bukkit versions
echo   build.bat velocity   - Build only Velocity version
echo   build.bat bukkit     - Build only Bukkit version
echo.
echo Output files:
echo   - target/YinwuChat-2.12.jar (all platforms)
echo   - target/YinwuChat-Velocity-2.12.jar (Velocity only)
echo   - target/YinwuChat-Bukkit-2.12.jar (Bukkit only)
echo.
goto :end

:end
echo.
echo Press any key to exit...
pause >nul
