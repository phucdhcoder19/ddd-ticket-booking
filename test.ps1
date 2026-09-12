# Test nhanh cac API: .\test.ps1
$base = "http://localhost:9999"

function Show($title) { Write-Host "`n=== $title ===" -ForegroundColor Cyan }

Show "1. Xem ve id=1"
try { Invoke-RestMethod "$base/ticket/1" | ConvertTo-Json -Depth 5 }
catch { Write-Host $_.Exception.Message -ForegroundColor Red }

Show "2. Mua 2 ve  -> mong doi: OK"
try {
    Invoke-RestMethod -Uri "$base/ticket/buy" -Method Post `
        -ContentType "application/json" -Body '{"ticketId":1,"quantity":2}' | ConvertTo-Json
} catch { Write-Host $_.Exception.Message -ForegroundColor Red }

Show "3. Mua 99999 ve -> mong doi: HET_VE, chan o Redis, KHONG co SQL"
try {
    Invoke-RestMethod -Uri "$base/ticket/buy" -Method Post `
        -ContentType "application/json" -Body '{"ticketId":1,"quantity":99999}' | ConvertTo-Json
} catch { Write-Host $_.Exception.Message -ForegroundColor Red }

Show "4. Mua -5 ve -> mong doi: 400, chan o @Valid"
try {
    Invoke-RestMethod -Uri "$base/ticket/buy" -Method Post `
        -ContentType "application/json" -Body '{"ticketId":1,"quantity":-5}' | ConvertTo-Json
} catch { Write-Host "Bi chan (dung nhu mong doi): $($_.Exception.Message)" -ForegroundColor Yellow }

Show "5. Xem lai ve -> so ve phai giam 2"
try { Invoke-RestMethod "$base/ticket/1" | ConvertTo-Json -Depth 5 }
catch { Write-Host $_.Exception.Message -ForegroundColor Red }
