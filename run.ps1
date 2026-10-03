# Build + run the app:  .\run.ps1
Set-Location $PSScriptRoot

if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) {
    Write-Host "'mvn' not found. Reopen the terminal (PATH was just updated)." -ForegroundColor Red
    exit 1
}

# --- Start MySQL + Redis if they are stopped (containers stop when the machine shuts down) ---
Write-Host "=== Checking Docker ===" -ForegroundColor Cyan
$running = docker ps --format "{{.Names}}"
foreach ($c in @("pre-event-mysql", "pre-event-redis")) {
    if ($running -contains $c) {
        Write-Host "  $c is running" -ForegroundColor Green
    } else {
        Write-Host "  $c is stopped -> starting..." -ForegroundColor Yellow
        docker start $c | Out-Null
        Start-Sleep -Seconds 10
    }
}

Write-Host "`n=== Building 5 modules ===" -ForegroundColor Cyan
mvn -q install -DskipTests
if ($LASTEXITCODE -ne 0) { Write-Host "BUILD FAILED" -ForegroundColor Red; exit 1 }

Write-Host "`n=== Starting the app on port 9999 ===`n" -ForegroundColor Green
mvn -pl train-start spring-boot:run
