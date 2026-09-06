<#
.SYNOPSIS
  Run the Chromia MCP server on the `public` profile and expose it through a
  Cloudflare quick tunnel, ready to paste into ChatGPT as a custom connector.

.DESCRIPTION
  serve-local.ps1 gives you a URL on THIS machine with the full toolset.
  This script is the other case: a URL on the public internet, which means
  anyone who has it can call every advertised tool. So it changes three things
  and says so on screen:

    1. CHROMIA_MCP_PROFILE=public. Every tool that acts on this machine or uses
       a key is refused: local_chain_up, provision_testnet_container,
       claim_testnet_tchr, deploy_testnet_chain. The compiler loop, the docs
       tools (including ChatGPT's search/fetch), the explorer queries and the
       prompt catalog all stay - that is the product. `/health` and the MCP
       serverInfo report the profile, so you can verify what you published.
    2. No database. run_rell_tests with entities and any DB-backed path then
       refuse cleanly instead of running against your local PostgreSQL.
       -WithDb opts back in, deliberately.
    3. The server binds 127.0.0.1 only. The tunnel is the only way in, and it
       is a process you can see and stop.

  It prints BOTH endpoints on the tunnel host:
    https://<name>.trycloudflare.com/mcp   Streamable HTTP (current transport)
    https://<name>.trycloudflare.com/      SSE (what ChatGPT's own docs show)
  ...and the exact ChatGPT connector steps for each.

  Ctrl+C stops the tunnel and the server, in that order, and verifies the port
  is free again.

  A quick tunnel needs no Cloudflare account, accepts no terms on your behalf,
  and costs nothing. It also hands a random public URL to whoever you give it
  to: treat it as a link that anyone with it can use, and set
  CHROMIA_MCP_AUTH_TOKEN if you want a bearer gate (ChatGPT's custom connector
  can send one; see README).

.PARAMETER Port
  Pin a local port. Default 0 = auto-pick the first free port from 3001.

.PARAMETER Jar
  Path to chromia-mcp-server.jar. Default: CHROMIA_MCP_JAR, else
  app/build/libs/chromia-mcp-server.jar next to this script, else
  ~/.chromia-mcp/chromia-mcp-server.jar.

.PARAMETER Heap
  JVM max heap, passed to -Xmx. Default 2g.

.PARAMETER Cloudflared
  Path to cloudflared.exe. Default: PATH, else
  "C:\Program Files (x86)\cloudflared\cloudflared.exe".

.PARAMETER WithDb
  Keep/apply the local dev database for DB-backed tools. Off by default: a
  public URL should not reach your PostgreSQL.

.PARAMETER Profile
  Tool profile. Default `public`. `full` publishes EVERY tool, including the
  ones that run on this machine and sign with your keys - the script refuses
  unless you also pass -IUnderstandFullProfileIsDangerous.

.PARAMETER NoTunnel
  Start the server and print what WOULD be published, then stop. Nothing is
  exposed. This is what the test suite runs.

.EXAMPLE
  .\serve-public.ps1
  .\serve-public.ps1 -Port 3005
  .\serve-public.ps1 -NoTunnel          # dry run: no public endpoint
#>
[CmdletBinding()]
param(
    [int]$Port = 0,
    [string]$Jar = '',
    [string]$Heap = '2g',
    [string]$Cloudflared = '',
    [ValidateSet('public', 'full')]
    [string]$Profile = 'public',
    [switch]$WithDb,
    [switch]$IUnderstandFullProfileIsDangerous,
    [switch]$NoTunnel
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path

function Fail([string]$msg) { Write-Host "[serve-public] ERROR: $msg" -ForegroundColor Red; exit 1 }
function Info([string]$msg) { Write-Host "[serve-public] $msg" }
function Warn([string]$msg) { Write-Host "[serve-public] WARNING: $msg" -ForegroundColor Yellow }

if ($Profile -eq 'full' -and -not $IUnderstandFullProfileIsDangerous) {
    Fail @'
-Profile full publishes every tool to a public URL, including local_chain_up
(starts a chain on this machine), deploy_testnet_chain (reads your deploy
keystore and runs `chr`), provision_testnet_container and claim_testnet_tchr
(sign and spend with your funding key). If that is really what you want, pass
-IUnderstandFullProfileIsDangerous as well - and set CHROMIA_MCP_AUTH_TOKEN.
'@
}

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

# --- cloudflared --------------------------------------------------------
# Resolved even for -NoTunnel, so a dry run tells you whether the real run
# would work rather than failing later.
if (-not $Cloudflared) {
    $onPath = Get-Command cloudflared -ErrorAction SilentlyContinue
    if ($onPath) { $Cloudflared = $onPath.Source }
    else { $Cloudflared = 'C:\Program Files (x86)\cloudflared\cloudflared.exe' }
}
$haveCloudflared = Test-Path $Cloudflared
if (-not $haveCloudflared -and -not $NoTunnel) {
    Fail "cloudflared not found at '$Cloudflared'. Install it, put it on PATH, or pass -Cloudflared <path>."
}

# --- Port ---------------------------------------------------------------
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
$env:CHROMIA_MCP_PROFILE = $Profile
$env:CHROMIA_MCP_COMPACT_TOOLS = 'false'
if ($env:CHROMIA_MCP_DISABLE_TOOLS) {
    Info "Keeping CHROMIA_MCP_DISABLE_TOOLS ('$env:CHROMIA_MCP_DISABLE_TOOLS') on top of the $Profile profile."
}

# A quick tunnel forwards the client's Host, so the server sees
# <name>.trycloudflare.com. The default allow-list already covers
# *.trycloudflare.com; a custom domain needs CHROMIA_MCP_ALLOWED_HOSTS.
if ($env:CHROMIA_MCP_ALLOWED_HOSTS) {
    Info "CHROMIA_MCP_ALLOWED_HOSTS='$env:CHROMIA_MCP_ALLOWED_HOSTS' (added to localhost + *.trycloudflare.com)"
}

$dbNote = ''
if ($WithDb) {
    if (-not $env:CHROMIA_TEST_DATABASE_URL) {
        $env:CHROMIA_TEST_DATABASE_URL = 'jdbc:postgresql://localhost:5432/rell_mcp_tests?user=postchain&password=postchain'
    }
    $dbNote = "ENABLED by -WithDb ($($env:CHROMIA_TEST_DATABASE_URL)) - a public caller can run tests against it"
    Warn 'a public URL now reaches your local PostgreSQL (-WithDb)'
} else {
    if ($env:CHROMIA_TEST_DATABASE_URL) {
        Info 'Clearing CHROMIA_TEST_DATABASE_URL - a public endpoint should not reach your database (-WithDb keeps it).'
        Remove-Item Env:CHROMIA_TEST_DATABASE_URL
    }
    $dbNote = 'none (DB-backed paths refuse cleanly; -WithDb opts in)'
}

if (-not $env:CHROMIA_MCP_AUTH_TOKEN) {
    Warn 'CHROMIA_MCP_AUTH_TOKEN is not set - anyone with the tunnel URL can call the advertised tools.'
} else {
    Info 'CHROMIA_MCP_AUTH_TOKEN is set - clients must send Authorization: Bearer <token> (/health stays open).'
}

# --- Launch the server (loopback only) ----------------------------------
$javaArgs = @("-Xmx$Heap", '-jar', $Jar, '--sse', '--host', '127.0.0.1', '--port', $Port, '--profile', $Profile)
Info "jar:      $Jar"
Info "profile:  $Profile"
Info "database: $dbNote"
Info "starting: java $($javaArgs -join ' ')"

$proc = Start-Process -FilePath $java.Source -ArgumentList $javaArgs -NoNewWindow -PassThru -WorkingDirectory $repoRoot
$tunnel = $null

try {
    $healthUrl = "http://127.0.0.1:${Port}/health"
    $health = $null
    foreach ($i in 1..60) {
        if ($proc.HasExited) { Fail "server exited during startup (exit code $($proc.ExitCode)). Check the log output above." }
        try {
            $h = Invoke-RestMethod -Uri $healthUrl -TimeoutSec 2
            if ($h.status -eq 'healthy') { $health = $h; break }
        } catch { Start-Sleep -Milliseconds 500 }
    }
    if (-not $health) { Fail "server did not answer $healthUrl within 30s." }
    if ($health.profile -ne $Profile) {
        Fail "server reports profile '$($health.profile)' but '$Profile' was requested - refusing to publish it."
    }

    Write-Host ''
    Write-Host "  Chromia MCP server is UP (v$($health.version), profile $($health.profile), pid $($proc.Id))" -ForegroundColor Green
    Write-Host "    local Streamable HTTP : http://127.0.0.1:${Port}/mcp"
    Write-Host "    local SSE             : http://127.0.0.1:${Port}/"
    Write-Host "    health                : $healthUrl"
    Write-Host ''

    if ($NoTunnel) {
        Write-Host '  -NoTunnel: nothing was published.' -ForegroundColor Yellow
        Write-Host "  A real run would start: $Cloudflared tunnel --url http://127.0.0.1:$Port"
        if (-not $haveCloudflared) { Write-Host "  (cloudflared was NOT found at '$Cloudflared' - install it before a real run.)" -ForegroundColor Yellow }
        Write-Host '  Stopping the server.'
        exit 0
    }

    # --- Cloudflare quick tunnel ----------------------------------------
    # No account, no login, no terms accepted here: `tunnel --url` mints an
    # anonymous https://<random>.trycloudflare.com that lives as long as this
    # process. The URL is printed on cloudflared's stderr.
    $tunnelOut = Join-Path ([System.IO.Path]::GetTempPath()) "chromia-mcp-tunnel-$PID.log"
    Info "starting: $Cloudflared tunnel --url http://127.0.0.1:$Port"
    $tunnel = Start-Process -FilePath $Cloudflared `
        -ArgumentList @('tunnel', '--url', "http://127.0.0.1:$Port") `
        -NoNewWindow -PassThru -RedirectStandardError $tunnelOut -RedirectStandardOutput "$tunnelOut.out"

    $publicUrl = $null
    foreach ($i in 1..60) {
        if ($tunnel.HasExited) { Fail "cloudflared exited (code $($tunnel.ExitCode)). See $tunnelOut" }
        if (Test-Path $tunnelOut) {
            $m = Select-String -Path $tunnelOut -Pattern 'https://[a-z0-9-]+\.trycloudflare\.com' -AllMatches |
                 Select-Object -First 1
            if ($m) { $publicUrl = $m.Matches[0].Value; break }
        }
        Start-Sleep -Milliseconds 1000
    }
    if (-not $publicUrl) { Fail "cloudflared did not print a public URL within 60s. See $tunnelOut" }

    Write-Host ''
    Write-Host "  PUBLIC URL: $publicUrl" -ForegroundColor Green
    Write-Host "    Streamable HTTP : $publicUrl/mcp"
    Write-Host "    SSE             : $publicUrl/"
    Write-Host "    health          : $publicUrl/health"
    Write-Host ''
    Write-Host '  Connect it in ChatGPT (Settings -> Connectors -> Create / Advanced -> Developer mode):' -ForegroundColor Cyan
    Write-Host "    Name             : Chromia MCP"
    Write-Host "    MCP server URL   : $publicUrl/mcp     (Streamable HTTP - prefer this)"
    Write-Host "                       $publicUrl/sse     (only if the connector insists on SSE)"
    Write-Host "    Authentication   : $(if ($env:CHROMIA_MCP_AUTH_TOKEN) { 'API key / Bearer -> your CHROMIA_MCP_AUTH_TOKEN' } else { 'No authentication (anyone with the URL can call it)' })"
    Write-Host "    Then 'Create'. ChatGPT calls search/fetch for research and the other tools in developer mode."
    Write-Host ''
    Write-Host "  Anything not on the $Profile profile is refused with an explanation, not hidden." -ForegroundColor DarkGray
    Write-Host '  Press Ctrl+C to take the tunnel and the server down.'
    Write-Host ''

    while (-not $proc.HasExited -and -not $tunnel.HasExited) { Start-Sleep -Seconds 1 }
    if ($tunnel.HasExited) { Info "cloudflared exited with code $($tunnel.ExitCode) - see $tunnelOut" }
} finally {
    # Runs on Ctrl+C too. Tunnel first: close the public door before the room.
    if ($tunnel -and -not $tunnel.HasExited) {
        Info 'stopping tunnel...'
        Stop-Process -Id $tunnel.Id -Force -ErrorAction SilentlyContinue
        try { Wait-Process -Id $tunnel.Id -Timeout 10 -ErrorAction SilentlyContinue } catch {}
    }
    if (-not $proc.HasExited) {
        Info 'stopping server...'
        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
        try { Wait-Process -Id $proc.Id -Timeout 10 -ErrorAction SilentlyContinue } catch {}
    }
    if (Test-PortFree $Port) { Info "stopped - port $Port is free again." }
    else { Warn "port $Port still busy after shutdown." }
}
