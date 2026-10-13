@echo off
rem Builds and starts the game on Windows. Use "run.bat --plain" on old consoles.
cd /d "%~dp0"
chcp 65001 > nul
if not exist out mkdir out
dir /s /b src\*.java > sources.txt
javac --release 17 -encoding UTF-8 -d out @sources.txt || exit /b 1
del sources.txt
java -cp out rush.Main %*
