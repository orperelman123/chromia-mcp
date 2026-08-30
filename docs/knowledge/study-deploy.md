# Chromia official docs study: deploy / config / CLI / database / token-chain / vector-search

Compiled 2026-08-26 (Asia/Jerusalem) from official Chromia developer docs only. Pages were fetched with WebFetch and, when that timed out or returned 404, with `curl` (HTTP 200). Nothing here is inferred beyond what those pages state.

Compared against existing `/workspace/chromia-knowledge/production-deploy.md` (same date, broader production brief). A dedicated **delta** section is first.

---

## Crawl notes and path corrections

Official sitemap (`https://docs.chromia.com/sitemap.xml`, fetched 2026-08-26) is the source of truth for URLs. Several paths named in the task do not exist as indexes:

| Requested path | Result | Actual pages |
| --- | --- | --- |
| `https://docs.chromia.com/build/cli/` | **404** | `.../build/cli/introduction`, `.../build/cli/commands/`, plus per-command pages |
| `https://docs.chromia.com/build/deployment/frontend` | **404** | `.../build/deployment/deploy-frontend-dapp` |
| `https://docs.chromia.com/build/database/` | not in sitemap | `.../build/database/overview`, `.../build/database/getting-started` |
| `https://docs.chromia.com/build/vector-search/` | not in sitemap | `.../build/vector-search/overview`, `.../build/vector-search/sample-workloads` |

`/build/deployment/` and `/build/configuration/` exist as section indexes. `/build/token-chain/` exists as a section index.

CLI first-two-letter shortcuts are documented: `chr de cr` is the same as `chr deployment create`. Shell completion: `chr --generate-completion [bash|zsh|fish]`. Default CLI interaction uses `--use-db`; `--no-db` runs without a database.

Latest published CLI version on the release-notes page: **Chromia CLI 0.30.0** (released 2026-02-27). Bundled versions in that release: rell 0.15.2, postchain 3.49.2, postchain-chromia 3.39.3, eif 0.32.0, chromia-cli-tools 0.10.0.

---

## New vs `/workspace/chromia-knowledge/production-deploy.md`

`production-deploy.md` is a production-ops brief (networks, lease, deploy/update, providers/PMC, Filehub, EIF, FT4, observability, security). This study is the **developer-docs tree** for deploy + config + CLI + database + token-chain + vector-search. Overlap is intentional; the following is **new or more complete** here, or **changed in the official CLI changelog** since that brief's command descriptions.

### Material CLI changelog not reflected in `production-deploy.md`

From [CLI release notes](https://docs.chromia.com/build/cli/cli-release-notes):

- **0.30.0 (2026-02-27):** `chr deployment create` **writes the deployment result back to `chromia.yml`** and prints the changes. The older "you must paste the `chains:` snippet yourself or the next `create` overrides the previous deployment" warning in the deploy guides and in `production-deploy.md` is still printed by the command-reference page, but the 0.30.0 release notes say the CLI now writes it back.
- **0.30.0:** `chr deployment update` schema comparison now **detects enum changes** (add/remove/reorder). Dangerous enum changes (reordered or removed values) **block deployment until approved**.
- **0.30.0:** `chr build --skip-lib-check` skips library verification.
- **0.29.8 (2026-01-16):** dynamically load provider URLs instead of hardcoding them; **remove `url` and `brid` requirement** in `chromia.yml` for deployment action commands. Matches the reserved-name auto-fill already described in project-config, but the changelog is explicit.
- **0.28.3 (2025-10-24):** `chr deployment container configuration` added; `deployment pause-container` / `resume-container` reorganized as `deployment container pause` / `resume` (old names kept as aliases).
- **0.28.0 (2025-09-22):** `--mainnet` / `--testnet` as alternatives to `--api-url`; when only one blockchain is defined, config is inferred from `deployments.<network>`.

### Topics this crawl covers that `production-deploy.md` only sketches or omits

- **On-chain frontend hosting** (`webStatic`, Next.js `output: "export"`, URL `http://localhost:7740/web_query/<blockchainRid>/web_static`). Not in the production brief.
- **Vault listing as a full how-to** (quick hardcoded vs dynamic DB; `dapp_content_type` enum indexes; admin ops; `chr query find_dapp_details`; recommended image sizes; Filehub $0.10/MB; Testnet verification checkmark via Chromia team admin). Production brief has one paragraph.
- **BSC Testnet tCHR faucet** (BscScan Write Contract at `0x0e61dfdbd5b979adbf3e5b5fe7ee40f85b6daa8d`, token import `0x8e59d72e4dda56f26963c6b8c77ca1959e9a74f0`, weekly claim, tBNB gas). Production brief only covers Chromia Testnet faucet allowance.
- **Full `chromia.yml` schema** (compile, database env overrides, test, libs library-chain vs git, docs, YAML anchors, `!include`). Production brief only documents `deployments` + a few production knobs.
- **Full blockchain-properties table** (sync, query cache/timeouts, async queries, revolt, allowed GTX/sync extensions). Production brief lists a subset.
- **Complete `chr` command catalog** (node, query, tx, test, repl, library, seeder, generate, code, tools, multi-signature, eif, create-rell-dapp). Production brief covers `deployment`, `keygen`, and `eif` only.
- **Token Chain developer/user flows** (`propose_token`, `propose_token_bridge`, `mint_token`, 100 CHR listing fees, fee split, `get_token_chain_constants`, account funding from Economy Chain). Production brief only says Token Chain exists.
- **Vector DB extension** (lease with Vector DB selected, `VectorDbGTXModule`, `vector_db_extension` YAML, `com.chromia.vector_db` 2.2.0, `query_closest_objects`). Production brief does not cover this.
- **Database section** as published (thin product pages). Not in the production brief.

### Already covered in `production-deploy.md` (not repeated at the same depth here)

Provider staking, PMC lease/container/economy, Prometheus/log4j, Filehub operator deploy, EIF bridge-lease validator addresses, entity update compatibility, 6-month lease suspension, SCU sizes / ~90 USD weekly target, Economy Chain RID via `get_economy_chain_rid`, native-CHR staking phase-out. Those pages were **not** in this crawl's required tree.

### Docs typo observed

Testnet getting-started step 2 says "Specify your network (**mainnet**) and blockchain name" while describing Testnet deployment. The rest of the Testnet pages use `--network testnet`.

---

## 1. Deployment

Section index: [Deployment](https://docs.chromia.com/build/deployment/). Cards: Get Testnet tokens, Deploy to Testnet, Deploy to Mainnet, List on Vault, Deploy frontend dapp. For testing, docs also point to the Getting Started Testnet guide.

### 1.1 Get Testnet tokens

Index: [Get Testnet tokens](https://docs.chromia.com/build/deployment/testnet-tokens/). Two paths.

#### Chromia Testnet tCHR

Source: [get-tchr-chromia](https://docs.chromia.com/build/deployment/testnet-tokens/get-tchr-chromia).

1. Open the **Chromia Testnet Faucet** (page names it; does not print a host).
2. Connect wallet (MetaMask or Coinbase Wallet).
3. Request Tokens; complete CAPTCHA if prompted.
4. If no Testnet Economy Chain account exists, **Create Account**. The faucet then allocates the weekly allowance.
5. Sign a message in MetaMask (signature only; "no gas fees").
6. Tokens appear on Vault and Staking pages. "View my account in Vault" or open Testnet Economy Chain in Testnet Vault.

**Allowance:** 1000 tCHR every 7 days. Tokens have no real-world value. Revisit the faucet periodically for more.

#### BSC Testnet tCHR

Source: [get-tchr-binance](https://docs.chromia.com/build/deployment/testnet-tokens/get-tchr-binance). For bridge / EVM integration testing.

Documented difference:

- Chromia testnet tCHR: Economy Chain account via Chromia faucet; 1000 tCHR weekly.
- BSC testnet tCHR: claim to any EVM wallet; no special Chromia account.

Steps:

1. Open the faucet on BscScan **Write Contract**. The page links `https://testnet.bscscan.com/address/0x0e61dfdbd5b979adbf3e5b5fe7ee40f85b6daa8d#writeContract`.
2. Connect to Web3 (MetaMask, WalletConnect, Coinbase Wallet).
3. Import token in MetaMask: **`0x8e59d72e4dda56f26963c6b8c77ca1959e9a74f0`**.
4. Call the **`1. claim`** function, then Write. **tBNB gas is required.**
5. Claim is **once a week**.

### 1.2 Deploy to Testnet

Index: [Deploy to testnet](https://docs.chromia.com/build/deployment/testnet/). Five pages.

#### Getting started (overview)

Three steps: obtain a container, deploy, connect a client.

- Generate a public-private key pair **specifically for the testnet container**, separate from the wallet that receives tCHR.
- Lease a container in Chromia **Testnet Vault** to get a Container ID.
- Put Container ID in `chromia.yml`.
- CLI deploy; receive Blockchain RID.
- Client: pool of system node URLs + dapp Blockchain RID.

#### Get a container

Prerequisites: tCHR, Chromia CLI, a container key pair.

```bash
chr keygen --key-id="testnet_container_key"
```

Writes under `.chromia/`:

- Public: `.chromia/testnet_container_key.pubkey`
- Private: `.chromia/testnet_container_key`

Default key id if omitted: `chromia_key`. Re-running `chr keygen` without `--key-id` can overwrite it.

Docs: "Keep your private key secure. You may share the public key, but the private key should never be shared."

Lease:

1. https://vault.testnet.chromia.com/en/containers/ — connect wallet.
2. Lease a container, pick cluster.
3. Paste public key from `.chromia/<key-id>.pubkey`. Adjust SCUs, storage, duration; optional auto-renewal.
4. Sign. Result is a **Container ID**. Example in docs: `15ddfcb25dcb43577ab311fe78aedab14fda25757c72a787420454728fb80304`.
5. Container ID can be retrieved later on the same Vault page.

#### Deploy the dapp

Prerequisites: Chromia CLI, a compiling dapp, Container ID.

`chromia.yml` example from the Testnet deploy page (Directory Chain BRID and a system-node URL are written out):

```yaml
deployments:
  testnet:
    brid: x"6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92"
    url: https://node0.testnet.chromia.com:7740
    container: <ContainerID>
```

CLI key config at `~/.chromia/config`:

```text
key.id = testnet_container_key
```

Windows: `C:\Users\<user>\.chromia\config`. macOS/Linux: `/Users/<user>/.chromia/config` or `/home/<user>/.chromia/config`. Verify key files exist in `.chromia/`.

```shell
chr deployment create --settings chromia.yml --network testnet --blockchain hello
# optional:
chr deployment create --settings chromia.yml --network testnet --blockchain hello --config <config file path>
```

If create fails: change dapp code. "Chromia uses dapp configuration and code to create the Blockchain RID, which must be unique for each dapp."

On success, save the printed `chains:` mapping:

```yaml
deployments:
  testnet:
    chains:
      hello: x"<BlockchainRID>"
```

Update (requires `chains:`):

```shell
chr deployment update --settings chromia.yml --network testnet --blockchain hello
```

Entity-update limitations are linked to the Rell entity page (not re-fetched in this crawl).

**Warning on the Testnet deploy page:** "The deployed dapp will be accessible in about five minutes."

#### Connect a client

TypeScript `postchain-client`. Client needs a **pool of system-node URLs** plus the dapp **Blockchain RID**. It queries the Directory Chain on those nodes for the current dapp replica set.

Confirm deploy on Explorer with Current network = Testnet. Accessible in about five minutes.

Hello World frontends: remove admin key pair from production client code.

Testnet directory node pool as printed:

```ts
const directoryNodeUrlPool = [
  "https://node0.testnet.chromia.com:7740",
  "https://node1.testnet.chromia.com:7740",
  "https://node2.testnet.chromia.com:7740",
  "https://node3.testnet.chromia.com:7740",
];
const chromiaClient = await pcl.createClient({
  blockchainRID,
  directoryNodeUrlPool,
});
```

#### List on Testnet Vault

Same `find_dapp_details` contract as Mainnet Vault listing (see section 1.4). Testnet-specific extras:

- Prerequisites include tCHR and Filehub "or any comfortable web3 storage".
- After implementing the query: `chr deployment update --settings chromia.yml --network testnet --blockchain hello_world`.
- **Verification checkmark (optional):** contact the Chromia team admin; they review quality standards.
- Example mock seeder: GitLab `dapp-aggregator` `setupMocks.ts`.
- Hardcoded example repo and database-based repo are linked as "vault listing repository" / "database-based vault listing repo" (page does not print the full Git URLs in the extracted body).

### 1.3 Deploy to Mainnet

Index: [Deploy to Mainnet](https://docs.chromia.com/build/deployment/mainnet/). Pages: getting-started, get-container, deploy-dapp, connect-client, multi-deployment.

#### Getting started

Same three steps as Testnet: container (CLI key + Vault lease), deploy (`chromia.yml` + `chr deployment create` on Mainnet), connect client (system-node URL pool + Blockchain RID).

#### Get a container

Prerequisites: Chromia CLI, `chromia.yml`, key pair (`chr keygen` + key-pair management).

**Create a Chromia account** by depositing CHR from BNB Smart Chain or Ethereum Mainnet to the Economy Chain:

- At least **10 CHR** on BSC or Ethereum Mainnet in a compatible wallet (e.g. MetaMask).
- https://vault.chromia.com/en/deposit
- Connect EVM wallet, choose source network, deposit. Account is created and activated during deposit.

**Lease:**

1. Container lease page, then Lease a container.
2. Select cluster.
3. Enter public key; adjust SCUs, storage, duration; optional auto-renewal.
4. Accept price, then Lease (payment in CHR). Note Container ID.
5. If the lease fails, "a refund is initiated for the same amount."

The Mainnet get-container page does **not** restate the 20 CHR other-chain account-creation path that appears in `production-deploy.md` (that text is not on this page).

#### Deploy the dapp

Pre-deploy checklist on the page: `deployments.mainnet.brid`, `url`, `container`, and `chains` empty for first deployment. Using the reserved name `mainnet` "automatically configures the `brid` and `url`". A custom name requires those fields.

```yaml
blockchains:
  hello:
    module: main
deployments:
  mainnet:
    container: <ContainerID>
```

```shell
chr deployment create --settings chromia.yml --network mainnet --blockchain hello
# optional --config
```

Same uniqueness / save-`chains:` rules as Testnet. Update:

```shell
chr deployment update --settings chromia.yml --network mainnet --blockchain hello
```

#### Connect a client

Same Directory-Chain discovery model. Hello World: remove admin keys from production frontend.

Mainnet system-node pool as printed (page: "for the most recent and complete list, visit the Chromia Explorer and view the list of nodes in the system cluster"):

```ts
const directoryNodeUrlPool = [
  "https://system.chromaway.com",
  "https://chromia.validatrium.club",
  "https://chromia-mainnet-systemnode-1.stakin-nodes.com",
  "https://chroma.node.monster:7741",
  "https://dapps0.chromaway.com",
  "https://chromia-mainnet.w3coins.io:7740",
  "https://mainnet-dapp1.sunube.net:7740",
];
```

Published snippet uses `pcl.creatClient` (one `e`) in the Mainnet connect-client page. Testnet page uses `pcl.createClient`. Do not "correct" the Mainnet spelling; that is how the page is published.

#### Multi-owner deployment

Source: [multi-deployment](https://docs.chromia.com/build/deployment/mainnet/multi-deployment).

Prerequisites: a leased container, `pmc` and `chr`, public keys of intended dapp providers.

1. Create a container lease (Vault).
2. Promote others: `chr deployment voterset add-dapp-provider`
3. List: `chr deployment voterset list`
4. Add members (page uses `--add-members`):

```shell
chr deployment voterset update -vs <voter set name> --add-members <pk1>,<pk2>
```

The CLI command-reference page documents the flag as **`--add-member`** (singular). Both strings appear in official docs.

5. Threshold:

```shell
chr deployment voterset update -vs <voter set name> --threshold <int>
```

Page example: threshold `2` in a set of 3 = two-thirds. CLI command reference is more precise:

- `0`: supermajority `n - (n-1)/3` (usually around 67%)
- `-1`: simple majority
- positive number: that many voters

Updates:

```shell
pmc blockchain update
pmc proposal vote --accept --id <proposal ID>
```

Proposals via `chr`:

```shell
chr deployment proposal list
chr deployment proposal vote --id <proposal ID> --accept
chr deployment proposal revoke --id <proposal ID>   # creator only
```

Examples show `pmc voterset info --name chain_of_alliance` and `pmc container info --name coa` with threshold 1 (single) vs 2 (both providers must approve).


### 1.4 Vault listing (Mainnet + Testnet)

Index: https://docs.chromia.com/build/deployment/vault-listing/ — Vault lists a dapp automatically once it exposes find_dapp_details.

Prerequisites (index): deployed dapp on Mainnet or Testnet; media files; Filehub or any web3 storage.

Two approaches:

- Quick (hardcoded): best for prototypes. Fast, no admin key. Redeploy for every change.
- Dynamic (database): best for production / frequent updates. Updates without redeploy. Requires admin key management.

#### Quick setup

Module quick_vault_listing.rell. Query signature (keep dapp_rowid even if unused):

    query find_dapp_details(dapp_rowid: rowid, requested_content_types: list<dapp_content_type>? = null)

Return fields documented: rowid, name, description, launch_url, genre, chain_list (name, brid, role), content (media list). brid is the deployed dapp Blockchain RID. Content types: landscape, portrait, promotional, video, icon. Multiple images of the same type are allowed.

Import in main.rell: import quick_vault_listing;

    chr deployment update --network <deployment_name> --blockchain <blockchain_name>
    chr query --network <deployment_name> --blockchain <blockchain_name> --output-format json find_dapp_details dapp_rowid=0 'requested_content_types=[]'

Example Filehub media URL printed on the quick page: https://filehub-gw.chromia.com/mainnet/ff03a1098eb24314ecd5277cbf352d480318f53191610079c377cb864fc23d8b

#### Dynamic setup

Module dynamic_vault_listing.rell.

    struct module_args { admin_pubkey: byte_array; dapp_name: text; }

Entities: dapp (key name; mutable description, launch_url, genre), dapp_media (key dapp, name; mutable url; type), blockchain (key dapp, brid; index brid; index mutable name; mutable role).

Admin-gated ops: create_or_update_dapp, create_or_update_blockchain, create_or_update_dapp_media.

chromia.yml moduleArgs.dynamic_vault_listing: admin_pubkey and dapp_name.

    chr deployment update --network <deployment_name> --blockchain <blockchain_name>
    chr tx --network <deployment_name> --blockchain <blockchain_name> create_or_update_dapp "Your dapp description" "https://your-dapp-url.com" "Your genre"
    chr tx ... create_or_update_blockchain 'x"D2...3B2"' <blockchain_name> "Role description"
    chr tx ... create_or_update_dapp_media "icon" "https://..." 4

Media type enum index: landscape=0, portrait=1, promotional=2, video=3, icon=4.

Changes via those operations are immediate; no redeploy. Admin key is required for all metadata operations.

#### Shared media / Filehub notes

Recommended image sizes (vault-listing pages and Testnet list-dapp-vault):

- Horizontal preview: 180x100 (recommended x3: 540x300)
- Vertical preview: 180x240 (recommended x3: 540x720)
- Big image: 510x286 (recommended x3: 1530x858)
- Pagination image: 86x48 (recommended x3: 258x144)

Filehub: Fixed cost of $0.10 per MB for perpetual storage with free access for everyone. Requires CHR tokens on the Economy Chain (minimum 1 CHR deposit).

---

## 1.5 Deploy frontend on-chain

### deploy-frontend-dapp

_Official page extract. File: `build_deployment_deploy-frontend-dapp.txt`._

On this page 

Deploy the frontend of your dapp on-chain 

This topic provides a detailed guide on deploying a frontend application into the blockchain. It includes steps for
configuring environment variables, building and packaging the application, updating the blockchain settings, and
accessing the deployed application through a web interface. 

info 

While this topic uses Next.js for demonstration, you can use any frontend framework (React, Vue.js, Angular, Svelte,
vanilla JavaScript, etc.) as long as it generates files for deployment. Simply adapt the build commands and
configuration to match your chosen framework. 
Prerequisites ​ 

A dapp should be ready for the deployment. 

The dapp should be placed into the root of the frontend application file structure. An example can be found here . 

Steps to deploy ​ 

Run the node to get the blockchainRid value, it can be found in the terminal of dapp logs. 

chr node start --wipe 

Set the .env file by updating NEXT_PUBLIC_BRID to the new blockchainRid value. 

Replace the empty Next.js configuration with the following: 

const nextConfig = { 
output : "export" , 
images : { unoptimized : true } , 
basePath : ` /web_query/ ${ process . env . NEXT_PUBLIC_BRID } /web_static ` , 
} ; 

Build the frontend and package it using the command: 

pnpm build 

Add webStatic to your rell/chromia.yml file as shown below: 

blockchains : 
asset_management : 
webStatic : out # this attribute 

Update the blockchain to enable new queries by running: 

chr node update 

Navigate to the following URL to interact with your deployed webpage: 

http://localhost:7740/web_query/<blockchainRid>/web_static 

To learn more about developing production-ready dapps, visit the Chromia course page .


## 2. Configuration

Index: https://docs.chromia.com/build/configuration/ — project-structure, project-config, blockchain-properties. The three pages were fetched with WebFetch in this crawl (not from local HTML extracts).

### 2.1 Project structure

chr create-rell-dapp layout: chromia.yml; src/main.rell; src/test/arithmetic_test.rell and data_test.rell.

main.rell starts with the module keyword (single-file module) and includes a Hello World query.

Multifile modules: a folder containing module.rell that starts with module. Files in the same module see each other without imports.

Entry point in chromia.yml is the module name (filename for a single-file module, or folder name), not a file path. Use module_a, not module_a/module.rell.

### 2.2 Project settings (chromia.yml)

Default name chromia.yml; can be renamed. Most attributes have defaults.

blockchains: mandatory name + module (entrypoint). Optional: moduleArgs, config, test.

moduleArgs requires a matching struct module_args in that module; values are read as chain_context.args.<field>.

Per-blockchain test.modules / test.moduleArgs / test.failOnError override project-level tests.

deployments: reserved names mainnet and testnet auto-fill Directory Chain brid and url when used with chromia-cli.

Verify brid / url on Explorer: Current network, Clusters, system, copy an API URL; System Chains, directory_chain, copy brid.

Published Directory Chain BRIDs:

- Mainnet: x"7E5BE539EF62E48DDA7035867E67734A70833A69D2F162C457282C319AA58AE4"
- Testnet: x"6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92"

url: string or list of system-node URLs. Examples: https://system.chromaway.com or that URL plus https://mainnet-dapp1.sunube.net:7740.

container: Container ID. chains: dict of blockchain-name to dapp Blockchain RID. Keys must match blockchains:. Not required on first create; CLI prompts with the RID. Lost RID: Chromia Explorer.

compile example: rellVersion 0.14.9, source src, target build, deprecatedError false, quiet true, strictGtvConversion true. strictGtvConversion default true; recommended left true. Only available from Rell 0.13.9 (before that, behaved as false).

database (local PostgreSQL) complete example defaults: password/username/database postchain, host localhost, logSqlErrors true, schema rell_app, driver org.postgresql.Driver. Minimal if following the official Postgres setup: database.schema only.

Do not store passwords in YAML. Env overrides (take precedence): CHR_DB_URL, CHR_DB_USER, CHR_DB_PASSWORD, CHR_DB_SCHEMA.

String substitution in YAML: foo: ${MY_VAR:-default_var}.

test: modules, moduleArgs, failOnError. Blockchain-level test overrides project-level.

libs, two kinds.

Library-chain (recommended):

    libs:
      <library_id>:          # e.g. com.chromia.ft4
        version: <version>   # required
        registry: mainnet    # mainnet (default) | testnet | localhost | custom URL
        brid: x"..."         # only for custom registry URL

External Git:

    libs:
      <library_name>:
        registry: <git url>
        path: <path inside repo>
        tagOrBranch: <value>
        rid: x"<GTV hash of library Rell files>"
        insecure: false      # true skips rid check; not recommended for production

docs: title, footerMessage, customStyleSheets, customAssets (copied to images/), additionalContent (markdown; # Dapp <title> must match docs.title; # Module <name> sections), sourceLink.remoteUrl, sourceLink.remoteLineSuffix (#L for GitHub/GitLab, #lines- for Bitbucket). Generate-docs-site page also documents additionalModules.

YAML anchors must live under definitions:. !include other.yml and !include other.yml#tag are supported.

### 2.3 Blockchain configuration properties

Set under blockchains.<name>.config. Example skeleton includes signers, configurationfactory, features.merkle_hash_version: 2, blockstrategy, gtx.modules.

Core:

- signers (array, required)
- sync (string, default "")
- sync_ext (array, default []): IcmfReceiverSynchronizationInfrastructureExtension, EifSynchronizationInfrastructureExtension
- configurationfactory (string, required)
- txqueuecapacity (int, 2500)
- historic_brid (bytea, fork)
- dependencies (gtv)
- config_consensus_strategy (string)
- query_cache_ttl_seconds (int, 0 disables)
- async_query_queue_capacity (int, 0 disables)
- async_query_timeout_seconds (int, 3600)
- async_query_result_retention_seconds (int, 3600)
- query_timeout_seconds (int, 60; <=0 disables)
- max_block_future_time (int, 60000 ms; -1 disables)
- add_primary_key_to_header (boolean, false; not verified)

features.merkle_hash_version default 2. v1 deprecated (hash-collision bugs).

blockstrategy: name (BlockBuildingStrategy class); maxblocksize default 26 MiB (dapp max 27262976); maxblocktransactions 100; mininterblockinterval default 25 (dapp minimum allowed 1000 ms); maxblocktime 30000; maxtxdelay 1000; minbackofftime 20; maxbackofftime 2000; maxspecialendtransactionsize 1024; preemptiveblockbuilding true.

gtx: max_transaction_size 25 MiB; max_transaction_signatures 100; default allowed modules Rell, StandardOps, IcmfSender, IcmfReceiver, Iccf, Eif, WebStaticGTXModuleFactory; allowoverrides false; slow_op_threshold -1; slow_prioritization_query_threshold -1.

revolt: timeout 10000; exponential_delay_initial 1000; exponential_delay_power_base 1.2 (string); exponential_delay_max 600000; fast_revolt_status_timeout -1; revolt_when_should_build_block false.

### 2.4 Configuration source URLs

- https://docs.chromia.com/build/configuration/project-structure
- https://docs.chromia.com/build/configuration/project-config
- https://docs.chromia.com/build/configuration/blockchain-properties


WebFetch of project-config, blockchain-properties, and project-structure succeeded in this crawl; those pages are summarized in the existing production-deploy.md deployments section and are restated in the commentary already written above for Directory BRIDs, compile/database/libs/docs, and the properties tables. The three source pages are:

- https://docs.chromia.com/build/configuration/project-structure
- https://docs.chromia.com/build/configuration/project-config
- https://docs.chromia.com/build/configuration/blockchain-properties

## 3. CLI source extracts

Command index: https://docs.chromia.com/build/cli/commands/ (not /build/cli/ which is 404).
WebFetched and used in section 1 rather than re-dumped here: commands/deployment, commands/keygen, key-pair-management.
The blocks below are official page extracts (curl + HTML text extraction). CLI 0.30.0 release notes are included in full.


### Introduction

_Official page extract. File: `build_cli_introduction.txt`._

On this page 

Introduction to Chromia CLI 

Chromia CLI simplifies the development cycle and deployment of
Rell dapps, where all the needed capability is available in one CLI. It's a command-line tool that provides a way to
interact with the Chromia blockchain using a set of commands. 

The Chromia CLI tool is designed to be user-friendly and easy to use. It provides a set of commands that can be used in
a terminal or console window and supports a range of options and flags to customize the behavior of the commands. 

For information about installing Chromia CLI, see Install Chromia CLI . Once installed, you can
use Chromia CLI to interact with the Chromia blockchain and build decentralized applications and services. 

tip 

You can use Chromia CLI in Gitlab CI and Bitbucket Pipeline. Read the continuous integration article for more
information. 
How the CLI Works ​ 

The Chromia CLI is your main tool for building and deploying dapps. Understanding how different commands work together
will help you build more efficiently. 

Local Development vs Remote Operations ​ 

Local commands (work with your local node): 

chr node start - Starts a local blockchain node on your machine 

chr query - Queries a local or remote blockchain 

chr test - Runs your test files 

chr build - Builds your blockchain configuration 

Remote commands (work with Testnet/Mainnet): 

chr deployment create - Deploys your dapp to a remote network 

chr deployment update - Updates an existing deployment 

Common Development Workflow ​ 

Here's a typical workflow when building a Chromia dapp: 

1. Create your project 

chr create-rell-dapp 

This generates a new Rell project with a basic structure including 
chromia.yml , 
main.rell , and test files.
In 
main.rell you will see some basic Rell code, including the query 
hello_world 

2. Start local node (requires PostgreSQL) 

chr node start 

This compiles your Rell code and starts a blockchain locally. 

3. Query your dapp 

chr query hello_world 

Tests a query against your running node. When running a single local blockchain, no additional options are needed— 
chr query targets the local node by default. 4. Run tests 

chr test 

Execute your Rell test files to verify your dapp works correctly. 

5. Deploy to Testnet See how to deploy your code to testnet here . 

chr deployment create 

Once your dapp is ready, deploy it to Chromia's public Testnet for others to interact with. 

Database Requirement ​ 

Chromia is built on relational blockchain technology, leveraging SQL databases for efficient data storage and querying.
For local development, this means you need a running database when using CLI commands that interact with a local blockchain. 

See Database setup for installation instructions. 

Understanding Key Management ​ 

The CLI automatically manages keys stored in 
~/.chromia/ . When you run deployment commands, the CLI uses your
configured key to sign transactions. 

Keys are used to: 

Sign transactions when deploying or updating dapps 

Authenticate operations on the blockchain 

Prove ownership of accounts 

See Key Pair Management for detailed information on generating and managing keys.


### build

_Official page extract. File: `build_cli_commands_build.txt`._

build 

Usage: chr build [<options>] 

Build an application and create a blockchain configuration 

Configuration Properties: 
-s, --settings=<settings> Alternate path for project settings file 

Options: 
-bc, --blockchain=<blockchain> 
Explicitly specify which blockchain(s) to compile 
-f, --format=(GTV|XML) Blockchain configuration format 
--hide-lib-warnings Hide library warnings in build output 
--skip-lib-check Skipping library verification step 
-h, --help Show this message and exit 

The build command ( 
chr build ) creates a blockchain configuration for your dapp . By default, it reads from 
src folder and built
configuration gets placed in the root folder under 
build , but you can change it in the project config file ( 
chromia.yml ) in 
compile:source and 
compile:target . 

If you are in the working directory where the 
chromia.yml file presides, you can run the following command to create
the blockchain configuration: 

chr build 

Otherwise, you can set a path for the 
chromia.yml file or specify a different Project Settings file: 

chr build --settings chromia.yml


### create-rell-dapp

_Official page extract. File: `build_cli_commands_create-rell-dapp.txt`._

create-rell-dapp 

Usage: chr create-rell-dapp [<options>] [<name>] 

Generates a template project 

Template projects: 
Minimal - Minimal working example including sample queries/operations and 
tests. 
Plain - A plain skeleton with empty main and test files. 
Plain-Multi - A plain skeleton with empty main and test files using multiple 
modules. 
Plain-library - A plain skeleton with structure for library development 
Asset Management - A template focused on asset management on the Chromia 
blockchain. It includes components for blockchain operations and a frontend 
for user interaction 

Options: 
-d, --base-dir=<path> Directory to generate template project in 
--template=(plain|plain-multi|minimal|plain-library|asset-management) 
Project template 
--devcontainer Setup devcontainer for project 
-h, --help Show this message and exit 

Arguments: 
<name> Dapp name 

You can use the create-rell-dapp command ( 
chr create-rell-dapp ) to create a new Rell structured "Hello World" project. It creates a project config file ( 
chromia.yml ), a main module ( 
main.rell ) in the 
src/ folder, and test files in the 
src/test/ folder in your
working directory. 

chr create-rell-dapp 

The project structure is as follows: 

|--chromia.yml 
|--src 
|--main.rell 
|--test 
|--arithmetic_test.rell 
|--data_test.rell


### code

_Official page extract. File: `build_cli_commands_code.txt`._

code 

Usage: chr code [OPTIONS] COMMAND [ARGS]... 

Code quality management (checking, formatting, linting) 

╭─ Options ───────────────────────────────────────────────────────────────────╮ 
│ -h, --help Show this message and exit │ 
╰─────────────────────────────────────────────────────────────────────────────╯ 
╭─ Commands ──────────────────────────────────────────────────────────────────╮ 
│ lint Analyze Rell code to find potential issue and coding style │ 
│ violations. Configurable using .rell_lint file │ 
│ format Automatically format Rell code. Configurable using .rell_format │ 
│ file │ 
│ check Check Rell code for compilation errors │ 
╰─────────────────────────────────────────────────────────────────────────────╯ 

You can use the code command ( 
chr code ) for code quality management (formatting and linting). 

code lint 

Usage: chr code lint [<options>] [<files>]... 

Analyze Rell code to find potential issue and coding style violations. 
Configurable using .rell_lint file 

Configuration Properties: 
-s, --settings=<settings> Alternate path for project settings file 

Options: 
--source-dir=<path> source directory 
-fo, --formatter-options=<path> 
Formatter options file (default '.rell_format') 
-lo, --linter-options=<path> Linter options file (default '.rell_lint') 
--fix Fix all auto-fixable issues 
-h, --help Show this message and exit 

Arguments: 
<files> Files or dirs to show/fix linter issues from. (*.rell: All files 
ending with rell extension) (main/*: Matches all files in main 
directory on explicit path) (**/main/*: Matches all files in main 
directory independent on parent paths) 

The lint command 
chr code lint allows you to analyze Rell code to find potential issue and coding style. 

code format 

Usage: chr code format [<options>] [<files>]... 

Automatically format Rell code. Configurable using .rell_format file 

Configuration Properties: 
-s, --settings=<settings> Alternate path for project settings file 

Options: 
--source-dir=<path> source directory 
--file=<path> single Rell file 
-fo, --formatter-options=<path> 
Formatter options file (default '.rell_format') 
-h, --help Show this message and exit 

Arguments: 
<files> Files or dirs to format (*.rell: All files ending with rell 
extension) (main/*: Matches all files in main directory on explicit 
path) (**/main/*: Matches all files in main directory independent on 
parent paths) 

The format command 
chr code format allows you to automatically format Rell code. It can be configured by using 
.rell_format . 

code check 

Usage: chr code check [<options>] 

Check Rell code for compilation errors 

Configuration Properties: 
-s, --settings=<settings> Alternate path for project settings file 

Options: 
--hide-lib-warnings Hide library warnings in build output 
-h, --help Show this message and exit 

The check command 
chr code check allows you to check Rell code for potential issues and style violations.


### eif

_Official page extract. File: `build_cli_commands_eif.txt`._

eif 

Usage: chr eif [OPTIONS] COMMAND [ARGS]... 

Ethereum Integration Framework commands 

╭─ Options ───────────────────────────────────────────────────────────────────╮ 
│ -h, --help Show this message and exit │ 
╰─────────────────────────────────────────────────────────────────────────────╯ 
╭─ Commands ──────────────────────────────────────────────────────────────────╮ 
│ generate-events-config Generate solidity events that EIF will listen to │ 
╰─────────────────────────────────────────────────────────────────────────────╯ 

generate-events-config 

Usage: chr eif generate-events-config [<options>] 

Generate solidity events that EIF will listen to 

Options: 
--abi=<path> Path to a JSON ABI file or a directory of JSON ABI files 
--events=<text> Names of the relevant events (Comma separated) 
--target=<path> Target file to generate events in (defaults to 
"build/eif-events.yaml") 
--format=(XML|YAML) Output file format 
-h, --help Show this message and exit 

The Ethereum Interoperability Framework (EIF) command 
chr eif generate-events-config allows you to generate Solidity
events related to certain actions within the dapp (for example, user registrations, token transfers, or other important
activities). EIF would monitor and use these events to monitor the Ethereum blockchain and trigger specific responses
when these events occur.


### generate

_Official page extract. File: `build_cli_commands_generate.txt`._

On this page 

generate 

Usage: chr generate [OPTIONS] COMMAND [ARGS]... 

Generate client stubs, documentation, and entity relations for a rell project 

╭─ Options ───────────────────────────────────────────────────────────────────╮ 
│ -h, --help Show this message and exit │ 
╰─────────────────────────────────────────────────────────────────────────────╯ 
╭─ Commands ──────────────────────────────────────────────────────────────────╮ 
│ client-stubs Generates client code for a rell dapp │ 
│ graph Generates entity relation graphs in mermaid format │ 
│ docs-site Generate a documentation site for a dapp ontology │ 
╰─────────────────────────────────────────────────────────────────────────────╯ 

generate client-stubs ​ 

Usage: chr generate client-stubs [<options>] 

Generates client code for a rell dapp 

Configuration Properties: 
-s, --settings=<settings> Alternate path for project settings file 

kotlin: 
--package=<text> Name of package 

Options: 
-m, --module=<text> Explicitly set which modules to generate code for. 
Separate modules with ',' 
-d, --target=<path> Directory to generate code in 
--hide-lib-warnings Hide library warnings in build output 
--kotlin, --typescript, --javascript, --python 
Language to generate client for 
-h, --help Show this message and exit 

The client-stubs command ( 
chr generate client-stubs ) generates code that can be used to communicate with the Rell
backend. 

generate graph ​ 

Usage: chr generate graph [<options>] 

Generates entity relation graphs in mermaid format 

Configuration Properties: 
-s, --settings=<settings> Alternate path for project settings file 

Options: 
-m, --module=<text> Explicitly set which modules to generate code for. 
Separate modules with ',' 
-d, --target=<path> Directory to generate code in 
--hide-lib-warnings Hide library warnings in build output 
--mdx Surround with mdx tags 
--entity-relation / --class-diagram 
Presented as entity relation diagram or class diagram 
-h, --help Show this message and exit 

The graph command ( 
chr generate graph ) generates mermaid graphs, helping to visualize the entity relations of a dapp. 

Using the 
--entity-relation / 
--class-diagram options, you can decide whether to present each entity as a class or a
database entity. 

If the graph is intended to be shown in a markdown file, you can use the 
--mdx flag to wrap the generated code in a
code block. This is useful, for example, when including the graph in a docasaurus site or Github Wiki . 

generate docs-site ​ 

Usage: chr generate docs-site [<options>] 

Generate a documentation site for a dapp ontology 

Configuration Properties: 
-s, --settings=<settings> Alternate path for project settings file 

Options: 
-d, --target=<path> Directory to generate code in 
-i, --include=<text> Libs to actively include in the navigation of the 
generated docs site, by default all are excluded. To 
include a library, the full identifier must be 
specified as an example for the library foo, the 
inclusion of it would be lib.foo 
--hide-lib-warnings Hide library warnings in build output 
-h, --help Show this message and exit 

The generate docs-site command ( 
chr generate docs-site ) generates a complete site that documents the API of your dapp. 

Configure the output by adding a 
docs section to 
chromia.yml : 

chromia.yml 

docs : 
title : My Rell Dapp 
footerMessage : © 2024 Copyright MyCompany 
customStyleSheets : 
- my - styles.css 
customAssets : 
- my - logo.png 
additionalContent : 
- my - doc.md 
sourceLink : 
remoteUrl : https : //github.com/my - org/my - repo/blob/main/src 
remoteLineSuffix : "#L" 

For more information on the configuration properties, see project settings file . 

The generated site contains JS code to provide a navigation bar. Although you can open the 
index.html page in your
browser, the best experience is achieved by hosting the site on a web server. 

This can be done using docker: 

docker run -dit --name my-docs-site -p 8080 :80 -v " $PWD " :/usr/local/apache2/htdocs/ httpd:2.4 

Or, if you already have a node-js project: 

npm install http-server 
npx http-server


### help

_Official page extract. File: `build_cli_commands_help.txt`._

help 

Usage: chr help [<options>] 

Show this message and exit 

Options: 
-h, --help Show this message and exit 

The help command ( 
chr help ) shows general help information and lists all available commands.


### library command

_Official page extract. File: `build_cli_commands_library.txt`._

On this page 

library 

Usage: chr library [OPTIONS] COMMAND [ARGS]... 

Manage organizations and libraries in the Chromia library ecosystem 

╭─ Options ───────────────────────────────────────────────────────────────────╮ 
│ -h, --help Show this message and exit │ 
╰─────────────────────────────────────────────────────────────────────────────╯ 
╭─ Commands ──────────────────────────────────────────────────────────────────╮ 
│ install Install library dependencies │ 
│ list List all available libraries │ 
│ view View detailed information about a library │ 
│ versions List all versions of a library │ 
╰─────────────────────────────────────────────────────────────────────────────╯ 

You can use the library command ( 
chr library ) to manage organizations and libraries in the Chromia library ecosystem. 

library install ​ 

Usage: chr library install [<options>] [<library-id>] 

Install library dependencies 

Configuration Properties: 
-cfg, --config=<config> Alternate path for client configuration file 
-s, --settings=<settings> Alternate path for project settings file 

Custom Library chain options: 

Specify a custom library chain (overrides the default mainnet target) 

--url=<text> Url where library-chain is deployed. Ex: testnet, 
localhost, https://custom-network.chromia.dev:7740 
-b, --brid=<value> Brid (hex string) of library-chain 

Options: 
-lib, --library=<text> Name of library(ies) to install from chromia.yml file 
-f, --force Force installation even if RID verification fails. 
This bypasses integrity checks and should only be 
used if you trust the source. Use with caution as it 
may install corrupted or tampered libraries. 
-h, --help Show this message and exit 

Arguments: 
<library-id> ID of the library to install with optional version, e.g. 
'chromia-lib@1.0.0'. if no version is specified the latest 
version will be installed 

The install command ( 
chr library install ) installs library dependencies for your project. 

library list ​ 

Usage: chr library list [<options>] 

List all available libraries 

Configuration Properties: 
-cfg, --config=<config> Alternate path for client configuration file 
-s, --settings=<settings> Alternate path for project settings file 

Custom Library chain options: 

Specify a custom library chain (overrides the default mainnet target) 

--url=<text> Url where library-chain is deployed. Ex: testnet, 
localhost, https://custom-network.chromia.dev:7740 
-b, --brid=<value> Brid (hex string) of library-chain 

Options: 
-l, --limit=<int> Maximum number of libraries to display 
-o, --offset=<int> Number of libraries to skip 
--sort-by=(asc|desc) Sort order for libraries 
-h, --help Show this message and exit 

The list command ( 
chr library list ) shows a list of available libraries. 

library view ​ 

Usage: chr library view [<options>] <library_id> 

View detailed information about a library 

Configuration Properties: 
-cfg, --config=<config> Alternate path for client configuration file 
-s, --settings=<settings> Alternate path for project settings file 

Custom Library chain options: 

Specify a custom library chain (overrides the default mainnet target) 

--url=<text> Url where library-chain is deployed. Ex: testnet, 
localhost, https://custom-network.chromia.dev:7740 
-b, --brid=<value> Brid (hex string) of library-chain 

Options: 
-h, --help Show this message and exit 

Arguments: 
<library_id> ID of the library to view 

The view command ( 
chr library view ) displays detailed information about a specific library. 

library versions ​ 

Usage: chr library versions [<options>] <libraryid> 

List all versions of a library 

Configuration Properties: 
-cfg, --config=<config> Alternate path for client configuration file 
-s, --settings=<settings> Alternate path for project settings file 

Custom Library chain options: 

Specify a custom library chain (overrides the default mainnet target) 

--url=<text> Url where library-chain is deployed. Ex: testnet, 
localhost, https://custom-network.chromia.dev:7740 
-b, --brid=<value> Brid (hex string) of library-chain 

Options: 
-l, --limit=<int> Maximum number of versions to display 
-o, --offset=<int> Number of versions to skip 
-h, --help Show this message and exit 

Arguments: 
<libraryid> ID of the library to list versions for 

The versions command ( 
chr library versions ) shows available versions for a library.


### library guide

_Official page extract. File: `build_cli_library.txt`._

On this page 

Library commands 

The Library commands provide a CLI interface for discovering and installing libraries from the Chromia library registry.
These commands allow developers to browse available Rell libraries and integrate them into their projects. 

info 

The Library Chain is a complete dApp deployed on the Chromia blockchain that hosts the library registry. You can explore
it on the Chromia Explorer 
library list ​ 

List all available libraries in the registry. 

Usage: 

chr library list [ options ] 

Options: 

--limit <number> , 
-l <number> : Maximum number of libraries to display (default: 10) 

--offset <number> , 
-o <number> : Number of libraries to skip (default: 0) 

--sort-by <order> : Sort order ( 
asc or 
desc , default: 
desc ) 

Example: 

chr library list 

library view ​ 

View detailed information about a specific library. 

Usage: 

chr library view < library_id > [ options ] 

Arguments: 

library_id : ID of the library to view 

Example: 

chr library view com.chromia.ft4 

library versions ​ 

List all versions of a specific library. 

Usage: 

chr library versions < library_id > [ options ] 

Arguments: 

library_id : ID of the library to list versions for 

Options: 

--limit <number> , 
-l <number> : Maximum number of versions to display (default: 10) 

--offset <number> , 
-o <number> : Number of versions to skip (default: 0) 

Example: 

chr library versions com.chromia.ft4 

library install ​ 

Install libraries from the registry to your local project. 

The 
library install command reads your 
chromia.yml file and installs all libraries defined in the 
libs section. It
supports both library-chain libraries (published to Chromia's library registry) and external Git libraries. 

Usage: 

chr library install < library_id > [ options ] 

Arguments: 

library_id (optional): Specific library ID to install. If not provided, all configured libraries in 
chromia.yml will be installed. 

Options: 

--force , 
-f : Force installation even if RID verification fails (use with caution) 

warning 

When using the 
--force flag, the RID verification is skipped. This is not recommended as it can lead to security
vulnerabilities. 
Configuration ​ 

Libraries are configured in your 
chromia.yml file using the 
libs section: 

Library-chain libraries ​ 

Published to Chromia's library registry: 

libs : 
com.chromia.ft4 : # Library from Chromia's library-chain registry 
version : 0.0.1 

Configuration options: 

version : Version of the library to install 

External Git libraries ​ 

Hosted in Git repositories: 

libs : 
ft4 : # External library from Git 
registry : https : //bitbucket.org/chromawallet/ft3 - lib 
path : rell/src/lib/ft4 
tagOrBranch : v1.0.0r 
rid : x"FA487D75E63B6B58381F8D71E0700E69BEDEAD3A57D1E6C1A9ABB149FAC9E65F" 
insecure : false 

Configuration options: 

registry : Git repository URL 

path : Path within the repository to the library 

tagOrBranch : Git tag or branch to use 

rid : Expected RID for verification 

insecure : Skip RID verification (not recommended) 

Installation examples ​ 

Install all configured libraries: 

chr library install 

Install a specific library: 

chr library install com.chromia.ft4 

note 

It installs the latest version of the library. It will print message like this: 

add this into chromia.yaml file under libs : 
com.chromia.ft4 
version: 1.0 .0 

you have to add this into chromia.yaml file under libs 

Install a specific version: 

chr library install com.chromia.ft4@1.0.0 

Install latest version: 

chr library install com.chromia.ft4 

Force installation: 

chr library install com.chromia.ft4 --force 

warning 

Use the 
--force flag with caution, as it bypasses RID verification which is an important security measure. 
Using installed libraries ​ 

Once installed, you can import and use libraries in your Rell code. Libraries are imported using their simple name: 

import ft4 ; 

// Use library functions 
val result = ft4 . some_function ( ) ; 

note 

When importing libraries in Rell code, use the library's simple name (e.g., 
ft4 ) rather than the full library ID
(e.g., 
com.chromia.ft4 ). 
Common options ​ 

All library commands inherit common options for connecting to the library-chain: 

Connection options: 

--url <url> : URL to the target node. Only required if library-chain is deployed on a custom network (e.g., 
https://custom-network ). Defaults to Chromia's mainnet if not provided. 

--brid <hex> : Blockchain RID of the library-chain. Only required if library-chain is deployed on a custom network. 

Examples of common usage patterns: 

chr library < command > [ options ]


### multi-signature

_Official page extract. File: `build_cli_commands_multi-signature.txt`._

On this page 

multi-signature 

Usage: chr multi-signature [OPTIONS] COMMAND [ARGS]... 

Handle transactions with need of multiple signers 

╭─ Options ───────────────────────────────────────────────────────────────────╮ 
│ -h, --help Show this message and exit │ 
╰─────────────────────────────────────────────────────────────────────────────╯ 
╭─ Commands ──────────────────────────────────────────────────────────────────╮ 
│ create Creates a new transaction for multi signature and signs it with │ 
│ your key │ 
│ sign Sign a existing transaction with your key │ 
│ send Send a fully signed transaction │ 
│ view View a existing transaction │ 
╰─────────────────────────────────────────────────────────────────────────────╯ 

You can use the multi-signature command ( 
chr multi-signature ) for handling transactions that need multiple signers. 

multi-signature create ​ 

Usage: chr multi-signature create [<options>] <opname> [<args>]... 

Creates a new transaction for multi signature and signs it with your key 

Configuration Properties: 
-cfg, --config=<config> Alternate path for client configuration file 
-s, --settings=<settings> Alternate path for project settings file 

dApp target options: 
-brid, --blockchain-rid=<text> Target Blockchain RID 
--cid=<int> Target Blockchain IID 
--api-url=<text> Target api url 
--mainnet, --testnet Select network, use instead of --api-url 

Deployment: 

Use a configured deployment network target in chromia.yml 

-d, --network=<text> Specify which deployment target to use 
-bc, --blockchain=<text> Name of blockchain in deployment configuration 

FT compatible dapps options: 
--ft-auth Adds ft4.ft_auth operation for FT-compatible dapps 
--ft-account-id=<text> Explicitly specify which account to use 
-id, --auth-descriptor-id=<text> 
Explicitly specify which auth descriptor id to use 

Key pair source: 
--secret=<path> Path to secret file (pubkey/privkey) 
--key-id=<key_id> Key ID of the keypair to use 

Signers, leave out this option to send transaction directly: 
--signer=<value> Public keys of signer (can be repeated) 
--signers=<value> Comma separated list of keys of signers 
--signers-file=<path> Path to file containing public keys of signers, one 
per line 

Options: 
--target=<path> Path where file should be saved 
--timeb-from=<value> Add timeb operation to make transaction fail if 
applied before the given time (UTC). Supported 
formats: yyyy-MM-dd HH:mm, yyyy-MM-dd'T'HH:mm and 
milliseconds since 1970 (unix/epoch time) 
--timeb-at=<value> Add timeb operation to make transaction fail if 
applied after the given time (UTC). Supported formats: 
yyyy-MM-dd HH:mm, yyyy-MM-dd'T'HH:mm and milliseconds 
since 1970 (unix/epoch time) 
--timeb-after=<value> Add timeb operation to make transaction fail if 
applied after the given number of seconds from now. 
-h, --help Show this message and exit 

Arguments: 
<opname> name of the operation to execute. 
<args> arguments to pass to the operation. (integer: 123) (big_integer: 
1234L) (string: foo, "bar") (bytearray: will be encoded using the 
rell notation x"<myByteArray>" and will initially be interpreted as 
a hex-string.) (array: [foo,123]) (dict: 
["key1":value1,"key2":value2]) 

The create command 
chr multi-signature create allows you to create a new transaction for multi signature and signs it
with your key. 

multi-signature sign ​ 

Usage: chr multi-signature sign [<options>] 

Sign a existing transaction with your key 

Configuration Properties: 
-cfg, --config=<config> Alternate path for client configuration file 

Key pair source: 
--secret=<path> Path to secret file (pubkey/privkey) 
--key-id=<key_id> Key ID of the keypair to use 

Options: 
-f, --file=<path> Path to file of transaction 
--target=<path> Path where file should be saved 
--file-name=<text> Override default name of output file 
-h, --help Show this message and exit 

The sign command 
chr multi-signature sign allows you to sign a existing transaction with your key. 

multi-signature send ​ 

Usage: chr multi-signature send [<options>] 

Send a fully signed transaction 

Configuration Properties: 
-cfg, --config=<config> Alternate path for client configuration file 
-s, --settings=<settings> Alternate path for project settings file 

Key pair source: 
--secret=<path> Path to secret file (pubkey/privkey) 
--key-id=<key_id> Key ID of the keypair to use 

dApp target options: 
-brid, --blockchain-rid=<text> Target Blockchain RID 
--cid=<int> Target Blockchain IID 
--api-url=<text> Target api url 
--mainnet, --testnet Select network, use instead of --api-url 

Deployment: 

Use a configured deployment network target in chromia.yml 

-d, --network=<text> Specify which deployment target to use 
-bc, --blockchain=<text> Name of blockchain in deployment configuration 

Options: 
-a, --await / --no-await Wait for transaction to be included in a block 
-f, --file=<path> Path to file of transaction 
-h, --help Show this message and exit 

The send command 
chr multi-signature send allows you to send a fully signed transaction. 

multi-signature view ​ 

Usage: chr multi-signature view [<options>] 

View a existing transaction 

Options: 
-f, --file=<path> Path to file of transaction 
-h, --help Show this message and exit 

The view command 
chr multi-signature view allows you to view a existing transaction.


### node

_Official page extract. File: `build_cli_commands_node.txt`._

On this page 

node 

Usage: chr node [OPTIONS] COMMAND [ARGS]... 

Interact with a test node 

╭─ Options ───────────────────────────────────────────────────────────────────╮ 
│ -h, --help Show this message and exit │ 
╰─────────────────────────────────────────────────────────────────────────────╯ 
╭─ Commands ──────────────────────────────────────────────────────────────────╮ 
│ start Starts a test node │ 
│ update Updates a running test node │ 
╰─────────────────────────────────────────────────────────────────────────────╯ 

You can use the node command ( 
chr node ) to start or update a node with your applications running on it. 

node start ​ 

To start your node, you use ( 
chr node start ). 

Usage: chr node start [<options>] 

Starts a test node 

If a blockchain has already been started on the configured database schema, 
the configuration will be added to the next height such that the node will be 
started with the new config. Use --wipe to wipe the database schema upon 
startup and thus enforce starting the chain from height=0. 

Configuration Properties: 
-s, --settings=<settings> Alternate path for project settings file 

Options: 
-bc, --blockchain-config=<path> 
Manually specify which blockchain-configs to 
run 
--name=<name> Only start specified blockchains (multiple) 
-p=<key=value> Override any property value (usage: -p 
key=value) 
-np, --node-properties=<path> Full path to override node properties file 
--directory-chain-mock Adds a blockchain on ID 0 that responds to the 
cluster management api and anchoring api. Used 
together with integration tests involving 
frontend clients. Can be used with node 
discovery features, ICCF and cross-chain 
transfers using the FT-protocol 
--hide-lib-warnings Hide library warnings in build output 
--sql-log Log sql expressions 
--wipe / --no-wipe If a database should be wiped before startup 
-h, --help Show this message and exit 

By default, it starts each blockchain under the 
blockchains key in your project config file ( 
chromia.yml ). 

If you are in the working directory where the 
chromia.yml presides: 

chr node start 

You can set a path for the Project Settings file or specify a different file: 

chr node start --settings chromia.yml 

If you instead want to start a node from a build, you can refer to the blockchain config file: 

chr node start --blockchain-config build/my_rell_dapp.xml 

You can use the 
--wipe option to reset the database before execution and use the 
-np or 
--node-properties if you
want to override the default node settings. 

You can use the 
--name option to specify which blockchain to start from the blockchains set in 
chromia.yml . If you
have specified multiple blockchains, you can chain them to start multiple blockchains at once. 

#chromia.yml 

blockchains : 
foo : 
module : main 
bar : 
module : main 

Start one blockchain named 
foo : 

chr node start --name foo 

Start two blockchains named 
foo and 
bar : 

chr node start --name foo --name bar 

node update ​ 

To make updates to a local running node, you use ( 
chr node update ): 

Usage: chr node update [<options>] 

Updates a running test node 

Will add a configuration to a block height 2 higher than current height for 
the running blockchain. Make sure this command is executed with exactly the 
same chromia.yml and arguments as was used when starting the node using chr 
node start to make sure configurations are added to the correct chain ids. 

Configuration Properties: 
-s, --settings=<settings> Alternate path for project settings file 

Options: 
-bc, --blockchain-config=<path> 
Manually specify which blockchain-configs to 
run 
--name=<name> Only start specified blockchains (multiple) 
-p=<key=value> Override any property value (usage: -p 
key=value) 
-np, --node-properties=<path> Full path to override node properties file 
--directory-chain-mock Adds a blockchain on ID 0 that responds to the 
cluster management api and anchoring api. Used 
together with integration tests involving 
frontend clients. Can be used with node 
discovery features, ICCF and cross-chain 
transfers using the FT-protocol 
--hide-lib-warnings Hide library warnings in build output 
-n, --preemption=<int> Update the configuration at a height this many 
blocks into the future 
-h, --help Show this message and exit


### query

_Official page extract. File: `build_cli_commands_query.txt`._

On this page 

query 

Usage: chr query [<options>] <queryname> [<args>]... 

Make a query towards a running node 

Examples: 
╭─────────────────────────────────────────────────────────────────────╮ 
│# query primitive_args(arg1: integer, arg2: name, arg3: text, arg4: │ 
│byte_array, arg5: my_enum) │ 
│chr query primitive_args 'arg1=123' 'arg2=Alice' 'arg3="My Neighbor"'│ 
│'arg4=x"AB12"' 'arg5=0' │ 
│# query dict_arg(arg: map<text, integer>) │ 
│chr query dict_arg 'arg=["key": 12]' │ 
│# query map_arg(arg: map<my_enum, text>) │ 
│chr query map_arg 'arg=[[0, "first"],[1, "second"]]' │ 
│# query struct_arg(arg: my_struct) │ 
│chr query struct_arg 'arg=[12, "structs are arrays", x"AB"]' │ 
│ │ 
│# Use -- do avoid additional quotes │ 
│chr query my_query -- arg1=foo arg2=x"AB12" │ 
╰─────────────────────────────────────────────────────────────────────╯ 

Configuration Properties: 
-cfg, --config=<config> Alternate path for client configuration file 
-s, --settings=<settings> Alternate path for project settings file 

dApp target options: 
-brid, --blockchain-rid=<text> Target Blockchain RID 
--cid=<int> Target Blockchain IID 
--api-url=<text> Target api url 
--mainnet, --testnet Select network, use instead of --api-url 

Deployment: 

Use a configured deployment network target in chromia.yml 

-d, --network=<text> Specify which deployment target to use 
-bc, --blockchain=<text> Name of blockchain in deployment configuration 

Options: 
-f, --output-format=(pretty|raw|JSON|XML|YAML) Output format 
-h, --help Show this message and exit 

Arguments: 
<queryname> name of the query to make. 
<args> arguments to pass to the query, passed either as key=value pairs 
or as a single dict. (integer: 123) (big_integer: 1234L) 
(string: foo, "bar") (bytearray: will be encoded using the rell 
notation x"<myByteArray>" and will initially be interpreted as a 
hex-string.) (array: [foo,123]) (dict: 
["key1":value1,"key2":value2]) 

You can use the query command ( 
chr query ) to test and interact with a chain that's either local or deployed without
using a client. You can set a path for the project settings file ( 
chromia.yml )
or specify a different file using 
--settings . If no dApp target or deployment options are used, the command will by
default target a local running node that is started with 
chr node start . 

Local Node ​ 

When you start a node, you can see the Blockchain RID as the standard
output. An example would look like this: 
Blockchain RID: FC17B67D66F6F35A5D8B75ED3F83AE222FB8C8FCA241624F06285150F10C6BAC . If you run the node on a different
URL than the default 
http://localhost:7740 , you can use the 
--api-url in the 
chromia.yml to specify it. 

To run the query 
hello_world specified in the 
main.rell file. 

chr query --blockchain-rid < BlockchainRID > hello_world 

If the query has arguments called 
foo , 
bar and 
baz , then the query would look like the following for the values
17, "hello" and "hello world" (quoting of strings is optional if they don't contain whitespace or special characters). 

chr query --blockchain-rid < BlockchainRID > hello_world foo = 17 bar = hello 'baz="hello world"' 

Deployed ​ 

For deployed chains, you can use the same commands as in a local environment, but you need to target the chain
explicitly. You can do this by either: 

Using a configured deployment target in your 
chromia.yml together with deployment options 

chr query --network deployment_configuration --blockchain my_rell_dapp hello_world 

Specifying 
--api-url or 
--mainnet / 
--testnet together with the BRID of the dApp you want to query. 

chr query --mainnet -brid 2D17B27D4F69E0A91B0CA39AF53EFA9B82CDAF698EF906A67C71C266983EEB7A hello_world


### repl

_Official page extract. File: `build_cli_commands_repl.txt`._

repl 

Usage: chr repl [<options>] [<script>] [<args>]... 

REPL is used to create a language shell for Rell that takes single user 
inputs, executes them, and returns the result. Inside the repl you can create 
local variables and execute Rell commands, it can be attached to a Rell 
module to be able to inspect a dapp state and execute dapp functionalities. 

Run query commands: To be able to run queries in the shell, a user must have 
a module defined from the start of the repl command in which the query is 
defined. Queries that do not depend on entities can be executed without a 
database connection, and queries that depend on an entity must then have a 
database connection defined. 

Run Operations commands: When a operation is executed from the repl shell, 
the database connection and a module needs to be defined from the start of 
the repl command, to be able to execute an operation it needs added to a 
transaction. This can be done by wrapping it with a test transaction like 
this: rell.test.tx(<your operation>..).run() 

Rell scripts: You can specify a file with Rell statements which will be read 
and executed (specifying - will read from standard input). Command line 
arguments can be specified and will be available as args: list<text>. This 
can not be combined with the -c option. Support for Rell scripts is 
experimental and may be changed or removed at any time. 

Configuration Properties: 
-s, --settings=<settings> Alternate path for project settings file 

Module source: 
-m, --module=<module> Name of module 
-bc, --blockchain=<value> Name of the blockchain from which to load the 
module and moduleArgs 

Options: 
--sql-log Log sql expressions 
--history-file=<path> Save command history to this file 
--use-db If a session towards the configured database should 
be established 
-c, --command=<command> Execute a single command 
-r, --raw-output Will print large object line by line and strings 
without quotes (deprecated) 
-f, --output-format=(pretty|raw|JSON|XML|YAML) 
Output format 
-d, --duration Print duration of the execution 
-h, --help Show this message and exit 

Arguments: 
<script> Script file 
<args> Arguments to script 

You can use the repl command ( 
chr repl ) to run specific Rell methods in the shell, which can be suitable for
troubleshooting.


### seeder command

_Official page extract. File: `build_cli_commands_seeder.txt`._

On this page 

seeder 

Usage: chr seeder [OPTIONS] COMMAND [ARGS]... 

Generate fake data for a local database 

╭─ Options ───────────────────────────────────────────────────────────────────╮ 
│ -h, --help Show this message and exit │ 
╰─────────────────────────────────────────────────────────────────────────────╯ 
╭─ Commands ──────────────────────────────────────────────────────────────────╮ 
│ init Create initial seeder configuration for blockchains │ 
│ generate Generate Rell blockchain seeder module │ 
╰─────────────────────────────────────────────────────────────────────────────╯ 

You can use the seeder command ( 
chr seeder ) to generate mock data for a local database. This is an early-stage feature
and may be subject to change. For more information on how to use the seeder command take a look at the seeder documentation 

seeder init ​ 

Usage: chr seeder init [<options>] 

Create initial seeder configuration for blockchains 

Configuration Properties: 
-s, --settings=<settings> Alternate path for project settings file 

Options: 
-bc, --blockchain=<blockchain> 
Blockchains to generate configuration for (defaults to all). 
-h, --help Show this message and exit 

The init command 
chr seeder init allows you to create initial seeder configuration for blockchains. 

seeder generate ​ 

Usage: chr seeder generate [<options>] 

Generate Rell blockchain seeder module 

Configuration Properties: 
-s, --settings=<settings> Alternate path for project settings file 

Options: 
--alternative-config-folder=<path> 
Alternative path to the root seeder configuration folder 
-bc, --blockchain=<blockchain> 
Blockchains to generate seeders for (defaults to all) 
-h, --help Show this message and exit 

The generate command 
chr seeder generate allows you to generate Rell blockchain seeder module.


### Seeder overview

_Official page extract. File: `build_cli_Seeder.txt`._

On this page 

Seeder command 

The Seeder command is a powerful CLI tool that helps you manage and populate your Chromia blockchain with initial data.
It's particularly useful during development and testing phases when you need to set up your blockchain with predefined
data structures and content. 

The seeder allows you to: 

Create and manage seed data for your blockchain 

Populate your blockchain with test data 

Basic usage ​ 

To use the seeder command, you can run: 

` chr seeder init ` - Initialize a new seeder configuration 
` chr seeder generate ` - Generate the Rell seeder modules


### configurable generators

_Official page extract. File: `build_cli_Seeder_configurable-generators.txt`._

Configurable generators 

The following generators support additional configuration options: 

Generator ID 
Configuration options 
Description 
Default value 

range 

min , 
max 
Controls the minimum and maximum values. Supported for Rell types: integer, decimal, big_integer 
(min, max) are required 

byte_array 

size 
Controls the size of the generated byte array 
32 

text 

min , 
max 
Controls the minimum and maximum length of the generated string 
(1, 10) 

predefined 

distribution 
values 
Controls the order picking values (SEQUENTIAL/RANDOM) List of values to be used 
SEQUENTIAL Empty array 
Example configurations: 

<entity_name> : 
count : 10 
attributes : 
# Integer with range 
<attribute_name_one> : 
generator : text 
min : 18 
max : 65 
<attribute_name_two> : 
generator : byte_array 
size : 20 
<attribute_name_three> : 
generator : predefineds 
values : [ 1 , 2 , 3 ] 
distribution : RANDOM


### test

_Official page extract. File: `build_cli_commands_test.txt`._

test 

Usage: chr test [<options>] 

Run tests in working directory 

Configuration Properties: 
-s, --settings=<settings> Alternate path for project settings file 

Options: 
-bc, --blockchain=<blockchain> 
Run tests for specified blockchain(s). 
-m, --modules=<modules> Run tests in this module(s) only. Must be the 
part of the specified modules or its 
submodules. Comma delimited, will default to 
all modules either under each selected 
blockchain or test 
--file=<path> Run tests in one file 
--tests=<> test method pattern 
--use-db / --no-db If a session towards the configured database 
should be established 
--test-report Generate JUnit XML test reports 
--test-report-dir=<path> JUnit XML test reports directory (defaults to 
"build/reports") 
--fail-on-error[=true|false] Sets test execution to stop on error and 
override any "failOnError" settings for tests 
that are in the scope being executed 
-ts, --timestamp Timestamp on logs 
--hide-lib-warnings Hide library warnings in build output 
-h, --help Show this message and exit 

You can use the test command ( 
chr test ) to run the tests specified in the 
test key in the project settings file ( 
chromia.yml ). 

Runs all the tests under the test attribute in 
./chromia.yml : 

chr test 

Runs all the tests under the test attribute of a selected Project Settings file: 

chr test --settings < path/chromia.yml > 

Runs a specific test module: 

chr test --modules test.data_test 

Runs test under test attribute for a specific blockchain: 

chr test --blockchain < blockchain name > 

Runs all the tests based on the specified text filter: 

chr test --tests my_filter


### tools

_Official page extract. File: `build_cli_commands_tools.txt`._

On this page 

tools 

Usage: chr tools [OPTIONS] COMMAND [ARGS]... 

Miscellaneous tools 

╭─ Options ───────────────────────────────────────────────────────────────────╮ 
│ -h, --help Show this message and exit │ 
╰─────────────────────────────────────────────────────────────────────────────╯ 
╭─ Commands ──────────────────────────────────────────────────────────────────╮ 
│ gtv Decode and convert GTV data │ 
│ validate-config Validate a chromia config file │ 
│ lib-model Generate a library model │ 
╰─────────────────────────────────────────────────────────────────────────────╯ 

You can use the tools command ( 
chr tools ) provides a set of miscellaneous tools for various tasks related to Chromia. 

tools gtv ​ 

Usage: chr tools gtv [<options>] 

Decode and convert GTV data 

Use --hex option or pipe binary GTV data to the command. 

Examples: 
╭──────────────────────────────────────────────────────────────────────╮ 
│chr gtv --hex A41A3018300A0C0161A2050C03464F4F300A0C0162A2050C03424152│ 
│chr gtv --output-format yaml < data.gtv │ 
╰──────────────────────────────────────────────────────────────────────╯ 

Options: 
--hex=<hex> Hex encoded GTV data 
-f, --output-format=(pretty|raw|JSON|XML|YAML) 
Output format 
--hash=<version> Calculate Merkle hash of the GTV data 
-h, --help Show this message and exit 

The gtv command 
chr tools gtv allows you to decode and convert GTV data. 

tools validate-config ​ 

Usage: chr tools validate-config [<options>] 

Validate a chromia config file 

Options: 
-f, --file=<path> 
-h, --help Show this message and exit 

The validate-config command 
chr tools validate-config allows you to validate a Chromia config file. 

tools lib-model ​ 

Usage: chr tools lib-model [<options>] 

Generate a library model 

Options: 
--name=<text> Name of the library 
-s, --library-source=<path> 
--registry=<text> Git reference 
--tag-or-branch=<text> Tag or branch the library is published on 
--insecure=true|false Allow insecure connections 
-h, --help Show this message and exit 

The lib-model command 
chr tools lib-model allows you to generate a library model.


### tx

_Official page extract. File: `build_cli_commands_tx.txt`._

On this page 

tx 

Usage: chr tx [<options>] <opname> [<args>]... 

Make a transaction towards a node. 

Supports both specifying the target node using url and brid/id or from a 
deployment which is specified in the chromia.yml This will post the 
transaction asynchronously unless --await is specified, in which it will wait 
until transaction has been included in a block. 

FT4 compatibility: To make a transaction towards a dapp that uses 
ft-authentication, use the --ft-auth flag. This only works if the signer 
keypair is connected to a ft-account with the correct authentication rules. 
Use --ft-account-id to explicitly state which account to use if keypair is 
connected to more than one account. Node: This does not work with 
evm-authentication 

ICCF: To verify a transaction using ICCF, specify the tx-rid to verify using 
--iccf-tx and which chain id the transaction was processed. The command will 
both construct and insert a iccf_proof operation prior to the user operation 
but will also add the the transaction as a gtx_transaction as first argument 
to the user operation. 

Examples: 
╭───────────────────────────────────────────────────────────────────────╮ 
│# operation primitive_args(arg1: integer, arg2: name, arg3: text, arg4:│ 
│byte_array, arg5: my_enum) │ 
│chr tx primitive_args 123 Alice "My Neighbor" 'x"AB12"' 0 │ 
│# operation dict_arg(arg: map<text, integer>) │ 
│chr tx dict_arg '["key": 12]' │ 
│# operation map_arg(arg: map<my_enum, text>) │ 
│chr tx map_arg '[[0, "first"],[1, "second"]]' │ 
│# operation struct_arg(arg: my_struct) │ 
│chr tx struct_arg '[12, "structs are arrays", x"AB"]' │ 
╰───────────────────────────────────────────────────────────────────────╯ 

Configuration Properties: 
-cfg, --config=<config> Alternate path for client configuration file 
-s, --settings=<settings> Alternate path for project settings file 

Key pair source: 
--secret=<path> Path to secret file (pubkey/privkey) 
--key-id=<key_id> Key ID of the keypair to use 

dApp target options: 
-brid, --blockchain-rid=<text> Target Blockchain RID 
--cid=<int> Target Blockchain IID 
--api-url=<text> Target api url 
--mainnet, --testnet Select network, use instead of --api-url 

Deployment: 

Use a configured deployment network target in chromia.yml 

-d, --network=<text> Specify which deployment target to use 
-bc, --blockchain=<text> Name of blockchain in deployment configuration 

FT compatible dapps options: 
--ft-auth Adds ft4.ft_auth operation for FT-compatible dapps 
--ft-account-id=<text> Explicitly specify which account to use 
--evm-auth=<address> Adds ft4.evm_auth operation for FT-compatible dapps 
--ft-register-account Adds ft4.register_account operation. To be used in 
combination with an account creation strategy. 

ICCF options: 
--iccf-tx=<text> Constructs a ICCF-proof for this tx-rid and 
inserts iccf_proof operation to the transaction. 
This will also add the tx as a gtx_transaction as 
argument to the operation 
--iccf-source=<value> Blockchain RID for the chain which the tx to be 
proven has taken place 
--source-api-url=<text> Source api url (default target api url) 
--iccf-force-intra-network Force usage of intra-network ICCF proof 
--iccf-arg-pos=<int> Which argument position to insert the 
gtx_transaction in the operation 

Options: 
-a, --await / --no-await Wait for transaction to be included in a block 
-nop Adds a nop to the transaction 
--timeb-at=<value> Add timeb operation to make transaction fail if 
applied after the given time (UTC). Supported 
formats: yyyy-MM-dd HH:mm, yyyy-MM-dd'T'HH:mm and 
milliseconds since 1970 (unix/epoch time) 
--timeb-after=<value> Add timeb operation to make transaction fail if 
applied after the given number of seconds from now. 
-h, --help Show this message and exit 

Arguments: 
<opname> Name of the operation to execute. 
<args> Types and their format as arguments to pass to the operation. 
(integer: 123) (big_integer: 1234L) (decimal: "1.2") (text: foo, 
"bar") (byte_array: 'x"<myByteArray>"') (list: '["foo",123]') 
(map<text, ...>: '["text_key":value1,"text_key2":value2]') 
(map<non_text_key_type, ...>: '[[non_text_key, value1], 
[non_text_key, value2]]') (struct: '["foo"]') 

You can use the tx (transaction) command ( 
chr tx ) in the same way as the query command, but you need to sign it with
your key pair. It can interact with either a local (Node) or deployed chain without the use of a client. 

You can set a path for the project settings file ( 
chromia.yml ) or specify a
different file using 
--settings . 

For deployed chains, you can use the same transaction commands as in a local environment, but you need to target the
chain explicitly. 

You can do this by either: 

Using a configured deployment target in your 
chromia.yml together with 
--network and 
--blockchain options, or 

Specifying 
--api-url or 
--mainnet / 
--testnet together with the BRID of the dApp you want to interact with. 

Local Node ​ 

You can see the Blockchain RID of the local node in the sout. An example
would look like the following: 
Blockchain RID: FC17B67D66F6F35A5D8B75ED3F83AE222FB8C8FCA241624F06285150F10C6BAC If you
are running the node on a different URL than the default 
http://localhost:7740 , you can use the 
--api-url option to
specify it. 

Perform a transaction with the operation 
set_name which takes one argument of type text 

chr tx set_name "my_name" 

If you want to pass a dictionary as an argument, you have to single quote it to avoid incorrect shell parsing. 

chr tx operation_with_dict_argument '["key1":value1,"key2":value2]' 

Deployed ​ 

Towards a deployed chain, you can do the same commands but need to to target the chain explicitly. You can do this by
either: 

Using a configured deployment target in your 
chromia.yml together with deployment options 

chr tx --network deployment_configuration --blockchain my_rell_dapp set_name "my_name" 

Specifying 
--api-url or 
--mainnet / 
--testnet together with the BRID of the dApp you want to interact with. 

chr tx --mainnet --blockchain-rid 2D17B27D4F69E0A91B0CA39AF53EFA9B82CDAF698EF906A67C71C266983EEB7A set_name "my_name"


### version

_Official page extract. File: `build_cli_commands_version.txt`._

version 

Usage: chr version [<options>] 

Show the version and exit 

Options: 
-h, --help Show this message and exit 

The version command ( 
chr version ) displays version information for the chr CLI tool and its components.


### generating-doc-site

_Official page extract. File: `build_cli_generating-doc-site.txt`._

On this page 

Generate documentation 

This topic explains how to generate a documentation site from your Rell source code and how to customize it to match
your project's branding and requirements. 

Overview ​ 

The Chromia CLI provides a built-in command to generate a documentation site from your Rell source code. 

API documentation automatically generated from Rell source code together with corresponding RellDoc comments in your code. 

Custom content pages that you can add 

Customizable styling and branding 

Generate a documentation site ​ 

To generate a documentation site, run the following command in your project directory: 

chr generate docs-site 

You can view the generated site by opening 
build/docs-site/index.html in your web browser. 

The generated site contains JS code to provide a navigation bar. Although you can open the index.html page in your
browser, the best experience is achieved by hosting the site on a web server which will enable the navigation. 

warning 

To view the navigation bar, the generated site needs to run on a web server, see Navigation bar not showing . 
Configure document generation ​ 

Document generation is configured in the 
docs section of your 
chromia.yml file. Here's an example configuration: 

docs : 
title : "My Rell Dapp" # Title of the documentation site 
footerMessage : "© 2024 Copyright MyCompany" # Custom footer message 
customAssets : # Custom assets to include 
- docs - assets/logo.png 
- docs - assets/favicon.ico 
customStyleSheets : # Custom CSS stylesheets 
- docs - assets/custom - styles.css 
additionalContent : # Additional markdown content 
- docs - assets/getting - started.md 
- docs - assets/tutorials/basic - usage.md 
additionalModules : 
- test.doc_module 
- lib.some_lib 

Add additional content ​ 

You can add custom content pages to your documentation site by creating Markdown ( 
.md ) files and referencing them in
the 
additionalContent section of your configuration. 

Example custom content file ​ 

// < title > must match the title property configured in chromia.yml at docs:title 

# Dapp <title> 

// Content 

// < module-1 > should be replaced with the name of targeted module 

# Module <module-1> 

// Content 

## Sub-titles are allowed 

// Content 

# Module <module-2> 

// Content 

Customize styling ​ 

You can customize the look and feel of your documentation site by creating CSS files and referencing them in the 
customStyleSheets section of your configuration. 

Example custom stylesheet ​ 

:root { 
--primary-color : #4a86e8 ; 
--secondary-color : #34a853 ; 
--text-color : #333333 ; 
--background-color : #e0c7f6 ; 
--code-background : #a2dbf7 ; 
} 

.navbar { 
background-color : var ( --primary-color ) ; 
} 

.sidebar { 
border-right : 1 px solid #e16565 ; 
} 

Add custom assets ​ 

You can include custom assets such as logos, favicons, and other images by placing them in a directory (e.g., 
docs-assets ) and referencing them in the 
customAssets section of your configuration. If you want to replace any of
the default images or logos, this can be achieved by simply adding your own image file and naming it the same as the one
you would like to replace. 

Add additional modules ​ 

You can specify additional modules to be included during documentation generation using the 
additionalModules configuration in your 
chromia.yml file. This is particularly useful for: 

Including test modules that contain usage examples 

Adding library modules that aren't part of the main module but should be documented 

Ensuring comprehensive API documentation by including supporting modules 

Configuration example ​ 

docs : 
# Other configuration options... 
additionalModules : 
- test.doc_module 
- test.examples 
- lib.common_utils 

Usage notes ​ 

Each module should be specified with its full path (e.g., 
test.module_name or 
lib.module_name ) 

Test modules can be valuable for documentation as they often contain practical usage examples 

You can organize your test modules specifically for documentation purposes (e.g., creating a 
test.doc_examples module) 

Troubleshooting ​ 

Navigation bar not showing ​ 

If you cannot view the navigation bar and you haven't added any custom styling that would cause this, it is most likely
because you are not running the site on a web server. Two quick ways of running it on a server would be: 

Using Docker 

docker run -dit --name my-docs-site -p 8080 :80 -v " $PWD " :/usr/local/apache2/htdocs/ httpd:2.4 

Using Node.js 

npm install http-server 
npx http-server 

Custom styling not applied ​ 

If your custom styles are not being applied: 

Check that the CSS file is correctly referenced in your 
chromia.yml 

Verify that the CSS selectors match the elements in the generated HTML 

Inspect the generated HTML to understand the structure and class names


### CLI release notes (full)

_Official page extract. File: `build_cli_cli-release-notes.txt`._

On this page 

Chromia CLI release notes 

The release notes lists all new features, resolved issues, and known issues of Chromia CLI in chronological order. 

Chromia CLI 0.30.0 ​ 

Released on February 27, 2026. 

Category 
Description 

Added 
- 
chr deployment create now writes the deployment result back to 
chromia.yml and prints the changes made. - 
chr build : Added 
--skip-lib-check flag to skip library verification during build. - Enum Change Detection on Deployment Update - Schema comparison during 
chr deployment update now detects enum changes in addition to entity changes. - Warns on enum value additions, removals, and reorderings. - Blocks deployment on dangerous enum changes (e.g., reordered or removed values) until approved. - Version Bumps - rell 0.15.2 - postchain 3.49.2 - postchain-chromia 3.39.3 - eif 0.32.0 - chromia-cli-tools 0.10.0 

Fixed 
- 
chr install : 
--brid and 
--url options now correctly override the target defined in 
chromia.yml . - Fixed duplicate progress rendering when installing libraries. 
Chromia CLI 0.29.10 ​ 

Released on January 20, 2026. 

Category 
Description 

Added 
- Version Bumps - - Bump EIF to 0.27.3 
Chromia CLI 0.29.9 ​ 

Released on January 19, 2026. 

Category 
Description 

Added 
- Version Bumps - - postchain.version 3.47.6 - - postchain-chromia.version3.38.1 
Chromia CLI 0.29.8 ​ 

Released on January 16, 2026. 

Category 
Description 

Added 
- Schema validation for Enum types - Dynamically load provider urls instead of hardcoding them - Remove 
url and 
brid requirement in chromia.yml for deploymentActionCommands - Version Bumps - chromia-cli-tools 0.8.14 

Fixed 
- fixed a bug in 
chr install where offset was not being used correctly 
Chromia CLI 0.29.7 ​ 

Released on December 19, 2025. 

Category 
Description 

Fixed 
- 
chr install now exits gracefully when no libraries are found in chromia.yml instead of throwing an error 
Chromia CLI 0.29.6 ​ 

Released on December 18, 2025. 

Category 
Description 

Added 
- Library install: Introduced real-time visual feedback for library downloads and installations. - Version Bumps - chromia-cli-tools 0.8.12 
Chromia CLI 0.29.5 ​ 

Released on December 10, 2025. 

Category 
Description 

Added 
- Library install: Options --url and --brid can now be used to override default mainnet option when installing for - Version Bumps - rell 0.15.0 
Chromia CLI 0.29.4 ​ 

Released on December 01, 2025. 

Category 
Description 

Fixed 
- Resolved minor issue with library install command 
Chromia CLI 0.29.3 ​ 

Released on December 01, 2025. 

Category 
Description 

Fixed 
- Improved error message for 
chr node start when no blockchain configurations is found 
Chromia CLI 0.29.2 ​ 

Released on November 26, 2025. 

Category 
Description 

Fixed 
- Fix a bug in 
chr build when building a project with Chromia libraries - Downgrade Rell version to 0.14.15 due to a bug in 0.14.16 
Chromia CLI 0.29.1 ​ 

Released on November 24, 2025. 

Category 
Description 

Added 
- 
chr install will update 
libs tag in chromia.yml - Add --signer and --signers option to multi-signature create command - Rell 0.14.16 
Chromia CLI 0.29.0 ​ 

Released on November 11, 2025. 

Category 
Description 

Added 
- 
chr generate client-stubs command can now generate stubs for python - 
chr multi-signature create command accepts no initial signer - 
chr multi-signature create command accepts signer file to be written without properties - Version bumps: - chromia-cli.tools 0.8.1 
Chromia CLI 0.28.3 ​ 

Released on October 24, 2025. 

Category 
Description 

Added 
- Add 
deployment container configuration command for toggling logs on container - Reorganize container command while keeping aliases for backward compatibility: - deployment pause-container -> deployment container pause - deployment resume-container -> deployment container resume - Version Bumps - directory-chain 1.101.15 - chromia-cli-tools 0.7.5 

Fixed 
- Fix 
signers not set in deployTagetOption 
Chromia CLI 0.28.2 ​ 

Released on October 14, 2025. 

Category 
Description 

Fixed 
- Adds error handling when no urls found for dApp brid from directory chain - Fix NPE when generating stubs of queries/functions that return unnamed tuples 
Chromia CLI 0.28.1 ​ 

Released on October 08, 2025. 

Category 
Description 

Fixed 
- Predefined urls for create statement 
Chromia CLI 0.28.0 ​ 

Released on September 22, 2025. 

Category 
Description 

Added 
- Automatic blockchain configuration inference - - When only one blockchain is defined, the configuration is automatically inferred from 
<deployments.network> in 
chromia.yml . - Default configurations for Chromia networks: - - Introduced 
--mainnet and 
--testnet flags as alternatives to 
--api-url . - - When using 
--network mainnet or 
--network testnet , default Chromia configurations are applied automatically. - Version Bumps - rell 0.14.15 - postchain 3.44.0 - postchain-client 3.38.0 

Fixed 
- Will not try to load keys by 
key.id if 
--secret option is used in commands 
Chromia CLI 0.27.11 ​ 

Released on September 22, 2025. 

Category 
Description 

Added 
- Option to specify argument position to insert ICCF transaction in 
chr tx command - Add confirmation check when deleting a blockchain from a container in 
chr deployment remove with 
-y flag to auto-confirm - 
--verify-rid option in 
chr library deploy to verify if the version to be deployed introduces code changes - Version Bumps - postchain 3.42.4 - postchain-client 3.37.1 - chromia-cli-tools 0.6.5 

Fixed 
- Enhanced error output for chr tx --ft-auth failures to include more context. - Adds validation to check if the 'BRID' provided is valid before creating directoryChain client 
Chromia CLI 0.27.10 ​ 

Released on September 08, 2025. 

Category 
Description 

Added 
- Add 
--hide-lib-warnings option to node commands - Version Bumps - chromia-cli-tools 0.6.4 
Chromia CLI 0.27.9 ​ 

Released on September 03, 2025. 

Category 
Description 

Added 
- 
--file option to 
chr test to run all tests in a single Rell file - 
chr code check command to check Rell code for compilation errors - 
chr install supports library chains - 
chr library list command to list available libraries on library chain - 
chr library view command to view details about specific library on library chain 

Fixed 
- Improve how test command deals with --modules option 
Chromia CLI 0.27.8 ​ 

Released on August 29, 2025. 

Category 
Description 

Added 
- Adds marker file after dependency installation to enable IDE reference resolution. - Print progress while posting transaction and awaiting confirmation - Support adding timeb operation in tx and multi-signature create commands - Version Bumps - chromia-cli-tools 0.6.2 

Fixed 
- Inconsistent library warning count and other minor bug fixes 
Chromia CLI 0.27.7 ​ 

Released on July 03, 2025. 

Category 
Description 

Added 
- Adds iccf source url option to 
chr tx command to support iccf tx across different clusters - Adds 
--devcontainer flag to 
chr create-rell-dapp command to create a project with devcontainer environment - Version Bumps - chromia-cli-tools 0.5.16 

Fixed 
- NPE when message is null in deploy command - Update testnet BRID for library commands 
Chromia CLI 0.27.6 ​ 

Released on June 23, 2025. 

Category 
Description 

Added 
- Version Bumps - rell 0.14.12 - postchain 3.40.0 - postchain-client 3.34.0 - chromia-cli-tools 0.5.15 - rell-toolbox 0.8.4 - rell-codegen 0.16.8 - rell-dokka 0.2.18 - directory-chain 1.95.0 - eif 0.16.3 

Fixed 
- NPE when message is null in deploy command - Update testnet BRID for library commands 
Chromia CLI 0.27.5 ​ 

Released on June 23, 2025. 

Category 
Description 

Fixed 
- Remove validation of 
registry in chromia model json schema and other small fixes 
Chromia CLI 0.27.4 ​ 

Released on June 20, 2025. 

Category 
Description 

Fixed 
- Fixed Windows-specific issue with GitCloner in 
chr install and other small improvements 
Chromia CLI 0.27.3 ​ 

Released on June 19, 2025. 

Category 
Description 

Fixed 
- Fix 
chr install on Windows. Uniform library RID calculation 
Chromia CLI 0.27.2 ​ 

Released on June 11, 2025. 

Category 
Description 

Added 
- Version Bumps - rell-codegen 0.16.7 - chromia-cli-tools 0.5.8 

Fixed 
- Fix 
chr deployment proposal rename to target correct blockchain-rid - Refactor FT auth to use new functionality in ft4-client - Update jgit to mitigate a vulnerability that affects it 
Chromia CLI 0.27.1 ​ 

Released on May 28, 2025. 

Category 
Description 

Added 
- 
--hide-lib-warnings option to commands that involve compilation - 
--get-pubkey option to 
keygen command to retrieve pubkey from local/global config or keyid - Version Bumps - rell 0.14.11 - postchain 3.37.0 - postchain-client 3.30.0 - chromia-cli-tools 0.5.6 - rell-toolbox 0.8.3 - rell-codegen 0.16.6 - rell-dokka 0.2.17 - directory-chain 1.94.1 - eif 0.16.0 - postchain-chromia 3.30.2 

Fixed 
- 
multi-signature : Make reading of saved transactions more robust and show an error message if it fails. 
Chromia CLI 0.27.0 ​ 

Released on May 15, 2025. 

Category 
Description 

Added 
- Seeder tool for generating realistic data for testing Dapps. - Additional modules support for documentation generation. - Pause and resume container 
Chromia CLI 0.26.4 ​ 

Released on May 12, 2025. 

Category 
Description 

Added 
- 
chr node start : More production like ICMF reception error handling - Version Bumps - rell 0.14.10 - postchain 3.34.0 - postchain-client 3.29.2 - chromia-cli-tools 0.5.2 - rell-toolbox 0.7.1 - rell-codegen 0.16.4 - rell-dokka 0.2.12 

Fixed 
- Displaying wrong blockchain config merkel hash 
Chromia CLI 0.26.2 ​ 

Released on April 23, 2025. 

Category 
Description 

Fixed 
- 
chr query : Accept plain RID and decimal as parameter to query command 
Chromia CLI 0.26.1 ​ 

Released on April 22, 2025. 

Category 
Description 

Added 
- 
chr kegen : Set file permissions on private key and mnemonic files - 
chr tx : Improve output from tx command - Improve parsing of GTV expressions passed to commands. Support keywords like 
null , 
true , 
false - Improve logging. Include 
com.chromia package 

Fixed 
- 
chr node start Display correct blockchain BRID when using old configurations - 
chr deployment create Correct indentation for print statement with multiple successful deployed chains 
Chromia CLI 0.26.0 ​ 

Released on April 04, 2025. 

Category 
Description 

Added 
- 
chr build : Added 
--hide-lib-warnings flag to suppress library warnings - 
chr tx : Added support for registering FT4 accounts with the 
--ft-register-account option - 
chr tx : Added support for sending intra-network ICCF proofs with the 
--iccf-force-intra-network option 

Breaking Changes 
- When using 
chr tx with any ICCF options against a local node (started with 
chr node start ), the node must now be running with the directory chain mock enabled via 
--directory-chain-mock 
Chromia CLI 0.25.0 ​ 

Released on April 02, 2025. 

Category 
Description 

Added 
- Integrated merkle hash calculator v2 - Revised multi signature workflow to new merkle hash calculator - Implemented an automatic fetch for brid in commands where it is required and not defined in 
chromia.yml - Version Bumps - rell 0.14.8 - postchain 3.29.0 - postchain-client 3.27.0 - chromia-cli-tools 0.4.15 - rell-toolbox 0.6.0 - rell-codegen 0.16.3 - rell-dokka 0.2.11 - eif 0.13.1 - directory-chain 1.83.0 
Chromia CLI 0.24.3 ​ 

Released on March 06, 2025. 

Category 
Description 

Added 
- Adds --evm-auth option to 
chr deployment voterset add-dapp-provider command to support signing transaction with metamask - Adds the ability to filter on system or user sql queries when logging sql queries while running tests - ChromiaClientConfig.apiUrls now take list of urls instead of single url to sync with chromia-cli-tools - Version Bumps - postchain-client 3.26.2 - chromia-cli-tools 0.4.7 
Chromia CLI 0.24.2 ​ 

Released on March 06, 2025. 

Category 
Description 

Added 
- Chromia-cli-tools version 0.4.2 
Chromia CLI 0.24.1 ​ 

Released on March 04, 2025. 

Category 
Description 

Added 
- Streamline usage of key IDs - Option 
--key-id was added to all commands that required keys to be used, making it easier to manage keys. - Calculate Merkle hash of the blockchain configuration - Option 
--hash was added to fetch config command to calculate the Merkle hash of the blockchain configuration. 
Chromia CLI 0.24.0 ​ 

Released on February 25, 2025. 

Category 
Description 

Added 
- Utility Commands - New command 
chr tools validate-config --file <path> to validate and parse Chromia configuration files. - New command 
chr tools lib-model to help library developers get the library configuration, including the RID. - Enhanced ICCF Support - Added support for ICCF in directory chain mock chain. - Code Formatting Improvements - Added trailing argument to 
chr code format command to be able to add file patterns on what files it should be acted on the filter is a glob matcher. - Test Output Enhancement - New output format for SQL logs when running 
chr test --sql-log , utilizing new API from Rell. - Version Bumps - Rell version 0.14.7. 

Fixed 
- New version of Rell includes bugfix of inconsistent use of hash calculator between operations code and test code - Renamed option 
pubkey to 
account-id in DeployInspectLeaseCommand. - Added support for Postchain older than 3.23.2. - Updated help text for voterset threshold option to clarify how the integer transforms into percentage. 
Chromia CLI 0.23.0 ​ 

Released on February 04, 2025. 

Category 
Description 

Added 
- Entity/Object Change Detection on Deployment Update - Automatically detects schema changes between the currently deployed dApp and the new one, requiring approval for risky modifications. - Warns on attribute/object/entity removal. - Shows new attributes and indexes. - Blocks deployment on dangerous changes until approved. - New Linter Rule - 
rule_outer_join_cartesian_product=true in 
.rell_lint file. - Warns about outer joins without join conditions, which result in a Cartesian product. - ICMF Receivers - Support all kinds of ICMF receivers for the in-memory ICMF mock when running 
chr node start . - Version Bumps - Rell version 0.14.6. - Postchain 3.27.3. 
Chromia CLI 0.22.4 ​ 

Released on January 27, 2025. 

Category 
Description 

Added 
- Automatic Browser Window Closing - When signing transactions using 
--evm-auth , the browser window will now automatically close upon completion. - REPL Command Enhancements - Stack traces will now be retained on errors. - Added an option to print the duration of executed scripts. - Improved Linting with Filters - The 
chr lint command now supports a new multi argument of glob strings that acts as a filter for what files or dirs should be linted. 

Fixed 
- Non-Interactive Terminals - Fixed an issue where users were prompted for input in non-interactive terminals. - REPL Command Fixes - Comments in scripts are now properly handled when executing the REPL command. - REPL can now be executed in workspaces with a faulty Chromia configuration. - Help Text Improvements - Updated help text for the 
chr tx command with clearer instructions on sending different transaction types. 
Chromia CLI 0.22.2 ​ 

Released on December 19, 2024. 

Category 
Description 

Added 
- Support FT4 EVM auth through web browser - Added support for FT4 EVM authentication via web browser in the 
chr tx command. 
Chromia CLI 0.22.0 ​ 

Released on December 11, 2024. 

Category 
Description 

Added 
- FT4 Auth Descriptor Selector - Introduces an interactive terminal picker for users with multiple auth descriptors, allowing direct selection of the desired descriptor. - Enhanced Test Result Printing - Integrates Rell's test result logger to provide more detailed and clearer failure messages. - Experimental chr repl scripts - Extends REPL functionality to accept Rell files as input and support command-line arguments for scripts. 

Fixed 
- Codegen - TypeScript client stubs now fully comply with Gtv type conversions, utilizing postchain-client's RawGtv type instead of generic 'any'. - Aligned client stub generation with TypeScript patterns, implementing QueryObject and Operation building approach in place of the previous transaction builder pattern. 
Chromia CLI 0.21.4 ​ 

Released on November 18, 2024. 

Category 
Description 

Added 
- New template project - Adds a new template project to the 
chr create-rell-dapp --template asset-management that contains a simple frontend to connect a wallet and mint tokens on a Rell backend. - ICCF source update - 
chr tx --iccf-source now takes a BRID instead of a chain ID, simplifying the flow and making it easier for users. - Help text improvement - Help text for 
chr deployment inspect --definitions now shows available definition types that the user can filter on. - Voterset command enhancements - You can now specify either the container ID or specific voterset name in the 
voterset info command. - Receive the voterset name from container ID using the 
voterset list command. - Multi-signature transaction - It is now possible to rename the output file when signing a transaction file for multi-signature. 

Fixed 
- Running 
chr keygen with 
--dry option now works again. - No longer asking for input when commands are executed in a non-interactive terminal. - Resolved issue where the parent directory couldn't be found from the working directory in the 
multi-signature sign command. - Fixed rendering of complex types so it respects indents and newlines without escaping in the 
multi-signature view command. - Removed creation of aliases to make auto-completion suggestions work again. Note that you would need to redo the auto-completion setup: Auto-completion setup . 
Chromia CLI 0.21.3 ​ 

Released on November 06, 2024. 

Category 
Description 

Added 
- Improved error messages - Static web building - Enables building static web content without Rell sources. - GTV data tool - New 
chr tools gtv command added for GTV data decoding and conversion. 

Fixed 
- Static web hosting support in 
chr node . - Prevents invalid XML characters in blockchain configuration. - Requires explicit file type and name selection for key storage to avoid accidental key overwrites. - Removes default name of generated file, requiring users to specify a file name or key-id during recovery to prevent accidental key overwriting. - Corrected help text formatting. 
Chromia CLI 0.21.0 ​ 

Released on October 23, 2024. 

Category 
Description 

Added 
- Multi-signature transaction support - 
chr multi-signature create for creating multi-signature transactions with specified signers and signature initialization. - 
chr multi-signature sign enables users to add signatures with customizable keypair options. - 
chr multi-signature send allows submission of fully signed transactions. - 
chr multi-signature view to display multi-signature transaction details. - Static web content packaging - Static web content can be packaged into blockchain configurations for direct Chromia node serving (requires Postchain 3.21+). - Java 21 dependency - Updated package managers now include a Java 21 dependency for 
chr execution. Users can control the Java version via the 
RELL_JAVA environment variable. 

Fixed 
- Exit status code is now 1 instead of 0 when the project settings file is not found. - Prevents escaping of non-ASCII and regular Unicode characters in 
query command raw output. - Formatter fixes - Fixed extra space after the first parenthesis in 
at expressions. - Doc comments are no longer lifted onto the same line as the previous definition when lacking additional newline. 
Chromia CLI 0.20.14 ​ 

Released on September 26, 2024. 

Category 
Description 

Added 
- Client stub documentation - Rell docs comments are now transformed into JS/TS/Kotlin docs comments when generating client stubs. - Test flag - A new 
--timestamp flag for tests allows including timestamps in terminal output for execution duration tracking. - Deployment renaming - New command 
chr deployment proposal rename creates a proposal to change the name of a deployed blockchain. 

Fixed 
- Fixed an issue where whitespace was incorrectly added after the first and before the last parenthesis in a multiline 
"where" expression. - Updated Chromia model to disallow hyphens in blockchain names, ensuring compatibility with directory chain specification. 
Chromia CLI 0.20.13 ​ 

Released on September 11, 2024. 

Category 
Description 

Added 
- YAML anchoring - Anchored values in the config file now respect the original type when included in attributes. - Recursively anchored values in the config file now respect the original type when included in attributes. - Formatter improvements - Rell files created with 
chr create-rell-dapp are now formatted according to default settings. 

Fixed 
- Removed unsupported SQL modules from the 
chromia.yml schema. - Formatter fixes - Fixed an issue where the formatter added an extra newline after a Rell docs comment when followed by an annotation. - Resolved an issue where whitespace was incorrectly added after the first and before the last parenthesis in a multiline 
"what" expression. 
Chromia CLI 0.20.12 ​ 

Released on August 30, 2024. 

Category 
Description 

Added 
- Java 21 upgrade - Upgraded to Java 21, which is now required for 
chr . - Rell version update - Updated to Rell 0.14.1. - Documentation generation - Excludes all libraries from navigation pages by default when generating docs with 
chr generate docs-site . To include a library, use 
--include=lib.<name of lib> . - Custom GTX modules - Allows custom GTX modules in blockchain config to be deployed. - New command: 
chr deployment voterset add-dapp-provider - Enables users with the Dapp Provider role to add others to the role within the network. 

Fixed 
- Resolved an issue displaying null bridges after failed deployments. - Plugins and package upgrades - Updated several plugins and packages: - Dokka plugin 0.2.7 - Rell Maven plugin 0.12.3 - Rell Gradle plugin 0.3.3 - Codegen 0.14.2 - Java 21 and Rell 0.14.1 upgrades applied across plugins. 
Chromia CLI 0.20.9 ​ 

Released on August 20, 2024. 

Category 
Description 

Added 
- Voterset and proposal functions - Adds two functions (experimental) for interacting with votersets and proposals for deployed dapps: - 
chr deployment voterset to manage your own votersets and view others' votersets. - 
chr deployment proposal to manage proposals within votersets accessible to your pubkey. - Rell compile version - Updated the default Rell compile version to 0.13.14, aligning with mainnet. Custom target versions can be specified in 
chromia.yml under 
compile:rellVersion . 
Chromia CLI 0.20.8 ​ 

Released on August 12, 2024. 

Category 
Description 

Fixed 
- Updated to the latest formatter to fix syntax errors. 
Chromia CLI 0.20.7 ​ 

Released on August 12, 2024. 

Category 
Description 

Added 
- Code lint and format updates - 
chr code lint and 
chr code format commands now ignore libraries defined in 
chromia.yml by default. To run these commands on external libraries, use the 
--source-dir option to specify the library path. 
Chromia CLI 0.20.6 ​ 

Released on August 01, 2024. 

Category 
Description 

Added 
- Rell linter and formatter - 
chr code lint analyzes Rell code for potential issues and coding style violations, configurable via 
.rell_lint file. - 
chr code format automatically formats Rell code, configurable via 
.rell_format file. 

Fixed 
- Documentation generator now includes doc comments of child attributes on the parent page. - Improved formatter handling of newlines after single-line comments. - Enhanced error message when attempting to create a deployment for an already defined chain in 
chromia.yml . 
Chromia CLI 0.20.4 ​ 

Released on July 11, 2024. 

Category 
Description 

Fixed 
- Fixed an issue in docs generation where the 
@see tag in user Rell docs caused duplicate text. 
Chromia CLI 0.20.3 ​ 

Released on July 08, 2024. 

Category 
Description 

Added 
- User-defined docs - 
chr generate docs-site now includes user-defined docs comments in the generated documentation site. 

Fixed 
- 
chr tx now waits for transaction confirmation by default. Use 
--no-await to skip waiting. 
Chromia CLI 0.20.2 ​ 

Released on June 18, 2024. 

Category 
Description 

Added 
- Example usage documentation - Added documentation for 
query and 
tx commands, including examples of how to use complex argument types. 

Fixed 
- Fixed code generation for boolean Rell types, now correctly typed as 
number in TypeScript queries and operations (input and return). - Code generation now exits with a non-zero exit code if unsuccessful. 
Chromia CLI 0.20.1 ​ 

Released on June 05, 2024. 

Category 
Description 

Added 
- New config property - Added 
add_primary_key_to_header to config, defaulting to 
true (impacts 
brid for chains). - JSON output support - 
deployment inspect and 
deployment info commands now support JSON output with the new argument --output-format=(table |JSON), defaulting to table format. Automatically defaults to JSON when output is piped. 

Fixed 
- Renamed 
--url flag to 
--api-url for 
deployment info/inspect commands for consistency. - Improved 
chr install speed by up to 15%. 
Chromia CLI 0.20.0 ​ 

Released on May 30, 2024. 

Category 
Description 

Added 
- Enhanced output formatting - Added pretty print for 
chr query results. - New 
--output-format flag for 
chr repl and 
chr query , supporting JSON, XML, and raw output. - 
chr repl command now supports input piping. - Configuration validation - 
chr deployment update now signs configuration validation requests to restrict execution to blockchain owners. - Documentation generation - 
chr generate docs-site now includes source links in generated documentation. - Blockchain RID configuration - Blockchain RIDs can now be configured as either strings or byte arrays in the deployment model. 

Fixed 
- Fixed issue where 
deployment update with multiple chains only validated the first chain in the input. - Clarified JSON schema descriptions for Chromia model and corrected typos. - Updated deployment commands to use the target network to define default chains if no blockchains are specified. - Improved TypeScript stubs in 
chr generate client-stubs to better align with 
postchain-client , reducing linter warnings. 
Chromia CLI 0.19.1 ​ 

Released on May 16, 2024. 

Category 
Description 

Fixed 
- Resolved a bug in 
chr node start where library chains caused a null pointer exception (NPE). Library chains are now filtered out from 
chr node start and 
chr node update . 
Chromia CLI 0.19.0 ​ 

Released on May 13, 2024. 

Category 
Description 

Added 
- Library support - New YAML field ( 
blockchains::type ) now supports configuration as either 
blockchain or 
library . - 
chr create-rell-dapp --template plain-library creates a library skeleton. - Libraries can now be compiled by configuring a blockchain as a library, e.g., 
blockchains.my_lib.type: library . - Library structure requirements - Libraries must be located in 
lib/<name> and match the blockchain name in the YAML. - Root module ( 
lib.<name> ) must exist, and everything in 
lib/<name> is considered part of the library. - Configuration setting update - 
revolt_when_should_build_block is set to 
true by default to align with upcoming directory chain requirements. - Dependency updates - Updated dependencies: - Rell 0.13.12 - Postchain 3.15.19 - Postchain-Chromia 3.15.19 

Fixed 
- This version impacts previously calculated blockchain RIDs due to updated configuration. 
Chromia CLI 0.18.2 ​ 

Released on May 02, 2024. 

Category 
Description 

Added 
- Internal improvements - Added exposed function to easily print configuration properties (internal use). 

Fixed 
- Properly reads from the 
key.id property when loading configuration. 
Chromia CLI 0.18.1 ​ 

Released on May 02, 2024. 

Category 
Description 

Added 
- Key management enhancements - New 
--key-id option for 
Keygen command allows users to store keys in 
.chromia folder in 
$SYS_HOME as 
"myKeyId" and 
"myKeyId.pubkey" (default: 
chromia_key ). - Config property 
key.id can now be set to easily switch between keys. - Renamed 
--save option to 
--file in 
Keygen command. - For deployment and transaction commands, 
--secret option takes precedence over 
key.id in configuration. 

Breaking Changes 
- Deprecated key recovery removal - Deprecated key recovery for keys generated pre-CLI version 0.15.0 removed. Users should test recovery of their keypair with the mnemonic. - Users needing old key pair recovery are recommended to install an older version of Chromia CLI. 
Chromia CLI 0.17.4 ​ 

Released on April 16, 2024. 

Category 
Description 

Added 
- Updated Rell version to 0.13.11 . 
Chromia CLI 0.17.3 ​ 

Released on April 12, 2024. 

Category 
Description 

Added 
- Auto-configuration - Automatically sets 
mininterblockinterval based on configuration rules in directory chain. 

Fixed 
- Updates 
brid property under 
icmf to 
bc-rid for ICMF runtime compatibility. - Improved Dokka generation for namespaces and mount names, where namespaces are treated as modules and mount names appear in signatures when 
@mount is used. - Reduced Chromia CLI disk size by 50%. 
Chromia CLI 0.17.2 ​ 

Released on April 10, 2024. 

Category 
Description 

Added 
- Experimental Windows support - Added native support for Windows users via 
scoop ( scoop.sh ). - Command: 
scoop bucket add chromia https://gitlab.com/chromaway/core-tools/scoop-chromia/ - Command: 
scoop install chr - Compatibility and updates - 
--ft-auth updated for compatibility with FT4 version 0.4.0+. - Updated Rell code generation to 0.13.5, fixing NPE for partially named tuples in query return types. - Updated EIF to 0.4.1 to resolve EIF configuration validation issue. - New 
strictGtvConversion option in 
chromia.yml under 
compile property to configure strict GTV conversion. 

Fixed 
- Fixed issue with 
repl on Mac. - Fixed deployment bug where Rell versions higher than the target cluster supported were deployed. 
Chromia CLI 0.17.1 ​ 

Released on April 05, 2024. 

Category 
Description 

Added 
- Updated Rell version to 0.13.10 . - Added 
--fail-on-error flag for the 
test command, overriding local 
test:failOnError configuration. 

Fixed 
- Updated Dokka plugin to 0.1.2 to fix broken links for anonymous functions. - Updated code generation to 0.13.4 to remove shadowing warnings in Kotlin stubs. - Set 
compile:quiet default to 
false for more verbose build messages in the terminal. Users can set it to 
true to suppress warnings. 
Chromia CLI 0.17.0 ​ 

Released on March 25, 2024. 

Category 
Description 

Added 
- Split client stubs and graph generation - 
chr generate client-stubs for client stubs and 
chr generate graph for mermaid graphs. - Deprecated 
chr generate-client-stubs in favor of separate commands. - Documentation generation - New command: 
chr generate docs-site , enabling static API reference pages for dApps. - Aggregation and expression syntax - Added annotations for 
list , 
set , and 
map aggregation on 
at-expressions . - New 
at-expression join syntax. - Dependency updates - Updated to Postchain 3.15.5 and Rell 0.13.9. 
Chromia CLI 0.16.3 ​ 

Released on March 15, 2024. 

Category 
Description 

Added 
- Added support for big integer values in 
chromia.yml with a capital "L" suffix (e.g., 
1234L ). - Reintroduced schema validation for configuration files. See the full schema here . 

Fixed 
- Verified that the Rell version in 
chromia.yml matches the highest supported version for the target cluster during deployment updates. - Updated 
jgit dependency to address a security vulnerability. 
Chromia CLI 0.16.2 ​ 

Released on March 12, 2024. 

Category 
Description 

Added 
- Temporarily removed support for big integer values in 
chromia.yml . - Rolled back the new parsing module to the previous version that works with anchors and references in YAML files. 
Chromia CLI 0.16.1 ​ 

Released on March 11, 2024. 

Category 
Description 

Added 
- Added support for big integer values in 
chromia.yml with a capital "L" suffix (e.g., 
1234L ). - Introduced economic chain support with the "Get Lease Information" command. This feature allows users to get lease information by container ID or public key (currently hidden but available under 
chr deployment lease-info ). - Updated the directory-chain version to 1.35.0. - Updated Postchain to version 3.15.3. 

Fixed 
- Standardized the message output for 
chr start when all blockchains have started, now showing "Node is initialized". - Fixed stacktrace errors in YAML parsing and improved error messages when configuration files have issues. 
Chromia CLI 0.16.0 ​ 

Released on February 05, 2024. 

Category 
Description 

Added 
- Added 
chr node start --directory-chain-mock , which provides a directory chain mock for use in integration tests and manual testing of frontend clients. - Enabled cross-chain transfers with FT4 and client-side usage of ICCF via node discovery features. 
Chromia CLI 0.15.3 ​ 

Released on January 29, 2024. 

Category 
Description 

Added 
- Added a prefilled 
.gitignore file to all templates created by 
create-rell-dapp . - Introduced a directory to wrap the generated code. The directory is named after the project name, or 
my-rell-dapp by default if no project name is provided. 

Fixed 
- Replaced all references to "hello" as the default blockchain name with 
my_rell_dapp if no project name is provided. 
Chromia CLI 0.15.2 ​ 

Released on January 25, 2024. 

Category 
Description 

Fixed 
- Added support for FT version 0.2.+ when using 
--ft-auth in 
chr tx . 
Chromia CLI 0.15.1 ​ 

Released on January 18, 2024. 

Category 
Description 

Fixed 
- Fixed bug where 
chr test failed for a blockchain test when ICMF was configured. 
Chromia CLI 0.15.0 ​ 

Released on January 17, 2024. 

Category 
Description 

Added 
- Added support for ICCF when running a test node with 
chr node start . - Enabled unit tests with ICCF using 
chr test . - Introduced support for sending ICCF proofs using 
--iccf-tx and 
--iccf-source in 
chr tx . (Note: ICCF proof operations are not verified in the test framework). - Improved argument parsing for 
chr tx/query , now supporting nested structures (e.g., dicts are encoded as 
[key: value] ). 

Fixed 
- Disabled git progress monitor when running 
chr install non-interactively (e.g., in CI). - Fixed 
start script to work for Alpine Linux and Busybox Docker images (version 0.14.3). - Reverted to using BIP for key generation in 
Keygen (version 0.14.3). - Fixed issue where global config overrides command-line input for 
--cid in 
chr tx/query (version 0.14.3). 
Chromia CLI 0.14.2 ​ 

Released on December 18, 2023. 

Category 
Description 

Added 
- Postchain 3.14.14, postchain-chromia 3.14.8, directory 1.30.0, postchain-client 3.12.1. - Added Chromia.yml validation schema to the repo. 

Fixed 
- Fixed issue with the 
CHR_LOG_LEVEL environment variable to properly set the log level for 
chr node start . - Fixed concurrency issue where messages were lost due to a concurrency problem when using ICMF with a test node. 
Chromia CLI 0.14.1 ​ 

Released on December 06, 2023. 

Category 
Description 

Fixed 
- Reverted the explicit choice of test scope ( 
-bc or no 
-bc ), so now 
chr test runs all test modules and all blockchains by default. 
Chromia CLI 0.14.0 ​ 

Released on December 05, 2023. 

Category 
Description 

Added 
- Added ICMF support for 
chr node start (EXPERIMENTAL). ( Note : Unprocessed messages will be lost during node restart, and the process may crash with an 
OutOfMemoryException if too many messages are sent. This is for testing, not production use.) - Enabled compression of files for networks running the Management chain during 
chr deployment create/update . - Added support for running tests on a selected module using 
--module for blockchain tests in 
chr test . - Increased robustness of 
chr tx for the 
--ft-auth flag. 
Chromia CLI 0.13.4 ​ 

Released on November 28, 2023. 

Category 
Description 

Fixed 
- Fixed JavaScript typo in 
generate-client-stubs . 
Chromia CLI 0.13.3 ​ 

Released on November 28, 2023. 

Category 
Description 

Added 
- Updated 
Keygen to conform to BIP39 and BIP32 standards. 
Chromia CLI 0.13.2 ​ 

Released on November 15, 2023. 

Category 
Description 

Added 
- Added Rell 0.13.5 release notes. - Shows unit test duration. - 
REPL now uses the GTV output format. 

Fixed 
- Exits with code 1 when a query/transaction fails in 
chr repl . - Prints an info message when a blockchain is successfully removed from a container. 
Chromia CLI 0.13.0 and 0.13.1 ​ 

Released on November 08, 2023. 

Category 
Description 

Added 
- Reads API URL and BRID from 
.chromia/config file. - Added the 
-c option to 
chr repl command to allow execution of a single command. - New command: 
chr deployment remove to remove deployed blockchain from a container. - Template flag for 
chr create-rell-dapp with options: 
Minimal , 
Plain , 
Plain-Multi . - Codegen 0.12.0: Added option to generate mermaid entity relation diagrams. 

Fixed 
- Fixed issue where 
chr repl did not print intro text when using the 
-c flag (bug from 0.13.0). - Fixed 
chr repl to exit with status code 1 when it fails with the 
-c flag (bug from 0.13.0). - Fixed default log level to be set as 
info instead of 
debug (macOS-specific bug).


## 4. Database source extracts

Pages: https://docs.chromia.com/build/database/overview and https://docs.chromia.com/build/database/getting-started (WebFetch in this crawl).

Overview: relational blockchain; Rell typed models; PostgreSQL ACID plus Postchain consensus; indexing, foreign keys, JSON/BLOB. Sidebar names Architecture, Scaling models, Deployment.

Getting started checklist: (1) install CLI and sample templates; (2) launch a local provider set with `chromia start` and inspect provisioned PostgreSQL — note `chromia start` is NOT listed on the CLI command-reference index; documented local start is `chr node start`; (3) deploy schema with Rell compiler, seed fixtures, query from a client SDK. Local components: provider nodes, PostgreSQL, Explorer.

## 5. Token Chain source extracts

### developer-token-proposal

_Official page extract. File: `build_token-chain_developer-token-proposal.txt`._

On this page 

Developer token proposal and bridge setup 

Developers and project teams can propose new FT4 tokens and optional bridges on the Token Chain. The process includes
fee payment, parameter configuration, and (optionally) bridge deployment. 

1. Proposing a new token ​ 

Anyone can submit a token proposal. A listing fee is required upfront and refunded if the proposal is rejected. By
default, the fee distribution is: 

25% burned to reduce overall supply and help maintain token value over time. 

25% to Chromia Foundation to support ecosystem development. 

50% directed to the project's resource pool for ongoing ecosystem incentives. 

Current fees 

The current upfront fee is 100 CHR for token proposals and 100 CHR for bridge proposals.
These fees may change over time - you can verify the current amounts using the 
get_token_chain_constants query (see
step 1 below). 
Your proposal should include: 

Token name : This is the name of the token. 

Token symbol : This is the symbol of the token. 

Token decimals : The number of decimal places to use for the token. 

Token icon : A URL pointing to an image file. 

Minting policies : Policies governing who can mint new tokens, how many and how often. Full format of policy can be found in the reference documentation . 

Account creation blockchains : Blockchains RIDs allowed to create accounts. 

note 

Burning a portion of the fee creates deflationary pressure on the governance token, aligning long-term incentives. This
will be enabled in a future release. 
Steps: ​ 

(Optional) Check current constants to verify the listing fee and other parameters: 

chr query --blockchain-rid ${TOKEN_CHAIN_RID} get_token_chain_constants 

Submit proposal (replace placeholders): 

chr tx --evm-auth ${EVM_WALLET_ADDRESS} --blockchain-rid ${TOKEN_CHAIN_RID} \ 
propose_token ${NAME} ${SYMBOL} ${DECIMALS} ${ICON_URL} \ 
${MINTING_POLICY} ${ACCOUNT_CREATION_BRIDS} 

Verify proposal status: 

chr query --blockchain-rid ${TOKEN_CHAIN_RID} get_proposals_by_proposer proposer = ${YOUR_ACCOUNT_ID} 

After approval, fetch your asset ID: 

chr query --blockchain-rid ${TOKEN_CHAIN_RID} \ 
ft4.get_assets_by_name name = ${YOUR_TOKEN_NAME} page_size = null page_cursor = null 

2. Proposing a bridge ​ 

If you don't have the 
EVM_TRANSACTION_SUBMITTER_CHAIN_RID , retrieve it with: 

chr query --blockchain-rid ${DIRECTORY_CHAIN_RID} get_evm_transaction_submitter_chain_rid 

Retrieve existing bridges: 

chr query --blockchain-rid ${EVM_TRANSACTION_SUBMITTER_CHAIN_RID} get_all_bridges 

Identify the Token Chain's validator contract in the response. 

Define your 
bridge_configuration in RELL format: 

struct bridge_configuration { 
network_id : integer ; 
bridge_contract : byte_array ; 
token_contract : byte_array ; 
eif . hbridge . bridge_mode ; 
use_snapshots : boolean ; 
skip_to_height : integer ; 
} 

Submit bridge proposal using the Chromia CLI (see the Deploy the bridge guide ): 

chr tx --evm-auth ${EVM_WALLET_ADDRESS} --blockchain-rid ${TOKEN_CHAIN_RID} \ 
propose_token_bridge ${ASSET_ID} ${BRIDGE_CONFIGURATIONS} 

Token minting ​ 

To mint tokens (if authorized): 

chr tx --evm-auth ${EVM_WALLET_ADDRESS} --blockchain-rid ${TOKEN_CHAIN_RID} \ 
mint_token ${ASSET_ID} ${AMOUNT} 

For full reference on on-chain functions and ICCF operations, see the 
ras_token_iccf documentation.


### architecture token-chain

_Official page extract. File: `get-started_about_architecture_chains_token-chain.txt`._

On this page 

Token Chain 

Overview ​ 

The Token Chain is a dedicated Chromia-based blockchain designed to streamline token management, account creation, and
cross-chain asset bridging. 

Key benefits ​ 

Simplified bridging setup : the Token Chain automates setup on the Chromia side, streamlining the process. Future
updates aim to automate the EVM side as well for greater efficiency. 

Enhanced trust and security : listing tokens on the Token Chain increases their credibility and provides a secure
environment. 

A secure hub for tokens : the Token Chain serves as a trusted system chain. Tokens can be moved back to the Token
Chain for security during updates or changes on a dapp chain. 

Streamlined onboarding : users of multiple projects on the Token Chain already have an account for bridging,
simplifying access to new projects. 

Greater flexibility : tokens are independent of any single dapp chain. If dissatisfaction arises with a dapp's
development, tokens can be transferred to a new forked version that aligns better with user preferences. 

Architecture ​ 

Account creation strategy ​ 

Each project maintains a pool of CHR on the Token Chain specifically for creating new accounts. You can choose: 

Open pool: anyone can request an account by paying a nominal fee. 

Minimum amount strategy: require a minimum CHR deposit per new account to prevent spam. 

Account creation on behalf of projects is authorized via ICCF and requires a list of blockchain RIDs for the chains
allowed to create accounts. Fees for account creation are sent to a Chromia Foundation–managed account, and foundation
members can redistribute these funds back to the economy chain or allocate them to the project's resource pool. 

Token minting policies ​ 

The built-in minting capability in FT4 allows you to use its before and after hooks to enforce minting policies. Token
proposals include: 

Maximum supply (optional) 

Authorized minters 

Minting interval and amount 

Accumulative vs. fixed-rate modes 

Option to specify rate and maximum supply per minter 

The full format of the policy can be found in the reference documentation . 

Repository ​ 

The Token Chain is open source and available on GitLab: 

Repository: https://gitlab.com/chromaway/core/directory-chain/-/tree/dev/src/token_chain


User account creation (WebFetch): fund by transferring CHR from Economy Chain to the Token Chain address; or from another Token Chain account; or a cross-chain transfer (small fee). Create via Vault UI or:

    chr tx --blockchain-rid ${ECONOMY_CHAIN_RID} transfer ${YOUR_TOKEN_CHAIN_ACCOUNT_ID} ${AMOUNT}

## 6. Vector search source extracts

### vector-search overview

_Official page extract. File: `build_vector-search_overview.txt`._

On this page 

Vector and Search overview 

Chromia combines PostgreSQL extensions, embedded vector stores, and on-chain access control to let you mix transactional
and AI-centric workloads. This section introduces the primitives that power semantic search, recommendations, and hybrid
filters. 

Capabilities at a glance ​ 

Native support for the 
pgvector extension with deterministic replication across providers. 

Full-text indexes that let you scope search queries to tenants or application chains. 

Deterministic execution for ranking functions so the same query returns the same ordered results across replicas. 

Concepts to understand ​ 

Concept 
Description 

Embeddings 
Store high-dimensional vectors per account, message, or object. 

Hybrid search 
Combine vector similarity with structured filters from the relational schema. 

Access control 
Use Rell to guard both writes and read access to sensitive embeddings. 
Ready to build? Continue with the sample workloads .


### sample workloads

_Official page extract. File: `build_vector-search_sample-workloads.txt`._

On this page 

Sample workloads 

Recommendation feed ​ 

Store embeddings for each piece of content in a dedicated table. 

Use the 
pgvector 
cosine_distance operator to rank candidates per user. 

Persist the recommendation in a cache table so you can audit what was shown to each user. 

AI-assisted search ​ 

Use a lightweight embedding model (such as 
text-embedding-3-small ) off-chain. 

Push vectors through the Filehub API or any backend worker. 

Combine similarity search with full-text indexing to keep results relevant and deterministic. 

RAG pipelines ​ 

Connect your knowledge base by: 

Ingesting documents into Filehub (for binary data) and referencing them from the database tables. 

Chunking the documents and storing embeddings per chunk. 

Serving RAG answers through Postchain REST endpoints guarded by ACL logic in Rell. 

Continue experimenting by mapping these steps to your domain-specific schema or by prototyping directly in the CLI
cookbook templates.


### Vector DB extension

_Official page extract. File: `ecosystem_extensions_vector-db.txt`._

On this page 

Vector DB 

Chromia’s Vector DB extension allows for the efficient storage and querying of complex multi-dimensional data in a
decentralized approach. It integrates relational database principles to support high-performance indexing and similarity
searches, making it ideal for AI-driven applications such as recommendation systems, natural language processing, and
image recognition. 

You can easily use Chromia’s Rell language and the Chromia CLI to define vector-based schemas, execute operations and
queries, and perform similarity searches. The integration with Chromia’s decentralized network ensures high
availability, scalability, and tamper-proof data management. This combination of blockchain technology, relational
indexing, and vector search opens up new possibilities for decentralized AI, gaming, and large-scale data analytics. 

Leasing a container ​ 

If you are unfamiliar with the process of leasing a container, follow these steps . 

info 

Ensure that the 
Vector DB extension is selected when leasing the container. 
Configuring blockchain in the dapp ​ 

When leasing your container, ensure that you select the Vector DB extension . Configure your blockchain to use
operations and queries to start utilizing this extension. Below is a sample configuration: 

chromia.yml 

blockchains : 
my_chain : 
module : my_chain_module 
config : 
gtx : 
modules : 
- "net.postchain.gtx.extensions.vectordb.VectorDbGTXModule" 
vector_db_extension : 
my_vector_collection : # Name of the collection. Multiple collections can be defined. 
dimensions : 768 # Required - set number of dimensions to use 
index : hnsw_cosine # Optional - available distance indices: hnsw_cosine, hnsw_l1, hnsw_l2, hnsw_ip (default: hnsw_cosine) 
query_max_vectors : 10 # Optional — max results returned per query (default: 10) 
store_batch_size : 300 # Optional — number of vectors stored per internal batch (default: 300) 

Vectors are stored and grouped by 
collections and 
contexts . A 
collection is defined in the blockchain
configuration to separate different types of vectors (e.g. dimensions, index, etc.). A 
context is used to separate
vectors within a 
collection , and is set by the dApp. 

A dApp can also dynamically manage collections. More information about this can be found in the repository documentation . 

info 

dimensions must match the length of the vectors you store — for example, 384, 768, or 1024 when using text embeddings. 
Integrating with Rell ​ 

Integrating the Vector DB Extension library into your Rell project is optional but recommended. To do this, add it to
your configuration as follows: 

chromia.yml 

libs : 
com.chromia.vector_db : 
version : 2.2.0 # Set to version you want to use 

Available versions can be found by running 
chr library versions com.chromia.vector_db . 

Run: 

chr install 

Minimal implementation example ​ 

Here is a simple dapp to store and remove vectors in the defined collection 
my_vector_collection : 

import lib . vector_db . * ; 

operation add_vector ( context : integer , vector : text , id : integer ) { 
store_vector ( "my_vector_collection" , context , vector , id ) ; 
} 

operation delete_vector ( context : integer , id : integer ) { 
delete_vector ( "my_vector_collection" , context , id ) ; 
} 

Deployment to Testnet ​ 

Deployment steps. BRID for Testnet Directory Chain can be found in the explorer . 

Expected output on successful deployment: 

Deployment of blockchain vector_example was successful 
Add the following to your project settings file : 
deployments : 
testnet : 
chains : 
my_chain : x"CEC6A318C873 ... 0A32C85429706" # you will get your own BRID 

Add the deployed chain into 
chromia.yml : 

deployments : 
testnet : # Deployment Target name 
brid : x"6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92" # Blockchain RID for Testnet Directory Chain 
url : https : //node0.testnet.chromia.com # Target URL for one of the nodes in Testnet 
container : 4d7890243fe710 ... 08c724700cbd385ecd17d6f # Replace with your container ID (Example - container: 15ddfcb25dcb43577ab311fe78aedab14fda25757c72a787420454728fb80304) 
chains : 
my_chain : x"CEC6A318C873 ... 0A32C85429706" # Replace with the actual BRID after the dapp gets deployed. It can be found in the terminal during deployment. 

Save the 
BRID from deployment into environment variables. 

vector_brid = CEC6A318C873B3013DB9476C084BEDE0EA3D03D5C686A2FACD50A32C85429706 

Save the 
URL of the test node into environment variables. 

url = https://node0.testnet.chromia.com 

note 

The following operation examples are based on the 
vector_example application from the Vector DB extension repository . You can reference this example
to see the complete implementation. 
Operation examples: ​ 

chr tx -brid $vector_brid --api-url $url add_message hej "[1.0, 2.0, 3.0]" 
chr tx -brid $vector_brid --api-url $url add_message hello "[1.0, 2.5, 3.0]" 
chr tx -brid $vector_brid --api-url $url add_message hei "[1.0, 2.0, 3.1]" 
chr tx -brid $vector_brid --api-url $url add_message "guten tag" "[1.0, 1.5, 3.5]" 

Querying vectors ​ 

The extension introduces a query function named 
query_closest_objects , which allows you to search for vectors. It
returns the closest vectors based on cosine similarity: 

Name 
Type 
Required 
Default 
Description 

collection 
text 
true 

Collection used by the dapp. Use different collections to separate different type of vectors. 

context 
integer 
true 

Context used by the dapp. Use different contexts to separate unrelated vectors. Use 
-1 to disable and query all vectors in all contexts. 

q_vector 
text 
true 

The query vector as a string (e.g. 
"[1.0, 2.0, 3.0]" ) 

max_distance 
decimal 
true 

Maximum allowed distance between the query and stored vectors 

query_max_vectors 
integer 
false 
10 
Maximum number of results to return 

query_template 
text or dict 
false 
– 
Apply a Rell query to enrich/filter/transform the results (see examples below) 

tip 

Use different 
context values to keep vector sets isolated — for example, one for product descriptions, and one for
support tickets. This allows you to run similarity searches independently across each domain. 
Query examples ​ 

Plain query with no 
query_template ​ 

chr query -brid $vector_brid --api-url $url query_closest_objects collection = my_vector_collection context = 0 q_vector = "[1.0, 2.0, 3.0]" max_distance = 1.0 max_vectors = 2 

[ 
{ 
"distance" : "0" , 
"id" : 1 , 
"context" : 0 , 
} , 
{ 
"distance" : "0.0001212999220387978" , 
"id" : 3 , 
"context" : 0 , 
} 
] 

Using a 
query_template to return the text messages ​ 

chr query -brid $vector_brid --api-url $url query_closest_objects collection = my_vector_collection context = 0 q_vector = "[1.0, 2.5, 3.0]" max_distance = 1.0 max_vectors = 2 'query_template=["type":"get_messages"]' 

[ 
"hello" , 
"hej" 
] 

Using a 
query_template to return distance and text ​ 

chr query -brid $vector_brid --api-url $url query_closest_objects collection = my_vector_collection context = 0 q_vector = "[1.0, 2.5, 3.0]" max_distance = 1.0 max_vectors = 2 'query_template=["type":"get_messages_with_distance"]' 

[ 
{ 
"distance" : "0" , 
"text" : "hello" 
} , 
{ 
"distance" : "0.005509683802306209" , 
"text" : "hej" 
} 
] 

Passing arguments to filter results ​ 

chr query -brid $vector_brid --api-url $url query_closest_objects collection = my_vector_collection context = 0 q_vector = "[1.0, 2.5, 3.0]" max_distance = 1.0 max_vectors = 2 'query_template=["type":"get_messages_with_filter", "args":["text_filter": "j"]]' 

[ 
"hej" 
] 

You can find the source code and additional details about the Vector DB extension in the 👉 official repository


### vector-db use case

_Official page extract. File: `get-started_use-cases_ai-on-chain_vector-db.txt`._

On this page 

Vector database applications 

Challenge ​ 

Building intelligent recommendation and similarity search systems presents significant technical and operational
challenges that traditional centralized databases struggle to address effectively. The core requirements include
high-performance similarity searches across large datasets, real-time query processing for instant suggestions, and
scalable infrastructure capable of handling millions of vector embeddings. Additionally, these systems must maintain
data integrity with tamper-proof metadata while providing cost-effective solutions for storing and querying
high-dimensional vector data. 

Traditional centralized databases often face performance bottlenecks when dealing with large-scale vector operations,
leading to slow response times and poor user experience. Data privacy concerns arise when sensitive user preferences and
metadata are stored in centralized systems, while high operational costs make it challenging to scale recommendation
systems to serve millions of users effectively. 

Solution ​ 

Chromia's Vector DB extension provides a decentralized vector database that integrates seamlessly with blockchain
technology, offering a comprehensive solution to the challenges faced by traditional recommendation and similarity
search systems. This innovative approach enables developers to build intelligent applications that are performant,
scalable, secure, and cost-effective. 

The solution leverages Chromia's blockchain architecture to provide fast similarity searches with configurable
parameters, efficient handling of large vector datasets through optimized indexing, and tamper-proof metadata storage
with blockchain immutability. The decentralized nature of the system ensures that data remains secure and verifiable
while providing the performance and scalability required for modern AI applications. 

Business value ​ 

Vector database applications deliver significant business benefits: 

Enhanced user experience ​ 

Personalized recommendations based on semantic understanding of content 

Real-time suggestions that adapt to user preferences and behavior 

Improved discovery of relevant content through intelligent similarity matching 

Operational advantages ​ 

Decentralized architecture eliminates single points of failure 

Tamper-proof data ensures metadata integrity and trust 

Scalable infrastructure supports growth from thousands to millions of users 

Cost optimization through efficient vector storage and querying 

Competitive differentiation ​ 

AI-powered insights that go beyond simple matching algorithms 

Semantic understanding of content, themes, and user preferences 

Blockchain transparency builds user trust in recommendation algorithms 

Use cases ​ 

Core functionality ​ 

Vector database applications provide: 

Store high-dimensional embeddings as vectors (e.g., text, images, audio, product data) 

Process natural language queries and convert to vector format 

Perform similarity searches to find relevant content based on semantic meaning 

Return personalized recommendations based on semantic similarity and user preferences 

Implementation overview ​ 

For technical implementation details, see the Vector DB extension documentation . 

The system leverages Chromia's Vector DB extension to: 

Store embeddings efficiently with blockchain immutability 

Perform fast similarity searches using cosine distance calculations 

Scale horizontally across Chromia's decentralized network 

Maintain data integrity with tamper-proof metadata storage 

The vector database workflow follows this process: 

User query : Natural language input is submitted to the system 

Embedding : Text is converted to high-dimensional vector format 

Vector search : Similarity calculations are performed against stored vectors 

Results : Ranked recommendations are returned based on semantic similarity 

Results ​ 

Performance benchmarks ​ 

Vector database applications achieve production-ready performance: 

Performance metrics: 

Query response time: Fast similarity searches with configurable parameters (< 100ms for 1M items) 

Data scale: Handles large vector datasets with efficient indexing (10M+ embeddings) 

Data integrity: Tamper-proof metadata storage with blockchain verification 

Flexible querying: Custom templates for enriched recommendations 

Scalable architecture: Distributed across Chromia's network for global applications 

Business impact ​ 

Enhanced user experience with accurate recommendations and personalized suggestions 

Decentralized storage with tamper-proof metadata guarantees 

Improved data privacy with secure, verifiable data handling 

Flexible architecture for building various recommendation systems 

Extending vector database applications ​ 

Vector database applications can be extended to various recommendation and similarity search use cases: 

Application 
Use case 

Product recommendations 
Similar product suggestions based on user behavior 

Content discovery 
Similar article/blog post recommendations 

Media recommendations 
Similar movies, music, and content suggestions 

Book recommendations 
Similar book suggestions based on reading history 

Game recommendations 
Similar game suggestions based on play history 

Restaurant recommendations 
Similar restaurant suggestions based on preferences 
Getting started ​ 

Technical implementation ​ 

To implement vector database applications: 

Set up the Vector DB extension - Follow the Vector DB extension setup guide 

Configure your blockchain - See configuration details 

Deploy your application - Use the deployment guide 

Integrate with Rell - Learn about Rell integration 

Learning resources ​ 

Learn by Building 

Semantic Search with Vector DB on Chromia -
Build a complete semantic search engine using sentence embeddings with the Vector Database Extension. (Advanced) 
Next steps ​ 

Explore the Vector DB extension repository 

Follow the deployment guide 

Check out other AI use cases on Chromia 

Ready to build your own AI application? Start with the Vector DB extension repository .


## 7. End-to-end sequence (only what this tree documents)

1. chr create-rell-dapp (optional template). Edit chromia.yml plus Rell under src/.
2. Local Postgres (project-config / Database setup). chr node start (--wipe to reset). chr test, chr query, chr tx.
3. Keys: chr keygen --key-id=.... Set key.id in ~/.chromia/config. Never commit privkeys or mnemonics.
4. Tokens: Testnet faucet 1000 tCHR / 7 days, or BSC Testnet claim, or Mainnet at least 10 CHR deposit via Vault.
5. Lease a container (Testnet or Mainnet Vault). Save Container ID. For Vector DB, select that extension at lease time.
6. deployments.testnet / deployments.mainnet plus container. Reserved names fill Directory BRID/URL (still re-verify on Explorer).
7. chr deployment create --network ... --blockchain .... Save printed chains: RID (CLI 0.30.0 also writes it back). Testnet: wait about 5 minutes; confirm on Explorer.
8. Client: directoryNodeUrlPool = current system cluster API URLs plus dapp BRID. No admin privkeys in frontend.
9. Updates: chr deployment update (optional --verify-only, --height). Multi-owner: voter set plus proposals. Enum reorder/remove is blocked until approved (CLI 0.30.0).
10. Optional: Vault find_dapp_details; Filehub media; webStatic for static frontend (local URL documented); Token Chain propose_token / user funding; Vector DB module plus query_closest_objects.

## 8. Canonical URLs crawled (2026-08-26)

All of the following returned HTTP 200 unless marked.

Deployment: /build/deployment/, testnet (getting-started, get-container, deploy-dapp, connect-client, list-dapp-vault), mainnet (getting-started, get-container, deploy-dapp, connect-client, multi-deployment), testnet-tokens (get-tchr-chromia, get-tchr-binance), vault-listing (quick, dynamic), deploy-frontend-dapp. /build/deployment/frontend is 404.

Configuration: /build/configuration/, project-structure, project-config, blockchain-properties.

CLI: /build/cli/ is 404. Crawled introduction, key-pair-management, cli-release-notes, generating-doc-site, commands/ and every command page (build, code, create-rell-dapp, deployment, eif, generate, help, keygen, library, multi-signature, node, query, repl, seeder, test, tools, tx, version), library/, Seeder/ plus configurable-generators, generator, seeder-example.

Database: overview, getting-started.

Token chain: /build/token-chain/, developer-token-proposal, user-account-creation, get-started/about/architecture/chains/token-chain.

Vector: /build/vector-search/overview, sample-workloads, ecosystem/extensions/vector-db, get-started/use-cases/ai-on-chain/vector-db.

Product hosts named by these pages: vault.chromia.com/en/deposit, vault.testnet.chromia.com/en/containers/, testnet.bscscan.com/address/0x0e61dfdbd5b979adbf3e5b5fe7ee40f85b6daa8d#writeContract, gitlab.com/chromaway/core/directory-chain/-/tree/dev/src/token_chain, bitbucket.org/chromawallet/ft3-lib, filehub-gw.chromia.com/mainnet/<hash>.

## 9. Not published in this tree (do not invent)

- Token Chain / Economy Chain / EVM-submitter / Filehub production BRIDs as static hex (placeholders and query names only).
- Chromia Testnet Faucet hostname (page says Chromia Testnet Faucet without a URL).
- A Testnet/Mainnet URL for hosted web_static frontends (only localhost is documented).
- What chromia start is or which flags it takes (named only on the database getting-started page; not in the CLI command list).
- A fixed CHR-per-SCU price, minting-policy binary format, or Token Chain validator contract address (identify via get_all_bridges).
- In-place attribute-type migration (still not described on these pages).

### Seeder generator catalog

Full catalog is in https://docs.chromia.com/build/cli/Seeder/generator (extract `build_cli_Seeder_generator.txt`, 16k chars). Configurable generators table is extracted above. Example page: https://docs.chromia.com/build/cli/Seeder/seeder-example.
