@echo off
echo ===== CLEANING PROJECT =====
echo Stopping Gradle daemon...
call gradlew --stop

echo Cleaning project...
call gradlew clean

echo Deleting build directories...
rmdir /s /q build 2>nul
rmdir /s /q app\build 2>nul
rmdir /s /q .gradle 2>nul

echo Deleting Gradle caches...
rmdir /s /q %USERPROFILE%\.gradle\caches\modules-2\files-2.1\org.jetbrains.kotlin 2>nul
rmdir /s /q %USERPROFILE%\.gradle\caches\transforms-3 2>nul
rmdir /s /q %USERPROFILE%\.gradle\caches\jars-3 2>nul

echo ===== REBUILDING PROJECT =====
echo Running Gradle with info and stacktrace...
call gradlew build --info --stacktrace

echo ===== BUILD COMPLETED =====
if %ERRORLEVEL% EQU 0 (
    echo Build successful!
) else (
    echo Build failed with error code %ERRORLEVEL%
)
