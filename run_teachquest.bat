@echo off
start "TeachQuest Frontend" cmd /k "cd /d C:\xampp\htdocs\TeachQuest\frontend && npm run dev"
start "TeachQuest Backend" cmd /k "cd /d C:\xampp\htdocs\TeachQuest\backend && .\mvnw.cmd spring-boot:run"
