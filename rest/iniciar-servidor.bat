@echo off
cd /d "%~dp0"
rem O Gradle 9 e o Spring Boot 4 precisam de Java 17 ou superior. Ajuste o caminho se necessario.
set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.11"
call gradle --no-daemon bootRun
