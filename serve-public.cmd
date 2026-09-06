@echo off
rem Double-click (or run) to publish the Chromia MCP server on the `public` profile
rem through a Cloudflare quick tunnel, with the exact ChatGPT connector steps.
rem All options are handled by serve-public.ps1 (run: powershell -File serve-public.ps1 -?)
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0serve-public.ps1" %*
