@echo off
:start
cmd /C mvn clean package exec:java -Dexec.args="0.0.0.0 153" 
goto start