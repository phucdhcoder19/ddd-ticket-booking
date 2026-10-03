# Quick API smoke test: .\test.ps1
# Every @RestController lives under /api (see WebConfig).
$base = "http://localhost:9999/api"

function Show($title) { Write-Host "`n=== $title ===" -ForegroundColor Cyan }

function Buy($body) {
    try {
        Invoke-RestMethod -Uri "$base/ticket/buy" -Method Post `
            -ContentType "application/json" -Body $body | ConvertTo-Json -Depth 5
    } catch { Write-Host $_.Exception.Message -ForegroundColor Yellow }
}

Show "1. View ticket id=1"
try { Invoke-RestMethod "$base/ticket/1" | ConvertTo-Json -Depth 5 }
catch { Write-Host $_.Exception.Message -ForegroundColor Red }

Show "2. Buy 2 tickets -> expected: SUCCESS + orderNumber + totalAmount"
Buy '{"ticketId":1,"userId":101,"quantity":2}'

Show "3. Buy 99999 tickets -> expected: 409 Sold out, rejected at Redis"
Buy '{"ticketId":1,"userId":101,"quantity":99999}'

Show "4. Missing userId -> expected: 400"
Buy '{"ticketId":1,"quantity":2}'

Show "5. View the ticket again -> stock must be 2 lower"
try { Invoke-RestMethod "$base/ticket/1" | ConvertTo-Json -Depth 5 }
catch { Write-Host $_.Exception.Message -ForegroundColor Red }
