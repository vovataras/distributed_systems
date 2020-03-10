@echo off
:start
cmd /C mvn package exec:java -Dexec.args="0.0.0.0 151" 
goto start