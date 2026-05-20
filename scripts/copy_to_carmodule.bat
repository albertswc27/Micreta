@echo off
REM Lanza el script de PowerShell de copia con permisos relajados.
REM Doble click sobre este .bat para copiar Micreta a CarModule.
powershell -ExecutionPolicy Bypass -File "%~dp0copy_to_carmodule.ps1"
pause
