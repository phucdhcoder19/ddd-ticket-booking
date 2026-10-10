# End-to-end check of the seat booking flow: .\test-flow.ps1
# Covers: sign-up -> waiting room -> hold -> passengers -> order -> my tickets -> refund,
# Idempotency-Key replay on POST /holds and POST /orders, and that another
# account cannot see or touch any of it.
# Needs the app running (.\run.ps1) with seeded stations and trains.
# Another instance: .\test-flow.ps1 -base http://localhost:9998/api
param([string]$base = "http://localhost:9999/api")
$failures = 0

function Show($title) { Write-Host "`n=== $title ===" -ForegroundColor Cyan }
function Check($ok, $label) {
    if ($ok) { Write-Host "  PASS  $label" -ForegroundColor Green }
    else     { Write-Host "  FAIL  $label" -ForegroundColor Red; $script:failures++ }
}
# The backend answers HTTP 200 and puts the real status in the body (ResultMessage).
# The one exception is the sign-in gate, a real HTTP 401: turned into the same shape here.
function Call($method, $path, $body = $null, $headers = @{}) {
    $params = @{ Uri = "$base$path"; Method = $method; Headers = $headers; ContentType = "application/json" }
    if ($body) { $params.Body = ($body | ConvertTo-Json -Depth 5) }
    try { Invoke-RestMethod @params }
    catch {
        if (-not $_.Exception.Response) { throw }
        [pscustomobject]@{ success = $false; code = [int]$_.Exception.Response.StatusCode }
    }
}
# A fresh account per run: 09 + 8 random digits
function NewAccount($name) {
    $phone = "09" + (-join (1..8 | ForEach-Object { Get-Random -Maximum 10 }))
    $r = Call POST "/auth/register" @{ phone = $phone; fullName = $name; password = "flow-test-1234" }
    @{ ok = $r.success; auth = @{ Authorization = "Bearer $($r.result.token)" }; phone = $phone }
}

Show "0. Create two accounts"
$userA = NewAccount "Flow Tester"
$userB = NewAccount "Other Person"
Check ($userA.ok -and $userB.ok) "registered A ($($userA.phone)) and B ($($userB.phone))"
$wrong = Call POST "/auth/login" @{ phone = $userA.phone; password = "not-the-password" }
Check ($wrong.code -eq 422) "wrong password rejected with code $($wrong.code)"
$authA = $userA.auth

Show "1. Join the waiting room"
$queue = Call POST "/queue"
Check ($queue.result.status -eq "ADMITTED") "admitted right away (status=$($queue.result.status))"
# Every booking call carries both the admission pass and A's token
$pass = @{ "X-Queue-Token" = $queue.result.token } + $authA

Show "2. Find a trip and a free berth"
$date = "2027-01-28"
$trips = Call GET "/trips?from=HNO&to=DNA&date=$date"
# Not every train has 4-berth sleepers (SE7 does not): take the first one that has 2 free
$trip = $null; $free = @()
foreach ($t in $trips.result) {
    $carriages = Call GET "/trips/$($t.id)/carriages?seatClass=BERTH_4&from=HNO&to=DNA"
    $free = @($carriages.result | ForEach-Object { $_.seats } | Where-Object { $_.status -eq "available" } | Select-Object -First 2)
    if ($free.Count -eq 2) { $trip = $t; break }
}
Check ($null -ne $trip) "found trip $($trip.trainCode) (id=$($trip.id))"
Check ($free.Count -eq 2) "found 2 free berths: $($free[0].id), $($free[1].id)"

Show "3. Hold without signing in -> 401, without a queue token -> 403"
$holdBody = @{ tripId = [long]$trip.id; seatClass = "BERTH_4"; seatIds = @($free[0].id); from = "HNO"; to = "DNA" }
$noAuth = Call POST "/holds" $holdBody @{ "X-Queue-Token" = $queue.result.token }
Check ($noAuth.code -eq 401) "no token rejected with code $($noAuth.code)"
$noPass = Call POST "/holds" $holdBody $authA
Check ($noPass.code -eq 403) "no queue token rejected with code $($noPass.code)"

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

Show "6. Another account cannot see the hold; order before passengers -> 422"
$holdId = $h1.result.holdId
$peek = Call GET "/holds/$holdId" $null $userB.auth
Check ($peek.code -eq 404) "B looking at A's hold gets $($peek.code)"
$early = Call POST "/orders" @{ holdId = $holdId; paymentMethod = "VNPAY" } $pass
Check ($early.code -eq 422) "rejected with code $($early.code)"

Show "7. Save the passenger (student discount)"
$saved = Call PUT "/holds/$holdId/passengers" @{ passengers = @(@{
    seatId = $free[0].id; fullName = "Jane Doe"; idNumber = "001203004567"; phone = "0912345678"; discount = "STUDENT" }) } $authA
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
$mine = Call GET "/me/tickets" $null $authA
$ticket = $mine.result | Where-Object { $_.orderCode -eq $o1.result.code } | Select-Object -First 1
Check ($null -ne $ticket) "ticket $($ticket.code) listed"
Check ($ticket.passengerId -like "001*4567" -and $ticket.passengerId -notlike "*203004*") "ID number is masked ($($ticket.passengerId))"
Check ($ticket.status -eq "VALID") "status VALID"
$minesB = Call GET "/me/tickets" $null $userB.auth
Check (@($minesB.result).Count -eq 0) "B sees none of A's tickets ($(@($minesB.result).Count))"

Show "10. Refund"
$stolen = Call POST "/tickets/$($ticket.id)/refund" $null $userB.auth
Check ($stolen.code -eq 404) "B refunding A's ticket gets $($stolen.code)"
$refund = Call POST "/tickets/$($ticket.id)/refund" $null $authA
Check ($refund.success -and $refund.result.status -eq "REFUNDED") "refunded, $($refund.result.refundAmount) back after the 20% fee"
$again = Call POST "/tickets/$($ticket.id)/refund" $null $authA
Check ($again.code -eq 422) "second refund rejected with code $($again.code)"

Write-Host ""
if ($failures -eq 0) { Write-Host "ALL CHECKS PASSED" -ForegroundColor Green }
else { Write-Host "$failures CHECK(S) FAILED" -ForegroundColor Red; exit 1 }
