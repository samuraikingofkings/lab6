@echo off
cd /d "%~dp0"
rem O cliente usa java.net.http, que precisa de Java 11 ou superior. Ajuste o caminho se necessario.
"C:\Program Files\Java\jdk-21.0.11\bin\java" cliente\ClienteREST.java
