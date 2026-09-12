# Build + chay app:  .\run.ps1
Set-Location $PSScriptRoot

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "Khong tim thay 'mvn'. Mo lai terminal (PATH vua duoc cap nhat)." -ForegroundColor Red
    exit 1
}

Write-Host "=== Build 5 module ===" -ForegroundColor Cyan
mvn -q install -DskipTests
if ($LASTEXITCODE -ne 0) { Write-Host "BUILD LOI" -ForegroundColor Red; exit 1 }

Write-Host "`n=== Khoi dong app tren cong 9999 ===`n" -ForegroundColor Green
mvn -pl train-start spring-boot:run
