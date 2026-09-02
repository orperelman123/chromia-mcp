<#
.SYNOPSIS
  Run the Chromia MCP server locally as an SSE server on localhost.

.DESCRIPTION
  One command to get a URL-addressable MCP endpoint on this machine, with the
  FULL toolset (no compact mode, no disabled tools). This is the primary way
  to run the server for clients that connect by URL (ChatGPT-style connectors,
  browser clients, other tools on this machine). Claude Code already talks to
  the same jar over stdio - see "Run it locally" in README.md.

  What it does:
    - finds the jar (app/build/libs, then ~/.chromia-mcp, or -Jar / CHROMIA_MCP_JAR)
    - picks a free port automatically (first free port from 3001; -Port pins one)
    - passes CHROMIA_TEST_DATABASE_URL from your environment, or falls back to
      the standard local Chromia dev database (postchain/postchain on
      localhost:5432 - the public well-known dev credentials, not a secret).
      DB-backed tools (run_rell_tests with entities, local_chain_up) need it;
      everything else works without a database.
    - forces the FULL tool catalog: CHROMIA_MCP_COMPACT_TOOLS=false and no
      CHROMIA_MCP_DISABLE_TOOLS (the hosted 512MB box had to disable tools;
      locally there is no reason to).
    - gives the JVM a fixed 2 GB heap (-Xmx2g). The hosted image used
      -XX:MaxRAMPercentage=70 because a container must never outgrow its cgroup
      limit; locally there is no such limit. Measured steady state with RAG +
      the Rell compiler is ~1.5 GB, so 2 GB covers the full toolset with
      headroom and is a rounding error on a 32 GB machine. Override with -Heap.
    - waits for /health, prints the URL, and shuts the JVM down cleanly on
      Ctrl+C (the port is verified free again on exit).

  No secrets are hardcoded: the DB fallback is the documented local dev
  default and every value is overridable via environment or parameters.

.PARAMETER Port
  Pin a specific port. Default 0 = auto-pick the first free port from 3001.

.PARAMETER BindHost
  Interface to bind. Default 127.0.0.1 (this machine only). Use 0.0.0.0 to
  serve the LAN - then ALSO set CHROMIA_MCP_AUTH_TOKEN, or anyone on the
  network can use the server.

.PARAMETER Jar
  Path to chromia-mcp-server.jar. Default: CHROMIA_MCP_JAR env var, else
  app/build/libs/chromia-mcp-server.jar next to this script, else
  ~/.chromia-mcp/chromia-mcp-server.jar.

.PARAMETER Heap
  JVM max heap, passed to -Xmx. Default 2g.

.PARAMETER NoDb
  Do not apply the local dev database fallback. DB-backed tools then refuse
  with a clean "No PostgreSQL configured" message instead of failing.

.EXAMPLE
  .\serve-local.ps1
  .\serve-local.ps1 -Port 3005
  .\serve-local.ps1 -BindHost 0.0.0.0        # LAN - set CHROMIA_MCP_AUTH_TOKEN first
#>
[CmdletBinding()]
param(
    [int]$Port = 0,
    [string]$BindHost = '127.0.0.1',
    [string]$Jar = '',
    [string]$Heap = '2g',
    [switch]$NoDb
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

function Fail([string]$msg) { Write-Host "[serve-local] ERROR: $msg" -ForegroundColor Red; exit 1 }
function Info([string]$msg) { Write-Host "[serve-local] $msg" }

# --- Java ---------------------------------------------------------------
$java = Get-Command java -ErrorAction SilentlyContinue
if (-not $java) { Fail 'Java 21+ is required but `java` was not found on PATH.' }

# --- Jar ----------------------------------------------------------------
if (-not $Jar) { $Jar = $env:CHROMIA_MCP_JAR }
if (-not $Jar) {
    $candidates = @(
        (Join-Path $repoRoot 'app\build\libs\chromia-mcp-server.jar'),
        (Join-Path $HOME '.chromia-mcp\chromia-mcp-server.jar')
    )
    $Jar = $candidates | Where-Object { Test-Path $_ } | Select-Object -First 1
}
if (-not $Jar -or -not (Test-Path $Jar)) {
    Fail "chromia-mcp-server.jar not found. Build it (.\gradlew.bat :app:shadowJar) or pass -Jar / set CHROMIA_MCP_JAR."
}

# --- Port: pin or auto-pick the first free one from 3001 ----------------
function Test-PortFree([int]$p) {
    try {
        $l = New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Loopback, $p)
        $l.Start(); $l.Stop(); return $true
    } catch { return $false }
}
if ($Port -gt 0) {
    if (-not (Test-PortFree $Port)) { Fail "Port $Port is already in use. Pick another with -Port, or omit -Port to auto-pick." }
} else {
    $Port = 3001..3050 | Where-Object { Test-PortFree $_ } | Select-Object -First 1
    if (-not $Port) { Fail 'No free port found in 3001-3050. Pass -Port <n> explicitly.' }
}

# --- Environment for the child JVM --------------------------------------
# Full toolset, always: no compact gateway, no disabled tools.
$env:CHROMIA_MCP_COMPACT_TOOLS = 'false'
if ($env:CHROMIA_MCP_DISABLE_TOOLS) {
    Info "Clearing CHROMIA_MCP_DISABLE_TOOLS ('$env:CHROMIA_MCP_DISABLE_TOOLS') - local runs get the full toolset."
    Remove-Item Env:CHROMIA_MCP_DISABLE_TOOLS
}

# Database for run_rell_tests (entity tests) and local_chain_up.
$dbNote = ''
if (-not $env:CHROMIA_TEST_DATABASE_URL) {
    if ($NoDb) {
        $dbNote = 'no database (DB-backed tools refuse cleanly: run_rell_tests with entities, local_chain_up)'
    } else {
        # Standard local Chromia dev database - the same well-known postchain/postchain
        # dev credentials the Chromia docs use everywhere. Not a secret; override by
        # setting CHROMIA_TEST_DATABASE_URL yourself.
        $env:CHROMIA_TEST_DATABASE_URL = 'jdbc:postgresql://localhost:5432/rell_mcp_tests?user=postchain&password=postchain'
        $dbNote = "default local dev database ($($env:CHROMIA_TEST_DATABASE_URL))"
    }
} else {
    $dbNote = 'CHROMIA_TEST_DATABASE_URL from your environment'
}

if ($BindHost -ne '127.0.0.1' -and $BindHost -ne 'localhost' -and -not $env:CHROMIA_MCP_AUTH_TOKEN) {
    Write-Host "[serve-local] WARNING: binding $BindHost without CHROMIA_MCP_AUTH_TOKEN - anyone on the network can use this server." -ForegroundColor Yellow
}

# --- Launch -------------------------------------------------------------
$javaArgs = @("-Xmx$Heap", '-jar', $Jar, '--sse', '--host', $BindHost, '--port', $Port)
Info "jar:      $Jar"
Info "heap:     -Xmx$Heap (fixed heap - no container limit locally)"
Info "database: $dbNote"
Info "starting: java $($javaArgs -join ' ')"

# WorkingDirectory = repo root so a locally generated app/build/embeddings.json
# is found; otherwise the RAG store downloads the published embeddings once at
# warmup (background - the server is usable immediately).
$proc = Start-Process -FilePath $java.Source -ArgumentList $javaArgs -NoNewWindow -PassThru -WorkingDirectory $repoRoot

try {
    # --- Ready check ----------------------------------------------------
    $probeHost = if ($BindHost -eq '0.0.0.0') { '127.0.0.1' } else { $BindHost }
    $healthUrl = "http://${probeHost}:${Port}/health"
    $ready = $false
    foreach ($i in 1..60) {
        if ($proc.HasExited) { Fail "server exited during startup (exit code $($proc.ExitCode)). Check the log output above." }
        try {
            $h = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 2
            if ($h.status -eq 'healthy') { $ready = $true; break }
        } catch { Start-Sleep -Milliseconds 500 }
    }
    if (-not $ready) { Fail "server did not answer $healthUrl within 30s." }

    $h = Invoke-RestMethod -Uri $healthUrl
    Write-Host ''
    Write-Host "  Chromia MCP server is UP (v$($h.version), pid $($proc.Id))" -ForegroundColor Green
    Write-Host "    MCP SSE endpoint : http://${probeHost}:${Port}/"
    Write-Host "    Health check     : $healthUrl"
    Write-Host ''
    Write-Host '  Connect an MCP client with:  { "url": "http://' -NoNewline
    Write-Host "${probeHost}:${Port}/`" }"
    Write-Host '  Press Ctrl+C to stop.'
    Write-Host ''

    Wait-Process -Id $proc.Id
    $exitCode = $proc.ExitCode
    if ($null -ne $exitCode -and $exitCode -ne 0) {
        Info "server exited with code $exitCode"
        exit $exitCode
    }
} finally {
    # Runs on Ctrl+C too: make sure the JVM is gone and the port is free.
    if (-not $proc.HasExited) {
        Info 'stopping server...'
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
        try { Wait-Process -Id $proc.Id -Timeout 10 -ErrorAction SilentlyContinue } catch {}
    }
    if (Test-PortFree $Port) { Info "stopped - port $Port is free again." }
    else { Write-Host "[serve-local] WARNING: port $Port still busy after shutdown." -ForegroundColor Yellow }
}
