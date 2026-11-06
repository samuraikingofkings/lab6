@echo off
cd /d "%~dp0"
if not exist bin mkdir bin
javac -encoding UTF-8 -cp ..\corba\bin -d bin TesteDesempenho.java || exit /b 1
java -cp "bin;..\corba\bin" TesteDesempenho -ORBInitialPort 1050 -ORBInitialHost 127.0.0.1 -ORBServerHost 127.0.0.1
