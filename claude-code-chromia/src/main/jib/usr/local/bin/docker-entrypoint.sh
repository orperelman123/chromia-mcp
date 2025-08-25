#!/usr/bin/env bash
set -Eeuo pipefail

bash postgres-entrypoint.sh postgres >/dev/null 2>/dev/null &

node /usr/local/lib/node_modules/@anthropic-ai/claude-code/cli.js "$@"
