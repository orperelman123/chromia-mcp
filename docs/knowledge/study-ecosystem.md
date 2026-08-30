# Chromia official ecosystem study

Compiled 2026-08-26 from official Chromia Developer Documentation (`docs.chromia.com`) only. Nothing here is invented. Every number, command, BRID, address, formula, and limitation is taken from the crawled pages. Where official pages disagree with each other, both statements are recorded.

Crawl method: sitemap.xml (384 URLs) -> 143 target pages under the requested trees -> curl HTML extract. WebFetch was used first; several section-root URLs 404 (Cloudflare/Docusaurus index pages). Child pages exist and were crawled.

---

## 0. URL map and crawl notes

Requested section roots and what actually exists on 2026-08-26:

| Requested URL | HTTP | Reality |
| --- | --- | --- |
| `https://docs.chromia.com/ecosystem/providers/` | 404 | No index page. Children exist under `/ecosystem/providers/...`. Alias `/providers/pmc/` is 200. |
| `https://docs.chromia.com/ecosystem/bridge/` | 404 | No index page. Start at `/ecosystem/bridge/overview`. |
| `https://docs.chromia.com/ecosystem/filehub/` | 404 | No index page. Start at `/ecosystem/filehub/overview`. |
| `https://docs.chromia.com/ecosystem/extensions/` | 200 | Index exists. |
| `https://docs.chromia.com/ecosystem/block-explorer/` | 200 | Index exists. |
| `https://docs.chromia.com/ecosystem/governance/` | 404 | No index page. Start at `/ecosystem/governance/overview`. |
| `https://docs.chromia.com/get-started/about/` | 200 | Index exists. |

`/ecosystem/providers/apis-1` is a stub page titled APIs that only links to Postchain APIs under `/build/clients/postchain-clients/`. The actual Node Management API is `/ecosystem/providers/nodes/api`.

Pages crawled: **143** sitemap URLs in the seven trees. Empty/stub: 1 (`apis-1`). All others returned 200 with article text.

## 1. Get Started / About — platform fundamentals

Source tree: `https://docs.chromia.com/get-started/about/` and children.

### 1.1 What Chromia is

Chromia is a next-generation Layer-1 / relational blockchain platform for high-performance dapps. It combines a relational database (PostgreSQL) with blockchain security. Headline properties from the About index and Benefits page:

- Relational blockchain technology, fee-free transactions for users, built-in data indexing.
- Cluster-based architecture with anchoring and cross-chain communication.
- Dapps written in **Rell**; frontend talks to the backend via postchain-client libraries.
- Hosting is cloud-like: developers lease **Standard Container Units (SCUs)** instead of users paying gas.
- Providers run nodes (System Providers on the system cluster, Node Providers on dapp clusters).
- Wallets: MetaMask, Rabby, Ledger, MPC wallets.

White paper linked from Benefits: `https://chromia.com/documents/Chromia-_-Platform-white-paper2019.pdf`.

### 1.2 Benefits vs EVM (as documented)

From `/get-started/about/benefits` and `/get-started/about/chromia-vs-evm`:

- No gas fees for users; resource leasing instead.
- Full application state stored on-chain in `entity` tables (not logs/mappings + off-chain indexers).
- Built-in automation via special operations `__begin_block` and `__end_block` (runs when a transaction triggers a new block; not a guaranteed wall-clock scheduler).
- Rell is SQL/Kotlin-like, type-safe, overflow-protected arithmetic, mandatory authorization checks.
- Authentication: `auth.authenticate()` (FT4) instead of `msg.sender`; also `op_context.is_signer()` for admin-level ops.
- NFTs can store metadata and images on-chain (EVM NFTs often rely on external storage).
- Each dapp runs on its own dedicated blockchain(s); traffic on one dapp does not congest others.
- Transaction uniqueness: RID is a Merkle hash of the transaction body without signatures. No protocol-level nonce or fees. `nop` can be used at dapp level if nonce-like uniqueness is needed.

### 1.3 Rell (brief, from About)

Rell (Relational Language) compiles to optimized SQL on Chromia PostgreSQL-backed blockchain. Public API of a chain is **operations** (writes, signed, transactional) plus **queries** (reads, no consensus needed; querying one node is documented as sufficient for most apps). Clients typically use a random node with fallback.

### 1.4 Nodes and Postchain

A node is a physical or virtual server running **Postchain** + **PostgreSQL**. Each node has a unique key pair (public key is its identity). Providers manage nodes.

Postchain responsibilities: receive/validate/execute signed GTX transactions; EBFT consensus; store history and per-dapp state in PostgreSQL; execute Rell; automatic indexing.

**Transaction lifecycle** (`/get-started/about/architecture/node`):

1. Client sends a GTX transaction to any node running that chain via REST API (random strategy + fallback).
2. Body contains: blockchain RID (BRID), list of operations + args, list of signers + signatures.
3. Receiving node validates, multicasts to cluster peers.
4. EBFT (adapted PBFT, described as Proof-of-Authority): primary for the round takes txs from its queue, proposes a block; others execute without committing; if header matches they sign; once **>2/3** signatures, commit. Validators take turns. Tolerates **<1/3** malicious/offline/isolated nodes.
5. Block production is dynamic: more frequent under load, slower when idle. Per-chain `blockstrategy` config.
6. Rell ops translate to SQL updates on that dapp own PostgreSQL instance. History can be replayed to reconstruct state at any height.
7. Queries do not go through consensus.

**Dapp containers:** the master Postchain instance on the node spawns isolated VMs (containers) per dapp, each with its own PostgreSQL. Externally the node is one entity that accepts txs for any chain it hosts.

### 1.5 Platform architecture — clusters and anchoring

One **system cluster** + one or more **dapp clusters**. Consensus: **EBFT**.

**System cluster** runs: Directory Chain, Economy Chain, System Anchoring Chain, Token Chain, Transaction Submitter Chain, and related system chains. System cluster nodes also replicate all cluster anchoring chains.

**Dapp cluster:** dapp blockchains run in isolated containers with dedicated vCPU/RAM/storage. Each dapp cluster has:

- its own **Cluster Anchoring Chain** (anchors dapp block headers);
- a **Directory Chain replica** (full copy of Directory Chain; dapp nodes use it to know which containers/chains to run).

**Anchoring hierarchy:**

1. Dapp/system chain block headers -> Cluster Anchoring Chain.
2. Cluster Anchoring Chain block headers -> System Anchoring Chain.
3. System Anchoring Chain is anchored to **Ethereum** via the Transaction Submitter Chain (periodically submits latest System Anchoring Block to an Ethereum smart contract).

On consensus failure, history is verified from System Anchoring Chain downward.

**Scalability (as documented):** add dapp clusters and nodes; each container has dedicated resources so one dapp cannot congest another; dapps can lease more SCUs or shard across multiple chains that talk via ICMF/ICCF.

**EVM interoperability:** EIF (Ethereum Interoperability Framework) reads EVM events and injects them into Chromia chains. Used for H-Bridge (lock on one chain, mint on the other). CHR already moves between Economy Chain, Ethereum, and BNB Chain. USDT/USDC support is described as on the roadmap in the architecture-summary page.

### 1.6 System chains

| Chain | Role | Repo (docs) |
| --- | --- | --- |
| **Directory Chain** | Central registry: clusters, nodes, containers, blockchains, configs, Rell code, providers. Tracks upgrades. Talks to Economy Chain via ICMF for leased resources. Open source. | `https://gitlab.com/chromaway/core/directory-chain` |
| **Economy Chain** | Hosting prices, container leases, provider rewards, staking rewards, CHR token + CHR EVM bridge. Payment proofs for dapps. | same repo, `src/economy_chain` |
| **Token Chain** | FT4-compatible token creation, account strategies, minting policies, EVM bridge automation. Tokens independent of any one dapp chain. | same repo, `src/token_chain` |
| **Cluster Anchoring Chain** | Collects block headers from all chains in a cluster (except SAC itself in the system cluster). | Postchain Kotlin + `src/anchoring_chain_cluster` |
| **System Anchoring Chain** | Anchors all Cluster Anchoring Chains. Top of hierarchy. | Postchain Kotlin + `src/anchoring_chain_system` |
| **Transaction Submitter Chain** | Lets Chromia nodes send txs to EVM; used to anchor SAC to Ethereum. | mentioned in platform-architecture |

**Economy Chain account creation** (two documented methods):

1. Bridge at least 10 CHR from Ethereum Mainnet or BSC via Vault (`https://vault.chromia.com/en/deposit/`). Account created during bridging; may need EVM address linking.
2. Cross-chain transfer: at least 20 CHR from another Chromia chain (Token Chain, ColorPool, etc.) to an unregistered Economy Chain address. 10 CHR one-time account-creation fee, 10 CHR remaining. Activate in Vault.

**Token Chain account strategies:** each project keeps a CHR pool. Open pool (anyone pays a nominal fee) or minimum-amount strategy. Account creation on behalf of projects is authorized via ICCF and a list of allowed blockchain RIDs. Fees go to a Chromia Foundation-managed account.

**Token minting policy fields:** optional max supply; authorized minters; minting interval and amount; accumulative vs fixed-rate; optional rate and max supply per minter. Reference: `https://chromaway.gitlab.io/core/directory-chain/-directory%20chain/token_chain/minting_policy/index.html`.

### 1.7 Chain / network governance (platform, not the dapp starter kit)

From `/get-started/about/architecture/chain-governance` and architecture-summary:

- System providers operate system chains (Economy, Directory, etc.). Node providers operate dapp clusters. Each cluster has its own providers and rules.
- Dapp-chain voter set is initially defined by the **container lease owner**, then can add/remove members and change threshold by proposal/vote.
- System chains: voter set = system providers; **two-thirds majority** to pass.
- Directory Chain tracks all chain upgrades / approved code.
- Transparency: Block Explorer + staking at `https://vault.chromia.com/en/staking/`.

**Composition numbers — official pages disagree:**

| Source | Providers | Nakamoto |
| --- | --- | --- |
| architecture-summary | 15 system + 6 node | 6 (system), 3 (dapp) |
| chain-governance | 18 total: 12 system cluster, 6 dapp cluster | 5 (system), 3 (dapp) |
| explorer Features page | Total Providers: 21 (example metrics) | — |

Do not treat any single count as authoritative without re-checking Explorer.

### 1.8 Protocols

#### GTV — Generic Transfer Value

ASN.1 DER serialization for every primitive and composite type. CHOICE: null, byteArray, string, integer, dict, array, bigInteger.

Rell to GTV mapping (operation input / query input / query output) is tabulated on the GTV page. Notable: `decimal` is GtvString; `boolean` is GtvInteger; entity/rowid are GtvInteger; named tuples are GtvDict; unnamed tuples/lists/sets are GtvArray; non-text maps are GtvArray of pairs.

Rell 0.13.9+ **strict** type conversion by default: `byte_array` only GtvByteArray (not GtvString); `integer` only GtvInteger (not GtvBigInteger); `big_integer` only GtvBigInteger; `decimal` only GtvString.

#### GTX — Generic Transaction Protocol

Built on GTV. Features: standard ASN.1 DER; built-in multi-sig; atomic multi-operation txs (all succeed or all fail). Body: `blockchainRid` + operations `[name, args]` + signers. Envelope: body + signatures. ASN.1 schema is published on the GTX page (`RawGtxOp`, `RawGtxBody`, `RawGtx`).

#### FT4 (About-level)

Library for fungible tokens and accounts. Accounts are explicit (must be registered before receiving transfers). Auth via key pairs, including Ethereum/BSC addresses / MetaMask, or Chromia Vault. On-chain and cross-chain transfers inside Chromia; EVM bridging via the bridging framework. Single-key or multi-key auth descriptors. Full FT4 docs live under `/build/ft4/` (out of this study crawl scope).

#### ICMF — Inter-chain Messaging Facility

Event-style messaging. Best when **clients cannot be relied on** to ferry proofs. Use cases: shared-state sync, event notification, multi-step workflows.

- **Global topics** (`G_` prefix): only system chains can send; any chain can receive without naming the sender BRID. Cluster anchoring stores the block RID of blocks that contain global messages, so receivers can poll the anchoring chain.
- **Local topics** (`L_` prefix): any chain can send; receiver must list exact sender BRIDs.

Guarantees: delivered **once and only once**; **no delivery-time guarantee**; order guaranteed **per sender chain and topic**.

Implementation: `IcmfSenderGTXModule` (hashes of sent messages in block header; Rell lib emits events). Receiver: `IcmfReceiverGTXModule` + `IcmfReceiverSynchronizationInfrastructureExtension` (background poll; injects via special ops that are **allowed to fail** — topic blocked then retried).

Rell lib: `com.chromia.icmf` version **1.102.2** in the protocol page example. `icmf.send_message(topic, gtv)`; extend `receiver.receive_icmf_message` or `metadata_receiver.receive_icmf_message` (adds sender height/timestamp).

Course: `https://learn.chromia.com/courses/icmf-course/introduction`. Lib source: `https://gitlab.com/chromaway/postchain-chromia/-/tree/dev/chromia-infrastructure/rell/src/lib/icmf`.

Economy Chain to Directory Chain global topics for containers: `G_create_cluster`, `G_create_container`, `G_upgrade_container`, `G_stop_container`, `G_restart_container`, `G_remove_container`. Anchoring to Directory: `G_configuration_updated`, `G_configuration_failed`, `G_last_anchored_heights`.

#### ICCF — Inter-chain Confirmation Facility

Client-driven cross-chain proof protocol. Client submits tx on source, waits for finality + anchoring, builds proof, submits `iccf_proof` + dapp op on target.

Verification steps on target (GTX module `IccfGTXModule` / docs also write `IccfGtxModule`): Merkle proof of tx in block; block signatures; anchoring — intra-cluster (cluster anchoring only) or inter-cluster (cluster + system anchoring). Intra-cluster tradeoff: **replica nodes cannot verify cluster-anchoring of the proven tx** (only signers do). Use inter-cluster proofs if that is unacceptable.

`iccf_proof` args: `blockchain_rid`, `tx_hash`, `tx_proof`, and for inter-cluster `raw_cluster_anchoring_tx`, `cluster_anchoring_tx_op_index`, `cluster_anchoring_tx_proof`.

Rell lib (`com.chromia.iccf` version **1.90.1** in the protocol page): `extract_operation_args`, `extract_operation_arg`, `require_operation`, `require_and_return_valid_proof`, `make_transaction_unique`. Defaults: require `iccf_proof` present; require proven-tx signers also sign current tx; optional inter-cluster / source-BRID allowlist / timestamp window.

JS client helper: `createIccfProofTx(...)`. Confirmation proof REST: `/tx/{blockchainRid}/{txRid}/confirmationProof`.

Example project: `https://gitlab.com/chromaway/example-projects/iccf-example`. Course: `https://learn.chromia.com/courses/iccf-course/introduction/`. FT4 uses ICCF for cross-chain transfers. Directory Chain uses ICCF for blockchain-based provider authentication.

### 1.9 Hosting / SCUs

- No user gas. Developers lease containers with dedicated vCPU, RAM, storage, I/O.
- Lease weekly, pay in CHR, priced to a USD-equivalent target.
- **1 SCU = 2 GB RAM, 0.5 vCPU, 16 GB storage, I/O 25 MiB/s read and 20 MiB/s write.**
- Weekly target cost: **approximately 90 USD** for a default **7-node** dapp cluster.
- Extra storage beyond base SCU is billed extra. Manual pay or auto-renewal. **Anyone** can pay a lease (community can keep a dapp alive).
- If payment overdue: container **suspended for six months**, reactivatable on payment; after that **permanently deleted**.
- Monetization is the dapp choice via FT4 strategies (transfer fee, subscription, pay-per-use, freemium).

### 1.10 Staking

**Provider requirements** (About + Providers pages agree on these numbers; may change via governance):

| Role | Total stake per node | Min self-stake (10%) |
| --- | --- | --- |
| System provider | 600,000 CHR | 60,000 CHR native on Mainnet |
| Node provider | 300,000 CHR | 30,000 CHR native on Mainnet |

Self-stake: native CHR on Chromia Mainnet. Delegated stake: About/providers and Providers overview say delegated stake can be native CHR, ERC-20 CHR, or BEP-20 CHR. Provider-staking page says delegated stake comes from native CHR delegated by users. User-delegation and staking-summary document an **EVM staking phase-out**: rewards on Ethereum and BSC decrease from **2 December 2025** and stop **1 April 2026**; migrate to native CHR via Vault. Announcement: `https://blog.chromia.com/staking-update-evm-reward-phase-out-and-upcoming-new-features/`.

User flow: Vault staking page `https://vault.chromia.com/en/staking/` -> connect wallet with native CHR -> pick provider (uptime, commission, reputation, total stake) -> delegate.

User rewards: **base APR 3%** governance rewards, plus variable provider-performance / fee share. Daily distribution. **Unbonding: 14 days (336 hours)**; no rewards during unbonding.

Provider PMC commands: `pmc economy set-provider-staking-account`, `pmc economy update-provider-staking-reward-share`.

### 1.11 Providers (About-level)

Two roles: **System providers** (system cluster; vote on adding system providers; create new dapp clusters as demand grows) and **Node providers** (dapp clusters only). System providers can also run dapp-cluster nodes. Node provider to system provider via existing system-provider vote. Supermajority **>2/3** for substantial changes.

Two key-pair types: **provider key** (proposals/votes; identity) and **node keys** (block signing).

PMC is required to manage nodes.

### 1.12 Wallets

MetaMask (must add Chromia network settings manually), Rabby (multi-chain, auto network detect), Ledger (via MetaMask integration), MPC wallets (distributed key, enterprise).

### 1.13 Extensions (About-level)

Pre-built **Docker images** added to a container. One extension per container. **Immutable** once added. Availability can be network-wide or cluster-vote. Free at launch. Obtained via Vault container lease (select at lease time or Add extension later). Requires bridging CHR or staking 10 CHR to create an account if needed.

## 2. Providers ecosystem

Source tree: children of `https://docs.chromia.com/ecosystem/providers/`.

### 2.1 Roles and voter sets

Three roles in the Providers overview table:

| Role | Permitted actions |
| --- | --- |
| Dapp Provider (DP) | Deploy dapps; add replica nodes (default). |
| System Provider (SP) | Govern system chains; add node to system cluster. |
| Node Provider (NP) | Add block-builder nodes. |

Voter sets list who can vote. SPs can do some ops directly (e.g. create a cluster); large changes need consensus. Example: upgrade NP to SP needs **>=2/3** of providers.

Staking and rewards text matches About (600k/300k, 10% self-stake). Rewards come from dapp hosting-fee pool; formula can be changed by proposal.

### 2.2 Node setup (operational)

Install PMC on a **separate trusted machine**, not on the node. **Java 21** required. Verify: `pmc --version`.

**Hardware (docs):**

| | System node | Dapp node |
| --- | --- | --- |
| RAM | >=128 GB, 3200 MHz | >=256 GB, 3200 MHz |
| CPU | 32 cores, >=3.0 GHz | 32 cores, >=3.0 GHz |
| Storage | 4 TB NVMe SSD | 8 TB NVMe SSD (2 TB OS/master + 6 TB subnodes with project quotas) |
| Network | >=1 Gbps symmetric | >=1 Gbps symmetric |

Benchmark: `https://gitlab.com/chromaway/core-tools/postchain-benchmark`. Targets: `1000 ms/op` single node, `2000-2500 ms/op` multi-node.

Ports: **443** API (TLS via reverse proxy; **7740** alternative), **7750** debug (can disable later), **9190** Prometheus, **9870** sync (**no TLS** on host). Static IPv4 + domain required.

Config files: `bc-config.xml` (genesis / height 0, obtained from `curl <system-api>/brid/iid_0` then `curl <system-api>/config/<brid>?height=0`); `node-config.system.properties` or dapp variant. Node key is not the provider key (`pmc keygen --save node_keypair`).

Genesis values in the published examples: `genesis.pubkey=037434C8D4F2B7B7DE44E80486A814676DC3D898FD4488E10E1940B1C4C5837200`, `genesis.host=system.chromaway.com`, `genesis.port=9870`. Directory Chain BRID in PMC config example: `7E5BE539EF62E48DDA7035867E67734A70833A69D2F162C457282C319AA58AE4`. API: `https://system.chromaway.com`.

Dapp node uses `D1MasterInfrastructureFactory` + subnode Docker image (example tag `chromia-subnode:3.16.16` in prepare-node; upgrade page uses `3.30.7`). System node uses `D1InfrastructureFactory`. `configuration.provider.node=managed`.

Start path: Docker + PostgreSQL (docs give postgres tunables, `max_locks_per_transaction = 1024`). TLS via Nginx + Lets Encrypt. Prometheus `metrics.prometheus.port=9190`. Logs via `log4j2.yml` (do not leave debug on).

**Register node (after being voted in):**

```
pmc node register --cluster system --pubkey <node-public-key> --host <domain> --port 9870 --api-url https://<domain>:7740
pmc config --local --set api.url=<your-node-api-url>
```

API URL must be `https://` and port 443 (or 7740).

**Replica nodes:** maintain a full copy of a dapp chain. Register as dapp provider with `chr tx --blockchain-rid EC_BRID --evm-auth FUNDING_ACCOUNT register_dapp_provider_with_payment NEW_PROVIDER_KEY` (costs a small amount of tCHR in the documented example). Download genesis config from `https://system.chromaway.com/config/7E5BE539...?height=0`. Postgres 16.3-alpine3.20. Server image example `chromia-server:3.25.4`. Then `pmc node register-replica`, admin `blockchain initialize`, then after full sync of management + SAC add replicas with `pmc blockchain replica add`. Documented BRIDs on that page:

- Management / Economy Chain (page uses both names): `15C0CA99BEE60A3B23829968771C50E491BD00D2E3AE448580CD48A8D71E7BBA`
- System Anchoring Chain: `B497391373BB74944193205EB37C84B0520D474F491E2EF4743F16F670DB289B`

**Maintenance:** software updates typically **Tuesdays**; urgent/security anytime. Proposals require votes. Falling behind updates can reduce performance.

**Upgrade Postchain:** `docker stop/rm postchain`; pull matching subnode image; update `container.docker-image`; start `chromia-server:<ver>`. Docs example version **3.30.7**. Server and subnode versions must match. Verify: `docker ps`, `curl http://127.0.0.1:7751/_debug`, `docker logs postchain`.

**FT4 key change:** default provider key also controls the Economy Chain FT4 account. Options: Chromia-native multisig; swap to EVM/MetaMask via `pmc economy auth-descriptor-evm-swap --evm-address=...`; or dedicated staking FT4 account (`chr keygen`, `pmc economy set-provider-staking-account`). Economy Chain Mainnet BRID repeated: `15C0CA99BEE60A3B23829968771C50E491BD00D2E3AE448580CD48A8D71E7BBA`. After EVM swap, a transfer signed with the old FT4 key **should fail**.

**Provider operation keys** (Directory + Economy, not FT4): `pmc provider key list|add|revoke|threshold`. Initial key remains the provider unique identifier even if revoked for signing. Threshold: `1` default; `-1` simple majority `keys/2+1`; `0` super-majority `keys - (keys-1)/3`. Revoked keys cannot be re-added. Adding a key requires a multi-key config containing current + new key.

**Automated network setup:** Ansible repo `https://gitlab.com/chromaway/network-setup` deploys Chromia + 4 Postchain nodes on AWS (Ubuntu 24.04, secondary disk, DNS `nodeN.my-network.domain.com`). **Demo/test only** — docs say do not store provider keys on node servers in production; add TLS, logging, monitoring for production.

### 2.3 Node Management API (nm_api)

When Postchain runs in managed mode (always, with a management chain), bc0 must answer:

| Query | Meaning |
| --- | --- |
| `nm_get_peer_infos` | Peers (IP, port, pubkey) |
| `nm_get_peer_list_version` | Integer that changes whenever the node list changes |
| `nm_compute_blockchain_list` | BRIDs the given pubkey should run |
| `nm_get_blockchain_configuration` | Effective config bytes for BRID at height |
| `nm_find_next_configuration_height` | Next config-change height (> given height), or null |
| `nm_get_blockchain_replica_node_map` | BRID to replica pubkeys |

Any module implementing this API can be bc0 (typically Rell + voting). Example: >50% of bc0 signers must agree before a new chain is visible.

### 2.4 Directory Chain + Economy Chain config (provider docs)

Directory module args: `initial_provider`, `genesis_node` (`pubkey, host, port, api_url, territory?`), `common` (`allow_blockchain_dependencies`, `provider_quota_max_actions_per_day`), `proposal_blockchain` (max config path depth 10, max config size 5 MiB, max block size 26 MiB, min inter-block 1000 ms, min fast revolt 2000 ms, allowed GTX modules / sync exts), `auth_service` (`pubkey`, `include_system_cluster`).

Allowed dapp GTX modules listed in the config example include Rell, StandardOps, ICMF sender/receiver, and others (see live page for the full allow-list).

Economy Chain config page covers FT4, ICMF, and EIF settings for assets and staking (full YAML on the live page).

### 2.5 Blockchain-based provider authentication (ICCF)

`add_provider_blockchain_auth(my_pubkey, blockchain_rid)` — after this, **normal signing no longer works** (docs warn about lock-out). Subsequent Directory ops use ICCF proofs of txs on the linked chain.

Ops: `remove_provider_blockchain_auth_iccf`, `propose_blockchain_iccf`, `propose_blockchain_action_iccf`, `propose_configuration_iccf`, `propose_forced_configuration_iccf`, `propose_blockchain_rename_iccf`. Shared extra args: `my_pubkey`, `tx_to_prove`, `op_index` + ICCF proof. External chain must define matching op names with Directory-like params (minus `my_pubkey`/`description`).

This is the documented pattern for DAO-controlled chain management.

### 2.6 Container management (dapp-provider / lease owner)

**Add dapp provider to container voter set:** lease creates an initial dapp provider + voter set (needed to approve deployments/configs). Only dapp providers can join.

```
chr deployment voterset add-dapp-provider --container-id ${CONTAINER_NAME} --evm-auth ${LEASE_OWNER_EVM_ADDRESS} --pubkey ${NEW_PROVIDER}
```

Then container voter set approves via `chr deployment proposal` commands.

**Transfer lease ownership:**

1. Find Directory Chain RID on Explorer (`https://explorer.chromia.com/mainnet/cluster/system`).
2. `chr query --blockchain-rid ${DIRECTORY_CHAIN_RID} get_economy_chain_rid`
3. Current owner: `chr tx --blockchain-rid ${ECONOMY_CHAIN_RID} --evm-auth ${CURRENT_OWNER_EVM_ADDRESS} offer_container_lease_ownership_transfer ${CONTAINER_NAME} ${NEW_OWNER_ACCOUNT}`
4. New owner: `accept_container_lease_ownership_transfer_offer ${CONTAINER_NAME}`
5. Cancel mistake: `remove_container_lease_ownership_transfer_offer ${CONTAINER_NAME}`

### 2.7 Provider rewards

Pool = all dapp container fees. Parameters: SCUs contributed, uptime, role. Formula updatable by proposal.

Interactive estimator on the rewards page (example defaults shown: 1 dapp cluster, 1x64-SCU system node, 1x128-SCU dapp node, 30% occupancy, 99% uptime -> weekly **$114.62** system + **$877.77** dapp = **$992.39**). Then shared with delegators.

**System provider per node:**

```
max {
  (Total System Providers Cost * (1 - System Provider Risk Share))
    + (System Provider Revenue Share per Node * System Provider Risk Share),
  System Provider Revenue Share per Node
} * Availability Factor
```

- Risk share currently **0.1** -> guaranteed >=90% of costs back, then scaled by uptime.
- Revenue share per node = (total dapp-cluster revenue potential) x system provider fee share (**currently 0.1**) / (system-cluster node count).
- Availability Factor: 90% uptime -> 0; 100% -> 1. **Rewards only above 90% uptime.**

**Dapp provider per node:**

```
( (Dapp Cluster Value * (1 - Dapp Provider Risk Share))
  + (Dapp Cluster Value * Occupancy Rate * Dapp Provider Risk Share) )
/ Number of Nodes in Dapp Cluster
* Availability Factor
```

- Risk share currently **0.2** -> guaranteed >=80% of cluster max revenue, then occupancy + uptime.
- Occupancy = fraction of SCUs leased in that cluster.
- Same 90% uptime floor.

**Staking reward share:** default **10%** of provider total reward to delegators. `pmc update-provider-staking-reward-share` (rewards page) / `pmc economy update-provider-staking-reward-share` (staking + economy command ref). Change applies after **one week** so delegators can undelegate.

### 2.8 PMC CLI — all command groups

PMC submits txs to Directory and Economy chains. Install: Homebrew tap `chromia/core` (`brew install chromia/core/pmc`); apt `https://apt.chromia.com` (`sudo apt-get install pmc`); Windows via Docker image `registry.gitlab.com/chromaway/core-tools/management-console/pmc:<version>`. Releases: `https://gitlab.com/chromaway/core-tools/management-console/-/releases`. Docker must mount `$HOME/.chromia`. Java 21. Install on a machine that is **not** the node.

Top-level: `pmc [OPTIONS] COMMAND` — `--generate-completion=(bash|zsh|fish)`, `--version`, `-h`.

| Group | Purpose | Subcommands (as documented) |
| --- | --- | --- |
| `help` / `version` | Help / version | — |
| `keygen` | Generate key pair; `--save` writes `.chromia/config` | — |
| `config` | Edit PMC config (`brid`, `api.url`, keys); `--list`, `--local --set` | — |
| `network` | Bootstrap / inspect a network | `initialize` (system cluster + naked system container; `initial_provider` is first SYSTEM_P member); `initialize-economy-chain`; `initialize-token-chain` (listed twice on the page); `initialize-evm-transaction-submitter-chain`; `initialize-evm-event-receiver-chain`; `initialize-evm-event-receiver-price-oracle-chain`; `initialize-price-oracle-chain`; `initialize-evm-event-receiver-token-chain`; `summary`; `version`; `verify` (all nodes accessible) |
| `node` | Node lifecycle | `register`; `register-replica`; `update`; `replace` (rotate keypairs; comma-delimited keys in client config); `disable` (removes from clusters/replicas); `enable`; `remove` (disabled only); `remove-from-cluster`; `info`; `verify`; `blockchains`; `containers`; `list`; `ping`; `check-cluster-removal-status` |
| `provider` | Provider lifecycle | `info`; `quotas`; `quota` (propose); `nodes`; `list`; `register` (three tiers); `enable`; `promote` (can add signer nodes to clusters); `disable`; `transfer-action-points`; `blockchain-auth`; `key` (`list/add/revoke/threshold`); `update` |
| `proposal` | Directory/network proposals | `info`; `list`; `revoke`; `vote` (yes/no); `retract-vote`; `download-jar` |
| `voterset` | Voter sets | `create`; `update` (governor must be an existing voter set); `list`; `info` |
| `cluster` | Clusters | `list`; `add` (not tracked by Economy Chain; nodes in it not rewarded — as documented); `info`; `containers`; `provider` (`--add false` to remove); `limits`; `remove` (irreversible); `verify`; `replica add/remove` |
| `container` | Containers (provider/governance path) | `add` (gives deployer voter set deploy rights); `info`; `limits`; `subnode-image`; `subnode-jar-extension`; `list`; `remove` (irreversible); `pause`; `resume`; `configuration` |
| `blockchain` | Chains | `add`; `import`; `finish-import`; `import-foreign-configurations`; `import-foreign-blocks`; `update`; `force-update` (**irreversible, last resort**); `get-proposed-forced-configuration`; `approve-force-update` (cluster node provider only); `stop`; `start`; `remove` (irreversible); `rename`; `replicas`; `signers`; `list`; `list-delayed-configurations`; `info`; `get-all-configurations`; `get-configuration`; `get` (deprecated); `get-forced-configurations`; `configuration-diff`; `get-proposed-blockchain-rid`; `replica`; `move` / `cancel-move` / `finish-move`; `archive` / `unarchive` (irreversible); `restore-configuration` (to anchored block); `link-evm-eoa-account` |
| `cluster-anchoring` | CAC config | `update`; `get` |
| `economy` | Economy Chain | `list-tags` / `add-tag` / `update-tag` / `remove-tag`; `add-cluster`; `list-clusters`; `change-cluster-tag`; `cluster-creation-status`; `version`; `metrics`; `get-constants`; `update-system-provider-constants`; `update-constants`; `auth-descriptor-evm-swap` (swap main FT4 signer to EVM, flags T); `update-price-oracle-rates`; `proposal` (`info/list/revoke/vote`); `set-provider-staking-account`; `claim-test-chr` (**Testnet only**); `update-provider-staking-reward-share`; `update-staking-constants` |
| `subnode-image` | Subnode Docker images | `add`; `update`; `disable`; `enable`; `add-to-cluster`; `remove-from-cluster`; `info`; `list` |
| `lease` | Container leases | `list`; `info`; `upgrade-container`; `assign-subnode-image`; `create-container`; `remove-container` (**no refund**); `list-pending-tickets`; `add-subnode-jar-extensions` |
| `subnode-jar-extension` | JAR extensions | `add`; `update`; `disable`; `enable`; `add-to-cluster`; `remove-from-cluster`; `info`; `list`; `download-jar` |
| `transaction` | Offline tx files | `send FILE`; `sign FILE`; `view FILE` |

Most mutating blockchain/cluster/container commands are **proposals** that apply after the relevant voter set votes.

**Troubleshooting (PMC page):** check logs, hardware, `node-config-*.properties`. Stalled chain0: add a new config at the stuck height via `postchain-cli.sh add-configuration` then restart all cluster nodes. Slow infrequent txs: lower `blockstrategy.maxtxdelay`. Prometheus port in properties. Docker log drivers (`fluentd` example); subnodes need `container.docker-log-driver` / `container.docker-log-opts`. EBFT trace logger: `net.postchain.network.peer.DefaultPeerCommunicationManager`.

## 3. Bridge (EIF / H-Bridge)

Start: https://docs.chromia.com/ecosystem/bridge/overview (section root 404).

### 3.1 Architecture

EIF is a GTX module added to a dapp. It reads EVM events (multiple networks, contracts, events), validates them, and injects them via special op __evm_block.

Components:
- Token Bridge (EVM contract): vault; lock on deposit, unlock on withdraw.
- Event Receiver Chain: listens to Token Bridge events; inserts via __evm_block; feeds Bridge Chain.
- Bridge Chain: reads events via ICMF (or embedded); tracks tokens/accounts/deposits/withdrawals; issues unlock proofs; snapshots for foreign tokens.
- Dapps: receive minted tokens via FT4 cross-chain transfer.

Two Solidity contracts in gitlab postchain-eif:
1. TokenBridge.sol — token originated on EVM (lock-and-mint / burn-and-release).
2. ChromiaTokenBridge.sol — token originated on Chromia.

Security: withdrawal proofs signed by Chromia nodes (block headers approximately multi-sig). For EVM-origin tokens, honest nodes can start mass exit so users recover from a last known snapshot if a majority of nodes is compromised.

Lock and mint: EVM deposit -> Event Receiver sees event -> Bridge Chain validates -> mint on Chromia.
Burn and release: burn on Chromia -> user builds proof -> submit to Token Bridge -> lockup period -> funds released.

### 3.2 Lease a bridge vs deploy

Leasing (Vault -> Containers lease -> overflow -> Bridge lease) auto-updates the Validator Contract node list (each update costs CHR based on ETH rate). Still need an EVM bridge contract. Provide: EVM chain, Chromia destination chain, EVM bridge address, validator address.

| Network | Validator contract |
| --- | --- |
| BSC Testnet | 0x83dB85F7ef4447524D3A31c0F4664a89173C68Eb |
| BSC Mainnet | 0xc755927508b7Ac3f7B31c9Ed396F3bE91C723d00 |
| Ethereum Mainnet | 0xc755927508b7Ac3f7B31c9Ed396F3bE91C723d00 |

Staking UIs named: https://staking.chromia.com/ and https://staking.testnet.chromia.com/. After lease, register the Chromia BRID on the EVM contract.

### 3.3 Deploy path (BSC Testnet example; adaptable to ETH mainnet / BSC mainnet / Sepolia)

Repos: https://bitbucket.org/chromawallet/chromia-bridge-demo and https://gitlab.com/chromaway/core/postchain-eif.git

Steps: deploy ERC-20 -> deploy validator + TokenBridge (npx hardhat deploy:bridge --network bsc_testnet --validator-address DIRECTORY_VALIDATOR --offset 2) -> allowToken:bridge -> deploy Event Receiver + Bridge chains on Chromia -> setBlockchainRid:bridge.

Validator types: manual (owner updates set); managed (Transaction Submitter updates on validator changes, recommended for dapps); Directory Chain validator (system-cluster validators; not deployed by dapp devs). Convert Chromia node pubkey to EVM address with chr repl crypto.eth_pubkey_to_address.

Required GTX modules:
- Event Receiver: EifGTXModule, IcmfSenderGTXModule, sync EifSynchronizationInfrastructureExtension.
- Bridge Chain: EifGTXModule, IcmfReceiverGTXModule, sync IcmfReceiverSynchronizationInfrastructureExtension.

Missing these means deposits/withdrawals will not process. --offset on deploy is the dispute/withdrawal lock period in blocks.


## 3. Bridge (EIF / H-Bridge)

Start: https://docs.chromia.com/ecosystem/bridge/overview (section root 404).

### 3.1 Architecture

EIF is a GTX module added to a dapp. It reads EVM events (multiple networks, contracts, events), validates them, and injects them via special op __evm_block.

Components documented:
- Token Bridge (EVM contract): vault; lock on deposit, unlock on withdraw.
- Event Receiver Chain: listens to Token Bridge events; inserts via __evm_block; feeds Bridge Chain.
- Bridge Chain: reads events via ICMF or embedded; tracks tokens, accounts, deposits, withdrawals; issues unlock proofs; snapshots for foreign tokens.
- Dapps: receive minted tokens via FT4 cross-chain transfer.

Two Solidity contracts in gitlab postchain-eif:
1. TokenBridge.sol — token originated on EVM (lock-and-mint / burn-and-release).
2. ChromiaTokenBridge.sol — token originated on Chromia.

Lock and mint: EVM deposit, Event Receiver sees event, Bridge Chain validates, mint on Chromia.
Burn and release: burn on Chromia, user builds proof, submit to Token Bridge, lockup period, funds released.

### 3.2 Lease a bridge vs deploy

Leasing (Vault -> Containers lease -> overflow menu -> Bridge lease) auto-updates the Validator Contract node list. Each update costs CHR based on the ETH rate. An EVM bridge contract is still required. Provide: EVM chain, Chromia destination chain, EVM bridge address, validator address.

Validator addresses from the docs:
- BSC Testnet: 0x83dB85F7ef4447524D3A31c0F4664a89173C68Eb
- BSC Mainnet: 0xc755927508b7Ac3f7B31c9Ed396F3bE91C723d00
- Ethereum Mainnet: 0xc755927508b7Ac3f7B31c9Ed396F3bE91C723d00

Staking UIs named: https://staking.chromia.com/ and https://staking.testnet.chromia.com/. After lease, register the Chromia BRID on the EVM contract.

### 3.3 Deploy path

Repos: https://bitbucket.org/chromawallet/chromia-bridge-demo and https://gitlab.com/chromaway/core/postchain-eif.git

BSC Testnet example, adaptable to ETH mainnet, BSC mainnet, Sepolia:
deploy ERC-20; deploy validator + TokenBridge (npx hardhat deploy:bridge --network bsc_testnet --validator-address DIRECTORY_VALIDATOR --offset 2); allowToken:bridge; deploy Event Receiver + Bridge chains on Chromia; setBlockchainRid:bridge.

Validator types: manual (owner updates set); managed (Transaction Submitter updates on validator changes, recommended for dapps); Directory Chain validator (system-cluster validators; not deployed by dapp developers). Convert Chromia node pubkey to EVM address with chr repl crypto.eth_pubkey_to_address.

Required GTX modules:
- Event Receiver: EifGTXModule, IcmfSenderGTXModule, sync EifSynchronizationInfrastructureExtension.
- Bridge Chain: EifGTXModule, IcmfReceiverGTXModule, sync IcmfReceiverSynchronizationInfrastructureExtension.

Missing these means deposits and withdrawals will not process. --offset on deploy is the dispute/withdrawal lock period in blocks.



---

# Remaining official page extracts

Digest above covers About, Providers, PMC, and Bridge architecture/lease/deploy. Below is extracted official article text for the remaining trees.

## 3b. Bridge pages

Compiled from official extracted article bodies. Not invented.

### Chromia bridge client

SOURCE: https://docs.chromia.com/ecosystem/bridge/bridge-client/

This section covers how to set up Chromia bridge client to facilitate token transfers between Chromia and Ethereum Virtual Machine (EVM) networks.

The Chromia bridge client supports interaction with:

  * The Economy Chain and any other chain that supports bridging. Specify the BRID and corresponding contract addresses when configuring. It can serve as more broad tool compared to [Vault](<https://vault.chromia.com/en/deposit/>).
  * Separately deployed bridge chain.

Alternatively, the [chromia-bridge-demo](<https://bitbucket.org/chromawallet/chromia-bridge-demo/src/main/>) can be used in place of [Vault](<https://vault.chromia.com/>) to interact with any bridge chain, including the Economy bridge chain.

For deposit-only functionality, the deposit transaction can be invoked directly via an EVM block explorer.

[Set up the Chromia bridge client for token transfers between Chromia and EVM networks. Approve ERC20 tokens, check allowances, set blockchain RID, and configure validators using the @chromia/bridge-client library.](</ecosystem/bridge/bridge-client/client>)[Manage interactions with Chromia by establishing connections, creating accounts, and initializing the client with an EVM provider using @chromia/ft4 for session management.](</ecosystem/bridge/bridge-client/work-with-client>)[Bridge tokens between EVM and Chromia by approving token spending, making deposits, linking accounts, and initiating withdrawals using the @chromia/bridge-client library.](</ecosystem/bridge/bridge-client/example>)

### Configure the Chromia bridge client

SOURCE: https://docs.chromia.com/ecosystem/bridge/bridge-client/client

The [Chromia bridge client](<https://www.npmjs.com/package/@chromia/bridge-client>) is a TypeScript library with utilities for interacting with the Chromia Token Bridge. This topic will walk you through setting up the Chromia bridge client to facilitate token transfers between Chromia and Ethereum Virtual Machine (EVM) networks.

## Prerequisites​

Before you begin, ensure you have the following:

  * **TokenBridge contract** : The address of a deployed `TokenBridge` contract on an EVM network.
  * **ERC20 token contract** : The address of an ERC20 token on the same EVM network.
  * **Blockchain RID** : The RID of a Chromia blockchain using the `hbridge` Rell library.

## Configuration checklist​

  * ### Approve ERC20 token​

The ERC20 token must be approved by the `TokenBridge` contract owner using the `allowToken` function. You can accomplish this with the `@chromia/bridge-client` using the `allowToken` method:
        
        const contractTransactionResponse = await bcl.allowToken(token);  
        

  * ### Check spending allowance​

Verify the spending allowance with the `checkAllowance` method:
        
        const allowance: bigint = await bcl.checkAllowance();  
        

  * ### Set blockchain RID​

Use the `setBlockchainRid` function in the `TokenBridge` contract to set the blockchain RID. You can use the built-in method `setBlockchainRid` with `@chromia/bridge-client`:
        
        const contractTransactionResponse = await bcl.setBlockchainRid(rid);  
        

  * ### Validator configuration​

Convert the public keys of the validating nodes in the Chromia network to EVM addresses and configure them in the Validator contract, either during deployment or by calling the `updateValidators` function.

### Example usage: Bridge from EVM to Chromia and vice versa

SOURCE: https://docs.chromia.com/ecosystem/bridge/bridge-client/example

### Bridge from EVM to Chromia​

  * #### Approve token spending​

Ensure the user approves token spending by the token bridge:
        
        const approvalResponse = await bcl.approveDepositAmount(BigInt(10));  
        

  * #### Bridge from EVM to Chromia​

To bridge tokens from EVM to Chromia, use the `depositToEvmBridgeContract` method, specifying the number of tokens to bridge:
        
        const contractTransactionResponse = await bcl.depositToEvmBridgeContract(BigInt(100));  
        

If the user has created an FT4 account and linked it to their EVM address, funds for foreign tokens will be minted directly to that account. For native tokens, funds will be transferred from the bridge account to the user's account.

If the user hasn't created an account, or has created one but hasn't linked it to their EVM address, funds for foreign tokens will be minted to the pool account instead. For native tokens, funds will be transferred from the bridge account to the pool account. In this case, the user must create and link an FT4 account. Once this is done, the funds will be transferred from the pool account to the user's account.

  * #### Link EVM account​

After a deposit, link the EVM account with the corresponding FT4 account created during the deposit process. Provide the `evmKeyStore` you created earlier:
        
        const accountLinkingResponse = await bcl.linkEvmEoaAccount(evmKeyStore);  
        

note

If the EVM account is linked, it will be returned to the `accountLinkingResponse`. An EVM account can have multiple FT4 accounts linked to it.

### Bridge from Chromia to EVM​

  * #### Bridge from Chromia​

Call the `bridgeFromChromia` method with the amount and the asset ID:
        
        // Network ID does not need to be provided as it will be fetched from the provider  
        
        
        const transactionResponse = await bcl.bridgeFromChromia(BigInt(10), Buffer.from("YOUR_ASSET_ID", "hex"));  
        

### Request withdrawal from EVM bridge​

  * #### Create a pending withdrawal request​

This needs to be accepted by the user to start the withdrawal process using the `requestEvmWithdraw` method:
        
        const erc20WithdrawalInfo = await bcl.getErc20WithdrawalByTransactionRid(  
        
        
          transactionResponse.receipt.transactionRid,  
        
        
          opIndex  
        
        
        );  
        
        
        // Get event proof for withdrawal  
        
        
        const eventProof = await bcl.getWithdrawRequestEventProof(erc20WithdrawalInfo.event_hash);  
        
        
        // Request withdrawal  
        
        
        const requestedWithdraw = await bcl.requestEvmWithdraw(eventProof);  
        

  * #### Check withdrawal status​

Depending on the bridge contract configuration, the user must wait a certain number of blocks on EVM to complete their withdrawal. This can be done using the `getPendingWithdrawFromProof` method:
        
        const { block_number } = await getPendingWithdrawFromProof(eventProof);  
        

Once the `block_number` has been reached on the target EVM chain, the user can withdraw their tokens:
        
        const withdrawal = await bcl.evmWithdraw(eventProof.leaf as Buffer);  
        

## Additional methods​

  * `getErc20Deposits(filter?: DepositFilter, pageSize?: number, pageCursor?: string)`: Returns all deposits specified by the filter.
  * `getErc20Withdrawals(filter?: WithdrawFilter, pageSize?: number, pageCursor?: string)`: Returns all withdrawals from the EVM bridge specified by the filter.
  * `setBlockchainRid(blockchainRid: Buffer)`: Sets the blockchain RID.

### Work with the client

SOURCE: https://docs.chromia.com/ecosystem/bridge/bridge-client/work-with-client

To interact with the Chromia blockchain using the bridge client, you'll need to manage an active `Session` that handles all Chromia-related queries and operations. However, the bridge client can be initialized without a session at the start. Here's a step-by-step guide to set up your application using `@chromia/ft4`.
    
    
    // 1: Setup a connection to postchain through postchain-client  
    
    
    const pcl = await createClient({  
    
    
      nodeUrlPool: "YOU_NODE_URL_POOL",  
    
    
      blockchainRid: "YOUR_BLOCKCHAIN_RID",  
    
    
    });  
    
    
      
    
    
    // 2: Create an account  
    
    
    const evmKeyStore = await createWeb3ProviderEvmKeyStore(window.ethereum);  
    
    
    const ad = createSingleSigAuthDescriptorRegistration([AuthFlag.Account, AuthFlag.Transfer], evmKeyStore.id);  
    
    
    const response = await registerAccount(pcl, evmKeyStore, registrationStrategy.open(ad));  
    
    
      
    
    
    // 3: Log in your user  
    
    
    const evmKeyStoreInteractor = createKeyStoreInteractor(pcl, evmKeyStore);  
    
    
    const accounts = await evmKeyStoreInteractor.getAccounts();  
    
    
    const session = await evmKeyStoreInteractor.getSession(accounts[0].id);  
    

note

Ensure that you save your `evmKeyStore` in your application state, as some methods in `@chromia/bridge-client` will require it.

With your `Session` object ready, you can initialize the bridge client. You will also need to provide an EVM Provider, either a `BrowserProvider` or a `JsonRpcProvider`:
    
    
    const provider = new BrowserProvider(window.ethereum);  
    
    
    const bcl = await bridgeClient(  
    
    
      { bridgeAddress: "YOUR_BRIDGE_ADDRESS", tokenAddress: "YOUR_TOKEN_ADDRESS" },  
    
    
      provider,  
    
    
      session  
    
    
    );  
    

If your application setup prevents having an active `Session` when instantiating the bridge client, you can still set up the client without it:
    
    
    const provider = new BrowserProvider(window.ethereum);  
    
    
    const bcl = await bridgeClient({ bridgeAddress: "YOUR_BRIDGE_ADDRESS", tokenAddress: "YOUR_TOKEN_ADDRESS" }, provider);  
    

Later, when you have access to the `Session`, you can set it on the client with `setSession(session: Session)`:
    
    
    bcl.setSession(session);

### Lease a bridge

SOURCE: https://docs.chromia.com/ecosystem/bridge/bridge-lease

Leasing a bridge allows you to utilize the Chromia bridge component without manually deploying it. This process replaces the step **Deploy the Chromia bridge component** found in the **[bridge deployment guide](</ecosystem/bridge/deploy-bridge/>)**.

You still need an **EVM bridge contract** to interact with the leased bridge. For more information, refer to **[Deploy the EVM bridge component](</ecosystem/bridge/deploy-bridge/deploy-bridge-contract>)**.

## How leasing works​

To verify a bridge withdrawal transaction on the Chromia network from the EVM side, your EVM account must invoke the `withdraw` function on the bridge smart contract. You need to submit the block header of the block containing the withdrawal, along with its block signatures for verification.

The bridge forwards your function call to the Validator Contract, which interacts with the Chromia nodes. Once the Validator Contract verifies the signatures' validity, it returns the operation's result to your EVM account. This process enables you to verify specific actions on the Chromia network.

The Validator Contract maintains a list of nodes and signers on the Chromia network. To avoid issues related to updates in the network, you need to update this list either manually as a developer or automatically by creating a bridge lease.

## The leasing process​

Creating a bridge lease facilitates automatic updates to the node list in the Validator Contract. Each update incurs a transaction fee, paid in **CHR** tokens, based on the current **ETH** exchange rate.

To initiate the process, visit Chromia Staking on **[Mainnet](<https://staking.chromia.com/>)** or **[Testnet](<https://staking.testnet.chromia.com/>)** , and connect your wallet.

Then, click **Containers lease** in the header. In the container list, select your required container, click the overflow menu, and choose **Bridge lease**.

Confirm your container selection by clicking **Lease a bridge**.

### Specify all bridge details​

When you lease a bridge, provide the following details:

  * **EVM chain**
  * **Chromia destination chain**
  * **EVM bridge contract address** (see **[Deploy the token bridge contract](</ecosystem/bridge/deploy-bridge/deploy-bridge-contract>)**)
  * **Validator contract address** based on your target network:
    * **BSC Testnet** : `0x83dB85F7ef4447524D3A31c0F4664a89173C68Eb`
    * **BSC Mainnet** : `0xc755927508b7Ac3f7B31c9Ed396F3bE91C723d00`
    * **Ethereum Mainnet** : `0xc755927508b7Ac3f7B31c9Ed396F3bE91C723d00`

After entering all the details, click **Lease bridge** to finalize the process.

## Final Step: Register the leased bridge on EVM​

Once you lease the bridge, you must register it on the EVM bridge contract. Follow the instructions in [Register the Chromia bridge component on EVM](</ecosystem/bridge/deploy-bridge/register-bridge>) to complete the setup.

### Troubleshoot the deployed bridge

SOURCE: https://docs.chromia.com/ecosystem/bridge/bridge-troubleshooting/

This section provides a guide for troubleshooting and debugging a **Chromia Bridge** deployment to help identify any missing or faulty steps.

[ Analyze and debug deposits from the EVM network to a Chromia chain through a deployed bridge.](</ecosystem/bridge/bridge-troubleshooting/bridge-deposit-troubleshooting>)[ Analyze and debug withdrawals from a Chromia chain to the EVM network through a deployed bridge.](</ecosystem/bridge/bridge-troubleshooting/bridge-withdraw-troubleshooting>)

### Bridge deposit troubleshooting guide

SOURCE: https://docs.chromia.com/ecosystem/bridge/bridge-troubleshooting/bridge-deposit-troubleshooting

If your deposit from the EVM to Chromia does not appear on the Chromia side, follow these steps to identify and resolve the issue.

## 1\. Verify EVM transaction​

First, check whether your transaction was successful on the EVM chain:

  * Look up your transaction hash on the appropriate blockchain explorer to verify that the transaction status is "Success."
  * Ensure that the transaction has enough block confirmations, as defined by the `eif.chains.<EVM_CHAIN>.evm_read_offset` parameter in the Event Receiver Chain configuration. The following values are recommended defaults:
    * **Mainnet** : 50 for Ethereum and 100 for BSC
    * **Testnet** : 10 for both Ethereum and BSC.

## 2\. Verify Event Receiver Chain configuration​

Ensure the Event Receiver chain is configured correctly to receive EIF events and send ICMF messages to the Bridge chain.
    
    
    blockchains:  
    
    
      <event_receiver_chain>:  
    
    
        module: <module_name>  
    
    
        config:  
    
    
          gtx:  
    
    
            modules:  
    
    
              - "net.postchain.eif.EifGTXModule"  
    
    
              - "net.postchain.d1.icmf.IcmfSenderGTXModule"  
    
    
          sync_ext:  
    
    
            - "net.postchain.eif.EifSynchronizationInfrastructureExtension"  
    

## 3\. Verify transaction block processing on the Event Receiver Chain​

Check whether the Event Receiver chain has processed your transaction block using the following CLI commands:

To retrieve the last processed block data:
    
    
    chr query --api-url $NODE -brid $ER get_last_evm_block_data -- network_id=56  
    
    
    # or  
    
    
    chr query --api-url $NODE -brid $ER get_last_evm_block_data_with_events -- network_id=56  
    

To retrieve specific block data:
    
    
    chr query --api-url $NODE -brid $ER get_evm_block_data -- network_id=56 height=12345678  
    
    
    # or  
    
    
    chr query --api-url $NODE -brid $ER get_evm_block_data_with_events -- network_id=56 height=12345678  
    

## 4\. Verify Bridge Chain configuration​

Ensure the Bridge chain is configured correctly to receive ICMF messages from the Event Receiver chain and process EIF events.
    
    
    blockchains:  
    
    
      <bridge_chain>:  
    
    
        module: <module_name>  
    
    
        config:  
    
    
          gtx:  
    
    
            modules:  
    
    
              - "net.postchain.d1.icmf.IcmfReceiverGTXModule"  
    
    
              - "net.postchain.eif.EifGTXModule"  
    
    
          sync_ext:  
    
    
            - "net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension"  
    
    
          icmf:  
    
    
            local:  
    
    
              - topic: L_evm_block_events  
    
    
                bc-rid: x"{EVENT_RECEIVER_BLOCKCHAIN_RID}"  
    

## 5\. Verify deposit event status on the Bridge Chain​

Use the following CLI commands to check whether the Bridge Chain has detected and processed your deposit.

### Retrieve deposit event by EVM transaction hash:​
    
    
    chr query --api-url $NODE -brid $BRIDGE eif.hbridge.get_erc20_deposits -- 'filter=[null,null,null,null,x"{EVM_TX_HASH}"]' page_size=null page_cursor=null  
    

### Retrieve deposit event by FT4 account ID:​
    
    
    chr query --api-url $NODE -brid $BRIDGE eif.hbridge.get_erc20_deposits -- 'filter=[null,null,x"{FT4_ACCOUNT_ID}",null,null]' page_size=null page_cursor=null  
    

note

To see additional filter fields for the deposit query, refer to [deposits.rell](<https://gitlab.com/chromaway/core/postchain-eif/-/blob/0.13.0/postchain-eif-rell/rell/src/hbridge/deposits.rell?ref_type=tags#L52>).

### Check for bounced deposit:​

If the deposit event is not found, check whether your deposit has bounced using the command below:
    
    
    chr query --api-url $NODE -brid $BRIDGE eif.hbridge.get_erc20_withdrawals -- filter='[null,null,x"{EVM_BENEFICIARY_ADDRESS}",null,null,null,null]' page_size=null page_cursor=null  
    

The deposit event may be bounced for the following reasons:

  1. The ERC-20 token is not registered on the Bridge Chain:
         
         chr query --api-url $NODE -brid $BRIDGE eif.hbridge.get_registered_erc20_assets -- network_id=56  
         

  2. The bridge contract is not registered on the Bridge Chain, or the ERC-20 token is not linked to the bridge contract:
         
         chr query --api-url $NODE -brid $BRIDGE eif.hbridge.get_bridge_contracts -- network_id=56  
         

  3. For native mode, if the deposit amount exceeds the bridge account balance.

  4. For deposits from a smart contract address, if the smart contract address is not linked to the FT4 account:
         
         chr query --api-url $NODE -brid $BRIDGE eif.hbridge.get_account_for_sc_address -- evm_address='x"{SMART_CONTRACT_ADDRESS}"' network_id=56  
         

or
         
         chr query --api-url $NODE -brid $BRIDGE eif.hbridge.get_sc_addresses_for_account -- account_id='x"{FT4_ACCOUNT_ID}"'  
         

  5. If the FT4 account is not created and the FT4 transfer strategy rules are not met, check the `lib.ft4.core.accounts.strategies.transfer` module arguments in the Bridge Chain configuration.

  6. If the FT4 account is not created and there is a pending deposit for the EOA address:
         
         chr query --api-url $NODE -brid $BRIDGE eif.hbridge.get_erc20_deposits -- 'filter=[null,null,x"{FT4_ACCOUNT_ID}",null,null]' page_size=null page_cursor=null  
         

Where `{FT4_ACCOUNT_ID}` can be calculated from the EOA address:
         
         chr repl -c 'x"{EOA_ADDRESS}".hash()'  
         

If the FT4 account exists but is not on the access list:
         
         chr query --api-url $NODE -brid $BRIDGE eif.hbridge.is_account_on_access_list -- account_id='x"{FT4_ACCOUNT_ID}"'  
         

## 6\. Verify account balance​

Allow some time for the deposit to be fully processed. Then, check your account balance on the Bridge chain and compare it with the expected amount.

### Find FT4 account ID by EVM address:​
    
    
    chr query --api-url $NODE -brid $BRIDGE eif.hbridge.get_account_for_eoa_address -- evm_address='x"{EOA_ADDRESS}"'  
    

or
    
    
    chr query --api-url $NODE -brid $BRIDGE eif.hbridge.get_account_for_sc_address -- evm_address='x"{SMART_CONTRACT_ADDRESS}"' network_id=56  
    

### Retrieve FT4 asset ID by symbol:​
    
    
    chr query --api-url $NODE -brid $BRIDGE ft4.get_assets_by_symbol -- symbol=CHR page_size=null page_cursor=null  
    

### Retrieve FT4 account balance:​
    
    
    chr query --api-url $NODE -brid $BRIDGE ft4.get_asset_balance -- account_id='x"{FT4_ACCOUNT_ID}"' asset_id='x"{FT4_ASSET_ID}"'

### Bridge withdrawal troubleshooting guide

SOURCE: https://docs.chromia.com/ecosystem/bridge/bridge-troubleshooting/bridge-withdraw-troubleshooting

If your withdrawal from Chromia to EVM doesn't appear on the EVM chain, follow these steps to identify and resolve the issue.

## 1\. Verify the withdrawal operation was successful on the Bridge Chain​

If you encounter the error `ERC20 token not found for network ID {...}, ft4 asset ID {...}, and ERC20 token address {...}`, it indicates that the ERC-20 token isn't registered on the Bridge chain. To verify the registration of the ERC-20 token, use the following CLI command:
    
    
    chr query --api-url $NODE -brid $BRIDGE eif.hbridge.get_registered_erc20_assets -- network_id=56  
    

If you receive the error `Can only withdraw to your own account`, it means that your FT4 account isn't linked to the EVM address.

To verify that your FT4 account is linked, use the following CLI command:
    
    
    # For EOA address  
    
    
    chr query --api-url $NODE -brid $BRIDGE eif.hbridge.get_eoa_addresses_for_account -- account_id='x"{FT4_ACCOUNT_ID}"'  
    
    
    
    # For smart contract address  
    
    
    chr query --api-url $NODE -brid $BRIDGE eif.hbridge.get_sc_addresses_for_account -- account_id='x"{FT4_ACCOUNT_ID}"'  
    

You can also check whether the withdrawal request was successfully created and is in the correct state with this command:
    
    
    chr query --api-url $NODE -brid $BRIDGE eif.hbridge.get_erc20_withdrawals -- filter='[null,null,x"{EVM_BENEFICIARY_ADDRESS}",null,null,null,null]' page_size=null page_cursor=null  
    

note

To see additional filter fields for the withdrawal query, refer to [withdrawals.rell](<https://gitlab.com/chromaway/core/postchain-eif/-/blob/0.13.0/postchain-eif-rell/rell/src/hbridge/withdrawals.rell?ref_type=tags#L71>).

## 2\. Verify the withdrawal request on the EVM side​

This section covers the most common issues that may arise when a withdrawal request can't be created on the EVM side.

  * `TokenBridge: blockchain rid is not set` – This error means that the Token Bridge wasn't initialized with the blockchain RID.

  * `TokenBridge: event hash was already used` – This error indicates that the withdrawal event has already been used for a withdrawal request.

  * `TokenBridge: block signature is invalid` – This error likely means that the validator set in the `Validator` or `ManagedValidator` contract is not updated.

To verify whether the validator set is current, use the following CLI command:
    
    
    postchain-eif-contracts$ npx hardhat inspect:bridge --network {NETWORK} --bridge-address {BRIDGE_ADDRESS}  
    

In this command, `{NETWORK}` can be one of `ethereum`, `bsc`, `sepolia`, or `bsc_testnet`. If the validator set differs from the one in the Directory Chain Validator contract, it indicates that it has not been updated.

The next step is to verify the `updateValidators` EVM transaction on the Transaction Submitter chain and manually re-submit the failed transaction.

## 3\. Complete withdrawal on the EVM side​

While completing the withdrawal on the EVM side, you may encounter the following errors:

  * `TokenBridge: no fund for the beneficiary` – This error occurs when attempting to withdraw funds for a beneficiary address that doesn't match the one specified in the withdrawal request. Ensure you are using the correct beneficiary address as stated during the creation of the withdrawal request.

  * `TokenBridge: not mature enough to withdraw the fund` – This error means that the required number of block confirmations hasn't yet been reached. The confirmation period is set during bridge initialization using the `--offset` parameter. The recommended value for Mainnet(s) is equivalent to 72 hours.

  * `TokenBridge: fund is pending or was already claimed` – This error indicates that either:

    * The withdrawal is still in a `Pending` state and isn't yet ready to be claimed.
    * The withdrawal has already been claimed previously and can't be claimed again (`Withdrawn` or `PostchainWithdrawn` states).

### Deploy the bridge

SOURCE: https://docs.chromia.com/ecosystem/bridge/deploy-bridge/

This section outlines the process of deploying the **Chromia** and **EVM components** of a token bridge, using **BSC testnet** as an example.

These steps can also be adapted for **Ethereum Mainnet, BSC Mainnet, or Ethereum Sepolia** by adjusting network configurations.

[Deploy an ERC20 token on an EVM chain (this guide uses BSC Testnet with the Test ALICE token).](</ecosystem/bridge/deploy-bridge/deploy-erc20-token>)[Deploy the token bridge contract on an EVM chain to enable cross-chain transfers.](</ecosystem/bridge/deploy-bridge/deploy-bridge-contract>)[Set up the Event Receiver Chain and deploy the Bridge Chain on Chromia to handle cross-chain transactions.](</ecosystem/bridge/deploy-bridge/deploy-bridge-chains>)[Connect the deployed bridges by setting the `bridge_demo_brid` in the EVM bridge contract.](</ecosystem/bridge/deploy-bridge/register-bridge>)[Use the frontend to verify your deployment, approve token transfers, and bridge tokens.](</ecosystem/bridge/deploy-bridge/interact-with-frontend>)

### Deploy Chromia bridge chains

SOURCE: https://docs.chromia.com/ecosystem/bridge/deploy-bridge/deploy-bridge-chains

This section describes deploying the Event Receiver Chain and the Bridge Chain on Chromia, enabling ERC20 token transfers between Chromia and EVM-compatible networks.

## Prerequisites​

Before proceeding, ensure you have the following:

  * **[Chromia CLI](</get-started/installation>)**

  * **[ERC20 token address](</ecosystem/bridge/deploy-bridge/deploy-erc20-token>)**

  * **[Deployed token bridge contrac](</ecosystem/bridge/deploy-bridge/deploy-bridge-contract>)**

  * **[Node.js](<https://nodejs.org/>)**

  * **[MetaMask](<https://metamask.io/>)** or a compatible wallet.

  * **[Chromia bridge demo repository](<https://bitbucket.org/chromawallet/chromia-bridge-demo/src/main/>)**

If you haven't cloned the repository yet, run:
    
    
    git clone https://bitbucket.org/chromawallet/chromia-bridge-demo.git  
    

## 1\. Navigate to the `rell` folder​

The following steps require you to be inside the `chromia-bridge-demo/rell` folder. Ensure you are in the correct directory:
    
    
    cd chromia-bridge-demo/rell  
    

## 2\. Install the FT4 Rell library​

Before proceeding, install the necessary dependencies using [`chr library install`](</build/cli/commands/library#library-install>):
    
    
    chr library install  
    

## 3\. Ensure GTX modules are included​

The GTX modules are essential for the bridge to function correctly, enabling transactions between Chromia and external blockchains. The bridge demo app already includes the correct GTX configuration in `chromia.yml`, so no changes are needed when following this guide. However, if you are setting up your chains, ensure that both the Event Receiver Chain and Bridge Chain include the required GTX modules and synchronization extensions.

### Check your `chromia.yml`​

Open `chromia.yml` and verify the following for each chain:

#### Event Receiver Chain​
    
    
    blockchains:  
    
    
      <event_receiver_chain>:  
    
    
        module: <module_name>  
    
    
        config:  
    
    
          gtx:  
    
    
            modules:  
    
    
              - "net.postchain.eif.EifGTXModule"  
    
    
              - "net.postchain.d1.icmf.IcmfSenderGTXModule"  
    
    
          sync_ext:  
    
    
            - "net.postchain.eif.EifSynchronizationInfrastructureExtension"  
    

#### Bridge Chain​
    
    
    blockchains:  
    
    
      <bridge_chain>:  
    
    
        module: <module_name>  
    
    
        config:  
    
    
          gtx:  
    
    
            modules:  
    
    
              - "net.postchain.eif.EifGTXModule"  
    
    
              - "net.postchain.d1.icmf.IcmfReceiverGTXModule"  
    
    
          sync_ext:  
    
    
            - "net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension"  
    

warning

If these modules are missing, deposits and withdrawals will not be processed correctly.

For further information about GTX and its role in Chromia's architecture, refer to the [Generic Transaction Protocol (GTX) documentation](</get-started/about/protocols/gtx>).

## 4\. Deploy the Event Receiver Chain to Testnet​

### Set the token bridge contract address and block height​

warning

The Event Receiver Chain name must be unique. Replace `<event_receiver_name>` with a custom identifier (e.g., `pirate_dapp_event_receiver`).

In your `chromia.yml`, update the configuration as follows:
    
    
    blockchains:  
    
    
      <event_receiver_name>:  
    
    
        module: event_receiver  
    
    
        config:  
    
    
          ...  
    
    
          eif:  
    
    
            chains:  
    
    
              bsc:  
    
    
                network_id: 97  
    
    
                contracts:  
    
    
                  - "<your_bridge_contract_address>"  
    
    
                skip_to_height: <latest_block_height>  
    

  * `<event_receiver_name>` — give this deployment a unique name.
  * `<your_bridge_contract_address>` — your deployed bridge contract address (must be quoted).
  * `<latest_block_height>` — a recent block height from the appropriate EVM chain which can be found on the evm explorer respectively.

note

ℹ️ The values `bsc` and `97` are defaults for **BSC Testnet**. You can adjust them if you are bridging to a different EVM network.

### Set your container ID​

Before deploying, ensure the `testnet` deployment block in your `chromia.yml` includes the correct container:
    
    
    deployments:  
    
    
      testnet:  
    
    
        brid: x"6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92"  
    
    
        url: https://node0.testnet.chromia.com  
    
    
        container: <container_id>  
    

You can obtain the testnet container by following this link:  
👉 [Get a testnet container](</get-started/create-dapp/deploy-to-testnet#lease-a-container>)

### Deploy the Event Receiver Chain to Testnet​

note

Make sure you have already created a [Testnet container](</get-started/create-dapp/deploy-to-testnet#lease-a-container>). It is required for deployment.

Run the following command to deploy the Event Receiver Chain to Testnet:
    
    
    chr deployment create --settings chromia.yml --network testnet --blockchain <event_receiver_name>  
    

or
    
    
    chr deployment create --settings chromia.yml --network testnet --blockchain <event_receiver_name> - - key-id=”<key file name>”  
    

By default, this command reads the `keyId` from the configuration file located at `.chromia/config`.

  * **Windows:** `C:\Users\<YourUsername>\.chromia\config`
  * **macOS and Linux:** `/Users/<YourUsername>/.chromia/config` or `/home/<YourUsername>/.chromia/config`

If you prefer to use a different configuration file, you can specify the path using the `--config` option:
    
    
    chr deployment create --settings chromia.yml --network testnet --blockchain <event_receiver_name> --config <config file path>  
    

You can also specify the Key ID by using the `--key-id=<your_key_id>` flag when invoking the command. This overrides all configuration files:
    
    
    chr deployment create --settings chromia.yml --network testnet --blockchain <event_receiver_name> --key-id=<your_key_id>  
    

You can also specify the Key ID by using the `--key-id=<your_key_id>` flag when invoking the command. This overrides all configuration files:
    
    
    chr deployment create --settings chromia.yml --network testnet --blockchain <event_receiver_name> --key-id=<your_key_id>  
    

You can also specify the Key ID by using the `--key-id=<your_key_id>` flag when invoking the command. This overrides all configuration files:
    
    
    chr deployment create --settings chromia.yml --network testnet --blockchain <event_receiver_name> --key-id=<your_key_id>  
    

Expected sample output after event receiver deployment:
    
    
    deployments:  
    
    
      testnet:  
    
    
        chains:  
    
    
          <event_receiver_name>: x"..."  
    

Make sure to save the event receiver BRID in your chromia.yml file under deployments, inside chains, using the unique name you gave your event receiver in the previous steps.

## 5\. Configure the Bridge Chain in `chromia.yml`​

Before deploying, update the configuration for the `<bridge_name>` chain in your `chromia.yml`:
    
    
    blockchains:  
    
    
      <bridge_name>:  
    
    
        ...  
    
    
        config:  
    
    
          ...  
    
    
          icmf:  
    
    
            receiver:  
    
    
              local:  
    
    
                - bc-rid: x"<event_receiver_brid>"  
    
    
                  topic: "L_evm_block_events"  
    
    
        moduleArgs:  
    
    
          bridge:  
    
    
            bsc_asset_network_id: 97  
    
    
            bsc_asset_address: x"<evm_token_address>"  
    
    
            asset_name: "Alice Test"  
    
    
            asset_symbol: "tALICE"  
    
    
            asset_decimals: 18  
    
    
            asset_icon: "https://s2.coinmarketcap.com/static/img/coins/64x64/8766.png"  
    
    
            bridge_address: x"<evm_bridge_contract_address>"  
    

Replace the following:

  * `<bridge_name>`: A unique name for your Bridge Chain.
  * `<event_receiver_brid>`: The RID output from deploying the Event Receiver Chain.
  * `<evm_token_address>`: The deployed ERC20 token address (hex-prefixed with x"").
  * `<evm_bridge_contract_address>`: The EVM bridge contract address (also prefixed with x"").

## 6\. Deploy the Bridge Chain to Testnet​

Run the following command to deploy the Bridge Chain to Testnet:
    
    
    chr deployment create --settings chromia.yml --network testnet --blockchain <bridge_name>  
    

or
    
    
    chr deployment create --settings chromia.yml --network testnet --blockchain <bridge_name>  - - key-id=”<key file name>”  
    

By default, this command reads the `keyId` from the configuration file located at `.chromia/config`.

  * **Windows:** `C:\Users\<YourUsername>\.chromia\config`
  * **macOS and Linux:** `/Users/<YourUsername>/.chromia/config` or `/home/<YourUsername>/.chromia/config`

If you prefer to use a different configuration file, you can specify the path using the `--config` option:
    
    
    chr deployment create --settings chromia.yml --network testnet --blockchain <bridge_name> --config <config file path>  
    

Expected sample output after event receiver deployment:
    
    
    deployments:  
    
    
      testnet:  
    
    
        chains:  
    
    
          <bridge_name>: x"..."  
    

Make sure to save the bridge BRID in your chromia.yml file under deployments, inside chains, using the unique name you gave your bridge in the previous steps.

## 7\. Initialize the Bridge Chain​

Run the following command:
    
    
    chr tx --api-url "https://node0.testnet.chromia.com" -brid <BRIDGE_BRID> init  
    

While this command is running, it performs the following steps:

  1. Initializes the Chromia bridge component.
  2. Registers the FT4 asset.
  3. Registers the ERC-20 token.
  4. Links the FT4 asset and ERC-20 token.
  5. Registers the bridge contract and configures it to work with the registered FT4 assets and ERC-20 tokens.

### Deploy the token bridge contract

SOURCE: https://docs.chromia.com/ecosystem/bridge/deploy-bridge/deploy-bridge-contract

This section walks through **deploying the token bridge contract on BSC Testnet** , enabling ERC20 token transfers between Chromia and EVM-compatible networks.

## Prerequisites​

Before proceeding, ensure you have the following:

  * **[Chromia CLI](</get-started/installation>)**
  * **[ERC20 token address](</ecosystem/bridge/deploy-bridge/deploy-erc20-token>)**
  * **[Node.js](<https://nodejs.org/>)**
  * **[MetaMask](<https://metamask.io/>)** or a compatible wallet.
  * **[Test BNB tokens](<https://www.bnbchain.org/en/testnet-faucet>)** or the respective native tokens for gas fees.
  * **[Postchain EIF contracts repository](<https://gitlab.com/chromaway/core/postchain-eif.git>)**

If you haven't cloned the repository yet, run:
    
    
    git clone https://gitlab.com/chromaway/core/postchain-eif.git  
    

## 1\. Set the directory validator address​

Set the **Directory Chain validator** address based on your target network:
    
    
    # BSC Testnet  
    
    
    export DIRECTORY_VALIDATOR=0x83dB85F7ef4447524D3A31c0F4664a89173C68Eb  
    
    
    
    # BSC Mainnet  
    
    
    export DIRECTORY_VALIDATOR=0xc755927508b7Ac3f7B31c9Ed396F3bE91C723d00  
    
    
    
    # Ethereum Mainnet  
    
    
    export DIRECTORY_VALIDATOR=0xc755927508b7Ac3f7B31c9Ed396F3bE91C723d00  
    

This guide focuses on **BSC Testnet** , but if deploying on **BSC Mainnet or Ethereum Mainnet** , use the appropriate validator address.

## 2\. Deploying validator contract​

Each bridge contract requires a validator contract. There are three types:

  * **Manually updated validator** :  
The contract owner updates the validator set manually. Suitable for testing or fixed validator configurations.

  * **Managed validator** :  
It is automatically updated by Chromia’s Transaction Submitter chain when validator changes occur. It is ideal for dapp developers building bridge applications.

  * **Directory Chain validator** :  
Used in custom Chromia networks to track the system cluster's validators. It is not deployed by dapp developers but is required when setting up managed validators.

### For manually updated validator contracts:​
    
    
    npx hardhat deploy:validator --network sepolia --verify --validators {VALIDATOR_0_ADDRESS},{VALIDATOR_1_ADDRESS},{VALIDATOR_2_ADDRESS}  
    

Here, `{VALIDATOR_i_ADDRESS}` represents the EVM address (with `0x` prefix) corresponding to the Chromia public key of `node_i`, and can be calculated using the following command:
    
    
    chr repl -c 'crypto.eth_pubkey_to_address(x"0338BB1915D6DD2E343524CF48CFBD2B53DB2A099D44FAD1D1206F516872754542")'  
    
    
    x"1B3821093FDCC3EFE225EF0835FE34DABABC60D3"  
    

### For managed validator contract​

If you already know the blockchain RID of your chain, you can supply it with `--blockchain-rid` (0x-prefixed):
    
    
    npx hardhat deploy:validator --network sepolia --verify --directory-validator {DIRECTORY_VALIDATOR_CONTRACT_ADDRESS}  
    

### Inspecting the validator contract​
    
    
    # For manually updated validators  
    
    
    npx hardhat inspect:validator --network sepolia --validator-address {VALIDATOR_CONTRACT_ADDRESS}  
    
    
    
    # For managed validators  
    
    
    npx hardhat inspect:managedValidator --network sepolia --validator-address {VALIDATOR_CONTRACT_ADDRESS}  
    
    
    
    # For directory chain validators  
    
    
    npx hardhat inspect:directoryValidator --network sepolia --validator-address {VALIDATOR_CONTRACT_ADDRESS}  
    

## 3\. Deploy the token bridge contract​

Run the following command to deploy the bridge contract:
    
    
    npx hardhat deploy:bridge --network bsc_testnet --validator-address $DIRECTORY_VALIDATOR --offset 2  
    

  * `--network bsc_testnet`: Deploys to BSC Testnet
  * `--validator-address $DIRECTORY_VALIDATOR`: Sets the directory validator contract
  * `--offset 2`: The dispute or withdrawal lock period (in blocks).

After deployment, the terminal will display the deployed contract addresses:
    
    
    Token bridge deployed to: 0x....................  
    
    
    Proxy admin address: 0x....................  
    

tip

Copy and save the **bridge contract address** for future use.

## 4\. Register the ERC20 token with the bridge​

After deploying the bridge contract, register your ERC20 token:
    
    
    npx hardhat allowToken:bridge --network bsc_testnet --bridge-address 0x{YOUR_BRIDGE_ADDRESS} --token-address 0x{YOUR_TOKEN_ADDRESS}  
    

This enables deposits and withdrawals for your ERC20 token on the bridge.

### Deploy an ERC20 token

SOURCE: https://docs.chromia.com/ecosystem/bridge/deploy-bridge/deploy-erc20-token

This section walks through deploying the `ALICE` ERC20 token on the BSC Testnet using Hardhat.

This is intended for users who don't have an existing ERC20 token and need one for testing.

## Prerequisites​

Before proceeding, ensure you have the following:

  * **[Chromia CLI](</get-started/installation>)**
  * **[Node.js](<https://nodejs.org/>)**
  * **[Hardhat](<https://www.npmjs.com/package/hardhat>)**
  * **[MetaMask](<https://metamask.io/>)** or a compatible wallet.
  * **[Test BNB tokens](<https://www.bnbchain.org/en/testnet-faucet>)** or the respective native tokens for gas fees.
  * **[Postchain EIF contracts repository](<https://gitlab.com/chromaway/core/postchain-eif.git>)**

    
    
    git clone https://gitlab.com/chromaway/core/postchain-eif.git  
    
    
    cd postchain-eif/postchain-eif-contracts  
    

## 1\. Install dependencies​
    
    
    npm install  
    

## 2\. Set up environment variables​

Create a `.env` file:
    
    
    cp .env.example .env  
    

### Generate a valid mnemonic​

Use the [BIP39 mnemonic generator](<https://iancoleman.io/bip39/>) to create a 12-word phrase.

Paste it into `.env`:
    
    
    MNEMONIC="your twelve-word mnemonic here"  
    
    
    REPORT_GAS=true  
    

### Configure API keys (Optional)​

Update `.env`:
    
    
    # Set API keys for Ethereum, BSC scan, and BASE scan  
    
    
    ETHERSCAN_API_KEY=your_etherscan_api_key  
    
    
    BSCSCAN_API_KEY=your_bscscan_api_key  
    
    
    BASESCAN_API_KEY=your_basescan_api_key  
    

  * Get `BSCSCAN_API_KEY` from [BscScan](<https://bscscan.com/myapikey>).

## 3\. Import your account into MetaMask​

### Derive the private key from your mnemonic​
    
    
    node -e "const { Wallet } = require('ethers'); console.log(Wallet.fromPhrase(process.argv[1]).privateKey)" "$(grep MNEMONIC .env | cut -d '=' -f2 | tr -d '"')"  
    

or for powershell
    
    
    $mnemonic = (Get-Content .env | Select-String -Pattern "^MNEMONIC=" | ForEach-Object { $_.Line -replace '^MNEMONIC=', '' } | ForEach-Object { $_ -replace '"', '' }); node -e "const { Wallet } = require('ethers'); console.log(Wallet.fromPhrase(process.argv[1]).privateKey)" $mnemonic  
    

### Import into MetaMask​

If you're setting up MetaMask for the first time:

  1. Open **MetaMask**.
  2. On the welcome screen, choose **“Import an existing wallet”**.
  3. Select **“Import account using private key”**.
  4. Paste the **private key** from the previous step.

If you already have an account in MetaMask:

  1. Click on your account avatar (top-right corner).
  2. Select **“Add account or hardware wallet”** → **“Import account”**.
  3. Paste the **private key** from the previous step.

## 4\. Compile the contract​
    
    
    npx hardhat compile  
    

## 5\. Deploy the ERC20 token​

Run the following command:
    
    
    npx hardhat deploy:alice --network bsc_testnet  
    

  * `deploy:alice`: Deploys the `ALICE` ERC20 token contract.
  * `--network bsc_testnet`: Deploys to Binance Smart Chain Testnet.

After successful deployment, the contract address will be displayed:
    
    
    Token deployed to: 0x....................  
    

warning

Copy and save the actual deployed contract address for later use.

### Interact with the frontend

SOURCE: https://docs.chromia.com/ecosystem/bridge/deploy-bridge/interact-with-frontend

This section guides you through configuring and running the frontend for the Chromia Bridge Demo.

## Prerequisites​

Before proceeding, ensure you have the following:

  * **[Chromia CLI](</get-started/installation>)**
  * **[Node.js](<https://nodejs.org/>)**
  * **[MetaMask](<https://metamask.io/>)** or a compatible wallet.
  * **[Chromia Bridge Demo repository](<https://bitbucket.org/chromawallet/chromia-bridge-demo/src/main/>)**

If you haven't cloned the repository yet, run:
    
    
    git clone https://bitbucket.org/chromawallet/chromia-bridge-demo.git  
    

To obtain the `<BRIDGE_DEMO_BRID>` for the Chromia bridge component:

  * If you **[deployed](</ecosystem/bridge/deploy-bridge/deploy-bridge-chains>)** the bridge, this is the BRID assigned during deployment.
  * If you **[leased](</ecosystem/bridge/bridge-lease>)** a bridge, you should have received this information.

If you're using the Economy Chain Bridge, use the

[Economy BRID](<https://explorer.chromia.com/mainnet/15C0CA99BEE60A3B23829968771C50E491BD00D2E3AE448580CD48A8D71E7BBA>) and the corresponding EVM-side contract addresses. For assistance, contact the Chromia support team. :::

## Navigate to the frontend folder​

The following steps require you to be inside the `chromia-bridge-demo/bridgefrontend` folder.

Ensure you are in the correct directory:
    
    
    cd chromia-bridge-demo/bridgefrontend  
    

## Set up the frontend configuration file​

Create a `.env` file:
    
    
    cp .env.example .env  
    

Next, edit the `.env` file with the required values:
    
    
    VITE_BRID=<BRIDGE_DEMO_BRID>  
    
    
    VITE_BRIDGE_ADDRESS=0x<YOUR_BRIDGE_EVM_ADDRESS>  
    
    
    VITE_TOKEN_ADDRESS=0x<YOUR_TOKEN_EVM_ADDRESS>  
    

  * `<BRIDGE_DEMO_BRID>`: The RID of the Chromia bridge component.
  * `<YOUR_BRIDGE_EVM_ADDRESS>`: The deployed EVM bridge contract address.
  * `<YOUR_TOKEN_EVM_ADDRESS>`: The ERC20 token contract address.

## Install dependencies​

Run the following command to install the necessary dependencies:
    
    
    npm install  
    

## Start the frontend app​

Launch the development server with the command:
    
    
    npm run dev  
    

The app will be accessible at <http://localhost:5173>.

## Import the token into MetaMask​

Before interacting with the bridge, you must import the test token into MetaMask (or your preferred wallet) to track your balances.

  1. Open MetaMask and **ensure you are on the BSC Testnet.**
  2. Click on **Import Token**.
  3. Enter the **token contract address** : `<YOUR_TOKEN_EVM_ADDRESS>`.
  4. Click **Next** and confirm the import.

### Verify your test token balance​

  * If you deployed the **ALICE test token** , your balance should start at **100,000 ALICE**.
  * If you are using a different test token, check the initial minting rules for that contract.

After verifying your balance, you can proceed to account setup.

## Initialize FT4 account​

  1. Click **Setup** to create an FT4 account linked to your EVM account.
  2. If the page reloads, click **Login**.

note

This frontend demo does not save its internal state or track transactions as a production environment would. If you refresh the page, it will lose all information about ongoing transactions.

However, you can still bridge tokens again after a reload; just start a new flow from the beginning.

## Approve token spending​

info

The **test ALICE token contract** has **18 decimal places** , meaning **3000 tokens** must be entered as:
    
    
    3000000000000000000000  
    

  1. Enter the value needed to approve **spending 3000 tokens**.
  2. Click **Approve spending of 3e+21 tokens by the Bridge**.
  3. Confirm the action in MetaMask.

## Bridge tokens from EVM to Chromia​

  1. To bridge **3000 tokens** , enter the following value:
         
         3000000000000000000000  
         

  2. Click **Bridge 3e+21 tokens to Chromia**.
  3. Confirm the transaction in MetaMask.

## Set up shell environment variables for easier queries​

To avoid copy-pasting the blockchain RIDs each time, set them **once** before running queries:
    
    
    export NODE=https://node0.testnet.chromia.com  
    
    
    
    export ER=<EVENT_RECEIVER_BRID>  
    
    
    
    export BRIDGE=<BRIDGE_DEMO_BRID>  
    

  * **`NODE`** : The URL of the Chromia node that handles transactions and queries.
  * **`ER`** : The RID of the `event_receiver` blockchain, which tracks token deposits and withdrawals.
  * **`BRIDGE`** : The RID of the `bridge` blockchain, which manages token bridging between Chromia and EVM.

## Verify token transfer​

  1. Ensure that **3000 tokens have been deducted** from your MetaMask balance.
  2. Check the latest deposits on Chromia by running:
         
         chr query --api-url $NODE -brid $BRIDGE eif.hbridge.get_erc20_deposits filter=[null,null,null,null,null] page_size=null page_cursor=null  
         

  3. Check your FT4 account balance. Use the values retrieved from the previous step to complete the following command:
         
         chr query --api-url $NODE -brid $BRIDGE ft4.get_asset_balance account_id=<YOUR_ACCOUNT_ID> asset_id=<YOUR_ASSET_ID>  
         

## Bridge tokens back to EVM​

  1. To bridge **1500 tokens** back, enter the following value:
         
         1500000000000000000000  
         

  2. Click **Bridge 1.5e+21 tokens to EVM** and confirm the action in MetaMask.
  3. Check for the withdrawal event by running:
         
         chr query --api-url $NODE -brid $BRIDGE eif.hbridge.get_erc20_withdrawals filter=[null,null,null,null,null,null,null] page_size=null page_cursor=null  
         

  4. Click **Request Withdraw from EVM** and confirm the transaction in MetaMask.
  5. Click **Withdraw from bridge** and confirm with MetaMask.
  6. Open MetaMask and verify that **1500 tokens have been restored** to your balance.

You have now successfully set up and configured the Chromia Bridge Demo to transfer tokens between the EVM and Chromia networks.

### Register the Chromia bridge on EVM

SOURCE: https://docs.chromia.com/ecosystem/bridge/deploy-bridge/register-bridge

To enable transfers between networks, we need to register the **Chromia bridge component** on the EVM bridge contract so that the EVM side recognizes it.

## Prerequisites​

Before proceeding, ensure you have the following:

  * **[Deployment or leasing of the Chromia bridge component](</ecosystem/bridge/deploy-bridge/deploy-bridge-chains>)** completed.

  * **[Chromia CLI](</get-started/installation>)**

  * **[Node.js](<https://nodejs.org/>)**

  * **[MetaMask](<https://metamask.io/>)** or a compatible wallet.

  * **[Postchain EIF contracts repository](<https://gitlab.com/chromaway/core/postchain-eif.git>)**

  * **The`<BRIDGE_DEMO_BRID>` for the Chromia bridge component.**

    * If you **[deployed](</ecosystem/bridge/deploy-bridge/deploy-bridge-chains>)** the bridge, this is the BRID assigned during deployment.
    * If you **[leased](</ecosystem/bridge/bridge-lease>)** a bridge, this should be provided to you.

## Set the Bridge Chain RID on the EVM bridge contract​

Now that we have the Bridge Chain, we need to configure the EVM bridge contract to recognize this blockchain.

First, ensure you are inside the `postchain-eif-contracts` directory:
    
    
    cd postchain-eif/postchain-eif-contracts  
    

Next, run the following command to link the Bridge Chain to the bridge contract:
    
    
    npx hardhat setBlockchainRid:bridge --network bsc_testnet --address 0x<YOUR_BRIDGE_EVM_ADDRESS> --blockchain-rid 0x<BRIDGE_DEMO_BRID>  
    

Run the following command to link the Bridge Chain to the bridge contract (include `--managed-validator 0x<YOUR_BRIDGE_VALIDATOR_ADDRESS>` if using a managed validator):
    
    
    npx hardhat setBlockchainRid:bridge --network bsc_testnet --address 0x<YOUR_BRIDGE_EVM_ADDRESS> --blockchain-rid 0x<BRIDGE_DEMO_BRID> --managed-validator 0x<YOUR_BRIDGE_VALIDATOR_ADDRESS>  
    

info

Remember to add the `0x` prefix when entering `<BRIDGE_DEMO_BRID>` to ensure the EVM contract accepts it.
    
    
    --blockchain-rid 0x....................  
    

This command updates the **EVM bridge contract** so it recognizes `<BRIDGE_DEMO_BRID>`.

### Mass exit

SOURCE: https://docs.chromia.com/ecosystem/bridge/mass-exit/

This section explains how to secure user funds using snapshot-based withdrawals in the event of validator compromise—while often described as an advanced bridging feature for recovery and fault tolerance, a true mass exit is an emergency contingency triggered when a supermajority of validators is compromised, serving as a last-resort measure rather than a planned feature.

[Understand what mass exit entails, its use cases, and its role in protecting funds during severe security incidents.](</ecosystem/bridge/mass-exit/overview>)[A step-by-step guide for configuring your EVM bridge contract and Chromia chains to support snapshot-based withdrawals during a mass exit scenario.](</ecosystem/bridge/mass-exit/setup>)[Instructions for triggering a mass exit, managing snapshot-based withdrawals, and implementing cross-chain recovery processes.](</ecosystem/bridge/mass-exit/operations>)[A reference guide for the CLI commands, Rell operations, and EVM contract calls used in managing mass exit flows.](</ecosystem/bridge/mass-exit/reference>)

### Mass exit operations

SOURCE: https://docs.chromia.com/ecosystem/bridge/mass-exit/operations

This section explains how to trigger the mass exit process and enable users to withdraw their funds using account state snapshots.

When a mass exit is triggered, standard deposits and withdrawals are immediately blocked, and the bridge enters mass exit mode. Users must then obtain account state proofs from the Chromia side as of the last known valid block, and submit them to the EVM contract to withdraw their funds.

## Step 1: Triggering mass exit​

The bridge owner can trigger the mass exit.

### Basic form (for trusted validator set):​
    
    
    function triggerMassExit(  
    
    
        bytes memory blockHeader,              //The block header from the most recent valid block  
    
    
        bytes[] memory sigs,                   //Validator signatures  
    
    
        address[] memory signers,              //Validator public keys  
    
    
        Data.ExtraProofData memory extraProof  //Extra proof data containing the Merkle root hash of the account state snapshot tree  
    
    
    )  
    

### If the validator contract has been hijacked, use historical validators to verify the block header:​
    
    
    function triggerMassExitWithHistoricalValidators(  
    
    
        bytes memory blockHeader,              //The block header from the most recent valid block  
    
    
        bytes[] memory sigs,                   //Validator signatures  
    
    
        address[] memory signers,              //Validator public keys  
    
    
        Data.ExtraProofData memory extraProof, //Extra proof data containing the Merkle root hash of the account state snapshot tree  
    
    
        address[] memory historicalValidators  //Historical validator addresses used to verify the block header  
    
    
    )  
    

  * Ensure that the `blockHeader` corresponds to a valid block no older than 3 days.
  * The signatures must align with the validator set at the time of that block.

> ✅ Once triggered, deposits and standard withdrawals are disabled.  
>  ✅ Snapshot-based withdrawal becomes the only supported method.

## Step 2: Withdrawing funds using account state snapshots​

### Retrieve account state proof​

Each account may use multiple state slots, corresponding to different networks, bridge contracts, protocols, etc. To get the account state proof, a user must first retrieve the relevant state slot IDs:
    
    
    val state_slot_ids = hbridge.get_state_slot_ids_for_address(  
    
    
        beneficiary,                   //The beneficiary's address  
    
    
        network_id                     //The EVM network ID  
    
    
    );  
    

Then, the account state proof can be retrieved using the following query:
    
    
    chr query -brid $BRIDGE get_account_state_merkle_proof \  
    
    
        -- blockHeight=$MASS_EXIT_BLOCK_HEIGHT \  
    
    
        accountNumber=$ACCOUNT_STATE_SLOT_ID  
    

Replace `$MASS_EXIT_BLOCK_HEIGHT` with the mass exit block height, and `$ACCOUNT_STATE_SLOT_ID` with the specific account state slot ID.

### Submit account state proof on EVM​

Submit the snapshot proof on the EVM to withdraw funds.
    
    
    function withdrawBySnapshot(  
    
    
        bytes calldata snapshot,       //Snapshot data of the account state  
    
    
        Data.Proof memory stateProof   //Merkle proof for the snapshot  
    
    
    )  
    

This function allows users to withdraw their balances from the bridge based on the snapshot state.

## Step 3: Complete in-progress withdrawals​

If a user initiated a withdrawal **before** the mass exit, they can complete it by following these steps:

### Get withdrawal by transaction + index​

Retrieve the specific withdrawal details using the transaction RID and operation index.
    
    
    val withdrawal = hbridge.get_erc20_withdrawal_by_tx(  
    
    
        tx_rid,                        //Rell transaction RID  
    
    
        op_index                       //Operation index within that transaction  
    
    
    );  
    

### Get event proof​

Obtain the Merkle proof for the withdrawal event.
    
    
    chr query -brid $BRIDGE get_event_merkle_proof \  
    
    
        -- eventHash=$WITHDRAWAL_EVENT_HASH  
    

Replace `$WITHDRAWAL_EVENT_HASH` with the hash of the withdrawal event.

### Retrieve withdrawal state slot IDs​

Retrieve the state slot IDs associated with the beneficiary's withdrawals.
    
    
    val slot_ids = hbridge.get_withdrawal_state_slot_ids_for_address(  
    
    
        beneficiary,                   //The beneficiary's address  
    
    
        network_id                     //The EVM network ID  
    
    
    );  
    

### Obtain account state proof again​

To get the account state proof, use the following query.
    
    
    chr query -brid $BRIDGE get_account_state_merkle_proof \  
    
    
        -- blockHeight=$MASS_EXIT_BLOCK_HEIGHT \  
    
    
        accountNumber=$ACCOUNT_STATE_SLOT_ID  
    

### Complete the withdrawal​

To complete the withdrawal initiated before the mass exit, use the `completeWithdrawalBySnapshot` function:
    
    
    function completeWithdrawalBySnapshot(  
    
    
        bytes calldata _stateRecord,   //Account state data; contains header and a list of withdrawal event hashes  
    
    
        uint64 n,                      //Index of the withdrawal hash  
    
    
        bytes memory _event,           //Withdrawal event  
    
    
        Data.Proof memory stateProof   //Snapshot proof  
    
    
    )  
    

## Step 4: Emergency withdrawal period​

After a mass exit is triggered, users have **90 days** to submit snapshot proofs for their balances.

warning

If a balance lacks an associated account state snapshot—due to a user not linking their EVM account with an FT4 account, or a misconfiguration on the Rell side—the snapshot-based withdrawal will fail. Following the 90-day period, the bridge owner can reclaim and withdraw those funds.

In this context, `misconfiguration` refers to an ERC‑20 token mistakenly registered without snapshot support. Snapshots are only supported when a single ERC‑20 token is linked to an FT4 asset and not across multiple EVM networks.

## Step 5: Pausing the bridge​

Any validator can temporarily pause the bridge if they detect unusual behaviour. This action immediately freezes deposits and withdrawals, allowing time to investigate potential issues and decide whether a mass exit is necessary. The bridge owner can later unpause the bridge if the anomaly turns out to be a false positive.

### How mass exit works

SOURCE: https://docs.chromia.com/ecosystem/bridge/mass-exit/overview

Mass exit serves as a critical emergency mechanism in the Chromia token bridge, safeguarding user funds when a crisis threatens the network's integrity.

Under normal conditions, Chromia validators on the Chromia side sign blocks, and once a commitment is submitted, a 72‑hour challenge window gives the bridge operator time to detect any fraudulent behavior and block unauthorized withdrawals.

However, if a supermajority of validators were ever to become compromised and begin signing invalid blocks, the bridge operator can trigger mass exit mode. In that mode, pre‑taken snapshots of account states are used to facilitate a secure exit for all users.

## When to use mass exit​

Use mass exit only as a last-resort mechanism in extreme situations, such as:

  * Discovering collusion or compromise among validators
  * Observing suspicious behaviors from validators that jeopardize bridge funds
  * Experiencing an irrecoverable security incident that affects consensus integrity

note

In these scenarios, the bridge owner or a designated multisig or governance-controlled administrator initiates a mass exit by referencing a previously signed block known to be valid.

## What happens in mass exit mode​

Once you trigger mass exit:

  * Deposits are blocked.
  * Standard withdrawals are blocked.
  * Users must withdraw funds using a snapshot proof of their account balance from the mass exit block. The snapshot is stored as a Merkle tree, with the root hash included in the block header.
  * FT4 tokens on the Chromia side, issued as equivalents to assets locked in the token bridge, remain abandoned. The number of abandoned tokens on the account can be arbitrary because a user may transfer existing tokens or receive additional tokens from other accounts.

This approach ensures that the system does not accept any transactions after a compromise. Users can then recover their recorded balances at a secure checkpoint.

## Security assumptions​

Mass exit relies on these trust and architectural principles:

  * Validators are recognized, staked participants with strong economic incentives to behave honestly.
  * The bridge owner operates as a separate entity, ideally in the form of a multisig or DAO contract.
  * It is highly unlikely that both the bridge owner and a supermajority of validators will be compromised simultaneously.

## Relationship to other bridge operations​

You do not need to implement mass exit in regular bridging scenarios.

You must explicitly enable and configure it using contracts that support snapshots and the appropriate chain setup. If you do not plan to utilize this feature, you can safely omit it.

### Mass exit API and CLI reference

SOURCE: https://docs.chromia.com/ecosystem/bridge/mass-exit/reference

This topic summarizes key CLI commands, Rell queries, and EVM contract calls used throughout the mass exit flow.

## EVM contract functions​

### Trigger mass exit​
    
    
    function triggerMassExit(  
    
    
      bytes memory blockHeader,     //Recent, verified block header  
    
    
      bytes[] memory sigs,          //Validator signatures endorsing the header  
    
    
      address[] memory signers,     //Validator addresses corresponding to the signatures  
    
    
      Data.ExtraProofData memory extraProof //Additional proof data if needed  
    

### Trigger with historical validators​
    
    
    function triggerMassExitWithHistoricalValidators(  
    
    
      bytes memory blockHeader,          //Recent, verified block header  
    
    
      bytes[] memory sigs,               //Validator signatures endorsing the header  
    
    
      address[] memory signers,          //Validator addresses corresponding to the signatures  
    
    
      Data.ExtraProofData memory extraProof,//Additional proof data if needed  
    
    
      address[] memory historicalValidators //Historical validator addresses used for verification  
    
    
    )  
    

### Withdraw via snapshot​
    
    
    function withdrawBySnapshot(  
    
    
      bytes calldata snapshot,       //Snapshot data capturing the account state  
    
    
      Data.Proof memory stateProof   //Merkle proof validating the snapshot  
    
    
    )  
    

### Complete in-progress withdrawal​
    
    
    function completeWithdrawalBySnapshot(  
    
    
      bytes calldata _stateRecord,   //Account state record data (header and withdrawal event hashes)  
    
    
      uint64 n,                      //Index of the specific withdrawal event in the record  
    
    
      bytes memory _event,           //Withdrawal event data  
    
    
      Data.Proof memory stateProof   //Merkle proof for the account state record  
    
    
    )  
    

## Chromia CLI and queries​

### Get account state proof​
    
    
    chr query -brid $BRIDGE get_account_state_merkle_proof \  
    
    
      -- blockHeight=$BLOCK_HEIGHT \  
    
    
         accountNumber=$ACCOUNT_STATE_SLOT_ID  
    

### Get state slot IDs for user balance​
    
    
    val state_slot_ids = hbridge.get_state_slot_ids_for_address(  
    
    
      beneficiary,//EVM address of the user  
    
    
      network_id  //Identifier of the EVM network  
    
    
    );  
    

### Get state slot IDs for withdrawal​
    
    
    val slot_ids = hbridge.get_withdrawal_state_slot_ids_for_address(  
    
    
      beneficiary,//EVM address of the user  
    
    
      network_id  //Identifier of the EVM network  
    
    
    );  
    

### Get withdrawal by tx + op index​
    
    
    val withdrawal = hbridge.get_erc20_withdrawal_by_tx(  
    
    
      tx_rid,   //Rell transaction RID  
    
    
      op_index  //Operation index within the transaction  
    
    
    );  
    

### Get withdrawal event proof​
    
    
    chr query -brid $BRIDGE get_event_merkle_proof \  
    
    
      -- eventHash=$WITHDRAWAL_EVENT_HASH  
    

## Cross-Chain recovery​

### Deploy recovery contract​
    
    
    npx hardhat deploy:recovery --network <network> \  
    
    
      --validator-address <VALIDATOR_CONTRACT_ADDRESS>  
    

### Set recovery contract RID​
    
    
    npx hardhat setBlockchainRid:recovery \  
    
    
      --network <network> \  
    
    
      --address <RECOVERY_CONTRACT_ADDRESS> \  
    
    
      --blockchain-rid <DOWNSTREAM_CHAIN_RID>  
    

### Register recovery contract on downstream​
    
    
    chr tx -brid $DOWNSTREAM_CHAIN_RID register_recovery_contract \  
    
    
      -- $NETWORK_ID x"'$RECOVERY_CONTRACT_ADDRESS'"  
    

### Register it upstream via ICCF on Bridge Chain​
    
    
    chr tx -brid $BRIDGE --iccf-tx $ICCF_TX_RID \  
    
    
      --iccf-source $DOWNSTREAM_CHAIN_RID \  
    
    
      register_recovery_contract -- 0

### Set up mass exit

SOURCE: https://docs.chromia.com/ecosystem/bridge/mass-exit/setup

This guide helps you enable the mass exit mechanism by configuring your bridge contracts and Chromia chains for snapshot-based withdrawals. Remember that you must prepare for mass exit in advance; you cannot retrofit it after deployment. If your bridge isn't set to snapshot mode, you won't be able to trigger a mass exit.

## Prerequisites​

Before you start, make sure that:

  * You have an EVM-compatible bridge set up or in progress.
  * You are using a bridge contract that supports snapshot-based withdrawals, such as `TokenBridgeWithSnapshotWithdraw`.
  * Your Chromia Bridge Chain registers tokens in foreign mode with `use_snapshots: true`.

### Token modes and mass exit implications​

**Foreign mode (EVM-originated tokens):**

  * The token originates on EVM.
  * FT4 tokens are **minted** when moving from EVM to Chromia.
  * FT4 tokens are **burned** when moving from Chromia to EVM.
  * **Snapshot-based mass exit is supported** and required to ensure asset recovery in case of bridge failure.
  * Simpler and preferred mode for enabling mass exits.

**Native mode (Chromia-originated tokens):**

  * The token originates on Chromia.
  * FT4 tokens are **locked** in a special blockchain account when moving from Chromia to EVM.
  * FT4 tokens are **released** from that account when moving from EVM to Chromia.
  * This ensures total supply is protected against faulty EVM contracts.
  * **Mass exit is not supported** in this mode. If Chromia is compromised, the demand to exit native assets to EVM is expected to be low.

### Step 1: Deploy snapshot-ready EVM bridge​

To deploy the EVM bridge contract with snapshot support, run the following command:
    
    
    npx hardhat deploy:snapshots \  
    
    
        --network <NETWORK> \  
    
    
        --verify \  
    
    
        --validator-address <VALIDATOR_CONTRACT_ADDRESS> \  
    
    
        --offset 2  
    

  * Use one of the supported networks: Ethereum (`ethereum`, `sepolia`), BSC (`bsc`, `bsc_testnet`), Base (`base`, `base_sepolia`).
  * Replace `<VALIDATOR_CONTRACT_ADDRESS>` with the address of your validator or directory validator contract.
  * The `--offset` parameter sets the dispute period (lockup interval) in blocks, and a 72-hour equivalent is recommended.

### Step 2: Register FT4 asset on the Bridge Chain​

In your Rell deployment on the Chromia Bridge Chain, register the asset using the following code:
    
    
    val asset = ft4.assets.Unsafe.register_asset(  
    
    
        name,                          //the name of the asset  
    
    
        symbol,                        //the symbol of the asset  
    
    
        decimals,                      //the decimals of the asset  
    
    
        blockchain_rid,                //the RID of the asset issuing blockchain  
    
    
        icon_url,                      //the URL of the asset icon  
    
    
        type                           //the type of the asset, defaults to `ASSET_TYPE_FT4`  
    
    
    );  
    

### Step 3: Register ERC-20 token with snapshot support​

Next, register your ERC-20 token on the Bridge Chain with this operation:
    
    
    val erc20_asset = hbridge.register_erc20_asset(  
    
    
        network_id,                    //EVM network ID (e.g., 97 for BSC Testnet)  
    
    
        token_address,                 //Address of the deployed ERC-20 contract  
    
    
        asset,                         //FT4 asset returned from the previous step  
    
    
        bridge_mode.foreign,           //ERC-20 token must be registered as a `foreign` asset.  
    
    
        true                           //Set `use_snapshots` to `true` to enable mass exit functionality  
    
    
    );  
    

note

Setting `use_snapshots = true` is essential for enabling mass exit support.

### Step 4: Create a bridge contract​

While still in the initialization phase, create a bridge contract:
    
    
    val bridge_contract = hbridge.get_or_create_bridge(  
    
    
        network_id,                  //EVM network ID  
    
    
        bridge_address               //Address where the bridge contract is deployed  
    
    
    );  
    

### Step 5: Bind ERC-20 to the bridge contract​

While still in the initialization phase, bind the ERC-20 asset to the bridge contract using this code:
    
    
    create bridge_erc20_asset(  
    
    
        bridge_contract,               //The bridge contract  
    
    
        erc20_asset                    //The ERC-20 token to bind to the bridge contract  
    
    
    );  
    

### Step 6: Cross-chain mass exit (recovery contract)​

If your blockchain does not support bridge functionality but receives bridged tokens from the Bridge Chain via cross-chain transfers, you must register a _recovery contract_ on the Bridge Chain to ensure that users on your chain can recover their assets in the event of a mass exit. Chains that receive tokens from the Bridge Chain are referred to as _downstream chains_.

To deploy a recovery contract use the following command:
    
    
    npx hardhat deploy:recovery \  
    
    
        --network <NETWORK> \  
    
    
        --verify \  
    
    
        --validator-address <BRIDGE_VALIDATOR_CONTRACT_ADDRESS>  
    

To obtain the bridge validator contract address, execute the `inspect:bridge` script:
    
    
    $ npx hardhat inspect:bridge --network <NETWORK> --bridge-address <BRIDGE_CONTRACT_ADDRESS>  
    

#### Set the RID for the downstream chain on the recovery contract​
    
    
    npx hardhat setBlockchainRid:recovery \  
    
    
        --network <NETWORK> \  
    
    
        --address <RECOVERY_CONTRACT_ADDRESS> \  
    
    
        --blockchain-rid <DOWNSTREAM_CHAIN_RID>  
    

#### Register the cross-chain FT4 asset on the downstream chain​
    
    
    val asset = crosschain.Unsafe.register_crosschain_asset(  
    
    
        id,                            //The ID of the asset to be registered  
    
    
        name,                          //The name of the asset to be registered  
    
    
        symbol,                        //The symbol of the asset to be registered  
    
    
        decimals,                      //The decimals of the asset to be registered  
    
    
        issuing_blockchain_rid,        //The blockchain RID of the issuing chain  
    
    
        icon_url,                      //The URL of the icon for the asset  
    
    
        type,                          //The type of the asset to be registered (see FT4 docs)  
    
    
        uniqueness_resolver,           //The uniqueness resolver for the asset (see FT4 docs)  
    
    
        origin_blockchain_rid          //The blockchain we'll receive this asset from  
    
    
                                        // (might not be the same as issuing_blockchain_rid)  
    
    
    );  
    

#### Register the ERC-20 token with snapshot support​

Lastly, to register ERC-20 token with snapshot support on the downstream chain, register steps 3-5 on the downstream chain.

#### Register recovery contract​

Registering a recovery contract on the Bridge Chain requires ICCF authentication, so the recovery contract must first be registered on the downstream chain.

To register the recovery contract on the downstream chain, use the following command:
    
    
    chr tx -brid $DOWNSTREAM_CHAIN_RID register_recovery_contract \  
    
    
        -- $NETWORK_ID x"'$RECOVERY_CONTRACT_ADDRESS'"  
    

Next, register it on the bridge chain (or upstream chain) using ICCF authentication:
    
    
    chr tx -brid $BRIDGE --iccf-tx $ICCF_TX_RID \  
    
    
        --iccf-source $DOWNSTREAM_CHAIN_RID \  
    
    
        register_recovery_contract -- 0  
    

With all of these steps completed, mass exit functionality is now enabled.

### EVM bridge

SOURCE: https://docs.chromia.com/ecosystem/bridge/overview

Chromia provides a bridging mechanism called Ethereum Interoperability Framework (EIF) that allows transferring ERC-20 tokens between Ethereum-compatible networks and the Chromia ecosystem. The EIF itself represents a GTX (Generic Transaction Protocol) module that can be added to the dApp, extending its original functionality.

The EIF GTX module performs the following activities:

  * Reads events from the EVM, supports multiple EVM networks, multiple contracts, and multiple contract events.
  * Validates EVM events.
  * Adds EVM events to the postchain block using the special operation `__evm_block`.

The EIF framework can be divided into the following components:

Component| Description  
---|---  
**Token Bridge**|  Acts as a vault that stores received tokens. Once ERC-20 tokens are deposited, they are locked. It supports depositing to and withdrawing from the smart contract.  
**Event Receiver Chain**|  A Chromia blockchain that listens to events from the Token Bridge and inserts them into Rell using the `__evm_block` operation. It provides state information to the Bridge Chain based on specific [events](<https://gitlab.com/chromaway/core/postchain-eif/blob/497b00c92844e033c4129b4eb6caca4a244a6079/postchain-eif-contracts/contracts/TokenBridge.sol#L58>).  
**Bridge Chain**|  Reads EVM events from the Event Receiver Chain via ICMF or directly if embedded. It tracks ERC-20 tokens, accounts, deposits, and withdrawals. It also provides proofs to unlock tokens and supports snapshots for foreign tokens.  
**Dapps**|  Dapp accounts receive newly minted tokens on the Bridge Chain through cross-chain transfers.  
  
For a visual representation of the EIF framework and the token bridge, see the diagram below:

## Bridge types​

A developer can choose between two bridge types, depending on where the token was initially minted:

  1. [Token Bridge](<https://gitlab.com/chromaway/core/postchain-eif/-/blob/dev/postchain-eif-contracts/contracts/TokenBridge.sol>) \- is used when the token was initially issued on the EVM chain.
  2. [Chromia Token Bridge](<https://gitlab.com/chromaway/core/postchain-eif/-/blob/dev/postchain-eif-contracts/contracts/ChromiaTokenBridge.sol>) \- is used when the token was initially issued on the Chromia blockchain.

The Token Bridge is a basic bridge contract used for transferring ERC-20 tokens. When users deposit tokens into the token bridge, the contract locks them and the corresponding tokens are minted on the Chromia side. Upon withdrawal, the bridge burns the tokens on Chromia and unlocks or transfers them back to the user on the EVM side.

Bridges remain secure through withdrawal proofs signed by Chromia nodes via block headers, resembling a multi-signature setup. If malicious actors compromise a majority of nodes for EVM-originating tokens, honest nodes can initiate a mass exit, allowing users to recover their balances based on a previously known state. This design makes it difficult for attackers to manipulate the bridge’s balance.

## Transfer mechanisms​

Bridges operate in two modes, based on the source chain of the token transfer:

  1. **Lock and mint** : Lock tokens on the source chain and mint them on the destination chain.
  2. **Burn and release** : Burn tokens on the source chain and release them on the destination chain.

### Lock and mint​

  1. **Deposit** \- On the EVM chain, users deposit tokens (ERC-20 or ERC-721) into the `EMV Token Bridge` contract.
  2. **Event monitoring** \- The Event Receiver Chain detects the deposit event and inserts it into Chromia using the `__evm_block` operation.
  3. **Processing** \- The bridge chain processes the event by decoding and validating the deposit.
  4. **Mint** \- The bridge mints equivalent tokens on Chromia.

The burn and release process occurs when users want to transfer tokens back to an EVM-compatible chain.

### Burn and release​

  1. **Burn** \- On Chromia, the user initiates a withdrawal by burning their tokens.
  2. **Generate proof** \- The user queries and generates proof of the withdrawal.
  3. **Withdrawal request** \- The user submits the proof to the Token Bridge contract to initiate withdrawal.
  4. **Lockup period** \- The contract verifies the proof and starts a configured security lockup period.
  5. **Fund release** \- After the lockup period expires, the bridge automatically releases the funds to the user’s address.

## 4. Filehub

Compiled from official extracted article bodies. Not invented.

### Configure and work with Filehub

SOURCE: https://docs.chromia.com/ecosystem/filehub/configure-filehub/

This section covers how to configure Filehub, including Filechain registration, payment setup, and enabling/disabling storage, as well as working with files stored in Filehub.

[Set up a Filehub instance for file storage and cross-chain payments. Initialize with the Directory Node URL and blockchain RID, set admin privileges, and register Filechains.](</ecosystem/filehub/configure-filehub/filehub-configure>)[Use Filehub's NPM package and Gateway for on-chain file storage and retrieval. Manage Filechains and payments with Filehub APIs, with a $0.10/MB fee for permanent storage.](</ecosystem/filehub/configure-filehub/filehub-work>)

### Configure Filehub

SOURCE: https://docs.chromia.com/ecosystem/filehub/configure-filehub/filehub-configure

This topic describes the steps to set up and manage a Filehub instance effectively. It covers how to register Filechains, toggle file storage payments on or off, and establish cross-chain asset payments. Proper setup ensures streamlined operations in file storage, payment handling, and Filechain management.

## Step 1: Initialize Filehub​

Start by initializing your Filehub with the required parameters. These should include the URL for the Directory Node and the specific blockchain RID for your Filehub instance.

note

Make sure to install the filehub package via npm:
    
    
    npm install filehub  
    
    
    
    const filehub = new Filehub({  
    
    
      directoryNodeUrlPool: DIRECTORY_NODE_URL_POOL, //URL for Directory Node  
    
    
      blockchainRid: FILEHUB_BLOCKCHAIN_RID, //Blockchain RID for the Filehub  
    
    
    });  
    

## Step 2: Set up admin privileges​

Use the private key of the Filehub or Filechain administrator to grant admin privileges, depending on which operations you need to perform.

note

Make sure to install the [postchain-client](<https://www.npmjs.com/package/postchain-client>) package via npm if you haven't already:
    
    
    npm install postchain-client  
    
    
    
    const admin = newSignatureProvider({ privKey: FILEHUB_ADMIN_PRIVKEY });  
    
    
    const filehubAdministrator = new FilehubAdministrator(filehub, admin);  
    

## Step 3: Register a Filechain​

To enable a Filechain for file storage operations, register it with your Filehub. Providing an EVM address during registration assigns administrative control over the Filechain and allows for fee collection.
    
    
    await filehubAdministrator.registerFilechain(  
    
    
      FILECHAIN_BLOCKCHAIN_RID, //Unique RID for Filechain  
    
    
      FILECHAIN_ADMIN_EVM_ADDRESS //Admin EVM address for the Filechain  
    
    
    );  
    

## Step 4: Enable/Disable a Filechain​

You can disable a Filechain when its storage capacity is full or for maintenance purposes. Once disabled, the Filechain will not accept any new file storage requests. Re-enable it when it is ready to handle new file uploads.
    
    
    // Disable a Filechain  
    
    
    await filehubAdministrator.disableFilechain(FILECHAIN_BLOCKCHAIN_RID);  
    
    
      
    
    
    // Enable a Filechain  
    
    
    await filehubAdministrator.enableFilechain(FILECHAIN_BLOCKCHAIN_RID);  
    

## Step 5: Configure payment systems​

Configure the asset for file storage payments. Ideally, select an asset from the Economy Chain for this purpose.
    
    
    await filehubAdministrator.configureCrosschainAsset(ft4Asset, issuingChain);  
    

## Step 6: Enable/Disable payments​

Administrators have the ability to enable or disable payments for file storage. Disabling payments stops file uploads until you enable payments again.
    
    
    // Enable payments for file storage  
    
    
    await filehubAdministrator.enablePayments();  
    
    
      
    
    
    // Disable payments for file storage  
    
    
    await filehubAdministrator.disablePayments();  
    

After completing these steps, your Filehub will be equipped to manage file storage, handle payments, and oversee Filechain operations efficiently. For more detailed configurations and integrations, consult the Filehub API documentation.

### Work with Filehub

SOURCE: https://docs.chromia.com/ecosystem/filehub/configure-filehub/filehub-work

Filehub allows users to store files on-chain and retrieve them using the [Filehub NPM package](<https://www.npmjs.com/package/filehub>). A Gateway is also available for easy file access, supporting various use cases such as images, videos, and static websites.

## Filehub client​

### Fetching a file​

To retrieve a file stored in Filehub, use the following code:
    
    
    const { Filehub } = require("filehub");  
    
    
    const filehub = new Filehub({  
    
    
      directoryNodeUrlPool: DIRECTORY_NODE_URL_POOL,  
    
    
      blockchainRid: FILEHUB_BLOCKCHAIN_RID,  
    
    
    });  
    
    
      
    
    
    const file = await filehub.getFile(fileHash);  
    

### Storing a file​

To store a file in Filehub, utilize the following example:
    
    
    const { Filehub, FsFile } = require("filehub");  
    
    
    const filehub = new Filehub({  
    
    
      directoryNodeUrlPool: DIRECTORY_NODE_URL_POOL,  
    
    
      blockchainRid: FILEHUB_BLOCKCHAIN_RID,  
    
    
    });  
    
    
      
    
    
    const file = FsFile.fromData(buffer, { contentType: "image/jpeg" });  
    
    
    await filehub.storeFile(ft4Session, file);  
    

## Payment model​

Filehub operates on a one-time payment model, charging users **$0.10 per megabyte** for permanent file storage. Unlike traditional storage solutions that may rely on random hard drives with uncertain longevity, Filehub ensures that files are fully stored on-chain, providing a guarantee of long-term accessibility.

note

Currently, Filehub operates on a general-purpose cluster, which is not specifically optimized for storage needs. As a result, the available storage capacity is somewhat limited. The initial release of Filehub aims to support projects on Chromia while providing early adopters an opportunity to explore its capabilities and potential applications. Looking ahead, we anticipate the introduction of dedicated storage-optimized clusters on the , which would enable us to significantly reduce storage costs for users.

## Filehub APIs​

Filehub offers a robust API for managing Filechains, facilitating file storage, and configuring payments. Key operations include:

  * **Registering a Filechain** : Assign a new Filechain with administrative privileges.
  * **Enabling/Disabling Filechains** : Control the availability of Filechains based on storage requirements.
  * **Managing Payments** : Enable or disable payments for file storage and configure the assets used for transactions.

#### Example​

Here’s how to register a new Filechain using the Filehub Administrator API:
    
    
    const filehub = new Filehub({  
    
    
      directoryNodeUrlPool: DIRECTORY_NODE_URL_POOL,  
    
    
      blockchainRid: FILEHUB_BLOCKCHAIN_RID,  
    
    
    });  
    
    
      
    
    
    const admin = newSignatureProvider({ privKey: FILEHUB_ADMIN_PRIVKEY });  
    
    
    const filehubAdministrator = new FilehubAdministrator(filehub, admin);  
    
    
      
    
    
    // Register a Filechain  
    
    
    await filehubAdministrator.registerFilechain(FILECHAIN_BLOCKCHAIN_RID, FILECHAIN_ADMIN_EVM_ADDRESS);

### Filehub setup and deployment

SOURCE: https://docs.chromia.com/ecosystem/filehub/filehub-setup/

This section covers the setup and deployment of Filehub, including the configuration of Filehub, Filechain, and the Gateway API.

A default deployment of Filehub is readily available on the Chromia Mainnet. You can access the user interface here: [Filehub UI](<https://filehub.chromia.com>), where CHR tokens are used as the payment method for uploading files.

Filehub is also live on Chromia Testnet: [Filehub Testnet UI](<https://filehub.testnet.chromia.com>). It mirrors the Mainnet experience but uses Testnet CHR, which you can obtain from the [Chromia Faucet](<https://faucet.testnet.chromia.com>).

If you'd prefer to set up your own instance of Filehub and deploy custom Filechains, you’re welcome to do so. The project is open-source, and all the necessary repositories can be found here: [Filehub Repositories](<https://gitlab.com/chromaway/filehub>).

[Set up a Filehub instance for decentralized file storage and indexing. Configure `chromia.yml` and deploy the Filehub blockchain.](</ecosystem/filehub/filehub-setup/deploy-filehub>)[Deploy a Filechain for file chunk storage and transaction validation. Configure `chromia.yml` and launch the Filechain blockchain.](</ecosystem/filehub/filehub-setup/deploy-filechain>)[Deploy a bundled Filehub and Filechain for local development using `start.sh`. Configure settings and manage environment setup and deployment.](</ecosystem/filehub/filehub-setup/deploy-bundle>)

### Deploy Filehub and Filechain bundle

SOURCE: https://docs.chromia.com/ecosystem/filehub/filehub-setup/deploy-bundle

A Filechain is inherently dependent on a Filehub, meaning that each Filechain must be deployed in conjunction with a Filehub. This project simplifies the process by packaging both Filehub and Filechain as a cohesive unit for deployment.

warning

This deployment is primarily intended for local development and testing purposes, utilizing a "mock" Directory Chain. For deployments on actual networks, refer to the independent deployment instructions for [Filehub](</ecosystem/filehub/filehub-setup/deploy-filehub>) and [Filechain](</ecosystem/filehub/filehub-setup/deploy-filechain>).

You can set up the [Filehub](</ecosystem/filehub/filehub-setup/deploy-filehub>) and [Filechain](</ecosystem/filehub/filehub-setup/deploy-filechain>) either as standalone systems or in a multi-node setup, enhancing redundancy and ensuring higher availability.

## Prerequisites​

Before starting, ensure you have the following tools and resources:

  * [Chromia CLI](</get-started/installation>)
  * Git
  * Docker

## Step 1: Run the deploy bundle​

The deployment script simplifies setting up your local development environment. It accepts three main input parameters:

  * `-r` => Enable rate limiter for Filehub (1 = enabled, 0 = disabled)
  * `-e` => Specify which endpoint to use for nodes (e.g., `docker` or `localhost`, defaults to `localhost`)
  * `-a` => Enable the Gateway API (1 = enabled, 0 = disabled)

##### Example commands:​
    
    
    ./start.sh          # Starts the deploy bundle with default settings  
    
    
    ./start.sh -r 0     # Rate limiter disabled  
    
    
    ./start.sh -e docker  # Node URLs configured for Docker  
    
    
    ./start.sh -a 0     # Gateway API disabled  
    
    
    ./start.sh -r 0 -a 0 # Both rate limiter and Gateway API disabled  
    
    
    ./start.sh -r 0 -e docker -a 0  # Disabled rate limiter & Gateway API, with Docker endpoint  
    

The script executes several steps to configure the development environment:

  1. **Environment detection:** Identifies the appropriate environment for deployment (local or production).
  2. **Reset and clean up:** Cleans up folders and Docker containers to reset the environment to a fresh state.
  3. **Repository cloning:** Clones the necessary repositories required for Filehub and Filechain.
  4. **Directory Chain preparation:** Extracts and installs the necessary packages, then builds the Directory Chain.
  5. **Database setup:** Starts a PostgreSQL container to handle the database requirements.
  6. **Directory Chain launch:** Starts the Directory Chain, which manages directory services.
  7. **Filehub deployment:** Installs, builds, and deploys the Filehub, which acts as the control hub.
  8. **Filechain deployment:** Installs, builds, and deploys the Filechain, which handles the actual file storage.
  9. **Gateway API Setup:** Configures and starts the Gateway API container, allowing easy file access via HTTP.

### Deploy Filechain

SOURCE: https://docs.chromia.com/ecosystem/filehub/filehub-setup/deploy-filechain

This topic outlines how to deploy a Filechain, which serves as a storage solution for managing and storing file chunks in the Chromia Filehub ecosystem.

## Prerequisites​

Before you begin, make sure you have access to the following tools and resources:

  * [Chromia CLI](</get-started/installation>)
  * Git
  * Docker

## Step 1: Create a Container​

Start by [creating a container](</build/deployment/mainnet/get-container>) for your Filechain instance.

## Step 2: Prepare `chromia.yml`​

Next, configure your `chromia.yml` file with the required settings, making sure to include the `trusted_filehub_brid` field with the blockchain RID from your Filehub deployment.

## Step 3: Deploy the Filechain Blockchain​

Follow the [blockchain deployment guide](</build/deployment/mainnet/deploy-dapp>) to deploy the Filechain blockchain. This blockchain will be responsible for storing file chunks and validating transactions for files uploaded through Filehub.

For additional storage capacity, repeat these steps for each Filechain you plan to deploy.

### Deploy Filehub

SOURCE: https://docs.chromia.com/ecosystem/filehub/filehub-setup/deploy-filehub

This topic outlines how to deploy a Filehub instance. Filehub acts as a decentralized solution for managing file storage and indexing across multiple blockchains.

## Prerequisites​

Before starting, ensure you have the following tools and resources:

  * [Chromia CLI](</get-started/installation>)
  * Git

## Step 1: Create a Container​

[Create a container](</build/deployment/mainnet/get-container>) for your Filehub instance.

## Step 2: Prepare `chromia.yml`​

.Set up your `chromia.yml` file according to your needs.

## Step 3: Deploy blockchain​

Follow the [blockchain deployment guide](</build/deployment/mainnet/deploy-dapp>) to deploy the Filehub blockchain. This blockchain is crucial for managing file storage and distribution across the connected Filechains.

### Overview of Filehub

SOURCE: https://docs.chromia.com/ecosystem/filehub/overview

Filehub is a scalable, reliable, and secure storage platform that Chromia's relational blockchain powers. It provides developers with a decentralized solution for storing and accessing files, ensuring data integrity and availability even when faced with external disruptions.

## Cost​

Filehub operates on a fixed payment model for storage. The current price is $0.10 per MB for perpetual storage. Your files will remain accessible for a minimum of 30 years. Access continues indefinitely with no renewal fees, as long as the network remains operational.

## File access​

Everyone can access all files stored in Filehub free of charge. If you want to store private files, you must encrypt them before upload.

## Getting started with Filehub​

To use Filehub, you need to provide it with CHR from the Economy Chain. A minimum deposit of 1 CHR is required to create an account.

  * If your CHR is currently on an EVM chain, you can create an Economy Chain account and bridge it over using the [Vault Transfer](<https://vault.chromia.com/en/transfer/>) feature.
  * Once your CHR is on the Economy Chain, you can create an account and transfer your CHR to the Filehub Chain using the [Vault UI for Filehub](<https://vault.chromia.com/en/dapps/dapp/?dapp=17-Filehub>).
  * Finally, navigate to the [Filehub UI](<https://filehub.chromia.com/>) and start uploading your files.

You can also access the [Filehub UI on Testnet](<https://filehub.testnet.chromia.com>), which behaves the same as the Mainnet version but uses Testnet CHR. You can request Testnet CHR from the [Chromia Faucet](<https://faucet.testnet.chromia.com>). Files uploaded to Testnet are not guaranteed to be stored permanently.

## Components​

Filehub consists of two fundamental components:

  * **Filehub blockchain** : This acts as the central index, tracking files and managing references to data chunks distributed across multiple Filechains.
  * **Filechain blockchain(s)** : These function as the actual blob storage, responsible for persisting file data

### Filehub​

The Filehub blockchain serves as the central indexing system, responsible for:

  * Managing the administration and availability of Filechains.
  * Selecting the appropriate Filechains for distributing and storing each file chunk.
  * Maintaining a comprehensive index of file chunks and their corresponding Filechain locations.
  * Handling the billing system for file storage, ensuring proper payment for services.

Filehub’s architecture is modular and scalable. It starts with a single Filehub and two Filechains, with the flexibility to expand incrementally by adding more Filechains as storage requirements grow. This design supports unlimited horizontal scaling, increasing both storage capacity and file distribution efficiency.

### Filechain​

The Filechain Blockchain(s) function as blob storage, handling the following tasks:

  * Storing file chunks in a secure and decentralized manner.
  * Validating payments made to Filehub using [ICCF (Interchain Confirmation Facility)](</get-started/use-cases/cross-chain/iccf>)-proofs.
  * Hashing incoming data and verifying it against expected hashes to ensure data integrity.
  * Accepting transactions and persisting file chunks on the Filechain blockchain.

Filechain serves strictly as a blob storage solution. It operates without knowledge of file metadata or the relationships between chunks. Its sole responsibility is to store data chunks and validate their integrity.

## ICCF and anchoring chain​

The [ICCF (Interchain Confirmation Facility)](</get-started/use-cases/cross-chain/iccf>) facilitates communication between Filehub and Filechains. This framework ensures that file chunks are only stored on Filechains after payment has been confirmed.

Here's how the process works:

  1. **Payment and allocation** : When a user uploads a file to Filehub, the platform allocates storage on a suitable Filechain and processes the payment.

  2. **ICCF proof generation** : Filehub generates an ICCF proof, which is a cryptographic signature verifying that the file has been allocated and paid for.

  3. **Proof submission** : The ICCF proof is submitted to the target Filechain.

  4. **Proof verification** : The Filechain independently verifies the ICCF proof to confirm that the payment for the corresponding file chunk has been completed.

This indirect communication mechanism, facilitated by the ICCF and the anchoring chain, ensures the security and integrity of the file storage process. By verifying payments before storing file chunks, Filehub prevents unauthorized access and ensures that users only pay for the storage they use.

## 5. Extensions

Compiled from official extracted article bodies. Not invented.

### AI Inference

SOURCE: https://docs.chromia.com/ecosystem/extensions/ai_inference

The AI Inference extension brings AI inference capabilities directly into dapps, enabling intelligent, real-time decision-making within the on-chain environment. By leveraging externally trained models, developers can seamlessly embed AI-driven logic into their applications while maintaining blockchain's core principles of trustlessness and transparency. This integration unlocks new possibilities for responsive and adaptive dapps.

This extension uses the Hybrid Compute framework.

[AI inference extension repository.](<https://gitlab.com/chromaway/core/ai-inference-extension>)

## Enable extension​

You can either pick the AI inference extension when leasing your container. Or you add the AI inference JAR extension to your container, making it possible to combine with other extensions.

## Blockchain configuration​

You will need to enable the Hybrid Compute framework, configure it to use the AI inference engine provided by this extension, set the appropriate timeout for inference computations and configure the model to use for inference.

chromia.yml
    
    
    blockchains:  
    
    
      ai_inference_dapp: # Ensure this name is unique. Rename it if you encounter duplicate issues during deployment.  
    
    
        module: main  
    
    
        config:  
    
    
          sync_ext:  
    
    
            - "net.postchain.hybridcompute.HybridComputeSynchronizationInfrastructureExtension"  
    
    
          gtx:  
    
    
            modules:  
    
    
              - "net.postchain.hybridcompute.HybridComputeGTXModule"  
    
    
          hybridcompute:  
    
    
            engine: "net.postchain.gtx.extensions.ai_inference.AiInferenceComputeEngine"  
    
    
          ai_inference:  
    
    
            model: "your-model-name" # Specify model name here. Example: Qwen/Qwen2.5-1.5B-Instruct  
    
    
            inference_timeout_seconds: 60 # Timeout in seconds for each inference  
    
    
            verification_timeout_seconds: 10 # Timeout in seconds for each verification  
    
    
            max_completion_tokens:  
    
    
              100 # An upper bound for the number of tokens that can be generated for a completion,  
    
    
              # including visible output tokens and reasoning tokens.  
    

### Configuration parameters​

Parameter| Description| Default| Required  
---|---|---|---  
`model`| Name of the AI model to use for inference| -| Yes  
`inference_timeout_seconds`| Timeout in seconds for each inference| 60| No  
`verification_timeout_seconds`| Timeout in seconds for each verification| 10| No  
`max_completion_tokens`| Upper bound for the number of tokens that can be generated for a completion, including visible output tokens and reasoning tokens| -| Yes  
  
## Integrating with Rell​

Install the Rell libraries:

chromia.yml
    
    
    libs:  
    
    
      hybridcompute:  
    
    
    libs:  
    
    
      com.chromia.hybridcompute:  
    
    
        version: 3.35.4  
    
    
      com.chromia.ai_inference:  
    
    
        version: 0.3.6  
    
    
    compile:  
    
    
      rellVersion: 0.14.15  
    

Import the module:
    
    
    import ai: lib.ai_inference;  
    

Submit a simple inference request with this function:
    
    
    /**  
    
    
     * Submits an inference request.  
    
    
     *  
    
    
     * @param id A unique identifier for the inference request.  
    
    
     * @param prompt The prompt to generate text for.  
    
    
     */  
    
    
    function submit_inference_request(id: text, prompt: text)  
    

Or submit a chat inference request with this function:
    
    
    /**  
    
    
     * Submits an chat inference request.  
    
    
     *  
    
    
     * @param id A unique identifier for the inference request.  
    
    
     * @param messages A list of messages comprising the conversation so far.  
    
    
     */  
    
    
    function submit_chat_inference_request(id: text, messages: list<chat_message>)  
    

Fetch the result with this function:
    
    
    struct inference_result {  
    
    
        /** The result of the inference, or null if an error occurred. */  
    
    
        result: text?;  
    
    
      
    
    
        /** An error message if the inference failed, or null if no error occurred. */  
    
    
        error: text?;  
    
    
      
    
    
        /** RID of the transaction where the result was reported. */  
    
    
        tx_rid: byte_array;  
    
    
      
    
    
        /** Index of the operation where the result was reported. */  
    
    
        op_index: integer;  
    
    
    }  
    
    
      
    
    
    /**  
    
    
     * Fetches the result of a previously submitted inference request.  
    
    
     *  
    
    
     * @param id The identifier of the inference request for which the result is being fetched.  
    
    
     * @return The result, or `null` if not ready yet  
    
    
     */  
    
    
    function fetch_inference_result(id: text): inference_result?  
    

Or extend this to be notified:
    
    
    /**  
    
    
     * Called when an inference is finished, successfully or failed.  
    
    
     *  
    
    
     * @param id The identifier of the inference request  
    
    
     * @param inference_result The result  
    
    
     */  
    
    
    @extendable function on_inference_result(id: text, inference_result)  
    

## Minimal implementation example​

Here's a minimal implementation [example in Rell](<https://gitlab.com/chromaway/core/ai-inference-extension/-/tree/dev/example>):

main.rell
    
    
    module;  
    
    
      
    
    
    import ai: lib.ai_inference;  
    
    
      
    
    
    // TODO should have authentication for this operation  
    
    
    operation submit_inference_request(id: text, prompt: text) {  
    
    
        ai.submit_inference_request(id, prompt);  
    
    
    }  
    
    
      
    
    
    // TODO should have authentication for this operation  
    
    
    operation submit_chat_inference_request(id: text, prompt: text) {  
    
    
        ai.submit_chat_inference_request(id, [ai.chat_message(role="user", message=prompt)]);  
    
    
    }  
    
    
      
    
    
    query fetch_inference_result(id: text): ai.inference_result? = ai.fetch_inference_result(id);  
    

## Advanced usage examples​

### Chat-based inference​

chat_inference.rell
    
    
    operation submit_conversation(conversation_id: text, user_message: text) {  
    
    
        // For a simple chat, create a conversation with system prompt and user message  
    
    
        val messages = [  
    
    
            ai.chat_message(role="system", message="You are a helpful assistant."),  
    
    
            ai.chat_message(role="user", message=user_message)  
    
    
        ];  
    
    
      
    
    
        ai.submit_chat_inference_request(conversation_id, messages);  
    
    
    }  
    
    
      
    
    
    operation continue_conversation(conversation_id: text, conversation_history: list<ai.chat_message>, user_message: text) {  
    
    
        // Add the new user message to the conversation history  
    
    
        val updated_messages = conversation_history + [ai.chat_message(role="user", message=user_message)];  
    
    
      
    
    
        ai.submit_chat_inference_request(conversation_id, updated_messages);  
    
    
    }  
    

### Batch processing​

batch_inference.rell
    
    
    operation submit_batch_inference(base_id: text, prompts: list<text>) {  
    
    
        var index = 0;  
    
    
        for (prompt in prompts) {  
    
    
            val request_id = "%s_%d".format(base_id, index);  
    
    
            ai.submit_inference_request(request_id, prompt);  
    
    
            index += 1;  
    
    
        }  
    
    
    }  
    
    
      
    
    
    query get_batch_results(base_id: text, count: integer): list<ai.inference_result?> {  
    
    
        val results = list<ai.inference_result?>();  
    
    
        var index = 0;  
    
    
        while (index < count) {  
    
    
            val request_id = "%s_%d".format(base_id, index);  
    
    
            results.add(ai.fetch_inference_result(request_id));  
    
    
            index += 1;  
    
    
        }  
    
    
        return results;  
    
    
    }  
    

### Conditional AI processing​

conditional_ai.rell
    
    
    operation smart_content_filter(content_id: text, content: text) {  
    
    
        // Only process content that meets certain criteria  
    
    
        if (content.size() > 10 and content.size() < 1000) {  
    
    
            val prompt = "Analyze the following content for appropriateness: %s".format(content);  
    
    
            ai.submit_inference_request(content_id, prompt);  
    
    
        }  
    
    
    }  
    
    
      
    
    
    @extend(ai.on_inference_result)  
    
    
    function (id: text, result: ai.inference_result) {  
    
    
        if (result.result != null) {  
    
    
            // Parse AI response and take action  
    
    
            if (result.result.contains("inappropriate")) {  
    
    
                // Flag content for review  
    
    
                flag_content_for_review(id, result.tx_rid, result.op_index);  
    
    
            } else {  
    
    
                // Approve content  
    
    
                approve_content(id, result.tx_rid, result.op_index);  
    
    
            }  
    
    
        }  
    
    
    }  
    

## Deployment to Testnet​

### Leasing a container​

If you are unfamiliar with leasing a container, follow these [steps](</build/deployment/testnet-tokens/get-tchr-chromia>).

info

Ensure you select the `AI Inference extension` when leasing the container. The AI Inference Extension is part of Chromia's extension ecosystem and may require cluster-level approval depending on your deployment target.

### Deployment​

Follow the [deployment steps](</get-started/create-dapp/deploy-to-testnet>).

You can find the BRID for the Testnet Directory Chain in the [explorer](<https://explorer.chromia.com/testnet/cluster/system>).

The deployment section in your `chromia.yml` will look like this:
    
    
    deployments:  
    
    
    testnet: # Deployment Target name  
    
    
      brid: x"6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92" # Blockchain RID for Testnet Directory Chain  
    
    
      url: https://node0.testnet.chromia.com # Target URL for one of the nodes in Testnet  
    
    
      container: 4d7890243fe710...08c724700cbd385ecd17d6f # Replace with your container ID (Example: container: 15ddfcb25dcb43577ab311fe78aedab14fda25757c72a787420454728fb80304)  
    
    
      chains:  
    
    
        ai_inference_dapp: x"9716FBFF3...663FEC673710" # Replace with the actual BRID after the dapp gets deployed. You can find it in the terminal during deployment.  
    

## Testing and examples​

### Simple inference test​

In the examples provided, `ai_inference_dapp` and `testnet` refer to the deployment parameters.

To send a simple inference request, use:
    
    
    chr tx -bc ai_inference_dapp -d testnet submit_inference_request 'a1' 'What is artificial intelligence?'  
    

To get a response, execute:
    
    
    chr query -bc ai_inference_dapp -d testnet fetch_inference_result id=a1  
    

Output:
    
    
    [  
    
    
      "error": null,  
    
    
      "result": "Artificial intelligence (AI) refers to the simulation of human intelligence in machines...",  
    
    
      "tx_rid": "1234567890ABCDEF...",  
    
    
      "op_index": 0  
    
    
    ]  
    

### Chat inference test​

To send a chat inference request, use:
    
    
    chr tx -bc ai_inference_dapp -d testnet submit_chat_inference_request 'chat1' 'Hello, how are you?'  
    

### Check inference status​
    
    
    chr query -bc ai_inference_dapp -d testnet get_inference_status id=a1  
    

### Batch testing​
    
    
    # Submit multiple requests  
    
    
    chr tx -bc ai_inference_dapp -d testnet submit_inference_request 'batch_1' 'What is artificial intelligence?'  
    
    
    chr tx -bc ai_inference_dapp -d testnet submit_inference_request 'batch_2' 'Explain machine learning'  
    
    
    chr tx -bc ai_inference_dapp -d testnet submit_inference_request 'batch_3' 'Define neural networks'  
    
    
      
    
    
    # Check results  
    
    
    chr query -bc ai_inference_dapp -d testnet fetch_inference_result id=batch_1  
    
    
    chr query -bc ai_inference_dapp -d testnet fetch_inference_result id=batch_2  
    
    
    chr query -bc ai_inference_dapp -d testnet fetch_inference_result id=batch_3

### What are Chromia extensions?

SOURCE: https://docs.chromia.com/ecosystem/extensions/overview

Chromia extensions serve as pre-built docker images that add specialized features or services to containers within the Chromia ecosystem. They help developers seamlessly integrate additional functionality without needing to write new code or modify existing applications. Acting as modular solutions, these extensions enable blockchain applications to scale and adapt to the specific needs of each project.

The primary goal of Chromia Extensions is to offer a simple and efficient way to implement complex functionalities. Developers can integrate off-chain data (via oracles), perform advanced computations (like AI inference), or optimize data management. These tools empower developers to create innovative solutions for various sectors, including financial services, gaming platforms, and supply chain management. By leveraging Chromia extensions, developers enhance and adapt decentralized applications, reducing the time and effort needed to implement sophisticated technologies.

## Advantages and limitations​

Chromia extensions offer developers significant benefits but also present certain limitations that they should consider before use.

**Advantages:**

  * **Modularity and flexibility:** Developers can quickly add specialized features, such as off-chain data integration, using extensions without building new functionalities from scratch.
  * **Pre-built solutions:** Developers have access to ready-to-use tools, which significantly reduce their development time and effort.
  * **Seamless compatibility:** Extensions integrate smoothly with containers, ensuring reliable operation and easy deployment, as they are designed with Chromia's architecture in mind.
  * **Free accessibility:** At launch, developers can access Chromia extensions free of charge, allowing them to experiment and explore new features without incurring financial costs.

**Limitations:**

  * **One extension per container:** Each container supports only one active extension.
  * **Immutability:** Once developers add an extension, they cannot remove or modify it. This permanence requires careful planning during setup to ensure compatibility and maintain container stability within the Chromia ecosystem.
  * **Cluster-specific availability:** The availability of extensions depends on the cluster where the container is deployed. Some extensions can be accessed across the entire network, while others require approval at the cluster level through a vote by the cluster's governors.

## How to obtain a Chromia extension​

### If the container doesn't exist yet​

  1. Go to the [Vault page](<https://vault.chromia.com/>).
  2. Connect your preferred wallet to the platform.
  3. Bridge any amount of CHR to Chromia or stake 10 CHR to create an account if you need to.
  4. Open the **Containers Lease** in the menu to the left and click **Lease a container**.
  5. Choose the cluster where you want to lease the container. The availability of extensions may vary by cluster.
  6. Specify the required resources for your container and select your desired extension from the dropdown menu.
  7. Click **Lease** and confirm the transaction in your wallet.
  8. Go to the **Container Lease** section to confirm that your container is activated with the applied extension.

### If the container already exists:​

  1. Go to the [Vault page](<https://vault.chromia.com/>).
  2. Connect your preferred wallet to the platform.
  3. Navigate to the **Container Lease** section and select **Add extension** from the container menu (⋮) to the right.
  4. Choose the desired extension from the available options and click **Add extension**.

### Stork Oracle

SOURCE: https://docs.chromia.com/ecosystem/extensions/stork

Stork is the leading oracle for low-latency DeFi protocols, delivering price data at ultra-low latency. Stork supports a wide range of use cases including perpetuals markets, lending protocols, and DeFi ecosystems. You can learn more about Stork [here](<https://stork.network>) and in the Stork [docs](<https://docs.stork.network>).

The Stork Oracle Chromia extension integrates Stork price feeds into Chromia blockchains by verifying and injecting the latest price updates at the beginning of each block.

## Blockchain configuration​

When you lease your container, ensure you select the **Stork Oracle Chromia extension**. To start using the extension, configure your blockchain to listen for asset updates and include the necessary Stork extensions. Below is a sample configuration:

chromia.yml
    
    
    blockchains:  
    
    
      <my_blockchain_name>:  
    
    
        module: <module_name>  
    
    
        config:  
    
    
          gtx:  
    
    
            modules:  
    
    
              - "net.postchain.stork.StorkOracleGTXModule"  
    
    
          sync_ext:  
    
    
            - "net.postchain.stork.StorkOracleSynchronizationInfrastructureExtension"  
    
    
          stork:  
    
    
            assets:  
    
    
              - "BTCUSD"  
    
    
              - "ETHUSD"  
    
    
              - "..."  
    

`Assets`: Specify the list of assets (e.g., BTCUSD, ETHUSD) that you want the extension to monitor for updates. You can find the complete list of assets at [this link](<https://docs.stork.network/resources/asset-id-registry>).

The extension should already be aware of all publisher public keys. However, if it should ever be necessary, you can override the Stork public key and publisher public keys as follows:

chromia.yml
    
    
    blockchains:  
    
    
      <my_blockchain_name>:  
    
    
        module: <module_name>  
    
    
        config:  
    
    
          stork:  
    
    
            stork_pubkey: x"0a803F9b1CCe32e2773e0d2e98b37E0775cA5d44"  
    
    
            publisher_pubkeys: # List ALL publisher keys here if you want to override  
    
    
              - x"5c946686b0302be54d85394015a9f9fa0952984e"  
    
    
              - "..."  
    

## Rell Integration​

Integrate the Stork Oracle Extension library into your Rell project by adding it to your configuration:

chromia.yml
    
    
    libs:  
    
    
      stork:  
    
    
        registry: https://gitlab.com/chromaway/core/stork-oracle-chromia-extension  
    
    
        path: rell/src/stork  
    
    
        tagOrBranch: 1.0.1  
    
    
        rid: x"EBB409F91EBC5EB3816570C9FDB5A170180249CF7F74EEDFC09C428E288F4114"  
    
    
        insecure: false  
    

To process price updates in your application, extend the `on_stork_oracle_prices_update` hook:
    
    
    @extend(on_stork_oracle_prices_update) function handle_price_update(stork_oracle_prices) {  
    
    
        // Add your custom logic here  
    
    
    }  
    

The `stork_oracle_prices` struct provides detailed information about price updates:
    
    
    struct stork_oracle_prices {  
    
    
        asset: text;  
    
    
        stork_price;  
    
    
        publisher_prices: list<publisher_price>;  
    
    
    }  
    
    
      
    
    
    struct stork_price {  
    
    
        price: big_integer;  
    
    
        signature;  
    
    
        timestamp_nanos: integer;  
    
    
        merkle_root: byte_array;  
    
    
        type: text;  
    
    
        version: text;  
    
    
        checksum: byte_array;  
    
    
    }  
    
    
      
    
    
    struct publisher_price {  
    
    
        price: big_integer;  
    
    
        signature;  
    
    
        timestamp_seconds: integer;  
    
    
    }  
    
    
      
    
    
    struct signature {  
    
    
        signer: byte_array;  
    
    
        r: byte_array;  
    
    
        s: byte_array;  
    
    
        v: byte_array;  
    
    
    }  
    

The extension automatically verifies the signatures, so you don't need to include that logic in your Rell code.

Prices are represented as `big_integer` with 18 decimal places. Use the following utility function to convert prices to decimal format:
    
    
    function convert_price_to_decimal(price: big_integer): decimal  
    

You can find the source code and further details about the Stork Oracle Chromia extension in the [official repository](<https://gitlab.com/chromaway/core/stork-oracle-chromia-extension>).

### Vector DB

SOURCE: https://docs.chromia.com/ecosystem/extensions/vector-db

Chromia’s Vector DB extension allows for the efficient storage and querying of complex multi-dimensional data in a decentralized approach. It integrates relational database principles to support high-performance indexing and similarity searches, making it ideal for AI-driven applications such as recommendation systems, natural language processing, and image recognition.

You can easily use Chromia’s Rell language and the Chromia CLI to define vector-based schemas, execute operations and queries, and perform similarity searches. The integration with Chromia’s decentralized network ensures high availability, scalability, and tamper-proof data management. This combination of blockchain technology, relational indexing, and vector search opens up new possibilities for decentralized AI, gaming, and large-scale data analytics.

## Leasing a container​

If you are unfamiliar with the process of leasing a container, follow these [steps](</build/deployment/testnet-tokens/get-tchr-chromia>).

info

Ensure that the `Vector DB extension` is selected when leasing the container.

## Configuring blockchain in the dapp​

When leasing your container, ensure that you select the **Vector DB extension**. Configure your blockchain to use operations and queries to start utilizing this extension. Below is a sample configuration:

chromia.yml
    
    
    blockchains:  
    
    
      my_chain:  
    
    
        module: my_chain_module  
    
    
        config:  
    
    
          gtx:  
    
    
            modules:  
    
    
              - "net.postchain.gtx.extensions.vectordb.VectorDbGTXModule"  
    
    
          vector_db_extension:  
    
    
            my_vector_collection: # Name of the collection. Multiple collections can be defined.  
    
    
              dimensions: 768 # Required - set number of dimensions to use  
    
    
              index: hnsw_cosine # Optional - available distance indices: hnsw_cosine, hnsw_l1, hnsw_l2, hnsw_ip (default: hnsw_cosine)  
    
    
              query_max_vectors: 10 # Optional — max results returned per query (default: 10)  
    
    
              store_batch_size: 300 # Optional — number of vectors stored per internal batch (default: 300)  
    

Vectors are stored and grouped by `collections` and `contexts`. A `collection` is defined in the blockchain configuration to separate different types of vectors (e.g. dimensions, index, etc.). A `context` is used to separate vectors within a `collection`, and is set by the dApp.

A dApp can also dynamically manage collections. More information about this can be found in the [repository documentation](<https://gitlab.com/chromaway/core/vector-db-extension#blockchain-configuration>).

info

`dimensions` must match the length of the vectors you store — for example, 384, 768, or 1024 when using text embeddings.

## Integrating with Rell​

Integrating the Vector DB Extension library into your Rell project is optional but recommended. To do this, add it to your configuration as follows:

chromia.yml
    
    
    libs:  
    
    
      com.chromia.vector_db:  
    
    
        version: 2.2.0 # Set to version you want to use  
    

Available versions can be found by running `chr library versions com.chromia.vector_db`.

Run:
    
    
    chr install  
    

## Minimal implementation example​

Here is a simple dapp to store and remove vectors in the defined collection `my_vector_collection`:
    
    
    import lib.vector_db.*;  
    
    
      
    
    
    operation add_vector(context: integer, vector: text, id: integer) {  
    
    
        store_vector("my_vector_collection", context, vector, id);  
    
    
    }  
    
    
      
    
    
    operation delete_vector(context: integer, id: integer) {  
    
    
        delete_vector("my_vector_collection", context, id);  
    
    
    }  
    

## Deployment to Testnet​

[Deployment steps.](</get-started/create-dapp/deploy-to-testnet>) BRID for Testnet Directory Chain can be found in the [explorer](<https://explorer.chromia.com/testnet/cluster/system>).

Expected output on successful deployment:
    
    
    Deployment of blockchain vector_example was successful  
    
    
    Add the following to your project settings file:  
    
    
    deployments:  
    
    
      testnet:  
    
    
        chains:  
    
    
          my_chain: x"CEC6A318C873...0A32C85429706" # you will get your own BRID  
    

Add the deployed chain into `chromia.yml`:
    
    
    deployments:  
    
    
    testnet: # Deployment Target name  
    
    
      brid: x"6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92" # Blockchain RID for Testnet Directory Chain  
    
    
      url: https://node0.testnet.chromia.com # Target URL for one of the nodes in Testnet  
    
    
      container: 4d7890243fe710...08c724700cbd385ecd17d6f # Replace with your container ID (Example - container: 15ddfcb25dcb43577ab311fe78aedab14fda25757c72a787420454728fb80304)  
    
    
      chains:  
    
    
        my_chain: x"CEC6A318C873...0A32C85429706" # Replace with the actual BRID after the dapp gets deployed. It can be found in the terminal during deployment.  
    

Save the `BRID` from deployment into environment variables.
    
    
    vector_brid=CEC6A318C873B3013DB9476C084BEDE0EA3D03D5C686A2FACD50A32C85429706  
    

Save the `URL` of the test node into environment variables.
    
    
    url=https://node0.testnet.chromia.com  
    

note

The following operation examples are based on the `vector_example` application from the [Vector DB extension repository](<https://gitlab.com/chromaway/core/vector-db-extension>). You can reference this example to see the complete implementation.

## Operation examples:​
    
    
    chr tx -brid $vector_brid --api-url $url add_message hej "[1.0, 2.0, 3.0]"  
    
    
    chr tx -brid $vector_brid --api-url $url add_message hello "[1.0, 2.5, 3.0]"  
    
    
    chr tx -brid $vector_brid --api-url $url add_message hei "[1.0, 2.0, 3.1]"  
    
    
    chr tx -brid $vector_brid --api-url $url add_message "guten tag" "[1.0, 1.5, 3.5]"  
    

## Querying vectors​

The extension introduces a query function named `query_closest_objects`, which allows you to search for vectors. It returns the closest vectors based on cosine similarity:

Name| Type| Required| Default| Description  
---|---|---|---|---  
`collection`| text| true| | Collection used by the dapp. Use different collections to separate different type of vectors.  
`context`| integer| true| | Context used by the dapp. Use different contexts to separate unrelated vectors. Use `-1` to disable and query all vectors in all contexts.  
`q_vector`| text| true| | The query vector as a string (e.g. `"[1.0, 2.0, 3.0]"`)  
`max_distance`| decimal| true| | Maximum allowed distance between the query and stored vectors  
`query_max_vectors`| integer| false| 10| Maximum number of results to return  
`query_template`| text or dict| false| –| Apply a Rell query to enrich/filter/transform the results (see examples below)  
  
tip

Use different `context` values to keep vector sets isolated — for example, one for product descriptions, and one for support tickets. This allows you to run similarity searches independently across each domain.

## Query examples​

### Plain query with no `query_template`​
    
    
    chr query -brid $vector_brid --api-url $url query_closest_objects collection=my_vector_collection context=0 q_vector="[1.0, 2.0, 3.0]" max_distance=1.0 max_vectors=2  
    
    
      
    
    
    [  
    
    
      {  
    
    
        "distance": "0",  
    
    
        "id": 1,  
    
    
        "context": 0,  
    
    
      },  
    
    
      {  
    
    
        "distance": "0.0001212999220387978",  
    
    
        "id": 3,  
    
    
        "context": 0,  
    
    
      }  
    
    
    ]  
    

### Using a `query_template` to return the text messages​
    
    
    chr query -brid $vector_brid --api-url $url query_closest_objects collection=my_vector_collection context=0 q_vector="[1.0, 2.5, 3.0]" max_distance=1.0 max_vectors=2 'query_template=["type":"get_messages"]'  
    
    
      
    
    
    [  
    
    
      "hello",  
    
    
      "hej"  
    
    
    ]  
    

### Using a `query_template` to return distance and text​
    
    
    chr query -brid $vector_brid --api-url $url query_closest_objects collection=my_vector_collection context=0 q_vector="[1.0, 2.5, 3.0]" max_distance=1.0 max_vectors=2 'query_template=["type":"get_messages_with_distance"]'  
    
    
      
    
    
    [  
    
    
      {  
    
    
        "distance": "0",  
    
    
        "text": "hello"  
    
    
      },  
    
    
      {  
    
    
        "distance": "0.005509683802306209",  
    
    
        "text": "hej"  
    
    
      }  
    
    
    ]  
    

### Passing arguments to filter results​
    
    
    chr query -brid $vector_brid --api-url $url query_closest_objects collection=my_vector_collection context=0 q_vector="[1.0, 2.5, 3.0]" max_distance=1.0 max_vectors=2 'query_template=["type":"get_messages_with_filter", "args":["text_filter": "j"]]'  
    
    
      
    
    
    [  
    
    
      "hej"  
    
    
    ]  
    

* * *

You can find the source code and additional details about the Vector DB extension in the  
👉 [official repository](<https://gitlab.com/chromaway/core/vector-db-extension>)

### Zero-knowledge Proof

SOURCE: https://docs.chromia.com/ecosystem/extensions/zkp

The Zero-knowledge Proof (ZKP) extension for Chromia enables developers to integrate advanced privacy-preserving features directly into their decentralized applications. By supporting PLONK (Permutations over Lagrange-bases for Oecumenical Noninteractive arguments of Knowledge) zero-knowledge proofs, this extension allows applications to verify computations and validate data without disclosing sensitive information.

PLONK is a zero-knowledge proof system that allows one party to prove to another that they have knowledge of certain information without revealing the information itself. PLONK is known for its efficient verification process and universal trusted setup, making it particularly suitable for blockchain applications. It improves upon previous ZK-SNARK systems by requiring only one universal trusted setup, which can be utilized for all circuits of a given size.

Original paper: <https://eprint.iacr.org/2019/953.pdf>

## Circuit and key prerequisites​

Chromia does not provide tools for creating zero-knowledge circuits or generating verification keys. You must create these externally using tools like [snarkjs](<https://github.com/iden3/snarkjs>) before configuring your Chromia blockchain. The verification keys shown in the configuration below are the output of your external circuit development and trusted setup process.

## Blockchain configuration​

To be able to verify PLONK proofs, you need to add [`ZKPGTXModule`](<https://gitlab.com/chromaway/core/zkp-extension/-/tree/1.0.0/src/main/kotlin/net/postchain/zkp?ref_type=tags>) to your blockchain configuration. In addition, you also need to add your verification key(s) to it. Since it's possible to have more than one key, you need to give them an identifier in the configuration. See the example below:
    
    
    blockchains:  
    
    
      <my_blockchain_name>:  
    
    
        module: <my_module_name>  
    
    
        config:  
    
    
          gtx:  
    
    
            modules:  
    
    
              - "net.postchain.zkp.ZKPGTXModule"  
    
    
          zkp:  
    
    
            plonk:  
    
    
              verification_keys:  
    
    
                <verification_key_id>:  
    
    
                  Qc:  
    
    
                    x: 0L  
    
    
                    y: 0L  
    
    
                  Ql:  
    
    
                    x: 14834510898339141169329695685511700666258129880115403401229217490569515343302L  
    
    
                    y: 9464224989088393801563875716232716915352580613417007162533827452727295446934L  
    
    
                  Qm:  
    
    
                    x: 14319141532948637259101778790582856386713203034743319527668731828185741400626L  
    
    
                    y: 18803411281712541135482987792568462445904566200816418568722397188523035189143L  
    
    
                  Qo:  
    
    
                    x: 21364495085187694205438833340386123229650091635008748320283906080029143641039L  
    
    
                    y: 20444368598332986012476714370015271579755130392073789088854120974413040917081L  
    
    
                  Qr:  
    
    
                    x: 16351212189779639003908022196613349497559341482069657547941244577951374625350L  
    
    
                    y: 4031150659123603989265639197462571629326313607153532089054168665871713116509L  
    
    
                  S1:  
    
    
                    x: 18126157909848214547544885505634038550836716698610211113963616409746950617323L  
    
    
                    y: 9480441132510474920656224855370744169816505579366946377741851539331522216404L  
    
    
                  S2:  
    
    
                    x: 15312771180064260077439958970242428939484644647049778052207315835174955686500L  
    
    
                    y: 3394171162668419319900753190450314872629638145244728026210508800204487774112L  
    
    
                  S3:  
    
    
                    x: 15657500937529974074969620066780498543702080929706007838285907462589433927278L  
    
    
                    y: 10353909101117571868937190939592663817839933444432483255231136649076311981690L  
    
    
                  X_2:  
    
    
                    x1: 19518502430870438181592443054401419581386850463392492809261261189226441340111L  
    
    
                    x2: 8184812567585147824016587988320202893865389288562095514010129273046904740147L  
    
    
                    y1: 10435902743221815611441838668065138241026433470816816910474433321658510434042L  
    
    
                    y2: 3114164934987634673554850993955208553008076750558125585683111415888626769753L  
    
    
                  curve: bn128  
    
    
                  k1: 2L  
    
    
                  k2: 3L  
    
    
                  nPublic: 1  
    
    
                  power: 11  
    

As you can see, you also specify which curve that should be used in the key. Currently, the following values are allowed:

  * `bn128`
  * `bls12381`

These correspond to the supported curves: `BN128` and `BLS12-381`.

## Rell library​

There is a small Rell [`library`](<https://gitlab.com/chromaway/core/zkp-extension/-/tree/1.0.0/rell?ref_type=tags>) that you can install to help verify proofs:
    
    
    libs:  
    
    
      zkp:  
    
    
        registry: https://gitlab.com/chromaway/core/zkp-extension.git  
    
    
        path: rell/src/lib/zkp  
    
    
        tagOrBranch: 1.0.0  
    
    
        rid: x"8934250CED0D8C7FB46C458CDB303236AA70F3666DA67376E75E89D16F125FF9"  
    

This library exposes the following functions that you can use to verify if a proof is present in the current transaction:
    
    
    /**  
    
    
     * Checks whether or not the current transaction contains a valid PLONK proof.  
    
    
     *  
    
    
     * @param verification_key_id ID of the verification key that the proof must have been validated with  
    
    
     * @param public_signals The public signals that the proof must have been validated with  
    
    
     */  
    
    
    function check_plonk_proof(  
    
    
        verification_key_id: text,  
    
    
        public_signals: list<big_integer>  
    
    
    )  
    
    
      
    
    
    /**  
    
    
     * Checks whether or not the current transaction contains a valid PLONK proof operation before current operation.  
    
    
     *  
    
    
     * @param verification_key_id ID of the verification key that the proof must have been validated with  
    
    
     * @return The public signals of the preceding proof operation  
    
    
     */  
    
    
    function extract_signals_from_preceeding_proof_op(verification_key_id: text): list<big_integer>  
    

To add a proof to a transaction, you need to call a gtx operation `zkp_plonk_verify` with the following arguments:
    
    
    [  
    
    
        GtvString,//verification key ID, needs to match verification key id defined in chromia.yml  
    
    
        PlonkProof,//Proof itself, see details below  
    
    
        GtvArray<GtvBigInteger>//Public signals  
    
    
    ]  
    

The `PlonkProof` consists of a `GtvArray` with the following structure:
    
    
    [  
    
    
        GtvArray<GtvBigInteger>,//A [x, y]  
    
    
        GtvArray<GtvBigInteger>,//B [x, y],  
    
    
        GtvArray<GtvBigInteger>,//C [x, y],  
    
    
        GtvArray<GtvBigInteger>,//Z [x, y],  
    
    
        GtvArray<GtvBigInteger>,//T1 [x, y],  
    
    
        GtvArray<GtvBigInteger>,//T2 [x, y],  
    
    
        GtvArray<GtvBigInteger>,//T3 [x, y],  
    
    
        GtvArray<GtvBigInteger>,//Wxi [x, y],  
    
    
        GtvArray<GtvBigInteger>,//Wxiw [x, y],  
    
    
        GtvBigInteger,//eval a  
    
    
        GtvBigInteger,//eval b  
    
    
        GtvBigInteger,//eval c  
    
    
        GtvBigInteger,//eval s1  
    
    
        GtvBigInteger,//eval s2  
    
    
        GtvBigInteger,//eval zW  
    
    
    ]  
    

## Example dapp operations​

### Converts public FT4 tokens to private tokens​

This operation, known as "shielding," converts publicly visible FT4 tokens into private commitments on the blockchain. The zero-knowledge proof ensures that the conversion is valid and that the correct amount is being shielded without revealing the actual token amounts or the user's private information. [`operation shield_tokens()`](<https://bitbucket.org/chromawallet/zkp-demo/src/20e7bcfa0f0bf89349770b4a72ec860033889998/rell/src/zkp_demo/private_token.rell#lines-27>)

### Converts private tokens back to public FT4 tokens​

This operation, known as "unshielding," converts private token commitments back to publicly visible FT4 tokens, making them available for standard blockchain operations without using zero-knowledge proof operations. [`operation unshield_tokens()`](<https://bitbucket.org/chromawallet/zkp-demo/src/20e7bcfa0f0bf89349770b4a72ec860033889998/rell/src/zkp_demo/private_token.rell#lines-55>)

### Private transfer functionality using zero-knowledge proofs​

This operation allows users to transfer private tokens while ensuring complete privacy regarding the transaction amounts. The zero-knowledge proof verifies that the sender has a sufficient amount of tokens without revealing the actual number of tokens.

[`operation private_transfer()`](<https://bitbucket.org/chromawallet/zkp-demo/src/20e7bcfa0f0bf89349770b4a72ec860033889998/rell/src/zkp_demo/operations.rell#lines-2>)

### Submitting a Proof from a Client​

Here is an example of how to call the `zkp_plonk_verify` operation from the [client side](<https://bitbucket.org/chromawallet/zkp-demo/src/20e7bcfa0f0bf89349770b4a72ec860033889998/client/src/services/ft4Client.js#lines-547>). In this example, we use the `shield_tokens` operation.

note

Note that the string used for the second argument in the `zkp_plonk_verify` operation matches both the string used in the call to [`extract_signals_from_preceding_proof_op`](<https://bitbucket.org/chromawallet/zkp-demo/src/20e7bcfa0f0bf89349770b4a72ec860033889998/rell/src/zkp_demo/private_token.rell#lines-31>) within the `shield_token` operation and the `verification_key_id` defined in the [`chromia.yml`](<https://bitbucket.org/chromawallet/zkp-demo/src/fed45649c324f9eb923316790e70ab0999a489d0/rell/chromia.yml#lines-14>).
    
    
    let txBuilder = this.session.transactionBuilder();  
    
    
    txBuilder.add(op("zkp_plonk_verify", "shield_operation", proofResult.proof, proofResult.publicSignals), {  
    
    
      authenticator: authenticator1,  
    
    
    });  
    
    
    txBuilder.add(op("shield_tokens", Buffer.from(encryptedNote)), {  
    
    
      authenticator: authenticator2,  
    
    
    });  
    
    
    const result = await txBuilder.buildAndSend();  
    

## Repository links​

  * [Chromia ZKP demo project](<https://bitbucket.org/chromawallet/zkp-demo/src/main/>)
  * [ZKPGTXModule](<https://gitlab.com/chromaway/core/zkp-extension/-/tree/1.0.0/src/main/kotlin/net/postchain/zkp?ref_type=tags>)
  * [Rell ZKP library](<https://gitlab.com/chromaway/core/zkp-extension/-/tree/1.0.0/rell?ref_type=tags>)

## 6. Block Explorer

Compiled from official extracted article bodies. Not invented.

### Explorer features

SOURCE: https://docs.chromia.com/ecosystem/block-explorer/features

The **Chromia Block Explorer** is a blockchain explorer for the Chromia Mainnet and Testnet that provides a comprehensive view of blockchain activity. It enables users to track transactions, explore blocks, view account activity, and analyze digital assets within the Chromia ecosystem. This topic will help you navigate the explorer and utilize its features effectively.

### Blockchains​

Each blockchain on Chromia operates independently, running its own set of applications and transactions.

The Block Explorer provides access to:

  * A list of active blockchains on the Chromia Mainnet or Testnet.
  * Blockchain-specific details, including:
    * **Blockchain Name** : The name of the blockchain.
    * **Blockchain ID** : A unique identifier for each blockchain.
    * **Block Height** : The current number of blocks in the chain.
    * **Total Transactions** : The total number of transactions recorded on the blockchain.
    * **Total Accounts** : The number of active accounts interacting with the blockchain.

#### Example Blockchain: Mines of Dalarnia​

  * **Blockchain Name** : Mines of Dalarnia
  * **Blockchain ID** : 5DCEAC1CAFE8CE46284B4FFA739C55567F3B91147DB27FF2E40FFD963C39BB8E
  * **Block Height** : 613,093
  * **Total Transactions** : 674,098
  * **Total Accounts** : 1,256

### Transactions​

Transactions record all activities on the Chromia network. The Block Explorer allows you to:

  * Search for transactions using a **Transaction Hash**.
  * View transaction details, including:
    * **Blockchain** : The blockchain where the transaction was recorded.
    * **Block Height** : The block number where the transaction is included.
    * **Date and Time** : When the transaction was confirmed.
    * **Signers** : The cryptographic public keys that authorized the transaction.
    * **Accounts** : Associated accounts interacting in the transaction (if applicable).
    * **Transaction ID** : A unique identifier for the transaction.
    * **Transaction Hash** : A cryptographic hash representing the transaction.
    * **Transfers** : Details about assets transferred within the transaction (if any).

### Blocks​

Blocks contain transactions and are essential for maintaining the integrity of the blockchain.

The Block Explorer provides:

  * **Block Height** : The sequential number of the block in the chain.
  * **Timestamp** : The date and time when the block was created.
  * **Transactions Included** : A list of transactions recorded in the block.

### Assets​

Chromia supports various tokens and digital assets, which can be tracked using the Block Explorer.

You can:

  * Search for specific tokens by name or contract address.
  * View token metadata, supply, and distribution details.
  * Monitor asset transfers and interactions with smart contracts.

#### Example Asset: Gold (Mines of Dalarnia)​

  * **Asset Name** : Gold
  * **ID** : 19E2C73D4AE8A660E601603C37D5EFC9EB5194665308155F5541ADF51C02DE52
  * **Symbol** : GOLD
  * **Decimals** : 0
  * **Insuring Blockchain** : 5DCEAC1CAFE8CE46284B4FFA739C55567F3B91147DB27FF2E40FFD963C39BB8E
  * **Supply** : 982,405
  * **Type** : RESOURCE

### Providers​

The **Providers** section displays details about active providers in the Chromia network, including their metrics and tiers.

#### Provider metrics​

  * **Total Providers** : 21
  * **Total Rewards Paid** : 3,067,888.99
  * **Pending Proposals** : 0

### Proposals​

The **Proposals** section displays governance proposals submitted to the Chromia network.

### About Chromia Block Explorer

SOURCE: https://docs.chromia.com/ecosystem/block-explorer/overview

Chromia Block Explorer is a powerful tool that provides insight into transactions and network activity within the Chromia blockchain ecosystem. It delivers essential functionalities that allow users to track blockchain activity.

The Block Explorer is designed to display data from a single network at a time. You can monitor both system chains and dapp chains, offering a comprehensive view of protocol functions, decentralized applications (dapps), and other network utilities.

There are two key implementations of the Chromia Explorer. The **Chromia Public Testnet** explorer allows users to observe transactions, smart contract interactions, and network performance across the public Testnet. It provides a clear view of ongoing blockchain activity, including system and dapp chains. The **Chromia Public Mainnet** explorer is dedicated to monitoring the blockchain activity associated with the Mainnet.

Chromia’s modular architecture is evident within the Explorer, as it structures protocol functions and dapps into distinct blockchains.

Within the **system** cluster, you can monitor fundamental components such as the directory chain, the economy chain, and the system-wide anchoring chain. These components play a crucial role in the integrity and coordination of the Chromia network.

Additionally, the **pink** cluster contains various dapps that have undergone testing on the public Mainnet. Among these are Fanzeal, My Neighbor Alice, Mines of Dalarnia and so on, which leverage Chromia’s blockchain infrastructure for different use cases. The network also supports developer utility dapps like Filehub, which provides a decentralized storage solution for the Chromia Network. The ability to track these projects through the explorer helps users and developers analyze ongoing blockchain developments and understand how Chromia's ecosystem is evolving.

Chromia Explorer plays a key role in making blockchain activity transparent and accessible. By providing an organized view of transactions, smart contracts, and network interactions, it serves as a valuable tool for developers, investors, and users looking to explore the Chromia blockchain in depth.

### Use the Block Explorer to test and validate your dapp deployment

SOURCE: https://docs.chromia.com/ecosystem/block-explorer/using-explorer

## Search for blockchain data​

You can utilize the search bar to locate transactions, accounts, blocks, providers, proposals, or tokens by entering relevant identifiers such as transaction hashes, addresses, blockchain names, or proposal IDs.

## Explore the Block Explorer dashboard​

The explorer’s interface consists of multiple sections:

  * **Home page** : Displays an overview of recent blockchain activity.
  * **CHR stats section** : Showcases CHR token metrics.
  * **Blockchains section** : Provides a history of transactions occurring on the network.
  * **Assets section** : Showcases information on tokens issued on the Chromia network.
  * **Providers section** : Lists active providers, their metrics, and associated nodes.
  * **Proposals section** : Displays governance proposals and their statuses.

## Monitor live blockchain activity​

To analyze activity on the Chromia network:

  1. Use the search bar to find specific **transactions, blocks, accounts, providers, or proposals**.
  2. Click on a transaction, block, or proposal to view its details.
  3. Navigate through different sections to explore **accounts, assets, providers, and governance proposals**.

## Test dapp functionality with read queries​

  1. Navigate to the **Config. and API** section where you see **Queries** , **Operations** , **Source code** , and **Config file** tabs.

  2. Click on the **Queries** tab to display a list of available Rell queries.
  3. Use the **Search** bar to find a specific query or browse the list of modules and queries.
  4. Click the **Query** or **+** icon next to the query name—in this case, 'get_account_by_id'.
  5. Enter the **account ID** in the `id (byte_array)` field. Ensure the ID is in the correct format.
  6. Click **Send query** to execute it, then review the response in the **JSON** tab.
  7. If adjustments are required, update the parameters or modify the query, and re-run the query.

## Validate dapp behavior with write operations​

  1. Navigate to the **Config. and API** section, and click **Operations** to view the list of available Rell operations.

  2. Use the **Search** bar to find a specific operation or browse the modules/operations list.
  3. Click the **Operation** or **+** icon next to the operation name—in this case, `register_asset`.
  4. Enter the required fields (e.g., `name`, `symbol`, `decimals`, `icon_url`) as prompted by the UI.
  5. If your dapp requires wallet authorization, click **Connect Wallet** and follow the instructions.
  6. Click **Submit** to run `register_asset`, then review the response.
  7. If you need to modify parameters or the underlying logic, make changes, then re-run the operation.

## Debug logic by reviewing source code​

  1. Navigate to the **Config. and API** section and click **Source code** to view the list of Rell files and modules.

  2. Click the **File** or **+** icon to expand a file (for example, `auro/main.rell`).
  3. Review the code directly in the interface, scrolling through the file contents to understand the logic or definitions.

## Confirm deployment settings in the config file​

  1. Navigate to the **Config. and API** section and click **Config file** to open the Rell configuration and environment settings.

  2. Review the existing parameters (e.g., `blockstrategy`, `gtx`, `rell`, `revolt`) to understand how your dapp is configured.

## 7. Governance kit

Compiled from official extracted article bodies. Not invented.

### Key features comparison

SOURCE: https://docs.chromia.com/ecosystem/governance/alternative-sol

## Comparison of governance systems​

### Aragon's gasless on-chain voting​

  * **Key features:**
    * Utilizes modular governance templates.
    * Implements effective dispute resolution mechanisms.
    * Offers a user-friendly interface.

### Snapshot X​

  * **Key features:**
    * Operates fully on-chain.
    * Reduces transaction costs by 10-50%.
    * Supports various voting strategies.
    * Integrates seamlessly with multiple blockchain networks.
    * Ensures a simple setup process.

### Vocdoni​

  * **Key features:**
    * Provides an open-source, decentralized, and universally verifiable voting protocol.
    * Prioritizes privacy, low costs, and scalability.
    * Maintains minimal marginal costs.
    * Conducts on-chain operations, saving on fees.

### Colony​

  * **Key features:**
    * Offers tools for project management and token issuance.
    * Employs a reputation-based decision-making process.

# Key features comparison

Feature| Aragon| Colony| Vocdoni| Snapshot X| Governance Tool  
---|---|---|---|---|---  
**Gasless voting**|  Offers gasless voting (LayerZero)| Does not offer gasless voting (low-cost only)| Provides gasless voting| Offers gasless voting (StarkNet)| Offers gasless voting (native Chromia tech)  
**EVM integration**|  Supports full integration| Supports full integration| Limited support| Provides full integration| Supports full integration  
**Multichain support**|  Provides full support (with Chromia-EVM bridge)| Does not support| Does not support| Does not support| Provides full support (Chromia-EVM bridge)  
**Automation of results**|  Offers partial automation (some integrations)| Does not offer automation| Does not offer automation| Provides limited automation (depends on setup)| Ensures full automation (auto proposal execution)  
**Customization of logic**|  Allows flexible customization (templates)| Offers moderate customization| Provides limited customization| Allows flexible customization (strategy plugins)| Ensures highly flexible customization  
**Cross-chain support**|  Supports cross-chain capabilities (via LayerZero)| Offers limited support| Does not support| Supports cross-chain capabilities (StarkNet-based)| Supports cross-chain capabilities (Chromia-EVM bridge)  
**Transparency**|  Ensures transparency| Ensures transparency| Ensures transparency| Ensures transparency| Ensures transparency  
**Ease of integration**|  Requires moderate technical knowledge| Requires moderate knowledge| Presents high barriers| Offers easy integration| Offers easy integration  
**Cost to operate**|  Maintains low operational costs (via LayerZero)| Maintains low costs| Maintains low costs| Maintains low costs| Ensures zero costs (gasless native Chromia)  
**Target audience size**|  Targets a large audience| Targets a moderate audience| Targets a niche audience| Targets a large audience| Targets a growing audience (focus on EVM DAOs)

### Getting started

SOURCE: https://docs.chromia.com/ecosystem/governance/getting-started/

This section provides step-by-step guidance on how to use the Governance Tool. You will learn how to set up governance, create and manage proposals, and participate in decentralized decision-making through voting.

[This section provides essential information about Rell, the custom programming language of the Chromia blockchain. Understanding Rell will enable you to use the Governance Tool Starter Kit effectively.](</ecosystem/governance/getting-started/rell-language>)[Begin with the Governance Tool Starter Kit to integrate decentralized governance into your project. Follow this step-by-step guide to set up governance, manage proposals, and enable voting mechanisms.](</ecosystem/governance/getting-started/governance-starter-kit>)[Understand the core components of the Governance Tool, including key modules, user roles, and the proposal lifecycle. Explore how decentralized decision-making operates within the system.](</ecosystem/governance/getting-started/governance-structure>)[Discover the available extensions for the Governance Tool, including the EIF, ICMF, and the delegates extension for synchronized transfers within decentralized applications (dapps).](</ecosystem/governance/getting-started/extensions>)

### Extensions

SOURCE: https://docs.chromia.com/ecosystem/governance/getting-started/extensions/

Extensions are modular components in the library that enhance the Governance Tool's functionality. They enable you to add new features without altering the core code.  
  
It's important not to confuse these extensions with [Chromia Extensions](<https://docs.chromia.com/intro/extensions>). The latter are optional Rell modules you can import into your dapp from the Governance Rell Library, making the Governance Tool more modular.

Each extension is a separate module that can be easily integrated into your dapp. Additionally, you'll find custom modules within the extensions that could be useful. Importing these is optional, and they can serve as a reference for building your own custom modules.

Currently, the following extensions are available:

[The EIF extension enables you to sync transfers within the dapp.](</ecosystem/governance/getting-started/extensions/eif>)[The ICMF extension allows you to synchronize transfers within the dapp.](</ecosystem/governance/getting-started/extensions/icmf>)[The Delegates extension facilitates the synchronization of transfers within the dapp.](</ecosystem/governance/getting-started/extensions/delegates>)

### Governance Tool Delegates extension

SOURCE: https://docs.chromia.com/ecosystem/governance/getting-started/extensions/delegates

The Governance Tool Delegates extension introduces delegate functionality to your dapp. This means that users can delegate their votes to a delegate, allowing the delegate to vote on proposals on behalf of the user, consolidating their voting power.

note

The calculation of voting power is performed for each individual vote. Therefore, the merging of voting powers for delegation occurs after each vote's power has been calculated.

## How to use​

To utilize the Delegates extension, you need to import the `lib.governance.extensions.delegates` module into your dapp.

No additional configuration in `chromia.yml` is needed.

### Features:​

  * Users can **delegate** their votes to a delegate using the operation `delegate_to(target_account_id: byte_array)`.
  * Users can **undelegate** their votes with the operation `undelegate()`.
  * Users can **override** their delegated votes using the operation `override_delegated_vote(option_item_id: rowid)`.

### Governance Tool EIF extension

SOURCE: https://docs.chromia.com/ecosystem/governance/getting-started/extensions/eif

The Governance Tool EIF extension enables synchronization with EVM events and handles them within the dapp. It uses Ethereum Interoperability Framework (EIF) to sync with EVM events.

## How to use​

To use the EIF extension, you need to import the `lib.governance.extensions.eif` module into your dapp.
    
    
    import lib.governance.extensions.eif;  
    

Also, you need to add the Ethereum Interoperability Framework module and library to your dapp in the `chromia.yml` file.
    
    
    # chromia.yml  
    
    
    blockchains:  
    
    
      your_dapp_name:  
    
    
        module: main # Your main module file name  
    
    
        config:  
    
    
          features:  
    
    
            merkle_hash_version: 2  
    
    
          gtx:  
    
    
            modules:  
    
    
              - "net.postchain.eif.EifGTXModule"  
    
    
          sync_ext:  
    
    
            - "net.postchain.eif.EifSynchronizationInfrastructureExtension"  
    
    
          eif:  
    
    
            snapshot:  
    
    
              version: 2  
    
    
            chains:  
    
    
              ethereum: # Your EVM chain name (bsc, ethereum, sepolia, tac_turin, soon more)  
    
    
                network_id: 1 # Network ID of the EVM chain  
    
    
                contracts_to_fetch: # Any number of contracts to fetch  
    
    
                  - address: "0x8a2279d4a90b6fe1c4b30fa660cc9f926797baa2" # Your contract address  
    
    
                    skip_to_height: 17024400 # Skip to the height of the contract  
    
    
                  - address: "0xC7b0F970c1EFBB181194Fc15ccD5C4a2c2Ab863B" # Your contract address  
    
    
                    skip_to_height: 17024400 # Skip to the height of the contract  
    
    
                evm_read_offset: 3 # EVM read offset doesn't need to change it  
    
    
                read_offset: 2 # Read offset doesn't need to change.  
    
    
                events: !include ./config/events.yaml # Events to fetch include the events you want to fetch from the contracts  
    
    
    libs:  
    
    
      icmf:  
    
    
        registry: https://gitlab.com/chromaway/core/directory-chain  
    
    
        path: src/lib/icmf  
    
    
        tagOrBranch: 1.82.4  
    
    
        rid: x"1A4B3C3A1325DEF2C426C4F0F93F7444BB074373A24367DAA958C32F21B2EA1D"  
    
    
        insecure: false  
    
    
      eif:  
    
    
        registry: https://gitlab.com/chromaway/postchain-eif  
    
    
        path: postchain-eif-rell/rell/src/eif  
    
    
        tagOrBranch: 0.13.1  
    
    
        rid: x"AB40EDB0B534726B5F3AA288A5D31BE840B24BA670775E4F95C1B2512F5B8F22"  
    
    
        insecure: false  
    
    
      eif_event_receiver:  
    
    
        registry: https://gitlab.com/chromaway/postchain-eif  
    
    
        path: postchain-eif-rell/rell/src/eif_event_receiver  
    
    
        tagOrBranch: 0.13.1  
    
    
        rid: x"B20700DA8FE4FC7E5880BCCCB6FBE0B6A22CD6FAB61CDF9CE20E06B62D4902F4"  
    
    
        insecure: false  
    

That is all you need to do to activate the EIF extension.

## Custom modules​

There are some custom modules inside the EIF extension that you can use to extend the functionality of the EIF extension. Furthermore, you can obtain a reference to build your own custom modules.

There are three custom modules inside the EIF extension:

  * `lib.governance.extensions.eif.erc20_chr`
  * `lib.governance.extensions.eif.vote_power_strategies`
  * `lib.governance.extensions.eif.vote_requirements`

### erc20_chr.rell​

This custom module is used to integrate full EVM CHR token support. It includes:

  * ETH, BSC, SEPOLIA, BSC_TESTNET **balance** and **stake** tracking
  * Automatic EVM token registration into the database
  * Extended **can_register** that can **check** users total evm CHR **balance** or **stake** or **both** in the **specified evm chains** from eif configuration in **chromia.yml**

**This** is what should be added to **chromia.yml** to use this module **additionally** to the EIF extension:
    
    
    # chromia.yml  
    
    
    blockchains:  
    
    
      your_dapp_name:  
    
    
        module: main # Your main module file name  
    
    
        config:  
    
    
          ...  
    
    
          eif:  
    
    
            ...  
    
    
            chains:  
    
    
              ethereum: # Your EVM chain name (bsc, ethereum, sepolia, bsc (same chain name for testnet of bsc), soon more)  
    
    
                network_id: 1 # Network ID of the EVM chain  
    
    
                contracts_to_fetch:  
    
    
                  - address: '0x8a2279d4a90b6fe1c4b30fa660cc9f926797baa2' # CHR token contract address  
    
    
                    skip_to_height: 17024400 # Skip to the height of the contract  
    
    
                  - address: '0xC7b0F970c1EFBB181194Fc15ccD5C4a2c2Ab863B' # CHR stake contract address  
    
    
                    skip_to_height: 17024400 # Skip to the height of the contract  
    
    
                evm_read_offset: 3 # EVM read offset doesn't need to change it  
    
    
                read_offset: 2 # Read offset doesn't need to change it  
    
    
                events: !include ./config/events.yaml # Events to fetch include the events you want to fetch from the contracts  
    
    
              bsc:  
    
    
                network_id: 56  
    
    
                contracts_to_fetch:  
    
    
                  - address: '0xf9CeC8d50f6c8ad3Fb6dcCEC577e05aA32B224FE' # Bsc CHR token contract address  
    
    
                    skip_to_height: 42419297 # Skip to the height of the contract  
    
    
                  - address: '0x6414FbBf1CC9d9253125e106435619577bAAc2aC' # Bsc CHR stake contract address  
    
    
                    skip_to_height: 42419297 # Skip to the height of the contract  
    
    
                evm_read_offset: 3 # EVM read offset doesn't need to change it  
    
    
                read_offset: 2 # Read offset doesn't need to change it  
    
    
                events: !include ./config/events.yaml # Events to fetch include the events you want to fetch from the contracts  
    
    
              ...  
    
    
        moduleArgs:  
    
    
          ...  
    
    
          lib.governance.extensions.eif.erc20_chr:  
    
    
            required_total_evm_chr_staked_balance_for_registration: 1000000000 # 1000 CHR  
    
    
            required_total_evm_chr_balance_for_registration: 1000000000 # 1000 CHR  
    

And **events.yaml** :
    
    
    # events.yaml  
    
    
      - anonymous: 0  
    
    
        inputs:  
    
    
          - indexed: 1  
    
    
            name: from  
    
    
            type: address  
    
    
      
    
    
      
    
    
          - indexed: 1  
    
    
            name: to  
    
    
            type: address  
    
    
      
    
    
          - indexed: 0  
    
    
            name: value  
    
    
            type: uint256  
    
    
        name: Transfer  
    
    
        type: event  
    
    
      
    
    
      - anonymous: 0  
    
    
        inputs:  
    
    
          - indexed: 1  
    
    
            name: from  
    
    
            type: address  
    
    
      
    
    
          - indexed: 0  
    
    
            name: balance  
    
    
            type: uint64  
    
    
        name: StakeUpdate  
    
    
        type: event  
    

### erc20_chr_vote_power_strategies.rell​

This custom module can be imported to add a total EVM CHR balance vote power calculation strategy that can be used on proposals.

**Requires** exactly **same** configuration as **erc20_chr** module. And it **activates** erc20_chr module automatically.

### vote_requirements.rell​

This custom module can be imported to add a **vote requirement** that **checks** your specified **evm token symbol** and **specified balance amount** to **restrict** users that have **less** than specified balance amount from voting.

Only import the module into your dapp; no additional configuration is required.

### Governance Tool ICMF extension

SOURCE: https://docs.chromia.com/ecosystem/governance/getting-started/extensions/icmf

The Governance Tool ICMF extension adds ICMF functionality to your decentralized application (dapp). It is important not to confuse it with the internal ICCF module of Chromia. The ICMF extension tracks ICMF CHR stake update messages, enabling the tracking of both economy chain and EVM chain stakes.

Since the data from the ICMF message pertains to both EVM and the Economy Chain, we cannot rely solely on the native stake balance to determine the Economy Chain stake balance.

This extension module does not require the EIF and can be used either with or without it.

The EVM data obtained comes from the Economy Chain, which sends stake updates from all chains as a single message, making it impossible to differentiate between EVM and Economy Chain stakes.

## How to use​

To utilize the ICMF extension, import the `lib.governance.extensions.icmf` module into your dapp.

## Configuration​
    
    
    blockchains:  
    
    
      your_dapp_name: # your dapp name  
    
    
        module: main  
    
    
        config:  
    
    
          features:  
    
    
            merkle_hash_version: 2  
    
    
          gtx:  
    
    
            modules:  
    
    
              - "net.postchain.d1.icmf.IcmfSenderGTXModule"  
    
    
              - "net.postchain.d1.icmf.IcmfReceiverGTXModule"  
    
    
              - "net.postchain.d1.iccf.IccfGTXModule"  
    
    
          sync_ext:  
    
    
            - "net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension"  
    
    
          icmf:  
    
    
            receiver:  
    
    
              local:  
    
    
                - topic: "G_staking_state_update"  
    
    
                  bc-rid: x"15C0CA99BEE60A3B23829968771C50E491BD00D2E3AE448580CD48A8D71E7BBA"  
    
    
              global:  
    
    
                topics:  
    
    
                  - G_staking_state_update  
    
    
    libs:  
    
    
      ...  
    
    
      iccf:  
    
    
        registry: https://gitlab.com/chromaway/core/directory-chain  
    
    
        path: src/lib/iccf  
    
    
        tagOrBranch: 1.76.3  
    
    
        rid: x"050298A4A457D8E8922CF662765739A3A16B3256C49BE646FA7D198ECF0214C5"  
    
    
        insecure: false  
    
    
      icmf:  
    
    
        registry: https://gitlab.com/chromaway/core/directory-chain  
    
    
        path: src/lib/icmf  
    
    
        tagOrBranch: 1.82.6  
    
    
        rid: x"B56524EE85B679371EAB1A069B03BE6A3E94F36993DEA900747BA5BAE8B99122"  
    

## Custom modules​

The ICMF module includes several custom modules that can inspire you or be used independently to facilitate implementation. These modules are optional and must be imported into your main module in addition to the icmf extension module. There are three custom modules:

  * **icmf_register_requirements.rell** : This module defines registration requirements based on native stake data from ICMF, which combines the total value of EVM stake and Economy Chain stake.
  * **icmf_vote_power_strategies.rell** : This module establishes vote power strategies derived from CHR stake balance for a given dapp.
  * **icmf_vote_requirements.rell** : This module outlines vote requirements based on native stake data from ICMF, which combines the total value of EVM stake and Economy Chain stake.

### icmf_register_requirements.rell​

This custom module introduces a new requirement in the registration process. It mandates that a specific native stake balance must be staked in the dapp.

#### Configuration:​
    
    
    blockchains:  
    
    
      your_dapp_name:  
    
    
        module: main  
    
    
        ...  
    
    
        moduleArgs:  
    
    
          lib.governance.extensions.icmf.icmf_register_requirements:  
    
    
            required_native_stake_balance: 1000000 # 1 CHR because we use 10^6 (6 decimals) as a base for CHR  
    

#### Operations:​

`operation update_native_stake_register_requirements(required_native_stake_balance: big_integer)`

This operation updates the required native stake balance for the registration process and is accessible when this custom module is imported.

### icmf_vote_power_strategies.rell​

This custom module introduces a new vote power strategy to the Governance Tool. It utilizes the native stake balance to calculate the vote power.

No additional configuration is necessary for this module. It adds one vote power strategy:

  * TOTAL_CHR_STAKE: This strategy determines vote power based on the user's total CHR staked balance across both EVM and the Economy Chain.

### icmf_vote_requirements.rell​

This custom module establishes a new vote requirement for the Governance Tool. It utilizes the native stake balance to calculate the vote requirement.

No additional configuration is needed for this module. It includes two vote requirements:

  * TOTAL_CHR_STAKE: This requirement determines vote power based on the user's total CHR staked balance across both EVM and the Economy Chain.
  * TOTAL_CHR_STAKE_SNAPSHOT: This requirement determines vote power based on the user's total CHR staked balance across both EVM and the Economy Chain at a specific snapshot time.

### Quickstart

SOURCE: https://docs.chromia.com/ecosystem/governance/getting-started/governance-starter-kit/

Unlock decentralized governance with the Governance Tool Starter Kit. This guide helps you set up and manage governance using Chromia’s modular system.

[Start with the Governance Tool Starter Kit, designed for developers to integrate decentralized governance easily, highlighting its modular architecture and key features.](</ecosystem/governance/getting-started/governance-starter-kit/overview>)[Discover how to configure governance using the Governance Tool Library, covering installation, settings, authentication, proposals, citizen rules, and voting mechanisms.](</ecosystem/governance/getting-started/governance-starter-kit/import>)[Customize governance in `chromia.yml` by managing snapshots, tracking events, and setting up event listeners for governance actions like token transfers and staking updates.](</ecosystem/governance/getting-started/governance-starter-kit/eif-configuration>)[Tailor governance functions to your DAO’s needs, including defining permissions, proposal rules, voting logic, citizen registration, veto mechanisms, and multi-chain governance.](</ecosystem/governance/getting-started/governance-starter-kit/customize-functions>)

### Customizing functions

SOURCE: https://docs.chromia.com/ecosystem/governance/getting-started/governance-starter-kit/customize-functions

The Governance Tool allows you to create reusable code components tailored to your Decentralized Autonomous Organization (DAO) environment by utilizing abstract modules and functions. The key overridden functions described below define governance-related permission checks and decision-making logic within the system. It is essential to customize these functions to establish governance rules that align with your specific DAO, proposal system, or blockchain environment. All functions that start with the prefix `can_` are abstract functions that can be overridden to customize the governance system's behavior.

Note that some descriptions of the functions are derived from the Governance EVM tool implementation. You will notice a "default" label if a function is a default implementation and has not been overridden from the Governance Library.

tip

Most of the overridden functions are similar to the default implementations of the abstract functions. They are provided as examples to make it easier for you to understand their purpose, rather than requiring you to search through the library code.

## Key functions​

### Citizen and registration permissions​

#### `can_update_citizen_type` (default implementation)​

This function ensures that only admins can modify a citizen’s type.

#### `can_register` (extendable function)​

This extended version of the function, available in the eif extension, checks whether a wallet address meets the required staked CHR or locked CHR balance across multiple blockchains (ETH/BSC). Developers can extend this function to customize the entry criteria for governance participation.

### Proposal management​

#### `can_create_draft_proposal` (default implementation)​

This function checks that a citizen is not on cooldown before submitting a draft proposal.

#### `can_use_proposal_parameters` (default implementation)​

This function validates whether the proposal duration, options, and requirements comply with configured limits (e.g., minimum duration, number of options).

#### `can_start_voting_on_proposal` (default implementation)​

This function ensures that only the proposal creator can initiate the voting process.

#### `can_finalize_proposal`​

This function sets the conditions for finalizing a proposal, ensuring that:

  * The veto period has ended.
  * The proposal is in the `ON_VOTING`, `VOTING_ENDED`, or `ON_VETO` state.
  * An admin can bypass the veto period if necessary.

#### `can_close_proposal` (default implementation)​

This function allows a proposal to close only when it has transitioned to the correct `CLOSED` state.

#### `can_change_proposal_config` (default implementation)​

This function restricts modifications to proposal configuration parameters to admins only.

#### `can_verify_draft_proposal` (default implementation)​

This function ensures that only councilors or admins can verify a draft proposal.

#### `can_approve_proposal` (default implementation)​

This function permits only councilors or admins to approve a proposal, which must be in the draft stage.

#### `can_vote` (default implementation)​

This function allows a citizen to vote only if:

  * The citizen is not the proposal's author (to prevent self-voting).

### Veto system​

#### `can_approve_or_disapprove_veto` (default implementation)​

This function ensures that only councilors or admins can approve or reject a veto request, provided the veto period is still active.

#### `can_finalize_veto` (default implementation)​

This function permits the finalization of a veto if:

  * The veto period has expired.
  * All councilors have voted.
  * The majority approval or disapproval threshold is met.

#### `can_create_veto` (default implementation)​

This function allows a councilor or admin to initiate a veto, provided they are not on a veto cooldown.

tip

You can find default implementations for abstract functions, providing a quick starting point.

## Why override these functions?​

  * Customize governance rules: Adjust voting power, registration criteria, and proposal approval processes according to your DAO's policies.
  * Enforce security and access control: Prevent unauthorized actions by restricting permissions to admins and councilors.
  * Optimize voting and proposal mechanisms: Tailor how vetoes, approvals, and vote durations function to meet your DAO's specific needs.
  * Multi-chain governance: Modify rules for users participating with CHR on Ethereum or Binance Smart Chain.

### Customizing with EIF and event configuration

SOURCE: https://docs.chromia.com/ecosystem/governance/getting-started/governance-starter-kit/eif-configuration

This topic provides a detailed explanation of the EIF configuration in `chromia.yml`, which defines chain-specific parameters for tracking events and contracts. Before diving into the configurations below, we strongly recommend reviewing the [EVM Event Receiver Configuration](<https://gitlab.com/chromaway/core/postchain-eif/-/blob/dev/doc/event-receiver-chain-configuration.md?ref_type=heads>) section to become more familiar with the topic.

## Chain configuration​

The `chains` section specifies blockchain-related parameters, including network IDs, contract addresses, and event tracking.

### Binance Smart Chain (BSC) Testnet configuration​
    
    
    blockchains:  
    
    
      <event_receiver_name>:  
    
    
        module: <module_name>  
    
    
        config:  
    
    
          eif:  
    
    
            chains:  
    
    
              bsc:  
    
    
                network_id: 97  
    
    
                contracts:  
    
    
                  - "0x753218363422002DF74F3D0D8d67f6CB38bE32D0" # Testnet BSC TwoWeeksNotice contract (staking)  
    
    
                  - "0x8A2279d4A90B6fe1C4B30fa660cC9f926797bAA2" # Testnet BSC CHR token contract (tCHR)  
    
    
                skip_to_height: 26282055  
    
    
                events: !include ./config/events.yaml  
    

  * **`network_id`** : Identifies the Binance Smart Chain (BSC) Testnet (ID: 97).
  * **`contracts`** :
    * `0x753218363422002DF74F3D0D8d67f6CB38bE32D0`: Represents the `TwoWeeksNotice` staking contract on the BSC Testnet.
    * `0x8A2279d4A90B6fe1C4B30fa660cC9f926797bAA2`: Represents the Testnet CHR token (`tCHR`).
  * **`skip_to_height`** : Specifies the block height from which to start tracking events.
  * **`events`** : References an external configuration file (`events.yaml`) that defines event tracking rules.

## Events configuration for Chromia EVM governance dapp​

This section provides an overview of the EVM events configured in the `events.yaml` file. These events are monitored and processed within the Chromia EVM governance dapp.

### Events overview​

The `events.yaml` file defines EVM events that are crucial for tracking governance-related activities. Each event includes attributes such as indexed parameters, types, and event names. The current configuration supports the following events:

#### `Transfer` event​

The `Transfer` event is a standard event in EVM-based blockchains, commonly used to track token transfers. It captures details about token movements from one address to another.

#### Event definition​
    
    
    Transfer:  
    
    
      parameters:  
    
    
        - name: from  
    
    
          type: address  
    
    
          indexed: true  
    
    
        - name: to  
    
    
          type: address  
    
    
          indexed: true  
    
    
        - name: value  
    
    
          type: uint256  
    

#### Parameters​

  * **from** (`address`, indexed): The sender's address.
  * **to** (`address`, indexed): The recipient's address.
  * **value** (`uint256`): The amount transferred.

#### Processing in governance dapp​

  * This event is monitored to track token transfers relevant to governance transactions.
  * It helps identify governance token movements, ensuring correct allocation and participation tracking.

### `StakeUpdate` event​

The `StakeUpdate` event is used to track changes in staking balances, which are essential for governance participation and voting power.

#### Event definition​
    
    
    StakeUpdate:  
    
    
      parameters:  
    
    
        - name: from  
    
    
          type: address  
    
    
          indexed: true  
    
    
        - name: balance  
    
    
          type: uint64  
    

#### Parameters​

  * **from** (`address`, indexed): The address whose stake is updated.
  * **balance** (`uint64`): The new stake balance.

#### Processing in governance dapp​

  * This event is monitored to update governance staking records.
  * Changes in stake balance influence voting power and governance participation.

## Integration with Chromia EVM governance dapp​

  * The dapp listens for these events using an event listener integrated with the EVM-compatible smart contract.
  * When an event is emitted, the dapp processes the data and updates governance-related records accordingly.
  * The `Transfer` event helps track token ownership, while `StakeUpdate` ensures accurate staking records for voting mechanisms.

### Configuration elements​

This section outlines the configuration elements from the YAML-like structure provided in the repository:

**EIF test configuration ( &eif_test):**

  * `chains`:
    * **bsc:** Configuration for the Binance Smart Chain (BSC):
      * `network_id`: BSC Testnet network ID.
  * `contracts`: Addresses of two Testnet contracts for staking and CHR tokens.
    * `skip_to_height`: Block height to start processing from.
    * `events`: External event configuration from `./config/events.yaml`.

**Common arguments ( &common_args):**

  * `lib.ft4.core.accounts`: Configuration for account rate limits.
  * `lib.ft4.core.admin`: Admin public key.
  * **lib.governance:**
    * `admin_pubkey`, `admin_evm_wallet`
    * `economy_chain_bridge`
    * `required_total_evm_chr_staked_balance_for_registration`, `required_total_evm_chr_balance_for_registration`
    * **lib.governance.citizens:** Configurations related to citizen cooldowns, proposal rejections, and vetoes:
      * Settings such as `citizen_cooldown_days_on_draft_proposal` and `citizen_cooldown_days_on_proposal_rejection` are defined with specific cooldown periods.
    * **lib.governance.proposals:** Configuration for proposals:
      * `proposal_configs`:
        * `option_item_limit`: 10
        * `max_duration`: 2592000000000
        * `min_duration`: 3600000
        * `title_min_length`: 3
        * `title_max_length`: 100
        * `category_min_length`: 3
        * `category_max_length`: 30
        * `option_min_length`: 3
        * `option_max_length`: 30
        * `description_max_length`: 10000
    * **lib.governance.extensions.eif:** Sync transfers within the dapp:
      * `sync_transfers_inside_dapp`: true (This is true by default; there's no need to define it here. However, if set to false, stake and balance handling will be disabled.)
    * **lib.governance.votes:** Configuration for veto periods:
      * `veto_config`:
        * `veto_period`: 43200000 (Set to 72 hours in milliseconds)
    * **lib.governance.auth:** Optional configuration for authorization public key for governance, if using a centralized auth server.

**Compile version:**

  * `rellVersion`: Compilation version (0.14.9).

### Activate EIF extension​

To activate the EIF extension, import the eif extension into your dapp's module, similar to how you would import ft4 library modules:
    
    
    import lib.governance.extensions.eif;  
    

### EIF libraries overview​

The libraries used in the governance project form part of the **Ethereum Interoperability Framework (EIF)** and serve distinct purposes for enabling cross-chain communication between EVM and Postchain nodes.

For detailed information about EIF libraries, refer to the [EIF documentation](<https://gitlab.com/chromaway/core/postchain-eif/-/tree/dev/doc?ref_type=heads>).

**Libraries**

`eif`

  * **Registry** : [GitLab Repository](<https://gitlab.com/chromaway/postchain-eif>)
  * **Path** : `postchain-eif-rell/rell/src/eif`
  * **Version** : `0.9.0`
  * **Resource ID (RID)** : `x"D9FAA2AA88DEE0D1EAAFDFE8E28C1BAD89284CE4B6E75C1DBA9272DD8C3D944E"`
  * **Insecure** : `false`
  * **Description** : The core EIF library that manages the main framework for interoperability.

`eif_event_receiver`

  * **Registry** : [GitLab Repository](<https://gitlab.com/chromaway/postchain-eif>)
  * **Path** : `postchain-eif-rell/rell/src/eif_event_receiver`
  * **Version** : `0.9.0`
  * **Resource ID (RID)** : `x"BDEB142851437327590715FF0912258D5435D294A59857B545BF69D27A0ADBB0"`
  * **Insecure** : `false`
  * **Description** : A sub-library for receiving and processing events emitted by EVM nodes or other components.

`eif_event_connector`

  * **Registry** : [GitLab Repository](<https://gitlab.com/chromaway/postchain-eif>)
  * **Path** : `postchain-eif-rell/rell/src/eif_event_connector`
  * **Version** : `0.9.0`
  * **Resource ID (RID)** : `x"4A669C5F98AEE970FECD5B77116E737196C7D3C7C6217DFD7EAC2F9317FC9461"`
  * **Insecure** : `false`
  * **Description** : Handles the integration and connection of event data into the EIF workflow, enabling seamless data transfer between systems.

These libraries work together to form the backbone of the EIF framework:

  * The **`eif`** library manages the core interoperability features.
  * The **`eif_event_receiver`** library ensures efficient event listening and processing.
  * The **`eif_event_connector`** library integrates event data into the framework.

## Dynamic configuration operations in EIF​

We introduce three dynamic configuration operations that enhance adaptability by allowing real-time updates to event and contract tracking capabilities:

### Operations​

#### `add_new_eif_events`​

  * **Purpose** : Enables administrators to dynamically add new events to be tracked by the framework.
  * **Input** : A list of event configurations (`eif_dynamic_config.event_config`).
  * **Benefit** : Provides flexibility for monitoring evolving blockchain activity.

#### `add_new_eif_contracts`​

  * **Purpose** : Allows administrators to register new contracts within EIF.
  * **Input** : A list of contract configurations (`eif_dynamic_config.contract_config`).
  * **Benefit** : Facilitates seamless integration of additional contract systems.

### `add_new_evm_erc20_and_eif_contract`​

  * **Purpose** : This function simplifies the process of adding new ERC20 tokens and their corresponding contracts to the EIF.
  * **Input parameters** :
    * Network ID
    * Token Address
    * Token Name
    * Token Symbol
    * Decimals
  * **Functionality** : It automatically registers the token on the specified EVM chain and links it as a contract in the EIF.

### Key advantages​

These operations significantly enhance the modularity of the EIF by:

  * Allowing dynamic onboarding of tokens, contracts, and event tracking.
  * Minimizing the need for manual intervention.
  * Improving scalability in multi-chain environments.

note

Additionally, you can update the configuration in the `chromia.yml` file and redeploy the dapp to add new events or contracts.

### Importing governance into your project

SOURCE: https://docs.chromia.com/ecosystem/governance/getting-started/governance-starter-kit/import

To build and run the Governance Tool on Chromia, you need to set up the project and properly configure the blockchain to connect the frontend with the Rell backend for efficient data handling. Before proceeding, we recommend reviewing the [project settings file](</build/configuration/project-config>) section once again for details about libraries or the project settings file.

## Governance as a library​

The `libs` section defines external libraries used in your project. It specifies the library name, registry URL, path within the repository, optional branch or tag, RID, and the option for insecure download verification.

  1. Add the following entries under the `libs` section in your `chromia.yml` file:

    
    
    libs:  
    
    
      pagination:  
    
    
        registry: ssh:/git@bitbucket.org:chromawallet/lib-pagination-utils.git  
    
    
        path: rell/src/core/pagination  
    
    
        tagOrBranch: master  
    
    
        insecure: true  
    
    
      ft4:  
    
    
        registry: https://gitlab.com/chromaway/ft4-lib.git  
    
    
        path: rell/src/lib/ft4  
    
    
        tagOrBranch: v1.0.0r  
    
    
        rid: x"FA487D75E63B6B58381F8D71E0700E69BEDEAD3A57D1E6C1A9ABB149FAC9E65F"  
    
    
        insecure: false  
    
    
      governance:  
    
    
        registry: git@bitbucket.org:chromawallet/originals-governance.git  
    
    
        path: rell/src/governance  
    
    
        tagOrBranch: production  
    
    
        insecure: true  
    

note

If you're using the [Governance Starter Kit repository](<https://bitbucket.org/chromawallet/governance-starter-kit/src/production/>), you'll find this library already included in your `chromia.yml` file, along with common arguments and other libraries such as [`eif`](<https://gitlab.com/chromaway/core/postchain-eif/-/tree/dev/doc?ref_type=heads>), [`ft4`](</build/ft4/intro>), and [`pagination`](</build/ft4/pagination>).

  2. Customize the parameters in your `config.yml` file as shown below.

## Common arguments​

Utilize a reference (`&common_args`) to standardize settings across all modules.

## Accounts configuration​

Manage rate limiting for user accounts in the `lib.ft4.core.accounts` section:
    
    
    blockchains:  
    
    
      <my_chain_name>:  
    
    
        module: <module_name>  
    
    
        moduleArgs:  
    
    
          lib.ft4.core.accounts:  
    
    
            rate_limit:  
    
    
              active: false  
    
    
              max_points: 10  
    
    
              recovery_time: 5000  
    
    
              points_at_account_creation: 1  
    

  * **`active`** : Enables or disables rate limiting.
  * **`max_points`** : The maximum points allowed before rate limiting is triggered.
  * **`recovery_time`** : Sets the recovery time (in milliseconds).
  * **`points_at_account_creation`** : Initial points granted to a new account.

When setting `points_at_account_creation` for rate limiting, a user can perform at most one operation per time unit. The recovery time for these points can be set using the `recovery_time` property, which defaults to 5000 ms. Additionally, you can configure the `max_points` an account can reach, or disable rate limiting altogether by setting `active` to false.

## Administrative configuration​

Define your administrative controls in the `lib.ft4.core.admin` and `lib.governance` sections:
    
    
    blockchains:  
    
    
      <my_chain_name>:  
    
    
        module: <module_name>  
    
    
        moduleArgs:  
    
    
          lib.ft4.core.admin:  
    
    
            admin_pubkey: `your_admin_pubkey`  
    

  * **`admin_pubkey`** : The public key of your system administrator.

## Authentication settings​

Specify authorized operations in the `lib.ft4.core.auth` section:
    
    
    blockchains:  
    
    
      <my_chain_name>:  
    
    
        module: <module_name>  
    
    
        moduleArgs:  
    
    
          lib.ft4.core.auth:  
    
    
            evm_signatures_authorized_operations:  
    
    
              - governance.citizens.register_citizen  
    

  * **`evm_signatures_authorized_operations`** : Defines operations that require EVM signatures.

## Governance parameters​

Manage staking, citizen participation, and voting power through the governance module:
    
    
    blockchains:  
    
    
      <my_chain_name>:  
    
    
        module: <module_name>  
    
    
        moduleArgs:  
    
    
          lib.governance:  
    
    
            admin_pubkey: `your_admin_pubkey`  
    
    
            admin_evm_wallet: `your_admin_evm_wallet`  
    
    
            economy_chain_brid: `economy_chain_brid`  
    
    
            required_staked_chr_in_eth: `required_staked_chr_in_eth`  
    
    
            required_staked_chr_in_bnb: `required_staked_chr_in_bnb`  
    
    
            required_chr_balance: `required_chr_balance`  
    
    
            required_staked_chr_balance: `required_staked_chr_balance`  
    

  * **`admin_evm_wallet`** : The EVM-compatible wallet for administration.
  * **`economy_chain_brid`** : The identifier for the Economy Chain.
  * **`required_staked_chr`** : The minimum CHR staking requirements.

## Citizen participation rules​

Define cooldown periods for various governance actions:
    
    
    blockchains:  
    
    
      <my_chain_name>:  
    
    
        module: <module_name>  
    
    
        moduleArgs:  
    
    
          lib.governance.citizens:  
    
    
            citizen_cooldown_days_on_draft_proposal: 0  
    
    
            citizen_cooldown_days_on_proposal_rejection: 0  
    
    
            councilor_cooldown_days_on_veto: 30  
    
    
            min_draft_cooldown_days: 1  
    
    
            max_draft_cooldown_days: 365  
    
    
            min_rejection_cooldown_days: 1  
    
    
            max_rejection_cooldown_days: 365  
    
    
            min_veto_cooldown_days: 1  
    
    
            max_veto_cooldown_days: 365  
    

  * **Cooldown Periods** : Establish restrictions on re-engaging in governance activities.

## Proposal and voting configuration​

Set your proposal parameters and veto settings:
    
    
    blockchains:  
    
    
      <my_chain_name>:  
    
    
        module: <module_name>  
    
    
        moduleArgs:  
    
    
          lib.governance.proposals:  
    
    
            proposal_configs:  
    
    
              option_item_limit: 10  
    
    
              max_duration: 2592000000000  
    
    
              min_duration: 3600000  
    

  * **`option_item_limit`** : The maximum number of choices per proposal.
  * **max_duration / min_duration** : Time limits for proposals.

    
    
    blockchains:  
    
    
      <my_chain_name>:  
    
    
        module: <module_name>  
    
    
        moduleArgs:  
    
    
          lib.governance.votes:  
    
    
            veto_config:  
    
    
              veto_period: 100000  
    

  * **`veto_period`** : This specifies the time period during which a veto can be initiated after a proposal is approved or an event occurs. During this time, entities eligible to veto can challenge the decision.

The veto configuration ensures balance by giving veto powers only a limited window while protecting decisions from being perpetually subject to veto threats.

## Installing dependencies​

Use the install command (`chr install`) to download and use third-party Rell libraries in your dapp:
    
    
    chr install  
    

After installing the Rell dependencies, you will find a folder named `governance` placed under the `lib` directory, alongside other built-in libraries such as `eif` and `ft4`.

# Importing governance into your project

Once the Rell dependencies are installed, access the **governance** library located in the `lib` folder, ready for use upon proper import. When you import the entire governance library, you may unintentionally expose numerous operations. Carefully consider whether you want to import each module.

To provide you with more control, the library is divided into separate modules.

## Governance library modules​

Below are the modules that compose the governance library. Familiarizing yourself with these will help you understand the operations available to users.

  * **`citizens`** : Manages user registrations, verifications, and roles within the governance system.
  * **`auth`** : Provides a way for centralized authentication server usage for registration, which is an optional feature.
  * **`proposals`** : Facilitates the creation, review, and tracking of governance proposals.
  * **`votes`** : Enables secure voting mechanisms and tallies votes for proposals.
  * **Extensions** : Extensions are optional modules that can be used to extend the functionality of the Governance Tool.
    * **`eif`** : Provides cross-chain communication between EVM nodes and Postchain nodes via event emissions. Required for EVM chain-based voting.
    * **`icmf`** : Manages the ICMF (Interchain Message Format) for cross-chain communication, useful for acquiring stake data from the Economy Chain.
    * **`delegates`** : Enables and manages delegates for the Governance Tool.

To integrate specific governance functionalities into the main application module, include the following import statements in the `main.rell` file:
    
    
    import lib.governance.citizens;  
    
    
    import lib.governance.proposals;

### Governance Starter Kit

SOURCE: https://docs.chromia.com/ecosystem/governance/getting-started/governance-starter-kit/overview

The [Governance Tool Starter Kit](<https://bitbucket.org/chromawallet/governance-starter-kit/src/production/>) is a developer-focused toolkit designed to simplify the creation and deployment of decentralized governance systems on Postchain. It empowers communities to achieve efficient and transparent decision-making. This kit includes the Governance Tool library, which allows you to demonstrate and simulate essential governance functionalities such as voting mechanisms, proposal management, and decision-making workflows within decentralized systems.

In contrast to the Governance Tool—which is a complete implementation ready for immediate deployment—the Starter Kit serves as a modular library. This design enables developers to integrate the kit into their applications, offering flexibility and expediting the prototyping process while maintaining compatibility with the core governance system.

## Key differences​

  * **Governance Tool** : A comprehensive implementation that can be deployed as-is.
  * **Governance Tool Starter Kit** : A lightweight library derived from the original project, enabling developers to leverage essential governance functionalities without having to start from scratch.

For instance, if you want to create a custom voting system for your DAO, you can use the Starter Kit to save time and quickly launch your DAO with a basic template.

### Governance structure

SOURCE: https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/

This section offers an overview of the Governance Tool, focusing on its key modules, user roles, and the proposal lifecycle in a decentralized decision-making framework.

[Learn about the fundamental modules of the Governance Tool, including citizen registration, proposal management, voting, and veto mechanisms. Discover how these elements create a transparent, decentralized governance system.](</ecosystem/governance/getting-started/governance-structure/key-modules>)[Understand the different user roles in the Governance Tool: Viewers, Citizens, Counselors, and Admins, along with their specific permissions and responsibilities.](</ecosystem/governance/getting-started/governance-structure/user-types>)[Explore the processes in the Governance Tool, from user registration and ranking to proposal creation, validation, voting, vetoes, and execution.](</ecosystem/governance/getting-started/governance-structure/user-proposal-flows>)

### Key modules in the governance system

SOURCE: https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/key-modules

This topic outlines the core components of the Governance Tool, detailing how citizens interact with proposals, voting, and the veto mechanism.

## Citizens​

Citizens play an essential role in governance, engaging in key processes such as registration and role management.

  * **Registration** : Citizens register using an external authentication mechanism, providing an authentication descriptor and a signature for account creation.

  * **Role management** : Administrators assign and manage roles, defining responsibilities within the governance system.

## Proposals​

Proposals form the backbone of the governance process and are structured into distinct phases.

  * **Proposal creation**  
Citizens draft and submit proposals to initiate governance decisions.

    * **Draft submission** : Eligible citizens create proposals by providing details such as title, category, and duration.
    * **Validation and authorization** : Only registered citizens can submit draft proposals.
    * **Cooldown enforcement** : A cooldown period may apply before a citizen can submit another proposal.
  * **Draft verification**  
Proposals undergo review before advancing.

    * **Councilor review** : A councilor evaluates each draft to ensure its readiness.
    * **Status update** : The system updates the draft’s status after review.
  * **Voting**  
Verified proposals move to the voting phase, allowing citizens to cast their votes.

  * **Finalization**  
The system processes and archives the final decision.

    * **Completion** : The system marks the proposal as finalized.
    * **Outcome storage** : Voting results and decisions are archived for future reference.

## Votes​

The voting mechanism empowers citizens to shape governance outcomes.

  * **Casting votes** : Citizens select from available options on a proposal.
  * **Finalizing votes** : The system automatically concludes the voting phase once the deadline passes.

## Veto mechanism​

The veto mechanism safeguards against potentially harmful proposals.

  * **Veto period** : There is a specific timeframe, called the veto period, during which a veto can be initiated after voting has concluded. For example, if the veto period is set to 12 hours, a permissioned group of citizens can veto within 12 hours after voting has finished.
  * **Initiating a veto** : Citizens can propose a veto against specific proposals or options.
  * **Voting on a veto** : Citizens decide whether to approve or reject the veto.
  * **Finalizing a veto** : If conditions are met, the system enforces the veto, preventing the proposal from proceeding.

### User and proposal flows

SOURCE: https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/user-proposal-flows

## User flow​

Users begin by exploring the Governance Tool, which provides essential information about the governance system and its processes.

### Registration​

Users register using a MetaMask wallet, which automatically creates:

  * A citizen account
  * An FT4 account

### Rank advancement​

Users can progress within the governance system through active participation:

  * Citizens may be promoted to counselors based on an admin decision or through a successful governance proposal.

### Ban (upcoming feature)​

A ban list will enable admins to remove non-compliant users from the governance system.

### Cooldown periods​

To prevent spam and ensure fair governance, cooldown mechanisms will regulate actions:

  * Proposal creation cooldown: Limits how often users can submit proposals.
    * Default setting: Disabled in the demo environment.
    * Admins and counselors can enable and configure cooldowns.
  * Veto execution cooldown: Restricts the frequency with which veto actions can be performed.

## Proposal flow​

### Creating a proposal​

Citizens can draft proposals by providing the following information:

  * Title
  * Category
  * Description
  * Duration
  * Voting options and commands

### Validation​

  * The system automatically validates proposals upon saving.
  * It checks for compliance with predefined rules, such as character limits and content flags.
  * Validation logic is configurable.

### Approval​

  * Proposals require admin or counselor approval through a voting process.
  * Demo setting: Admins must manually save a draft before clicking approve.

### Verification​

  * The system verifies if a proposal meets quorum requirements.
  * Verification occurs after each approve or disapprove vote.
  * Demo rule: At least 50 percent of counselors must approve a proposal for it to be verified.

### Voting​

Citizens vote based on:

  * Token balance or stake
  * Voting power strategies, such as linear, quadratic, or hybrid models

The voting period ends when:

  * The set time limit expires.
  * A predefined threshold is met.
  * Demo feature: Admins can manually end voting.

### Veto​

  * Admins and counselors can veto proposals within a defined period.
  * The veto process is separate from regular voting:
    * Veto decisions go through an approval or disapproval phase, requiring additional review before finalization.
    * When a veto is initiated, the proposal moves into a special "ON_VETO" state, suspending the normal decision-making process.
    * Vetoing requires additional checks, making it a privileged action restricted to specific roles (admins and counselors).
    * Vetoing acts as a blocking mechanism, preventing a proposal or option from proceeding unless the veto is overturned.

### Execution​

  * The system executes commands from the winning proposal.
  * After execution, participants receive a refund of their voting power.

### Adding a new user type

SOURCE: https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/user-types

The governance system comprises four main user roles, each with specific rights and responsibilities.

## Viewer (not registered user)​

**Description:**  
Viewers can access governance information without needing to register or authenticate.

**Rights:**

  * View active and past proposals.
  * Observe voting results and proposal outcomes.
  * Explore the governance structure and system documentation.

**Restrictions:**

  * Viewers cannot vote or create proposals.
  * They have no influence on governance decisions.

## Citizen​

**Description:**  
Citizens are registered users who meet the system requirements as verified by the `can_register` function (e.g., CHR token balance). Upon registration, a governance account and an FT4 account are created automatically.

**Rights:**

  * Create and submit proposals.
  * Vote on proposals if eligible.
  * Participate in governance discussions.

**Restrictions:**

  * Citizens must adhere to cooldown periods between proposal submissions.
  * They must meet registration criteria.
  * Citizens cannot approve or veto proposals unless they are assigned the roles of counselor or admin.

## Counselor (moderator)​

**Description:**  
Counselors are citizens promoted by an admin or via a proposal command (e.g., `make_counselor`). They serve as moderators and validators.

**Rights:**

  * All the rights of citizens, plus:
    * Approve proposals during validation.
    * Veto proposals within the designated veto period.
    * Participate in higher-level governance decisions.

**Restrictions:**

  * Counselors must adhere to a cooldown period for veto actions.
  * They cannot override system configurations or execute commands without admin approval.

## Admin​

**Description:**  
Admins are the highest authority within the governance system, usually comprising developers or owners of the governance dapp.

**Rights:**

  * Full control over governance settings, including:
    * Configuring voting rules and cooldown periods.
    * Managing proposal creation and validation parameters.
    * Approving or vetoing proposals directly.
    * Forcing the conclusion of voting or veto periods.
    * Executing commands from approved proposals.
    * Assigning or revoking counselor roles.

**Restrictions:**

  * While there are no technical limitations, actions should conform to governance principles to maintain trust.

# Adding a new user type

User types can be added using the `create_user_type` function:
    
    
    citizens.create_citizen_type("PROVIDER");  
    

# Updating an existing user type

As an admin, you can also update an existing user type by calling the `update_citizen_type` operation, or you can override the `can_update_citizen_type` function to change the access control of this operation.
    
    
    operation update_citizen_type(account_id: byte_array, new_type: text) {  
    
    
        val signer = citizens.ft4_auth.authenticate();  
    
    
        val citizen_type = citizens.citizen_types @ { .name == new_type };  
    
    
        citizens.can_update_citizen_type(signer, account_id, citizen_type);  
    
    
        citizens.update_citizen_type(account_id, citizen_type);  
    
    
    }

### Vote power strategies

SOURCE: https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/vote-power-strategies

Vote power strategies are used to calculate the voting power of a user. You can add any strategy you want to the Governance Tool. Once you add a new strategy, you can select it for a proposal.

## How to add a new vote power strategy​

To add a new vote power strategy, you need to extend the `votes.get_available_vote_power_calculation_strategies` function.

Here’s an example of how to add a new vote power strategy:
    
    
    function derive_max_vote_power_from_native_stake_balance(citizen: citizens.citizen, proposals.proposal?): big_integer {  
    
    
        val native_stake_balance = icmf.get_native_stake_balance(citizen.account.id);  
    
    
        return big_integer(native_stake_balance);  
    
    
    }  
    
    
      
    
    
    @extend(votes.get_available_vote_power_calculation_strategies)  
    
    
    function icmf_get_available_vote_power_calculation_strategies(): map<text, votes.my_vote_power_calculation_strategy> {  
    
    
        val strategies = map<text, votes.my_vote_power_calculation_strategy>();  
    
    
        strategies.put(  
    
    
            "TOTAL_CHR_STAKE",//Name of the strategy, which needs to be unique  
    
    
            votes.my_vote_power_calculation_strategy(  
    
    
                name = "TOTAL_CHR_STAKE",//Unique name of the strategy  
    
    
                description = "Derives voting power based on the user's total balance of CHR stake across all EVM and Chromia chains.",  
    
    
                derive_max_voting_power = derive_max_vote_power_from_native_stake_balance(*),  
    
    
                base_vote_power = 10L.pow(6)  
    
    
            )  
    
    
        );  
    
    
        return strategies;  
    
    
    }  
    

The idea is to use extendable functions to return a map of vote power strategies. The results of every extended function will be merged together into one map at execution time. For more information, check how [extendable functions](</rell/language-features/modules/function#extendable-functions>) work in Rell.
    
    
    struct my_vote_power_calculation_strategy {  
    
    
        name: text;  
    
    
        description: text;  
    
    
        derive_max_voting_power: (citizens.citizen, proposals.proposal?) -> big_integer;  
    
    
        base_vote_power: big_integer; // This is the voting power added to the total vote power of the option, serving as the base vote power for users who do not utilize any vote power on a vote.  
    
    
    }  
    

For each vote power strategy, you need to define the following properties:

### Name​

A unique identifier for the strategy, which will be displayed in the UI. This helps users distinguish between different voting strategies. **Warning:** If you use the same name for different strategies, you will encounter an error whenever you attempt to use or retrieve vote power strategies.

### Description​

A detailed explanation of how the strategy works and its functionality. This helps users grasp the purpose and behavior of the strategy while viewing it in the UI.

### Derive max voting power​

The core function that calculates a user's voting power. This function takes a citizen and an optional proposal as input and returns the calculated voting power as a big integer.

Example:
    
    
    function derive_max_vote_power_from_native_stake_balance(citizen: citizens.citizen, proposals.proposal?): big_integer {  
    
    
        val native_stake_balance = icmf.get_native_stake_balance(citizen.account.id);  
    
    
        return big_integer(native_stake_balance);  
    
    
    }  
    

### Base vote power​

The baseline voting power value used in calculations. For instance, if you're using a token with 6 decimal places for voting power, you would set this to 10^6. This ensures proper scaling of voting power values.

### Vote requirements

SOURCE: https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/vote-requirements

Vote requirements are criteria used to determine whether a user has sufficient voting power to participate in a proposal.

## How to add a new vote requirement​

To add a new vote requirement, you must extend the `votes.get_available_vote_requirements` function.

Here is an example of how to add a new vote requirement:
    
    
    struct native_chr_stake_vote_requirements_params {  
    
    
        required_balance: big_integer;  
    
    
    }  
    
    
      
    
    
    function validate_proposal_vote_requirement_parameters_for_native_chr_stake_requirement(params: byte_array) {  
    
    
        val native_chr_stake_vote_requirements_params = native_chr_stake_vote_requirements_params.from_bytes(params);  
    
    
        require(  
    
    
            native_chr_stake_vote_requirements_params  
    
    
                .required_balance > 0L,  
    
    
            "Required balance must be greater than 0"  
    
    
        );  
    
    
    }  
    
    
      
    
    
    function validate_vote_for_native_chr_stake_requirement(params: proposals.validate_vote_params): boolean {  
    
    
        val native_chr_stake_vote_requirements_params = native_chr_stake_vote_requirements_params  
    
    
            .from_bytes(  
    
    
                params  
    
    
                    .extendable_vote_requirements  
    
    
                    .params  
    
    
            );  
    
    
        return native_chr_stake_vote_requirements_params  
    
    
            .required_balance <= icmf.get_native_stake_balance(  
    
    
            params  
    
    
                .voter_account_id  
    
    
        );  
    
    
    }  
    
    
      
    
    
    function extract_native_chr_stake_vote_requirements_params(params: byte_array) {  
    
    
        return native_chr_stake_vote_requirements_params.from_bytes(params).to_gtv_pretty();  
    
    
    }  
    
    
      
    
    
    struct native_chr_stake_snapshot_balance_vote_requirements_params {  
    
    
        required_balance: big_integer;  
    
    
        snapshot_timestamp: timestamp;  
    
    
    }  
    
    
      
    
    
    function validate_vote_for_native_chr_stake_snapshot_requirement(params: proposals.validate_vote_params): boolean {  
    
    
        val native_chr_stake_snapshot_balance_vote_requirements_params = native_chr_stake_snapshot_balance_vote_requirements_params  
    
    
            .from_bytes(  
    
    
                params  
    
    
                    .extendable_vote_requirements  
    
    
                    .params  
    
    
            );  
    
    
        return native_chr_stake_snapshot_balance_vote_requirements_params  
    
    
            .required_balance <= icmf  
    
    
            .get_native_stake_balance_at_timestamp(  
    
    
                params  
    
    
                    .voter_account_id,  
    
    
                native_chr_stake_snapshot_balance_vote_requirements_params  
    
    
                    .snapshot_timestamp  
    
    
            );  
    
    
    }  
    
    
      
    
    
    function validate_proposal_vote_requirement_parameters_for_native_chr_stake_snapshot_requirement(params: byte_array) {  
    
    
        val native_chr_stake_snapshot_balance_vote_requirements_params = native_chr_stake_snapshot_balance_vote_requirements_params  
    
    
            .from_bytes(  
    
    
                params  
    
    
            );  
    
    
        require(  
    
    
            native_chr_stake_snapshot_balance_vote_requirements_params  
    
    
                .required_balance > 0L,  
    
    
            "Required balance must be greater than 0"  
    
    
        );  
    
    
        require(  
    
    
            native_chr_stake_snapshot_balance_vote_requirements_params  
    
    
                .snapshot_timestamp > 0L,  
    
    
            "Snapshot timestamp must be greater than 0"  
    
    
        );  
    
    
    }  
    
    
      
    
    
    function extract_native_chr_stake_snapshot_balance_vote_requirements_params(params: byte_array) {  
    
    
        return native_chr_stake_snapshot_balance_vote_requirements_params.from_bytes(params).to_gtv_pretty();  
    
    
    }  
    
    
      
    
    
    @extend(proposals.get_available_vote_requirements)  
    
    
    function icmf_get_available_vote_requirements(): map<text, proposals.my_vote_requirements> {  
    
    
        val vote_requirements = map<text, proposals.my_vote_requirements>();  
    
    
        vote_requirements.put(  
    
    
            "NATIVE_CHR_STAKE",  
    
    
            proposals.my_vote_requirements(  
    
    
                validate_vote = validate_vote_for_native_chr_stake_requirement(*),  
    
    
                validate_proposal_vote_requirement_parameters = validate_proposal_vote_requirement_parameters_for_native_chr_stake_requirement(  
    
    
                    *  
    
    
                ),  
    
    
                extract_params = extract_native_chr_stake_vote_requirements_params(*),  
    
    
                params = [  
    
    
                    proposals.input_field(  
    
    
                    name = "required_balance",  
    
    
                    type = "big_integer",  
    
    
                    label = "Required Native CHR Stake Balance",  
    
    
                    required = true  
    
    
                )  
    
    
                ]  
    
    
            )  
    
    
        );  
    
    
        vote_requirements.put(  
    
    
            "NATIVE_CHR_STAKE_SNAPSHOT",  
    
    
            proposals.my_vote_requirements(  
    
    
                validate_vote = validate_vote_for_native_chr_stake_snapshot_requirement(*),  
    
    
                validate_proposal_vote_requirement_parameters = validate_proposal_vote_requirement_parameters_for_native_chr_stake_snapshot_requirement(  
    
    
                    *  
    
    
                ),  
    
    
                extract_params = extract_native_chr_stake_snapshot_balance_vote_requirements_params(*),  
    
    
                params = [  
    
    
                    proposals.input_field(  
    
    
                    name = "required_balance",  
    
    
                    type = "big_integer",  
    
    
                    label = "Required Native CHR Stake Balance at Snapshot",  
    
    
                    required = true  
    
    
                ),  
    
    
                    proposals.input_field(  
    
    
                    name = "snapshot_timestamp",  
    
    
                    type = "timestamp",  
    
    
                    label = "Snapshot Time",  
    
    
                    required = true  
    
    
                )  
    
    
                ]  
    
    
            )  
    
    
        );  
    
    
      
    
    
        return vote_requirements;  
    
    
    }  
    
    
      
    
    
      
    

The goal is to utilize extendable functions to return a map of vote requirements, similar to what is done with `vote_power_strategies`. Each result from the extended functions will be merged into a single map at execution time. You can refer to how [extendable functions](</rell/language-features/modules/function#extendable-functions>) operate in Rell.
    
    
    entity extendable_vote_requirements {  
    
    
        key proposal, name;  
    
    
        proposal;  
    
    
        name;  
    
    
        params: byte_array = x"";  
    
    
    }  
    
    
      
    
    
    struct extendable_vote_requirements_struct {  
    
    
        name;  
    
    
        params: byte_array;  
    
    
    }  
    
    
      
    
    
    struct validate_vote_params {  
    
    
        voter_account_id: byte_array;  
    
    
        extendable_vote_requirements;  
    
    
        locked_vote_power_amount: big_integer;  
    
    
    }  
    
    
      
    
    
    struct my_vote_requirements {  
    
    
        validate_vote: (validate_vote_params) -> boolean; // The first parameter is the voter, the second is the vote requirement parameters, and the third is the locked vote power amount  
    
    
        validate_proposal_vote_requirement_parameters: (byte_array) -> unit; // Validates the vote requirement parameters of a proposal  
    
    
        extract_params: (byte_array) -> gtv;  
    
    
        params: list<input_field>;  
    
    
    }  
    

The `extendable_vote_requirements` entity is used to store the vote requirements for proposals that are created. In the vote requirements struct, you need to define the following properties:

### validate_vote​

The `validate_vote` function checks whether a user has enough voting power to cast a vote on a proposal. This function is called during the `vote_on_proposal` operation, and it must verify that the user's vote is eligible to be counted.

### validate_proposal_vote_requirement_parameters​

The `validate_proposal_vote_requirement_parameters` function validates the vote requirement parameters associated with a proposal.

### extract_params​

The `extract_params` function extracts the vote requirement parameters based on a specific vote requirement type.

### params​

The `params` field is a list of input fields that will be displayed in the user interface (UI).

### Vote weight strategies

SOURCE: https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/vote-weight-strategy

Vote weight strategies are methods used to determine the weight of a vote. The LINEAR calculation is the standard vote weight strategy, meaning that when calculating the winner of a proposal, the vote power is considered as is.

The QUADRATIC calculation is a vote weight strategy that computes vote power as the square of the vote amount. This means that when determining the winning option of a proposal, the total vote power is calculated by squaring the amount of vote power.

### Example:​

  * Person A votes for option 1 with 100 vote power.
  * Person B votes for option 2 with 36 vote power.
  * Person C votes for option 3 with 36 vote power.

Calculating total votes:

  * For Option 1: (100^2 = 10,000)
  * For Option 2: Person B's vote ( (36^2 = 1,296) + ) Person C's vote ( (36^2 = 1,296) = 2,592 )

In this case, Option 1 wins with the LINEAR vote weight strategy.

## How to add a new vote weight strategy​

To add a new vote weight strategy, extend the `votes.get_available_vote_weight_strategies` function. The structure of the vote weight strategy is as follows:
    
    
    struct my_proposal_vote_weight_strategy {  
    
    
        name: text;  
    
    
        description: text;  
    
    
        calculate_winner_option: (proposals.proposal) -> proposals.option_item;  
    
    
        calculate_total_vote: (proposals.option_item) -> big_integer;  
    
    
        display_total_vote: (proposals.option_item) -> decimal;  
    
    
    }  
    

### Example of adding a new vote weight strategy​
    
    
    @extendable  
    
    
    function get_available_proposal_vote_weight_strategies(): map<text, votes.my_proposal_vote_weight_strategy>;  
    
    
      
    
    
    function linear_calculate_winner_option(proposals.proposal) {  
    
    
        return proposals.option_item @ {  
    
    
            .proposal == proposal  
    
    
        } (  
    
    
                $,  
    
    
                @omit @sort_desc .total_vote  
    
    
            ) limit 1;  
    
    
    }  
    
    
      
    
    
    function linear_calculate_total_vote(proposals.option_item): big_integer {  
    
    
        return option_item.total_vote;  
    
    
    }  
    
    
      
    
    
    function linear_display_total_vote(proposals.option_item): decimal {  
    
    
        val vote_power_base_vote = get_available_vote_power_calculation_strategies()  
    
    
            .get(  
    
    
                option_item.proposal  
    
    
                    .vote_power_calculation_strategy  
    
    
            ).base_vote_power;  
    
    
        return option_item.total_vote.to_decimal() / vote_power_base_vote;  
    
    
    }  
    
    
      
    
    
    @extend(votes.get_available_proposal_vote_weight_strategies)  
    
    
    function example_get_available_proposal_vote_weight_strategies() {  
    
    
        val strategies = map<text, votes.my_proposal_vote_weight_strategy>();  
    
    
        strategies.put(  
    
    
            "LINEAR",  
    
    
            votes.my_proposal_vote_weight_strategy(  
    
    
                name = "LINEAR",  
    
    
                description = "Linear vote weight strategy",  
    
    
                calculate_winner_option = linear_calculate_winner_option(  
    
    
                    *  
    
    
                ),  
    
    
                calculate_total_vote = linear_calculate_total_vote(  
    
    
                    *  
    
    
                ),  
    
    
                display_total_vote = linear_display_total_vote(*)  
    
    
            )  
    
    
        );  
    
    
        return strategies;  
    
    
    }  
    

### Structure explanation​

  * **Name:** The name of the vote weight strategy.

  * **Description:** A brief description to be displayed in the user interface (UI).

  * **calculate_winner_option:** This function computes and returns the winning `option_item` record.

  * **calculate_total_vote:** This function computes and returns the total votes of an `option_item`. It is used to display total votes for different options in the UI.

  * **display_total_vote:** This function formats the total vote number for display in the UI.

### Rell language

SOURCE: https://docs.chromia.com/ecosystem/governance/getting-started/rell-language/

[Rell](</rell/rell-intro>) is a programming language designed explicitly for dapp development on the Chromia blockchain. Unlike traditional platforms that rely on virtual machines, Rell takes a language-focused approach to better align with Chromia’s relational data model enables efficient encoding of queries and operations.

With Rell, developers can define data models, write queries, and implement procedural logic. The language compiles into SQL while ensuring secure and reliable execution.

Key features include strong type safety to prevent mismatches and errors, built-in security measures like overflow protection and mandatory authorization checks, a concise syntax that reduces redundancy and enhances readability, and meta-programming capabilities that allow reusable templates for more efficient development.

## Getting started​

To begin using Rell templates:

  1. See the [Rell Introduction](</rell/rell-intro>) for a basic understanding
  2. Explore available extensions and features on the [Chromia Learn platform](<https://learn.chromia.com/>).
  3. Join the [Chromia developer community](<https://chromia.com/developers>) for support

## Start building​

This [section](</get-started/installation>) provides all the essentials to begin dapp development on the Chromia blockchain. It includes instructions for installing and configuring your local development environment. You will also find an An extensive guide that will teach you how to install and use the Chromia CLI to write and execute pipelines for testing and deploying Rell code on the Chromia blockchain platform. You should follow these simple steps to start building your dapp: [set up your environment](</get-started/installation>), create a basic ["Hello World" dapp](</get-started/create-dapp/>), acquire [Testnet tokens](</build/deployment/testnet-tokens/>), and [deploy](</get-started/create-dapp/deploy-to-testnet>) your dapp to the public Testnet.

warning

To test the Governance Tool for EVM, you should obtain [test tokens (tCHR)](</build/deployment/testnet-tokens/get-tchr-binance>) on the Binance Smart Chain Testnet.

## Configure your project and blockchain​

Below, we summarize how to configure a project and blockchain to develop and run a dapp on Chromia. You need to set up your project and configure the blockchain to seamlessly connect the frontend with the Rell backend for efficient data management. For detailed information on how to configure your project and blockchain, please refer to the [configuration section](</build/configuration/>).

### Project Structure​

A Chromia project is organized within a directory containing Rell code, a `chromia.yml` configuration file, and modules for dapp logic. Running `chr create-rell-dapp` generates a new project with a `main.rell` entry and test files.

### Project Settings File​

The `chromia.yml` file defines the dapp’s configuration, including blockchain settings and deployment details. It can be renamed, and most attributes have default values requiring changes only if customization is needed.

### Configuration Properties​

Key settings in `BaseBlockChainConfiguration` include **signers, sync, and configuration factory** , while block strategy settings define parameters like **max block size and minimum inter-block interval**. Default values and types are provided for guidance.

### Economy Chain configuration​

Defines parameters related to **assets, staking, and EVM integration** , covering asset identification, staking rewards, and operational limits such as **maximum minting amounts, staking intervals, and transaction bonuses**.

tip

Please check the Chromia block explorer for the [Economy Chain](<https://explorer.chromia.com/mainnet/blockchain/15C0CA99BEE60A3B23829968771C50E491BD00D2E3AE448580CD48A8D71E7BBA>) and find detailed information about the [Economy Chain configuration](</ecosystem/providers/nodes/economy-chain-config>).

## Advanced components of Governance Tool​

When using the Governance Tool, you will interact with various other components developed within the Chromia ecosystem. Among the most important are the [Ethereum Interoperability Framework (EIF)](<https://gitlab.com/chromaway/core/postchain-eif/-/tree/dev/doc>), [FT4 accounts](</build/ft4/account-management/>), and the rell [pagination library](<https://bitbucket.org/chromawallet/lib-pagination-utils/src/master/>) that will help you paginate your data. Gaining a solid understanding of these concepts before diving deep into building will be very beneficial.

tip

For user accounts and their management within the Governance Tool, including registration and authentication, we’ll guide you through installing the [FT4 library](</build/ft4/intro>) and configuring accounts. For EIF, the Governance Tool primarily uses `eif`, `eif_event_receiver`, and `eif_event_connector`, which are located in the [GitLab repository](<https://gitlab.com/chromaway/core/postchain-eif/-/tree/dev/postchain-eif-rell/rell/src>).

### Work with proposals

SOURCE: https://docs.chromia.com/ecosystem/governance/governance-proposals/

This section covers how to create and manage proposals within the Governance Tool.

[Learn how to create a proposal to suggest changes or decisions within the Governance Tool. This includes logging in, drafting your proposal, submitting it for verification, and preparing it for voting.](</ecosystem/governance/governance-proposals/create-proposal>)

### Create a proposal

SOURCE: https://docs.chromia.com/ecosystem/governance/governance-proposals/create-proposal

Creating a proposal is the first step in a governance dapp, which allows citizens to suggest changes or decisions for the network. This process ensures that ideas are presented and ready for community input. Follow the steps below to create a proposal:

## Prerequisites​

Before you begin, ensure you have the following:

  * **Citizen registration** — Ensure you are registered as a citizen within the Governance Tool system via an external authentication mechanism like MetaMask.
  * **MetaMask or compatible wallet** — Install and configure MetaMask or use another supported Ethereum-compatible wallet.
  * **Sufficient privileges** — Ensure your citizen account has the required permissions to create proposals. Contact an administrator if needed.
  * **Active citizen status** — Ensure you are not under any cooldown period from recent actions.
  * **Familiarity with proposal guidelines** — Review the proposal parameters (e.g., duration, voting requirements, categories) to ensure compliance when drafting your proposal.

## **Step 1: Log in as a citizen**​

Ensure you are registered as a citizen and logged in using your authenticated wallet (e.g., MetaMask). Only registered citizens with the required privileges can create proposals.

## **Step 2: Start the draft creation process**​

  1. Navigate to the **Proposals** section.
  2. Click **Create a proposal** to begin drafting your proposal.

You will be prompted to fill in the following details:

  * **Title** — Provide a concise and descriptive name for your proposal.
  * **Category** — Select the category that best fits the type of decision your proposal addresses.
  * **Vote options** — Enter the available options for the community to vote on. You might specify "Approve," "Reject," or any other alternatives depending on the proposal's nature. These options will guide the community's decision-making process.
  * **Duration** — Specify how long the voting period should last.

Ensure all required fields are filled out accurately before proceeding.

## **Step 3: Send to verification**​

  1. Click **Preview** to see your proposal.

  2. Preview the details and click **Send to verification** to submit your proposal for verification.

  3. The system will automatically check the following conditions:

     * **Eligibility:** Only citizens with appropriate privileges can create a draft.

     * **Cooldown period:** If you have recently performed specific actions (e.g., submitted another proposal), you may be subject to a cooldown period before creating a new draft.

If you meet all requirements, your draft will be submitted successfully.

If not, the system will display an error message indicating the issue.

## **Step 4: Wait for verification**​

After submission, your draft enters the **verification phase** , where an administrator reviews it. During this phase:

  * Track the status of your draft under the **My proposals** section.

  * The draft may be approved, rejected, or sent back for modifications.

Ensure your draft complies with governance guidelines to avoid delays in verification.

## **Step 5: Submit the proposal for voting**​

After the administrator has verified and approved your proposal, you can either submit it immediately for voting or schedule it for a later time.

### **Option 1: Submit for immediate voting**​

  1. Once the proposal is approved, you can submit it immediately for voting.

  2. Navigate to the **My proposals** section, where your proposal will appear with an **Approved** status.

  3. Click on the proposal, review it, and click **Publish**.

  4. If everything is correct, click **Publish instantly** to make the proposal available for citizens to vote on.

  5. The proposal will now be open for voting, and citizens can choose from the options provided.

### **Option 2: Schedule for future voting**​

  1. Once the proposal has been approved, navigate to the **My proposals** section if you prefer to schedule it for voting at a later time.

  2. Navigate to the scheduling section and choose a suitable voting period start date and time.

  3. Click **Schedule a publication** to finalize your selection.

  4. Your proposal will be scheduled, and the voting period will begin automatically at the chosen time.

### Voting process

SOURCE: https://docs.chromia.com/ecosystem/governance/governance-voting-process/

This section explains how voting works in the Governance Tool and how you can participate in the decision-making process. It covers different voting methods, the voting flow, and provides step-by-step instructions for casting votes on proposals.

warning

Please note that these methods are not specifically defined in the Governance Library. They serve as examples to help you get started. The Governance Library offers full flexibility to customize the voting process to meet your specific needs.

[Explore the various voting mechanisms used in the Governance Tool, including majority voting, quorum-based voting, lazy voting, unanimous voting, and more. Learn how each method works, when to use them, and their respective advantages and limitations.](</ecosystem/governance/governance-voting-process/voting-types>) [Understand the complete voting process, from calculating voting power to determining results. Learn about different voting strategies, dynamic voting, and how voting power evolves throughout the process.](</ecosystem/governance/governance-voting-process/voting-flow>)[Discover how to participate in governance by casting your vote on proposals. This guide will walk you through accessing the proposals section, reviewing the details, selecting your vote, and confirming your submission.](</ecosystem/governance/governance-voting-process/vote-proposal>)

### Vote on a proposal

SOURCE: https://docs.chromia.com/ecosystem/governance/governance-voting-process/vote-proposal

Voting is an essential part of the **Governance Tool** , enabling citizens to participate in decision-making. Follow the steps below to cast your vote on a proposal.

## Prerequisites​

Before you vote, ensure you meet the following requirements:

  * **Citizen registration** : You must be registered as a citizen through an external authentication method, such as MetaMask.
  * **Connected wallet** : Use MetaMask or another Ethereum-compatible wallet linked to your account.
  * **Active voting period** : The proposal must be open for voting.
  * **Eligibility** : Your citizen account must meet the required criteria (e.g., it should not be under cooldown).

## Step 1: Navigate to the proposals section​

  1. Log in to the **Governance Tool** using MetaMask or your chosen wallet.
  2. Open the **Proposals** section to view a list of active proposals.

## Step 2: Select a proposal​

Scroll through the list and choose a proposal to vote on. Each proposal displays its status and available voting options.

## Step 3: Review the proposal​

Click on the proposal to view its details, including the description, voting options, and deadline. Carefully review the information before proceeding.

If prompted, confirm the **citizen registration request** in your wallet.

## Step 4: Choose your vote​

Select your preferred voting option (e.g., **Approve** , **Reject** , or other choices).

## Step 5: Cast your vote​

Click **Vote** to submit your decision. Your vote will be recorded on the blockchain.

## Step 6: Confirm submission​

A confirmation message will appear once your vote is successfully registered.

## Step 7: Wait for the voting period to end​

Once the voting period concludes, votes will be counted, and the proposal will be finalized based on the majority decision.

## Voting outcomes​

After the voting period ends:

  * **If approved** , the proposal will move forward for execution.
  * **If rejected** , the proposal will be discarded.

Stay engaged by tracking proposal results in the **Governance Dashboard**.

### Voting flow

SOURCE: https://docs.chromia.com/ecosystem/governance/governance-voting-process/voting-flow

## Voting requirements and strategies for voting power​

Voting power determines the weight of a user's vote based on factors such as stake, token balance, and other criteria. This mechanism ensures that governance decisions proportionately reflect the contributions or commitments of stakeholders.

### Voting power calculation​

There are three primary approaches to calculating voting power in decentralized autonomous organizations (DAOs):

#### Snapshot-based approach​

  * **Definition:**  
Voting power is determined by token balances or stakes at a specific moment (a snapshot), typically taken when a proposal is created or when voting begins.

  * **Key features:**

    * **Static calculation:** Once the snapshot is taken, voting power remains fixed, even if tokens are transferred or unstaked afterward.
  * **Advantages:**

    * Prevents manipulation of voting power during the voting period.
    * Simplifies calculations and verification processes.
  * **Example:**  
If a user holds 1,000 CHR at the time of the snapshot, their voting power remains 1,000 throughout the voting process, regardless of any subsequent token transfers.

#### Dynamic approach​

  * **Definition:**  
Voting power is recalculated in real-time based on current token balances or stakes during the voting period.

  * **Key features:**

    * **Adaptive calculation:** Voting power immediately reflects any changes in balance or stake.
  * **Advantages:**

    * Accurately reflects a user’s ongoing commitment to the system.
    * Discourages transferring tokens or unstaking during the voting period.
  * **Disadvantages:**

    * Increases implementation complexity and the need for continuous verification.
  * **Example:**  
If a user starts with 1,000 CHR voting power and then transfers tokens before the vote concludes, their voting power immediately decreases to 500.

#### Mixed strategy​

  * **Definition:**  
This is a hybrid model that combines elements of both snapshot and dynamic approaches. Different rules may apply based on the proposal type or specific governance settings.

  * **Key features:**

    * Provides flexibility to tailor governance rules for various scenarios.
  * **Example:**  
Utilize snapshot-based voting for general proposals and switch to dynamic voting for time-sensitive or high-impact decisions.

### Voting results calculation​

After the voting period concludes, the system calculates the results using the selected voting power strategy and proposal-specific rules. The process involves the following steps:

  1. **Aggregation:**  
Votes are tallied based on each user’s weighted voting power.

  2. **Voting power formulas:**

     * **Linear:** Voting power is directly proportional to the number of tokens held or staked.
     * **Quadratic:** Voting power scales nonlinearly, helping balance the influence between large and small holders.
     * **Mixed:** A combination of linear and quadratic formulas, customized through governance configurations.
  3. **Threshold check:**  
Proposals must meet predefined conditions (e.g., majority approval, quorum requirements) to pass.

  4. **Result declaration:**

     * The option with the highest weighted vote count wins.
     * In the event of a tie, fallback mechanisms such as administrative intervention or a veto by designated counselors may be triggered.

### Voting power flow in the Governance Tool​

In the Governance Tool, voting power is treated as a "spendable resource" allocated to proposals. The allocation method depends on the proposal type and the voting power strategy (e.g., snapshot vs. dynamic, balance vs. staked tokens, linear vs. quadratic). Once voting concludes, the voting power is either restored to the user or adjusted for future use.

#### Voting power lifecycle​

  1. **Initial calculation:**  
A user’s total voting power is determined by:

     * **Token balance:** e.g., CHR or other supported tokens.
     * **Staked tokens:** Tokens that have been committed to the governance process.
     * **Additional criteria:** Future features might include factors such as NFT ownership or reputation.
  2. **Voting participation:**  
When casting a vote, a user allocates a portion of their voting power:

     * **Snapshot-based voting:** The allocated power is locked at the snapshot value and remains unchanged until the voting period ends.
     * **Dynamic voting:** The vote may be removed if token balances or stakes decrease during the voting period.

### Example scenario​

#### User's initial state​

  * **CHR in wallet:** 300 tokens
  * **CHR staked:** 1,200 tokens

#### Voting power allocation​

  * **Proposal A (snapshot-based, linear voting):**

    * **Total voting power:** 300 (wallet) + 1,200 (staked) = **1,500**
    * **Vote example:** The user votes with 1,500 voting power. They always vote with their full voting power. If their voting power decreases during the voting period, their vote will be removed.
  * **Proposal B (dynamic, quadratic voting):**

    * **Voting power calculation:** A quadratic formula is applied, e.g., voting power = sqrt(1,200).
    * **Vote example:** The user’s voting power is dynamically adjusted based on their current stake. If tokens are transferred or unstaked before the vote concludes, the vote will be removed.

### Voting types

SOURCE: https://docs.chromia.com/ecosystem/governance/governance-voting-process/voting-types

The Governance Tool operates on a simple majority voting system. This approach counts only the percentage of "yes" votes and does not require a quorum. You have the flexibility to customize the tool for various voting strategies.

## General types of voting in DAOs​

### Majority voting​

A proposal passes if it receives more than a specified percentage of "yes" votes.

  * **Use case** : Commonly used in democratic governance.
  * **Pros** : Simple and widely understood.
  * **Cons** : May allow proposals to pass with low participation if there is no quorum required.

### Quorum-based voting​

A proposal is only valid if it reaches a minimum number of votes (quorum).

  * **Use case** : Ensures that decisions have sufficient participation.
  * **Pros** : Prevents a small group from making significant decisions.
  * **Cons** : Can delay progress if voter turnout is low.

### Supermajority voting​

This method requires a higher threshold (e.g., ⅔ or ¾ approval) for a proposal to pass.

  * **Use case** : Used for structural changes like amendments to governance rules.
  * **Pros** : Ensures a strong consensus.
  * **Cons** : Harder to pass, which may slow down decision-making.

### Lazy voting​

Proposals pass by default unless a specific number of participants vote against them.

  * **Use case** : Reduces voter fatigue in systems with frequent proposals.
  * **Pros** : Increases efficiency and speeds up decisions.
  * **Cons** : Risk of unintended approvals if users remain inactive.

### Unanimous voting​

All eligible voters must vote "yes" for a proposal to pass.

  * **Use case** : Used for high-stakes decisions (e.g., constitutional changes).
  * **Pros** : Guarantees full consensus.
  * **Cons** : A single dissenting vote can block the proposal, potentially causing deadlock.

### Plurality voting​

The option with the most votes wins, even if it does not receive a majority of the votes.

  * **Use case** : Suitable for multi-option proposals.
  * **Pros** : Allows for multiple choices in governance.
  * **Cons** : A winner may emerge with less than **50 percent support** , leading to fragmented results.

### Ranked-choice voting​

Voters rank proposals by preference. If no option wins outright, votes are redistributed.

  * **Use case** : Used in elections and proposals with multiple competing options.
  * **Pros** : Reduces vote splitting and promotes compromise.
  * **Cons** : More complex to implement and count.

### Weighted voting​

Voting power is determined by stake, token balance, or other predefined criteria.

  * **Use case** : The Governance Tool can configure voting power based on **CHR holdings, stake, or hybrid models**.
  * **Pros** : Reflects varying levels of commitment and investment.
  * **Cons** : There is a risk of centralization if power is concentrated among a few users.

### Veto voting​

Certain users (e.g., admins or counselors) can block a proposal even if it receives majority support.

  * **Use case** : The Governance Tool allows **admins or counselors to veto during a designated period.
  * **Pros** : Provides an additional layer of review and oversight.
  * **Cons** : Can limit decentralized decision-making.

### Approval voting​

Voters can approve multiple proposals, and the option with the most approvals wins.

  * **Use case** : Suitable for scenarios where multiple proposals can coexist.
  * **Pros** : Encourages broad representation of preferences.
  * **Cons** : May dilute the vote impact if users approve too many proposals.

### Overview

SOURCE: https://docs.chromia.com/ecosystem/governance/overview

The Governance Tool is an advanced, on-chain voting solution built on the [Chromia blockchain](<https://chromia.com/>). It integrates seamlessly with Ethereum-compatible ecosystems by utilizing EVM tokens as voting power. With Chromia's innovative bridging technology, this tool provides a secure and efficient connection between Chromia and Ethereum networks. It features a gas-free, fully automated, and highly customizable framework, catering to the diverse needs of modern decentralized applications while supporting both staked tokens and tokens held in wallets.

## Key features​

The Governance Tool is designed to facilitate flexible and robust decentralized decision-making. Its core features include:

### Effortless integration​

Integrate governance into your decentralized application with minimal effort. A streamlined onboarding process ensures that you can quickly deploy a secure and reliable governance system.

### Modular and configurable architecture​

Every component of the Governance Tool can be tailored to meet the unique needs of your community or project. Its modular design allows developers and administrators to easily modify elements, ensuring that the framework evolves alongside your project.

### Cost-effective operation​

Take advantage of a solution that eliminates additional gas fees while providing transparent and fair hosting costs. This cost efficiency allows you to maintain robust governance without compromising performance.

### Customizable voting mechanics​

Adjust voting power and criteria to fit your project’s specific requirements. This adaptability enables the creation of voting systems that genuinely reflect your community's values and decision-making processes.

### Automated execution​

Utilize auto-executable commands through extendable functions that simplify complex governance tasks. [Learn more about extendable functions](</rell/language-features/modules/function#extendable-functions>).

## Governance process​

The Governance Tool empowers communities to actively participate in network decisions with transparency and inclusivity. It defines clear roles for citizens, administrators, and councilors, creating a balanced and effective decision-making process. Key stages include:

  * **Citizen registration:** Secure onboarding for community members, ensuring legitimacy and transparency.
  * **Proposal creation and management:** Create and manage proposals with essential details (e.g., title, category, duration) that are reviewed by councilors or administrators.
  * **Voting:** Cast votes during a designated phase to ensure every community member's voice is heard.
  * **Veto process:** Exercise veto rights to block proposals that may be harmful or controversial.

After review, approved proposals move into the voting phase. Depending on the outcome, proposals are either implemented or archived, ensuring decisions are made in the best interest of the network.

## Governance for Chromia dapp developers​

For developers building on Chromia, this Governance Tool provides a comprehensive and adaptable framework to incorporate governance features into your dapps. Using this pre-built template accelerates the development process and allows you to focus on enhancing user experience, security, and feature development.

The flexible architecture supports everything from basic voting systems to more complex structures involving token staking and decentralized proposal creation. As your governance needs evolve, you can easily update or modify the template—refining voting rules, adding new features, or adjusting processes—without disrupting the overall system.

---
## 8. Published identifiers (from crawled docs; re-verify on Explorer)
- Mainnet Directory Chain BRID: 7E5BE539EF62E48DDA7035867E67734A70833A69D2F162C457282C319AA58AE4
- Testnet Directory Chain BRID: 6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92
- Economy/management chain BRID Mainnet: 15C0CA99BEE60A3B23829968771C50E491BD00D2E3AE448580CD48A8D71E7BBA
- System Anchoring Chain BRID: B497391373BB74944193205EB37C84B0520D474F491E2EF4743F16F670DB289B
- Genesis node pubkey: 037434C8D4F2B7B7DE44E80486A814676DC3D898FD4488E10E1940B1C4C5837200
- System API host: system.chromaway.com (sync 9870)
- Testnet node example: https://node0.testnet.chromia.com
- Directory validator BSC testnet: 0x83dB85F7ef4447524D3A31c0F4664a89173C68Eb
- Directory validator BSC and ETH mainnet: 0xc755927508b7Ac3f7B31c9Ed396F3bE91C723d00
- Vault: https://vault.chromia.com
- Explorer: https://explorer.chromia.com/mainnet/cluster/system and testnet sibling
- Filehub UI: https://filehub.chromia.com
- Faucet: https://faucet.testnet.chromia.com

## 9. Coverage inventory

- get-started/about: 31 sitemap URLs (architecture, chains, protocols GTV/GTX/FT4/ICMF/ICCF, staking, hosting, providers, wallets, Rell, extensions)
- ecosystem/providers: 48 sitemap URLs including all PMC command pages, nodes, container-management, rewards, nm_api; apis-1 is a stub
- ecosystem/bridge: 20 sitemap URLs (overview, lease, client, deploy, mass-exit, troubleshooting)
- ecosystem/filehub: 8 sitemap URLs
- ecosystem/extensions: 6 sitemap URLs (overview, AI inference, vector-db, stork, zkp)
- ecosystem/block-explorer: 4 sitemap URLs
- ecosystem/governance: 26 sitemap URLs (dapp governance starter kit, not network governance)

Total selected sitemap URLs: 143. HTTP 200: 143. Stub: apis-1.
Not in this crawl (outside requested trees): full FT4 library, Chromia CLI reference, Postchain REST beyond ICCF mention.

## 10. Internal inconsistencies to remember

1. Provider counts / Nakamoto coefficients differ across architecture-summary, chain-governance, and explorer Features examples.
2. Delegated stake asset types: Providers/About say native + ERC-20 + BEP-20; provider-staking page says native only; staking-summary documents EVM reward sunset 2 Dec 2025 to 1 Apr 2026.
3. PMC staking-share command is written as pmc update-provider-staking-reward-share on the rewards page and pmc economy update-provider-staking-reward-share on staking + economy command reference.
4. Replica page labels 15C0CA99 as both Management chain and Economy Chain.
5. One extension per container vs AI Inference page allowing a JAR so it can combine with other extensions.
6. Section index URLs under /ecosystem/providers|bridge|filehub|governance/ 404; always use child paths.

When implementing, prefer the more specific operational page over a summary card, and re-read Explorer / Vault for live BRIDs, validator sets, and provider counts.
