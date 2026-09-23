@echo off
title Komal's Birthday Surprise Web Experience
echo ========================================================
echo   Happy Birthday Komal! 🌸✨
echo   Starting local server at http://localhost:8080/ ...
echo ========================================================
powershell -ExecutionPolicy Bypass -File "%~dp0serve.ps1"
if errorlevel 1 (
    echo Starting direct browser preview...
    start "" "%~dp0index.html"
)
