# Audit: standalone Chromia / Rell / Postchain agents for Claude Code and OpenAI Codex

**Audit date:** 2026-08-26 (Asia/Jerusalem)
**Production baseline:** Rell 0.16.7 (2026-08-14), Chromia CLI 0.33.x (tag 0.33.2), FT4 v1.1.0r, docs.chromia.com. Official Chromaway sources first; nothing invented.

## Verdict
There is no official standalone Chromia/Rell/Postchain coding-agent product (no marketplace plugin, no Codex pack, no package). What exists is a small official tool stack plus one portable SKILL.md and some repo-local CLAUDE.md files.

Closest artifacts:
1. bitbucket.org/chromawallet/chromia-skill (portable SKILL.md)
2. gitlab.com/chromaway/core-tools/chromia-lsp-mcp (Rell LSP MCP)
3. gitlab.com/chromaway/core-tools/chromia-mcp (docs/explorer MCP)
4. claude-code-chromia Docker image (chr 0.27.7, stale)
5. Repo-local CLAUDE.md in directory-chain, postchain-eif, chromia-lsp-mcp

## Search coverage (negative results)

gitlab.com/chromaway: official MCP repos plus repo-local CLAUDE.md in postchain-eif and chromia-lsp-mcp. No AGENTS.md. No Codex skill tree.
github.com/chromaway: legacy colored-coins/wallet repos only. No coding-agent files.
github.com/ChromiaProject: mirrors. directory-chain has repo-local CLAUDE.md. chromia-lsp-mcp README documents Claude Code MCP setup. No standalone agent repo.
Anthropic claude-plugins-official marketplace.json (289 plugins, 2026-08-26): zero hits for Chromia, Rell, Postchain, or Chromaway.
PyPI: no chromia package (404).
Codex official catalog has no Chromia skill. Chromaway does not publish AGENTS.md.
docs.chromia.com has no dedicated Claude Code or Codex page. Official AI surface is MCP plus LSP MCP.

## 1. chromia-skill (portable SKILL.md)

URL: https://bitbucket.org/chromawallet/chromia-skill
Official?: Chromaway-authored, not on gitlab.com/chromaway. Commit 4b639bd 2026-04-10 by Tewuhbo Mihret (tewuhbo.mihret@chromaway.com). Same Bitbucket workspace used by official chromia-mcp docs ingest. Not a marketplace plugin.
Form: SKILL.md plus references/*.md. No tools, no MCP, no hooks, no scripts.
Skill id/version: chromia-skill / 0.1.0
Claimed hosts: README says Cursor, Claude Code, Codex. Install snippets only for Claude Code (.claude/skills/) and Cursor (.cursor/skills/). Codex is mentioned but no ~/.codex/skills or .agents/skills path. Frontmatter has metadata.openclaw requiring bin chr.
Size: 2436 lines (SKILL.md 497; 9 reference files).

Files include SKILL.md and nine reference markdown files.
References: ft4-integration, iccf-icmf, eif-evm, crc2-nft, postchain-client, filehub, ai-extensions, governance, deployment.
No plugin manifest and no Codex AGENTS.md in that repo.
Knowledge: Rell medium (project layout, entity/struct/@mount; not 0.15/0.16 language). chromia.yml medium-high. FT4 high (accounts, flags, strategies; pins v1.1.0r). CLI/deploy medium. Client unversioned. EIF/CRC2/Filehub/AI shallow with placeholder tags.

Versions cited vs 2026-08-26 production:
- Rell example pin 0.14.5 vs production 0.16.7 (stale by ~7 minors).
- FT4 v1.1.0r matches production.
- ICCF lib tag 1.87.0 is stale vs current directory-chain.
- CLI unnamed; commands look pre-0.30 vs production 0.33.x.
- TypeScript clients are unpinned.
- Skill last commit 2026-04-10 (four months stale).

Robustness gaps: no LSP loop; Codex install path missing; no live docs hook; conflicting PLACEHOLDER_PUBKEY rules; tx_time listed then forbidden; EIF versions are placeholders; not a marketplace plugin; no changelog or tests.

## 2. Official chromia-mcp (docs plus explorer MCP, not a coding agent)

URL: https://gitlab.com/chromaway/core-tools/chromia-mcp
Hosted: https://mcp.chromia.dev (SSE /sse)
Official yes. Cloned dev HEAD 14677776 2026-01-13 (Issame Zguiri). Kotlin MCP server. No SKILL.md, no AGENTS.md, no CLAUDE.md.
Tools: explorer/chain (filter_blockchains, get_blockchain_details, analytics, transactions, network_stats, node_unavailability), assets/accounts, chromia_dapp_query (read-only, default rell.get_app_structure), fetch_docs RAG, get_prompts. ChatGPT fetch/search are stubs. README TODO: no transaction execution.
RAG sources last_updated 2025-10-17: chromia-docs, cookbook, directory-chain doc, directory1-example, postchain-eif doc.
Gaps: last app commit 2026-01-13. Docs index dated 2025-10-17. No write/deploy tools.
README still documents the unmaintained LSP package.

## 3. Official claude-code-chromia Docker image

URL: https://gitlab.com/chromaway/core-tools/chromia-mcp/-/tree/dev/claude-code-chromia
Image: registry.gitlab.com/chromaway/core-tools/chromia-mcp/claude-code-chromia (about 9 months old).
Official. Author Mikael Staldal (ChromaWay).
Includes: Ubuntu 24.04 base, Java 21, Node 24.8.0, Postgres 16, global Claude Code CLI (unpinned at image build), Chromia CLI 0.27.7, PMC 3.51.1, chromia-mcp-server.jar. Wrapper scripts mount cwd and ~/.claude.
Does not include SKILL.md, CLAUDE.md, AGENTS.md, or chromia-lsp-mcp.
Version gaps: chr 0.27.7 vs 0.33.2; PMC 3.51.1 stale; bundled Rell pre-0.16 vs language 0.16.7; Claude Code snapshot about 9 months old. Runtime wrapper, not a knowledge agent. Related base image: chromaway/core-tools/chromia-images/claude-code Dockerfile only.

## 4. Official chromia-lsp-mcp (Rell language intelligence, not an agent)
Canonical: https://gitlab.com/chromaway/core-tools/chromia-lsp-mcp
Mirror: https://github.com/ChromiaProject/chromia-lsp-mcp
Image: registry.gitlab.com/chromaway/core-tools/chromia-lsp-mcp latest; cloned tag 0.1.1 dated 2026-08-14. Official and actively maintained.
README documents Claude Code, Cursor, VS Code Copilot. No Codex mcp-add block. CLAUDE.md is for editing this MCP repo, not writing Rell dapps.
Tools: start_lsp, restart, open/save/close document, diagnostics, hover, completions, code actions, definition, references, symbols, rename, format, log level. Prompt: lsp_guide. No FT4/deploy skill.
Pins rell-lsp 0.16.6 vs production Rell 0.16.7 (one patch behind, same day as tag 0.1.1). Container-only distribution. Best official Rell coding tool for Claude Code; not a standalone Chromia agent.

## 5. Repo-local CLAUDE.md files (not standalone)
directory-chain (ChromiaProject mirror / chromaway/core/directory-chain): repo map, chr test vs Maven it/, PMC coupling. Cited Rell 0.15.3 and API v97; live tree is newer (version.rell 110, CLI 0.33 ships Rell 0.16.0).
postchain-eif CLAUDE.md: deep project guide (Solidity 0.8.24, Kotlin EIF, Rell hbridge/CRC2). Cited Rell 0.14.5, Postchain 3.47.7; pom later moved toward 3.49.14. Stale vs build files.
chromia-lsp-mcp CLAUDE.md: how to develop the LSP MCP, not dapp knowledge.
Postchain core .gitignore mentions CLAUDE.local files (engineers use Claude Code internally). No published agent in the public tree.

## 6. Unofficial / adjacent (not coding agents)

github.com/chromindscan/chromia-mcp: Claude Desktop MCP to send CHR via wallet. Not Rell/FT4/CLI authoring.
Skywork listing is a mirror page of the Bitbucket skill.
The old Node LSP package is unmaintained.

## 7. Gaps vs production Chromia on 2026-08-26

1. No single packaged product with skills plus MCP plus version pins matching Rell 0.16.7 / CLI 0.33.x.
2. Skill Rell pin is 0.14.5; LSP is 0.16.6; Docker CLI is 0.27.7 — three different eras.
3. No official Codex artifact (AGENTS.md, .codex/skills, or documented Codex MCP add).
4. No Claude Code marketplace plugin.
5. chromia-mcp cannot submit transactions; docs index dated 2025-10-17.
6. No maintained FT4/EIF/ICCF version matrix except skill hardcodes FT4 v1.1.0r and ICCF 1.87.0.
7. docs.chromia.com CLI notes lag GitLab (0.30.0 vs 0.33.x). RAG-only agents under-teach current CLI.
8. No hooks, evals, or golden Rell tests attached to the skill. Quality is prompt-only.

## 8. Closest usable stack today

1. Use chromia-lsp-mcp per project (README Claude Code snippet; image tag 0.1.1 or later).
2. Use https://mcp.chromia.dev/sse for explorer and fetch_docs. Answers may predate Rell 0.16 / CLI 0.33.
3. Copy chromia-skill into .claude/skills/chromia-skill. For Codex, place it under .agents/skills yourself. Change rellVersion 0.14.5 to 0.16.1 (the newest pin the CLI-bundled Rell accepts; 0.16.7 is the language source tag and fails `chr build`) before scaffolding.
4. Run current chr 0.33.x on the host. Do not use the Docker image CLI 0.27.7 for production work.
5. For directory-chain or EIF, read that repo CLAUDE.md then verify versions against chromia.yml / pom.xml (markdown lags).
There is no official Chromia Claude Code agent or Chromia Codex agent beyond the above.

## Sources checked (primary)

- https://gitlab.com/chromaway/core-tools/chromia-mcp (clone, README, McpTools.kt, prompt_templates.json, docs-repositories.json, claude-code-chromia)
- https://gitlab.com/chromaway/core-tools/chromia-lsp-mcp (clone, README, CLAUDE.md, CHANGELOG, libs.versions.toml, Prompts.kt)
- https://gitlab.com/chromaway/core-tools/chromia-images/-/tree/dev/claude-code (Dockerfile)
- https://bitbucket.org/chromawallet/chromia-skill (clone, commit 4b639bd)
- https://gitlab.com/chromaway/core/postchain-eif/-/raw/dev/CLAUDE.md
- https://github.com/ChromiaProject/directory-chain/blob/dev/CLAUDE.md
- Anthropic official marketplace.json: 289 plugins, 0 Chromia hits
- GitHub chromindscan/chromia-mcp (wallet MCP, unofficial)
- OpenAI Codex skills.md (generic; no Chromia)
- Workspace notes: rell-cli.md and code-cli-directory.md (Rell 0.16.7, CLI 0.33.2)
- Clones used for this audit live under chromia-knowledge/sources/.
