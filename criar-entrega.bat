@echo off
cd /d "%~dp0"
rem Cria a entrega sem a base de dados do orbd (corba\orb.db), que e regenerada ao arrancar
if exist entrega-lab6.zip del entrega-lab6.zip
%SystemRoot%\System32\tar.exe -a -c -f entrega-lab6.zip --exclude "orb.db" --exclude "entrega-lab6.zip" * || exit /b 1
echo Criado entrega-lab6.zip
