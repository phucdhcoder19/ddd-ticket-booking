# Test nhanh cac API: .\test.ps1
$base = "http://localhost:9999"

function Show($title) { Write-Host "`n=== $title ===" -ForegroundColor Cyan }

function Buy($body) {
    try {
        Invoke-RestMethod -Uri "$base/ticket/buy" -Method Post `
            -ContentType "application/json" -Body $body | ConvertTo-Json -Depth 5
    } catch { Write-Host $_.Exception.Message -ForegroundColor Yellow }
}

Show "1. Xem ve id=1"
try { Invoke-RestMethod "$base/ticket/1" | ConvertTo-Json -Depth 5 }
catch { Write-Host $_.Exception.Message -ForegroundColor Red }

Show "2. Dat 2 ve -> mong doi: SUCCESS + orderNumber + totalAmount"
Buy '{"ticketId":1,"userId":101,"quantity":2}'

Show "3. Dat 99999 ve -> mong doi: 409 Het ve, chan o Redis"
Buy '{"ticketId":1,"userId":101,"quantity":99999}'

Show "4. Thieu userId -> mong doi: 400"
Buy '{"ticketId":1,"quantity":2}'

Show "5. Xem lai ve -> so ve phai giam 2"
try { Invoke-RestMethod "$base/ticket/1" | ConvertTo-Json -Depth 5 }
catch { Write-Host $_.Exception.Message -ForegroundColor Red }
