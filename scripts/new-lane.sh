#!/usr/bin/env bash
# Provision a lane so it cannot collide with another: its own worktree, its own
# C.UTF-8 database, its own test-env file. Two suites on one schema fail with
# "Missing metadata entities for existing tables: c0.<the other lane's tables>",
# which reads as flaky and is not.
#
#   bash scripts/new-lane.sh <branch> [base]     # base defaults to origin/main
set -euo pipefail
branch="${1:?usage: new-lane.sh <branch> [base]}"
base="${2:-origin/main}"
slug="$(echo "$branch" | tr -c 'a-z0-9' '_' | tr -s '_' | sed 's/^_//;s/_$//')"
main="$(git rev-parse --show-toplevel)"
wt="$(dirname "$main")/chromia-mcp-wt-$slug"
db="chromia_mcp_test_$slug"

git -C "$main" fetch -q origin || true
[ -d "$wt" ] || git -C "$main" worktree add "$wt" -b "$branch" "$base" >/dev/null

# Fed to WSL on STDIN. This used to go through a temp FILE, and mktemp hands
# Git Bash a /tmp path that WSL's wslpath resolves to /mnt/c/tmp - which does
# not exist - so provisioning died after the worktree was already created and
# left a lane with no database. stdin needs no shared filesystem at all.
wsl.exe -d Ubuntu -u root -- bash -s <<INNER | tr -d ''
set -e
if ! sudo -u postgres psql -p 5433 -tAc "select 1 from pg_database where datname='$db'" | grep -q 1; then
  sudo -u postgres psql -p 5433 -c "CREATE DATABASE $db OWNER chromia TEMPLATE template0 ENCODING 'UTF8' LC_COLLATE 'C.utf8' LC_CTYPE 'C.utf8'" >/dev/null
fi
# Postchain's own collation gate - must print t, or DB-backed tests fail confusingly
echo "collation: \$(sudo -u postgres psql -p 5433 -d $db -tAc "SELECT 'A'<'a' and 'Ї'<'ї' and upper('ї')='Ї' and lower('Ї')='ї'")"
INNER

sed "s|/chromia_mcp_test[a-z_]*?|/$db?|" "$main/local-test-env.properties" > "$wt/local-test-env.properties"
echo "lane ready: $wt (branch $branch, db $db)"
grep -o '5433/[a-z_]*' "$wt/local-test-env.properties" | head -1
