@echo off
title Compilando Epic Vanguard Mod...
echo ========================================================
echo   Iniciando build do Epic Vanguard Mod
echo ========================================================
echo.

if not defined JAVA_HOME (
    if exist "%APPDATA%\.minecraft\runtime\java-runtime-gamma\windows\java-runtime-gamma\bin\java.exe" (
        set "JAVA_HOME=%APPDATA%\.minecraft\runtime\java-runtime-gamma\windows\java-runtime-gamma"
        set "PATH=%JAVA_HOME%\bin;%PATH%"
    )
)

call gradlew.bat build -x test

echo.
if %ERRORLEVEL% equ 0 (
    echo ========================================================
    echo   BUILD CONCLUIDA COM SUCESSO!
    echo   Arquivo .jar gerado em:
    echo   build\libs\epicvanguard-1.0.0.jar
    echo ========================================================
) else (
    echo ========================================================
    echo   ERRO NA COMPILACAO. Verifique as mensagens acima.
    echo ========================================================
)
echo.
pause
