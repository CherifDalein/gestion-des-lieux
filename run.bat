@echo off
REM GestionDesLieux - Script de compilation et execution (Windows)
REM Usage: run.bat [compile|test|package|run|clean|all]

setlocal enabledelayedexpansion

echo ================================================
echo   GestionDesLieux - Spring Boot Application
echo ================================================
echo.

if exist "gradlew.bat" (
    set GRADLE=gradlew.bat
    echo [INFO] Utilisation du Gradle Wrapper
) else (
    where gradle >nul 2>nul
    if errorlevel 1 (
        echo [ERREUR] Ni gradlew.bat ni Gradle ne sont disponibles!
        echo.
        echo Solutions :
        echo   1. Utilisez le Gradle Wrapper inclus dans le projet
        echo   2. Ou installez Gradle : https://gradle.org/install/
        echo.
        pause
        exit /b 1
    )
    set GRADLE=gradle
    echo [INFO] Utilisation de Gradle systeme
)
echo.

set ACTION=%1
if "%ACTION%"=="" set ACTION=run

if "%ACTION%"=="compile" goto COMPILE
if "%ACTION%"=="test" goto TEST
if "%ACTION%"=="package" goto PACKAGE
if "%ACTION%"=="run" goto RUN
if "%ACTION%"=="clean" goto CLEAN
if "%ACTION%"=="all" goto ALL
goto USAGE

:COMPILE
echo [1/1] Compilation du projet...
call %GRADLE% clean classes -x test
if errorlevel 1 goto ERROR
echo [OK] Compilation reussie
echo.
goto END

:TEST
echo [1/1] Execution des tests...
call %GRADLE% test
if errorlevel 1 goto ERROR
echo [OK] Tests executes avec succes
echo.
goto END

:PACKAGE
echo [1/1] Creation du JAR executable...
call %GRADLE% clean bootJar -x test
if errorlevel 1 goto ERROR
call :FIND_JAR
if errorlevel 1 goto NOJAR
echo [OK] JAR cree : !JAR_PATH!
echo.
goto SHOW_INFO

:RUN
echo Demarrage de l'application...
echo.
call :FIND_JAR
if errorlevel 1 (
    echo JAR introuvable, compilation en cours...
    call %GRADLE% clean bootJar -x test
    if errorlevel 1 goto ERROR
    call :FIND_JAR
    if errorlevel 1 goto NOJAR
)
java -jar "!JAR_PATH!"
goto END

:CLEAN
echo Nettoyage du projet...
call %GRADLE% clean
if errorlevel 1 goto ERROR
if exist "data" (
    del /q data\*.mv.db 2>nul
    del /q data\*.trace.db 2>nul
    del /q data\*.db 2>nul
)
echo [OK] Projet nettoye
echo.
goto END

:ALL
echo [1/3] Compilation du projet...
call %GRADLE% clean classes -x test
if errorlevel 1 goto ERROR
echo [2/3] Execution des tests...
call %GRADLE% test
if errorlevel 1 goto ERROR
echo [3/3] Creation du JAR executable...
call %GRADLE% bootJar
if errorlevel 1 goto ERROR
call :FIND_JAR
if errorlevel 1 goto NOJAR
echo [OK] Build complet termine : !JAR_PATH!
echo.
goto SHOW_INFO

:FIND_JAR
set JAR_PATH=
for %%F in (build\libs\*.jar) do (
    echo %%~nxF | findstr /i /v "plain" >nul
    if not errorlevel 1 (
        set JAR_PATH=build\libs\%%~nxF
        goto JAR_FOUND
    )
)
exit /b 1

:JAR_FOUND
exit /b 0

:SHOW_INFO
echo Application prete.
echo.
echo URLs disponibles :
echo    - API REST : http://localhost:8080
echo    - OpenAPI JSON : http://localhost:8080/api-docs
echo    - H2 Console : http://localhost:8080/h2-console
echo.
echo Base H2 :
echo    - URL JDBC : jdbc:h2:file:./data/lieux_db;MODE=MySQL;DB_CLOSE_DELAY=-1
echo    - Username : sa
echo    - Password : ^(vide^)
echo.
goto END

:NOJAR
echo.
echo [ERREUR] Aucun JAR executable n'a ete trouve dans build\libs !
pause
exit /b 1

:USAGE
echo Usage: %0 {compile^|test^|package^|run^|clean^|all}
echo.
echo   compile  - Compile le code source
echo   test     - Lance les tests
echo   package  - Cree le JAR executable
echo   run      - Lance l'application ^(defaut^)
echo   clean    - Nettoie le projet
echo   all      - Compile, teste et package
goto END

:ERROR
echo.
echo [ERREUR] La commande a echoue!
pause
exit /b 1

:END
endlocal
