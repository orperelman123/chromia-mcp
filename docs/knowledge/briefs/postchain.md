# Postchain ready-brief

Official only. Compiled 2026-08-26 from:
- `/workspace/chromia-knowledge/code-postchain.md` (Postchain `dev`, project `32294340`)
- `/workspace/chromia-knowledge/postchain-postgres.md`
- `/home/box/agent-data/workflows/chromia-production-deploy/SKILL.md`
- Cross-checked: Postchain `Node-Configuration-Properties.md`, `Blockchain-Configuration-Properties.md`, `postchain-restapi.yaml` v`"22"`, Directory Chain `proposal_blockchain` / `chromia-mainnet.yml` / `config_test.rell`.

If a file is silent, this brief says so. Source wins over docs.

---

## 1. One JDBC write transaction per block; SAVEPOINT per GTX tx

**Code, not docs.** `postchain-postgres.md` §5.4 / §11.3 called this Unclear. `code-postchain.md` §4 settles it.

| Step | What | Class / file |
|---|---|---|
| Write pool | `defaultAutoCommit = false` | `StorageBuilder.kt` |
| Block connection reused | `currentEContext` across begin / append / finalize | `BaseBlockchainEngine.withDBConnection` |
| Per GTX tx | `Storage.withSavepoint`; fail → `rollback(savepoint)` only | `BaseStorage.withSavepoint`, `BaseManagedBlockBuilder.maybeAppendTransaction` |
| Block abort | `rollback(blockBuilderSavepoint)` named `blockBuilder${nanoTime}` | `BaseManagedBlockBuilder.rollback` / `makeBlockBuilder` |
| Consensus commit | `closeWriteConnection(eContext, true)` → `connection.commit()` | `BaseManagedBlockBuilder.commit`, `BaseStorage.closeWriteConnection` |
| Witness | `UPDATE` `block_witness` on the same JDBC tx | `SQLDatabaseAccess.commitBlock` |
| Serialized DB ops | single worker: `buildBlock` / `loadProposedBlock` / `commitBlock` / `addBlock` | `BaseBlockDatabase` |

Postgres: `isSavepointSupported() = true`. If savepoints unsupported: warn “Unclear if Postchain will work under these conditions.”

**Silent:** write-pool isolation (not set in `StorageBuilder`; do **not** claim REPEATABLE READ). Read pool **is** `TRANSACTION_REPEATABLE_READ` + `defaultReadOnly = true`. Whether Rell entity tables share the JDBC tx is not named; they use `ctx.conn` if they go through `TxEContext`.

GTX atomicity is ops-level (`transaction.kt`: any `op.apply` false → `UserMistake`), nested **inside** the block JDBC tx via SAVEPOINT.

---

## 2. Node REST that exists in source

Source of truth: `postchain-base/src/main/resources/restapi-docs/postchain-restapi.yaml` **version `"22"`**. `REST-API.md`: JSON or binary GTV (`application/octet-stream`); config GET is XML or GTV; 4xx/5xx body has `error` and sometimes `code`. Default `api.port` **7740**. Body/data caps **55 MiB**. REST must expose **RID**, not `iid`.

Do **not** treat `sources/chromia-mcp/.../openapi_spec` (version `"17"`) as current.

### Transaction

| Method | Path | Notes from OpenAPI |
|---|---|---|
| `POST` | `/tx/{blockchainRid}` | hex JSON `{tx}` or binary GTV. **“read replicas do not support this endpoint”** → 403. 409 already in queue. 503 queue full. 400 parse / incorrect / unknown op (`OPERATION_NOT_FOUND`). |
| `GET` | `/tx/{blockchainRid}/waiting` | waiting RIDs. Replicas 403. |
| `GET` | `/tx/{blockchainRid}/waiting/{txRid}` | binary + `X-Transaction-Timestamp`. Replicas 403. |
| `GET` | `/tx/{blockchainRid}/rejected` | RID + reason + timestamp. Replicas 403. |
| `GET` | `/tx/{blockchainRid}/{txRid}` | confirmed tx |
| `GET` | `/tx/{blockchainRid}/{txRid}/status` | `unknown` \| `waiting` \| `confirmed` \| `rejected`. “stores up to 1000 rejected … per chain in memory.” Replicas **not supported**; use next row. |
| `GET` | `/tx/{blockchainRid}/{txRid}/confirmationProof` | |
| `GET` | `/transactions/{blockchainRid}/{txRid}` | `?tx-data=` `?decode-tx=` — replica-safe status substitute |
| `GET` | `/transactions/{blockchainRid}` | list; schema max **600**, default 25; **prose says “up to 500”** |
| `GET` | `/transactions/{blockchainRid}/count` | |

Admission after POST: `BaseTransactionQueue.enqueue` (`BaseTransactionQueue.kt`) — duplicate RID, `checkCorrectness` on **shared read**, optional `gtx_api.priority_check_v2` else `_v1`, cap `txqueuecapacity` **2500**, `MAX_REJECTED = 1000`. `NetworkAwareTxQueue` (`NetworkAwareTxEnqueuer.kt`) on `OK` broadcasts raw tx to **all other signer peers**. Node prop `forwarding_replica` default `false` (independent of the 403).

### Blocks / queries / node / config

| Method | Path |
|---|---|
| `GET` | `/blocks/{blockchainRid}` list max **100**, default 25 |
| `GET` | `/blocks/{blockchainRid}/{blockRid}` |
| `GET` | `/blocks/{blockchainRid}/height/{height}` |
| `GET` | `/blocks/{blockchainRid}/confirm/{blockRid}` |
| `GET` | `/query/{blockchainRid}?type=` — “development, testing, and simple web applications”; production: `/query_gtv` |
| `POST` | `/query/{blockchainRid}` — **deprecated** |
| `GET` | `/dquery/{blockchainRid}` — **deprecated**; use `/web_query` |
| `GET` | `/web_query/{blockchainRid}/{type}` (+ path → `path: list<text>`, query → `query_params`) |
| `GET`/`POST` | `/query_gtv/{blockchainRid}` — POST may return `X-Query-Response-Signature` (Merkle hash **V2** of `query_response_signature_data`) |
| `POST` | `/query_async/{blockchainRid}` → `GET /query_async/{blockchainRid}/{queryRid}` |
| `GET` | `/node/{blockchainRid}/my_status` — `WaitBlock` \| `HaveBlock` \| `Prepared` |
| `GET` | `/node/{blockchainRid}/statuses` |
| `GET` | `/brid/iid_{chainIid}` — Directory Chain is `iid_0`; **plain text** RID |
| `GET` | `/blockchain/{blockchainRid}/height` (`?container=` during migration) |
| `GET` | `/blockchain/{blockchainRid}/nodestate` — **experimental** (`RUNNING_VALIDATOR`, `RUNNING_READ_ONLY`, …) |
| `GET`/`POST` | `/config/{blockchainRid}` — GET XML/GTV; POST validates only (managed mode needs `X-Postchain-Signature`) |
| `GET` | `/config/{blockchainRid}/next_height` |
| `GET` | `/config/{blockchainRid}/features` — “Currently, only the merkle hash version” |
| `GET` | `/errors/{blockchainRid}` — last 5 errors |
| `GET` | `/metadata/{blockchainRid}` — ops + queries |
| `GET` | `/version` and `/version/{blockchainRid}` |
| `GET` | `/infrastructure_version` and `/infrastructure_version/{blockchainRid}` |
| `GET` | `/highest_block_height_anchoring_check/{blockchainRid}` — `cac` / `sac` / `evm` |

Debug API is a **separate** port (`debug.port` default **7750**). Admin gRPC is `container.admin-rpc-port` **50051**. Neither is in this OpenAPI.

---

## 3. `merkle_hash_version` must be 2 — where enforced

**Postchain engine does not require 2.** `Blockchain-Configuration-Properties.md` `features.merkle_hash_version`: type `int`, **default 1**. Column on `"cN.configurations"`. Unknown feature flags rejected (old nodes cannot run new-feature chains). `GET /config/{brid}/features` returns it.

**Directory Chain proposal gate requires ≥ 2** (not the JVM engine):

| Where | What |
|---|---|
| `proposal_blockchain` moduleArgs | `require_min_merkle_hash_version` (required integer). `DC-configuration-and-setup.md` |
| `chromia-mainnet.yml` / `chromia-testnet.yml` | `require_min_merkle_hash_version: 2` and `require_min_merkle_hash_version_ignore_existing_chains: true` (“For now…”) |
| `config_test.rell` `test_feature_merkle_hash_version_validations` | new chain with v1 → **“Merkle hash version must be at least 2”**; downgrade 2→1 → **“Merkle hash version can't be downgraded from version 2 to 1”**; existing-chain v1 updates allowed when ignore flag is on |
| DC `calculate_configuration_hash` (`blockchain.rell`) | missing feature → hash version **1** (`get_or_default("merkle_hash_version", (1).to_gtv())`) |
| Mainnet YAML `required_blockchain_features` | **`[]`** — feature is *allowed*, min version is the real gate |
| DC-setup example `required_blockchain_features` | lists `"merkle_hash_version"` — **example ≠ mainnet YAML** |

Chromia CLI / `rell-cli.md` / SKILL say “default 2” / “must be 2” / “v1 deprecated (hash collisions)”. That is **network + docs**, not the engine default.

---

## 4. Postgres 16+, URL/schema, tables, collation, query timeout

| Fact | Source | Silent / contradict |
|---|---|---|
| Driver **only** `org.postgresql.Driver` → `PostgreSQLDatabaseAccess` | `DatabaseAccessFactory.kt` | other drivers throw |
| `database.url` / `database.schema` / user / password — empty, **must be set** | `Node-Configuration-Properties.md`. Env: `POSTCHAIN_DB_URL`, `POSTCHAIN_DB_SCHEMA`, `POSTCHAIN_DB_USERNAME`, `POSTCHAIN_DB_PASSWORD` | |
| One JDBC URL + one `search_path` schema **per process** | `StorageBuilder.kt` `SET search_path TO ${schema}` | **not** one Postgres database per blockchain RID |
| Per-chain tables: quoted `"c${chainId}.$table"` in that schema (e.g. `"cN.blocks"`). Housekeeping `LIKE 'c${chainId}.%'` | `SQLDatabaseAccess.tableName` | SKILL shorthand `"c<iid>.<mount>"` is the same pattern |
| Node-global (unprefixed): `meta`, `containers`, `blockchains`, `peerinfos`, `blockchain_replicas`, `must_sync_until`, `snapshot_sync_state`, `snapshot_sync_context_state` | `initializeApp` | |
| App schema version **`DB_VERSION = 13`**; upgrade only; no downgrade | `StorageBuilder.kt` | |
| Wipe = `DROP SCHEMA IF EXISTS $schema CASCADE` | `StorageBuilder.kt` | `chr node start --wipe` |
| Collation check: `'A'<'a'`, `'Ї'<'ї'`, `upper('ї')='Ї'`, `lower('Ї')='ї'`. Error text: initialize with **`LC_COLLATE = 'C.UTF-8' LC_CTYPE = 'C.UTF-8' ENCODING 'UTF-8'`** | `PostgreSQLDatabaseAccess.checkCollation` | CLI Functional.md says **`en_US.UTF-8`**. Engine text is C.UTF-8. `database.suppressCollationCheck` only warns |
| Postgres **16+** | Chromia CLI Functional.md + SKILL (local). OpenAPI example `database-server-version: "16.7"` | **Postchain QuickGuide does not state a Postgres major.** Engine-silent. |
| `query_timeout_seconds` | **blockchain** config, default **60** (doc: “1 min”); 0/neg disables | `Blockchain-Configuration-Properties.md` |
| Local CLI default JDBC | `jdbc:postgresql://localhost:5432/postchain`; user/pass `postchain`; tests `<schema>_tests` | CLI Functional.md |
| Two pools | blockBuilder R/W 10/8 wait 100 ms; shared R/W 10/2 wait 10_000 ms | `Node-Configuration-Properties.md` |
| `binaryTransfer=false` | pgjdbc 42.5.1 workaround | `StorageBuilder.kt` |

---

## 5. EBFT, clusters, containers, anchoring (as stated)

### EBFT (`EBFT-Overview.md`, `EBFT-Overview_v2.md`)

- PBFT bound: **3f+1** for f faults. Proof-of-authority / consortium (QuickGuide).
- States: **WaitBlock → HaveBlock → Prepared** (2f others HaveBlock same RID) → fetch commit sigs (must be recorded in DB) → commit.
- Primary = `(height + round) % n`. Revolt: 2f revolt flags increment round. Timeout `exponential_delay_initial * exponential_delay_power_base ^ round`, cap `exponential_delay_max` (defaults 1000, 1.2, 600_000). `revolt.timeout` default 10_000.
- v2: HaveBlock status already carries the block signature → usually skip extra sig fetches.
- Process: `ValidatorBlockchainProcess` = `BaseStatusManager` + `BaseBlockManager` + `BaseBlockDatabase` + `ValidatorSyncManager`. `isSigner()` is `!syncManager.isInFastSync()`.
- Signers **always** fast-sync; replicas fast-sync then **slow-sync** (10-block `GetBlockRange`). `NetworkAwareTxQueue` wraps the engine queue.
- Official perf: keep build time **well below half the revolt timeout** (`Tweaking-postchain-performance.md`).

### Production hierarchy (Chromia docs + Directory Chain, not Postchain-only)

```
Providers → Nodes (Postchain) → Clusters (system | dapp) → Containers (leased) → Blockchain(s)
```

- **System cluster:** Directory, Economy, SAC, Token, Transaction Submitter, … ; system nodes **replicate all cluster anchoring chains**.
- **Dapp cluster:** own **Cluster Anchoring Chain (CAC)** + **Directory Chain replica**.
- CM API: cluster name, anchoring-chain RID, peers, signers at height, API URLs.
- Containers: dedicated vCPU/RAM/storage; Economy → Directory via ICMF create/upgrade/stop/restart/remove. NM API: `container_units`, `max_blockchains`, `extra_storage`. Implementation: **master launches Docker subnodes** (`container.master-port` 9860, REST 7740, debug 7750, admin 50051). `api.subnode-http-redirect` → 307.
- Blockchain states in engine enum (`BlockchainState.kt`): `RUNNING`, `PAUSED`, `REMOVED`, `IMPORTING`, `ARCHIVED`, `UNARCHIVING`. NM API list in `postchain-postgres.md` omits **`ARCHIVED`**.
- Official role name is **signer** (`signers`), not “validator.”

### Anchoring (docs + OpenAPI + node props)

```
chain headers → CAC (per cluster) → SAC → Ethereum (Transaction Submitter Chain + EVM contract)
```

- SAC-anchored history wins on conflict. System cluster CAC anchors system chains **except** SAC.
- Node can poll: `anchoring-check.cluster-anchor-check-interval-ms` / `system-…` default 3_600_000; **EVM check default off** (`evm-anchor-check-interval-ms` **-1**). REST: `/highest_block_height_anchoring_check/{brid}`.
- FT4 `buildAndSendWithAnchoring` waits CAC **and** SAC. Numeric SCU sizes: **silent in these three files**; SKILL states 2 GB / 0.5 vCPU / 16 GB (not re-verified here).

---

## 6. Blockchain config vs node config — property sources

Two different documents. Do not mix keys.

| | Node | Blockchain |
|---|---|---|
| Doc | `doc/configuration/Node-Configuration-Properties.md` | `doc/configuration/Blockchain-Configuration-Properties.md` |
| Loaded from | properties file and/or `POSTCHAIN_*` env | chain config GTV/XML in DB; `chromia.yml` `blockchains.<name>.config`; Directory Chain proposals |
| Provider | `configuration.provider.node` = `properties` \| `manual` \| `managed` (default `properties`). Chromia mainnet/testnet = **managed** vs Directory `nm_api` | Height-scoped `BlockchainConfiguration` (`Postchain-Configuration.md`): hard fork at 1000 → C1 for 0..999, C2 from 1000 |
| Identity / P2P / DB / REST / containers / sync / anchoring-check | node | — |
| `signers`, `gtx.*`, `blockstrategy.*`, `revolt.*`, `features.*`, `query_timeout_seconds`, `txqueuecapacity`, `sync` / `sync_ext` | — | blockchain |
| Factory | — | `configurationfactory` **required** (typically `GTXBlockchainConfigurationFactory`) |

Node groups (names only, from the node doc): `database.*`, `messaging.*`, `api.*`, `debug.port`, `container.*`, `fastsync.*`, `slowsync.*`, `snapshotsync.*`, `anchoring-check.*`, `housekeeping.*`, `rate-limit.blocks`, `forwarding_replica`, `node.<i>.{pubkey,host,port}`, `initial-peer.*`, `infrastructure` default `base/ebft`.

Blockchain groups: `signers` (required), `blockstrategy`, `gtx`, `revolt`, `snapshot`, `features`.

Directory Chain **additionally** caps proposed dapp configs (`proposal_blockchain` / `Mainnet-Blockchain-Configuration-Limits.md`): `max_config_size` 5 MiB, `maxblocksize` ≤ 26 MiB, `mininterblockinterval` ≥ **1000 ms**, `allowed_dapp_chain_gtx_modules`, `allowed_blockchain_features` = `merkle_hash_version`.

Chromia public node-config page is a **subset** and stale in places (API default 443 vs engine 7740; older single `database.readConcurrency`). **GitLab Node-Configuration-Properties.md wins.**

---

## 7. Docs vs source (source wins)

1. **SQL tx granularity.** Docs silent / brief Unclear. Code: one write JDBC tx per block + SAVEPOINT per GTX tx.
2. **One DB per chain.** Never stated. Code: one schema + `"cN.*"` tables. Housekeeping deletes **tables** of removed chains.
3. **Wiki vs README.** Wiki home: “pre-release ‘alpha’ … production use is NOT recommended.” Current `dev` `README.md` does **not** repeat that.
4. **Collation.** CLI Functional.md: `en_US.UTF-8`. Engine error text: **`C.UTF-8` / `C.UTF-8` / `UTF-8`**.
5. **`GTXTransactionFactory` constructor default 1 MiB** vs `GtxConfigurationData` / config doc **25 MiB**. Production path passes `gtxConfig.maxTxSize`.
6. **`features.snapshot_enabled`:** docs/briefs sometimes boolean; config table **int, default 0**. Enable **only at genesis**.
7. **`features.merkle_hash_version`:** Chromia CLI / SKILL “default 2” / “must be 2.” Engine default **1**. Min-2 is Directory `require_min_merkle_hash_version`.
8. **Blockchain states.** NM API list omits engine `ARCHIVED`.
9. **Replica POST /tx.** Brief: works if `forwarding_replica=true`. OpenAPI: replicas **403** on POST /tx, waiting, rejected, status.
10. **Write isolation.** Do not claim REPEATABLE READ on writes. Only the read pool sets it.
11. **Java.** QuickGuide: **17**. Chromia CLI: **21+**. Different products.
12. **REST port.** Engine **7740**. Chromia node-config page: 443.
13. **`/transactions` limit.** Schema max 600; OpenAPI prose “up to 500.”
14. **`tx_confirmation_time` (`standard.kt`)** SQL `FROM blocks` **without** `"cN."` prefix. Flag; do not invent a view.
15. **Mainnet GTX allow-list.** `Mainnet-Blockchain-Configuration-Limits.md` lists `ZKPGTXModule`. `chromia-mainnet.yml` does **not**; it **does** list `EthereumAuthGTXModule`. **YAML wins** for this tree.
16. **`mininterblockinterval`.** Engine default **25 ms**. Mainnet proposal min **1000 ms**.
17. **`container.min-space-quota-buffer-mb`.** Engine default **300**. Chromia node-config page lists 100.
18. **`required_blockchain_features`.** DC-setup example includes `merkle_hash_version`; mainnet YAML is `[]`.
19. **Postgres 16+.** CLI/SKILL only. Engine QuickGuide silent.
20. **FT4 `priority_check`.** FT4 docs name `priority_check` / `priority_check_v1`. Engine prefers `gtx_api.priority_check_v2` then `_v1`. Non-`UserMistake` exceptions from prioritizer are **ignored** (tx still enqueued).

---

## 8. Production footguns

- **Not one DB per chain.** One `database.url` + `database.schema` per **process**. Many chains → many `"c<iid>.*"` tables. `housekeeping.interval_ms` (default 30 s) **drops tables** of `REMOVED` chains.
- **Subnodes, not a shared pgdata.** Production dapps: master Docker-launches subnodes. `container.bind-pgdata-volume` default **true** — persist **that** volume. `container.postgres_max_locks_per_transaction` default **1024**. Quota: `container.min-space-quota-buffer-mb` **300** → container **read-only**. Idle subnode close: `container.idle-timeout-ms` 5 min. Filesystem LOCAL / ZFS / EXT4.
- **Managed mode.** Mainnet/testnet nodes poll Directory (`nm_compute_blockchain_info_list`, `nm_get_blockchains_for_container`, `nm_get_blockchain_configuration`, …). Manual properties will not pick up DC deploys.
- **POST /tx on a replica is 403** unless the node is a signer (OpenAPI). `forwarding_replica` is a separate flag, default false.
- **Broadcast all signers** on enqueue (`NetworkAwareTxEnqueuer.kt` comments: “DoS attacks becomes easy”; “we could possibly limit broadcasting to 2f nodes?” — not implemented).
- **Collation C.UTF-8** or the process fails (unless `suppressCollationCheck`).
- **Set `features.merkle_hash_version: 2`** on new dapps. Engine default is 1; DC will reject v1 proposals (“must be at least 2”). Cannot downgrade.
- **`features.snapshot_enabled` only at genesis**; all modules must be `SnapshotAware`.
- **Unknown `features` keys rejected** — old Postchain cannot run new-feature chains.
- **Mainnet caps tighter than engine defaults** (`mininterblockinterval` 1000 vs 25; config ≤ 5 MiB; allowed GTX modules only).
- **Build time ≪ ½ revolt timeout** or honest primaries revolt.
- **No official `pg_dump` / PITR runbook** in these sources. Documented: bind-pgdata, export/import (`ImporterExporter`; managed skips config export), snapshot sync, Directory preserves code/config.
- **Rell SQL names not in Postchain.** Factory is `net.postchain.rell.module.RellPostchainModuleFactory`. Do not invent table names.
- **Keys are not interchangeable:** node `messaging.privkey`, container/deployer key, FT4/Economy keys. SKILL: never ship admin/container keys in frontend.
- **Windows native PostgreSQL not recommended for production** (install docs).
- **FT4 admin module must not be in production dapps** (`postchain-postgres.md` §7.5).
- `chr node start --wipe` **destroys the schema**.

---

## Still silent (do not invent)

- Write-connection isolation level.
- Rell `entity` / `object` → SQL table/column names.
- Whether `standard.kt` unqualified `blocks` is a bug or a view.
- Numeric SCU sizes in the three assigned files (SKILL has numbers; not in Postchain repo).
- Provider `pg_dump` / WAL / PITR procedure.
- Full application ICMF send API (topics + `emit_event` are documented; cookbook is not in these files).

---

## Canonical URLs

- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Node-Configuration-Properties.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Blockchain-Configuration-Properties.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/postchain-base/src/main/resources/restapi-docs/postchain-restapi.yaml
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/ebft-protocol/EBFT-Overview.md
- https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/doc/directory_chain/DC-configuration-and-setup.md
- https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/doc/directory_chain/Mainnet-Blockchain-Configuration-Limits.md
