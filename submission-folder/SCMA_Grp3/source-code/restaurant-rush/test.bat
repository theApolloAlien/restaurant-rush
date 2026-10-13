@echo off
rem Compiles the game and the rule checks, then runs them on Windows.
rem javac starts from RulesTest and compiles every class it uses from src and test. Relative
rem paths keep this working when the folder name contains spaces.
cd /d "%~dp0"
chcp 65001 > nul
if exist out-test rmdir /s /q out-test
javac --release 17 -encoding UTF-8 -sourcepath "src;test" -d out-test test\rush\RulesTest.java || exit /b 1
java -cp out-test rush.RulesTest
