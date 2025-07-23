@echo off
powershell -Command ".\mvnw spring-boot:run | Tee-Object -FilePath build.log"
