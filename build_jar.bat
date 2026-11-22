@echo off
setlocal EnableDelayedExpansion
title Compilando Epic Vanguard Mod...
echo ========================================================
echo   Iniciando build do Epic Vanguard Mod
echo ========================================================
echo.

rem Usa o Java 17 que vem com o launcher do Minecraft se JAVA_HOME nao estiver definido.
if not defined JAVA_HOME (
    set "MC_JAVA=%APPDATA%\.minecraft\runtime\java-runtime-gamma\windows\java-runtime-gamma"
    if exist "!MC_JAVA!\bin\java.exe" (
        set "JAVA_HOME=!MC_JAVA!"
        set "PATH=!MC_JAVA!\bin;!PATH!"
    )
)

call gradlew.bat build -x test
set "BUILD_RESULT=%ERRORLEVEL%"

echo.
if "%BUILD_RESULT%"=="0" (
    echo ========================================================
    echo   BUILD CONCLUIDA COM SUCESSO!
    echo   Arquivo .jar gerado em build\libs\:
    for %%F in ("build\libs\EpicVanguard *.jar") do echo     %%~nxF
    echo ========================================================
) else (
    echo ========================================================
    echo   ERRO NA COMPILACAO. Verifique as mensagens acima.
    echo ========================================================
)
echo.
pause
endlocal
