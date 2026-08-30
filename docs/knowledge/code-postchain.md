# Postchain engine from official source (dev)

**Knowledge brief (official Postchain repo only)**  
Compiled: 2026-08-26  
Repo: https://gitlab.com/chromaway/core/postchain (project id `32294340`, default branch `dev`)  
Method: GitLab project API + raw files. No git clone. Nothing invented beyond what those files state.  
Compared against: `/workspace/chromia-knowledge/postchain-postgres.md`.

---

## 0. What was read

### Docs (quoted paths)

- `README.md`
- `doc/QuickGuide.md`
- `doc/configuration/Postchain-Configuration.md`
- `doc/configuration/Node-Configuration-Properties.md`
- `doc/configuration/Blockchain-Configuration-Properties.md`
- `doc/configuration/Tweaking-postchain-performance.md`
- `doc/ebft-protocol/EBFT-Overview.md`
- `doc/ebft-protocol/EBFT-Overview_v2.md`
- `doc/rest-api/REST-API.md`
- `doc/client-guidelines/Client-Guidelines.md`
- `doc/snapshots/snapshots.md`
- `doc/snapshots/snapshot-modules.md`
- `doc/synchronization/Fast-Synchronizer.md`
- `doc/synchronization/Slow-Synchronizer.md`
- `doc/synchronization/Snapshot-Synchronizer.md`
- `doc/synchronization/Peer-States.md`
- Wiki home (`/-/wikis/home`)

### Source (quoted paths)

Maven modules on `dev`: `postchain-spi`, `postchain-base`, `postchain-gtx-data`, `postchain-gtv`, `postchain-gtv-yaml`, `postchain-common`, `postchain-server`, `postchain-cli`, `postchain-server-cli`, `postchain-admin-service`, `postchain-admin-client-cli`, `postchain-devtools`, `postchain-dilithium`, `postchain-web-static`.

Key files:

- `postchain-base/src/main/kotlin/net/postchain/PostchainNode.kt`
- `postchain-base/src/main/kotlin/net/postchain/StorageBuilder.kt`
- `postchain-base/src/main/kotlin/net/postchain/StorageInitializer.kt`
- `postchain-base/src/main/kotlin/net/postchain/base/data/BaseStorage.kt`
- `postchain-base/src/main/kotlin/net/postchain/base/data/DatabaseAccessFactory.kt`
- `postchain-base/src/main/kotlin/net/postchain/base/data/SQLDatabaseAccess.kt`
- `postchain-base/src/main/kotlin/net/postchain/base/data/PostgreSQLDatabaseAccess.kt`
- `postchain-base/src/main/kotlin/net/postchain/base/data/BaseManagedBlockBuilder.kt`
- `postchain-base/src/main/kotlin/net/postchain/base/data/BaseTransactionQueue.kt`
- `postchain-base/src/main/kotlin/net/postchain/base/BaseBlockchainEngine.kt`
- `postchain-base/src/main/kotlin/net/postchain/base/BaseBlockchainInfrastructure.kt`
- `postchain-base/src/main/kotlin/net/postchain/base/BaseBlockBuildingStrategy.kt`
- `postchain-base/src/main/kotlin/net/postchain/base/BaseTransactionPrioritizer.kt`
- `postchain-base/src/main/kotlin/net/postchain/base/NetworkAwareTxEnqueuer.kt` (class `NetworkAwareTxQueue`)
- `postchain-base/src/main/kotlin/net/postchain/ebft/BaseBlockDatabase.kt`
- `postchain-base/src/main/kotlin/net/postchain/ebft/BaseBlockManager.kt`
- `postchain-base/src/main/kotlin/net/postchain/ebft/PersistOnlyBlockWriter.kt`
- `postchain-base/src/main/kotlin/net/postchain/ebft/worker/ValidatorBlockchainProcess.kt`
- `postchain-base/src/main/kotlin/net/postchain/gtx/transaction.kt`
- `postchain-base/src/main/kotlin/net/postchain/gtx/GTXBlockchainConfiguration.kt`
- `postchain-base/src/main/kotlin/net/postchain/gtx/GTXTransactionFactory.kt`
- `postchain-base/src/main/kotlin/net/postchain/gtx/CompositeGTXModule.kt`
- `postchain-base/src/main/kotlin/net/postchain/gtx/GTXSchemaManager.kt`
- `postchain-base/src/main/kotlin/net/postchain/gtx/standard.kt`
- `postchain-base/src/main/kotlin/net/postchain/gtx/GtxConfigurationData.kt`
- `postchain-base/src/main/resources/restapi-docs/postchain-restapi.yaml`
- `postchain-spi/src/main/kotlin/net/postchain/core/Engine.kt`
- `postchain-spi/src/main/kotlin/net/postchain/core/Storage.kt`
- `postchain-spi/src/main/kotlin/net/postchain/core/Tx.kt`
- `postchain-spi/src/main/kotlin/net/postchain/core/ExecutionContext.kt`
- `postchain-spi/src/main/kotlin/net/postchain/core/GlobalStorageInitializer.kt`
- `postchain-spi/src/main/kotlin/net/postchain/core/BlockchainState.kt`
- `postchain-spi/src/main/kotlin/net/postchain/core/block/BlocksMangment.kt`
- `postchain-spi/src/main/kotlin/net/postchain/base/AbstractBlockBuilder.kt`
- `postchain-spi/src/main/kotlin/net/postchain/base/data/BaseBlockBuilder.kt`
- `postchain-spi/src/main/kotlin/net/postchain/base/data/BaseBlockStore.kt`
- `postchain-spi/src/main/kotlin/net/postchain/base/data/PersistOnlyBlockBuilder.kt`
- `postchain-spi/src/main/kotlin/net/postchain/gtx/modules.kt`
- `postchain-spi/src/main/kotlin/net/postchain/gtx/operation.kt`
- `postchain-spi/src/main/kotlin/net/postchain/gtx/SnapshotAware.kt`
- `postchain-gtx-data/src/main/kotlin/net/postchain/gtx/Gtx.kt`

---

## 1. What the repo says Postchain is

`README.md`: “Postchain is a blockchain framework designed primarily for consortium databases.” License: commercial (ChromaWay) or GPL with linking exceptions (`LICENSE`). No production-status sentence in the current README.

Wiki home (older text, still published): “This is a pre-release ‘alpha’ version … production use is NOT recommended.” The current `dev` README does not repeat that sentence.

`doc/QuickGuide.md`:

- Consortium / permissioned / enterprise / federated / DLT. Blocks signed by a majority of consortium members. Called **proof-of-authority**.
- Modular: consensus, tx format, crypto can be customized independently.
- Differentiator: “all blockchain data is stored in an SQL database, transaction logic can be defined in terms of SQL code (particularly, stored procedures).” SQL is a **black box**: “not a database plugin … works with databases such as PostgreSQL as is, without any special configuration or modification.”
- Stack: Kotlin, Java, SQL; PostgreSQL; OS = anything with **Java 17** (tested Linux, Mac OS X); SECP256k1 signing (customizable); SHA256 hashing.
- “Custom blockchains should be programmed in Rell.”
- Components: **Core** (interfaces), **Base** (enterprise base classes), **GTX** (optional recommended tx format + composable modules), **API** (REST), **EBFT** (PBFT-derived, replaceable), client SDKs (JS, Kotlin, C#).

---

## 2. Engine architecture (as coded)

### 2.1 Object graph

`doc/configuration/Postchain-Configuration.md` + `postchain-spi/.../Engine.kt`:

1. `BlockchainConfiguration` — stateless factory for “what chain of blocks is valid and how the state is updated.” Height-scoped: hard fork at height 1000 means C1 for 0..999, C2 from 1000. Config data “will be kept in the database.”
2. `BlockchainEngine` — block-building only; “ignorant about other nodes.” Holds `blockBuilderStorage` and `sharedStorage`.
3. `BlockDatabase` — connects engine to consensus (`BaseBlockDatabase`).
4. `PostchainNode` — builds the object graph.

`PostchainNode.kt` startup order:

1. Build **two** `Storage` instances against the same `AppConfig` JDBC URL/schema: `"block builder"` and `"shared"` (different pool sizes / wait times).
2. `ServiceLoader.load(GlobalStorageInitializer)` on a **committed** shared write connection, “before any blockchain starts” (`GlobalStorageInitializer.kt`).
3. Collation check + `getDatabaseServerVersion`.
4. Infrastructure factory → `PostchainContext` → `BlockchainInfrastructure` → `BlockchainProcessManager`.

`BaseBlockchainInfrastructure.makeBlockchainEngine`:

- `BlockQueries` from `sharedStorage`.
- Optional prioritizer if the module exposes query `gtx_api.priority_check_v2` or, else, `gtx_api.priority_check_v1` (`BaseTransactionPrioritizer.kt`). V2 also sends `compound_ops`.
- `BaseTransactionQueue` on `sharedStorage`.
- `BlockBuildingStrategy` from configuration.
- Optional `BaseAsyncQueryQueue` if `async_query_queue_capacity > 0` and config is `GTXModuleAware`.
- Engine uses `blockBuilderStorage` for the write/build path.

EBFT process (`ValidatorBlockchainProcess.kt`):

- `BaseStatusManager` + `BaseBlockManager` + `BaseBlockDatabase` (single-thread executor).
- `ValidatorSyncManager` (fast/slow/snapshot sync + revolt).
- `PersistOnlyBlockWriter` (lazy; “unless we snapshot sync”).
- `NetworkAwareTxQueue` wraps the engine queue and broadcasts accepted txs to **all other signer peers**.
- `isSigner()` is `!syncManager.isInFastSync()` — while fast-syncing, a signer process is not treated as a signer.

Read-only processes exist as `ReadOnlyBlockchainProcess`, `ForceReadOnlyBlockchainProcess`, `HistoricBlockchainProcess`. `NODE_ID_READ_ONLY = -1` (`ExecutionContext.kt`).

`BlockchainState` enum (`BlockchainState.kt`): `RUNNING`, `PAUSED`, `REMOVED`, `IMPORTING`, `ARCHIVED`, `UNARCHIVING`.

### 2.2 SQL layout (stated in code)

`DatabaseAccessFactory.kt`: the **only** accepted driver is `org.postgresql.Driver` → `PostgreSQLDatabaseAccess`. Any other class throws.

`StorageBuilder.kt`:

- Current app schema version **`DB_VERSION = 13`**.
- `initializeApp` supports versions `1..13`; **downgrade is disallowed**; upgrade only if `allowUpgrade`.
- Connections: Apache DBCP2 `BasicDataSource`; `defaultAutoCommit = false`; `SET search_path TO ${schema}` (Postgres-specific). Comment: `DataSource.setDefaultSchema` was avoided because of a Windows/WSL docker bug (POS-129).
- Read pool: `TRANSACTION_REPEATABLE_READ`, `defaultReadOnly = true`.
- Write pool: `defaultAutoCommit = false`; **does not set isolation** in this file (Postgres JDBC default is not restated here).
- `binaryTransfer=false` — workaround for pgjdbc 42.5.1.
- `ApplicationName = "Postchain $connectionName"`.
- Wipe: `DROP SCHEMA IF EXISTS $schema CASCADE`.

`SQLDatabaseAccess.tableName`: per-chain tables are **quoted identifiers** `"c${chainId}.$table"` in the **current schema**, not a Postgres schema per chain. Housekeeping lists them with `table_name LIKE 'c${chainId}.%'`. Functions: `"c${chainId}.$function"`.

**Node-global tables** (no chain prefix), created in `initializeApp` / version steps:

| Table | Role (from create SQL / field names) |
|---|---|
| `meta` | keys `version`, `chain_iid` |
| `containers` | `container_iid`, `name` UNIQUE |
| `blockchains` | `chain_iid` PK, `blockchain_rid` UNIQUE (v7) |
| `peerinfos` | host, port, `pub_key` PK, timestamp |
| `blockchain_replicas` | `(blockchain_rid, node)` PK, FK to peerinfos |
| `must_sync_until` | `chain_iid` → `block_height` |
| `snapshot_sync_state` | per `chain_iid`: height, root_hash |
| `snapshot_sync_context_state` | per `(chain_iid, context_id)` offsets |

**Per-chain tables** (`initializeBlockchain`):

| Identifier | Columns (Postgres DDL in `PostgreSQLDatabaseAccess.kt`) |
|---|---|
| `"cN.blocks"` | `block_iid BIGSERIAL PK`, `block_height` UNIQUE, `block_rid` UNIQUE, `block_header_data`, `block_witness`, `timestamp` |
| `"cN.transactions"` | `tx_iid BIGSERIAL PK`, `tx_rid` UNIQUE, `tx_data`, `tx_hash`, `block_iid` FK, `tx_number` UNIQUE |
| `"cN.sys.transaction_signers"` | `signer`, `tx_iid` FK; index on signer |
| `"cN.configurations"` | `height` PK, `configuration_data`, `configuration_hash` UNIQUE, `merkle_hash_version` |
| `"cN.sys.faulty_configuration"` | `configuration_hash`, `report_height` |
| `"cN.sys.snapshot_contexts"` | `context_name` PK, `context_id` IDENTITY from 0 |
| `"cN.sys.snapshot_updated_datum"` | `(context_id, datum_id)` UNIQUE, `datum_hash`, `datum` |
| `"cN.gtx_module_version"` | `module_name` PK, `version` (created by `GTXSchemaManager`) |

Also created on demand: `"cN.${prefix}_event_leafs"`, `"cN.${prefix}_state_leafs"`, `"cN.${name}_pages"` (EIF/ICMF/snapshots). Snapshot module tables use prefix `sys.x.gtx_module` (`SnapshotAware.kt` constant `SNAPSHOT_TABLE_PREFIX`).

`REST-API.md`: several tables have integer PK `iid`; REST must expose **RID**, not `iid`, for containers, blockchains, blocks, transactions.

`housekeeping.interval_ms` (node config, default 30000): “delete DB tables of removed blockchains.” Implementation: `removeAllBlockchainSpecificTables` + `removeAllBlockchainSpecificFunctions`.

Collation (`PostgreSQLDatabaseAccess.checkCollation`): fails unless `'A'<'a'`, `'Ї'<'ї'`, `upper('ї')='Ї'`, `lower('Ї')='ї'`. Error text: “initialize Postgres with `LC_COLLATE = 'C.UTF-8' LC_CTYPE = 'C.UTF-8' ENCODING 'UTF-8'`.” `database.suppressCollationCheck` only logs a warning.

`setLocalLockTimeout` / `resetLocalLockTimeout`: `SET LOCAL lock_timeout` (used by timed queries).

### 2.3 GTX

`doc/QuickGuide.md` + OpenAPI `postchain-restapi.yaml` + `Gtx.kt`:

GTX is a signed GTV (DER ASN.1) array:

```
[ [ blockchainRid, [ [opName, [args...]], ... ], [signerPubKeys...] ], [signatures...] ]
```

- Tx RID = SHA-256 Merkle hash of the **body** (OpenAPI). Hash of full GTV is `myHash`.
- Signatures: ECDSA secp256k1; count must equal signers; no duplicate signers (`transaction.kt`).
- Factory (`GTXTransactionFactory.kt`): rejects wrong `blockchainRid`; rejects size > `maxTransactionSize`; rejects too many signatures. `decodeAndValidateTransaction` re-encodes and requires canonical GTV bytes.

`GTXModule` (`modules.kt`): `makeTransactor`, `getOperations`, `getQueries`, `query`, `initializeDB`, `makeBlockBuilderExtensions`, `getSpecialTxExtensions`. Optional `PostchainContextAware.initializeContext` after `initializeDB`. Optional `SnapshotAware`. Optional `OperationWrapper` (e.g. wrap `iccf_proof`).

`CompositeGTXModule`: composes modules; duplicate op/query names fail unless `gtx.allowoverrides`. If snapshots enabled, adds `RootSnapshotBlockBuilderExtension` and per-module snapshot page/leaf tables.

`StandardOpsGTXModule` (`standard.kt`): ops `nop`, `__nop`, `timeb`; queries `last_block_info`, `tx_confirmation_time`; plus `GTXAutoSpecialTxExtension`. `nop`/`timeb` are **compound** and **single-per-transaction**. `timeb` checks `ctx.timestamp` against `[from, until]`.

Special ops: name starts with `"__"` (`GTXOperation.isSpecial`). Special txs are appended by the engine, not the queue (`Tx.kt`, `BaseBlockchainEngine` rejects specials from the queue).

GTX apply (`transaction.kt`):

- `checkCorrectness`: signatures (parallel if not already checked while syncing) + each op’s `checkCorrectness`. Reject if no “normal” (non-compound) operation unless the tx is **pure special**.
- `apply`: check then call every `op.apply`; any `false` → `UserMistake`. That is the coded meaning of GTX atomicity (all ops or fail).
- Syncing uses `checkCorrectnessWhileSyncing` / `applyWhileSyncing` (less restrictive; skips “no normal op” spam rule).

`iccf_proof` is cited in `Tx.kt` as the example **compound** op that must be paired with a normal op.

---

## 3. Transaction lifecycle (client → Postgres commit)

### 3.1 Admission (REST)

OpenAPI `postchain-restapi.yaml` (version `"22"`):

- `POST /tx/{blockchainRid}` — hex JSON `{tx}` or binary GTV. **“read replicas do not support this endpoint”** → 403. 409 already in queue. 503 queue full. 400 parse/incorrect/unknown op.
- `GET /tx/{blockchainRid}/{txRid}/status` — `unknown` | `waiting` | `confirmed` | `rejected`. “The node stores up to 1000 rejected transaction statuses per chain in memory.” Read replicas: **not supported**; use `GET /transactions/{blockchainRid}/{txRid}?tx-data=false`.
- Other tx endpoints: `/tx/.../waiting`, `/waiting/{txRid}`, `/rejected`, `GET /tx/{rid}`, `/transactions/{rid}`, `/transactions` (limit default 25, max **600**; prose says “up to 500”), `/transactions/count`, `/tx/{rid}/confirmationProof`.
- Blocks: `/blocks/{brid}`, `/blocks/{brid}/{blockRid}`, `/blocks/{brid}/height/{height}`, `/blocks/{brid}/confirm/{blockRid}`. List limit max **100**, default 25.
- Queries: `GET /query/{brid}?type=...` — “intended for development, testing, and simple web applications. For more complex production use cases … `/query_gtv`.”
- `REST-API.md`: JSON or binary GTV (`application/octet-stream`); config endpoints XML or GTV; 4xx/5xx body has `error` and sometimes `code`; 404 vs empty-200 is inconsistent for missing subordinate resources.
- Node config: `api.port` default **7740**; `api.max-request-body-size` / `api.max-data-size` **55 MiB**; concurrency auto from CPUs and `database.sharedReadConcurrency`.

`BaseTransactionQueue.enqueue` (`BaseTransactionQueue.kt`):

1. Duplicate RID → `DUPLICATE`.
2. `tx.checkCorrectness` on a **shared read** connection.
3. Optional `prioritizer.prioritize` (`gtx_api.priority_check_v2` preferred, else `_v1`). `UserMistake` rejects; any other exception is **ignored** (tx still enqueued). If `txCostPoints > accountPoints` → `FULL`.
4. If queue size < `txqueuecapacity` (default 2500) → enqueue (priority `TreeSet`, higher first, then sequence).
5. If full: reject if priority ≤ lowest; else evict by account rate-limit or evict lowest priority (`UserMistake("Transaction evicted due to prioritization")`).
6. `MAX_REJECTED = 1000` in-memory reject map (matches OpenAPI).
7. Status: in queue or taken → `WAITING`; in rejects → `REJECTED`; else `UNKNOWN`. Confirmed is **not** a queue state; REST confirmed = `BlockQueries.isTransactionConfirmed`.

`NetworkAwareTxQueue` (REST path, `NetworkAwareTxEnqueuer.kt`): on `OK`, `sendPacket(Transaction(raw), signerPeers)` to **all other signers**. Comment in that file: enqueue locally **and** broadcast so txs are not dropped across primary changes; “DoS attacks becomes easy”; “High bandwidth requirement”; “we could possibly limit broadcasting to 2f nodes?” — not implemented.

Node property `forwarding_replica` (default `false`): “If blockchains running in replica mode should forward submitted transactions to signer nodes.” OpenAPI independently says posting to a read replica is 403.

### 3.2 Block building (primary)

`BaseBlockManager.update` + `BaseBlockBuildingStrategy`:

- Intent `BuildBlockIntent` only if `!mustWaitBeforeBuildBlock()` and (`shouldBuildPreemptiveBlock()` or `shouldBuildBlock()`).
- Preemptive: `preemptiveblockbuilding` (default true) **and** special-tx handler `shouldAffectBlockBuilding() == false` **and** queue non-empty.
- Stop reasons: `maxblocktransactions` (default 100), `maxblocksize` (26 MiB), `maxblocktime` (30 s since last block), `maxtxdelay` (1 s after first tx), `mininterblockinterval` (25 ms).
- Failed builds: exponential backoff `min(2^failedCount + minbackofftime, maxbackofftime)`.
- `Tweaking-postchain-performance.md`: keep build time **well below half the revolt timeout** (primary must build **and** others load before timeout).

`BaseBlockchainEngine.buildBlockInternal`:

- `begin(null)` → new block.
- Loop: `takeTransaction` (wait ≥ 20 ms or `mininterblockinterval`); skip/reject specials from queue; `maybeAppendTransaction`.
- `finalizeBlock()` then later EBFT collects signatures.

### 3.3 Apply (the per-tx work inside an open block)

`AbstractBlockBuilder.appendTransaction` + `BaseBlockBuilder` + `BaseBlockStore`:

1. `begin`: `store.beginBlock` → `insertBlock(height)` (row with height only) → `block_iid`. `bctx` = `BaseBlockEContext`. Optional **Begin** special tx if `buildingNewBlock`.
2. Per tx: size/count limits; special-tx position checks; `store.addTransaction` = `INSERT` tx (+ signers) on the **same** write connection; `checkCorrectness` / `apply` (or WhileSyncing). `txctx.done()`; emit events to registered `TxEventSink`s (`BaseBlockBuilder.processEmittedEvent` — missing sink is `ProgrammerMistake`).
3. `finalizeBlock`: optional **End** special tx; `makeBlockHeader` (Merkle of tx hashes, safe timestamp ≥ prev+1, extension extraData, optional `primary` pubkey); `store.finalizeBlock` = `UPDATE blocks SET block_rid, block_header_data, timestamp`.
4. `commit(witness)`: validate witness; `store.commitBlock` = `UPDATE blocks SET block_witness`; `bctx.blockWasCommitted()`.

`maybeAppendTransaction` (`BaseManagedBlockBuilder`): wraps append in `Storage.withSavepoint`. Failure **rolls back that savepoint** and returns the exception (tx rejected; block connection stays open). If savepoints unsupported: warn “Unclear if Postchain will work under these conditions.” Postgres: `isSavepointSupported() = true`.

`makeBlockBuilder` also sets a **block-level** savepoint `blockBuilder${nanoTime}`. `rollback()` rolls the whole block back to it.

`loadProposedBlock` (non-primary / sync apply): decode (parallel by default; reuse queued tx if same hash), verify Merkle root, `appendTransaction` each, `finalizeAndValidate`. Same write connection + savepoints.

`persistBlock` / `PersistOnlyBlockBuilder`: **inserts txs and header/witness without `apply`**. Used by `PersistOnlyBlockWriter` after snapshot sync (“we might never use this … unless we snapshot sync”). Header still validated (prev RID, height, config hash, Merkle root).

### 3.4 EBFT commit

`doc/ebft-protocol/EBFT-Overview.md`:

- 3f+1 for f faults (PBFT bound).
- States: WaitBlock → HaveBlock (fetched + valid) → Prepared (2f others HaveBlock same RID) → fetch commit signatures → commit. Signatures “must be recorded in the database.”
- Primary = `(height + round) % n`. Revolt: flag; 2f revolt flags increment round. Timeout grows `exponential_delay_initial * exponential_delay_power_base ^ round`, cap `exponential_delay_max`.
- Status heartbeat `MAX_STATUS_INTERVAL` default 1 s. Serial from clock on restart.
- Modules: StatusManager (authoritative), BlockManager (intents + DB ops), sync.

v2 (`EBFT-Overview_v2.md`): status at HaveBlock already carries the block signature, so Prepared nodes usually skip extra signature fetches.

`BaseBlockDatabase`: single worker thread; `buildBlock` / `loadProposedBlock` / `commitBlock` / `addBlock` serialized. `commitBlock` calls `theBlockBuilder.commit(witnessBuilder.getWitness())`.

`BaseManagedBlockBuilder.commit`: `beforeCommit` → `blockBuilder.commit(witness)` → **`storage.closeWriteConnection(eContext, true)`** → `afterCommit` (remove txs from queue, `strategy.blockCommitted`, `afterCommitHandler` which may request restart).

---

## 4. Postgres commit model (as stated in code)

This is no longer “unclear.” The code states the following.

**One JDBC transaction per block**, not per GTX tx.

Evidence:

- Write connections are created with `defaultAutoCommit = false` (`StorageBuilder.kt`).
- Engine reuses `currentEContext` write connection across begin/append/finalize (`BaseBlockchainEngine.withDBConnection`).
- Each GTX tx is isolated with a **JDBC SAVEPOINT** (`BaseStorage.withSavepoint` / `maybeAppendTransaction`). Failed tx: `rollback(savepoint)` only.
- Block abort: `rollback(blockBuilderSavepoint)` (`BaseManagedBlockBuilder.rollback`).
- Consensus commit: `closeWriteConnection(..., commit=true)` → `connection.commit()` (`BaseStorage.closeWriteConnection`).
- `commitBlock` SQL only writes `block_witness` on the already-inserted/finalized row (`SQLDatabaseAccess.commitBlock`).
- After commit, `afterCommitHandler` runs; a restart flag closes the engine.

Read queries use a separate read-only pool, isolation **REPEATABLE READ**, and `closeReadConnection` also `commit()`s (ends the read snapshot). Read connections are **thread-local cached / refcounted**.

Shared write contexts (`Storage.createSharedContext` / `claimSharedContext`): `ReentrantLock` so one DB transaction can be claimed across threads. Comment: “Only use this if it's absolutely necessary.”

Fatal SQLException: log; if `exit-on-fatal-error` then `exitProcess(1)`.

**Not stated:** write-pool isolation level; whether Rell entity tables share the same JDBC transaction (they use `ctx.conn`, so they do if they go through `TxEContext` — the engine does not name Rell tables).

---

## 5. Sync and snapshots

### Fast (`Fast-Synchronizer.md` + node props)

- Signers **always** fast-sync. Replicas: fast-sync until tip, then slow-sync.
- Parallel jobs (`fastsync.parallelism` default 10); commit **in height order**.
- Exit when no syncable peers, unless `fastsync.exit_delay` (60 s) not elapsed or `must_sync_until_height` not reached.
- Messages: `GetBlockHeaderAndBlock` / `ProposedBlock`; legacy `GetBlockAtHeight` / `CompleteBlock`.
- Peer states (`Peer-States.md`): SYNCABLE, DRAINED (with height), UNRESPONSIVE, BLACKLISTED (all non-SYNCABLE time-limited).

### Slow (`Slow-Synchronizer.md`)

- Replicas at tip only. One random peer, **10-block** `GetBlockRange`. Adaptive sleep every 20 requests. `slowsync.enabled` default true. Does not talk to legacy peers.

### Snapshots (`snapshots.md`, `snapshot-modules.md`, `Snapshot-Synchronizer.md`, `CompositeGTXModule.kt`)

- Enable **only at genesis** (`features.snapshot_enabled`; unknown feature flags rejected). All modules must be `SnapshotAware`.
- While producing: modules `emitDatum`; periodically seal Merkle-like pages; roots in block header. Interval default 100; keep last 10; `levels_per_page` default 2.
- Join: chain new on this node; ≥ `snapshotsync.threshold` (default 10_000) behind; a peer advertises a snapshot. Headers to snapshot height → datums+proofs per context → verify roots → **then** normal block sync. After snapshot, historical blocks can be written with `PersistOnlyBlockBuilder` (no apply).
- Datums: dense ids from 0; GTV payload; permanent vs updatable; **no deletes** (tombstones). Import order not guaranteed. `initializeImport` / `constructDatum` / `finalizeImport`.
- Node snapshot-sync caps: `snapshotsync.max_data_size` 26_000_000; `max_time` 5 s.

---

## 6. Production limits stated in this repo

From `Blockchain-Configuration-Properties.md` / `GtxConfigurationData.kt` / `Node-Configuration-Properties.md` / OpenAPI / `BaseTransactionQueue` / `standard.kt`. These are **engine defaults**. Directory Chain mainnet caps (other repo) can be tighter.

| Item | Value in this repo |
|---|---|
| `gtx.max_transaction_size` | 25 MiB (`GtxConfigurationData`; config doc). **Constructor default** on `GTXTransactionFactory` is `1024 * 1024` (1 MiB) if the factory is built without config — production path passes config. |
| `gtx.max_transaction_signatures` | 100 |
| `blockstrategy.maxblocksize` | 26 MiB |
| `blockstrategy.maxblocktransactions` | 100 |
| `blockstrategy.mininterblockinterval` | 25 ms |
| `blockstrategy.maxblocktime` | 30_000 ms |
| `blockstrategy.maxtxdelay` | 1_000 ms |
| `txqueuecapacity` | 2_500 |
| Rejected-status memory | 1_000 (`MAX_REJECTED`) |
| `query_timeout_seconds` | 60 (0/neg disables) |
| `query_cache_ttl_seconds` | 0 |
| `async_query_timeout_seconds` | 1 hour |
| `max_block_future_time` | 60_000 ms |
| `revolt.timeout` | 10_000 ms |
| `revolt.exponential_delay_max` | 600_000 ms |
| `api.max-request-body-size` / `max-data-size` | 55 MiB |
| `api.port` | 7740 |
| `debug.port` | 7750 |
| `messaging.port` | 9870 |
| `container.rest-api-port` | 7740 |
| `container.master-port` (netty) | 9860 |
| `container.admin-rpc-port` | 50051 |
| `container.postgres_max_locks_per_transaction` | 1024 |
| `container.min-space-quota-buffer-mb` | 300 (read-only near quota) |
| `container.bind-pgdata-volume` | true |
| `container.idle-timeout-ms` | 5 min |
| Pools: blockBuilder R/W | 10 / 8; wait write 100 ms |
| Pools: shared R/W | 10 / 2; wait write 10_000 ms |
| `rate-limit.blocks` | 100 unanswered block reqs/peer |
| `fastsync.parallelism` | 10 |
| `snapshotsync.threshold` | 10_000 |
| `nop` arg max | 64 bytes/chars |
| App DB schema version | 13 |
| Java (QuickGuide) | 17 |
| REST list limits | txs: default 25 max 600; blocks: default 25 max 100 |
| `connection.max_unknown_peer_connections_per_chain` | 20 |

`features.snapshot_enabled` is documented as **int, default 0**. Unknown feature flags rejected so old nodes cannot run new-feature chains.

EBFT performance note (official): reduce `maxblocktransactions` or raise revolt backoff if honest builds are slow.

---

## 7. Contradictions / deltas vs `/workspace/chromia-knowledge/postchain-postgres.md`

Only items where **this repo’s code or in-repo docs** disagree with, or now settle, a statement in that brief.

### Settled (was Unclear in the brief)

1. **SQL transaction granularity.** Brief §5.4 / §11.3: “not specified.” **Code: one JDBC write transaction per block**; per-GTX-tx **SAVEPOINT**; `commit()` of the block connection is the Postgres `COMMIT`. Witness is an `UPDATE` in that same transaction. See §4.
2. **Table naming / one DB vs schema.** Brief §2.2 / §11.1: “Unclear whether … one schema with many per-chain tables.” **Code: one `search_path` schema per process; per-chain tables are quoted names `"c{chainId}.{name}"` in that schema; housekeeping `LIKE 'c{chainId}.%'`.** Node-global tables (`meta`, `blockchains`, `containers`, …) are unprefixed. This is still not “one Postgres database per blockchain RID.”
3. **Rell-entity SQL names.** Still **not** in this repo (Rell lives elsewhere). Engine tables above **are** specified.

### Contradictions / mismatches

4. **Wiki vs README production status.** Brief already treated wiki “production NOT recommended” as stale. Confirmed: wiki home still has the alpha sentence; `README.md` on `dev` does not.
5. **Collation.** Brief §7.4: CLI Functional.md wants `en_US.UTF-8`; install docs `C.UTF-8`. **This repo’s collation check error text is only `C.UTF-8` / `C.UTF-8` / `UTF-8`.** Engine source of truth for Postchain itself is `PostgreSQLDatabaseAccess.checkCollation`.
6. **`GTXTransactionFactory` default 1 MiB vs config/docs 25 MiB.** Factory constructor default is `1024 * 1024`. `GtxConfigurationData` / Blockchain-Configuration-Properties / the brief all say **25 MiB**. The configuration factory **passes** `gtxConfig.maxTxSize`, so a normally constructed GTX chain uses 25 MiB. The 1 MiB default is only if someone constructs the factory without that argument.
7. **`snapshot_enabled` type.** Brief treats it as a boolean feature. In-repo config table: **type `int`, default 0**.
8. **Blockchain states.** Brief §3.5 NM API list: `RUNNING`, `PAUSED`, `IMPORTING`, `UNARCHIVING`, `REMOVED`. Engine enum also has **`ARCHIVED`**.
9. **Replica POST /tx.** Brief: replica accepts client txs only if `forwarding_replica=true`. OpenAPI: **“read replicas do not support this endpoint”** (403), and the same for waiting/rejected/status. Both can be true (forwarding is a node property; OpenAPI describes the replica model as 403). The brief should not imply a replica REST submit works without that flag.
10. **Write isolation.** Brief implies two write pools without isolation. Code: **read pool is REPEATABLE READ**; **write pool isolation is not set in `StorageBuilder`**. Do not claim write = REPEATABLE READ.
11. **Java.** QuickGuide: Java **17**. Brief also cites Chromia CLI Java **21+**. Those are different products; this repo’s documented engine runtime is 17.
12. **REST default port.** This repo: **7740**. Brief already notes Chromia node-config page saying 443. Engine source of truth remains 7740 (`Node-Configuration-Properties.md`).
14. **`tx_confirmation_time` query** (`standard.kt`) reads `FROM blocks WHERE block_iid = ?` **without** the `cN.` prefix in the SQL string. That is a raw table name `blocks` in the current `search_path`. Per-chain tables are actually `"cN.blocks"`. Whether that query works depends on search_path / extra views — **not explained in code comments**. Flag, do not invent a view.

### Not contradictions (brief matches code)

- Two storage pools and their default concurrencies/waits.
- EBFT states, 3f+1, primary formula, revolt exponential formula.
- Fast vs slow sync signer/replica split; 10-block slow ranges; snapshot genesis-only.
- `iid` vs RID on REST.
- GTX atomic multi-op; `nop` / `timeb`; 55 MiB API caps; 26 MiB / 100 tx block defaults; 2500 queue.
- GlobalStorageInitializer once per node vs `initializeDB` per chain.
- Subnode `max_locks_per_transaction` 1024; bind-pgdata default true; quota buffer 300.

---

## 8. Explicitly still unclear from *this* repo

1. Write-connection isolation level (not set in `StorageBuilder`).
2. Mapping of Rell `entity` / `object` to SQL table/column names (not in Postchain; Rell module is a GTX factory loaded by name).
3. Whether `standard.kt` `tx_confirmation_time` unqualified `blocks` is a bug, a leftover, or relies on an unstated view.
4. Numeric SCU sizes, Directory Chain mainnet caps, ICMF/EIF application APIs — other repos / docs, not this tree.
5. Provider `pg_dump` / PITR procedure — not in this tree. Coded backups: schema wipe, `ImporterExporter`, snapshot sync, `container.bind-pgdata-volume`.

---

## 9. Canonical raw URLs (this study)

- https://gitlab.com/chromaway/core/postchain/-/raw/dev/README.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/QuickGuide.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Postchain-Configuration.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Node-Configuration-Properties.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Blockchain-Configuration-Properties.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/ebft-protocol/EBFT-Overview.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/rest-api/REST-API.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/postchain-base/src/main/resources/restapi-docs/postchain-restapi.yaml
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/postchain-base/src/main/kotlin/net/postchain/StorageBuilder.kt
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/postchain-base/src/main/kotlin/net/postchain/base/data/PostgreSQLDatabaseAccess.kt
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/postchain-base/src/main/kotlin/net/postchain/base/data/SQLDatabaseAccess.kt
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/postchain-base/src/main/kotlin/net/postchain/base/data/BaseManagedBlockBuilder.kt
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/postchain-spi/src/main/kotlin/net/postchain/base/AbstractBlockBuilder.kt
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/postchain-base/src/main/kotlin/net/postchain/gtx/transaction.kt
