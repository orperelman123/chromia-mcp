@echo off
rem Double-click (or run) to start the Chromia MCP server locally as a URL server
rem (Streamable HTTP at /mcp, legacy HTTP+SSE at the root; there is no /sse path).
rem All options are handled by serve-local.ps1 (run: powershell -File serve-local.ps1 -?)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0serve-local.ps1" %*
