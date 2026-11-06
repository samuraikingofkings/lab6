@echo off
cd /d "%~dp0"
idlj -fall -td gerado Banco.idl || exit /b 1
if not exist bin mkdir bin
javac -encoding UTF-8 -d bin gerado\SistemaBancario\*.java ServidorBanco.java ClienteBanco.java || exit /b 1
echo Compilacao concluida.
