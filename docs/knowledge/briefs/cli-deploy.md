# Chromia CLI + deploy — compact ready-brief

**For:** Chromia standalone expert agent. Official sources only. Do not invent APIs.  
**Written:** 2026-08-26 (IL / UTC+3)  
**Sources:** `/workspace/chromia-knowledge/code-cli-directory.md`, `production-deploy.md`, `study-deploy.md`, `official-docs-map.md`; skills `chromia-production-deploy`, `chromia-official-docs`, `chromia-rell-and-cli`.  
**Rule:** quote names from those files. If a file is silent, say so. Source (GitLab) wins over docs.

---

## Versions (source first)

| Artifact | Docs (live 2026-08-26) | GitLab source |
|---|---|---|
| Chromia CLI `chr` | `/build/cli/cli-release-notes` headings stop at **0.30.0** (2026-02-27); bundled Rell 0.15.2 | Repo `chromaway/core-tools/chromia-cli` (id 39844192). Tags through **0.33.2** (`e65f948c`, 2026-07-20). `CHANGELOG.md` on `dev` ends **[0.33.1] - 2026-06-24**. Tag 0.33.2 GitLab text: `"No changes."` (CI-only). 0.33.0 ships Rell **0.16.0**. |
| Directory-chain | Silent on `api_version` number | Repo `chromaway/core/directory-chain` (id 46396847). Tag **1.110.10** (`88a75d09`, 2026-07-31). `src/version.rell`: `query api_version(): integer = 110`. Semver is `1.<api>.<patch>` per README. |
| Java | CLI 0.20.12 notes: "Java 21 upgrade — … now required for `chr`". 0.21.0: package managers include Java 21; override via `RELL_JAVA`. | Same requirement. Skill + `rell-cli.md`: **Java 21+**. Local persistence: **PostgreSQL 16+** (`chromia-rell-and-cli` SKILL). |

Docs command pages describe the **0.30.x-era** surface. Confirm flags on the installed binary / GitLab CHANGELOG. (`code-cli-directory.md` §2.5; `official-docs-map.md` §4 CLI; `chromia-official-docs` SKILL.)

Binary name is **`chr`**. Registration: `chromia-cli/src/main/kotlin/com/chromia/main.kt`. Clikt default = kebab-case of class minus `Command` unless `name=` is set. (`code-cli-directory.md` §1.)

First two letters of each command/subcommand work as a shortcut (`chr de cr` = `chr deployment create`). Docs note; Clikt feature, **not** restated in `main.kt`. (`code-cli-directory.md` §2.1; `study-deploy.md` crawl notes.)

---

## 1. `chr` command catalog (source `main.kt`)

### Top-level (registered in `main`)

| Command | Class | Notes |
|---|---|---|
| `help` | `HelpCommand` | |
| `version` | `VersionCommand` | Prints CLI + rell + postchain + EIF + Java versions |
| `build` | `BuildCommand` | "Build an application and create a blockchain configuration" |
| `create-rell-dapp` | `CreateRellDappCommand` | `name = "create-rell-dapp"` |
| `deployment` | `DeploymentCommand` | "Create and maintain deployments" |
| `eif` | `EifCommand` | "Ethereum Integration Framework commands" |
| `generate` | `GenerateCommand` | "Generate client stubs, documentation, and entity relations for a rell project" |
| `keygen` | `KeygenCommand` | `name = "keygen"` |
| `node` | `NodeCommand` | "Interact with a test node" |
| `query` | `QueryCommand` | Query a running node |
| `repl` | `ReplCommand` | |
| `test` | `TestCommand` | "Run tests in working directory" |
| `tx` | `TxCommand` | |
| `code` | `CodeCommand` | "Code quality management (checking, formatting, linting)" |
| `fetch-config` | `FetchConfigCommand` | **Hidden** (`hiddenFromHelp = true`). `$EXPERIMENTAL_COMMAND`. "Fetch blockchain configuration" |
| `multi-signature` | `MultiSignatureCommand` | "Handle transactions with need of multiple signers" |
| `tools` | `ToolsCommand` | "Miscellaneous tools" |
| `seeder` | `SeederCommand` | "Generate fake data for a local database" |
| `library` | `LibraryCommand` | "Manage organizations and libraries in the Chromia library ecosystem" |

**Aliases** (`main.kt` `aliases()`):

| Alias | Resolves to |
|---|---|
| `generate-client-stubs` | `generate client-stubs` (comment: deprecated) |
| `gtv` | `tools gtv` |
| `install` | `library install` |

`CheckCommand`, `FormatCommand`, `LintCommand` exist as classes but are **not** registered at top level — they are subcommands of `code`. `InstallCommand.kt` exists; public entry is the `install` alias → `library install`. (`code-cli-directory.md` §1.1.)

Docs index lists every **non-hidden** top-level command. Source-only: `fetch-config` (hidden); top-level name `install` (docs only show `chr library install`). (`code-cli-directory.md` §2.1.)

### Groups (source)

| Group | Subcommands |
|---|---|
| `code` | `lint` (`.rell_lint`), `format` (`.rell_format`), `check` ("Check Rell code for compilation errors") |
| `generate` | `client-stubs`, `graph`, `docs-site` |
| `node` | `start` (`StartCommand`), `update` (`UpdateCommand`) |
| `eif` | `generate-events-config` |
| `multi-signature` | `create`, `sign`, `send`, `view` |
| `seeder` | `init`, `generate` |
| `tools` | `gtv` (`GtvCommand`), `validate-config` (`ValidateModelCommand`), `lib-model` (`LibModelGenerateCommand`) |
| `library` | `install`, `create`, `list`, `view`, `deploy`, `versions`, `invite-user`, `delete-dev`, `update-user-permission`, `accept-invitation`, `list-invitations`. Hidden groups: `organization …`, `developer …`. |

`create-rell-dapp --template` values (docs / `rell-cli.md`): `plain`, `plain-multi`, `minimal`, `plain-library`, `asset-management`.

### `chr deployment` (source `DeploymentCommand.commands()`)

Aliases: `pause-container` → `container pause`, `resume-container` → `container resume`.

| Command | Class | Notes |
|---|---|---|
| `deployment create` | `DeployCreateCommand` | `name = "create"` |
| `deployment info` | `DeployInfoCommand` | |
| `deployment inspect` | `DeployInspectCommand` | |
| `deployment update` | `DeployUpdateCommand` | |
| `deployment resume` | `DeployResumeCommand` | name = enum `resume` |
| `deployment pause` | `DeployPauseCommand` | name = `pause` |
| `deployment remove` | `DeployRemoveCommand` | name = `remove`; `-y` confirm |
| `deployment lease-info` | `DeployInspectLeaseCommand` | **Hidden**, experimental |
| `deployment proposal` | `ProposalCommand` | `vote`, `retract-vote`, `list`, `info`, `revoke`, `rename` |
| `deployment voterset` | `VotersetCommand` | `info`, `update`, `list`, plus `VotersetAddDappProvider` (docs: `voterset add-dapp-provider`) |
| `deployment container` | `ContainerCommand` | `configuration`, `pause`, `resume` |
| `deployment remove-container` | `RemoveContainerCommand` | **Hidden**. Economy-chain FT4 op; "Remove a container and its associated lease without refund" |

Live docs document all non-hidden deployment verbs including `proposal retract-vote` (added CLI **0.31.0**). Hidden, not on the live page: `lease-info`, `remove-container`. (`code-cli-directory.md` §1.3, §2.2.)

CLI ≥0.31 **removed** `chr test --sql-log`; `chr test` writes SQL stats as an HTML report. Use `chr repl --sql-log` for generated SQL. (`code-cli-directory.md` §3 [0.31.0]; `chromia-rell-and-cli` SKILL.)

`chr build --skip-lib-check` since 0.30.0. (`code-cli-directory.md` §3.)

---

## 2. `chr deployment create` writes `deployments.<net>.chains` since 0.30.0

**CHANGELOG [0.30.0] - 2026-02-27:** "`chr deployment create` writes the deployment result back to `chromia.yml` and prints the changes." (`code-cli-directory.md` §3, §4.)

**Source (`DeployCreateCommand.kt`):**

1. `preDeploymentVerification`: if `deployModel.chains` already contains the compiled chain name, abort. Confirm with `YesNoPrompt` unless `-y`. Non-interactive without `-y` → `CliktError("Please specify -y option to force deployment")`.
2. `performDeploymentOperation` → `ChromiaDeploymentApi.create(...)`.
3. `afterDeployment` on successes with non-null `blockchainRid`: builds `DeploymentUpdate(networkTarget.network, chain.name, rid)` then `ChromiaYmlWriter.updateDeploymentNodes(settings.modelFile, deploymentsUpdate)` and prints a diff via `printChromiaYmlDiff`. Writer lives in **chromia-cli-tools** (`com.chromia.build.tools.model.writer`) — that artifact was **not** re-fetched.
4. On writer exception only, prints a manual block:

```
deployments:
  <network>:
    chains:
      <name>: x"<hex rid>"
```

5. `explicitChainsToDeploy()` for **create** = `settings.model.blockchains.keys` (all local chains, not only those already in `deployments.*.chains`).

**`deployment update` does not write `chromia.yml`.** Its `explicitChainsToDeploy()` is `deployments[network].chains.keys`; empty → error `"No chains found in deployment"`.

**`deployment remove`** echoes that the user should clean the chain out of config; it does **not** rewrite the file.

Live `/build/cli/commands/deployment` create section (2026-08-26) and in-repo `docs/Functional.md` still say the CLI does **not** auto-update and you must paste the YAML. That is stale. Source + CHANGELOG 0.30.0 win. (`code-cli-directory.md` §2.3, §8.)

Older docs / pre-0.30.0: if you omit `chains:` and re-run `create`, CLI **overrides** the previous deployment. (`production-deploy.md` §2.5; `chromia-production-deploy` SKILL.)

---

## 3. Directory Chain BRIDs (copy exactly)

From [Project settings file](https://docs.chromia.com/build/configuration/project-config) — also in Testnet deploy guide, Hello World status-check URL, PMC provider-key example. (`production-deploy.md` §1; `study-deploy.md` §2.2; `official-docs-map.md` deploy section; `chromia-production-deploy` SKILL.)

| Network | Directory Chain BRID |
|---|---|
| **Mainnet** | `7E5BE539EF62E48DDA7035867E67734A70833A69D2F162C457282C319AA58AE4` |
| **Testnet** | `6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92` |

YAML form uses the `x"…"` wrapper: `x"7E5BE539EF62E48DDA7035867E67734A70833A69D2F162C457282C319AA58AE4"` / `x"6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92"`. PMC example shows Mainnet without the wrapper.

**Re-verify on Explorer** (docs): Current network → Clusters → **system** → copy an API URL; System Chains → **directory_chain** → copy `brid`. (`production-deploy.md` §1.)

### Reserved names auto-fill (CLI 0.29.8+)

Using deployment names **`mainnet`** or **`testnet`** in `chromia.yml` lets CLI fill Directory `brid` + system-node `url`. A custom name requires `brid` and `url` by hand. (`production-deploy.md` §1; `study-deploy.md` §1.3 / §2.2.)

CHANGELOG **0.29.8** (2026-01-16): "dynamically load provider urls instead of hardcoding them"; "Remove `url` and `brid` requirement in chromia.yml for deploymentActionCommands". (`study-deploy.md` CLI notes + extract at 0.29.8.) Skill: "Reserved names `mainnet` / `testnet` auto-fill Directory `brid` + `url` (CLI 0.29.8+)."

Also **0.28.0**: `--mainnet` / `--testnet` as alternatives to `--api-url`; when only one blockchain is defined, config is inferred from `deployments.<network>`. (`study-deploy.md`.)

### Published system-node URLs

**Testnet** (deploy + client guides): `https://node0.testnet.chromia.com:7740` … `node1` … `node2` … `node3` (same host, ports `:7740`). Hello World status path omits `:7740`: `https://node0.testnet.chromia.com/tx/{directory_chain_brid}/{tx_rid}/status`. (`production-deploy.md` §1.)

**Mainnet** (client guide; "for the most recent and complete list, visit the Chromia Explorer"): `https://system.chromaway.com`, `https://chromia.validatrium.club`, `https://chromia-mainnet-systemnode-1.stakin-nodes.com`, `https://chroma.node.monster:7741`, `https://dapps0.chromaway.com`, `https://chromia-mainnet.w3coins.io:7740`, `https://mainnet-dapp1.sunube.net:7740`. (`production-deploy.md` §1.)

Clients: **pool of system-node URLs** + **dapp Blockchain RID**. `postchain-client` queries Directory Chain to discover current dapp nodes. Do **not** ship admin/container privkeys in frontend. (`production-deploy.md` §2.8.)

**Economy Chain RID is not published as a static hex.** Official: `chr query --blockchain-rid ${DIRECTORY_CHAIN_RID} get_economy_chain_rid`. (`production-deploy.md` §1; "Not published".)

---

## 4. Directory-chain `api_version` 110; semver `1.<api>.<patch>`

- `src/version.rell` on `dev`: `query api_version(): integer = 110`. (`code-cli-directory.md` header + §6.)
- README: semver is `1.<api>.<patch>`. Latest tag **1.110.10**.
- `src/nm_api/nm_api.rell`: `nm_api_version() = 27`.
- `src/cm_api/module.rell`: `cm_api_version() = 3`.
- `src/features.rell`: `direct_cluster()` / `direct_container()` extendable, default `false`.
- `CLAUDE.md` says `src/version.rell` is "currently v97" and Rell 0.15.3 — **stale**. Source `version.rell` = 110; CLI 0.33.0 ships Rell 0.16.0. (`code-cli-directory.md` §6, §8.)

Generated Directory/Economy/Anchoring API: https://chromaway.gitlab.io/core/directory-chain/ — **wins** for query/op signatures. (`official-docs-map.md` §4.)

CLI uses generated package `com.chromia.directory1` (rell-maven-plugin; modules `common.queries,proposal_voter_set,proposal_blockchain,proposal_container`). CLI 0.33.1 depends on postchain-client **3.39.5**. (`code-cli-directory.md` §7.)

`DeployUpdateCommand` calls generated `cmGetBlockchainApiUrls` (unfiltered `cm_get_blockchain_api_urls`) then `require(urls.isNotEmpty())`. CM v3 also has `cm_get_active_blockchain_api_urls` (filters `state != REMOVED`). (`code-cli-directory.md` §6.4.)

---

## 5. `chromia.yml`

Schema page that wins: https://docs.chromia.com/build/configuration/project-config. Structure: `/build/configuration/project-structure`. (`official-docs-map.md` §4; `study-deploy.md` §2.)

### Module name, not file path

Entry in `blockchains.<name>.module` is a **module name** (`main`, `app`, `module_a`) — filename of a single-file module, or folder name of a directory module — **never** a file path. Do not write `module_a/module.rell`. (`study-deploy.md` §2.1; `chromia-rell-and-cli` SKILL; `rell-cli.md` §1.1.)

`blockchains` is required. **Chain names cannot contain hyphens** (CLI 0.20.14: "Updated Chromia model to disallow hyphens in blockchain names, ensuring compatibility with directory chain specification"). (`study-deploy.md` CLI 0.20.14; SKILL.)

Only the main module named in `blockchains.<name>.module` plus its import closure is active. (`chromia-rell-and-cli` SKILL.)

`moduleArgs` requires a matching `struct module_args` in that module; values are `chain_context.args` **only inside the module that declared `module_args`**. (`study-deploy.md` §2.2; SKILL.)

### `compile.rellVersion`

`compile` keys on project-config example: `rellVersion`, `source`, `target`, `deprecatedError`, `quiet`, `strictGtvConversion`. Example value shown: `rellVersion 0.14.9`. `strictGtvConversion` default **true**; leave true in production. Only available from Rell 0.13.9 (before that, behaved as false). (`study-deploy.md` §2.2.)

CLI 0.20.9: default Rell compile version 0.13.14 (then aligning with mainnet); "Custom target versions can be specified in `chromia.yml` under `compile:rellVersion`." (`study-deploy.md` CLI 0.20.9.)

Skill example: `compile.rellVersion: "0.16.0"` — **must not exceed the target cluster**. Confirm live cluster / CLI bundle before citing a number. CLI 0.33.0 bundles Rell 0.16.0; Rell docs list 0.16.4 / GitLab tags through 0.16.7 (`chromia-rell-and-cli` SKILL). Project-config example **0.14.9** is an illustration, not current mainnet.

Other compile keys on that page: `source: src`, `target: build`. `compile:target` overrides the timestamped snapshot dir under `build/`. (`production-deploy.md` §2.5.)

### `libs` pin shape (two kinds; project-config)

**Library-chain (recommended):**

```yaml
libs:
  <library_id>:          # e.g. com.chromia.ft4
    version: <version>   # required
    registry: mainnet    # mainnet (default) | testnet | localhost | custom URL
    brid: x"..."         # only for custom registry URL
```

**External Git:**

```yaml
libs:
  <library_name>:
    registry: <git url>
    path: <path inside repo>
    tagOrBranch: <value>
    rid: x"<GTV hash of library Rell files>"
    insecure: false      # true skips rid check; not recommended for production
```

(`study-deploy.md` §2.2; `chromia-rell-and-cli` SKILL git example uses `ft4` / `https://gitlab.com/chromaway/ft4-lib.git` / `path: rell/src/lib/ft4` / `tagOrBranch: v1.1.0r`.)

Library-chain installs default to **mainnet** unless registry is testnet. `libs.*.insecure: true` is **not recommended for production**. (`production-deploy.md` §4, §8.)

Rell import uses the **simple name** (`import ft4;`), not the full library id (`com.chromia.ft4`). (`study-deploy.md` library guide extract.)

### `deployments:`

| Field | Meaning |
|---|---|
| `deployments.<name>` | Target id. `mainnet` / `testnet` auto-fill Directory `brid` + `url` (0.29.8+). |
| `brid` | **Directory Chain** RID of the target network (byte array / hex). |
| `url` | One system-node URL or a list. |
| `container` | Container ID from Vault / PMC lease. |
| `chains.<blockchain>` | **Dapp** Blockchain RID. Key must match a `blockchains:` name. Omit on first create; required for update. CLI 0.30.0+ writes this back. |

Lost dapp RID: Explorer or `chr deployment info`. (`production-deploy.md` §4.)

### Secrets / env

Do **not** put DB passwords or private keys in committed YAML. Env overrides: `CHR_DB_URL`, `CHR_DB_USER`, `CHR_DB_PASSWORD`, `CHR_DB_SCHEMA`. Substitution: `${MY_VAR:-default}`. (`study-deploy.md` §2.2; SKILL.)

Unverified (cookbook only, **not** on project-config): `database.schema_version`, `test.timeout`, `test.parallel`, `build.output_dir`, `build.optimize`. Do not rely on them. (`chromia-rell-and-cli` SKILL.)

YAML anchors must live under `definitions:`. `!include other.yml` and `!include other.yml#tag` are supported. (`study-deploy.md` §2.2.)

---

## 6. Deploy / update / hosting / PMC / keys / containers / SCUs

### Two layers (`production-deploy.md` §2)

1. **Lease a container** on a dapp cluster (Economy Chain payment → Directory Chain allocation via ICMF).
2. **Deploy / update the Rell blockchain** into that container (`chr deployment create` / `update`, signed by the container’s deployer key).

`pmc` is the **provider/management CLI** (different binary, different tree). Dapp developers typically lease via Vault and deploy via `chr`. (`official-docs-map.md` trap: PMC is not Chromia CLI.)

### Keys

`chr keygen --key-id="testnet_container_key"` (or a mainnet-specific id). Writes under `~/.chromia/`:

- `{key-id}` — private key
- `{key-id}.pubkey` — public key
- `{key-id}_mnemonic` — recovery phrase

Default key id if omitted: **`chromia_key`**. Re-running `chr keygen` without `--key-id` can overwrite it. (`production-deploy.md` §2.2; `study-deploy.md` §1.2.)

Point CLI via `~/.chromia/config`: `key.id = testnet_container_key`. Or `--key-id`, `--secret`, `--config`.

**CLI key precedence:** `--secret` > `--key-id` > `--config` `key.id` > project `.chromia/config` > `~/.chromia/config`. Keys themselves always live under `~/.chromia/` (or `--file`). (`production-deploy.md` §7.)

Key classes (do not flatten): wallet/EVM (Vault, lease pay, `chr tx --evm-auth`); container/deployment (`chr keygen`); provider (Directory/Economy, PMC); node (EBFT); FT4 admin pubkey; Filehub admin. Deployer key ≠ CHR wallet. (`production-deploy.md` §7; SKILL.)

Docs: keep the private key secret; never share it. Provider key "not possible to recover". Revoked provider keys cannot be re-added.

### Tokens + lease

- **Testnet tCHR:** 1000 tCHR every 7 days. Faucet named "Chromia Testnet Faucet" (docs do not print a host). Live host recorded: `https://faucet.testnet.chromia.com/` (200). `https://faucet.chromia.com/` does **not** resolve. Also `pmc economy claim-test-chr` (Testnet only). (`production-deploy.md` §1–2; "Checked missing".)
- BSC Testnet tCHR (bridge testing): BscScan Write Contract `0x0e61dfdbd5b979adbf3e5b5fe7ee40f85b6daa8d`, token `0x8e59d72e4dda56f26963c6b8c77ca1959e9a74f0`, weekly `claim`, tBNB gas. (`study-deploy.md` §1.1; SKILL.)
- **Mainnet:** deposit **at least 10 CHR** from BSC or Ethereum via Vault `https://vault.chromia.com/en/deposit`, **or** send **at least 20 CHR** from another Chromia chain (10 CHR account-creation fee + 10 remaining). Mainnet get-container page does **not** restate the 20 CHR path. (`production-deploy.md` §2.1; `study-deploy.md` §1.3.)

Vault lease: Testnet `https://vault.testnet.chromia.com/en/containers/`; Mainnet `https://vault.chromia.com/en/containers/`. Paste **public** key from `.chromia/<key-id>.pubkey`. Result is **Container ID** (hex; docs example `15ddfcb25dcb43577ab311fe78aedab14fda25757c72a787420454728fb80304`). Failed Mainnet lease → refund of the same amount. Anyone can pay a lease. Overdue: **suspended for six months**, then **permanently deleted**. (`production-deploy.md` §2.3, §3.)

### Hosting / one SCU (official hosting page)

- 2 GB RAM
- 0.5 vCPU
- 16 GB storage
- I/O 25 MiB/s read, 20 MiB/s write

Lease **weekly**, pay in **CHR**, priced to a USD-equivalent target. **Published cost target:** "approximately **90 USD**" weekly for a default **7-node dapp cluster**. Docs do **not** publish a fixed CHR-per-SCU number. Vault / `pmc lease create-container` compute CHR at lease time. (`production-deploy.md` §3; "Not published".)

Default `pmc container add` limits: `--container-units 1`, `--max-blockchains 10`, `--extra-storage 0` MiB. Default `pmc economy add-cluster` CU sizes match one SCU: `--cu-cpu 50` (0.5 CPU), `--cu-ram 2048` MiB, `--cu-storage 16384` MiB, `--cu-io-read 25`, `--cu-io-write 20`. Default `--system-container-units 4`. (`production-deploy.md` §3.)

### PMC lease / container (operator path)

`pmc lease` → Economy Chain: `create-container` (`--cluster-name`, `--scus`, `--duration` weeks, optional `--extraStorage` GiB, `--extraComputeRequests`, `--subnode-image-name`, `--subnode-jar-extension-names`, `--auto-renew`, `--account-id` / `--evm-address`); `upgrade-container`; `list` / `info`; `list-pending-tickets`; `remove-container` (**no refund**); `assign-subnode-image` / `add-subnode-jar-extensions`.

Provider-side Directory proposals: `pmc container add|info|limits|list|remove|pause|resume|configuration|subnode-image|subnode-jar-extension`. Adding a container binds a **voter set** that may deploy into it. (`production-deploy.md` §2.4.)

DC source resource-limit types: `container_units`, `max_blockchains`, `extra_storage`, `extra_compute_requests`. Mainnet `container_removal_timeout: 31536000000` (comment: 1 year). `max_dapp_providers_per_lease: 50`. Hidden `chr deployment remove-container` posts FT4-auth `removeContainerOperation` on economy chain, no refund. (`code-cli-directory.md` §6.2.)

### First deploy / update

```bash
chr deployment create --settings chromia.yml --network testnet --blockchain hello
# also: --key-id, --secret, --config, --no-compression, -y
chr deployment update --settings chromia.yml --network testnet --blockchain hello
```

RID is computed from **dapp configuration and code** and **must be unique**. If create fails, change the code (unique comment / whitespace is enough in Hello World). Testnet: reachable in **about five minutes**. (`production-deploy.md` §2.5.)

Update flags on live page: `--height`, `--verify-only`, `--skip-verification`. Live page body does **not** mention entity/enum schema comparison, DROP warnings, or the interactive confirm — those exist only in source (`DeployUpdateCommand.validateSchema` + `SchemaComparator` + `ReportGenerator`). (`code-cli-directory.md` §2.4, §5.)

`--skip-verification` skips **both** node `validateConfiguration` and schema compare. `--verify-only` still runs schema compare (unsafe prompt skipped) then exits 0 with "Verification only, skipping sending updates".

Schema compare (0.30.0+): entities/objects + **enums**. Dangerous enum: value REMOVED, ordinal changed, or ADDED with ordinal `<= maxOldOrdinal`. Appending at the end is **not** dangerous. Whole-enum ADDED/REMOVED: `isDangerous = false` in source (CHANGELOG wording is about **value** removals/reorders). Attribute removal warning (0.31.0): "The database column will be DROPPED on next initialization. All data will be permanently deleted." (`code-cli-directory.md` §5.)

Rell entity page (what deploy guides link): **safe** — add attributes with defaults; add on empty tables; remove attributes; change mutability on non-`@log`. **Breaking** — change attribute type; add/remove `@log`. No documented in-place type-migration tool. (`production-deploy.md` §5; SKILL.)

Optional delay: `blockchains.<name>.config.directory_chain.config_delay` (ms; 24h example `86400000`). Query: `list_delayed_blockchain_configs`. (`production-deploy.md` §4.)

`features.merkle_hash_version` default **2**; v1 deprecated (hash-collision bugs). Dapp `blockstrategy.maxblocksize` max 26 MiB (`27262976`). `gtx.max_transaction_size` default 25 MiB. (`study-deploy.md` §2.3.)

### Lifecycle after first deploy (`production-deploy.md` §2.6)

| Command | Effect |
|---|---|
| `chr deployment update` | New config on the **same** Blockchain RID (requires `chains:`) |
| `chr deployment info` | `--verbose` for nodes; `--mainnet` / `--testnet` |
| `chr deployment inspect` | Live queries / operations / entities / objects / module_args |
| `chr deployment pause` / `resume` | Pause / resume the blockchain |
| `chr deployment remove` | **Permanent** (`-y`) |
| `chr deployment proposal *` | vote / list / info / revoke / rename (`retract-vote` since 0.31.0) |
| `chr deployment voterset *` | info / update / list / `add-dapp-provider` |
| `chr deployment container pause\|resume\|configuration` | Container pause/resume; propose `slow-db-statement-log-ms` |

### Multi-owner (`production-deploy.md` §2.7; `study-deploy.md` §1.3)

1. Lease. 2. `chr deployment voterset add-dapp-provider`. 3. `chr deployment voterset update -vs <name> --add-member <pk1>,<pk2>` — docs also show `--add-members` (plural) on the multi-deployment page; command-reference is **`--add-member`** (singular). Both strings appear in official docs. 4. Threshold: `0` = supermajority `n - (n-1)/3` (~67%); `-1` = simple majority; positive = that many voters. 5. Updates become proposals: `chr deployment proposal vote --id <id> --accept` or `pmc proposal vote`. Docs also show `pmc blockchain update` as initiator.

Lease ownership transfer is Economy Chain ops, **not** `chr deployment`: `offer_container_lease_ownership_transfer` / `accept_container_lease_ownership_transfer_offer` / `remove_container_lease_ownership_transfer_offer` via `chr tx --evm-auth`. (`production-deploy.md` §2.7.)

### Provider roles / staking (docs)

| Role | Permitted | Stake per node (reviewed; may change) |
|---|---|---|
| Dapp Provider (DP) | Deploy dapps; add replica nodes (default) | (not in the stake table) |
| Node Provider (NP) | Add block-builder nodes | 300,000 CHR; ≥10% = 30,000 self-stake native Mainnet CHR |
| System Provider (SP) | Govern system chains; some ops direct | 600,000 CHR; ≥10% = 60,000 self-stake |

NP can enable/disable a DP **without voting** (DC source comment). Promoting NP → SP needs super-majority (≥ ⅔). (`production-deploy.md` §3; `code-cli-directory.md` §6.1.)

### Frontend on-chain

`webStatic` GTX module. Next.js `output: "export"`. Served at `{node}/web_query/<blockchainRid>/web_static`. Page: `/build/deployment/deploy-frontend-dapp`. `/build/deployment/frontend` is **404**. Only **localhost** URL is documented for hosted `web_static`. (`study-deploy.md` §1.5, §9; SKILL.)

### Observability

Explorer https://explorer.chromia.com/ (defaults `/mainnet`) is the documented public surface. Providers: log4j2.yml; Prometheus `:9190/metrics`. No public hosted Grafana URL for dapp developers. (`production-deploy.md` §6.)

---

## 7. Docs path traps (section roots 404)

Pattern: **section roots 404**. Deep pages 200. (`official-docs-map.md` §1, §3; `chromia-official-docs` SKILL.)

| URL | Status | Use instead |
|---|---|---|
| `/get-started`, `/get-started/` | 404 | `/get-started/installation` |
| `/build`, `/build/` | 404 | a child, e.g. `/build/cli/introduction` |
| `/ecosystem`, `/ecosystem/` | 404 | `/ecosystem/providers/overview` |
| `/rell`, `/rell/` | 404 | `/rell/rell-intro` |
| `/reference`, `/reference/` | 404 | `/reference/ft4/` or `/reference/terminology` |
| `/cli`, `/cli/` | 404 | `/build/cli/introduction` or `/cli/introduction` (alias 200) |
| `/build/cli`, `/build/cli/` | 404 | `/build/cli/introduction` or `/build/cli/commands/` |
| `/ft4`, `/build/ft4` (and trailing `/`) | 404 | `/build/ft4/intro` |
| `/pages/rell/`, `/pages/rell-syslib/` | 404 | `/rell/language-features/systemlib/` (no generated Rell API site) |
| `/cli/project-config` | 404 | `/build/configuration/project-config` |
| `/build/deployment/frontend` | 404 | `/build/deployment/deploy-frontend-dapp` |
| `/sitemap-0.xml`, `/sitemap-1.xml`, `/robots.txt` | 404 | `/sitemap.xml` (384 URLs, one file) |
| `gitlab.com/chromaway/core-tools/chromia-cli/-/blob/master/CHANGELOG.md` | 404 | `…/blob/dev/CHANGELOG.md` (no `master`) |

Working aliases (200, not in sitemap — prefer sitemap path): `/cli/introduction`, `/cli/commands/<cmd>` → `/build/cli/…`; `/intro/about/architecture/` → `/get-started/about/architecture/`. Do not assume every `/intro/…` or `/cli/…` child aliases.

Other traps (`official-docs-map.md` §3):

- Trailing slash on **leaf** pages is fine (301/200). Bare **section directory** is 404, not a redirect to the first child.
- Generated FT4 Dokka paths contain a literal space: `-f-t4 -library/lib.ft4.…`. Copy from `/pages/ft4-rell/`.
- `chr` command pages lag new flags. New verbs appear first in GitLab CHANGELOG / `/build/cli/cli-release-notes` (and that page itself lags at 0.30.0).
- PMC (`/ecosystem/providers/pmc/`) ≠ `chr`.
- EIF appears in three places (CLI `chr eif`, governance extension, Bridge product). "How do I bridge CHR/ERC-20?" → `/ecosystem/bridge/overview`, not `chr eif`.
- `/get-started/about/protocols/ft4` is a protocol overview. Implementation is `/build/ft4/`.
- GitLab **short names 403**: use `gitlab.com/chromaway/core-tools/chromia-cli`, `…/core/directory-chain`, `…/core/postchain`, `…/core/postchain-client`, `gitlab.com/chromaway/rell`, `gitlab.com/chromaway/ft4-lib`. Top-level `/chromia-cli`, `/directory-chain`, `/postchain-client`, `/ft4/ft4-lib` are sign-in.

Open-first table: Rell → `/rell/rell-intro`; chromia.yml → `/build/configuration/project-config`; `chr` → `/build/cli/commands/`; deploy → `/build/deployment/testnet/getting-started` or `/mainnet/getting-started`; Directory queries → `https://chromaway.gitlab.io/core/directory-chain/`. (`official-docs-map.md` §5.)

---

## 8. Docs vs source contradictions — source wins

Order from `official-docs-map.md` §4 + `chromia-official-docs` SKILL: GitLab CHANGELOG / source for "does this flag exist in version X?"; command page for invocation; project-config for YAML schema; `chr --help` on the installed binary beats both. Directory queries: GitLab Pages + `directory-chain` repo over narrative.

| Claim | Where (stale) | Reality |
|---|---|---|
| `deployment create` does not write `chromia.yml`; you must paste `chains:` | Live `/build/cli/commands/deployment` create section; in-repo `docs/Functional.md` ("The CLI does not auto-update the configuration file."); deploy guides | Source since **0.30.0**: `DeployCreateCommand.afterDeployment` → `ChromiaYmlWriter.updateDeploymentNodes`. Manual YAML is **fallback on write failure**. (`code-cli-directory.md` §2.3, §4, §8) |
| CLI latest is 0.30.0 | Live `/build/cli/cli-release-notes` headings; in-repo CHANGELOG header still points at legacy `/cli/cli-release-notes` | Tags/CHANGELOG through **0.33.1**; tag **0.33.2** empty. `docs/Deployment.md`: `update-docs` job creates a PR on private `chromia-docs` that must be merged. |
| DC API v97 / Rell 0.15.3 | directory-chain `CLAUDE.md` | `version.rell` = **110**; CLI 0.33.0 ships Rell **0.16.0** |
| Mainnet allowed GTX includes `ZKPGTXModule` | `doc/directory_chain/Mainnet-Blockchain-Configuration-Limits.md` | `chromia-mainnet.yml` list has **`EthereumAuthGTXModule`**, not ZKP. `WebAuthnGTXModuleFactory` is commented out. Trust the YAML for what this `dev` tree would ship. (`code-cli-directory.md` §6.5, §8) |
| Schema compare is entities only | `Functional.md` update flow | Source also compares **enums** (0.29.8 / expanded 0.30.0). Live update page omits the whole schema-diff UI. |
| `CheckCommand` / `FormatCommand` / `LintCommand` / `InstallCommand` are user-facing top-level | `Functional.md` | User-facing: `chr code {lint,format,check}` and `chr install` → `chr library install` |
| Testnet getting-started step 2 says "Specify your network (**mainnet**)" | Testnet getting-started | Rest of Testnet pages use `--network testnet`. Typo. (`study-deploy.md` "Docs typo") |
| `--add-members` vs `--add-member` | multi-deployment page uses `--add-members`; command-reference `--add-member` | Both strings appear in official docs. Command-reference / source class name is the safer invoke form until `--help` on the binary is checked. (`study-deploy.md` §1.3) |
| Mainnet connect-client `pcl.creatClient` | Mainnet connect-client page | Testnet page uses `pcl.createClient`. Published Mainnet spelling is `creatClient` (one `e`). Do not "correct" the published page; the working API name is `createClient`. (`study-deploy.md` §1.3) |
| Docs allowed GTX on blockchain-properties: Rell, StandardOps, ICMF, ICCF, EIF, WebStatic | `/build/configuration/blockchain-properties` | Mainnet YAML **also** allows `EthereumAuthGTXModule`. Doc table incomplete vs `chromia-mainnet.yml`. |

**Which page wins (strict):** FT4 structs → `/pages/ft4-rell/`; CLI existence → GitLab CHANGELOG `dev`; CLI invoke → `/build/cli/commands/<cmd>`; YAML keys → `/build/configuration/project-config`; Directory ops → `chromaway.gitlab.io/core/directory-chain`; deploy how-to → `/build/deployment/{testnet,mainnet}/…` (first-dapp shortcut loses to the full guide). (`official-docs-map.md` §4.)

---

## Silent / do not invent

From the crawled trees (`production-deploy.md` "Not published"; `study-deploy.md` §9; `code-cli-directory.md` notes):

- Fixed CHR price per SCU (only ~90 USD/week target for a default 7-node cluster).
- Economy Chain / Filehub / Filechain / Token Chain production BRIDs as static hex (query `get_economy_chain_rid`; placeholders only).
- Chromia Testnet Faucet hostname in docs (page names it; `faucet.testnet.chromia.com` is live but not printed).
- Public hosted Grafana/metrics URL for dapp developers.
- In-place Rell attribute-type migration procedure.
- Hosted (non-localhost) `web_static` frontend URL.
- What `chromia start` is (named only on database getting-started; not in the CLI command list).
- Writer implementation of `ChromiaYmlWriter` (lives in chromia-cli-tools; not fetched).
- Cookbook-only YAML keys listed above.

---

## Canonical URLs (all 200 on 2026-08-26 unless marked)

- CLI catalog: https://docs.chromia.com/build/cli/commands/
- CLI deploy verbs: https://docs.chromia.com/build/cli/commands/deployment
- CLI notes (lag 0.30.0): https://docs.chromia.com/build/cli/cli-release-notes
- CLI source CHANGELOG: https://gitlab.com/chromaway/core-tools/chromia-cli/-/blob/dev/CHANGELOG.md
- `chromia.yml`: https://docs.chromia.com/build/configuration/project-config
- Testnet: https://docs.chromia.com/build/deployment/testnet/getting-started
- Mainnet: https://docs.chromia.com/build/deployment/mainnet/getting-started
- Directory API: https://chromaway.gitlab.io/core/directory-chain/
- Explorer: https://explorer.chromia.com/
