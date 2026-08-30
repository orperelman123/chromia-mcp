# Postchain and Chromia’s use of PostgreSQL

**Knowledge brief (official sources only)**  
Compiled: 2026-08-26  
Scope: Postchain as Chromia’s relational blockchain engine; how a Rell dapp becomes a Postchain chain with a Postgres backend; production hierarchy, Directory Chain, consensus, ICMF/ICCF/EIF, and documented ops/limits.

**Method.** Only official Chromia / ChromaWay sources were used (docs.chromia.com, gitlab.com/chromaway, chromaway.gitlab.io). Statements that are not explicit in those sources are marked **Unclear**. No inferred “best practices” beyond what the sources say.

---

## 1. What Postchain is, and how it sits under Rell

### 1.1 Definition

Postchain (JVM edition) is a modular blockchain framework designed primarily for **consortium** (permissioned / enterprise / federated) databases. In that model, blocks must be approved (signed) by a majority of consortium members — described as **proof-of-authority**, contrasted with proof-of-work and proof-of-stake.

The feature that differentiates Postchain from similar systems is **deep SQL-database integration**:

- All blockchain data is stored in an SQL database.
- Transaction logic can be defined in SQL (including stored procedures).
- Postchain uses SQL as a **black box**: it is **not** a database plugin and works with PostgreSQL as-is, without special configuration or modification of Postgres itself.

Sources: [Postchain QuickGuide](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/QuickGuide.md), [Postchain README](https://gitlab.com/chromaway/core/postchain/-/raw/dev/README.md).

The GitLab project describes Postchain as a “consortium blockchain framework (JVM edition).” The in-repo wiki home still carries a pre-release/alpha warning that “production use is NOT recommended”; that wiki text is older than the current `dev` docs and Chromia production network docs, and should not be treated as the current production-status statement. Chromia’s public docs describe Chromia as a production relational L1 that runs Postchain.

### 1.2 Tech stack (as documented)

| Item | Documented value |
|---|---|
| Languages | Kotlin, Java, SQL |
| SQL database | PostgreSQL |
| OS | Anything that supports Java 17; tested on Linux and macOS (Postchain QuickGuide). Chromia CLI separately requires **Java 21+** and **PostgreSQL 16+** for local persistence. |
| Crypto | SECP256k1 for signing by default (customizable); SHA256 for hashing |
| Programming model | Custom blockchains “should be programmed in Rell” |

Sources: [Postchain QuickGuide](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/QuickGuide.md), [Chromia CLI Functional.md](https://gitlab.com/chromaway/core-tools/chromia-cli/-/raw/dev/docs/Functional.md), [Get started / installation](https://docs.chromia.com/get-started/installation).

### 1.3 Module architecture

Postchain consists of:

- **Core** — common interfaces so modules interoperate.
- **Base** — base classes geared toward enterprise blockchains.
- **GTX** — optional but recommended generic transaction format (ASN.1 DER, native multi-signature, atomic multi-operation transactions). GTX modules define operations and queries; multiple modules compose into a composite module.
- **API** — REST API for submitting transactions and retrieving data.
- **EBFT** — consensus protocol based on PBFT (replaceable).
- **Client SDKs** — JavaScript, Kotlin, and C# are listed in the QuickGuide; Chromia docs also document TypeScript, Kotlin/Java, and Python clients.

A GTX transaction is a signed batch of named operations with arguments (RPC-like). It is **atomic**: all operations succeed or the transaction fails as a whole. Operations usually update the database; they may only perform checks.

Sources: [Postchain QuickGuide](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/QuickGuide.md).

### 1.4 How this sits under Rell and Chromia

Chromia is documented as a **relational blockchain platform** that “combines the power of traditional databases with blockchain security and decentralization.” Rell is “Chromia’s powerful programming language for building dapps.” Postchain’s QuickGuide states that custom blockchains should be programmed in Rell.

The production path from Rell source to a running chain, as documented by Chromia CLI:

1. Project settings live in `chromia.yml` (blockchains, modules, `moduleArgs`, libs, compile, database, deployments).
2. `chr build` compiles Rell and **creates a blockchain configuration** (XML or GTV) under `build/` (overridable via `compile:source` / `compile:target`).
3. That configuration is what Postchain loads. The Directory Chain’s allowed dapp GTX modules include `net.postchain.rell.module.RellPostchainModuleFactory` — the factory that turns compiled Rell into a Postchain GTX module.
4. Locally, `chr node start` starts a Postchain test node against PostgreSQL using that configuration.
5. On Chromia testnet/mainnet, `chr deployment create` compiles the same configuration and **submits a deployment proposal to the Directory Chain**. Subsequent code/config changes use `chr deployment update`.

Sources: [docs.chromia.com](https://docs.chromia.com/), [chr build](https://docs.chromia.com/cli/commands/build), [chr deployment](https://docs.chromia.com/cli/commands/deployment), [CLI Functional.md](https://gitlab.com/chromaway/core-tools/chromia-cli/-/raw/dev/docs/Functional.md), [Directory Chain mainnet limits](https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/doc/directory_chain/Mainnet-Blockchain-Configuration-Limits.md).

**Unclear from official sources retrieved:** a published, complete mapping of every Rell language construct to a specific PostgreSQL table/column/index name. Official sources treat Rell `entity` / `object` / attributes / indexes as the schema that is persisted and that `chr deployment update` diffs (see §2 and §6). They do not publish a table-naming specification.

---

## 2. PostgreSQL’s role

### 2.1 Why SQL is native

Official wording (Postchain QuickGuide):

> The main feature which differentiates Postchain from a similar system is that it integrates with SQL databases in a very deep way: all blockchain data is stored in an SQL database, transaction logic can be defined in terms of SQL code (particularly, stored procedures).

Chromia’s public positioning is the same idea at product level: a **relational** L1 with “built-in data indexing,” not a key-value chain with an external indexer bolted on.

Rell is the application language; Postchain is the engine that executes GTX operations/queries and commits state in PostgreSQL. Queries defined by GTX modules (including Rell queries) are served through the REST API against that SQL state.

Sources: [Postchain QuickGuide](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/QuickGuide.md), [docs.chromia.com](https://docs.chromia.com/), [Introduction](https://docs.chromia.com/intro/about).

### 2.2 One database per chain? Schemas?

**Official sources do not say “one PostgreSQL database per blockchain.”** What they do say:

**Node / process level (one JDBC target, one schema name)**

Postchain node properties include:

| Property | Meaning | Default |
|---|---|---|
| `database.driverclass` | JDBC driver | `org.postgresql.Driver` |
| `database.url` | JDBC URL | empty (must be set) |
| `database.schema` | Database schema | empty (must be set) |
| `database.username` / `database.password` | Credentials | empty |

Environment equivalents: `POSTCHAIN_DB_URL`, `POSTCHAIN_DB_SCHEMA`, `POSTCHAIN_DB_USERNAME`, `POSTCHAIN_DB_PASSWORD`.

Sources: [Node-Configuration-Properties.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Node-Configuration-Properties.md), [Chromia node config](https://docs.chromia.com/ecosystem/providers/nodes/node-config).

**Local Chromia CLI defaults**

- Default connection: `jdbc:postgresql://localhost:5432/postchain`.
- Dev-container defaults: database / user / password all `postchain`.
- Installation docs create a database named `postchain` with an explicit collation (see §7).
- `chr node start` applies configuration to “the configured database schema.” `--wipe` wipes that schema and restarts from height 0.
- Tests run in a **separate schema** named `<schema>_tests` so test data is isolated from development data.

Sources: [CLI Functional.md](https://gitlab.com/chromaway/core-tools/chromia-cli/-/raw/dev/docs/Functional.md), [Get started / installation](https://docs.chromia.com/get-started/installation), [chr node](https://docs.chromia.com/cli/commands/node).

**Multiple chains in one process / one container**

- A node can run multiple blockchains. `chr node start` starts each blockchain under `blockchains` in `chromia.yml` (optionally filtered by `--name`).
- Directory Chain NM API: `nm_get_blockchains_for_container` returns the list of blockchain RIDs in a container (RUNNING or PAUSED). A container can host more than one chain.
- Housekeeping: `housekeeping.interval_ms` is the “Interval for housekeeping procedure to **delete DB tables of removed blockchains**.” That wording implies **tables per blockchain** inside the node’s database/schema, not necessarily a separate Postgres database per chain.
- GTX modules have **global** DB init once per node (`GlobalStorageInitializer`) and **per-chain** `initializeDB(EContext)` when each blockchain starts.

Sources: [chr node](https://docs.chromia.com/cli/commands/node), [nm_api.rell](https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/src/nm_api/nm_api.rell), [Node-Configuration-Properties.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Node-Configuration-Properties.md), [POS-2115 / GlobalStorageInitializer](https://gitlab.com/chromaway/core/postchain/-/commit/504f9323fa92cfd42594b0ace9d20f08af62c302).

**Production containers (subnodes) have their own Postgres data directory**

On Chromia production nodes, dapp chains run in Docker **subnode containers** managed by a master node. Container properties include:

- `container.bind-pgdata-volume` — “Bind postgres data directory to host file system (so it's persisted)”; default `true`.
- `container.postgres_max_locks_per_transaction` — overrides Postgres `max_locks_per_transaction` in the **subnode database**; default `1024`.
- Filesystem options LOCAL / ZFS / EXT4 with quotas; `container.min-space-quota-buffer-mb` switches the container to read-only near quota.

So in production, isolation is documented at **container / subnode** level (dedicated resources + a persisted Postgres data dir per subnode), not as “one Postgres cluster per blockchain RID.”

Sources: [Node-Configuration-Properties.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Node-Configuration-Properties.md), [Chromia node config](https://docs.chromia.com/ecosystem/providers/nodes/node-config).

**Unclear:** whether a production subnode uses one Postgres database with multiple schemas, one schema with many per-chain tables, or one database per chain. Official node config exposes a single `database.url` + `database.schema` per process. Official housekeeping deletes **tables** of removed blockchains.

### 2.3 How state is stored and queried

**Stored**

- “All blockchain data is stored in an SQL database.”
- Blocks, transactions, and configurations are first-class DB objects. Postchain can **export/import** configurations and blocks via `DatabaseAccess` (`getAllBlocksWithTransactions`, etc.).
- REST API docs: several database tables have an integer primary key `iid` (internal id). The API must expose **RID** (resource identifier), not `iid`, for containers, blockchains, blocks, and transactions.
- Snapshots: while producing blocks, modules emit state changes as “datums” into a snapshot store; periodically sealed into a Merkle-like structure whose roots are committed in the block header. Snapshots can only be enabled at genesis (`features.snapshot_enabled`).
- Configuration data is “kept in the database”; `BlockchainConfiguration` is height-specific (hard forks / config changes at a height switch the factory objects used to validate and update state).

**Queried**

- GTX modules define **queries** performed via the client API / REST API.
- Blockchain config: `query_timeout_seconds` (default 1 minute; 0 or negative disables), `query_cache_ttl_seconds` (default 0 = no cache), optional async-query queue/timeout/retention.
- Clients call named queries (Kotlin example: `psClient.query("hello_world", …)`; CLI: `chr query`).
- CLI REPL with `--use-db` can access database entities.
- `chr test --use-db` / `--no-db` controls whether a session toward the configured database is established.
- Default CLI option for interacting with the chain is `--use-db`.

**Rell schema vs SQL**

`chr deployment update` performs **schema change detection**: it compares **entities, objects, and attributes** between deployed and new versions; warns on attribute/object/entity removal; shows new attributes and indexes; and “blocks dangerous changes until approved.” CLI can also `chr generate` entity-relationship diagrams. That is the official description of how Rell schema relates to the deployed chain. Exact SQL DDL is not published in the pages retrieved.

Sources: [Postchain QuickGuide](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/QuickGuide.md), [REST-API.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/rest-api/REST-API.md), [snapshots.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/snapshots/snapshots.md), [Postchain-Configuration.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Postchain-Configuration.md), [Blockchain-Configuration-Properties.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Blockchain-Configuration-Properties.md), [postchain-client README](https://gitlab.com/chromaway/core/postchain-client/-/raw/dev/README.md), [CLI Functional.md](https://gitlab.com/chromaway/core-tools/chromia-cli/-/raw/dev/docs/Functional.md), [chr test](https://docs.chromia.com/cli/commands/test).

### 2.4 Connection pools (production-relevant)

Two storage pools are built at node startup: **block builder** and **shared**.

| Property | Default |
|---|---|
| `database.blockBuilderReadConcurrency` | 10 |
| `database.sharedReadConcurrency` | 10 |
| `database.blockBuilderWriteConcurrency` | 8 |
| `database.sharedWriteConcurrency` | 2 |
| `database.blockBuilderMaxWaitWrite` | 100 ms |
| `database.sharedMaxWaitWrite` | 10 000 ms |
| `database.suppressCollationCheck` | false |
| `exit-on-fatal-error` | false |

Chromia’s published node-config page is a slightly older subset (it still lists a single `database.readConcurrency` in places). Prefer the Postchain GitLab table when the two differ.

REST API concurrency is auto-calculated from CPU count and `database.sharedReadConcurrency` (see §7).

Sources: [Node-Configuration-Properties.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Node-Configuration-Properties.md).

---

## 3. Production hierarchy: provider → node → cluster → container → blockchain

Documented Chromia production topology:

```
Providers
  └── Nodes (machines; build/verify blocks; run Postchain)
        └── Clusters (system cluster | dapp clusters)
              └── Containers (isolated vCPU / memory / storage; leased)
                    └── Blockchain(s) (each with its own RID, config, Rell code, SQL state)
```

### 3.1 Providers

Providers manage nodes and contribute computing power. Chromia’s intro distinguishes **System Providers** (core network) and **Node Providers** (dapp clusters); rewards are based on performance and governance participation.

Providers vote on proposals (new provider, network settings, blockchain config updates). Directory Chain enforces `provider_quota_max_actions_per_day` (example mainnet-like config: 100). A provider can have multiple keys and a signing threshold (Directory Chain provider multi-key module).

`nm_is_blockchain_provider(provider_pubkey, blockchain_rid)` returns whether that provider may deploy to the blockchain’s container.

Sources: [Platform architecture](https://docs.chromia.com/intro/architecture/platform-architecture), [Introduction](https://docs.chromia.com/intro/about), [Directory Chain README](https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/README.md), [nm_api.rell](https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/src/nm_api/nm_api.rell), [Node maintenance](https://docs.chromia.com/ecosystem/providers/nodes/node-maintenance-guidelines).

### 3.2 Nodes

“Nodes are individual machines responsible for processing transactions, validating blocks, and maintaining the blockchain ledger.” They build and verify blocks.

A node is identified by a secp256k1 keypair (`messaging.pubkey` / `messaging.privkey`). Peer-to-peer default port **9870**. REST API default port in Postchain docs is **7740** (Chromia node-config page says default 443, with 7740 also usable). Debug API default **7750**.

**Signer vs replica**

- Blockchain config `signers` is the list of block-signing public keys (required).
- EBFT consensus runs among signers. Minimum for BFT safety/liveness: **3f+1** nodes for up to f faulty (PBFT bound, cited in EBFT-Overview).
- `forwarding_replica`: “If blockchains running in **replica mode** should forward submitted transactions to signer nodes” (default `false`).
- Fast synchronizer: “**Signer nodes will always use fast synchronizer** while **replicas will only use it initially** and once synchronized to the tip will switch to **slow synchronizer**” (to reduce replica spam).
- Directory Chain NM API distinguishes cluster nodes, **cluster replica** nodes, and per-blockchain replica nodes (`get_cluster_replica_node_blockchains`, `get_blockchains_replicated_by_node`, `nm_get_blockchain_replica_node_map`).
- System cluster nodes **replicate all cluster anchoring chains**. Each dapp cluster maintains a **Directory Chain replica**.

Sources: [Platform architecture](https://docs.chromia.com/intro/architecture/platform-architecture), [EBFT-Overview](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/ebft-protocol/EBFT-Overview.md), [Node-Configuration-Properties.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Node-Configuration-Properties.md), [Fast-Synchronizer.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/synchronization/Fast-Synchronizer.md), [Slow-Synchronizer.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/synchronization/Slow-Synchronizer.md), [nm_api.rell](https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/src/nm_api/nm_api.rell).

### 3.3 Clusters

Chromia groups nodes into clusters. Each cluster runs specific blockchains. A network has:

- **One system cluster** — orchestrates the ecosystem; runs system chains (Directory Chain, Economy Chain, System Anchoring Chain, Token Chain, Transaction Submitter Chain, etc.). System-cluster nodes also replicate all cluster anchoring chains.
- **One or more dapp clusters** — run dapp blockchains inside resource-isolated containers. Each dapp cluster has its own **Cluster Anchoring Chain (CAC)** and a **Directory Chain replica**.

CM API (used by Postchain to learn cluster topology) exposes: cluster name, anchoring-chain RID, peer pubkeys/API URLs, blockchains in the cluster, signers at a height, API URLs for a chain, system-chain list, removed chains.

Sources: [Platform architecture](https://docs.chromia.com/intro/architecture/platform-architecture), [cm_api/module.rell](https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/src/cm_api/module.rell), [Directory chain docs](https://chromaway.gitlab.io/core/directory-chain/index.html).

### 3.4 Containers

Dapp blockchains run in **containers** with dedicated vCPU, memory, and storage, “leased from the network,” so a traffic spike on one dapp does not congest others.

Economy Chain manages container pricing, provider rewards, and lease payments, and tells Directory Chain (via ICMF) to create/upgrade/stop/restart/remove containers.

Directory Chain NM API documents container **resource limits**: `container_units`, `max_blockchains`, `extra_storage`, plus derived `cpu`, `ram`, `io_read`, `io_write`, `storage`. A container has a state (`RUNNING`, etc.) and may specify a subnode Docker image.

Implementation: master Postchain node launches **Docker subnodes**. Master/subnode ports: Netty 9860, REST 7740/443, debug 7750, admin gRPC 50051. Master can proxy or 307-redirect REST to subnodes (`api.subnode-http-redirect`).

Chromia intro: developers lease **Standard Container Units (SCUs)** instead of paying gas.

**Unclear from retrieved pages:** the exact vCPU/RAM/disk of one SCU on current mainnet. Economy Chain docs exist (`doc/economy_chain/EC-configuration-and-setup.md`) but were not fully retrieved for numeric SCU sizes.

Sources: [Platform architecture](https://docs.chromia.com/intro/architecture/platform-architecture), [Introduction](https://docs.chromia.com/intro/about), [nm_api.rell](https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/src/nm_api/nm_api.rell), [Node-Configuration-Properties.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Node-Configuration-Properties.md), [Configure Directory Chain](https://docs.chromia.com/ecosystem/providers/nodes/directory-chain-config).

### 3.5 Blockchains

Each dapp “operates on its own unique blockchain(s)” with custom logic and storage. Users create separate accounts per dapp. Developers may deploy multiple chains in a cluster and use ICMF/ICCF between them.

Identifiers (do not confuse):

- **Blockchain RID** — cryptographic resource id used by clients and APIs (hex). Directory Chain itself is looked up as `/brid/iid_0` (the directory chain always has **IID 0** in client docs).
- **Blockchain IID / chain id** — internal integer (`iid`); REST API must not expose `iid` as the public identifier.
- **Container name** and container `iid` exist internally in Postchain storage.

Blockchain states documented in NM API: `RUNNING`, `PAUSED`, `IMPORTING`, `UNARCHIVING`, `REMOVED` (plus moving/unarchiving migration records). CLI: `chr deployment pause` / `resume` / `remove` (remove is permanent).

Sources: [Platform architecture](https://docs.chromia.com/intro/architecture/platform-architecture), [Client-Guidelines.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/client-guidelines/Client-Guidelines.md), [postchain-client README](https://gitlab.com/chromaway/core/postchain-client/-/raw/dev/README.md), [nm_api.rell](https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/src/nm_api/nm_api.rell), [chr deployment](https://docs.chromia.com/cli/commands/deployment).

### 3.6 Anchoring hierarchy

```
Dapp / system chain block headers
    → Cluster Anchoring Chain (per cluster)
        → System Anchoring Chain
            → Ethereum (via Transaction Submitter Chain + EVM contract)
```

- Each CAC waits for block headers from the cluster’s chains and includes them in its own blocks.
- SAC does the same for all CACs.
- On consensus failure, history is verified hierarchically from SAC downward. Blocks anchored in SAC take precedence over conflicting versions.
- System cluster’s own CAC anchors system chains **except** SAC.
- Transaction Submitter Chain periodically submits the latest System Anchoring Block to an Ethereum smart contract.
- Postchain node can periodically **check** that heights are anchored to CAC / SAC / EVM (`anchoring-check.*` properties; EVM check default off, interval -1).

FT4 client `buildAndSendWithAnchoring` waits until the tx is anchored in cluster **and** system anchoring chains.

Sources: [Platform architecture](https://docs.chromia.com/intro/architecture/platform-architecture), [Directory chain index](https://chromaway.gitlab.io/core/directory-chain/index.html), [Node-Configuration-Properties.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Node-Configuration-Properties.md), [FT4 TransactionBuilder](https://docs.chromia.com/pages/ft4-ts-client/client/types/transaction_builder.TransactionBuilder.html).

---

## 4. Directory Chain: what it governs

Directory Chain is a Rell dapp that **manages all blockchains in the network, including itself**. It stores the information nodes need to synchronize and stay consistent: providers, nodes, containers, blockchain configurations (code), and the links between them.

It is the implementation of:

- **Node Management API (`nm_api`)** — what a Postchain node in **managed mode** asks in order to know which chains/containers to run, configs at height, replicas, limits, images, migrations.
- **Cluster Management API (`cm_api`)** — cluster peers, anchoring chains, API URLs, system chains.

Directory Chain also:

- Preserves code and configurations so a cluster/chain can be restored.
- Facilitates state validation via transaction history and deployed code at a given height.
- Communicates with Economy Chain over **ICMF** for resource allocation (leases → create/upgrade/stop/restart/remove container, create cluster, register dapp provider, assign subnode image / JAR extensions).
- Receives anchoring-chain ICMF topics `G_configuration_updated`, `G_configuration_failed`, and (Chromia docs) `G_last_anchored_heights`.
- Enforces **mainnet blockchain configuration limits** when a config is proposed (`proposal_blockchain` module args).
- Supports **delayed blockchain configuration**: after enough provider votes, apply only after `directory_chain.config_delay` milliseconds. Query: `list_delayed_blockchain_configs`.
- Supports provider proposals/voting (generic voter-set module; thresholds: `0` = supermajority `n - (n-1)/3` ≈ 67%, `-1` = simple majority, or an explicit count).
- Has a housekeeping module (`max_empty_container_time`) and a `node_software_version` module (recommended master/subnode Docker images).

**Managed mode vs manual / properties**

Node property `configuration.provider.node` is `properties` | `manual` | `managed` (default `properties`). Directory Chain README: it is “an implementation of the node management api for use when running postchain in **managed mode**.” Client guidelines: in managed mode, discover nodes via Directory Chain CM API; bootstrap by calling `/brid/iid_0` on any network node; use official explorer for an initial system-cluster node list.

Each dapp cluster runs a **Directory Chain replica** that stores configuration and Rell code for all dapps plus hierarchy (clusters, nodes, dapps, blockchains). Dapp nodes use that replica to decide which containers/chains to run.

System chains (Directory, Economy, CAC, SAC, Token, Transaction Submitter, EVM event receivers) must be deployed with `pmc network` commands, not generic dapp deploy. PMC (Postchain Management Console) is the provider-facing tool; Directory Chain README says PMC often needs updates when Directory/Economy APIs change (`version.rell`).

Sources: [Directory chain index](https://chromaway.gitlab.io/core/directory-chain/index.html), [module-docs.md](https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/module-docs.md), [README](https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/README.md), [DC-configuration-and-setup.md](https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/doc/directory_chain/DC-configuration-and-setup.md), [Configure Directory Chain](https://docs.chromia.com/ecosystem/providers/nodes/directory-chain-config), [Platform architecture](https://docs.chromia.com/intro/architecture/platform-architecture), [nm_api.rell](https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/src/nm_api/nm_api.rell), [cm_api/module.rell](https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/src/cm_api/module.rell), [Client-Guidelines.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/client-guidelines/Client-Guidelines.md), [full-network-setup.md](https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/doc/full-network-setup.md).

---

## 5. Transaction lifecycle: client → node → validation → block → Postgres commit

### 5.1 Client constructs a GTX transaction

1. Client is initialized with **blockchain RID** and an **endpoint pool** (one or more node REST URLs). Chromia client can look up system nodes. Directory Chain IID is 0; `/brid/iid_0` yields its RID.
2. `transactionBuilder()` / `chr tx` / FT4 `transactionBuilder`:
   - Add one or more named operations + GTV arguments.
   - Optionally prepend FT4 auth (`ft_auth` / EVM auth) or `iccf_proof`.
   - Optionally `addNop()` / `nop` so the same logical operation can be submitted more than once (uniqueness).
   - Sign (secp256k1; native multi-sig supported).
3. Post to a node REST API. Statuses documented by the Kotlin client:

   | Status | Meaning |
   |---|---|
   | `WAITING` | In the node’s TX queue |
   | `REJECTED` | Rejected (e.g. wrong args) |
   | `CONFIRMED` | Included in a block |
   | `UNKNOWN` | Investigate |

4. `chr tx` **awaits confirmation by default** (`--no-await` to post asynchronously). Clients poll `/tx/{blockchainRid}/{txRid}/status`. Poll interval/count are configurable on JS client (`statusPollInterval`, `statusPollCount`).
5. Optional: wait for cluster + system anchoring (`buildAndSendWithAnchoring`, `StandardChromiaClient.awaitAnchoredTx`).
6. Confirmation proof: `/tx/{blockchainRid}/{txRid}/confirmationProof`, verifiable against known signers at that height.

Sources: [postchain-client README](https://gitlab.com/chromaway/core/postchain-client/-/raw/dev/README.md), [Client-Guidelines.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/client-guidelines/Client-Guidelines.md), [CLI Functional.md](https://gitlab.com/chromaway/core-tools/chromia-cli/-/raw/dev/docs/Functional.md).

### 5.2 Admission on the node

- Replica: if `forwarding_replica` is true, submitted txs are forwarded to signer nodes; otherwise a replica is not documented as a block builder.
- Tx enters a queue of capacity `txqueuecapacity` (default **2500**).
- FT4 documents that Postchain calls query `priority_check` / `priority_check_v1` for every transaction; highest priority is executed first. If the extension throws, the tx is rejected.
- REST body limit `api.max-request-body-size` default **55 MiB**. Oversized request → HTTP 413.
- Client guidelines: HTTP 400/404/409/413 are typically client-side; 500/503/timeout/connection errors → retry another node. Resubmission is safe because a transaction can be included only once; if a new `nop`/nonce is used, use `timeb` so the previous attempt is permanently invalidated.

Sources: [Blockchain-Configuration-Properties.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Blockchain-Configuration-Properties.md), [Node-Configuration-Properties.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Node-Configuration-Properties.md), [lib.ft4.core.prioritization](https://docs.chromia.com/pages/ft4-rell/-f-t4%20-library/lib.ft4.core.prioritization/index.html), [Client-Guidelines.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/client-guidelines/Client-Guidelines.md).

### 5.3 Block building (primary) and EBFT validation

EBFT is a PBFT-derived protocol that polls **status** instead of relying on complex retransmission.

States: **WaitBlock** → **HaveBlock** (fetched from primary, validated) → **Prepared** (2f other nodes have the same block RID) → fetch commit signatures → **commit**.

Primary is `(height + round) % number_of_nodes`. If a primary is suspected faulty, a node sets **revolting**; when 2f nodes revolt, the round increments and a new primary is elected. Revolt timeout grows exponentially: `exponential_delay_initial * exponential_delay_power_base ^ round`, capped at `exponential_delay_max`.

Block-strategy defaults (overridable; **mainnet Directory Chain caps some of these** — see §7):

| Key | Default |
|---|---|
| `maxblocksize` | 26 × 1024 × 1024 (26 MiB) |
| `maxblocktransactions` | 100 |
| `mininterblockinterval` | 25 ms (Postchain default; **mainnet minimum allowed is 1000 ms**) |
| `maxblocktime` | 30 000 ms (empty block if queue empty) |
| `maxtxdelay` | 1000 ms (wait for more txs after the first) |
| `preemptiveblockbuilding` | true |

GTX `max_transaction_size` default **25 MiB**; `max_transaction_signatures` default 100.

After operations execute, Postchain can fire **event sinks** (`op_context.emit_event`). ICMF and EIF register such handlers.

Performance note (official): keep block-build time well **below half the revolt timeout**, because the primary must build and others must load the block before timeout. Reduce `maxblocktransactions` or raise revolt backoff if honest builds are too slow.

Sources: [EBFT-Overview](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/ebft-protocol/EBFT-Overview.md), [Blockchain-Configuration-Properties.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Blockchain-Configuration-Properties.md), [Tweaking-postchain-performance.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Tweaking-postchain-performance.md), [emit_event](https://docs.chromia.com/pages/rell/-rell%20-system%20-library/op_context/emit_event.html).

### 5.4 Postgres commit

Official sources describe commit as a **database operation invoked from the consensus layer**, not as a published SQL transcript:

- `BlockDatabase` connects `BlockchainEngine` to consensus.
- `BlockManager` calls database operations abstracted from consensus.
- After 2f+1 Prepared, the node fetches commit signatures (which **must be recorded in the database**) and commits the block.
- Two write pools: block-builder writes vs shared writes.
- Once committed, height advances; configuration at the new height may switch (`BlockchainConfiguration` is height-scoped).
- Housekeeping can later delete tables of **removed** chains.

**Unclear:** the exact SQL transaction boundaries (one Postgres transaction per block vs per tx) are not specified in the retrieved docs. The GTX transaction is atomically applied as a unit of blockchain logic; the engine then commits the block (including commit signatures) through `DatabaseAccess`.

Sources: [Postchain-Configuration.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Postchain-Configuration.md), [EBFT-Overview](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/ebft-protocol/EBFT-Overview.md).

### 5.5 After commit: sync, snapshots, anchoring

- Nodes that are behind use **fast sync** (parallel block fetch; signers stay on it) or **slow sync** (replicas at tip; adaptive sleep; 10-block ranges).
- Optional **snapshot sync** if the chain enabled snapshots at genesis and the node is ≥ `snapshotsync.threshold` (default 10 000) blocks behind.
- CAC / SAC / EVM anchoring as in §3.6.
- Clients should **stick to one node** for a multi-step flow because the chain is **eventually consistent** across nodes.

Sources: [Fast-Synchronizer.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/synchronization/Fast-Synchronizer.md), [Slow-Synchronizer.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/synchronization/Slow-Synchronizer.md), [snapshots.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/snapshots/snapshots.md), [Client-Guidelines.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/client-guidelines/Client-Guidelines.md).

---

## 6. Production operations

### 6.1 Node configuration

Production nodes are configured via properties files and/or environment variables (`POSTCHAIN_*`). Important groups:

- **Identity / P2P:** `messaging.privkey`, `messaging.pubkey`, `messaging.port` (9870), peer list `node.<i>.{pubkey,host,port}`, optional `initial-peer.*`.
- **Config provider:** `configuration.provider.node` = `properties` | `manual` | `managed`. Chromia mainnet/testnet nodes run **managed** against Directory Chain.
- **Infrastructure:** default `base/ebft`.
- **Database:** see §2.
- **REST / debug:** ports, concurrency, 55 MiB body/data caps, optional 307 redirect to subnodes.
- **Containers / subnodes:** Docker image (or image from Directory Chain), mount dirs, ZFS/EXT4 quotas, bind Postgres volume, health checks, idle timeout (default 5 minutes).
- **Sync / snapshots / revolt / anchoring-check / rate-limit.blocks.**

Sources: [Node-Configuration-Properties.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Node-Configuration-Properties.md), [Chromia node config](https://docs.chromia.com/ecosystem/providers/nodes/node-config).

### 6.2 Replica vs validator (signer)

Official term is **signer** (in `signers`) plus **replica mode**, not “validator” as a separate documented role name.

| | Signer | Replica |
|---|---|---|
| In `signers` | Yes | No |
| Builds/votes on blocks (EBFT) | Yes | No |
| Sync after catching up | Fast sync (always) | Slow sync |
| Can accept client txs | Yes | Only forwards to signers if `forwarding_replica=true` |
| Directory Chain | Cluster node / system node | Cluster replica node and/or per-chain replica set |

Sources: [Blockchain-Configuration-Properties.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Blockchain-Configuration-Properties.md), [Fast-Synchronizer.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/synchronization/Fast-Synchronizer.md), [Slow-Synchronizer.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/synchronization/Slow-Synchronizer.md), [nm_api.rell](https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/src/nm_api/nm_api.rell).

### 6.3 Backups, export/import, snapshots

Documented mechanisms (not a full `pg_dump` runbook):

- **`container.bind-pgdata-volume=true`** persists the subnode Postgres data directory on the host.
- Postchain **blockchain export/import**: export configurations (manual mode) and/or blocks; managed chains skip config export (configs live on Directory Chain) and export blocks only. Import can load configs and blocks into an existing chain.
- **Snapshots** (opt-in at genesis): faster join by fetching proven state near tip instead of every historical tx. Interval default 100 blocks; keep last 10 snapshots. Cannot be enabled retroactively; all modules must be snapshot-compatible.
- Directory Chain “preserves the code and configurations of blockchains, allowing for quick restoration.”
- `chr node start --wipe` is the opposite of backup: it **wipes the schema**.

**Unclear:** official docs retrieved do not specify a production `pg_dump` / WAL / PITR procedure for provider nodes. Node-maintenance docs cover software-update cadence, not database backup.

Sources: [Node-Configuration-Properties.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Node-Configuration-Properties.md), [export commit notes](https://gitlab.com/chromaway/core/postchain/-/commit/f77282039bc17e8a6549b6fe7bc3874ca07d136f), [snapshots.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/snapshots/snapshots.md), [Platform architecture](https://docs.chromia.com/intro/architecture/platform-architecture), [Node maintenance](https://docs.chromia.com/ecosystem/providers/nodes/node-maintenance-guidelines).

### 6.4 Schema migrations / Rell updates

**Local**

- `chr node start` on an existing schema **adds the new configuration at the next height** (does not replace from height 0) unless `--wipe`.
- `chr node update` adds a configuration at **current height + 2** (or `--preemption` blocks into the future). Must use the same `chromia.yml` / chain ids as `chr node start`.

**Production (Directory Chain)**

- `chr deployment update` compiles, runs **schema change detection** (entities/objects/attributes/indexes; warns or blocks removals), then submits an **update proposal** to Directory Chain.
- `--height` deploys the configuration at a specific height; `--verify-only` / `--skip-verification` exist.
- After enough provider votes, the config applies at the scheduled height, optionally after `config_delay`.
- Height-scoped configuration is a Postchain primitive: e.g. a hard fork at block 1000 means C1 for 0..999 and C2 from 1000.
- NM API: `nm_find_next_configuration_height`, `nm_get_blockchain_configuration`, pending/faulty config queries (precise configuration update), `nm_get_historic_configuration_height`.
- Directory Chain can **move** a blockchain between containers (`proposal_blockchain_move`; `nm_get_migrating_blockchain_node_info`; Postchain supports max **2** containers for a chain during migration).
- CLI constraint: “Deployed blockchains must use Rell versions supported by the target cluster. The CLI validates version compatibility before deployment.”
- Feature flags in `features` are rejected if unknown, so older Postchain nodes cannot run chains that need newer features. Snapshots can only be enabled on the first configuration.

There is a Directory Chain `migration` module described as migrating data “in the same way we do in EC.” Details of that module were not retrieved.

Sources: [chr node](https://docs.chromia.com/cli/commands/node), [chr deployment](https://docs.chromia.com/cli/commands/deployment), [CLI Functional.md](https://gitlab.com/chromaway/core-tools/chromia-cli/-/raw/dev/docs/Functional.md), [Postchain-Configuration.md](https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Postchain-Configuration.md), [nm_api.rell](https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/src/nm_api/nm_api.rell), [DC-configuration-and-setup.md](https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/doc/directory_chain/DC-configuration-and-setup.md).

### 6.5 Provider software updates

- Typical software updates: **Tuesdays**; may require restart or config changes; each update ships instructions.
- Urgent/security updates can happen any time.
- Governance **proposals** require voting.
- Providers who skip updates may see reduced performance / lag.

Sources: [Node maintenance guidelines](https://docs.chromia.com/ecosystem/providers/nodes/node-maintenance-guidelines).


---

## 7. Documented production constraints

### 7.1 Directory Chain / mainnet blockchain-config limits

Enforced when proposing a dapp-chain config (Mainnet-Blockchain-Configuration-Limits.md, DC-configuration-and-setup.md, Configure Directory Chain):

| Limit | Mainnet restriction |
|---|---|
| Config key path depth (`max_config_path_depth`) | 10 |
| Blockchain configuration size (`max_config_size`) | 5 242 880 (5 MiB) |
| `blockstrategy.maxblocksize` maximum | 27 262 976 (26 MiB) |
| `blockstrategy.mininterblockinterval` minimum | 1000 ms |
| `revolt.fast_revolt_status_timeout` minimum | 2000 ms |
| Allowed `features` | `merkle_hash_version` (required on the example mainnet moduleArgs; `require_min_merkle_hash_version: 2`) |
| `eif.snapshot.version` | required version 2 |
| `require_revolt_when_should_build_block` | default true (module arg) |
| `require_add_primary_key_to_header` | default true (module arg) |

**Default allowed dapp GTX modules**

- `net.postchain.rell.module.RellPostchainModuleFactory`
- `net.postchain.gtx.StandardOpsGTXModule`
- `net.postchain.d1.icmf.IcmfSenderGTXModule`
- `net.postchain.d1.icmf.IcmfReceiverGTXModule`
- `net.postchain.d1.iccf.IccfGTXModule`
- `net.postchain.eif.EifGTXModule`
- `net.postchain.web.WebStaticGTXModuleFactory`
- `net.postchain.zkp.ZKPGTXModule` (listed in the mainnet-limits doc; the YAML example in DC-configuration-and-setup omits ZKP)

**Default allowed sync extensions**

- `net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension`
- `net.postchain.eif.EifSynchronizationInfrastructureExtension`

Allowed Rell native functions example: `net.postchain.d1.BlockWitnessRellNative`.

### 7.2 Postchain block / tx / query defaults (may be tighter on mainnet)

| Parameter | Default |
|---|---|
| `gtx.max_transaction_size` | 25 MiB |
| `gtx.max_transaction_signatures` | 100 |
| `blockstrategy.maxblocktransactions` | 100 |
| `txqueuecapacity` | 2500 |
| `query_timeout_seconds` | 60 (0/negative disables) |
| `query_cache_ttl_seconds` | 0 |
| `async_query_timeout_seconds` | 1 hour |
| `max_block_future_time` | 60 000 ms |
| `revolt.timeout` | 10 000 ms |
| `revolt.exponential_delay_max` | 600 000 ms |

Source: Blockchain-Configuration-Properties.md.

### 7.3 REST / connection limits

| Parameter | Default |
|---|---|
| `api.max-request-body-size` | 55 MiB (413 if exceeded) |
| `api.max-data-size` | 55 MiB (response truncation flagged `X-Data-Truncated`) |
| `api.request-concurrency` | 0 = auto (standalone: min(sharedReadConcurrency, 5xCPUs); master: 2xCPUs) |
| `api.chain-request-concurrency` | -1 (unlimited); 503 if exceeded when set |
| `api.container-request-concurrency` | 25% of external pool (master) |
| `api.container-request-timeout-ms` | 60 000 |
| `connection.max_unknown_peer_connections_per_chain` | 20 |
| `rate-limit.blocks` | 100 unanswered block requests per peer |
| `fastsync.parallelism` | 10 (too high may violate peer rate limits) |

Sources: Node-Configuration-Properties.md, REST-API.md.

### 7.4 PostgreSQL constraints that are documented

- Driver: `org.postgresql.Driver`.
- Collation is **checked** at startup (`database.suppressCollationCheck` default false).
- Chromia CLI Functional.md: PostgreSQL **16+** and collation **`en_US.UTF-8`** for most persistence operations.
- Installation docs create the `postchain` DB with `LC_COLLATE`/`LC_CTYPE` `C.UTF-8` (macOS/Linux) or an ICU locale on Windows; they document fallbacks if the locale is missing.
- Subnode: `max_locks_per_transaction` default override **1024**.
- Disk quota: `container.min-space-quota-buffer-mb` (Postchain default 300; Chromia node-config page lists 100) — container goes **read-only** near the quota.
- Housekeeping deletes tables of removed chains (default interval 30 s).
- Windows native PostgreSQL is **not recommended for production** (installation docs).

Sources: Node-Configuration-Properties.md, CLI Functional.md, Get started / installation.

### 7.5 FT4 / dapp-level limits (not Postchain engine limits, but production-relevant)

| Parameter | Default |
|---|---|
| Account rate-limit `max_points` | 10 (max ops that can accumulate) |
| `recovery_time` | 5000 ms per point |
| Auth descriptors per account | 10 (capped at 200 if configured higher) |
| Max rules per auth descriptor | 8 |

Admin module (`lib.ft4.core.admin`) **must not** be present in production dapps.

Sources: FT4 configuration values, auth_descriptor_config.

---

## 8. Consensus, ICMF / ICCF, EVM anchoring (as documented)

### 8.1 Consensus

Chromia uses **EBFT**, an adapted PBFT. See section 5.3. Chromia platform-architecture page: an adapted version of the PBFT protocol known as EBFT.

### 8.2 ICMF — Interchain Messaging Facility

Documented as the protocol for sharing events/data between Chromia chains, within and across clusters.

Mechanics that are explicit:

- At the end of a transaction, after operations, Postchain runs **event sinks**. Rell `op_context.emit_event(type, data)` registers an event. ICMF and EIF use this internally.
- Production wiring is GTX modules plus a sync extension: `IcmfSenderGTXModule`, `IcmfReceiverGTXModule`, `IcmfReceiverSynchronizationInfrastructureExtension`.
- Receiver config lists **topics**, grouped (Directory Chain examples) as `icmf.receiver.anchoring.topics` and `icmf.receiver.global.topics`.

Directory Chain listens (docs plus DC setup; names as published):

From anchoring: `G_configuration_updated`, `G_configuration_failed`, and (Chromia docs page) `G_last_anchored_heights`.

From Economy Chain: `G_create_cluster`, `G_create_cluster_V2` / `G_create_cluster_v2`, `G_create_container`, `G_upgrade_container`, `G_stop_container`, `G_restart_container`, `G_remove_container`, `G_register_dapp_provider`, `G_assign_subnode_image_to_container`, `G_add_subnode_jar_extensions_to_container`.

Economy Chain also listens for Transaction Submitter topic `G_evm_transaction_submitter_cost_topic` once that chain exists.

CLI note (chromia-cli docs-review commit): ICMF support in `chr node start` is **for testing only**; unprocessed messages are lost on restart; large volumes may cause OutOfMemoryException.

**Unclear:** a full public Rell API reference for sending a payload to topic X on chain Y was not in the pages successfully retrieved. Platform docs state ICMF is how system chains and dapps share events; Directory/Economy configs show the topic-and-module wiring.

Sources: Platform architecture, emit_event, Configure Directory Chain, DC-configuration-and-setup.md, full-network-setup.md, chromia-cli docs-review commit.

### 8.3 ICCF — Interchain Confirmation Facility

ICCF is the **proof** path: a client (or relayer) takes a transaction that happened on chain A and submits an `iccf_proof` on chain B so B can verify inclusion.

FT4 cross-chain transfers are client-driven and ICCF-based:

- `init_transfer` on source (hops = blockchain RIDs including target). Source does not talk to the target; it does not check that the recipient exists.
- `apply_transfer` on each hop, **must include `iccf_proof`** that the previous hop tx was applied.
- `complete_transfer` on the source after the last apply (optional for funds; needed so clients stop showing the transfer as pending).
- Expiry path: cancel / `unapply_transfer` / `revert_transfer` / `recall_unclaimed_transfer`.

FT4 config recommends depending on the `iccf` lib from Directory Chain even if the dapp is not doing cross-chain yet.

`chr tx --iccf-tx` fetches the proof from the source chain and prepends `iccf_proof`. Local ICCF needs `--directory-chain-mock`.

`getSystemAnchoringIccfProofOp` exists on the FT4 TS client (proof via system anchoring).

The official ICCF example dapp: source records signers of a dummy op; target verifies a proof tx that the source tx was included, then records those signers.

Sources: lib.ft4.external.crosschain, FT4 configuration values, CLI Functional.md, ICCF Example, Platform architecture.

### 8.4 EIF / EVM interoperability and anchoring

EIF (Ethereum Interoperability Framework) imports events, generates verifiable proofs, manages state, and submits transactions between Chromia and EVM.

Documented pieces:

- `EifGTXModule` plus `EifSynchronizationInfrastructureExtension` (allowed on dapp chains).
- `SignerUpdateGTXModule` on Directory Chain.
- Event receiver chains (including a dynamic receiver for Token Chain) read EVM events via node-configured RPC URLs.
- Transaction Submitter Chain sends Chromia-originated txs to EVM (including periodic SAC headers to the Ethereum anchoring contract). At least one node needs EVM wallet balance.
- Economy Chain CHR bridge uses Chromia token-bridge plus managed validator contracts; staking rewards also cover legacy TwoWeeksNotice on Ethereum mainnet and BSC.
- Token Chain automates FT4 token plus bridge setup.
- `emit_event` is used internally by EIF.

Sources: Platform architecture, postchain-eif README, full-network-setup.md, Directory chain index.

---

## 9. End-to-end: Rell dapp to Postchain blockchain to Postgres

This is the path the official tools document.

1. **Author** Rell modules (`entity` / `object` / `operation` / `query`) and `chromia.yml` (`blockchains.<name>.module`, `moduleArgs`, `libs`, `database`, `deployments`).
2. **`chr build`** produces a blockchain configuration (XML/GTV) containing compiled Rell and Postchain settings (`signers`, `gtx.modules` including `RellPostchainModuleFactory`, `blockstrategy`, ICMF/EIF modules if used).
3. **Local:** PostgreSQL 16+ database `postchain` (documented default), `chr node start` starts a Postchain process, `initializeDB` per chain, schema created/updated, blocks produced against that schema. `chr test --use-db` uses `<schema>_tests`.
4. **Network:** lease a container (Economy Chain via ICMF tells Directory Chain to create/upgrade the container with CPU/RAM/storage/max_blockchains). `chr deployment create` submits the config to Directory Chain; save the printed Blockchain RID under `deployments.<network>.chains`.
5. **Managed nodes** poll Directory Chain (`nm_compute_blockchain_info_list`, `nm_get_blockchains_for_container`, `nm_get_blockchain_configuration`, `nm_get_container_limits`, `nm_get_container_image_or_default`). Master starts a subnode with a persisted Postgres volume, loads the Rell GTX module, and the chain begins at height 0 (or syncs via fast/snapshot sync).
6. **Clients** post GTX txs to node REST, then queue, then EBFT block, then Postgres commit of block plus app state. Queries read that SQL state.
7. **Headers** go to CAC, then SAC, then Ethereum. Cross-chain Chromia messages use ICMF (system) and/or ICCF proofs (FT4 and general confirmation).
8. **Updates:** `chr deployment update` diffs Rell schema, proposes a new height-scoped config; after votes (and optional delay) nodes switch `BlockchainConfiguration` at that height.

---

## 10. Canonical URLs

### Chromia docs

- https://docs.chromia.com/
- https://docs.chromia.com/intro/about
- https://docs.chromia.com/intro/about/architecture
- https://docs.chromia.com/intro/architecture/platform-architecture
- https://docs.chromia.com/get-started/installation
- https://docs.chromia.com/cli/commands/
- https://docs.chromia.com/cli/commands/build
- https://docs.chromia.com/cli/commands/test
- https://docs.chromia.com/cli/commands/node
- https://docs.chromia.com/cli/commands/deployment
- https://docs.chromia.com/build/configuration/project-config
- https://docs.chromia.com/build/ft4/configuration-values
- https://docs.chromia.com/ecosystem/providers/nodes/node-config
- https://docs.chromia.com/ecosystem/providers/nodes/directory-chain-config
- https://docs.chromia.com/ecosystem/providers/nodes/node-maintenance-guidelines
- https://docs.chromia.com/ecosystem/providers/container-management/
- https://docs.chromia.com/pages/rell/-rell%20-system%20-library/op_context/emit_event.html
- https://docs.chromia.com/pages/ft4-rell/-f-t4%20-library/lib.ft4.external.crosschain/index.html
- https://docs.chromia.com/pages/ft4-rell/-f-t4%20-library/lib.ft4.core.prioritization/index.html
- https://docs.chromia.com/pages/ft4-ts-client/client/types/transaction_builder.TransactionBuilder.html

### Postchain (gitlab.com/chromaway/core/postchain)

- https://gitlab.com/chromaway/core/postchain
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/README.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/QuickGuide.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Postchain-Configuration.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Node-Configuration-Properties.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Blockchain-Configuration-Properties.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/configuration/Tweaking-postchain-performance.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/ebft-protocol/EBFT-Overview.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/rest-api/REST-API.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/client-guidelines/Client-Guidelines.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/snapshots/snapshots.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/synchronization/Fast-Synchronizer.md
- https://gitlab.com/chromaway/core/postchain/-/raw/dev/doc/synchronization/Slow-Synchronizer.md

### Directory Chain (gitlab.com/chromaway/core/directory-chain)

- https://gitlab.com/chromaway/core/directory-chain
- https://chromaway.gitlab.io/core/directory-chain/index.html
- https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/README.md
- https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/module-docs.md
- https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/doc/directory_chain/DC-configuration-and-setup.md
- https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/doc/directory_chain/Mainnet-Blockchain-Configuration-Limits.md
- https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/doc/full-network-setup.md
- https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/src/nm_api/nm_api.rell
- https://gitlab.com/chromaway/core/directory-chain/-/raw/dev/src/cm_api/module.rell
- https://chromaway.gitlab.io/core/directory-chain/-directory%20chain/cm_api/index.html

### Postchain client and related Chromaway repos

- https://gitlab.com/chromaway/core/postchain-client
- https://gitlab.com/chromaway/core/postchain-client/-/raw/dev/README.md
- https://gitlab.com/chromaway/core-tools/chromia-cli/-/raw/dev/docs/Functional.md
- https://gitlab.com/chromaway/core-tools/chromia-cli/-/raw/dev/docs/Deployment.md
- https://gitlab.com/chromaway/core/postchain-eif
- https://gitlab.com/chromaway/core/postchain-eif/-/raw/dev/README.md
- https://gitlab.com/chromaway/example-projects/iccf-example
- https://chromaway.com/technology

---

## 11. Explicitly unclear (not invented)

1. **One Postgres database per blockchain** — not stated. Documented model: one JDBC URL plus schema per node/subnode process; tables per chain; a container may run multiple chains; subnodes persist their own pgdata.
2. **Exact Rell-entity to SQL table/column naming** — not in the retrieved language docs. Schema is described as entities/objects/attributes/indexes that `chr deployment update` diffs.
3. **SQL transaction granularity of a block commit** (one DB transaction per block vs per GTX tx) — not specified.
4. **Numeric SCU (vCPU/RAM/disk) on current mainnet** — hosting is lease SCUs; NM API exposes unit multipliers, not a published SCU size in the pages retrieved.
5. **Complete public ICMF send/receive Rell API** — topic/module wiring and `emit_event` are documented; a full application-level ICMF cookbook was not successfully retrieved.
6. **Provider pg_dump / PITR backup procedure** — not in retrieved official ops docs. Persist-volume, export/import, snapshots, and Directory Chain config restoration are what is documented.
7. **Chromia node-config page vs Postchain GitLab node-properties** — they overlap but are not identical (API default port 443 vs 7740; some pool property names). Treat GitLab Node-Configuration-Properties.md as the engine source of truth; Chromia page as the provider-facing subset.
