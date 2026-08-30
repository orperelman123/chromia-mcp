# Chromia Rell, chromia.yml, CHR/CLI, and testing — production knowledge brief

**Crawled:** 2026-08-26  
**Scope:** Official docs at docs.chromia.com plus official GitLab (gitlab.com/chromaway). No invented APIs or flags. Where sources disagree or a page is silent, that is stated.

**Source lag (read this first):**

| Artifact | docs.chromia.com (as crawled) | gitlab.com/chromaway (as crawled) |
|---|---|---|
| Rell language / compiler | Latest listed release **0.16.4** (2026-08-02) | Tags through **0.16.7** (release notes dated 2026-08-14) |
| Chromia CLI (`chr`) | Release notes stop at **0.30.0** (2026-02-27), which bundled **Rell 0.15.2** | Tags through **0.33.2**. CHANGELOG documents **0.33.1** (2026-06-24). **0.33.0** (2026-05-29) bumped bundled Rell to **0.16.0** |

CLI command pages on docs.chromia.com therefore describe the 0.30.x-era surface. GitLab CHANGELOG entries after 0.30.0 are cited separately when they change production behavior.

`https://docs.chromia.com/rell` returns **404**. The Rell section lives under `/rell/rell-intro`, `/rell/language-features/…`, `/rell/tests`, `/rell/releases`. CLI lives under `/build/cli/…`, not `/cli`.

Official code lives on **GitLab**, not GitHub: `gitlab.com/chromaway/rell` and `gitlab.com/chromaway/core-tools/chromia-cli`. Homebrew tap, Scoop bucket, and the CLI Docker image are also GitLab.

---

## 1. How a production Rell dapp is structured

### 1.1 Project layout

`chr create-rell-dapp` generates:

```
|-- chromia.yml
|-- src
    |-- main.rell
    |-- test
        |-- arithmetic_test.rell
        |-- data_test.rell
```

`main.rell` starts with the `module` keyword (single-file module). Official recommended multi-file layout for a larger module:

```
src/
  app/                    # directory module named "app"
    module.rell           # imports + mount names
    entities.rell         # entities, enums, small structs
    operations.rell
    queries.rell
    functions.rell
    structs.rell          # when there are more than ~3 structs
```

In `chromia.yml`, the blockchain entry point is the **module name** (`main`, `app`, `module_a`), not a file path. Do not write `module_a/module.rell`.

Templates accepted by `chr create-rell-dapp --template`: `plain`, `plain-multi`, `minimal`, `plain-library`, `asset-management`. `--devcontainer` scaffolds a Docker/VS Code devcontainer.

### 1.2 Modules

A Rell dapp is a tree of modules. A module is either:

- a **single-file module**: a `.rell` file that starts with `module;`
- a **directory module**: all `.rell` files in a directory that do not themselves have a module header. `module.rell` always belongs to the directory module, even if it has a header. A directory module does not require `module.rell`.

The **root module** is the directory module of `.rell` files in the source-root; it has an empty name.

Every file of a directory module sees all other files in that module. A single-file module sees only its own definitions.

At runtime only the **main module** (the one named in `blockchains.<name>.module`) and modules it imports (directly or indirectly) are active. Inactive modules contribute neither operations/queries nor tables.

Imports:

```rell
import app.single;            // alias = last path segment
import alias: app.multi;      // custom alias
import .d;                    // relative: current.d
import alias: ^;              // parent
import alias: ^^;             // grandparent
import foo.*;                 // wildcard into current namespace
import foo.{f, g};            // selected definitions
import foo.{a: f, b: g};      // rename on import
```

Anonymous imports (`import _: some.mod;`) exist since Rell 0.13.12 — they activate a module without adding names.

Namespaces can group imports (common FT4 pattern):

```rell
namespace ft4 {
  import lib.ft4.core.accounts.{ft4_account: account};
  import lib.ft4.core.admin;
}
```

### 1.3 Entities

Persistent data. Mapped to a PostgreSQL table. Created/deleted with `create` / `delete`. A variable of entity type holds the **rowid** (primary key), not the attribute values.

```rell
entity user {
    first_name: text;
    last_name: text;
    year_of_birth: integer;
    mutable salary: integer;
}
```

Rules from official docs:

- If the attribute name is omitted, it defaults to the type name (`company;` means `company: company`).
- Attributes may have default values used when omitted from `create`.
- Implicit `rowid` attribute is the primary key.
- **Entity attributes cannot be nullable.**
- `key` = unique constraint + index. `index` = non-unique index. Both can be composite. Order of composite columns matters (left-prefix, same as SQL).
- Combined `key first_name: text, last_name: text;` cannot also mark those fields `mutable`.
- Relations are modeled as entity attributes of entity type (stored as the referenced rowid) plus uniqueness:
  - 1-1: both sides `key`
  - 1-many: `index` on the many side, `key` on the unique side
  - many-many: junction entity with `key left, right`
- `@log entity …` adds an implicit `transaction` attribute, makes the entity immutable and non-deletable. Adding or removing `@log` after deploy is incompatible.
- Compatible schema changes: add attributes with defaults; add attributes to empty tables; remove attributes (since 0.15.1 columns are dropped automatically); change mutability on non-`@log` entities. Incompatible: change attribute type; add/remove `@log`.
- Size-constraint annotations (`@size`, `@min_size`, `@max_size`) apply to entity/object attributes since 0.15.1 (text and byte_array).
- `@mount` on an entity attribute (since 0.15.1) names the **column**, so a rename does not drop data.

### 1.4 Objects

Singleton persistent rows. Auto-initialized at blockchain init. Cannot be `create`d or `delete`d from code. Every attribute needs a default value.

```rell
object event_stats {
    mutable event_count: integer = 0;
    mutable last_event: text = "n/a";
}

query get_event_count() = event_stats.event_count;

operation process_event(event: text) {
    update event_stats ( event_count += 1, last_event = event );
}
```

### 1.5 Operations

The write API. Invoked by name from a blockchain transaction.

- Can modify the database.
- Do not return a value.
- Parameter types **must be GTV-compatible**.
- Default parameter values are allowed; **new defaults must be appended** so existing clients keep working (since 0.14.3).
- Size-constraint annotations on `text` / `byte_array` parameters (since 0.14.14).
- Typical auth pattern: take a `pubkey`, then `require(op_context.is_signer(user_pubkey))`.

Annotations:

- `@compound` — cannot be the only kind of operation in a transaction.
- `@singular` — at most once per transaction.
- Both can be combined.

**Guard blocks** (read-only argument verification):

```rell
operation transfer_tokens(from_pubkey: pubkey, to_pubkey: pubkey, amount: integer) {
    guard {
        val sender = user @ { .pubkey == from_pubkey };
        require(op_context.is_signer(from_pubkey));
        require(sender.balance >= amount);
        require(amount > 0);
    }
    // writes happen after the guard
}
```

Restrictions: only *declarations* (not assignments) may appear before `guard`; the guard may read the DB but not write; `op_context.emit_event()` is illegal in a guard. Guards run during Postchain `checkCorrectness()` **and** again during `apply()`, so `print`/`log` in a guard execute twice. Since 0.15.0 the guard also runs during operation validation.

### 1.6 Queries

The read API.

- Cannot modify the database (compile-time check).
- Must return a value (type inferred if omitted).
- Parameter types **and** return type must be GTV-compatible.
- Short form: `query q(x: integer): integer = x * x;`
- Full form with `return`.
- Default parameters allowed (same client-compat rule as operations).

### 1.7 Functions

Reusable logic. Can modify the DB when called from an operation. Can be called from queries, operations, and other functions. No explicit return type ⇒ `unit`.

Also: default parameters, named arguments, function values, partial application (`f(*)`, `f(123, *)`), `@extendable` / `@extend`, `@test` (test modules only, no parameters), `@native` (Java/Kotlin implementation; must be deterministic; mapped in `blockchains.<name>.config.gtx.rell.native`).

### 1.8 Structs

In-memory only. Attributes immutable unless `mutable`. Garbage-collected. Constructed by calling the struct name. `.to_struct()` / `.to_mutable_struct()` copy an entity/object; `create user(s)` creates from `struct<user>`.

Special forms:

- `struct<mutable T>`
- `struct<some_operation>` — attributes = that operation's parameters. Has `.to_gtx_operation()` and `.to_test_op()`.
- Built-ins used in production/testing: `gtx_operation { name: text; args: list<gtv>; }`, `gtx_transaction_body`, `gtx_transaction`, `rell.test.keypair { priv: byte_array /* 32 bytes */; pub: byte_array /* 33 bytes */; }`.

All GTV-compatible structs have `from_gtv` / `to_gtv` / `from_bytes` / `to_bytes` / `from_gtv_pretty` / `to_gtv_pretty`.

### 1.9 Enums

```rell
enum currency { USD, EUR, GBP }
```

Each constant has `.name: text` and `.value: integer` (declaration index). `T.values()`, `T.value(text)`, `T.value(integer)`.

**Production warning (CLI 0.30.0+):** `chr deployment update` detects enum value additions, removals, and reorderings. Reorder or remove is treated as dangerous (numeric `.value` is the stored form). Do not reorder production enums.

### 1.10 Mount names

Entities/objects → SQL table name. Operations/queries → the name clients call.

Default = fully qualified definition name (`namespace.foo.entity` → `foo.entity`). Override with `@mount('…')` on the entity/object/operation/query, or on a namespace/module to prefix children. Relative forms: `.` appends, `^` pops one segment.

Mount names are fixed at definition, not at import. Since 0.13.11 mount names are limited to **58 characters**. Table names are additionally prefixed by blockchain ID at SQL level (`"c0.housekey"`).

### 1.11 Special operations

Auto-invoked lifecycle / integration hooks. Global namespace — use `@mount('extension.name')` plus an `__` prefix on the operation name:

| Operation | When |
|---|---|
| `__begin_block(height: integer)` | Before regular txs in a block |
| `__end_block(height: integer)` | After regular txs |
| `__icmf_message` | ICMF inbound |
| `__evm_block` | EIF / EVM block processed |
| `__timeb` | Time-bounded tx validity (UTC ms; client clock must be close to signer) |

---

## 2. How Rell maps to PostgreSQL

Chromia stores each dapp chain in PostgreSQL. Rell compiles to SQL. Official mapping, pieced only from stated docs:

| Rell | PostgreSQL / SQL |
|---|---|
| `entity` | Table. Created/altered at dapp start. |
| implicit `rowid` | 64-bit integer PK from a per-blockchain sequence. New inserts get a value strictly greater than the last allocated rowid on that chain. Deletes do not reuse. |
| entity-typed attribute | Column holding the referenced rowid (join key). |
| `object` | Table with a single auto-inserted row. |
| `key attr` | Unique index / unique constraint. |
| `index attr` | Non-unique index. Docs state PostgreSQL B-tree, lookup *O(log n)*. |
| composite `key` / `index` | Multi-column unique / non-unique index. Left-prefix order matters. |
| `create` | `INSERT`. Bulk form: `create MyEntity(list<struct<MyEntity>>): list<MyEntity>` — one SQL statement. |
| `update` | `UPDATE`. Only `mutable` attributes. Cardinality `@` / `@?` / `@*` / `@+` must match or the tx fails. |
| `delete` | `DELETE`. Same cardinality operators. Multi-entity form deletes only the first entity. |
| `@` / `@?` / `@*` / `@+` | `SELECT` with cardinality check. |
| `integer` | 64-bit signed (`-2^63` … `2^63-1`). Native mapping: Java/Kotlin `long` / `Long`. |
| `big_integer` | SQL `NUMERIC`. Interpreter: `java.math.BigInteger`. Precision 131072 digits. Literal suffix `L`. |
| `decimal` | SQL `NUMERIC`. Interpreter: `java.math.BigDecimal`. 20 decimal places, 131072 integer digits. Not IEEE float. |
| `json` | PostgreSQL `JSON`. |
| `text`, `byte_array` | Most member functions used inside at-expressions are translated to SQL equivalents. |
| mount name | Unquoted logical table name; SQL identifier is `"c<iid>.<mount>"` (observed in official `chr repl --sql-log` output: `"c0.housekey"`, `"c0.owner"`). |

Relational expression pipeline (matches SQL logical order, not SQL written order):

```
FROM        CARDINALITY  WHERE           WHAT   TAIL
entity_name @            { condition }   ()     ;
```

- `@` exactly one (error otherwise)
- `@?` zero or one
- `@+` one or more
- `@*` zero or more

Joins: `(c: channel, m: message) @* { … } (m.text)`. Annotations in WHAT: `@sort`, `@sort_desc`, `@omit`, `@list` / `@set` / `@map` (aggregation, since 0.13.9). Tail: `limit`, `offset`.

**What official docs do not specify in one place:** exact PostgreSQL type for `text`, `byte_array`, `boolean`, `timestamp` alias, or `enum` columns. Observed facts only:

- enums convert to GTV as integer on operation input and as string on query output (see §6) — the SQL column type is not documented on the pages crawled.
- `rowid` / entity references are 64-bit integers in GTV and in SQL logs.
- Entity attributes are non-nullable at the language level, so columns are created NOT NULL (inferred; docs do not print `CREATE TABLE`).

Optimization notes the official docs *do* state:

- Index/key writes cost extra; index only what you filter or join on.
- Mutating an indexed field is slow on large tables.
- `chr repl --sql-log --use-db --module <main>` (and historically `chr test --sql-log`) dumps the generated SQL. Fields in WHERE should be `key`; fields in JOIN should be `index`.
- Statement order in WHERE and FROM affects the generated plan. Prefer the most selective predicate first.
- Since 0.13.9, keys and indexes can be added or removed at database initialization.

System entities (immutable, not creatable from code):

```rell
entity block {
    block_height: integer;
    block_rid: byte_array;
    timestamp;
}
entity transaction {
    tx_rid: byte_array;
    tx_hash: byte_array;
    tx_data: byte_array;   // GTV-encoded GTX transaction
    block;
}
```

`tx_data` decodes as `gtx_transaction.from_bytes(.tx_data)`.

Do not read `op_context.transaction.block` except `block_height` — other block attributes are null during the building block and throw. Use `op_context.block_height` and `op_context.last_block_time`.

---

## 3. chromia.yml — production keys

Official reference: [Project settings file](https://docs.chromia.com/build/configuration/project-config). Most keys have defaults; only `blockchains` is required for a minimal file.

### 3.1 `blockchains` (required)

```yaml
blockchains:
  hello:
    module: main          # entry module; imports determine the closure
```

Per-chain keys documented:

| Key | Type | Meaning |
|---|---|---|
| `<name>` | string | Blockchain name. Hyphens are **disallowed** (CLI 0.20.14; directory-chain spec). |
| `module` | string | Entry module name. |
| `moduleArgs` | dict | Per-imported-module arguments. The target module must declare `struct module_args { … }`. Read via `chain_context.args` **in that module**. You cannot read `args` if `module_args` is not defined. Attributes with defaults may be omitted. |
| `config` | dict | Raw blockchain/GTV config (see §3.8). |
| `test` | dict | Per-chain tests (`modules`, `moduleArgs`, `failOnError`). Overrides project-level `test.moduleArgs` for those modules. |
| `type` | `blockchain` \| `library` | Since CLI 0.19.0. `library` compiles a library whose root must live at `lib/<name>` matching the YAML key. |

Native-function mapping (Rell 0.15.0+), under the chain:

```yaml
blockchains:
  my_chain:
    module: main
    config:
      gtx:
        rell:
          native:
            my_module: com.domain.pack.MyNativeModule
```

### 3.2 `deployments`

```yaml
deployments:
  testnet:                          # reserved names: mainnet, testnet
    container: <containerIID>
    chains:
      my_rell_dapp: x"<BlockchainRid>"
```

| Key | Type | Meaning |
|---|---|---|
| `<name>` | string | Target name. `mainnet` / `testnet` fill `brid` + `url` automatically when used with the CLI. |
| `brid` | byte_array (`x"…"`) | Directory-chain RID. Official values on the project-config page: mainnet `x"7E5BE539EF62E48DDA7035867E67734A70833A69D2F162C457282C319AA58AE4"`; testnet `x"6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92"`. CLI 0.28.0+ / 0.29.8: `url` and `brid` are optional for Chromia networks. |
| `url` | string or list | System node API URL(s). |
| `container` | string | Container ID. |
| `chains` | dict name → hex RID | Keys **must** match a `blockchains` entry. Omit on first deploy; the CLI prints the RID to add. First-deploy RID is the genesis identifier for all later updates. |

### 3.3 `compile`

```yaml
compile:
  rellVersion: 0.14.9
  source: src
  target: build
  deprecatedError: false
  quiet: true
  strictGtvConversion: true
```

| Key | Type | Default / notes |
|---|---|---|
| `rellVersion` | semver string | Language/compiler compatibility version written into the blockchain config (`gtx.rell.version` / compilerVersion). Deployment refuses a version higher than the target cluster supports. |
| `source` | path | Default `src`. |
| `target` | path | Default `build`. Also used as snapshot output for deploys. |
| `deprecatedError` | bool | Fail the build on deprecated syntax. |
| `quiet` | bool | Default `false` since CLI 0.17.1. |
| `strictGtvConversion` | bool | Default `true`. Only exists from Rell 0.13.9; older behaved as false. Leave true in production. |

### 3.4 `database` (local node / tests)

```yaml
database:
  password: postchain
  username: postchain
  database: postchain
  host: localhost
  logSqlErrors: true
  schema: rell_app
  driver: org.postgresql.Driver
```

Official default local DB (install docs): database/user/password all `postchain`. Devcontainer and Homebrew/apt flows create that role.

Environment overrides (win over YAML): `CHR_DB_URL`, `CHR_DB_USER`, `CHR_DB_PASSWORD`, `CHR_DB_SCHEMA`.

YAML string substitution: `foo: ${MY_VAR:-default_var}`.

Docker-host notes: macOS `host.docker.internal`; Windows `172.17.0.1`; Linux `--network=host`.

### 3.5 `test`

```yaml
test:
  modules:
    - test.arithmetic_test
    - test.data_test
  moduleArgs:
    test.arg_test:
      value: 4
  failOnError: true
```

| Key | Meaning |
|---|---|
| `modules` | Test module names (relative to source, e.g. `test.data_test`). |
| `moduleArgs` | Args for app modules **or** test modules during the test run. |
| `failOnError` | Abort the suite on first failure. Overridable by `chr test --fail-on-error`. |

Per-chain `blockchains.<name>.test` is the other place tests can be declared.

### 3.6 `libs`

Two official shapes.

Library-chain (recommended):

```yaml
libs:
  com.chromia.ft4:
    version: "<semver>"
    registry: mainnet          # mainnet (default) | testnet | localhost | URL
    brid: x"…"                 # only for a custom registry URL
```

External Git:

```yaml
libs:
  ft4:
    registry: https://gitlab.com/chromaway/ft4-lib.git
    path: rell/src/lib/ft4
    tagOrBranch: v1.1.0r
    rid: x"<gtv-hash-of-rell-files>"
    insecure: false            # true skips rid check — not for production
```

FT4 examples on docs also pin `iccf` from `https://gitlab.com/chromaway/core/directory-chain`.

### 3.7 Other documented top-level keys

- `docs` — `chr generate docs-site`: `title`, `footerMessage`, `customStyleSheets`, `customAssets`, `additionalContent`, `sourceLink.remoteUrl`, `sourceLink.remoteLineSuffix`.
- `definitions` — YAML-anchor bucket. Anchors must live here.
- `!include other.yml` and `!include other.yml#key` — compose settings files.
- Big integers in YAML use a capital `L` suffix (`1234L`).

### 3.8 `blockchains.<name>.config` (blockchain properties)

Documented under [Configuration properties](https://docs.chromia.com/build/configuration/blockchain-properties). Production-relevant subset:

```yaml
config:
  features:
    merkle_hash_version: 2          # v1 deprecated (hash collisions). Default 2.
  blockstrategy:
    maxblocksize: 27262976          # Chromia max 26 MiB
    maxblocktransactions: 100
    mininterblockinterval: 1000     # Chromia min 1000 ms
  gtx:
    modules:
      - net.postchain.rell.module.RellPostchainModuleFactory
    max_transaction_size: 26214400
    rell:
      native: { … }
  revolt:
    revolt_when_should_build_block: true   # CLI 0.19.0 default
```

Also: `signers`, `query_timeout_seconds` (default 60), `query_cache_ttl_seconds` (default 0 = off), `max_block_future_time` (default 60000 ms), `add_primary_key_to_header`.

Allowed GTX modules on Chromia (from that page): `RellPostchainModuleFactory`, `StandardOpsGTXModule`, `IcmfSenderGTXModule`, `IcmfReceiverGTXModule`, `IccfGTXModule`, `EifGTXModule`, `WebStaticGTXModuleFactory`.

### 3.9 Keys seen on cookbook pages that are **not** in the project-config reference

The cookbook “create a new Rell dapp” page showed `database.schema_version`, `test.timeout`, `test.parallel`, `build.output_dir`, `build.optimize`. Those keys are **not** listed on the official project-config page. Treat them as unverified; do not rely on them.

---

## 4. Official CLI commands (exact names)

Binary: `chr`. First-two-letter shortcuts work (`chr de cr` = `chr deployment create`). Requires **Java 21+** (`RELL_JAVA` overrides). Auto-completion: `chr --generate-completion [bash|zsh|fish]`. Default DB mode is `--use-db`; `--no-db` skips it.

Install (official):

- macOS: `brew tap chromia/core https://gitlab.com/chromaway/core-tools/homebrew-chromia.git && brew install chromia/core/chr`
- Linux: apt repo `https://apt.chromia.com`
- Windows: Scoop bucket `https://gitlab.com/chromaway/core-tools/scoop-chromia.git`
- Docker: `registry.gitlab.com/chromaway/core-tools/chromia-cli/chr:latest`

Keys live in `~/.chromia/`. `chr keygen --key-id=chromia_key` is the getting-started flow.

### 4.1 Command index (docs command-reference page)

`help`, `version`, `build`, `create-rell-dapp`, `deployment`, `eif`, `generate`, `library`, `keygen`, `node`, `query`, `repl`, `test`, `tx`, `code`, `multi-signature`, `tools`, `seeder`.

Release notes / CHANGELOG also use `chr install` as a first-class command (library install that updates `libs:`). The command-reference page lists `chr library install` instead. Both names appear in official sources; the documented library subcommands are below.

### 4.2 Create

```
chr create-rell-dapp [<options>] [<name>]
  -d, --base-dir=<path>
  --template=(plain|plain-multi|minimal|plain-library|asset-management)
  --devcontainer
```

### 4.3 Build

```
chr build [<options>]
  -s, --settings=<settings>
  -bc, --blockchain=<blockchain>
  -f, --format=(GTV|XML)
  --hide-lib-warnings
  --skip-lib-check          # added CLI 0.30.0
```

Reads `compile.source` (default `src`), writes `compile.target` (default `build`).

### 4.4 Test

```
chr test [<options>]
  -s, --settings=<settings>
  -bc, --blockchain=<blockchain>
  -m, --modules=<modules>        # comma-delimited; must be in scope
  --file=<path>                  # one Rell file (CLI 0.27.9+)
  --tests=<>                     # test method pattern
  --use-db / --no-db
  --test-report                  # JUnit XML
  --test-report-dir=<path>       # default build/reports
  --fail-on-error[=true|false]
  -ts, --timestamp
  --hide-lib-warnings
```

Runs modules listed under `test` and/or `blockchains.*.test`.

**`--sql-log` discrepancy:** documented on the cookbook “run tests” page and historically on CLI ≤0.30. GitLab CHANGELOG **[0.31.0] 2026-03-04**: “The `--sql-log` flag has been removed.” SQL stats became an HTML report next to other reports. If you are on CLI ≥0.31, do not pass `--sql-log` to `chr test`. `chr repl --sql-log --use-db` is still documented on the Rell optimization page.

The cookbook also shows `chr test --verbose`. That flag is **not** on the official `chr test` usage block. Unverified.

### 4.5 Node

```
chr node start
  -s, --settings=<settings>
  -bc, --blockchain-config=<path>   # e.g. build/my_rell_dapp.xml
  --name=<name>                     # repeatable
  -p=<key=value>
  -np, --node-properties=<path>
  --directory-chain-mock            # chain 0 mock for ICCF / FT4 x-chain / frontend
  --hide-lib-warnings
  --sql-log
  --wipe / --no-wipe                # wipe schema → start at height 0
```

If a schema already has the chain, the new config is scheduled at the next height unless `--wipe`.

```
chr node update                     # same selection flags
  -n, --preemption=<int>            # apply this many blocks ahead (default: height+2)
```

Must be invoked with the **same** `chromia.yml` and args as `start`, so chain IDs match.

### 4.6 Deploy

```
chr deployment create
  -cfg, --config=<config>
  -s, --settings=<settings>
  --secret=<path> | --key-id=<key_id>
  -d, --network=<text>
  -bc, --blockchain=<text>
  --no-compression
  --hide-lib-warnings
  -y                                # skip confirm
```

CLI 0.30.0+: writes the new chain RID back into `chromia.yml`.

```
chr deployment update               # plus --height, --verify-only, --skip-verification
chr deployment info                 # --brid/--cid/--api-url | --mainnet/--testnet | -d/-bc
chr deployment inspect              # --modules, --list-modules, --module-args,
                                    # --definitions=(queries|operations|entities|objects),
                                    # --signature=<value>
chr deployment pause | resume | remove
chr deployment proposal vote | retract-vote | list | info | revoke | rename
chr deployment voterset info | update | list | add-dapp-provider
chr deployment container configuration | pause | resume
```

Aliases kept: `chr deployment pause-container` / `resume-container`.

Local workflow from official intro:

1. `chr create-rell-dapp`
2. `chr node start` (needs PostgreSQL)
3. `chr query hello_world`
4. `chr test`
5. `chr deployment create --network testnet --secret .secret`

### 4.7 Adjacent commands you will hit in production

- `chr query <name> [args…]` — default target is the local node. `--api-url` if not `http://localhost:7740`. `--network testnet` / `--mainnet` since 0.28.0.
- `chr tx <op> [args…]` — signs with configured key. `--no-await` to skip confirmation wait. FT4/ICCF/timeb flags exist; ICCF against a local node requires `--directory-chain-mock` (breaking change in 0.26.0).
- `chr keygen --key-id=…`
- `chr library install [<library-id>]` (`--url`, `-b/--brid`, `-lib`, `-f/--force`)
- `chr library list | view | versions`
- `chr generate client-stubs` / `chr generate docs-site` / `chr generate graph`
- `chr code lint` / `chr code format` / `chr code check` (0.27.9+)
- `chr tools validate-config`, `chr tools gtv`, `chr tools lib-model`
- `chr version`
- `chr seeder init` / `chr seeder generate` — early-stage; docs say it may change.

---

## 5. Testing

### 5.1 Test modules

A test module is a module annotated `@test`:

```rell
@test module;

import main;

function test_foo() {
    assert_equals(2 + 2, 4);
}
```

Executed functions:

- name `test` or prefix `test_`
- or `@test function foo()` (preferred since 0.14.13 — violations are compile errors, not silent skips)

`@test` functions: no parameters; only in `@test` modules; cannot combine with `@extendable`/`@extend`/`abstract`/`override`.

If a module name ends in `_test`, it is treated as the test companion of the same name without the suffix (`program` ↔ `program_test`).

`rell.test.*` is **only** visible inside test modules.

Disable: `@disabled` on the module (skips the module and submodules) or on a single test function (since 0.15.1). Using `@disabled` on a non-test is a compile error.

Each test function runs independently. Output includes per-test and suite duration.

### 5.2 Builders (no fixtures API by that name)

Official “fixture” equivalent is the fluent builder + helper functions you write yourself.

- `rell.test.tx()` → transaction builder (ops + signers)
- `rell.test.block()` → block builder (list of txs)
- Calling an operation in test scope produces a `rell.test.op`
- `.run()` executes; `.run_must_fail()` / `.run_must_fail("message")` asserts failure
- `rell.test.nop()` — unique no-op (makes otherwise-identical txs distinct)

```rell
function test_add_user() {
    assert_equals(data.user @* {}(.name), list<text>());
    val tx = rell.test.tx(data.add_user('Bob'));
    assert_equals(data.user @* {}(.name), list<text>()); // not yet applied
    tx.run();
    assert_equals(data.user @* {}(.name), ['Bob']);
}
```

Multi-tx block:

```rell
rell.test.block().tx(tx1).tx(tx2).tx(tx3).run();
```

### 5.3 Assertions (official list)

`assert_equals`, `assert_not_equals`, `assert_true`, `assert_false`, `assert_null`, `assert_not_null`, `assert_gt` / `ge` / `lt` / `le`, range forms `assert_ge_le` / `assert_ge_lt` / `assert_gt_le` / `assert_gt_lt`, `assert_fails(fn)` / `assert_fails(expected_message, fn)`, `assert_events`.

### 5.4 Test keys — never production

`rell.test.keypairs` / `.pubkeys` / `.privkeys` with names: `alice`, `bob`, `charlie`, `dave`, `eve`, `frank`, `grace`, `heidi`, `trudy`.

`rell.test.keypair` is `{ priv: byte_array /* 32 bytes */, pub: byte_array /* 33 bytes */ }`.

### 5.5 Block time

Deterministic.

| Symbol | Meaning |
|---|---|
| `rell.test.DEFAULT_BLOCK_INTERVAL` | 10000 ms |
| `rell.test.DEFAULT_FIRST_BLOCK_TIME` | 2020-01-01 00:00 UTC (ms) |
| `rell.test.last_block_time` | last built block |
| `rell.test.next_block_time` | next block, once built |
| `rell.test.block_interval` | current interval |
| `rell.test.set_block_interval(ms)` | future interval (does not override an already-scheduled next time) |
| `rell.test.set_next_block_time(ms)` | absolute |
| `rell.test.set_next_block_time_delta(ms)` | relative to last; no-op if there is no previous block |

### 5.6 Other test utilities

`rell.test.get_events(): list<(text, gtv)>` — events from the last block construction.

### 5.7 Common pitfalls (only those official docs actually warn about)

1. **Operations do nothing until `.run()`.** Asserting entity state between `tx(…)` and `.run()` is the documented way to prove that.
2. **`run_must_fail()` without a message** hides the real error. Official best practice: always pass the expected string.
3. **`@log` / `print` inside `guard`** fire twice (validate + apply).
4. **Test keypairs in production** — docs say never.
5. **`--sql-log` on `chr test`** is gone as of CLI 0.31.0.
6. **ICCF proofs are not verified in the unit-test framework** (CLI 0.15.0 note).
7. **`moduleArgs` in tests** must match a `struct module_args` in the module under test; otherwise `chain_context.args` is inaccessible.
8. **Per-test isolation** — do not rely on side effects from another `test_*` function.
9. **`@test` vs `test_` prefix** — a mis-annotated `@test` fails compilation; a misnamed `test_` is silently ignored.
10. **Enum reorder** will be blocked on `chr deployment update` (0.30.0+). Tests that depend on `.value` will break if you insert in the middle.
11. **Entity attributes are not nullable** — use a junction / optional lookup (`@?`) rather than `T?` columns.
12. **`op_context` is illegal in queries.** Use `op_context.exists` if shared code may run in either context.
13. **Size of `text` / `byte_array`** — validate early (`require(x.size() <= MAX)` or `@max_size`). Official performance/integrity advice.
14. **Cookbook `test.timeout` / `test.parallel`** are not in the project-config reference. Unverified.
15. Seeder (`chr seeder`) is documented as early-stage and may change.

There is **no** official fixture / `setUp` / `tearDown` keyword. Shared setup is ordinary functions (FT4 examples: `register_admin()`, `register_alice()`).

---

## 6. Production types: GTV, byte_array, pubkey, timestamps

### 6.1 GTV

Generic Transfer Value. Postchain serialization of operation arguments and query results. ASN.1 DER. Variants: null, byteArray, string, integer, dict, array, bigInteger.

```
GtvMessages DEFINITIONS ::= BEGIN
DictPair ::= SEQUENCE { name UTF8String, value RawGtv }
RawGtv ::= CHOICE {
    null [0] NULL,
    byteArray [1] OCTET STRING,
    string [2] UTF8String,
    integer [3] INTEGER,
    dict [4] SEQUENCE OF DictPair,
    array [5] SEQUENCE OF RawGtv,
    bigInteger [6] INTEGER
}
END
```

`gtv_type` enum (since 0.15.3): `NULL`, `BYTEARRAY`, `STRING`, `INTEGER`, `DICT`, `ARRAY`, `BIGINTEGER`. Read via `.type`.

Functions: `gtv.from_json(text|json)`, `gtv.from_bytes`, `gtv.from_bytes_or_null`, `.to_json()`, `.to_bytes()`, `.hash()`. Every GTV-compatible Rell type has `T.from_gtv` / `from_gtv_pretty` / `.to_gtv()` / `.to_gtv_pretty()` / `.hash()`.

**Conversion table (official):**

| Rell type | Operation input | Query input | Query output |
|---|---|---|---|
| `rowid` / entity | GtvInteger | GtvInteger | GtvInteger |
| enum | GtvInteger | GtvInteger **or** GtvString | **GtvString** |
| struct | GtvArray | GtvArray **or** GtvDict | **GtvDict** |
| integer | GtvInteger | GtvInteger | GtvInteger |
| big_integer | GtvBigInteger | GtvBigInteger | GtvBigInteger |
| decimal | GtvString | GtvString | GtvString |
| boolean | GtvInteger | GtvInteger | GtvInteger |
| json | GtvString | GtvString | GtvString |
| text | GtvString | GtvString | GtvString |
| byte_array | GtvByteArray | GtvByteArray | GtvByteArray |
| nullable | GtvNull or T | GtvNull or T | GtvNull or T |
| list / set | GtvArray | GtvArray | GtvArray |
| `map<text, _>` | GtvDict | GtvDict | GtvDict |
| non-text map | GtvArray of pairs | GtvArray of pairs | GtvArray of pairs |
| named tuple | GtvDict | GtvDict | GtvDict |
| unnamed tuple | GtvArray | GtvArray | GtvArray |

**Strict mode** (`compile.strictGtvConversion`, default true, since 0.13.9) — operation inputs:

| Rell | Strict accepts | Non-strict also accepts |
|---|---|---|
| byte_array | GtvByteArray | GtvString |
| integer | GtvInteger | GtvBigInteger |
| big_integer | GtvBigInteger | GtvInteger |
| decimal | GtvString | GtvInteger, GtvBigInteger |

Some Rell types are not GTV-compatible and therefore cannot be operation/query parameters or query results. Docs do not list the full exclusion set on the crawled pages; if the compiler rejects a signature, that is the authority.

Rell 0.15.0 changed GTV `big_integer` JSON serialization — clients talking JSON-GTV need a matching postchain-client.

### 6.2 `byte_array`

Binary. Hex literal: `x"0373…3b15"`. Construct from hex text, Base64, or `list<integer>` (0–255). Members: `.size()`, `.sub()`, `.to_hex()`, `.to_base64()`, `.to_list()`, `.sha256()`, `.empty()`, `.repeat()`, `.reversed()`, `[]`, `+`.

Used for: pubkeys, account IDs, blockchain RIDs, tx/block RIDs, hashes, raw GTV.

FT4-style production check from official best-practices (example, not a language rule): account/asset IDs are 32 bytes and not all-zero.

### 6.3 `pubkey`

**Type alias: `pubkey` = `byte_array`.** Not a distinct runtime type.

Compressed secp256k1 public keys in examples and in `rell.test.keypair.pub` are **33 bytes**. `rell.test.keypair.priv` is **32 bytes**.

`op_context.is_signer(pubkey)` / `op_context.get_signers()` take this type. Hex literal `x"…"` is assignable.

### 6.4 Timestamps

**Type alias: `timestamp` = `integer`.** Unix time in **milliseconds**.

Sources of time:

- `op_context.last_block_time` — last *committed* block, ms. Use this in operations for `created_at` (official examples).
- System entity `block.timestamp`.
- `rell.test.*_block_time` in tests.
- `rell.time` namespace (since 0.14.14) — UTC format/parse via `rell.time.format(pattern)`: `.ms_to_text(ms)`, `.text_to_ms(text)`, `.text_to_ms_or_null(text)`. Specifiers: `y M w W D d E u a H h m s S`. Quoted literals with `'…'`.

`__timeb` special operation: tx valid only inside a UTC ms window; client clock must be reasonably in sync with the signer. DST/timezone skew fails it.

There is **no** distinct datetime type. Do not treat `timestamp` as seconds.

### 6.5 Other aliases

- `name` = `text`
- `tuid` = `text`

### 6.6 Native-function type map (Rell 0.15.0+)

| Rell | Kotlin | Java |
|---|---|---|
| big_integer | `java.math.BigInteger` | same |
| boolean | `Boolean` | `boolean` |
| byte_array | `ByteArray` | `byte[]` |
| decimal | `java.math.BigDecimal` | same |
| gtv | `net.postchain.gtv.Gtv` | same |
| integer / rowid | `Long` | `long` |
| text | `String` | `String` |
| unit (return) | `Unit` | `void` |

Native functions **must be deterministic**. Maven coordinate: `net.postchain.rell:rell-api-native:<RELL_VERSION>`.

---

## 7. Versions actually seen (2026-08-26)

### Rell compiler / language

From [docs.chromia.com/rell/releases](https://docs.chromia.com/rell/releases) (table on that page):

| Version | Date | Production-relevant note |
|---|---|---|
| 0.16.4 | 2026-08-02 | Latest **on the docs site**. Linter + LSP fixes. |
| 0.16.1 | 2026-07-15 | Lambdas, value-block `if`/`when` arms, jump expressions. |
| 0.16.0 | 2026-05-28 | Resolved runtime model, jOOQ SQL generation, ANTLR parser. |
| 0.15.3 | 2026-03-30 | `gtv_type` / `gtv.type`. |
| 0.15.2 | 2026-02-24 | Snapshots; bundled in CLI 0.30.0. |
| 0.15.1 | 2026-02-09 | `@disabled` tests; `@mount` on attributes; size annotations on entity/object attrs; dropped columns for removed attrs. |
| 0.15.0 | 2025-12-09 | `@native`; guard during validation; big_integer JSON-GTV change. |
| 0.14.16 | 2025-11-13 | JSON `[]` / getters; `text.regex_replace`; collection `+` `-` `&`. |
| 0.14.14 | 2025-09-19 | `rell.time`; size annotations on parameters. |
| 0.14.13 | 2025-09-09 | `@compound` `@singular`; function-level `@test`; size annotations on struct attrs. |
| 0.14.3 | 2024-11-28 | Default params on operations/queries called from chain. |
| 0.13.9 | 2024-02-23 | Add/remove keys/indexes at init; strict GTV. |

From **gitlab.com/chromaway/rell** (official repo, crawled same day):

- Tags: `0.16.7`, `0.16.6`, `0.16.5`, `0.16.4`, … `0.16.0`.
- `doc/release-notes/0.16.7.txt` header: **RELEASE NOTES 0.16.7 (2026-08-14)**.
- Oldest compatibility mode cited there: **0.10.9**.
- Language-version gates (0.16.7 notes) confirm when later constructs became legal (`@native` = 0.15.0, `@mount` on attributes = 0.15.1, etc.).

`rell.get_rell_version()` / `rell.get_postchain_version()` / `rell.get_build()` are the runtime way to ask a live chain.

### Chromia CLI

From [docs.chromia.com/build/cli/cli-release-notes](https://docs.chromia.com/build/cli/cli-release-notes): latest **0.30.0 (2026-02-27)**, Rell **0.15.2**, Postchain **3.49.2**, postchain-chromia **3.39.3**.

From **gitlab.com/chromaway/core-tools/chromia-cli**:

| CLI tag | CHANGELOG date | Bundled Rell (when stated) |
|---|---|---|
| 0.33.2 | tagged; no CHANGELOG section seen on `dev` | (not stated in the TOC we fetched) |
| 0.33.1 | 2026-06-24 | (version-bump section lists postchain 3.49.16, not Rell) |
| 0.33.0 | 2026-05-29 | **rell 0.16.0** |
| 0.32.0 | 2026-04-19 | rell 0.15.3 |
| 0.31.0 | 2026-03-04 | — ; **removed `chr test --sql-log`** |
| 0.30.0 | 2026-02-27 | rell 0.15.2 |

`chr version` prints the installed CLI and component versions — that is the only way to know what a given machine actually has.

### Other versions named in official pages

- CLI requires **Java 21+**.
- PostgreSQL **16** in the Homebrew install path (`postgresql@16`).
- FT4 library docs pages still show “Rell 0.16.0” in the generated lib reference footer.
- Merkle hash **v2** is the current recommended default (`features.merkle_hash_version: 2`).
- Local test block interval default: **10 seconds**.
- First test-block timestamp default: **2020-01-01 00:00 UTC**.

---

## 8. Canonical URLs used

### Docs homepage and maps

- https://docs.chromia.com
- https://docs.chromia.com/sitemap.xml
- https://docs.chromia.com/rell — **404** (section is not at this path)

### Rell language

- https://docs.chromia.com/rell/rell-intro
- https://docs.chromia.com/rell/core-concepts
- https://docs.chromia.com/rell/modules
- https://docs.chromia.com/rell/tests
- https://docs.chromia.com/rell/releases
- https://docs.chromia.com/rell/special-operations
- https://docs.chromia.com/rell/rell-best-practices
- https://docs.chromia.com/rell/analyze-rell-dapp-code
- https://docs.chromia.com/rell/language-features/modules/entity
- https://docs.chromia.com/rell/language-features/modules/object
- https://docs.chromia.com/rell/language-features/modules/operation
- https://docs.chromia.com/rell/language-features/modules/query
- https://docs.chromia.com/rell/language-features/modules/function
- https://docs.chromia.com/rell/language-features/modules/struct
- https://docs.chromia.com/rell/language-features/modules/enum
- https://docs.chromia.com/rell/language-features/modules/mount
- https://docs.chromia.com/rell/language-features/database/create
- https://docs.chromia.com/rell/language-features/database/update
- https://docs.chromia.com/rell/language-features/database/delete
- https://docs.chromia.com/rell/language-features/types/simple-types
- https://docs.chromia.com/rell/language-features/types/complex-types
- https://docs.chromia.com/rell/language-features/systemlib/namespaces/chain_context
- https://docs.chromia.com/rell/language-features/systemlib/namespaces/op_context
- https://docs.chromia.com/rell/language-features/systemlib/namespaces/time
- https://docs.chromia.com/rell/language-features/systemlib/system-entities
- https://docs.chromia.com/rell/language-features/systemlib/system-queries
- https://docs.chromia.com/pages/rell/index.html (stdlib, generated)
- https://docs.chromia.com/pages/rell-stdlib/index.html
- https://docs.chromia.com/pages/rell/-rell%20-system%20-library/rell.test/index.html

### Project / CLI / deploy

- https://docs.chromia.com/build/configuration/project-config
- https://docs.chromia.com/build/configuration/project-structure
- https://docs.chromia.com/build/configuration/blockchain-properties
- https://docs.chromia.com/build/database/overview
- https://docs.chromia.com/build/cli/introduction
- https://docs.chromia.com/build/cli/commands/
- https://docs.chromia.com/build/cli/commands/create-rell-dapp
- https://docs.chromia.com/build/cli/commands/build
- https://docs.chromia.com/build/cli/commands/test
- https://docs.chromia.com/build/cli/commands/node
- https://docs.chromia.com/build/cli/commands/deployment
- https://docs.chromia.com/build/cli/commands/library
- https://docs.chromia.com/build/cli/commands/version
- https://docs.chromia.com/build/cli/cli-release-notes
- https://docs.chromia.com/build/cookbook/cli/create-rell-dapp
- https://docs.chromia.com/build/cookbook/cli/run-tests
- https://docs.chromia.com/get-started/installation
- https://docs.chromia.com/get-started/create-dapp/
- https://docs.chromia.com/get-started/create-dapp/run-dapp-cli
- https://docs.chromia.com/get-started/about/protocols/gtv

### Official GitLab (code + versions newer than the docs site)

- https://gitlab.com/chromaway/rell
- https://gitlab.com/chromaway/rell/-/tags
- https://gitlab.com/chromaway/rell/-/blob/dev/doc/release-notes/0.16.7.txt
- https://gitlab.com/chromaway/core-tools/chromia-cli
- https://gitlab.com/chromaway/core-tools/chromia-cli/-/tags
- https://gitlab.com/chromaway/core-tools/chromia-cli/-/blob/dev/CHANGELOG.md
- https://gitlab.com/chromaway/core-tools/homebrew-chromia.git (brew tap)
- https://gitlab.com/chromaway/core-tools/scoop-chromia.git (Windows)
- https://gitlab.com/chromaway/ft4-lib.git (FT4 examples)
- https://gitlab.com/chromaway/core/directory-chain (ICCF lib examples)
- Docker image: `registry.gitlab.com/chromaway/core-tools/chromia-cli/chr:latest`

### Pages attempted and not used as authority

- https://docs.chromia.com/rell — 404
- https://docs.chromia.com/cli — 404 (real path is `/build/cli`)
- https://docs.chromia.com/rell/language-features/database/overview — fetch timed out; SQL mapping taken from core-concepts, entity, create/update/delete, analyze-rell-dapp-code, simple-types instead
- Cookbook chromia.yml snippets that introduce keys absent from project-config — recorded as unverified, not as fact

---

## Unclear / not specified in official sources crawled

- Exact PostgreSQL column types for `text`, `byte_array`, `boolean`, `enum`, `timestamp`.
- A single published `CREATE TABLE` dump (only runtime SQL logs on the optimization page).
- Whether `chr install` is an alias of `chr library install` or a separate entry point — both names appear officially.
- Full CLI 0.33.2 changelog (tag exists; no section in the CHANGELOG TOC we fetched).
- Whether docs.chromia.com CLI pages will be updated past 0.30.0; today they lag GitLab by three minor versions.
- Cookbook keys `test.timeout`, `test.parallel`, `build.output_dir`, `build.optimize`, `database.schema_version`.
- A named “fixtures” API — does not appear.
