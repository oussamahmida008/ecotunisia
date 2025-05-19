@echo off
echo ===== SETTING UP JAVA 8 =====
set JAVA_HOME_BACKUP=%JAVA_HOME%

REM Try to find Java 8 in common locations
if exist "C:\Program Files\Java\jdk1.8.0_301" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_301
) else if exist "C:\Program Files\Java\jdk1.8.0_291" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_291
) else if exist "C:\Program Files\Java\jdk1.8.0_281" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_281
) else if exist "C:\Program Files\Java\jdk1.8.0_271" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_271
) else if exist "C:\Program Files\Java\jdk1.8.0_261" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_261
) else if exist "C:\Program Files\Java\jdk1.8.0_251" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_251
) else if exist "C:\Program Files\Java\jdk1.8.0_241" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_241
) else if exist "C:\Program Files\Java\jdk1.8.0_231" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_231
) else if exist "C:\Program Files\Java\jdk1.8.0_221" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_221
) else if exist "C:\Program Files\Java\jdk1.8.0_211" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_211
) else if exist "C:\Program Files\Java\jdk1.8.0_202" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_202
) else if exist "C:\Program Files\Java\jdk1.8.0_201" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_201
) else if exist "C:\Program Files\Java\jdk1.8.0_191" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_191
) else if exist "C:\Program Files\Java\jdk1.8.0_181" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_181
) else if exist "C:\Program Files\Java\jdk1.8.0_171" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_171
) else if exist "C:\Program Files\Java\jdk1.8.0_161" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_161
) else if exist "C:\Program Files\Java\jdk1.8.0_151" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_151
) else if exist "C:\Program Files\Java\jdk1.8.0_144" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_144
) else if exist "C:\Program Files\Java\jdk1.8.0_141" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_141
) else if exist "C:\Program Files\Java\jdk1.8.0_131" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_131
) else if exist "C:\Program Files\Java\jdk1.8.0_121" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_121
) else if exist "C:\Program Files\Java\jdk1.8.0_111" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_111
) else if exist "C:\Program Files\Java\jdk1.8.0_101" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_101
) else if exist "C:\Program Files\Java\jdk1.8.0_91" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_91
) else if exist "C:\Program Files\Java\jdk1.8.0_77" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_77
) else if exist "C:\Program Files\Java\jdk1.8.0_74" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_74
) else if exist "C:\Program Files\Java\jdk1.8.0_73" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_73
) else if exist "C:\Program Files\Java\jdk1.8.0_72" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_72
) else if exist "C:\Program Files\Java\jdk1.8.0_71" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_71
) else if exist "C:\Program Files\Java\jdk1.8.0_66" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_66
) else if exist "C:\Program Files\Java\jdk1.8.0_65" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_65
) else if exist "C:\Program Files\Java\jdk1.8.0_60" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_60
) else if exist "C:\Program Files\Java\jdk1.8.0_51" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_51
) else if exist "C:\Program Files\Java\jdk1.8.0_45" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_45
) else if exist "C:\Program Files\Java\jdk1.8.0_40" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_40
) else if exist "C:\Program Files\Java\jdk1.8.0_31" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_31
) else if exist "C:\Program Files\Java\jdk1.8.0_25" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_25
) else if exist "C:\Program Files\Java\jdk1.8.0_20" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_20
) else if exist "C:\Program Files\Java\jdk1.8.0_11" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_11
) else if exist "C:\Program Files\Java\jdk1.8.0_05" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_05
) else if exist "C:\Program Files\Java\jdk1.8.0" (
    set JAVA_HOME=C:\Program Files\Java\jdk1.8.0
) else (
    echo Java 8 not found in common locations.
    echo Please install Java 8 or set JAVA_HOME manually.
    exit /b 1
)

echo Using Java 8 from: %JAVA_HOME%
echo.

echo ===== CLEANING PROJECT =====
echo Stopping Gradle daemon...
call gradlew --stop

echo Cleaning project...
call gradlew clean

echo Deleting build directories...
rmdir /s /q build 2>nul
rmdir /s /q app\build 2>nul
rmdir /s /q .gradle 2>nul

echo ===== REBUILDING PROJECT =====
echo Running Gradle with info and stacktrace...
call gradlew build --info --stacktrace

echo ===== RESTORING JAVA HOME =====
set JAVA_HOME=%JAVA_HOME_BACKUP%

echo ===== BUILD COMPLETED =====
if %ERRORLEVEL% EQU 0 (
    echo Build successful!
) else (
    echo Build failed with error code %ERRORLEVEL%
)
