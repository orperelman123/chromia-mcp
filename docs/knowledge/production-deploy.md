# Chromia production deployment brief

Compiled 2026-08-26 from official Chromia docs only (`docs.chromia.com` plus official product URLs those pages name). Nothing here is inferred beyond what those sources state. Live HTTP checks on 2026-08-26 are noted inline.

---

## 1. Networks

Chromia has a public **Mainnet** and a public **Testnet**. A network has one **system cluster** (Directory Chain, Economy Chain, System Anchoring Chain, Token Chain, Transaction Submitter Chain, and related system chains) plus one or more **dapp clusters**.

Using the reserved deployment names `mainnet` or `testnet` in `chromia.yml` lets Chromia CLI fill Directory Chain BRID and system-node URL automatically. A custom deployment name requires `brid` and `url` to be set by hand.

### Directory Chain BRIDs (published)

From [Project settings file](https://docs.chromia.com/build/configuration/project-config):

| Network | Directory Chain BRID |
| --- | --- |
| Mainnet | `x"7E5BE539EF62E48DDA7035867E67734A70833A69D2F162C457282C319AA58AE4"` |
| Testnet | `x"6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92"` |

The same Testnet Directory Chain BRID appears in the Testnet deploy guide and in the Hello World Testnet status-check URL. The same Mainnet Directory Chain BRID appears in the PMC provider-key config example (`brid = 7E5BE539…` without the `x""` wrapper).

Docs tell you to **re-verify** both `brid` and `url` on Explorer:

1. Open Chromia Explorer and set Current network to Mainnet or Testnet.
2. Clusters → **system**.
3. Copy an **API URL** from the node list.
4. System Chains → **directory_chain** → copy `brid`.

### Official system-node URLs (published)

**Testnet** (deploy + client guides):

- `https://node0.testnet.chromia.com:7740`
- `https://node1.testnet.chromia.com:7740`
- `https://node2.testnet.chromia.com:7740`
- `https://node3.testnet.chromia.com:7740`

The Hello World Testnet guide also uses the REST path
`https://node0.testnet.chromia.com/tx/{directory_chain_brid}/{tx_rid}/status`
(that example omits `:7740`; pattern given as `{node}/tx/{directory_chain_brid}/{tx_rid}/status`).

**Mainnet** client guide (system-cluster Directory Chain nodes; “for the most recent and complete list, visit the Chromia Explorer and view the list of nodes in the system cluster”):

- `https://system.chromaway.com`
- `https://chromia.validatrium.club`
- `https://chromia-mainnet-systemnode-1.stakin-nodes.com`
- `https://chroma.node.monster:7741`
- `https://dapps0.chromaway.com`
- `https://chromia-mainnet.w3coins.io:7740`
- `https://mainnet-dapp1.sunube.net:7740`

The project-config examples also show `https://system.chromaway.com` as a Mainnet `url`. PMC provider setup uses `api.url=https://system.chromaway.com`.

Clients should pass a **pool of system-node URLs** plus the **dapp Blockchain RID**. `postchain-client` queries the Directory Chain on those nodes to discover the nodes that currently run the dapp.

### Product URLs (checked 2026-08-26)

| URL | HTTP | Notes |
| --- | --- | --- |
| https://explorer.chromia.com/ | 200 → `/mainnet` | Official explorer |
| https://explorer.chromia.com/mainnet/cluster/system | 200 | System cluster |
| https://explorer.chromia.com/testnet/cluster/system | 200 | System cluster |
| https://chromia.com/explorer | 200 | Marketing/explorer landing |
| https://vault.chromia.com/ | 200 | Mainnet Vault |
| https://vault.chromia.com/en/deposit | 200 → `/deposit/` | CHR deposit (docs path) |
| https://vault.chromia.com/en/containers/ | 200 → `/containers/` | Container lease |
| https://vault.testnet.chromia.com/ | 200 | Testnet Vault |
| https://vault.testnet.chromia.com/en/containers/ | 200 → `/containers/` | Testnet container lease |
| https://faucet.testnet.chromia.com/ | 200 | Testnet faucet (docs name it “Chromia Testnet Faucet” without a hardcoded host; this host is live) |
| https://faucet.chromia.com/ | **does not resolve** | Not a published docs URL; recorded because a naive host guess fails |
| https://filehub.chromia.com/ | 200 | Filehub UI (docs say “Filehub UI”; this host is live) |
| https://filehub.testnet.chromia.com/ | 200 | Testnet Filehub UI |
| https://filehub-gw.chromia.com/ and `/mainnet/` | **404** | Docs example Filehub gateway path is `https://filehub-gw.chromia.com/mainnet/<hash>`; the directory roots 404 |

Economy Chain RID is **not** published as a static hex in the developer guides. Official way to obtain it: `chr query --blockchain-rid ${DIRECTORY_CHAIN_RID} get_economy_chain_rid`.

---

## 2. How you deploy a blockchain

Two layers:

1. **Lease a container** on a dapp cluster (Economy Chain payment → Directory Chain allocation via ICMF).
2. **Deploy / update the Rell blockchain** into that container (`chr deployment create` / `update`, signed by the container’s deployer key).

PMC (`pmc`) is the provider/management CLI that submits transactions to Directory Chain and Economy Chain. Dapp developers typically lease via Vault and deploy via `chr`. Providers and multi-owner operators also use `pmc container`, `pmc lease`, `pmc blockchain`, and `pmc proposal`.

### 2.1 Prerequisites

- Chromia CLI (`chr`).
- A compiling dapp (`chromia.yml` + Rell).
- A key pair for the container (not the same key as the wallet that holds CHR / tCHR).
- Tokens:
  - **Testnet:** tCHR. Faucet allowance documented as **1000 tCHR every 7 days**. Also `pmc economy claim-test-chr` (Testnet only).
  - **Mainnet:** native CHR on the Economy Chain. Create an account by depositing **at least 10 CHR** from BNB Smart Chain or Ethereum Mainnet via Vault (`https://vault.chromia.com/en/deposit`), or by sending **at least 20 CHR** from another Chromia chain (10 CHR one-time account-creation fee, 10 CHR remaining).

### 2.2 Generate the container / deployment key

```bash
chr keygen --key-id="testnet_container_key"   # or a mainnet-specific id
```

Writes under `~/.chromia/`:

- `{key-id}` — private key
- `{key-id}.pubkey` — public key
- `{key-id}_mnemonic` — recovery phrase

Default key id if omitted: `chromia_key` (re-running `chr keygen` without `--key-id` can overwrite it).

Point CLI at that key via `~/.chromia/config`:

```text
key.id = testnet_container_key
```

Or pass `--key-id`, `--secret`, or `--config` on the command.

**Docs warning:** keep the private key secret; never share it.

### 2.3 Lease a container (developer path: Vault)

**Testnet** — [Get a container](https://docs.chromia.com/build/deployment/testnet/get-container):

1. Open https://vault.testnet.chromia.com/en/containers/ and connect a wallet.
2. Lease a container → pick cluster → paste the **public** key from `.chromia/<key-id>.pubkey`.
3. Adjust SCUs, extra storage, duration; optional auto-renewal.
4. Sign. Result is a **Container ID** (hex; example in docs: `15ddfcb25dcb43577ab311fe78aedab14fda25757c72a787420454728fb80304`).

**Mainnet** — [Get a container](https://docs.chromia.com/build/deployment/mainnet/get-container):

1. Deposit CHR to Economy Chain (see above).
2. Vault container-lease page → Lease → cluster → public key → SCUs / storage / duration / auto-renewal.
3. Pay in CHR. Failed lease → refund of the same amount.
4. Note the Container ID.

Anyone can pay a lease (community / emergency extension). If payment is overdue the container is **suspended for six months** and can be reactivated by payment; after that it is **permanently deleted**.

### 2.4 Lease a container (operator path: PMC)

`pmc lease` talks to Economy Chain:

| Command | Purpose |
| --- | --- |
| `pmc lease create-container` | Create: `--cluster-name`, `--scus`, `--duration` (weeks), optional `--extraStorage` (GiB), `--extraComputeRequests`, `--subnode-image-name`, `--subnode-jar-extension-names`, `--auto-renew`, `--account-id` / `--evm-address` |
| `pmc lease upgrade-container` | Change SCUs / duration / extra storage on an existing container |
| `pmc lease list` / `info` | List / inspect leases |
| `pmc lease list-pending-tickets` | Pending lease tickets |
| `pmc lease remove-container` | Remove container **and lease, no refund** |
| `pmc lease assign-subnode-image` / `add-subnode-jar-extensions` | Custom subnode image / JAR extensions |

Provider-side container proposals (Directory Chain) live under `pmc container add|info|limits|list|remove|pause|resume|configuration|subnode-image|subnode-jar-extension`. Adding a container binds a **voter set** that may deploy blockchains into it.

Economy Chain → Directory Chain ICMF topics that allocate resources: `G_create_cluster`, `G_create_container`, `G_upgrade_container`, `G_stop_container`, `G_restart_container`, `G_remove_container`.

### 2.5 First deploy (`chr deployment create`)

Configure `chromia.yml` (see §4). Then:

```bash
# Testnet (Hello World / getting-started form)
chr deployment create --network testnet --key-id testnet_container_key

# Explicit settings + blockchain name (full guides)
chr deployment create --settings chromia.yml --network testnet --blockchain hello
chr deployment create --settings chromia.yml --network mainnet --blockchain hello
```

Also supported: `--secret .secret`, `--config <path>`, `--no-compression`, `-y` (skip confirm).

What happens:

- CLI reads `deployments.<network>` (`container`, and optionally `brid`/`url`).
- Builds a snapshot under `build/` (timestamped; `compile:target` overrides).
- Blockchain RID is computed from **dapp configuration and code** and **must be unique**. If create fails, change the code (unique comment / whitespace is enough in the Hello World guide).
- On success, CLI prints a `chains:` snippet. **You must save that genesis Blockchain RID.** If you omit `chains` and run `create` again, CLI **overrides the previous deployment**.
- Testnet warning: the deployed dapp is reachable in **about five minutes**. Confirm on Explorer (network = Testnet).

Pending-tx check (Testnet Hello World):

```bash
curl -X GET "https://node0.testnet.chromia.com/tx/6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92/{tx_rid}/status"
```

### 2.6 Lifecycle after first deploy

| Command | Effect |
| --- | --- |
| `chr deployment update` | New config on the **same** Blockchain RID (requires `chains:`) |
| `chr deployment info` | Deployed-chain info (`--verbose` for nodes; `--mainnet` / `--testnet`) |
| `chr deployment inspect` | Live queries / operations / entities / objects / module_args |
| `chr deployment pause` / `resume` | Pause / resume the blockchain |
| `chr deployment remove` | **Permanent** removal (`-y` confirm) |
| `chr deployment proposal *` | Vote / list / info / revoke / rename proposals |
| `chr deployment voterset *` | Voter-set info / update / list / `add-dapp-provider` |
| `chr deployment container pause\|resume\|configuration` | Container pause/resume; propose slow-DB-statement log threshold |

Update flags: `--height=<int>` (apply at a height), `--verify-only` (check config, do not send), `--skip-verification`.

### 2.7 Multi-owner production

[Multi-owner dapp deployment](https://docs.chromia.com/build/deployment/mainnet/multi-deployment):

1. Lease a container.
2. Add dapp providers: `chr deployment voterset add-dapp-provider`.
3. Add members: `chr deployment voterset update -vs <name> --add-member <pk1>,<pk2>` (docs also show `--add-members` in one example).
4. Threshold: `--threshold <int>` — `0` = supermajority `n - (n-1)/3` (~67%); `-1` = simple majority; positive = that many voters.
5. Updates become proposals. Vote with `chr deployment proposal vote --id <id> --accept` or `pmc proposal vote --accept --id <id>`. Docs also show `pmc blockchain update` as the update initiator for multi-owner.

Transfer lease ownership (Economy Chain ops, not `chr deployment`):

```bash
chr query --blockchain-rid ${DIRECTORY_CHAIN_RID} get_economy_chain_rid
chr tx --blockchain-rid ${ECONOMY_CHAIN_RID} --evm-auth ${CURRENT_OWNER_EVM_ADDRESS} \
  offer_container_lease_ownership_transfer ${CONTAINER_NAME} ${NEW_OWNER_ACCOUNT}
chr tx --blockchain-rid ${ECONOMY_CHAIN_RID} --evm-auth ${NEW_OWNER_EVM_ADDRESS} \
  accept_container_lease_ownership_transfer_offer ${CONTAINER_NAME}
# recover: remove_container_lease_ownership_transfer_offer
```

### 2.8 Connect a production client

Do **not** ship admin privkeys in frontend code. Use a pool of **system** node URLs + the dapp Blockchain RID:

```ts
const chromiaClient = await pcl.createClient({
  blockchainRID,
  directoryNodeUrlPool, // system-cluster URLs, not dapp-node URLs
});
```

The client queries Directory Chain and follows the current replica set.

---

## 3. Providers, clusters, containers, SCUs, CHR cost

### Roles

| Role | Permitted (docs) |
| --- | --- |
| Dapp Provider (DP) | Deploy dapps; add replica nodes (default) |
| Node Provider (NP) | Add block-builder nodes |
| System Provider (SP) | Govern system chains; add nodes to the system cluster; some ops (e.g. create a cluster) can be direct; large changes need a vote |

Significant changes go through **voter sets**. Promoting NP → SP needs a super-majority (≥ ⅔).

Each provider has:

- **Provider key pair** — sign Directory/Economy txs and proposals; identity on the network; “not possible to recover”; write down privkey/mnemonic.
- **Node key pair(s)** — one per node; used by consensus to sign blocks.

Provider operation keys can later be listed / added / revoked / thresholded (`pmc provider key *`) independently of the original registration key (that original key remains the provider identifier). FT4 financial keys on Economy Chain are a separate concern (`pmc economy auth-descriptor-evm-swap`).

### Clusters

- **System cluster:** one per network; runs system chains; replicates all cluster anchoring chains.
- **Dapp cluster:** nodes run dapp blockchains in isolated containers. Each dapp cluster has a Directory Chain **replica** and a **Cluster Anchoring Chain**. Dapp block headers → cluster anchoring → System Anchoring Chain → Ethereum (Transaction Submitter Chain).

`pmc economy add-cluster` proposes a cluster: name, voter set, governor, tag, cluster units, extra storage. Default container-unit sizes in that command match one SCU: `--cu-cpu 50` (0.5 CPU), `--cu-ram 2048` MiB, `--cu-storage 16384` MiB, `--cu-io-read 25`, `--cu-io-write 20`. Default `--system-container-units 4`.

Economy **tags** hold SCU price and extra-storage price **in USD per day** (`pmc economy add-tag --scu-price --extra-storage-price`). Price-oracle updates are proposals (`pmc economy update-price-oracle-rates`).

### Containers and SCUs

Blockchains run in **containers** on every node of a cluster. Resources are isolated (other containers on the same node do not steal your vCPU/RAM/I/O).

**One SCU** (official hosting page):

- 2 GB RAM
- 0.5 vCPU
- 16 GB storage
- I/O 25 MiB/s read, 20 MiB/s write

Lease **weekly**, pay in **CHR**, priced to a **USD-equivalent target**. Cost drivers: number of SCUs + extra storage beyond the SCU allocation. Manual pay or auto-renewal. Any participant can pay.

**Published cost target:** “The current weekly target cost is approximately **90 USD** for a default **7-node dapp cluster**.” Docs do **not** publish a fixed CHR-per-SCU number (CHR amount moves with the oracle). Vault and `pmc lease create-container` compute the CHR total at lease time.

Default `pmc container add` limits: `--container-units 1`, `--max-blockchains 10`, `--extra-storage 0` MiB.

Overdue lease: **6-month suspension**, then **permanent delete**.

### Provider staking (required to earn)

Reviewed periodically; may change via governance.

| Role | Total stake per node | Of which self-stake (native CHR on Mainnet) |
| --- | --- | --- |
| System provider | 600,000 CHR | ≥ 10% = 60,000 CHR |
| Node provider | 300,000 CHR | ≥ 10% = 30,000 CHR |

Self-stake must be **native CHR on Chromia Mainnet**. Delegated stake (provider overview): native CHR, ERC-20 CHR, or BEP-20 CHR. User-delegation pages describe delegations as **native CHR**. Do not flatten those two statements.

PMC: `pmc economy set-provider-staking-account`, `pmc economy update-provider-staking-reward-share` (0–100%). Default **10%** of provider reward to delegators; change applies after a **one-week delay**.

Rewards come from the hosting-fee pool. Availability factor: 90% uptime → 0, 100% → 1 (uptime must exceed 90% to earn). SP risk share currently **0.1**; NP/dapp-provider risk share currently **0.2**; system-provider fee share currently **0.1**. Full formulas: [Provider rewards](https://docs.chromia.com/ecosystem/providers/rewards).

---

## 4. `chromia.yml` deployments section and Blockchain RID

### Shape

```yaml
blockchains:
  hello:
    module: main

deployments:
  testnet:          # reserved name → CLI fills Directory BRID + URL
    container: <ContainerID>
    # optional if name is testnet/mainnet:
    brid: x"6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92"
    url: https://node0.testnet.chromia.com:7740
    # after first successful create:
    chains:
      hello: x"<DappBlockchainRID>"

  mainnet:
    container: <ContainerID>
    chains:
      hello: x"<DappBlockchainRID>"
```

| Field | Meaning |
| --- | --- |
| `deployments.<name>` | Target id. `mainnet` / `testnet` auto-fill Directory `brid` + `url`. |
| `brid` | **Directory Chain** RID of the target network (byte array / hex). |
| `url` | One system-node URL or a list. |
| `container` | Container ID from Vault / PMC lease. |
| `chains.<blockchain>` | **Dapp** Blockchain RID. Key must match a `blockchains:` name. Omit on first create; required for update. |

Lost dapp RID: Explorer (or `chr deployment info`).

`url` examples from project-config:

```yaml
url: https://system.chromaway.com
url:
  - https://system.chromaway.com
  - https://mainnet-dapp1.sunube.net:7740
```

### Blockchain RID vs other ids

- **Blockchain RID (BRID):** global identifier of a specific blockchain configuration/code. Clients, `chr query --blockchain-rid`, Explorer, and `chains:` all use this. First-deploy printout is the **genesis** RID and stays the identifier across updates.
- **Chain ID / IID:** local instance id (node-specific). Directory Chain IID is 0 in client docs.
- RID uniqueness: create fails if configuration+code already exists on the network.

Useful `blockchains.*.config` production knobs (see [Configuration properties](https://docs.chromia.com/build/configuration/blockchain-properties)): `features.merkle_hash_version` (default **2**; v1 deprecated for hash-collision bugs), `blockstrategy.maxblocksize` (dapp max 26 MiB), `gtx.max_transaction_size` (default 25 MiB), allowed GTX modules include Rell, StandardOps, ICMF, ICCF, **EIF**, WebStatic.

Delayed config application (user-protection):

```yaml
blockchains:
  <name>:
    config:
      directory_chain:
        config_delay: 86400000   # ms; 24h example from security tips
```

A voted config is applied only after the delay. Query: `list_delayed_blockchain_configs`.

Do **not** put DB passwords or private keys in committed YAML. Docs: store secrets in the environment; `CHR_DB_*` overrides; `${MY_VAR:-default}` substitution is supported. `libs.*.insecure: true` (skip library RID hash) is **not recommended for production**.

---

## 5. Upgrades: applying a Rell module update

### Mechanism

1. Change Rell / `chromia.yml`.
2. `chains:` **must** already contain the genesis Blockchain RID.
3. `chr deployment update --settings chromia.yml --network <mainnet|testnet> --blockchain <name>`
4. Single-owner: the signed update is the new blockchain configuration.
5. Multi-owner: update is a proposal; required voter-set signatures (`chr deployment proposal vote` / `pmc proposal vote`).
6. Optional: `--height` to schedule; `--verify-only` to dry-run.

On start/update the node **alters the SQL schema** for new/changed entities and objects. Limits are in [Entity — Changing entity definitions](https://docs.chromia.com/rell/language-features/modules/entity) (the page the deploy guides link as “limitations on updating Rell entities”).

### Compatible (safe) entity changes

- Add attributes **with default values** (existing rows get the default).
- Add attributes on **empty** tables even without defaults.
- **Remove** attributes.
- Change attribute **mutability** (mutable ↔ immutable) on entities **not** annotated `@log`.

### Incompatible (breaking) entity changes

- Change an attribute **type** (compatibility / data-corruption risk). “Plan your schema carefully.”
- **Add or remove `@log`** on an existing entity.

`@log` entities get an implicit `transaction` attribute and cannot be updated or deleted.

There is **no documented in-place type-migration tool**. A breaking entity change is not a normal `deployment update`. Docs do not describe a supported production rewrite path for type changes.

Governance starter kit (optional) adds proposal/veto flows on top of this.

---

## 6. Observability

### Explorer (dapp developers)

Official explorer: https://explorer.chromia.com/ (HTTP 200 on 2026-08-26; defaults to /mainnet). Testnet: set Current network to Testnet, or https://explorer.chromia.com/testnet/cluster/system.

Documented capabilities (https://docs.chromia.com/ecosystem/block-explorer/features and https://docs.chromia.com/ecosystem/block-explorer/using-explorer):

- Switch Mainnet / Testnet; inspect system cluster (directory, economy, system anchoring) and dapp clusters (docs mention the pink cluster and Filehub as a utility dapp).
- Search txs, accounts, blocks, providers, proposals, tokens.
- Per-chain: name, Blockchain ID (RID), height, tx count, accounts.
- Config. and API: Queries (run live), Operations (submit; wallet connect), Source code, Config file (blockstrategy, gtx, rell, revolt).
- Providers / proposals / CHR stats.

This is the documented way to confirm a deploy, recover a RID, and inspect live queries without Prometheus.

### Node logs (providers)

Enable-logging page (https://docs.chromia.com/ecosystem/providers/nodes/logging):

- System logs in all containers; level via log4j2.yml in /data/chromia/node/node-mounted-files.
- Do not leave debug on (fills disk). Example: root warn, net.postchain info, JSON console appender.

Dapp operators can propose slow-db-statement-log-ms via `chr deployment container configuration` / `pmc container configuration`.

### Metrics (providers)

Prometheus / Grafana page (https://docs.chromia.com/ecosystem/providers/nodes/setup-prometheus):

- Enable: metrics.prometheus.port=9190 in node properties, or `chr node start -p metrics.prometheus.port=9190`, or POSTCHAIN_PROMETHEUS_PORT. Logging page also mentions addmetrics.prometheus.port=9190 in node-config.system.properties.
- Scrape localhost:9190/metrics. With subnodes, use Chromia metrics-collector Docker image and scrape it.
- Do not aggregate metrics across the cluster as a single performance number (docs).
- Published series include blockchains, subnodes, containers, submitted_transactions (OK/INVALID/DUPLICATE/FULL), transaction_queue_size, processed_transactions (ACCEPTED/REJECTED), blocks / signedBlocks / confirmedBlocks, confirmedTransactions, blockHeight, revolt counters, query timers. Tags: node_pubkey, chainIID, blockchainRID, queryName.
- Sample Grafana dashboard and an alert (node is not producing or retrieving blocks, rate(blockHeight_total[10m])) are in that page.

`pmc economy metrics --pubkey` returns Economy-Chain provider metrics (not Postchain Prometheus).

No public hosted Grafana/metrics URL for dapp developers is documented. Explorer is the public observability surface.

---

## 7. Security: keys and secrets

### Key classes

| Key | Who | Use |
| --- | --- | --- |
| Wallet / EVM key | Developer or user | Vault deposit, lease payment, chr tx --evm-auth, faucet claim |
| Container / deployment key (chr keygen --key-id) | Dapp deployer | chr deployment create/update/pause/remove; pubkey registered on the lease |
| Provider key | Provider | Directory/Economy proposals; PMC privkey/pubkey in .chromia/config |
| Node key | Each node | EBFT block signatures |
| FT4 account / admin pubkey | Dapp | lib.ft4.core.admin.admin_pubkey and auth descriptors -- not a frontend secret |
| Filehub admin privkey | Filehub operator | FilehubAdministrator |

CLI key precedence: --secret > --key-id > --config key.id > project .chromia/config > ~/.chromia/config. Keys themselves always live under ~/.chromia/ (or --file).

### Official never-commit / never-ship rules

- Keep your private key secure. You may share the public key, but the private key should never be shared.
- Never share your private key. Anyone with your private key can control all accounts associated with it. If you lose your private key, you lose access to your accounts permanently.
- Provider key: keep this key pair safe as it is not possible to recover; write down the privkey/mnemonic.
- Production clients: remove admin key pair configuration; do not embed privkeys. Hello World / connect-client pages show deleting adminPubkey / adminPrivkey from production frontend.
- chromia.yml: Consider storing sensitive information like passwords in secure environments instead of directly in the YAML file. Use CHR_DB_PASSWORD etc.
- libs.*.insecure: false in production.
- Revoked provider keys cannot be re-added.
- Rate-limit new FT4 accounts (points_at_account_creation, recovery_time) and require(...) on operations (https://docs.chromia.com/rell/security).
- Optional config_delay so users can see a config change before it applies.

.chromia/, *_mnemonic, --secret files, and FT4 admin privkeys belong in gitignore / secret storage, not the repo. Docs do not publish a sample .gitignore; the rule is the key-management and never-share language above.


---

## 8. Adjacent production pieces

### Filehub

Decentralized on-chain file storage: Filehub chain (index, billing, Filechain selection) plus one or more Filechain blob chains. Payment proven with ICCF before a Filechain accepts chunks.

- Price: 0.10 USD per MB, perpetual; files kept at least 30 years; no renewal while the network runs. Reads are free. Encrypt before upload if private.
- Account: minimum 1 CHR deposit from Economy Chain (bridge via Vault if CHR is still on EVM; then Vault Filehub transfer).
- Testnet Filehub uses tCHR (faucet); Testnet files are not guaranteed permanent.

- Client package is documented under Filehub client pages. RID is not hardcoded in those docs.
- Live Filehub UIs checked 2026-08-26: filehub.chromia.com (200) and filehub.testnet.chromia.com (200). Docs refer to Filehub UI without always printing those hosts.
- Own Filehub deploy: lease a container, write chromia.yml, deploy the Filehub blockchain, then filechain and bundle setup pages. Admin API can register Filechains and configure the payment asset (prefer Economy Chain).

### EIF / EVM bridge

Ethereum Interoperability Framework (EIF) is a GTX module (EifGTXModule plus EifSynchronizationInfrastructureExtension) that reads EVM events and injects them via the evm_block special operation.

Components: Token Bridge (EVM vault), Event Receiver Chain, Bridge Chain (ICMF or embedded), dapps that receive minted FT4 via cross-chain transfer.

Modes:

- Token originally on EVM: lock and mint / burn and release.
- Token originally on Chromia: Chromia Token Bridge.

Withdrawals: user burns on Chromia, builds a proof, submits to the EVM contract, waits a configured lockup, then funds release. Mass-exit exists for EVM-origin tokens if a majority of nodes is compromised.

Lease a bridge (skip self-hosting the Chromia-side component; you still deploy the EVM contract): Vault, container overflow menu, Bridge lease. Pay CHR (priced from ETH). Then register the lease on the EVM contract.

Published validator contract addresses (bridge-lease page):

- BSC Testnet: 0x83dB85F7ef4447524D3A31c0F4664A89173C68Eb
- BSC Mainnet: 0xc755927508b7Ac3f7B31c9Ed396F3bE91C723d00
- Ethereum Mainnet: 0xc755927508b7Ac3f7B31c9Ed396F3bE91C723d00

CLI helper: chr eif generate-events-config builds the Solidity-event YAML or XML that EIF listens to.

Economy Chain is the official CHR hub and bridges CHR to Ethereum Mainnet and BNB Smart Chain.

### FT4 on Mainnet

FT4 is the account and token standard (required for Economy Chain, Vault, and Filehub payments). Native CHR on Economy Chain is an FT4 asset.

- Account ID is hash of a public key, or hash of an EVM address.
- Auth descriptors: multi-sig, roles, temporary access, recovery.
- Rate limits and account-creation strategies (open / transfer-fee / subscription) are moduleArgs in chromia.yml.
- Cross-chain FT4 transfers use ICCF.
- Token Chain is the system chain for FT4 issuance, shared accounts, and bridge automation; tokens can sit there during a dapp upgrade.

Library-chain installs default to mainnet unless registry is testnet. Git-hosted FT4 still needs rid plus insecure: false in production.

Vault listing after deploy: implement find_dapp_details (hardcoded or dynamic) and run deployment update. Vault then lists the dapp automatically.

### Staking: native CHR as of April 2026

Official phase-out text (staking summary and user-delegation pages):

Staking rewards on Ethereum and Binance Chain are being phased out. As of December 2, 2025, rewards on EVM chains will gradually decrease and will be discontinued by April 1, 2026. Official docs encourage all stakers to migrate to native CHR staking on Chromia Mainnet through Chromia Vault.

User flow: Vault, connect a wallet that holds native CHR, pick a provider, delegate. Rewards daily. Documented base APR 3 percent governance rewards, plus variable provider/network-fee share. Undelegate: 14-day (336-hour) unbonding; no rewards during unbonding.

Provider self-stake is already native Mainnet CHR (see section 3).

Economy Chain module args (node-config reference; example values, not a live Mainnet dump): staking_withdrawal_delay_ms example 1209600000 (14 days), plus reward-rate and payout-interval fields. Treat those as config schema, not current Mainnet constants. Live constants: pmc economy get-constants.

---

## 9. End-to-end production sequence (as documented)

1. Generate a deploy key (chr keygen with a key-id). Store privkey and mnemonic offline. Set key.id in ~/.chromia/config.
2. Get tokens: Testnet faucet (1000 tCHR / 7 days) or Mainnet CHR deposit (at least 10 CHR from ETH or BSC, or at least 20 CHR from another Chromia chain).
3. Lease a container (Vault or pmc lease create-container): cluster, pubkey, SCUs, extra storage, weeks, auto-renew. Save Container ID.
4. chromia.yml: deployments.mainnet or deployments.testnet with container. Reserved names fill Directory BRID and URL; otherwise set them from Explorer.
5. Ensure code uniqueness, then chr deployment create with network and blockchain name.
6. Paste the printed chains mapping (name to BRID) into chromia.yml. Confirm on Explorer after about five minutes on Testnet.
7. Optional: add dapp providers and a voter-set threshold for multi-sig updates.
8. Client: directoryNodeUrlPool must be current system-cluster API URLs from Explorer, plus the dapp BRID. No privkeys in the client.
9. Pay the lease (or enable auto-renew). Overdue: six-month suspend, then delete.
10. Updates: compatible entity changes only, then chr deployment update (or proposal plus votes). Use verify-only first if needed.
11. Observe: Explorer queries/source/config; providers add Prometheus and log4j2.yml.

---

## Canonical URLs

All of the following returned HTTP 200 on 2026-08-26 unless marked otherwise.

### Networks, deploy, config

- https://docs.chromia.com/
- https://docs.chromia.com/build/deployment/
- https://docs.chromia.com/build/deployment/testnet/getting-started
- https://docs.chromia.com/build/deployment/testnet/get-container
- https://docs.chromia.com/build/deployment/testnet/deploy-dapp
- https://docs.chromia.com/build/deployment/testnet/connect-client
- https://docs.chromia.com/build/deployment/mainnet/getting-started
- https://docs.chromia.com/build/deployment/mainnet/get-container
- https://docs.chromia.com/build/deployment/mainnet/deploy-dapp
- https://docs.chromia.com/build/deployment/mainnet/connect-client
- https://docs.chromia.com/build/deployment/mainnet/multi-deployment
- https://docs.chromia.com/build/deployment/testnet-tokens/get-tchr-chromia
- https://docs.chromia.com/get-started/create-dapp/deploy-to-testnet
- https://docs.chromia.com/build/cli/commands/deployment
- https://docs.chromia.com/build/cli/commands/keygen
- https://docs.chromia.com/build/cli/key-pair-management
- https://docs.chromia.com/build/configuration/project-config
- https://docs.chromia.com/build/configuration/blockchain-properties
- https://docs.chromia.com/build/deployment/vault-listing/quick-vault-listing

### Architecture, hosting, economy

- https://docs.chromia.com/get-started/about/architecture/platform-architecture
- https://docs.chromia.com/get-started/about/architecture/chains/directory-chain
- https://docs.chromia.com/get-started/about/architecture/chains/economy-chain
- https://docs.chromia.com/get-started/about/architecture/chains/token-chain
- https://docs.chromia.com/get-started/about/hosting
- https://docs.chromia.com/ecosystem/providers/nodes/directory-chain-config
- https://docs.chromia.com/ecosystem/providers/nodes/economy-chain-config

### Providers, PMC, containers, staking

- https://docs.chromia.com/ecosystem/providers/overview
- https://docs.chromia.com/ecosystem/providers/rewards
- https://docs.chromia.com/ecosystem/providers/pmc/
- https://docs.chromia.com/ecosystem/providers/pmc/pmccli-installation
- https://docs.chromia.com/ecosystem/providers/pmc/commands/economy
- https://docs.chromia.com/ecosystem/providers/pmc/commands/container
- https://docs.chromia.com/ecosystem/providers/pmc/commands/lease
- https://docs.chromia.com/ecosystem/providers/container-management/
- https://docs.chromia.com/ecosystem/providers/container-management/transfer-container-ownership
- https://docs.chromia.com/ecosystem/providers/nodes/provider-keypair
- https://docs.chromia.com/ecosystem/providers/nodes/manage-provider-keys
- https://docs.chromia.com/get-started/about/staking-summary
- https://docs.chromia.com/get-started/about/staking/
- https://docs.chromia.com/get-started/about/staking/provider-staking
- https://docs.chromia.com/get-started/about/staking/user-delegation

### Upgrades, security, observability

- https://docs.chromia.com/rell/language-features/modules/entity
- https://docs.chromia.com/rell/security
- https://docs.chromia.com/ecosystem/block-explorer/overview
- https://docs.chromia.com/ecosystem/block-explorer/features
- https://docs.chromia.com/ecosystem/block-explorer/using-explorer
- https://docs.chromia.com/ecosystem/providers/nodes/logging
- https://docs.chromia.com/ecosystem/providers/nodes/setup-prometheus
- https://docs.chromia.com/reference/terminology

### Filehub, EIF/bridge, FT4

- https://docs.chromia.com/ecosystem/filehub/overview
- https://docs.chromia.com/ecosystem/filehub/filehub-setup/deploy-filehub
- https://docs.chromia.com/ecosystem/filehub/configure-filehub/filehub-configure
- https://docs.chromia.com/ecosystem/filehub/configure-filehub/filehub-work
- https://docs.chromia.com/build/clients/filehub-client
- https://docs.chromia.com/ecosystem/bridge/overview
- https://docs.chromia.com/ecosystem/bridge/bridge-lease
- https://docs.chromia.com/build/cli/commands/eif
- https://docs.chromia.com/build/ft4/intro

### Official product hosts

- https://explorer.chromia.com/
- https://vault.chromia.com/
- https://vault.chromia.com/en/deposit
- https://vault.chromia.com/en/containers/
- https://vault.testnet.chromia.com/
- https://vault.testnet.chromia.com/en/containers/
- https://filehub.chromia.com/
- https://filehub.testnet.chromia.com/

### Checked missing / not usable as a homepage

- https://faucet.chromia.com/ does not resolve. Use the Testnet Faucet linked from docs. https://faucet.testnet.chromia.com/ is live; docs do not print that host.
- https://filehub-gw.chromia.com/ and https://filehub-gw.chromia.com/mainnet/ returned 404 on 2026-08-26. Object URLs of that host under /mainnet/ plus a hash are what Vault-listing examples use.

### Redirect aliases (still 200)

Older paths such as https://docs.chromia.com/providers/pmc/ and https://docs.chromia.com/intro/architecture/platform-architecture redirect into the current /ecosystem/ and /get-started/ trees.

---

## Not published in official docs (do not invent)

- A fixed CHR price per SCU (only a roughly 90 USD per week target for a default 7-node dapp cluster, plus USD-per-day tag prices).
- Economy Chain BRID as a static hex (query get_economy_chain_rid).
- Filehub / Filechain production BRIDs (placeholders only).
- A public hosted metrics or Grafana URL for dapp developers.
- An in-place Rell attribute-type migration procedure.
