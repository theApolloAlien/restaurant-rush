@echo off
rem Builds and starts the game on Windows. Use "run.bat --plain" on old consoles.
rem javac starts from Main and compiles every class it uses from src. Relative paths keep this
rem working when the folder name contains spaces.
cd /d "%~dp0"
chcp 65001 > nul
if exist out rmdir /s /q out
javac --release 17 -encoding UTF-8 -sourcepath src -d out src\rush\Main.java || exit /b 1
java -cp out rush.Main %*
