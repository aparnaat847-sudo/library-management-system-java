@echo off
cd /d "%~dp0"
javaw -jar LibraryManagement.jar
if errorlevel 1 (
  echo Java was not found or failed to start. Install JDK 17 or newer from https://adoptium.net
  pause
)
