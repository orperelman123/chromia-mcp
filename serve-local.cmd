@echo off
rem Double-click (or run) to start the Chromia MCP server locally over SSE.
rem All options are handled by serve-local.ps1 (run: powershell -File serve-local.ps1 -?)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0serve-local.ps1" %*
