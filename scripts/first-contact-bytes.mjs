#!/usr/bin/env node
// What an agent pays before its first useful call, measured ON THE WIRE.
//
//   node scripts/first-contact-bytes.mjs --jar app/build/libs/chromia-mcp-server.jar
//
// Spawns the jar over stdio twice - full catalog and CHROMIA_MCP_COMPACT_TOOLS=true -
// and prints the byte length of the tools/list, prompts/list and resources/list
// payloads, the same metric the agent-experience audit used (section 1).
//
// Audit baseline, measured on the base jar 2026-09-06:
//   full     tools 74  tools/list 115,244 B  prompts/list -32601  resources/list 590 B  total 115,908 B (~28,977 tok)
//   compact  tools 43  tools/list  89,648 B  prompts/list -32601  resources/list 590 B  total  90,312 B (~22,578 tok)
import { spawn } from 'node:child_process';
import { resolve } from 'node:path';

const args = Object.fromEntries(process.argv.slice(2).reduce((acc, a, i, arr) => {
  if (a.startsWith('--')) acc.push([a.slice(2), arr[i + 1] && !arr[i + 1].startsWith('--') ? arr[i + 1] : 'true']);
  return acc;
}, []));
const jar = resolve(args.jar ?? 'app/build/libs/chromia-mcp-server.jar');

const drive = (compact) => new Promise((done) => {
  const env = { ...process.env };
  if (compact) env.CHROMIA_MCP_COMPACT_TOOLS = 'true';
  else delete env.CHROMIA_MCP_COMPACT_TOOLS;
  const proc = spawn('java', ['-jar', jar, '--stdio'], { env, stdio: ['pipe', 'pipe', 'ignore'] });
  let buffer = '';
  const pending = new Map();
  proc.stdout.on('data', (d) => {
    buffer += d;
    let nl;
    while ((nl = buffer.indexOf('\n')) >= 0) {
      const line = buffer.slice(0, nl).trim();
      buffer = buffer.slice(nl + 1);
      if (!line) continue;
      let message;
      try { message = JSON.parse(line); } catch { continue; }
      const fn = pending.get(message.id);
      if (fn) { pending.delete(message.id); fn(message); }
    }
  });
  let nextId = 1;
  const send = (method, params) => new Promise((res) => {
    const id = nextId++;
    pending.set(id, res);
    proc.stdin.write(JSON.stringify({ jsonrpc: '2.0', id, method, params }) + '\n');
  });

  (async () => {
    await send('initialize', { protocolVersion: '2024-11-05', capabilities: {}, clientInfo: { name: 'm', version: '1' } });
    proc.stdin.write(JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized', params: {} }) + '\n');
    const tools = await send('tools/list', {});
    const prompts = await send('prompts/list', {});
    const resources = await send('resources/list', {});
    const size = (value) => (value === undefined ? 0 : Buffer.byteLength(JSON.stringify(value)));
    const out = {
      mode: compact ? 'compact' : 'full',
      tools: tools?.result?.tools?.length ?? 0,
      toolsListBytes: size(tools?.result?.tools),
      prompts: prompts?.result?.prompts?.length ?? 0,
      promptsListBytes: prompts?.error ? 0 : size(prompts?.result?.prompts),
      promptsError: prompts?.error?.code ?? null,
      resourcesListBytes: size(resources?.result),
    };
    out.totalBytes = out.toolsListBytes + out.promptsListBytes + out.resourcesListBytes;
    out.approxTokens = Math.round(out.totalBytes / 4);
    proc.kill();
    done(out);
  })();
});

const rows = [await drive(false), await drive(true)];
console.log('mode     tools prompts  tools/list  prompts/list  resources  total       ~tokens');
rows.forEach((r) => {
  console.log(
    `${r.mode.padEnd(8)} ${String(r.tools).padStart(5)} ${String(r.prompts).padStart(7)} ` +
    `${String(r.toolsListBytes).padStart(11)} ${String(r.promptsListBytes).padStart(13)} ` +
    `${String(r.resourcesListBytes).padStart(10)} ${String(r.totalBytes).padStart(11)} ${String(r.approxTokens).padStart(9)}` +
    (r.promptsError ? `  prompts/list error ${r.promptsError}` : '')
  );
});
console.log(JSON.stringify(rows));
