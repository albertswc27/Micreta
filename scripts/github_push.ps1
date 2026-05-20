# Sube Micreta v0.2.0 a https://github.com/albertswc27/Micreta.git
#
# Requisitos en tu Windows:
#   1. Git for Windows instalado (`git --version` debe responder)
#   2. Credenciales de GitHub configuradas (Git Credential Manager las cachea
#      al primer push y te pide login en el navegador)
#
# Uso:
#   powershell -ExecutionPolicy Bypass -File scripts\github_push.ps1
#   o doble click sobre scripts\github_push.bat

$ErrorActionPreference = "Stop"
$repoUrl = "https://github.com/albertswc27/Micreta.git"
$repoRoot = Split-Path -Parent $PSScriptRoot

Write-Host "Raíz del proyecto: $repoRoot" -ForegroundColor Cyan
Set-Location $repoRoot

# 1. Verificar git instalado
try {
    $gitVersion = git --version
    Write-Host "$gitVersion" -ForegroundColor Green
} catch {
    Write-Host "Git no está instalado. Descárgalo de https://git-scm.com/download/win y vuelve a ejecutar." -ForegroundColor Red
    Read-Host "Pulsa Enter para salir"
    exit 1
}

# 2. Si hay .git/ corrupto del sandbox, borrarlo
if (Test-Path ".git") {
    $configValid = $false
    try {
        # Si el config se puede leer, asumimos repo válido
        $null = git -C "." rev-parse --git-dir 2>$null
        if ($LASTEXITCODE -eq 0) { $configValid = $true }
    } catch {}
    if (-not $configValid) {
        Write-Host "Detectado .git/ inválido — limpiando..." -ForegroundColor Yellow
        # Quitar atributo read-only que puede haber dejado el sandbox
        Get-ChildItem -Path ".git" -Recurse -Force | ForEach-Object {
            $_.Attributes = 'Normal'
        }
        Remove-Item ".git" -Recurse -Force -ErrorAction SilentlyContinue
    }
}

# 3. Inicializar repo si no existe
if (-not (Test-Path ".git")) {
    Write-Host "git init..." -ForegroundColor Cyan
    git init -q
    git branch -M main
}

# 4. Configurar identidad local (solo para este repo)
git config user.email "albert.cabezuelo.arevalo@gmail.com"
git config user.name "Albert Cabezuelo"

# 5. Añadir todo, respetando .gitignore
Write-Host "git add -A..." -ForegroundColor Cyan
git add -A
$staged = git status --short
Write-Host "Archivos staged:" -ForegroundColor Cyan
Write-Host $staged

# 6. Commit (solo si hay cambios)
$hasChanges = (git status --short).Length -gt 0
$alreadyCommitted = (git rev-parse --verify HEAD 2>$null) -ne $null

if ($hasChanges) {
    $message = if ($alreadyCommitted) {
        "chore: sync changes"
    } else {
        "Initial commit — Micreta v0.2.0 'Daily driver'`n`n" +
        "24 features del sprint v0.2.0 + endurecimiento v0.1.0.`n" +
        "Ver FEATURES.md, README.md y docs/adr/ para el detalle."
    }
    Write-Host "git commit..." -ForegroundColor Cyan
    git commit -q -m $message
} else {
    Write-Host "No hay cambios para commitear." -ForegroundColor Yellow
}

# 7. Remote
$existingRemote = git remote get-url origin 2>$null
if (-not $existingRemote) {
    Write-Host "git remote add origin $repoUrl" -ForegroundColor Cyan
    git remote add origin $repoUrl
} elseif ($existingRemote -ne $repoUrl) {
    Write-Host "Actualizando remote origin → $repoUrl" -ForegroundColor Yellow
    git remote set-url origin $repoUrl
} else {
    Write-Host "Remote origin ya apunta a $repoUrl" -ForegroundColor Green
}

# 8. Push
Write-Host "" -ForegroundColor White
Write-Host "git push -u origin main..." -ForegroundColor Cyan
Write-Host "Si es la primera vez, Git te abrirá una ventana para autenticarte" -ForegroundColor Yellow
Write-Host "con GitHub (login + autorización). Solo pasa una vez." -ForegroundColor Yellow
git push -u origin main

if ($LASTEXITCODE -eq 0) {
    Write-Host "" -ForegroundColor White
    Write-Host "Subido. Repo en https://github.com/albertswc27/Micreta" -ForegroundColor Green
} else {
    Write-Host "" -ForegroundColor White
    Write-Host "El push falló. Causas habituales:" -ForegroundColor Red
    Write-Host "  - El repo ya tiene commits remotos. Ejecuta: git pull --rebase origin main && git push" -ForegroundColor Red
    Write-Host "  - Credenciales no aceptadas. Cierra cualquier popup de auth e intenta otra vez." -ForegroundColor Red
}

Read-Host "Pulsa Enter para cerrar"
