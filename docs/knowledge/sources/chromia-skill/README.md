# Chromia development agent skill

Agent skill for Chromia blockchain dApp development: Rell, Chromia CLI (`chr`), Postchain, `chromia.yml`, FT4, cross-chain patterns, and related clients. Use it with any coding agent that loads skills from a `SKILL.md` directory (Cursor, Claude Code, Codex, or your own toolchain).

## What it covers

- `chromia.yml` configuration and project structure
- FT4 accounts, auth descriptors, and authentication patterns
- ICCF cross-chain proofs and ICMF async messaging
- EIF EVM integration and CRC2 NFT standard
- Filehub decentralized storage
- Postchain client setup (`@chromia/postchain-client`, `@chromia/ft4`)
- AI extensions (vector DB, inference, Stork oracle)
- CLI workflows: test, local node, deployment

> **Not for** Ethereum/Solidity, Solana/Rust, or other non-Chromia blockchain development.

## Installation

The skill root is this repo’s `chromia-skill/` directory (must contain `SKILL.md` and optional `references/`). Copy it to wherever your agent expects skills; many tools expect **one folder per skill** whose name matches the skill id—here that is **`chromia-skill`** (see `name` in `chromia-skill/SKILL.md` frontmatter).

**Generic rule:** install so the agent sees `…/chromia-skill/SKILL.md` (and `…/chromia-skill/references/` if present).

There are two installation scopes:

- **Project-level** — the skill loads only when the agent works inside that specific repo. Install into the project’s config directory (e.g. `.claude/skills/` or `.cursor/skills/` at the repo root). Best when only one project needs Chromia guidance.
- **User-level** — the skill loads in every project you open. Install into your home directory config (e.g. `~/.claude/skills/` or `~/.cursor/skills/`). Best if you work on multiple Chromia projects and want the skill always available.

### Claude Code

Project-level:

```bash
cp -r chromia-skill /path/to/your-project/.claude/skills/chromia-skill
```

User-level:

```bash
cp -r chromia-skill ~/.claude/skills/chromia-skill
```

### Cursor

Project-level:

```bash
cp -r chromia-skill /path/to/your-project/.cursor/skills/chromia-skill
```

User-level:

```bash
cp -r chromia-skill ~/.cursor/skills/chromia-skill
```

### Other agents

Follow your product’s docs for “skills” or “instructions”: place the same folder layout (`SKILL.md` + `references/`) in the path that tool reads at session start.

## Reference files

The `chromia-skill/references/` directory contains detailed reference docs:

| File | Topic |
|---|---|
| `ft4-integration.md` | FT4 accounts, auth descriptors, session management |
| `iccf-icmf.md` | ICCF cross-chain proofs, ICMF async messaging |
| `eif-evm.md` | EIF Ethereum interoperability framework |
| `crc2-nft.md` | CRC2 NFT standard |
| `postchain-client.md` | Postchain client initialization and usage |
| `filehub.md` | Filehub decentralized storage |
| `ai-extensions.md` | Vector DB, inference, Stork oracle |
| `governance.md` | Governance and provider staking |

## Contributing

Contributions are welcome — fork the repo, make your changes under `chromia-skill/`, and open a pull request.
