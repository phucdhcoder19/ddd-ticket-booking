# End-to-end check of the seat booking flow: .\test-flow.ps1
# Covers: waiting room -> hold -> passengers -> order -> my tickets,
# and Idempotency-Key replay on POST /holds and POST /orders.
# Needs the app running (.\run.ps1) with seeded stations and trains.
$base = "http://localhost:9999/api"
$failures = 0

function Show($title) { Write-Host "`n=== $title ===" -ForegroundColor Cyan }
function Check($ok, $label) {
    if ($ok) { Write-Host "  PASS  $label" -ForegroundColor Green }
    else     { Write-Host "  FAIL  $label" -ForegroundColor Red; $script:failures++ }
}
# The backend always answers HTTP 200 and puts the real status in the body (ResultMessage)
function Call($method, $path, $body = $null, $headers = @{}) {
    $params = @{ Uri = "$base$path"; Method = $method; Headers = $headers; ContentType = "application/json" }
    if ($body) { $params.Body = ($body | ConvertTo-Json -Depth 5) }
    Invoke-RestMethod @params
}

Show "1. Join the waiting room"
$queue = Call POST "/queue"
Check ($queue.result.status -eq "ADMITTED") "admitted right away (status=$($queue.result.status))"
$pass = @{ "X-Queue-Token" = $queue.result.token }

Show "2. Find a trip and a free berth"
$date = "2027-01-28"
$trips = Call GET "/trips?from=HNO&to=DNA&date=$date"
$trip = $trips.result | Select-Object -First 1
Check ($null -ne $trip) "found trip $($trip.trainCode) (id=$($trip.id))"
$carriages = Call GET "/trips/$($trip.id)/carriages?seatClass=BERTH_4&from=HNO&to=DNA"
$free = $carriages.result | ForEach-Object { $_.seats } | Where-Object { $_.status -eq "available" } | Select-Object -First 2
Check ($free.Count -eq 2) "found 2 free berths: $($free[0].id), $($free[1].id)"

Show "3. Hold without a queue token -> expected 403"
$holdBody = @{ tripId = [long]$trip.id; seatClass = "BERTH_4"; seatIds = @($free[0].id); from = "HNO"; to = "DNA" }
$noPass = Call POST "/holds" $holdBody
Check ($noPass.code -eq 403) "rejected with code $($noPass.code)"

Show "4. Hold twice with the same Idempotency-Key -> same hold"
$key1 = [guid]::NewGuid().ToString()
$h1 = Call POST "/holds" $holdBody ($pass + @{ "Idempotency-Key" = $key1 })
$h2 = Call POST "/holds" $holdBody ($pass + @{ "Idempotency-Key" = $key1 })
Check ($h1.success) "first hold succeeded ($($h1.result.holdId))"
Check ($h2.success -and $h2.result.holdId -eq $h1.result.holdId) "second call replayed the same hold ($($h2.result.holdId))"

Show "5. Same key, different seat -> expected 422"
$otherBody = $holdBody.Clone(); $otherBody.seatIds = @($free[1].id)
$reused = Call POST "/holds" $otherBody ($pass + @{ "Idempotency-Key" = $key1 })
Check ($reused.code -eq 422) "rejected with code $($reused.code)"

Show "6. Order before passengers -> expected 422"
$holdId = $h1.result.holdId
$early = Call POST "/orders" @{ holdId = $holdId; paymentMethod = "VNPAY" } $pass
Check ($early.code -eq 422) "rejected with code $($early.code)"

Show "7. Save the passenger (student discount)"
$saved = Call PUT "/holds/$holdId/passengers" @{ passengers = @(@{
    seatId = $free[0].id; fullName = "Jane Doe"; idNumber = "001203004567"; phone = "0912345678"; discount = "STUDENT" }) }
Check ($saved.success) "passengers saved"

Show "8. Pay twice with the same Idempotency-Key -> same order"
$key2 = [guid]::NewGuid().ToString()
$o1 = Call POST "/orders" @{ holdId = $holdId; paymentMethod = "VNPAY" } ($pass + @{ "Idempotency-Key" = $key2 })
$o2 = Call POST "/orders" @{ holdId = $holdId; paymentMethod = "VNPAY" } ($pass + @{ "Idempotency-Key" = $key2 })
Check ($o1.success) "order created ($($o1.result.code), total $($o1.result.totalAmount))"
Check ($o2.success -and $o2.result.code -eq $o1.result.code) "second call replayed the same order"
$base0 = $h1.result.items[0].price
# AwayFromZero = the server's HALF_UP; PowerShell's default would round .5 to even
$expected = [math]::Round($base0 * 0.9 / 1000, [MidpointRounding]::AwayFromZero) * 1000
Check ($o1.result.totalAmount -eq $expected) "total $($o1.result.totalAmount) = base $base0 minus 10% student discount"

Show "9. My tickets"
$mine = Call GET "/me/tickets"
$ticket = $mine.result | Where-Object { $_.orderCode -eq $o1.result.code } | Select-Object -First 1
Check ($null -ne $ticket) "ticket $($ticket.code) listed"
Check ($ticket.passengerId -like "001*4567" -and $ticket.passengerId -notlike "*203004*") "ID number is masked ($($ticket.passengerId))"
Check ($ticket.status -eq "VALID") "status VALID"

Write-Host ""
if ($failures -eq 0) { Write-Host "ALL CHECKS PASSED" -ForegroundColor Green }
else { Write-Host "$failures CHECK(S) FAILED" -ForegroundColor Red; exit 1 }
