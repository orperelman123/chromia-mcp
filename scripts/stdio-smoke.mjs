// Stdio-transport smoke: the surface Claude Code and the npm launcher use.
//   node scripts/stdio-smoke.mjs <path-to-jar>
//   node scripts/stdio-smoke.mjs --launcher   (runs packages/npm/bin/chromia-mcp.mjs;
//                                              set CHROMIA_MCP_JAR to skip the download)
import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const arg = process.argv[2];
let cmd, cmdArgs;
if (arg === '--launcher') {
  cmd = process.execPath;
  cmdArgs = [join(here, '..', 'packages', 'npm', 'bin', 'chromia-mcp.mjs')];
} else {
  cmd = 'java';
  cmdArgs = ['-jar', arg || join(here, '..', 'app', 'build', 'libs', 'chromia-mcp-server.jar'), '--stdio'];
}
console.log('STDIO TARGET:', cmd, cmdArgs.join(' '));

const proc = spawn(cmd, cmdArgs, { cwd: join(here, '..') });
let buf = ''; const pending = new Map(); let nextId = 1;
proc.stdout.on('data', d => {
  buf += d.toString(); let i;
  while ((i = buf.indexOf('\n')) >= 0) {
    const line = buf.slice(0, i).trim(); buf = buf.slice(i + 1);
    if (!line) continue;
    try { const m = JSON.parse(line); if (m.id !== undefined && pending.has(m.id)) { pending.get(m.id)(m); pending.delete(m.id); } } catch {}
  }
});
proc.stderr.on('data', () => {});
const rpc = (method, params, t = 240000) => {
  const id = nextId++;
  proc.stdin.write(JSON.stringify({ jsonrpc: '2.0', id, method, params }) + '\n');
  return new Promise((res, rej) => { const h = setTimeout(() => rej(new Error('timeout ' + method)), t); pending.set(id, m => { clearTimeout(h); res(m); }); });
};
const text = m => m?.result?.content?.[0]?.text ?? JSON.stringify(m?.error ?? m?.result);
const results = [];
const check = (label, ok, detail) => { results.push([label, ok]); console.log(`${ok ? 'PASS' : 'FAIL'} ${label} ${detail ?? ''}`); };

const call = (name, args) => rpc('tools/call', { name, arguments: args });
const parse = m => { try { return JSON.parse(text(m)); } catch { return {}; } };

try {
  const init = await rpc('initialize', { protocolVersion: '2024-11-05', capabilities: {}, clientInfo: { name: 'stdio-smoke', version: '1' } });
  check('initialize', !!init.result?.serverInfo, JSON.stringify(init.result?.serverInfo));
  proc.stdin.write(JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized', params: {} }) + '\n');

  const tl = await rpc('tools/list', {});
  const names = (tl.result?.tools || []).map(t => t.name);
  check('tools/list', names.length >= 30, `${names.length} tools`);

  const rl = await rpc('resources/list', {});
  check('resources/list', (rl.result?.resources || []).length >= 3, `${(rl.result?.resources || []).length} resources`);

  // --- The agent loop over the Claude Code transport ---
  const ft4 = 'module;\nimport lib.ft4.auth;\nentity note { key id: text; body: text; }\n' +
    'operation add_note(id: text, body: text) {\n  val account = auth.authenticate();\n' +
    '  require(id.size() > 0, "empty id");\n  create note(id, body);\n}\n';
  let j = parse(await call('rell_check', { source: ft4 }));
  check('rell_check compiles FT4 code', j.ok === true, JSON.stringify(j.errors ?? '').slice(0, 120));

  j = parse(await call('rell_check', { source: 'module;\nquery broken() = nope;' }));
  check('rell_check reports line numbers', j.ok === false && j.errors?.[0]?.line === 2, JSON.stringify(j.errors).slice(0, 100));

  j = parse(await call('rell_security_check', { source: ft4 }));
  check('rell_security_check: FT4 code is clean', j.ok === true, JSON.stringify(j.findings ?? '').slice(0, 120));

  j = parse(await call('rell_security_check', { source: 'module;\nentity v { key k: text; }\noperation drain(k: text) { delete v @* { .k == k }; }' }));
  check('rell_security_check flags unauth mutation', j.ok === false && JSON.stringify(j.findings).includes('unauthenticated-mutation'), null);

  j = parse(await call('run_rell_tests', { files: { 't.rell': '@test module;\nfunction test_x() { assert_equals(2 + 2, 4); }' } }));
  check('run_rell_tests passes', j.ok === true, j.notes?.slice(0, 80));

  j = parse(await call('run_rell_tests', { files: { 't.rell': '@test module;\nfunction test_bad() { assert_equals(1, 2); }' } }));
  check('run_rell_tests reports failure', j.ok === false && j.failed === 1, JSON.stringify(j.cases ?? '').slice(0, 100));

  const scaffold = parse(await call('scaffold_dapp', { name: 'stdio_journey', template: 'ft4' }));
  check('scaffold_dapp ft4', scaffold.template === 'ft4' && !!scaffold.files?.['client/example.ts'], null);

  j = parse(await call('validate_chromia_yml', { yaml: scaffold.files['chromia.yml'] }));
  check('validate_chromia_yml on scaffold', j.ok === true, JSON.stringify(j.errors ?? '').slice(0, 100));

  j = parse(await call('write_deployment_config', { network: 'testnet', name: 'stdio_journey' }));
  check('write_deployment_config', (j.yaml ?? j.chromia_yml ?? '').includes('testnet'), null);

  let t = text(await call('chromia_help', {}));
  check('chromia_help index', t.includes('chr_deploy_help'), null);

  t = text(await call('chromia_help', { topic: 'chr_build' }));
  check('chromia_help topic', t.length > 100, null);

  t = text(await call('get_network_stats', {}));
  check('live analytics over stdio', t.includes('countAllAccounts'), t.slice(0, 60));

  t = text(await call('no_such_tool', {}));
  check('unknown tool errors cleanly', /unknown tool|not found/i.test(t), t.slice(0, 60));

  t = text(await call('get_blockchain_details', {}));
  check('missing param message', /rid|missing/i.test(t), t.slice(0, 60));
} catch (e) {
  check('stdio session', false, e.message);
} finally {
  const failed = results.filter(r => !r[1]);
  console.log(`\n=== STDIO SMOKE: ${results.length - failed.length}/${results.length} PASS ===`);
  proc.kill();
  process.exit(failed.length ? 1 : 0);
}
