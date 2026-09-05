#!/usr/bin/env node
// npx chromia-mcp -> downloads (once) and runs the Chromia MCP server over stdio.
// Requires Java 21+. Set CHROMIA_MCP_JAR to use a local jar instead.
// The jar is fetched from the GitHub release matching this package version
// into CHROMIA_MCP_HOME (default ~/.chromia-mcp), once.

import { spawnSync, spawn } from 'node:child_process';
import { createWriteStream, existsSync, mkdirSync, renameSync } from 'node:fs';
import { get } from 'node:https';
import { homedir } from 'node:os';
import { join } from 'node:path';
import { createRequire } from 'node:module';

const VERSION = createRequire(import.meta.url)('../package.json').version;
const RELEASE_URL = `https://github.com/orperelman123/chromia-mcp/releases/download/v${VERSION}/chromia-mcp-server.jar`;
const DIR = process.env.CHROMIA_MCP_HOME || join(homedir(), '.chromia-mcp');
const JAR = process.env.CHROMIA_MCP_JAR || join(DIR, `chromia-mcp-server-${VERSION}.jar`);

function fail(msg) { console.error(`[chromia-mcp] ${msg}`); process.exit(1); }

const javaCheck = spawnSync('java', ['-version'], { encoding: 'utf8' });
if (javaCheck.error) fail('Java 21+ is required but `java` was not found on PATH.');
// `java -version` prints to stderr: `openjdk version "21.0.4" ...` (or "1.8.0_x" for 8).
const javaMajor = Number((javaCheck.stderr || '').match(/version "(?:1\.)?(\d+)/)?.[1] ?? 0);
if (javaMajor && javaMajor < 21) fail(`Java 21+ is required; the \`java\` on PATH is ${javaMajor}. Install a JDK 21+ (e.g. Temurin 21) or put it first on PATH.`);

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
  console.error(`[chromia-mcp] downloading server v${VERSION} (~280MB, one-time) ...`);
  try {
    await download(RELEASE_URL, JAR);
  } catch (e) {
    fail(`download failed: ${e.message}. If the repo/release is private, download manually and set CHROMIA_MCP_JAR.`);
  }
}

// AppCDS: the first run writes a class-data archive next to the jar at exit and
// every later start maps it instead of parsing the 280 MB jar's classes -
// spawn -> initialize 1.6 s -> 0.7 s measured (2026-09-05). Per jar version, so
// an update never meets a stale archive; the JVM ignores an unusable one.
const JSA = JAR.replace(/\.jar$/, '') + '.jsa';
const args = process.argv.slice(2);
const child = spawn('java', ['-XX:+AutoCreateSharedArchive', `-XX:SharedArchiveFile=${JSA}`, '-jar', JAR, ...(args.length ? args : ['--stdio'])], { stdio: 'inherit' });
child.on('exit', (code) => process.exit(code ?? 0));
