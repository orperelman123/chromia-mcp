#!/usr/bin/env node
// npx chromia-mcp -> downloads (once) and runs the Chromia MCP server over stdio.
// Requires Java 21+. Set CHROMIA_MCP_JAR to use a local jar instead.
// The jar is fetched from the GitHub release matching this package version.

import { spawnSync, spawn } from 'node:child_process';
import { createWriteStream, existsSync, mkdirSync, renameSync } from 'node:fs';
import { get } from 'node:https';
import { homedir } from 'node:os';
import { join } from 'node:path';
import { createRequire } from 'node:module';

const VERSION = createRequire(import.meta.url)('../package.json').version;
const RELEASE_URL = `https://github.com/orperelman123/chromia-mcp/releases/download/v${VERSION}/chromia-mcp-server.jar`;
const DIR = join(homedir(), '.chromia-mcp');
const JAR = process.env.CHROMIA_MCP_JAR || join(DIR, `chromia-mcp-server-${VERSION}.jar`);

function fail(msg) { console.error(`[chromia-mcp] ${msg}`); process.exit(1); }

const javaCheck = spawnSync('java', ['-version'], { encoding: 'utf8' });
if (javaCheck.error) fail('Java 21+ is required but `java` was not found on PATH.');

function download(url, dest, redirects = 0) {
  return new Promise((resolve, reject) => {
    if (redirects > 5) return reject(new Error('too many redirects'));
    get(url, (res) => {
      if (res.statusCode >= 300 && res.statusCode < 400 && res.headers.location) {
        res.resume();
        return resolve(download(res.headers.location, dest, redirects + 1));
      }
      if (res.statusCode !== 200) return reject(new Error(`HTTP ${res.statusCode} for ${url}`));
      const tmp = `${dest}.download`;
      const out = createWriteStream(tmp);
      res.pipe(out);
      out.on('finish', () => { out.close(() => { renameSync(tmp, dest); resolve(); }); });
      out.on('error', reject);
    }).on('error', reject);
  });
}

if (!existsSync(JAR)) {
  mkdirSync(DIR, { recursive: true });
  console.error(`[chromia-mcp] downloading server v${VERSION} (~230MB, one-time) ...`);
  try {
    await download(RELEASE_URL, JAR);
  } catch (e) {
    fail(`download failed: ${e.message}. If the repo/release is private, download manually and set CHROMIA_MCP_JAR.`);
  }
}

const args = process.argv.slice(2);
const child = spawn('java', ['-jar', JAR, ...(args.length ? args : ['--stdio'])], { stdio: 'inherit' });
child.on('exit', (code) => process.exit(code ?? 0));
