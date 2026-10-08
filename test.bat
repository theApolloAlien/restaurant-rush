@echo off
rem Compiles the game and the rule checks, then runs them on Windows.
cd /d "%~dp0"
chcp 65001 > nul
if not exist out-test mkdir out-test
dir /s /b src\*.java test\*.java > sources.txt
javac --release 17 -encoding UTF-8 -d out-test @sources.txt || exit /b 1
del sources.txt
java -cp out-test rush.RulesTest
