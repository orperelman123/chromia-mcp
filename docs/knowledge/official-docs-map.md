# Chromia official documentation map

Retrieved 2026-08-26 (IL / UTC+3). Official sources only. Every URL below was live-checked (HTTP GET/HEAD) or listed in the official sitemap. Invented paths are not included. 404s are marked.

Primary site: https://docs.chromia.com
Sitemap (single file, 384 URLs): https://docs.chromia.com/sitemap.xml
`https://docs.chromia.com/sitemap-0.xml` — **404**
`https://docs.chromia.com/sitemap-1.xml` — **404**
`https://docs.chromia.com/robots.txt` — **404**

Sitemap noise (do not treat as docs): `/ignore_me`, `/search`.

---

## 1. How the docs site is organized

The homepage (`https://docs.chromia.com/`) states five top-level sections. There is **no** landing page at the section root — `/get-started`, `/build`, `/ecosystem`, `/rell`, `/reference` are all **404**. Open a child page, not the section URL.

| Section | What it is for | Canonical prefix | Pages in sitemap |
|---|---|---|---|
| **Get Started** | High-level Chromia intro, architecture, protocols, install, first dapp, use cases | `/get-started/` | 44 |
| **Build** | How to actually ship: CLI, `chromia.yml`, FT4, clients, cookbook, deploy testnet/mainnet, database, token-chain, integrations | `/build/` | 164 |
| **Ecosystem** | Network-level tools: providers/nodes/PMC, Directory/Economy config, Bridge/EIF, Filehub, Explorer, Governance, extensions | `/ecosystem/` | 112 |
| **Rell** | Language book: intro, types, modules, systemlib, tests, security, release notes | `/rell/` | 58 |
| **Reference** | Pointers only (2 pages): FT4 generated APIs + terminology | `/reference/` | 2 |

Also on the sitemap, not a nav section:

- `/updates` — platform changelog hub
- `/pages/ft4-rell/` and `/pages/ft4-ts-client/` — generated API sites (linked from `/reference/ft4/`, **not** in the 384-URL sitemap)

Official narrative docs live under `docs.chromia.com`. Generated Rell/TS API and Directory-chain API live on separate generated sites (same Chromia/ChromaWay origin).

### Get Started (`/get-started/`)

- `about/` — what Chromia is: architecture, chains (Directory, Economy, Token, Cluster/System Anchoring), node, hosting, providers, staking, wallets, Rell vs EVM, protocols (FT4, GTV, GTX, ICCF, ICMF)
- `installation` — install paths: Dev Container, Codespaces, local (Postgres + `chr`)
- `create-dapp/` — Hello World via CLI + first testnet deploy
- `use-cases/` — Vector DB, AI inference, Stork, ICCF/ICMF

### Build (`/build/`)

- `cli/` — Chromia CLI intro, commands, seeder, libraries, keys, doc-site generation, CLI release notes
- `configuration/` — `chromia.yml` (project-config), project structure, blockchain properties
- `ft4/` — FT4 narrative (accounts, assets, auth, client, backend, config values, setup)
- `clients/` — Postchain clients (JS/TS, Kotlin, Python, Go, Rust, C#), REST API, FT4/Filehub/Bridge clients, React kit, MCP
- `cookbook/` — copy-paste recipes (CLI, queries, txs, account strategies, data inspection)
- `deployment/` — testnet + mainnet + tCHR + Vault listing + frontend deploy
- `database/` — local Postgres / relational model
- `token-chain/`, `integrations/`, `vector-search/`

### Ecosystem (`/ecosystem/`)

- `providers/` — run a node, PMC CLI, Directory/Economy chain config, containers, rewards
- `bridge/` — EIF/bridge overview, deploy, client, lease, mass-exit, troubleshooting
- `filehub/` — Filehub + Filechain deploy/configure
- `block-explorer/` — Explorer features and usage
- `governance/` — on-chain governance + EIF/ICMF extensions
- `extensions/` — Stork, Vector DB, AI inference, ZKP

### Rell (`/rell/`)

Language book. Start at `/rell/rell-intro` (not `/rell`). System library is **narrative** under `/rell/language-features/systemlib/` — there is **no** generated `/pages/rell/` site (**404**).

### Reference (`/reference/`)

- `/reference/ft4/` — hub that only links out to generated FT4 Rell + FT4 TS client APIs
- `/reference/terminology`

---

## 2. Compact URL index

Canonical form is the sitemap path. Most pages also 200 with a trailing slash. Bare section roots 404 (see §3).

### Get started

- https://docs.chromia.com/ — homepage / section map
- https://docs.chromia.com/get-started/installation — install CLI, Postgres, Dev Container, Codespaces
- https://docs.chromia.com/get-started/create-dapp/ — first dapp
- https://docs.chromia.com/get-started/create-dapp/run-dapp-cli
- https://docs.chromia.com/get-started/create-dapp/deploy-to-testnet
- https://docs.chromia.com/get-started/about/ — platform overview
- https://docs.chromia.com/get-started/about/what-is-rell
- https://docs.chromia.com/get-started/about/architecture/ — clusters, nodes, anchoring
- https://docs.chromia.com/get-started/about/architecture-summary
- https://docs.chromia.com/get-started/about/architecture/platform-architecture
- https://docs.chromia.com/get-started/about/architecture/node
- https://docs.chromia.com/get-started/about/architecture/chain-governance
- https://docs.chromia.com/get-started/about/architecture/chains/directory-chain
- https://docs.chromia.com/get-started/about/architecture/chains/economy-chain
- https://docs.chromia.com/get-started/about/architecture/chains/token-chain
- https://docs.chromia.com/get-started/about/architecture/chains/cluster-anchoring-chain
- https://docs.chromia.com/get-started/about/architecture/chains/system-anchoring-chain
- https://docs.chromia.com/get-started/about/protocols/ — FT4, GTV, GTX, ICCF, ICMF
- https://docs.chromia.com/get-started/about/protocols/ft4
- https://docs.chromia.com/get-started/about/protocols/gtv
- https://docs.chromia.com/get-started/about/protocols/gtx
- https://docs.chromia.com/get-started/about/protocols/iccf
- https://docs.chromia.com/get-started/about/protocols/icmf
- https://docs.chromia.com/get-started/about/providers
- https://docs.chromia.com/get-started/about/hosting
- https://docs.chromia.com/get-started/about/supported-wallets
- https://docs.chromia.com/get-started/use-cases/

### Rell language

Intro / practice:

- https://docs.chromia.com/rell/rell-intro — **open this first** for any Rell question
- https://docs.chromia.com/rell/core-concepts
- https://docs.chromia.com/rell/modules
- https://docs.chromia.com/rell/rell-best-practices
- https://docs.chromia.com/rell/rell-doc — doc comments
- https://docs.chromia.com/rell/tests
- https://docs.chromia.com/rell/security
- https://docs.chromia.com/rell/special-operations
- https://docs.chromia.com/rell/analyze-rell-dapp-code
- https://docs.chromia.com/rell/releases

Language features:

- https://docs.chromia.com/rell/language-features/identifiers-syntax
- https://docs.chromia.com/rell/language-features/types/ — simple / collection / complex / iterables / sub / virtual
- https://docs.chromia.com/rell/language-features/modules/ — entity, object, struct, enum, function, operation, query, namespace, mount, abstract, size-constraint
- https://docs.chromia.com/rell/language-features/statements/
- https://docs.chromia.com/rell/language-features/expressions/
- https://docs.chromia.com/rell/language-features/database/ — create / update / delete

System library (narrative, official for built-ins):

- https://docs.chromia.com/rell/language-features/systemlib/
- https://docs.chromia.com/rell/language-features/systemlib/global-functions
- https://docs.chromia.com/rell/language-features/systemlib/require-function
- https://docs.chromia.com/rell/language-features/systemlib/system-entities
- https://docs.chromia.com/rell/language-features/systemlib/system-queries
- https://docs.chromia.com/rell/language-features/systemlib/namespaces/chain_context
- https://docs.chromia.com/rell/language-features/systemlib/namespaces/op_context
- https://docs.chromia.com/rell/language-features/systemlib/namespaces/crypto
- https://docs.chromia.com/rell/language-features/systemlib/namespaces/meta
- https://docs.chromia.com/rell/language-features/systemlib/namespaces/time

Generated Rell language API at `/pages/rell/` or `/pages/rell-syslib/` — **404**. Use the narrative systemlib pages. Compiler source of truth: GitLab `chromaway/rell` (see §4).

### CLI

Canonical (sitemap):

- https://docs.chromia.com/build/cli/introduction
- https://docs.chromia.com/build/cli/commands/ — command index
- https://docs.chromia.com/build/cli/commands/build
- https://docs.chromia.com/build/cli/commands/code
- https://docs.chromia.com/build/cli/commands/create-rell-dapp
- https://docs.chromia.com/build/cli/commands/deployment
- https://docs.chromia.com/build/cli/commands/eif
- https://docs.chromia.com/build/cli/commands/generate
- https://docs.chromia.com/build/cli/commands/help
- https://docs.chromia.com/build/cli/commands/keygen
- https://docs.chromia.com/build/cli/commands/library
- https://docs.chromia.com/build/cli/commands/multi-signature
- https://docs.chromia.com/build/cli/commands/node
- https://docs.chromia.com/build/cli/commands/query
- https://docs.chromia.com/build/cli/commands/repl
- https://docs.chromia.com/build/cli/commands/seeder
- https://docs.chromia.com/build/cli/commands/test
- https://docs.chromia.com/build/cli/commands/tools
- https://docs.chromia.com/build/cli/commands/tx
- https://docs.chromia.com/build/cli/commands/version
- https://docs.chromia.com/build/cli/key-pair-management
- https://docs.chromia.com/build/cli/library/
- https://docs.chromia.com/build/cli/Seeder/
- https://docs.chromia.com/build/cli/generating-doc-site
- https://docs.chromia.com/build/cli/cli-release-notes — docs-site changelog (latest listed: CLI 0.30.0, 2026-02-27)

Working **legacy alias** (not in sitemap, 200):

- https://docs.chromia.com/cli/introduction
- https://docs.chromia.com/cli/commands/
- https://docs.chromia.com/cli/commands/build (and the other command slugs)

Bare `/cli` and `/build/cli` — **404**.

Official source repo: https://gitlab.com/chromaway/core-tools/chromia-cli
CHANGELOG (dev branch, 200): https://gitlab.com/chromaway/core-tools/chromia-cli/-/blob/dev/CHANGELOG.md
`…/blob/master/CHANGELOG.md` — **404** (no `master` branch)

### chromia.yml

- https://docs.chromia.com/build/configuration/project-config — **the** `chromia.yml` schema (blockchains, deployments, compile, database, test, libs, docs, YAML anchors/`!include`)
- https://docs.chromia.com/build/configuration/project-structure
- https://docs.chromia.com/build/configuration/blockchain-properties
- https://docs.chromia.com/build/database/overview — local Postgres defaults used by `database:`
- https://docs.chromia.com/build/database/getting-started
- FT4-specific keys: https://docs.chromia.com/build/ft4/configuration-values plus generated `module_args` structs (see FT4)

`/cli/project-config` — **404**.

### FT4

Narrative (sitemap `/build/ft4/…`):

- https://docs.chromia.com/build/ft4/intro — **open this first** for “what is FT4”
- https://docs.chromia.com/build/ft4/terms
- https://docs.chromia.com/build/ft4/setup/ft4-setup
- https://docs.chromia.com/build/ft4/setup/imports
- https://docs.chromia.com/build/ft4/configuration-values — narrative `moduleArgs` tables
- https://docs.chromia.com/build/ft4/account-management/overview
- https://docs.chromia.com/build/ft4/account-management/auth-descriptors
- https://docs.chromia.com/build/ft4/account-management/multisig
- https://docs.chromia.com/build/ft4/asset-management/asset
- https://docs.chromia.com/build/ft4/asset-management/transfer-assets
- https://docs.chromia.com/build/ft4/backend/accounts/overview
- https://docs.chromia.com/build/ft4/backend/accounts/accounts-and-auth-descriptors
- https://docs.chromia.com/build/ft4/backend/accounts/open
- https://docs.chromia.com/build/ft4/backend/accounts/fixed
- https://docs.chromia.com/build/ft4/backend/accounts/subscription
- https://docs.chromia.com/build/ft4/backend/accounts/account-linking
- https://docs.chromia.com/build/ft4/backend/assets/register-assets
- https://docs.chromia.com/build/ft4/backend/assets/asset-amounts
- https://docs.chromia.com/build/ft4/backend/assets/locking-assets
- https://docs.chromia.com/build/ft4/backend/authentication/auth
- https://docs.chromia.com/build/ft4/backend/authentication/auth-descriptors-and-rules
- https://docs.chromia.com/build/ft4/backend/authentication/multi-sig
- https://docs.chromia.com/build/ft4/backend/cross-chain/introduction
- https://docs.chromia.com/build/ft4/backend/cross-chain/cross-chain-assets
- https://docs.chromia.com/build/ft4/backend/cross-chain/cross-chain-transfers
- https://docs.chromia.com/build/ft4/backend/cross-chain/automate-cross-chain-asset-registration
- https://docs.chromia.com/build/ft4/client/client-setup
- https://docs.chromia.com/build/ft4/client/client-login
- https://docs.chromia.com/build/ft4/client/client-key-store
- https://docs.chromia.com/build/ft4/client/client-account-registration
- https://docs.chromia.com/build/ft4/client/client-auth-descriptors
- https://docs.chromia.com/build/ft4/client/client-transfer-assets
- https://docs.chromia.com/build/ft4/client/client-orchestrator
- https://docs.chromia.com/build/ft4/pagination
- https://docs.chromia.com/build/ft4/prioritization
- https://docs.chromia.com/build/ft4/code-examples
- https://docs.chromia.com/build/ft4/releases/ft4

Reference hub + generated APIs (not in sitemap; live 200):

- https://docs.chromia.com/reference/ft4/ — links only
- https://docs.chromia.com/pages/ft4-rell/ — **FT4 Rell generated API** (struct / function / op / query source of truth)
- https://docs.chromia.com/pages/ft4-rell/index.html — same
- https://docs.chromia.com/pages/ft4-ts-client/ — **FT4 TypeScript client generated API**
- https://docs.chromia.com/pages/ft4-ts-client/index.html — same

Generated Rell modules observed on `/pages/ft4-rell/` (Dokka-style paths under `-f-t4 -library/`):

- `lib.ft4` (includes `module_args`, `get_module_args`)
- `lib.ft4.core.accounts` (+ `auth_flags`, `linking`, `strategies`, `strategies.open`, `strategies.transfer`)
- `lib.ft4.core.admin`
- `lib.ft4.core.assets` (+ `Unsafe`)
- `lib.ft4.core.auth`
- `lib.ft4.core.crosschain` (+ `Unsafe`)
- `lib.ft4.core.prioritization`
- `lib.ft4.external.accounts` (+ `linking`, `strategies`)
- `lib.ft4.external.admin`
- `lib.ft4.external.assets`
- `lib.ft4.external.auth`
- `lib.ft4.external.crosschain`
- `lib.ft4.utils`
- `lib.ft4.version`

Example generated struct page (authority for field names/types):  
`https://docs.chromia.com/pages/ft4-rell/-f-t4 -library/lib.ft4.core.accounts/module_args/index.html`  
`https://docs.chromia.com/pages/ft4-rell/-f-t4 -library/lib.ft4/module_args/index.html`

Bare `/ft4`, `/ft4/`, `/build/ft4`, `/build/ft4/` — **404**.

Official source: https://gitlab.com/chromaway/ft4-lib (200). `gitlab.com/chromaway/ft4/ft4-lib` — **403 / sign-in**.

Cookbook recipes that use FT4 (not a substitute for the API):

- https://docs.chromia.com/build/cookbook/overview
- https://docs.chromia.com/build/cookbook/account-creation/open-strategy
- https://docs.chromia.com/build/cookbook/account-creation/transfer-fee-strategy
- https://docs.chromia.com/build/cookbook/account-creation/transfer-open-strategy
- https://docs.chromia.com/build/cookbook/account-creation/transfer-subscription-strategy
- https://docs.chromia.com/build/cookbook/transaction-creation/make-transfer
- https://docs.chromia.com/build/cookbook/transaction-creation/call-operation-with-ft4-auth
- https://docs.chromia.com/build/cookbook/transaction-creation/crosschain-transfer

### Clients (JS/TS / Kotlin / Python + others)

Hub:

- https://docs.chromia.com/build/clients/overview
- https://docs.chromia.com/build/clients/postchain-clients/

Postchain clients:

- https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/
- https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/hello-world-quickstart
- https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/reference
- https://docs.chromia.com/build/clients/postchain-clients/kotlin-client
- https://docs.chromia.com/build/clients/postchain-clients/python-client
- https://docs.chromia.com/build/clients/postchain-clients/go-client
- https://docs.chromia.com/build/clients/postchain-clients/rust-client
- https://docs.chromia.com/build/clients/postchain-clients/c-sharp-client
- https://docs.chromia.com/build/clients/postchain-rest-api

Product clients / kits:

- https://docs.chromia.com/build/clients/ft4-client — JS/TS FT4 client narrative
- https://docs.chromia.com/pages/ft4-ts-client/ — generated TS API
- https://docs.chromia.com/build/clients/filehub-client
- https://docs.chromia.com/build/clients/bridge-client
- https://docs.chromia.com/build/clients/react-kit
- https://docs.chromia.com/build/clients/mcp-server

Official GitLab (public 200):

- https://gitlab.com/chromaway/core/postchain-client — Kotlin/Java Postchain client
- Top-level `gitlab.com/chromaway/postchain-client` and `…/clients/postchain-client` — **403 / sign-in** (not a public URL)

### Deployment (testnet / mainnet)

- https://docs.chromia.com/build/deployment/ — deploy hub
- https://docs.chromia.com/build/deployment/testnet/getting-started — **open this first** for testnet
- https://docs.chromia.com/build/deployment/testnet/get-container
- https://docs.chromia.com/build/deployment/testnet/deploy-dapp
- https://docs.chromia.com/build/deployment/testnet/connect-client
- https://docs.chromia.com/build/deployment/testnet/list-dapp-vault
- https://docs.chromia.com/build/deployment/testnet-tokens/get-tchr-chromia
- https://docs.chromia.com/build/deployment/testnet-tokens/get-tchr-binance
- https://docs.chromia.com/build/deployment/mainnet/getting-started — **open this first** for mainnet
- https://docs.chromia.com/build/deployment/mainnet/get-container
- https://docs.chromia.com/build/deployment/mainnet/deploy-dapp
- https://docs.chromia.com/build/deployment/mainnet/connect-client
- https://docs.chromia.com/build/deployment/mainnet/multi-deployment
- https://docs.chromia.com/build/deployment/deploy-frontend-dapp
- https://docs.chromia.com/get-started/create-dapp/deploy-to-testnet — first-dapp shortcut (not the full guide)
- CLI verbs: https://docs.chromia.com/build/cli/commands/deployment
- `chromia.yml` `deployments:` block: https://docs.chromia.com/build/configuration/project-config

Directory-chain BRIDs quoted on the project-config page (verify on Explorer before using):

- Mainnet directory: `x"7E5BE539EF62E48DDA7035867E67734A70833A69D2F162C457282C319AA58AE4"`
- Testnet directory: `x"6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92"`

### Postchain / providers / nodes

Architecture (what a node is):

- https://docs.chromia.com/get-started/about/architecture/node
- https://docs.chromia.com/get-started/about/architecture/platform-architecture
- https://docs.chromia.com/get-started/about/providers

Run / operate a node:

- https://docs.chromia.com/ecosystem/providers/overview
- https://docs.chromia.com/ecosystem/providers/nodes/ — **open this first** for node/Postgres ops
- https://docs.chromia.com/ecosystem/providers/nodes/prepare-node
- https://docs.chromia.com/ecosystem/providers/nodes/start-a-node
- https://docs.chromia.com/ecosystem/providers/nodes/add-node
- https://docs.chromia.com/ecosystem/providers/nodes/add-replica-node
- https://docs.chromia.com/ecosystem/providers/nodes/node-config
- https://docs.chromia.com/ecosystem/providers/nodes/node-maintenance-guidelines
- https://docs.chromia.com/ecosystem/providers/nodes/upgrade-postchain
- https://docs.chromia.com/ecosystem/providers/nodes/logging
- https://docs.chromia.com/ecosystem/providers/nodes/setup-tls
- https://docs.chromia.com/ecosystem/providers/nodes/setup-prometheus
- https://docs.chromia.com/ecosystem/providers/nodes/api
- https://docs.chromia.com/ecosystem/providers/nodes/automated-network-setup
- https://docs.chromia.com/ecosystem/providers/nodes/provider-keypair
- https://docs.chromia.com/ecosystem/providers/nodes/manage-provider-keys
- https://docs.chromia.com/ecosystem/providers/nodes/change-ft4-key
- https://docs.chromia.com/ecosystem/providers/nodes/install-pmc
- https://docs.chromia.com/ecosystem/providers/apis-1
- https://docs.chromia.com/ecosystem/providers/blockchain-based-provider-authentication
- https://docs.chromia.com/ecosystem/providers/rewards
- https://docs.chromia.com/ecosystem/providers/container-management/

PMC (provider CLI, not `chr`):

- https://docs.chromia.com/ecosystem/providers/pmc/pmccli-installation
- https://docs.chromia.com/ecosystem/providers/pmc/commands/
- https://docs.chromia.com/ecosystem/providers/pmc/troubleshooting

Local Postchain via developer CLI:

- https://docs.chromia.com/build/cli/commands/node (`chr node start`)
- https://docs.chromia.com/build/database/overview — Postgres for local node
- https://docs.chromia.com/get-started/installation — default DB `postchain` / user `postchain` / password `postchain`

Official GitLab (public 200):

- https://gitlab.com/chromaway/core/postchain
- Top-level `gitlab.com/chromaway/postchain` — redirects to sign-in (not a public project URL)

### Directory / Economy

Narrative:

- https://docs.chromia.com/get-started/about/architecture/chains/directory-chain
- https://docs.chromia.com/get-started/about/architecture/chains/economy-chain
- https://docs.chromia.com/ecosystem/providers/nodes/directory-chain-config
- https://docs.chromia.com/ecosystem/providers/nodes/economy-chain-config
- https://docs.chromia.com/get-started/about/staking/
- https://docs.chromia.com/get-started/about/staking/provider-staking
- https://docs.chromia.com/get-started/about/staking/user-delegation
- https://docs.chromia.com/ecosystem/providers/rewards
- PMC economy/lease/cluster/provider commands under `/ecosystem/providers/pmc/commands/`

Generated Directory-chain / system-chain API (official GitLab Pages):

- https://chromaway.gitlab.io/core/directory-chain/ — **open this** for Directory/Economy/Anchoring module, query, and operation signatures
- Source: https://gitlab.com/chromaway/core/directory-chain (200)
- CHANGELOG tree (dev): https://gitlab.com/chromaway/core/directory-chain/-/blob/dev/CHANGELOG.md (200; GitLab serves the tree)
- Top-level `gitlab.com/chromaway/directory-chain` — **403 / sign-in**

### Filehub

- https://docs.chromia.com/ecosystem/filehub/overview
- https://docs.chromia.com/ecosystem/filehub/filehub-setup/deploy-filehub
- https://docs.chromia.com/ecosystem/filehub/filehub-setup/deploy-filechain
- https://docs.chromia.com/ecosystem/filehub/filehub-setup/deploy-bundle
- https://docs.chromia.com/ecosystem/filehub/configure-filehub/filehub-configure
- https://docs.chromia.com/ecosystem/filehub/configure-filehub/filehub-work
- https://docs.chromia.com/build/clients/filehub-client

### EIF / bridge

Bridge product (EVM ↔ Chromia):

- https://docs.chromia.com/ecosystem/bridge/overview — **open this first**
- https://docs.chromia.com/ecosystem/bridge/bridge-lease
- https://docs.chromia.com/ecosystem/bridge/deploy-bridge/deploy-bridge-chains
- https://docs.chromia.com/ecosystem/bridge/deploy-bridge/deploy-bridge-contract
- https://docs.chromia.com/ecosystem/bridge/deploy-bridge/deploy-erc20-token
- https://docs.chromia.com/ecosystem/bridge/deploy-bridge/register-bridge
- https://docs.chromia.com/ecosystem/bridge/deploy-bridge/interact-with-frontend
- https://docs.chromia.com/ecosystem/bridge/bridge-client/client
- https://docs.chromia.com/ecosystem/bridge/bridge-client/work-with-client
- https://docs.chromia.com/ecosystem/bridge/bridge-client/example
- https://docs.chromia.com/ecosystem/bridge/bridge-troubleshooting/bridge-deposit-troubleshooting
- https://docs.chromia.com/ecosystem/bridge/bridge-troubleshooting/bridge-withdraw-troubleshooting
- https://docs.chromia.com/ecosystem/bridge/mass-exit/overview
- https://docs.chromia.com/build/clients/bridge-client

EIF as a module / CLI / governance extension (not the same as the user-facing Bridge guide):

- https://docs.chromia.com/build/cli/commands/eif — `chr eif …`
- https://docs.chromia.com/ecosystem/governance/getting-started/extensions/eif
- https://docs.chromia.com/ecosystem/governance/getting-started/governance-starter-kit/eif-configuration

### Explorer / Vault

Explorer:

- https://docs.chromia.com/ecosystem/block-explorer/overview
- https://docs.chromia.com/ecosystem/block-explorer/features
- https://docs.chromia.com/ecosystem/block-explorer/using-explorer

Vault listing (publish a dapp in Chromia Vault):

- https://docs.chromia.com/build/deployment/vault-listing/
- https://docs.chromia.com/build/deployment/vault-listing/quick-vault-listing
- https://docs.chromia.com/build/deployment/vault-listing/dynamic-vault-listing
- https://docs.chromia.com/build/deployment/testnet/list-dapp-vault

---

## Official GitLab (chromaway)

Group: https://gitlab.com/chromaway (200)

Public projects confirmed 200 (use these, not the short names):

| Project | Public URL | Notes |
|---|---|---|
| Rell | https://gitlab.com/chromaway/rell | Language + compiler. `…/core/rell` is **403** |
| Postchain | https://gitlab.com/chromaway/core/postchain | Node / Postchain. Top-level `/postchain` is sign-in |
| FT4 lib | https://gitlab.com/chromaway/ft4-lib | Rell FT4 library. `…/ft4/ft4-lib` is **403** |
| Chromia CLI | https://gitlab.com/chromaway/core-tools/chromia-cli | `chr`. Top-level `/chromia-cli` is **403**. CHANGELOG on `dev`, not `master` |
| Directory chain | https://gitlab.com/chromaway/core/directory-chain | System chains. Pages: https://chromaway.gitlab.io/core/directory-chain/ |
| Postchain client | https://gitlab.com/chromaway/core/postchain-client | Kotlin/Java client. Top-level `/postchain-client` is **403** |

Public groups: https://gitlab.com/chromaway/core (200), https://gitlab.com/chromaway/core-tools (200).  
`gitlab.com/chromaway/clients` and `gitlab.com/chromaway/ft4` — **403 / sign-in**.

Do not treat blogs, tweets, or unofficial wikis as authority.

---

## 3. Known 404s and path traps

Pattern: **section roots 404**. Deep pages 200. Several old prefixes still alias.

### Confirmed 404

| URL | Status | Use instead |
|---|---|---|
| `https://docs.chromia.com/rell` and `/rell/` | 404 | `/rell/rell-intro` |
| `https://docs.chromia.com/cli` and `/cli/` | 404 | `/build/cli/introduction` or `/cli/introduction` |
| `https://docs.chromia.com/build/cli` and `/build/cli/` | 404 | `/build/cli/introduction` |
| `https://docs.chromia.com/ft4` and `/ft4/` | 404 | `/build/ft4/intro` |
| `https://docs.chromia.com/build/ft4` and `/build/ft4/` | 404 | `/build/ft4/intro` |
| `https://docs.chromia.com/intro` and `/intro/` | 404 | `/get-started/about/` |
| `https://docs.chromia.com/get-started` and `/get-started/` | 404 | `/get-started/installation` |
| `https://docs.chromia.com/build` and `/build/` | 404 | a child, e.g. `/build/cli/introduction` |
| `https://docs.chromia.com/ecosystem` and `/ecosystem/` | 404 | a child, e.g. `/ecosystem/providers/overview` |
| `https://docs.chromia.com/reference` and `/reference/` | 404 | `/reference/ft4/` or `/reference/terminology` |
| `https://docs.chromia.com/pages/rell/` | 404 | `/rell/language-features/systemlib/` |
| `https://docs.chromia.com/pages/rell-syslib/` | 404 | same |
| `https://docs.chromia.com/cli/project-config` | 404 | `/build/configuration/project-config` |
| `https://docs.chromia.com/sitemap-0.xml` | 404 | `/sitemap.xml` (already complete) |
| `https://docs.chromia.com/robots.txt` | 404 | — |
| `https://gitlab.com/chromaway/core-tools/chromia-cli/-/blob/master/CHANGELOG.md` | 404 | `…/blob/dev/CHANGELOG.md` |

### Working aliases (200, not in sitemap — prefer the sitemap path)

| Alias | Canonical |
|---|---|
| `/cli/introduction`, `/cli/commands/`, `/cli/commands/<cmd>` | `/build/cli/…` |
| `/intro/about/architecture/` | `/get-started/about/architecture/` |

Do not assume every `/intro/…` or `/cli/…` child aliases. Only the ones HEAD-checked above are confirmed.

### Other traps

- Trailing-slash: leaf pages 301/200 to `…/` ; that is fine. **Bare directory of a section is 404**, not a redirect to the first child.
- Generated FT4 Dokka paths contain a literal space and hyphen soup: `-f-t4 -library/lib.ft4.…`. Copy from `/pages/ft4-rell/`, do not guess.
- `chr` command pages document flags; they lag new flags. New verbs/flags appear first in GitLab CHANGELOG / `/build/cli/cli-release-notes`.
- PMC (`/ecosystem/providers/pmc/`) is **not** Chromia CLI. Different binary, different command tree.
- EIF appears in three places (CLI command, governance extension, Bridge product). “How do I bridge CHR/ERC-20?” → `/ecosystem/bridge/overview`, not `chr eif`.
- `/get-started/about/protocols/ft4` is a protocol overview. Implementation is `/build/ft4/`.
- Library install in `chromia.yml` has two styles (library-chain vs git). Project-config is the schema; FT4 setup page has the concrete FT4 `libs:` example.

---

## 4. Which page wins when sources disagree

Order is strict. Later rows do not override earlier ones.

### FT4 Rell (structs, `module_args`, operations, queries)

1. **Wins:** generated FT4 Rell API — https://docs.chromia.com/pages/ft4-rell/ and the specific `lib.ft4…/module_args/` (or function/op) page.
2. Source if the generated page is missing or stale: https://gitlab.com/chromaway/ft4-lib (the Rell in that tag/RID).
3. Narrative `/build/ft4/configuration-values` — human tables, defaults, which keys exist. **Loses** on field name, type, nesting, or requiredness if it disagrees with `module_args`.
4. `/build/ft4/setup/imports` and `/build/ft4/setup/ft4-setup` — which modules to import and a full example `chromia.yml`. Lose to (1) on types.
5. Cookbook — usage sketches only.

Typical conflict: narrative `configuration-values` lists a flat/defaulted key; generated `lib.ft4.core.accounts/module_args` has the real struct (nested `rate_limit`, `auth_descriptor`, etc.). Code the struct.

### FT4 TypeScript client

1. **Wins:** https://docs.chromia.com/pages/ft4-ts-client/
2. Narrative: `/build/ft4/client/*` and `/build/clients/ft4-client`
3. Postchain JS/TS client (`/build/clients/postchain-clients/javascript-typescript/`) is the lower-level client. FT4 client wraps it. Do not mix their types.

### Rell language / systemlib

1. **Wins for syntax and built-ins:** `/rell/…` book, especially `/rell/language-features/systemlib/` and the type/module pages.
2. **Wins for compiler behavior / new keywords / version:** GitLab https://gitlab.com/chromaway/rell plus `/rell/releases`.
3. There is no generated `/pages/rell/` API. Do not invent one.

### CLI commands / flags / chromia.yml keys that `chr` understands

1. **Wins for “does this flag exist in version X?”:** GitLab CHANGELOG on `dev` — https://gitlab.com/chromaway/core-tools/chromia-cli/-/blob/dev/CHANGELOG.md — then the mirrored `/build/cli/cli-release-notes`.
2. **Wins for “how do I invoke it / what are the flags?”:** `/build/cli/commands/<cmd>` (canonical). `/cli/commands/<cmd>` is the same page via alias.
3. **Wins for `chromia.yml` schema (keys, types, defaults, `!include`, env override):** `/build/configuration/project-config`.
4. If CHANGELOG says a flag was added last week and the command page omits it, **CHANGELOG wins** for existence; still use the command page for the rest of the interface.
5. `chr --help` / `chr <cmd> --help` on the installed binary beats both for the binary you actually have.

### Deploy testnet / mainnet

1. **Wins:** `/build/deployment/testnet/…` or `/build/deployment/mainnet/…` matching the network.
2. `deployments:` YAML + directory BRID/URL: `/build/configuration/project-config` (and Explorer to re-verify BRID).
3. First-dapp `/get-started/create-dapp/deploy-to-testnet` is a subset. Lose to the full testnet guide.
4. Provider/container internals: `/ecosystem/providers/…` (you are not a dapp deployer in that tree).

### Directory / Economy / system chains

1. **Wins for queries/operations/modules:** https://chromaway.gitlab.io/core/directory-chain/ + https://gitlab.com/chromaway/core/directory-chain
2. Operator how-to (edit config, run node): `/ecosystem/providers/nodes/directory-chain-config` and `economy-chain-config`
3. Conceptual: `/get-started/about/architecture/chains/directory-chain` and `economy-chain`

### Postchain / Postgres / node

1. Local dapp Postgres + `database:` in `chromia.yml`: `/get-started/installation` + `/build/database/overview` + project-config `database:`
2. Production node / Postchain upgrade / TLS / Prometheus: `/ecosystem/providers/nodes/*`
3. Engine internals / versions: https://gitlab.com/chromaway/core/postchain
4. `chr node` is a **dev** node, not a provider node. Do not apply provider runbooks to `chr node start`.

### Bridge / EIF

1. User/dapp bridge: `/ecosystem/bridge/overview` and children
2. `chr eif`: `/build/cli/commands/eif`
3. Governance-kit EIF module: `/ecosystem/governance/getting-started/extensions/eif`

---

## 5. How to look something up

Open **one** first URL. Escalate only if that page cannot answer.

| Question | Open first | Then | Last resort (official) |
|---|---|---|---|
| Rell syntax, types, `entity`/`struct`/`query`/`operation` | https://docs.chromia.com/rell/rell-intro | matching `/rell/language-features/…` page | `/rell/releases` + https://gitlab.com/chromaway/rell |
| Rell built-in (`chain_context`, `op_context`, `require`, crypto, time, system entities) | https://docs.chromia.com/rell/language-features/systemlib/ | the namespace child page | GitLab `chromaway/rell` (no `/pages/rell/` site) |
| “What is FT4 / do I need it?” | https://docs.chromia.com/build/ft4/intro | `/build/ft4/terms`, `/get-started/about/protocols/ft4` | — |
| FT4 `module_args` / struct fields / op+query signatures | https://docs.chromia.com/pages/ft4-rell/ → `lib.ft4…/module_args/` | `/build/ft4/configuration-values` for prose/defaults | https://gitlab.com/chromaway/ft4-lib |
| FT4 import / chromia.yml `libs:` | https://docs.chromia.com/build/ft4/setup/ft4-setup | `/build/ft4/setup/imports` | project-config `libs:` |
| FT4 JS/TS client method | https://docs.chromia.com/pages/ft4-ts-client/ | `/build/ft4/client/*` | `/build/clients/ft4-client` |
| `chr` command / flag | https://docs.chromia.com/build/cli/commands/ | `/build/cli/cli-release-notes` | GitLab CLI CHANGELOG `dev` |
| `chromia.yml` key | https://docs.chromia.com/build/configuration/project-config | FT4 configuration-values if the key is under `lib.ft4.*` | CLI CHANGELOG if the key is new |
| Deploy my dapp (testnet) | https://docs.chromia.com/build/deployment/testnet/getting-started | get-container → deploy-dapp → connect-client | `/build/cli/commands/deployment` |
| Deploy my dapp (mainnet) | https://docs.chromia.com/build/deployment/mainnet/getting-started | same sequence under `/mainnet/` | project-config `deployments:` |
| Node / Postchain / Postgres (local dev) | https://docs.chromia.com/get-started/installation | `/build/database/overview`, `chr node` command page | project-config `database:` |
| Node / Postchain / Postgres (provider / production) | https://docs.chromia.com/ecosystem/providers/nodes/ | node-config, prepare-node, upgrade-postchain | https://gitlab.com/chromaway/core/postchain |
| Directory / Economy query or module | https://chromaway.gitlab.io/core/directory-chain/ | `/get-started/about/architecture/chains/directory-chain` (or economy) | https://gitlab.com/chromaway/core/directory-chain |
| Filehub | https://docs.chromia.com/ecosystem/filehub/overview | setup + configure children | `/build/clients/filehub-client` |
| Bridge / EIF (move tokens) | https://docs.chromia.com/ecosystem/bridge/overview | deploy-bridge / bridge-client / troubleshooting | `/build/clients/bridge-client` |
| Explorer | https://docs.chromia.com/ecosystem/block-explorer/overview | features, using-explorer | — |
| Vault listing | https://docs.chromia.com/build/deployment/vault-listing/ | quick vs dynamic | testnet `list-dapp-vault` |
| JS/TS Postchain client (not FT4) | https://docs.chromia.com/build/clients/postchain-clients/javascript-typescript/ | hello-world + reference | — |
| Kotlin / Java client | https://docs.chromia.com/build/clients/postchain-clients/kotlin-client | https://gitlab.com/chromaway/core/postchain-client | — |
| Python client | https://docs.chromia.com/build/clients/postchain-clients/python-client | — | — |

### One-line default

- Rell question → `/rell/rell-intro`
- FT4 question → `/build/ft4/intro`, then **generated** `/pages/ft4-rell/` for anything you will type into Rell
- Deploy question → `/build/deployment/testnet/getting-started` or `/mainnet/getting-started`
- Node / Postgres question → local? `/get-started/installation`. Provider? `/ecosystem/providers/nodes/`

---

## Source notes

- Sitemap fetched 2026-08-26: 384 URLs, one file, no sitemap index.
- Homepage “How to use the docs” is the official section definition used in §1.
- Generated FT4 sites and Directory-chain Pages are official Chromia/ChromaWay properties; they are not in `sitemap.xml`.
- Status checks: curl HEAD/GET from this box. Cloudflare served the docs site (200/404 as listed). GitLab 403s are “not a public project at that path”, not “does not exist”.
