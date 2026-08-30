# Chromia CLI + Directory Chain — source study

**Written:** 2026-08-26 (IL / UTC+3)  
**Method:** GitLab REST API + `/-/raw/` only. No `git clone`.  
**Do not invent.** Every claim below is from a named file, tag, or live docs page.

| Repo | Project ID | Default branch | Latest tag (API) | Last activity (API) |
|---|---|---|---|---|
| [chromaway/core-tools/chromia-cli](https://gitlab.com/chromaway/core-tools/chromia-cli) | 39844192 | `dev` | **0.33.2** (`e65f948c`, 2026-07-20) | 2026-08-11 |
| [chromaway/core/directory-chain](https://gitlab.com/chromaway/core/directory-chain) | 46396847 | `dev` | **1.110.10** (`88a75d09`, 2026-07-31) | 2026-08-19 |
| [chromaway/core/postchain-client](https://gitlab.com/chromaway/core/postchain-client) (skim) | 46288950 | `dev` | not enumerated | 2026-07-22 |

CLI `CHANGELOG.md` on `dev` ends at **[0.33.1] - 2026-06-24**. Tag **0.33.2** GitLab release text is `"No changes."` (CI-only merge).  
Directory-chain `src/version.rell` on `dev`: `query api_version(): integer = 110`. Semver is `1.<api>.<patch>` per README.

---

## 1. Chromia CLI command catalog (source)

Registration is `chromia-cli/src/main/kotlin/com/chromia/main.kt`. Binary name is `chr`. Clikt default command name = kebab-case of class minus `Command` unless `name=` is set.

### 1.1 Top-level (registered in `main`)

| Command | Class | Help / notes |
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
| `fetch-config` | `FetchConfigCommand` | **Hidden** (`hiddenFromHelp = true`). Marked `$EXPERIMENTAL_COMMAND`. "Fetch blockchain configuration" |
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

`CheckCommand`, `FormatCommand`, `LintCommand` exist as classes but are **not** registered at top level. They are subcommands of `code`. `InstallCommand.kt` exists; the public entry is the `install` alias → `library install`.

### 1.2 `chr code`

`CodeCommand.commands()`:

| Command | Class | Help |
|---|---|---|
| `code lint` | `LintCommand` | Analyze Rell; `.rell_lint` |
| `code format` | `FormatCommand` | Format Rell; `.rell_format` |
| `code check` | `CheckCommand` | "Check Rell code for compilation errors" |

### 1.3 `chr deployment`

`DeploymentCommand.commands()` + aliases `pause-container` → `container pause`, `resume-container` → `container resume`.

| Command | Class | Notes |
|---|---|---|
| `deployment create` | `DeployCreateCommand` | `name = "create"` |
| `deployment info` | `DeployInfoCommand` | |
| `deployment inspect` | `DeployInspectCommand` | |
| `deployment update` | `DeployUpdateCommand` | |
| `deployment resume` | `DeployResumeCommand` | `DeployActionCommand(BlockchainAction.resume, …)`; command name = enum name `resume` |
| `deployment pause` | `DeployPauseCommand` | name = `pause` |
| `deployment remove` | `DeployRemoveCommand` | name = `remove`; `-y` confirm |
| `deployment lease-info` | `DeployInspectLeaseCommand` | **Hidden**, experimental |
| `deployment proposal` | `ProposalCommand` | |
| `deployment voterset` | `VotersetCommand` | |
| `deployment container` | `ContainerCommand` | |
| `deployment remove-container` | `RemoveContainerCommand` | **Hidden**. Economy-chain FT4 op; "Remove a container and its associated lease without refund" |

Proposal subcommands (`ProposalCommand.commands()`):

| Command | Class |
|---|---|
| `proposal vote` | `ProposalVoteCommand` |
| `proposal retract-vote` | `ProposalRetractVoteCommand` |
| `proposal list` | `ProposalListCommand` |
| `proposal info` | `ProposalInfoCommand` |
| `proposal revoke` | `ProposalRevokeCommand` |
| `proposal rename` | `ProposalRenameBlockchainCommand` |

Voterset (`VotersetCommand.commands()`): `info`, `update`, `list`, plus `VotersetAddDappProvider` (official docs page lists `voterset add-dapp-provider`; source class file is `VotersetAddDappProvider.kt`).

Container (`ContainerCommand.commands()`):

| Command | Class |
|---|---|
| `container configuration` | `ContainerConfigurationCommand` | Propose container config (`slow-db-statement-log-ms`) |
| `container pause` | `PauseContainerCommand` | |
| `container resume` | `ResumeContainerCommand` | |

### 1.4 `chr generate`

| Command | Class | `name=` |
|---|---|---|
| `generate client-stubs` | `GenerateClientStubsCommand` | `client-stubs` |
| `generate graph` | `GenerateMermaidGraphCommand` | `graph` |
| `generate docs-site` | `GenerateDocsSiteCommand` | `docs-site` |

### 1.5 `chr library`

`LibraryCommand.commands()` registers organization + developer groups (both `hiddenFromHelp = true`) plus library ops at the `library` level:

| Command | Class | `name=` |
|---|---|---|
| `library organization …` | `OrganizationCommand` | hidden group |
| `library developer …` | `DeveloperCommand` | hidden group |
| `library install` | `InstallLibraryCommand` | `install` |
| `library create` | `CreateLibraryCommand` | `create` |
| `library list` | `ListLibrariesCommand` | `list` |
| `library view` | `ViewLibraryCommand` | `view` |
| `library deploy` | `DeployNewLibraryVersionCommand` | `deploy` |
| `library versions` | `ListVersionsCommand` | `versions` |
| `library invite-user` | `InviteLibraryUserCommand` | `invite-user` |
| `library delete-dev` | `RemoveLibraryUserCommand` | `delete-dev` |
| `library update-user-permission` | `UpdateLibraryUserPermissionCommand` | |
| `library accept-invitation` | `AcceptLibraryInvitationCommand` | |
| `library list-invitations` | `ListLibraryInvitationsCommand` | |
| (also constructed) | `UpdateLibraryDescription` | file present in tree; not re-read here |

Organization (hidden): `create`, `list`, `remove-user`, `update-permission`, `view`, `accept-invitation`, `invite-user`.  
Developer (hidden): `create`, `get`, `update`.

### 1.6 Other groups

| Group | Subcommands |
|---|---|
| `node` | `start` (`StartCommand`), `update` (`UpdateCommand`) |
| `eif` | `generate-events-config` |
| `multi-signature` | `create`, `sign`, `send`, `view` |
| `seeder` | `init`, `generate` |
| `tools` | `gtv` (`GtvCommand`), `validate-config` (`ValidateModelCommand`), `lib-model` (`LibModelGenerateCommand`) |

---

## 2. Command catalog vs docs

Compared against:

- Live `https://docs.chromia.com/build/cli/commands/` (fetched 2026-08-26)
- Live `https://docs.chromia.com/build/cli/commands/deployment`
- Live `https://docs.chromia.com/build/cli/cli-release-notes` (page headings)
- In-repo `docs/Functional.md`, `docs/Deployment.md` on CLI `dev`

### 2.1 Official command index vs source

Docs index lists (in page order): `help`, `version`, `build`, `create-rell-dapp`, `deployment`, `eif`, `generate`, `keygen`, `multi-signature`, `node`, `query`, `repl`, `seeder`, `test`, `tools`, `tx`, `code`, `library`.

**Match:** every non-hidden top-level command is on the index.

**Source-only (not on the docs index):**

- `fetch-config` — hidden + experimental in source
- `install` as a top-level *name* — docs describe `chr library install`; source also aliases `chr install` → `library install`

Docs note (live page): first two letters of each command/subcommand work as a shortcut (`chr de cr` = `chr deployment create`). That is a Clikt feature, not restated in `main.kt`.

### 2.2 Official `deployment` page vs source

Live page documents: `create`, `info`, `inspect`, `update`, `pause`, `resume`, `remove`, `proposal` (`vote`, `retract-vote`, `list`, `info`, `revoke`, `rename`), `voterset` (`info`, `update`, `list`, `add-dapp-provider`), aliases `pause-container` / `resume-container`, `container` (`configuration`, `pause`, `resume`).

**On the live page, not hidden:** `proposal retract-vote` is documented (added in CLI 0.31.0). So the *command reference* page is newer than the *release-notes* page.

**Source-only / hidden, not on the live deployment page:**

- `deployment lease-info` (hidden, experimental)
- `deployment remove-container` (hidden; economy-chain FT4 `removeContainerOperation`)

### 2.3 Official `deployment create` text is stale vs source

Live docs (2026-08-26) still say:

> When deploying to a target for the first time, the command will show a prompt after a successful deployment with the chain configuration that you need to add to your project settings file.

Example shown is the manual YAML block under `deployments.<network>.chains:`.

**Source (`DeployCreateCommand.afterDeployment`) does write `chromia.yml`.** It calls `ChromiaYmlWriter.updateDeploymentNodes(settings.modelFile, deploymentsUpdate)` and prints a diff via `printChromiaYmlDiff`. The manual "Add the following…" block is only emitted if that write throws.

In-repo `docs/Functional.md` is also stale on this point (section "Deployment Configuration Updates (inferred)"): "The CLI does not auto-update the configuration file." That contradicts `DeployCreateCommand.kt` and CHANGELOG 0.30.0.

### 2.4 Official `deployment update` page vs schema/enum

Live usage lists `--height`, `--verify-only`, `--skip-verification`. The extracted page body does **not** mention entity/enum schema comparison, DROP warnings, or the interactive confirm for unsafe changes. Those exist only in source (`DeployUpdateCommand.validateSchema` + `SchemaComparator` + `ReportGenerator`).

### 2.5 Official CLI release notes vs CHANGELOG / tags

Live `/build/cli/cli-release-notes` **headings stop at Chromia CLI 0.30.0**.  
GitLab CHANGELOG on `dev` continues through **0.33.1**. Tags exist through **0.33.2**.

In-repo `CHANGELOG.md` header says changelogs are generated up to `https://docs.chromia.com/cli/cli-release-notes` (legacy `/cli/` path). `docs/Deployment.md` says the `update-docs` job creates a PR on private `chromia-docs` that must be merged; "When a new command is created, manual steps are required."

### 2.6 In-repo `docs/Functional.md` vs `main.kt`

`Functional.md` points at `CheckCommand.kt`, `FormatCommand.kt`, `LintCommand.kt`, `InstallCommand.kt` as if they were the user-facing commands. User-facing names are `chr code {lint,format,check}` and `chr install` → `chr library install`.

`Functional.md` "Deployment Update Flow" mentions entity/object/attribute comparison and "blocks dangerous changes until approved". It does **not** mention enums (added 0.29.8 / expanded 0.30.0).

---

## 3. CHANGELOG after 0.30.0 (tags through 0.33.x)

Source: `CHANGELOG.md` on `dev`, plus GitLab tag/release API.

### [0.30.0] - 2026-02-27 (baseline; included because later text refers to it)

- `chr deployment create` writes the deployment result back to `chromia.yml` and prints the changes.
- `chr build --skip-lib-check`.
- **Enum change detection** on `chr deployment update` (additions, removals, reorderings). Dangerous enum changes blocked until approved.
- Version bumps: rell 0.15.2, postchain 3.49.2, postchain-chromia 3.39.3, eif 0.32.0, chromia-cli-tools 0.10.0.

### [0.31.0] - 2026-03-04

- **Added** `chr deployment proposal retract-vote`.
- `chr test` writes SQL query statistics as an **HTML report file**. `--sql-log` **removed**.
- Warning on attribute removal during `chr deployment update`: database column **will be DROPPED** on next initialization; data permanently deleted.

### [0.31.1] - 2026-03-12 (tag)

GitLab release: "Captures all exceptions during rell version validation". CHANGELOG file on `dev` has **no 0.31.1 section**. The same fix is described under 0.32.0 in CHANGELOG.

### [0.32.0] - 2026-04-19

- Deployment commands handle all exception types during Rell version validation (crash on invalid GTV from node).
- Bumps: rell 0.15.3, chromia-cli-tools 0.11.3, rell-maven-plugin 1.2.1, postchain-client 3.39.1.
- Tag commit also: migrate to `ChromiaPredefinedNetworks` API (cli-tools 0.11.1+).

### [0.32.1] - 2026-05-06

- Fix `chr install` client init when there are no libraries to install (skip lib-chain connection).

### [0.33.0] - 2026-05-29

- rell **0.16.0**. Tag message: "Update Rell and refactor module and schema handling".

### [0.33.1] - 2026-06-24

- postchain 3.49.16, postchain-chromia 3.39.11, postchain-client 3.39.5, eif 0.32.6, chromia-cli-tools 0.12.1.

### [0.33.2] - 2026-07-20 (tag only)

- GitLab release: `"No changes."` Commit is CI speedup. **No CHANGELOG section.**

---

## 4. `deployment create` writing `chains:`

File: `DeployCreateCommand.kt`.

1. `preDeploymentVerification`: if `deployModel.chains` already contains the compiled chain name, abort (`PrintMessage`). Confirm with `YesNoPrompt` unless `-y`. Non-interactive without `-y` → `CliktError("Please specify -y option to force deployment")`.
2. `performDeploymentOperation` → `ChromiaDeploymentApi.create(...)`.
3. `afterDeployment` on successes with non-null `blockchainRid`:
   - Builds `DeploymentUpdate(networkTarget.network, chain.name, rid)`.
   - `ChromiaYmlWriter.updateDeploymentNodes(settings.modelFile, deploymentsUpdate) { diff -> terminal.printChromiaYmlDiff(...) }`.
   - Writer lives in **chromia-cli-tools** (`com.chromia.build.tools.model.writer`), not this repo. This study did not fetch that artifact.
4. On writer exception, prints a manual block:

```
deployments:
  <network>:
    chains:
      <name>: x"<hex rid>"
```

5. `explicitChainsToDeploy()` = `settings.model.blockchains.keys` (all local chains, not only those already in `deployments.*.chains`).

`deployment update` does **not** write `chromia.yml`. `explicitChainsToDeploy()` for update is `deployments[network].chains.keys`; empty → error "No chains found in deployment".

`deployment remove` (action) echoes that the user should clean the chain out of config; it does not rewrite the file.

---

## 5. Schema diff and enum checks

Used only on `chr deployment update` (`DeployUpdateCommand.validateSchema`), after `nodeClient.validateConfiguration(chain.config)` succeeds.

Pipeline:

1. `nodeClient.getConfiguration()` (deployed) vs compiled `chain.config`.
2. `BlockchainConfigSchemaParser.parse`: reads `gtx.rell.sources`, `gtx.rell.version`, `gtx.rell.modules` from the GTV config, writes sources to a temp dir, `RellApiCompile.compileApp`, then walks `RR_App` entities + enums.
3. `SchemaComparator.compareSchemas` → `SchemaComparison(entityDifferences, enumDifferences)`.
4. If both lists empty: print "No schema changes detected…".
5. Else `ReportGenerator.getSchemaChangesReport`. If `containsUnsafeChanges` and not `--verify-only`: interactive `YesNoPrompt`; non-interactive → `CliktError` telling the user to pass `--skip-verification`.

`--skip-verification` skips **both** node `validateConfiguration` and schema compare. `--verify-only` still runs schema compare (and the unsafe prompt is skipped because of `!verifyOnly`) then exits 0 with "Verification only, skipping sending updates".

### 5.1 What is compared (`schema.kt`, `SchemaComparator.kt`)

Entities (and objects via `isObject`): ADDED / REMOVED / MODIFIED. Field compare: `type`, `nullable`, `defaultValue`, `indexKind` (`UNIQUE` display `"key"`, `INDEX` display `"index"`).

Enums: ADDED / REMOVED / MODIFIED. Values compared by name; ordinal change → `MODIFIED`.

**Dangerous enum change** (`isEnumChangeDangerous`):

- Any value **REMOVED** (comment: "shifts ordinals").
- Any existing value **ordinal changed**.
- Any **ADDED** value whose new ordinal is `<= maxOldOrdinal` (inserted not at the end).

Appending a new value at the end is **not** marked dangerous.

`createEnumDifference` for whole-enum ADDED/REMOVED sets `isDangerous = false`. ReportGenerator does **not** treat whole-enum REMOVED as unsafe (`containsUnsafeChanges` stays false for that branch). CHANGELOG 0.30.0 says "Blocks deployment on dangerous enum changes (e.g., reordered or removed values) until approved" — that applies to **value** removals/reorders on an existing enum, not necessarily deleting the entire enum type (source does not flag whole-enum removal as unsafe).

### 5.2 Report text (`ReportGenerator.kt`)

Unsafe (`containsUnsafeChanges = true`):

- Entity/object **removed**: "corresponding table isn't physically dropped".
- Attribute **removed**: "The database column will be DROPPED on next initialization. All data will be permanently deleted." (wording from 0.31.0).
- Attribute type / nullable / default **changed**.
- Dangerous enum modifications (WARNING prefix; value added not at end; value removed; ordinal changed).

**Not** marked unsafe: adding entity/attribute; adding/removing/changing index kind; adding an enum; adding an enum value at the end.

When unsafe or `hasDangerousEnumChanges()`, the report appends:  
`https://docs.chromia.com/rell/language-features/modules/entity#changing-entity-definitions`

---

## 6. Directory chain

README: "implementation of the node management api for use when running postchain in managed mode. It also defines the cluster management api."

`module-docs.md`: Directory (providers, nodes, configs), Economy (leases, rewards, staking), Cluster Anchoring, System Anchoring.

`src/version.rell`: DC API version **110**.  
`src/nm_api/nm_api.rell`: `nm_api_version() = 27`.  
`src/cm_api/module.rell`: `cm_api_version() = 3`.  
`src/features.rell`: `direct_cluster()` / `direct_container()` extendable, default `false`.

Latest tag **1.110.10** (2026-07-31). Notable release notes from tags 1.110.x: blockchain-move redesign (keep src until `configuration_updated`); `container_removal_timeout` mainnet 1 year; `chromia-pvn.yml` privnet; `SignerUpdateGTXModule` on pvn management chain; index on `resource_usage_statistics`; `get_blockchains_replicated_by_node` also filters container state; drop obsolete `G_change_dapp_providers_state` topic from DC docs.

`CLAUDE.md` says `src/version.rell` is "currently v97" and Rell 0.15.3 — **stale vs `version.rell` = 110**.

### 6.1 Providers

Tiers used in source: `NODE_PROVIDER`, `DAPP_PROVIDER`. System access is a role (`roles.has_system_access`), not a separate tier. `propose_provider_state` comment: "Permission: SP > [SP, NP, DP], NP > DP". NP can enable/disable a DP **without voting**. SP/NP changes for other cases go through `proposal_type.provider_state` on `SYSTEM_P`.

`common/provider.rell`:

- `register_and_enable_provider`: creates `provider`, `provider_rl_state` from `provider_quota.max_actions_per_day`, optional `cluster_provider` / `voter_set_member`.
- `provider_rate_limit` / `recover_provider_rate_limit`: recover `max_actions_per_day` over 24h.
- Quotas: `max_nodes`, `max_containers` (non-system containers).
- `disable_provider`: `active=false`, deactivate nodes, delete cluster/replica/blockchain-replica memberships, remove from voter sets, re-evaluate pending proposals.
- `enable_provider`: if system access, ensure `SYSTEM_P` membership.

`proposal_provider/`: `proposal_provider_state.rell`, `proposal_provider_quota.rell`, `proposal_provider_is_system.rell`, `proposal_provider_batch.rell`.

Queries (`common/queries/common_queries_provider.rell`): `get_nodes_by_provider`, `get_provider_points`, `get_provider_clusters`, `get_provider_data`, `get_all_providers`, `get_providers(tier, system, require_active)`, `get_provider_quotas`, `get_provider_auth_blockchain`, `get_provider_keys`, `get_provider_keys_and_threshold`, `get_provider_by_key`.

Mainnet `moduleArgs.common.provider_quota_max_actions_per_day`: **100**.  
`common.init.initial_provider`: `0319852651DB3ACA5D5DFDF71D8600566345FE1713BCBC7F2C1B6434B7A8C88C6F`.  
Genesis node pubkey `037434C8D4F2B7B7DE44E80486A814676DC3D898FD4488E10E1940B1C4C5837200`, host `system.chromaway.com:9870`, API `https://system.chromaway.com:7740`, territory `SE`.

ICMF: `L_provider_update`, `L_provider_auth_update` (`messaging/provider.rell`). Legacy v1 decode for both message shapes.

### 6.2 Containers

`common/container.rell`:

- Resource limit types used: `container_units`, `max_blockchains`, `extra_storage`, `extra_compute_requests`.
- `create_container_with_limits`: `container_units > 0`, extras `>= 0`, cluster quota check.
- `create_container_impl`: voter set `container_<name>_deployer`, threshold `-1 … deployers.size()`.
- `upgrade_container`: cannot set `max_blockchains` below current chain count; extra_storage cannot exceed cluster available.
- Cannot remove: system container; container that is source/destination of `moving_blockchain` or `unarchiving_blockchain`.
- System container created with `cluster.system_container_units` and `system_container_defaults.*`.

Queries: `get_container`, `get_container_data`, `get_container_blockchain`, `get_containers`.

`proposal_container/`: action, configuration, limits, subnode image, jar extension, remove.

`direct_container/container_op.rell`: present (gated by `features.direct_container()`, default false).

Mainnet economy `container_removal_timeout: 31536000000` (comment: 1 year).  
`max_dapp_providers_per_lease: 50`, `max_bridge_leases_per_container: 10`.  
`proposal_container.proposal_container_configuration.min_slow_db_statement_log_ms: 1000`.

CLI `deployment container configuration` proposes `ContainerConfigurationData` / `proposeContainerConfigurationOperation`.  
Hidden `deployment remove-container` posts FT4-auth `removeContainerOperation` on the economy chain (`getEconomyChainRid()`), no refund (help text).

### 6.3 NM API (v27)

Header comment in `nm_api/module.rell` versions 1–27. Live queries in `nm_api.rell` (not the removed ones listed in the version history):

| Query | Since (comment) |
|---|---|
| `nm_api_version` | 1 |
| `nm_get_peer_infos` | 1 |
| `nm_find_next_configuration_height` | 1 |
| `nm_get_blockchain_configuration` | 1 |
| `nm_compute_blockchain_list` | 1; recovered v19; deprecated in favor of `nm_compute_blockchain_info_list` |
| `nm_compute_blockchain_info_list` | 4 (RUNNING/PAUSED; later IMPORTING/UNARCHIVING) |
| `nm_get_blockchain_replica_node_map` | 4 |
| `nm_get_container_limits` | 3 — map: `container_units`, `max_blockchains`, `storage` (= unit storage + extra), `cpu`, `ram`, `io_read`, `io_write` |
| `nm_get_containers` | 3 — RUNNING containers on node's clusters |
| `nm_get_blockchains_for_container` | 3 |
| `nm_get_container_for_blockchain` | 3 — throws if REMOVED |
| `nm_get_blockchain_dependencies` | 3 |
| `nm_get_pending_blockchain_configuration` | 5 |
| `nm_get_pending_blockchain_configuration_by_hash` | 5 |
| `nm_get_faulty_blockchain_configuration` | 5 |
| `nm_get_blockchain_state` | 6 |
| `nm_get_management_chain` | 7 |
| `nm_get_blockchain_configuration_options` | 8 |
| `nm_get_blockchain_configuration_v5` | 5; deprecated v18 |
| `nm_get_blockchain_signers_in_latest_configuration` | 9 |
| `nm_find_next_inactive_blockchains` | 11 |
| `nm_get_blockchain_containers_for_node` | 14 — 1 or 2 names during move/unarchive |
| `nm_get_migrating_blockchain_node_info` | 15 — `final_height` (v16 rename) |
| `nm_get_blockchain_configuration_info` | 18 |
| `nm_is_blockchain_provider` | 19 |
| `nm_get_container_image` | 20 |
| `nm_find_previous_configuration_height` | 21 |
| `nm_get_container_image_or_default` | 22 |
| `nm_get_container_creation_time` | 23 |
| `nm_get_container_rate_limits` | 23 — currently only key `"ai_inference"`, period `millis_per_week` |
| `nm_get_all_containers` | 24 |
| `nm_get_historic_configuration_height` | 25 |
| `nm_get_container_configuration` | 26 — `slow_db_statement_log_ms` |
| `nm_get_container_jar_extensions` | 27 |
| `nm_get_jar_extension` | 27 — raw JAR bytes |

History comment: several queries removed in v17 (`nm_find_next_removed_blockchains`, `nm_get_unarchiving_blockchain_node_info`, `nm_get_peer_list_version`, `nm_compute_blockchain_list`, `nm_get_blockchain_configuration_v5`, `nm_get_container_for_blockchain_on_node`); v5 + list recovered later.

### 6.4 CM API (v3)

`cm_api/module.rell`:

| Query | Notes |
|---|---|
| `cm_api_version` | `= 3` |
| `cm_get_cluster_info` | name, anchoring_chain rid, peers `(pubkey, api_url)` |
| `cm_get_cluster_names` | `cluster.operational == true` |
| `cm_get_cluster_blockchains` | all `container_blockchain` rids in cluster (no state filter in this query) |
| `cm_get_peer_info` | signers at height; "doesn't take into account pending configurations" |
| `cm_get_blockchain_cluster` | DC self → `clusters.system` |
| `cm_get_blockchain_api_urls` | **no REMOVED filter** |
| `cm_get_cluster_anchoring_chains` | |
| `cm_get_system_anchoring_chain` | `byte_array?` |
| `cm_get_system_chains` | `blockchain.system` |
| `cm_get_removed_cluster_blockchains` | v2 |
| `cm_get_removed_cluster_anchoring_chains` | v2 |
| `cm_get_active_blockchain_api_urls` | **v3** — requires chain exists and `state != REMOVED` |

CLI `DeployUpdateCommand` calls generated `cmGetBlockchainApiUrls` (the unfiltered one) then `require(urls.isNotEmpty())`.

### 6.5 Mainnet config limits

Two sources. YAML comment says: "Please update doc/Mainnet-Blockchain-Configuration-Limits.md and notify docs team if you modify these."

**`chromia-mainnet.yml` `moduleArgs.proposal_blockchain.util` (authoritative for what mainnet *this tree* would deploy):**

| Key | Value |
|---|---|
| `max_config_path_depth` | 10 |
| `max_config_size` | 5242880 (5 MiB) |
| `max_block_size` | 27262976 (26 MiB) |
| `min_inter_block_interval` | 1000 |
| `min_fast_revolt_status_timeout` | 2000 |
| `allowed_dapp_chain_gtx_modules` | RellPostchainModuleFactory, StandardOpsGTXModule, IcmfSenderGTXModule, IcmfReceiverGTXModule, IccfGTXModule, EifGTXModule, WebStaticGTXModuleFactory, **EthereumAuthGTXModule** |
| `allowed_dapp_chain_sync_exts` | IcmfReceiverSynchronizationInfrastructureExtension, EifSynchronizationInfrastructureExtension |
| `allowed_blockchain_features` | `merkle_hash_version` |
| `required_blockchain_features` | `[]` |
| `require_min_merkle_hash_version` | 2 |
| `require_min_merkle_hash_version_ignore_existing_chains` | `true` (comment: "For now...") |
| `require_eif_snapshot_version` | 2 |
| `allowed_dapp_chain_native_functions` | `net.postchain.d1.BlockWitnessRellNative` |

`WebAuthnGTXModuleFactory` is **commented out** in the allowed GTX list.

**`doc/directory_chain/Mainnet-Blockchain-Configuration-Limits.md` differs:**

- Table column "Controlled by arg" uses `allowed_features` / `required_blockchain_features`; YAML key is `allowed_blockchain_features`.
- Doc "Default allowed GTX Modules" includes **`net.postchain.zkp.ZKPGTXModule`**. That class is **not** in the YAML list.
- Doc does **not** list `EthereumAuthGTXModule` (YAML does).
- Doc does **not** list `require_min_merkle_hash_version`, `require_min_merkle_hash_version_ignore_existing_chains`, or `allowed_dapp_chain_native_functions`.

Trust the YAML for what this `dev` tree would ship; the markdown table is incomplete/divergent.

Mainnet directory-chain node image in the same file: `chromia-server` / `chromia-subnode` tag **3.28.1** (digests in YAML). That is the configured node software version in this file, not necessarily what is running on live mainnet.

Mainnet DC `features.merkle_hash_version: 2`. `allow_blockchain_dependencies: false`.

### 6.6 ICMF topics

Constants in `src/messaging/*.rell` use `ICMF_TOPIC_GLOBAL_PREFIX` / `ICMF_TOPIC_LOCAL_PREFIX` from `lib.icmf.constants` (library, not re-fetched). YAML topic names use `G_` / `L_` prefixes.

**Declared in messaging sources:**

| Constant (rell) | Prefix | Topic suffix |
|---|---|---|
| `configuration_updated_topic` | GLOBAL | `configuration_updated` |
| `configuration_failed_topic` | GLOBAL | `configuration_failed` |
| `create_cluster_v2_topic` | GLOBAL | `create_cluster_v2` |
| `create_cluster_error_topic` | LOCAL | `create_cluster_error` |
| `create_container_topic` | GLOBAL | `create_container` |
| `stop_container_topic` | GLOBAL | `stop_container` |
| `restart_container_topic` | GLOBAL | `restart_container` |
| `upgrade_container_topic` | GLOBAL | `upgrade_container` |
| `remove_container_topic` | GLOBAL | `remove_container` |
| `assign_subnode_image_to_container_topic` | GLOBAL | `assign_subnode_image_to_container` |
| `add_subnode_jar_extensions_to_container_topic` | GLOBAL | `add_subnode_jar_extensions_to_container` |
| `register_dapp_provider_topic` | GLOBAL | `register_dapp_provider` |
| `ticket_container_result_topic` | LOCAL | `ticket_container_result` |
| `cluster_update_topic` | LOCAL | `cluster_update` |
| `cluster_node_update_topic` | LOCAL | `cluster_node_update` |
| `node_update_topic` | LOCAL | `node_update` |
| `subnode_image_update_topic` | LOCAL | `subnode_image_update` |
| `cluster_subnode_image_update_topic` | LOCAL | `cluster_subnode_image_update` |
| `subnode_jar_extension_update_topic` | LOCAL | `subnode_jar_extension_update` |
| `cluster_subnode_jar_extension_update_topic` | LOCAL | `cluster_subnode_jar_extension_update` |
| `provider_update_topic` | LOCAL | `provider_update` |
| `provider_auth_update_topic` | LOCAL | `provider_auth_update` |
| `container_blockchain_update_topic` | LOCAL | `container_blockchain_update` |
| `signer_list_update_topic` | LOCAL | `signer_list_update` |
| `blockchain_rid_topic` | LOCAL | `blockchain_rid_topic` |
| `bridge_mapping_topic` | LOCAL | `_bridge_mapping` (leading underscore in suffix) |
| `node_availability_report_topic` | GLOBAL | `node_availability_report` |
| `resource_usage_statistics_topic` | GLOBAL | `resource_usage_statistics` |
| `economy_chain_staking_state_update` | GLOBAL | `staking_state_update` |

**Mainnet DC (`chromia-mainnet.yml` blockchain `mainnet`) receives:**

- Anchoring: `G_configuration_updated`, `G_configuration_failed`
- Global from economy chain rid `x"15C0CA99BEE60A3B23829968771C50E491BD00D2E3AE448580CD48A8D71E7BBA"`: `G_create_cluster_v2`, `G_create_container`, `G_stop_container`, `G_restart_container`, `G_upgrade_container`, `G_remove_container`, `G_assign_subnode_image_to_container`, `G_add_subnode_jar_extensions_to_container`, `G_register_dapp_provider`

**Mainnet economy chain receives:**

- Anchoring: `G_node_availability_report`, `G_resource_usage_statistics`
- Directory-chain local: `L_create_cluster_error`, `L_ticket_container_result`, `L_cluster_update`, `L_provider_update`, `L_provider_auth_update`, `L_node_update`, `L_cluster_node_update`, `L_blockchain_rid_topic`, `L_subnode_image_update`, `L_cluster_subnode_image_update`, `L_subnode_jar_extension_update`, `L_cluster_subnode_jar_extension_update`
- Global: `G_evm_transaction_submitter_cost_topic` (from evm_transaction_submitter rid `44DD7379…`), `G_token_price_changed` (from price_oracle rid `1C80EC35…`)
- Local: `L_evm_block_events` (bc-rid `51857CBD…`)

Tag 1.110.10: "Remove obsolete `G_change_dapp_providers_state` topic from DC configuration docs". That topic is **not** in current `messaging/` sources.

`blockchain_auth_icmf.icmf_rate_limit_ms` is **commented out** on mainnet DC.

---

## 7. postchain-client (skim)

Repo README on `dev`: Kotlin client, **not** on Maven Central. Registries: chromia-parent `50818999`, postchain `32294340`, postchain-client `46288950`.

Artifacts named in README:

- `net.postchain.client:postchain-client` — `PostchainClientProviderImpl().createClient(PostchainClientConfig(bcRid, endpointPool, keyPairs))`. `query(name, gtv)`, `transactionBuilder()` → `addOperation` / `addNop` / `sign` / `post`. Statuses: WAITING, REJECTED, CONFIRMED, UNKNOWN.
- `net.postchain.client:chromia-client` — `StandardChromiaClient(url)` looks up system nodes; example `awaitAnchoredTx`.
- `net.postchain.client:ft4-client` — `addFtAuthenticationOp`, `addEvmAuthenticationOp` / `addEvmSignaturesOp`, auth-descriptor helpers, `getAssetBalance`.

CLI 0.33.1 depends on postchain-client **3.39.5**. CLI uses generated directory-chain client package `com.chromia.directory1` (rell-maven-plugin in `chromia-cli/pom.xml`, modules `common.queries,proposal_voter_set,proposal_blockchain,proposal_container`).

---

## 8. Stale / conflicting artifacts (do not paper over)

| Claim | Where | Reality in this study |
|---|---|---|
| `deployment create` does not write `chromia.yml` | Official `/build/cli/commands/deployment` create section; in-repo `docs/Functional.md` | Source since 0.30.0 writes via `ChromiaYmlWriter`; manual YAML is fallback on write failure |
| CLI release notes latest 0.30.0 | Live `/build/cli/cli-release-notes` headings | Tags/CHANGELOG through 0.33.1; tag 0.33.2 empty |
| DC API v97 / Rell 0.15.3 | `CLAUDE.md` | `version.rell` = 110; CLI 0.33.0 ships Rell 0.16.0 |
| Mainnet allowed GTX includes ZKP | `doc/…/Mainnet-Blockchain-Configuration-Limits.md` | YAML list has EthereumAuth, not ZKP |
| Schema compare is entities only | `Functional.md` update flow | Source also compares enums; 0.30.0 CHANGELOG |

---

## 9. Files read (box copies under `/workspace/chromia-knowledge/raw-cli` and `raw-dc`)

CLI: `CHANGELOG.md`, `README.md`, `main.kt`, `ChromiaCommand.kt`, all `*Command.kt` under `src/main/kotlin` that were listed in the recursive tree (335 tree entries; command sources downloaded), `DeployCreateCommand.kt`, `DeployUpdateCommand.kt`, `DeployActionCommand.kt`, `RemoveContainer.kt`, `schema/*.kt`, in-repo `docs/*.md`, live official command/deployment/release-notes HTML.

DC: `README.md`, `CLAUDE.md`, `module-docs.md`, `src/version.rell`, `src/features.rell`, `src/chain_version.rell`, `src/nm_api/*`, `src/cm_api/module.rell`, `src/common/{provider,container,module}.rell`, `src/common/queries/{module,common_queries_provider,common_queries_container}.rell`, `src/messaging/*`, `src/proposal_provider/proposal_provider_state.rell`, `chromia-mainnet.yml`, `doc/directory_chain/Mainnet-Blockchain-Configuration-Limits.md`.

postchain-client: `README.md` on `dev` only.
