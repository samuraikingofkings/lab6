@echo off
cd /d "%~dp0"
java -cp bin ServidorBanco -ORBInitialPort 1050 -ORBInitialHost 127.0.0.1 -ORBServerHost 127.0.0.1
