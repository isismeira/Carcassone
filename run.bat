@echo off
setlocal
cd /d "%~dp0"

set "PORTA=%~1"
if not defined PORTA set "PORTA=8080"

if not exist build mkdir build

echo Compilando...
javac -d build src\java\carcassonne\*.java src\java\carcassonne\modelo\*.java src\java\carcassonne\controle\*.java src\java\carcassonne\visao\*.java
if errorlevel 1 exit /b 1

echo Subindo o servidor na porta %PORTA%...
java -cp build carcassonne.Principal --porta %PORTA%
