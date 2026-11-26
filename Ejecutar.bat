@echo off
cd /d "%~dp0"
set "JBR=C:\Program Files\Android\Android Studio\jbr\bin\java.exe"
if exist "%JBR%" (
  "%JBR%" -jar DigitalizateDesktop.jar
) else (
  java -jar DigitalizateDesktop.jar
)
if errorlevel 1 pause
