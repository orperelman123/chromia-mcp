# Wait for the shared build slot, then run the full loop gate for this lane.
$dir = "C:\Users\Orpe7\chromia-mcp-wt-fix_round16_tool"
$log = "$dir\lane-r16-tool-gate.log"

while ($true) {
    $busy = @(Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object {
        $_.CommandLine -match 'gradle-wrapper\.jar|GradleWorkerMain|Gradle Test Executor'
    }).Count
    $free = [int]((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory / 1024)
    if ($busy -eq 0 -and $free -ge 2500) { break }
    Start-Sleep -Seconds 20
}

Set-Location $dir
& node scripts/loop-gate.mjs --dir "C:/Users/Orpe7/chromia-mcp-wt-fix_round16_tool" --expect-min 1497 *> $log
"GATE_EXIT=$LASTEXITCODE" | Out-File -Append -Encoding utf8 $log
