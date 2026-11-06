@echo off
cd /d "%~dp0"
rem Apaga o estado persistente do orbd (referencias antigas com o IP da rede)
if exist orb.db rmdir /s /q orb.db
orbd -ORBInitialPort 1050 -ORBServerHost 127.0.0.1
