# Build + chay app:  .\run.ps1
Set-Location $PSScriptRoot

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "Khong tim thay 'mvn'. Mo lai terminal (PATH vua duoc cap nhat)." -ForegroundColor Red
    exit 1
}

# --- Bat MySQL + Redis neu dang tat (container tu tat khi tat may) ---
Write-Host "=== Kiem tra Docker ===" -ForegroundColor Cyan
$running = docker ps --format "{{.Names}}"
foreach ($c in @("pre-event-mysql", "pre-event-redis")) {
    if ($running -contains $c) {
        Write-Host "  $c dang chay" -ForegroundColor Green
    } else {
        Write-Host "  $c dang tat -> dang bat..." -ForegroundColor Yellow
        docker start $c | Out-Null
        Start-Sleep -Seconds 10
    }
}

Write-Host "`n=== Build 5 module ===" -ForegroundColor Cyan
mvn -q install -DskipTests
if ($LASTEXITCODE -ne 0) { Write-Host "BUILD LOI" -ForegroundColor Red; exit 1 }

Write-Host "`n=== Khoi dong app tren cong 9999 ===`n" -ForegroundColor Green
mvn -pl train-start spring-boot:run
