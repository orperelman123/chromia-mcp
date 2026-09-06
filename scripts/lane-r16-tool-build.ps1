# Wait for the shared build slot, then run the round-16 verify_guards probes.
param([string]$Tasks = "probes")

$dir = "C:\Users\Orpe7\chromia-mcp-wt-fix_round16_tool"
$log = "$dir\lane-r16-tool-build.log"

while ($true) {
    $busy = @(Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object {
        $_.CommandLine -match 'gradle-wrapper\.jar|GradleWorkerMain|Gradle Test Executor'
    }).Count
    $free = [int]((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory / 1024)
    if ($busy -eq 0 -and $free -ge 2500) { break }
    Start-Sleep -Seconds 20
}

Set-Location $dir
if ($Tasks -eq "probes") {
    & .\gradlew.bat --no-daemon test --rerun-tasks `
        --tests "org.chromia.VerifyGuardsProbeTest" `
        --tests "org.chromia.Round16VerifyGuardsProbeTest" `
        --tests "org.chromia.VerifyGuardsToolTest" *> $log
} else {
    & .\gradlew.bat --no-daemon build --rerun-tasks *> $log
}
"EXIT=$LASTEXITCODE" | Out-File -Append -Encoding utf8 $log
