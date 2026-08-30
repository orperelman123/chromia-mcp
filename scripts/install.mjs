#!/usr/bin/env node
// One-command install: download the latest chromia-mcp-server.jar release and
// register it as the `chromia` MCP server in Claude Code (user scope).
//
//   node scripts/install.mjs            # download + register
//   node scripts/install.mjs --print    # only print the registration command
//
// Needs: gh CLI authenticated with access to the repo, java 21+, claude CLI.

import { execSync } from 'node:child_process';
import { mkdirSync, existsSync } from 'node:fs';
import { homedir } from 'node:os';
import { join } from 'node:path';

const REPO = 'orperelman123/chromia-mcp';
const DIR = join(homedir(), '.chromia-mcp');
const JAR = join(DIR, 'chromia-mcp-server.jar');

const sh = (cmd) => execSync(cmd, { stdio: 'inherit' });
const printOnly = process.argv.includes('--print');

if (!printOnly) {
  mkdirSync(DIR, { recursive: true });
  console.log(`Downloading latest chromia-mcp-server.jar from ${REPO} ...`);
  sh(`gh release download --repo ${REPO} --pattern chromia-mcp-server.jar --dir "${DIR}" --clobber`);
  if (!existsSync(JAR)) {
    console.error('Download failed: jar not found. Is a release published?');
    process.exit(1);
  }
}

const register = `claude mcp add chromia --scope user --env CHROMIA_MCP_COMPACT_TOOLS=true -- java -jar "${JAR}" --stdio`;
if (printOnly) {
  console.log(register);
} else {
  console.log('Registering with Claude Code (user scope, compact tools)...');
  try { sh('claude mcp remove chromia -s user'); } catch { /* not registered yet */ }
  sh(register);
  console.log('Done. The chromia MCP server is available in all your Claude Code projects.');
}
