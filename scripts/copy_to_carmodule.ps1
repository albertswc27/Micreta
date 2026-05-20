# Copia el proyecto Micreta a C:\Users\alber\OneDrive\Escritorio\Proyectos\CarModule
# Excluye build artifacts, .gradle, .idea, etc.
#
# Uso (desde PowerShell, en la raíz del proyecto):
#   powershell -ExecutionPolicy Bypass -File scripts\copy_to_carmodule.ps1
#
# Si la carpeta destino ya existe, se sincroniza (no se borra).
# Si quieres limpieza total, añade el flag -Clean.

param(
    [switch]$Clean = $false
)

$source = $PSScriptRoot | Split-Path -Parent
$destination = "C:\Users\alber\OneDrive\Escritorio\Proyectos\CarModule"

Write-Host "Origen      : $source" -ForegroundColor Cyan
Write-Host "Destino     : $destination" -ForegroundColor Cyan

if (-not (Test-Path $destination)) {
    Write-Host "Creando $destination ..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $destination -Force | Out-Null
}

if ($Clean) {
    Write-Host "Limpieza activada — borrando contenido previo del destino..." -ForegroundColor Yellow
    Get-ChildItem -Path $destination -Force | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
}

$excludeDirs = @(
    "build",
    ".gradle",
    ".idea",
    "captures",
    ".cxx",
    ".externalNativeBuild",
    "local.properties"
)
$excludeFiles = @(
    "*.iml",
    "*.apk",
    "*.aab",
    "*.keystore",
    ".DS_Store"
)

# Robocopy: rápido, idempotente, perfecto para sincronizar
$args = @(
    $source, $destination,
    "/MIR",                      # mirror (espeja origen → destino, borra extras)
    "/XD"
) + $excludeDirs + @("/XF") + $excludeFiles + @(
    "/R:1", "/W:1",              # 1 reintento, 1s espera
    "/NFL", "/NDL", "/NP",       # output más limpio
    "/MT:8"                      # multi-thread
)

Write-Host "`nRobocopy en marcha..." -ForegroundColor Cyan
& robocopy @args
$code = $LASTEXITCODE

# Robocopy: códigos 0-7 son éxito, 8+ son error real
if ($code -ge 8) {
    Write-Host "`nError en robocopy (código $code)." -ForegroundColor Red
    exit $code
}

Write-Host "`n✅ Copia completada en $destination" -ForegroundColor Green
Write-Host "Abre la nueva ubicación en Android Studio para trabajar desde ahí." -ForegroundColor Green
