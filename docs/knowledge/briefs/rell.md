# Rell ready-brief (Chromia expert)

**Written:** 2026-08-26 (Asia/Jerusalem)
**Authority order:** GitLab `chromaway/rell` tag **0.16.7** (`code-rell.md`) wins over docs.chromia.com `/rell/` through **0.16.4** (`study-rell.md`). CLI / `chromia.yml` from `rell-cli.md` + `SKILL.md`. If a file is silent, this brief says so. Do not invent APIs.

**Repos:** https://gitlab.com/chromaway/rell (project `32802097`; GitHub is a read-only mirror). Default branch `dev`. Tag `0.16.7` commit `5db26c32` (2026-08-14 10:45 +02:00 = 11:45 IDT). `RellVersions.VERSION_STR = "0.16.7"` in `rell-base/utils/src/utils/RellVersions.kt`.

**Related notes:** `/workspace/chromia-knowledge/code-rell.md`, `study-rell.md`, `rell-cli.md`, `/home/box/agent-data/workflows/chromia-rell-and-cli/SKILL.md`.

---

## 1. Compiler pipeline — `S_` → 13 `C_` passes → `R_` → `RR_` → `Rt_`

Prefixes (`rell-base/README.md`, `doc/rell-architecture.md`; `code-rell.md` §2):

| Prefix | Layer | Role |
|---|---|---|
| `S_` | AST | Parsed source |
| `G_` | Grammar helpers | better-parse wrappers (positions) |
| `C_` | Compilation | AST → model, **13 ordered passes** |
| `V_` | Value-expression | Intermediate compiled expressions |
| `R_` | Compiler model | Mutable working model (lazy fields, IDE baggage) |
| `RR_` | Resolved runtime IR | Immutable, serializable; **only** model the runtime consumes |
| `Rt_` | Runtime | Interpreter, values, SQL generation |
| `M_` | Type lattice | Compile-time generics, variance, bounds |
| `L_` | Library framework | Stdlib declarations (Kotlin DSL) |
| `Db_` | DB expressions | At-expression compilation artifacts; become `RR_DbExpr` |

Documented pipeline (`doc/rell-architecture.md`):

```
Source → Parsing (S_) → Compilation (C_/V_) → Compiler model (R_) → resolve() → Resolved IR (RR_) → Interpreter (Rt_)
                                                                                     ↕ FlatBuffers
```

Invariant (`doc/rell-architecture.md`, `rell-base/rr-serialization/README.md`):

```
interpret(compile(code)) ≡ interpret(deserialize(serialize(compile(code))))
```

**Production still compiles from source every time.** `deserializeRellApp` is not used from any production path in `rell-gtx` or `rell-api-gtx`. FlatBuffers exist and are tested (`testRoundTrip` on every `./gradlew check`) but are **not** the on-chain deploy format (`rr-serialization/README.md`; `code-rell.md` §2).

Gradle split (`rell-base/README.md`): `utils ← rr-tree ← rr-serialization`; `frontend` (parser, passes, `R_`, `resolve`); `runtime` (interpreter, values, SQL, stdlib). Runtime is split into `runtime-core`, `runtime-interpreter`, `runtime-truffle`. Truffle is a second backend; the **interpreter is the production one** (`code-rell.md` §2). Architecture doc still says “runtime.”

### 1.1 Entry — `C_Compiler.compile`

File: `rell-base/frontend/src/main/kotlin/compiler/base/core/c_compiler.kt` (`code-rell.md` §3.1)

1. `C_LibBridge.ensureInitialized()`
2. Load mid-modules (`C_ModuleLoader`)
3. Check main modules (abstract cannot be main; test module cannot be main unless `ide` / `appModuleInTestsError` is off)
4. Compile mid → ext modules
5. `C_AppContext` + `C_ExtModuleCompiler.compileModules()`
6. `controller.run()` — drain the 13-pass queue
7. If no errors: `resolve(rApp, resolverRuntime)` → `RR_App`
8. Return `C_CompilationResult(rrApp, rApp, messages, files, …, compilationSysFns)`

### 1.2 The 13 `C_CompilerPass` values (same file, in order)

`DEFINITIONS` → `NAMESPACES` → `MODULES` → `MEMBERS` → `ABSTRACT` → `APPDEFS` → `EXPRESSIONS` → `FRAMES` → `DOCS` → `COMPLETIONS` → `VALIDATION` → `APPLICATION` → `FINISH`

`C_CompilerController.run()` executes each pass’s queue to empty, then close callbacks. `onPass` that targets a pass later than `current+1` is re-queued onto the next pass so later-scheduled work still runs in definition order (`c_compiler.kt` comment).

`c_app.kt` hooks (`code-rell.md` §3.2):

- `NAMESPACES`: assemble namespace graph
- `ABSTRACT`: `C_AbstractCompiler.compile`
- `APPDEFS`: build app defs + `C_StructGraphUtils.processStructs`
- `VALIDATION`: global constants
- `APPLICATION`: `createApp()` → `R_App`

### 1.3 Parser

`rell-base/frontend/src/main/antlr` exists. Docs 0.16.0 said “ANTLR mainline parser.” `DEVELOPMENT.md` still mentions grammar helpers wrapping better-parse. `rell-toolbox/ast` has a separate ANTLR grammar checked by `:rell-toolbox:ast:grammarTest`. Compiler frontend itself: `rell-base/frontend/src/main/kotlin/compiler/parser/` (`grammar.kt`, `tokenizer.kt`, `RellAntlrVisitor.kt`) (`code-rell.md` §3.3).

### 1.4 Compilation does **not** emit SQL

It emits `R_App` then `RR_App`. SQL is generated **at runtime** from `RR_DbExpr` trees (`doc/rell-architecture.md`: “The interpreter generates parameterized SQL directly from these trees”). Docs intro “compiler translates Rell into SQL” is the wrong layer (`code-rell.md` §12.2). Playground “SQL dry-run” is this interpreter path without a DB (`study-rell.md` §7.1).

Calculations **must be deterministic**. Expressions must produce the same results in interpreted and database contexts. Locale-sensitive Java APIs are a consensus hazard (`DEVELOPMENT.md`; `-PwithLocales` runs `tr_TR`, `ar_SA`, `ja_JP`).

---

## 2. How Rell becomes SQL at runtime (`DbSqlGen` / jOOQ)

Three SQL surfaces (`code-rell.md` §4):

1. **DDL / schema init** — `SqlGen` + `SqlInit` (`rell-base/runtime-core/src/main/kotlin/sql/sql_gen.kt`, `sql_init.kt`)
2. **DQL / DML at runtime** — `DbSqlGen` in the interpreter (`rell-base/runtime-interpreter/src/runtime/rr_interp_sql_gen.kt`)
3. **System SQL helpers** — PL/pgSQL functions named `rell_*` plus per-chain `c<id>.make_rowid` / `make_rowids`

### 2.1 Table-name formula (source-defined, not inferred)

`Rt_ChainSqlMapping` in `rell-base/runtime-core/src/main/kotlin/runtime/rt_common.kt`:

```
prefix = "c" + chainId + "."
fullName(mountName) = prefix + mountName.str()
```

Pinned by `rell-base/src/test/kotlin/sql/SqlEmissionTest.kt`: `"c0.user"`, `"c0.company"`, `"c0.emp"`, `"c0.data"`. Analyze-page log: `"c0.housekey"`, `"c0.owner"` (`study-rell.md` §14). Dots stay **inside** the quoted identifier (`"c0.foo.user"`). Mount max **58** because PG identifier max is 63 and the shortest prefix is `c0.` (3 chars) — `s_def.kt` `MAX_ENTITY_MOUNT_NAME_LEN = 58`. Attribute / attribute-`@mount` max **63**. Gated since 0.12.0 (`NAME_LEN_SWITCH`) (`code-rell.md` §11.3).

`rt_entity_sql_table.kt`: regular/external entities use `chainMapping.fullName(mountName)`; system `transaction` / `block` use `transactionsTable` / `blocksTable` (`c<id>.transactions`, `c<id>.blocks`). Rowid column for user entities is `"rowid"`; for system tx/block it is `tx_iid` / `block_iid` (`r_sql.kt`).

System tables on the same prefix (`rt_common.kt`): `c<id>.rowid_gen`, `c<id>.make_rowid`, `c<id>.make_rowids`, `c<id>.blocks`, `c<id>.transactions`, `c<id>.sys.classes`, `c<id>.sys.attributes`. Plus `R_SqlConstants.SYSTEM_CHAIN_TABLES`: `events`, `states`, `event_pages`, `snapshot_pages`, `configurations`, `gtx_module_version`.

Reserved mount names (`c_def_mount.kt`): `R_SqlConstants.SYSTEM_OBJECTS` plus `"block"` and `"transaction"` (back-compat), and any name starting with `sys`. Conflict is a compiler error when `mountConflictError` is on.

### 2.2 Entity → `CREATE TABLE` (`SqlGen.genEntity`)

`sql_gen.kt`:

- Table name = `entity.sqlMapping.table(sqlCtx)`
- Column `rowid BIGINT NOT NULL`
- Constraint `PK_<tableName> PRIMARY KEY (rowid)`
- One column per attribute: name = `attr.sqlMapping` (attribute `@mount` or the attribute name — `c_def_entity.kt`: `sqlMapping = mainDef.sqlMapping ?: mainDef.name.str`)
- SQL type from `interpreter.resolveType(attr.type).sqlAdapter.sqlType`, forced `.nullable(false)`
- Entity-typed attributes → `FOREIGN KEY (col) REFERENCES <refTable>(rowid)` named `<table>_<col>_FK` (when `addFkConstraints`)
- Size constraints → `CHECK` on `length(col)` (text) or `octet_length(col)` (byte_array), compared as `BIGINT` (a previous `toInt()` truncated `@max_size` > 2^31 and inverted the predicate)
- Keys → `UNIQUE` named `K_<table>_%d`
- Indexes → `CREATE INDEX IDX_<table>_%d`. Single-column **json** attributes get `USING gin (<col> jsonb_path_ops)` instead of a B-tree

**All columns are `NOT NULL`.** That is the SQL-level reason entity attributes cannot be nullable (`code-rell.md` §4.2).

### 2.3 Rell type → jOOQ / PostgreSQL

From `Rt_*Value` companions (`sqlType`) (`code-rell.md` §4.3):

| Rell | jOOQ `sqlType` | File |
|---|---|---|
| `boolean` | `SQLDataType.BOOLEAN` | `rt_value_boolean.kt` |
| `integer` | `SQLDataType.BIGINT` | `rt_value_integer.kt` |
| `rowid` | `SQLDataType.BIGINT` | `rt_value_rowid.kt` |
| `text` | `SQLDataType.CLOB` | `rt_value_text.kt` |
| `byte_array` | `SQLDataType.BLOB` | `rt_value_bytearray.kt` |
| `json` | `SQLDataType.JSONB` | `rt_value_json.kt` |
| `decimal` | `Lib_DecimalMath.DECIMAL_SQL_TYPE` | `rt_value_decimal.kt` |
| `big_integer` | `NUMERIC` (via `rell_biginteger_*` in `sql_gen.kt`; companion not re-read in that study) | `sql_gen.kt` |
| entity / enum | entity = rowid `BIGINT`; enum = integer adapter (`rt_type_gtv.kt` stub) | |

**json split (do not collapse):** DDL type is `JSONB`; bind uses `PGobject` with `type = "json"` (not `"jsonb"`) (`rt_value_json.kt`). Docs simple-types said PostgreSQL `JSON` — **source wins: JSONB** (`code-rell.md` §12.3).

`r_type.kt` `computeSqlInfo()`: SQL-compatible **and** allowed as entity attributes: boolean, integer, big_integer, decimal, text, byte_array, rowid, json, entity, enum. Nullable is SQL-compatible only if language ≥ 0.13.10 (`NULLABLE_SQL_SWITCH`) but **`allowedForEntityAttributes = false` always**. Everything else (`range`, `gtv`, collections, tuples, structs, functions, virtual, unit, …) is `R_TypeSqlInfo.NONE`.

`text` → jOOQ `CLOB`, `byte_array` → jOOQ `BLOB` (PG `text` / `bytea` in practice) (`code-rell.md` §11.8). Docs `/rell/` pages are **silent** on exact PG types for `text` / `byte_array` / `boolean` / `enum` (`study-rell.md` §18.5).

### 2.4 Rowid = counter table, not a SEQUENCE

`sql_gen.kt` `genRowidSql` (`code-rell.md` §4.4):

- Table `c<id>.rowid_gen` with `last_value BIGINT NOT NULL`, inserted as `0`
- Function `c<id>.make_rowid()`: `UPDATE … SET last_value = last_value + 1 RETURNING last_value` (SQL language)
- Function `c<id>.make_rowids(n BIGINT)`: rejects `n <= 0 OR n >= 1000000000`; increments by `n`; returns first new id (`v - n + 1`)

Per-chain counter table. Deletes do **not** rewind it. Matches docs “strictly greater than last allocated” (`study-rell.md` §5.1), but docs call it a “per-blockchain **sequence**” — source is a counter table + function, not a PostgreSQL `SEQUENCE`. Bulk insert uses `make_rowids` (`code-rell.md` §11.4). `rowid(integer)` rejects negatives; interned 0…1000 (`rt_value_rowid.kt`).

`ParameterizedSql` (`rt_sql_builder.kt`) refuses more than **32767** bind parameters (PSQL driver limit).

### 2.5 Runtime SELECT — `DbSqlGen`

`rr_interp_sql_gen.kt` (`code-rell.md` §4.5):

- Walks `RR_DbExpr` to jOOQ `Field` / `Condition`
- Aliases `A00`, `A01`, … (`"A%02d"`)
- Relationship hops (`a.b.c`) become JOINs tracked per root alias
- Binds collected in tree-traversal order; SQL is parameterized (`?`)
- `LIMIT` / `OFFSET` appended after render
- Aliases are **unquoted** in `FROM "c0.user" A00` so `A00."name"` matches PG case-folding (`columnField` comment)

Pinned USER SQL (`SqlEmissionTest.kt`):

```
select A00."rowid" from "c0.user" A00 where A00."name" = ?
select A00."rowid" from "c0.emp" A00 join "c0.company" A01 on A00."company" = A01."rowid" where A01."name" = ?
select A00."rowid", A01."rowid" from "c0.emp" A00, "c0.company" A01 where A00."company" = A01."rowid" …
select COALESCE(SUM(A00."k"),0) from "c0.data" A00
select A00."k" from "c0.data" A00 order by A00."k", A00."rowid" LIMIT ?
```

`@sum 1` becomes a bound long (`binds=[long]`), not a SQL literal `1`. `when`/`if` in WHERE becomes `CASE WHEN`. Path access becomes `JOIN`. Two-entity from-list becomes a **comma join** (old-style), not `INNER JOIN` syntax, unless it is a path/outer join. Outer join is the `@outer` form only.

`chr repl --sql-log` is the USER stream `SqlEmissionTest` captures (SYS vs USER interceptor categories). CLI ≥0.31 **removed** `chr test --sql-log` (`rell-cli.md` §4.4).

### 2.6 System functions are write-once by name

`sql_gen.kt` comment: “When changing a function, change its name e.g. to fn_v2. Functions in the database are not upgraded — a function is created only once, if there is no function with the same name.” Global `rell_*` functions “are now created by `RellGlobalStorageInitializer` at node startup, in a committed transaction before any blockchain starts, to avoid deadlocks” (`sql_init.kt` `processFunctions`). Chain-specific `make_rowids` is still created per chain.

Named functions (`r_sql_constants.kt` / `sql_gen.kt`): `rell_integer_power`, `rell_biginteger_from_text`, `rell_biginteger_power`, `rell_decimal_from_text`, `rell_decimal_to_text`, `rell_bytea_substr1/2`, `rell_text_repeat`, `rell_text_substr1/2`, `rell_text_getchar`, `rell_json_array_get` / `_or_null`, `rell_json_object_get`, `rell_json_as_{integer,big_integer,boolean_or_null,text}[_or_null]`, `rell_json_size`.

Behaviors encoded in those bodies: `integer.pow` / `big_integer.pow` negative exponent → exception; `text`/`byte_array` `sub`/`[]` 0-based, out of range → exception (not SQL `SUBSTR` silent clip); `json` array get index must be in `0..2147483647` (PG `INT` max, **not** Rell `integer`/`BIGINT`); `decimal.to_text` in SQL strips trailing zeros via regexp.

### 2.7 Schema init / evolution (`SqlInit.init`)

**Must** be wrapped in a transaction (comment: mid-step failure can otherwise leave partial DDL). Plan (`sql_init.kt`; `code-rell.md` §4.7):

1. Optionally drop all entity tables + wipe meta (`isSnapshot`)
2. Discover existing chain tables + functions
3. Create `rowid_gen` / `make_rowid` / meta tables if empty DB
4. Per entity (topological): create table or evolve
5. Per object: same + insert the singleton row if new
6. Warn on tables whose meta exists but code no longer defines them (`dbinit:no_code`)

Meta lives in `c<id>.sys.classes` / `c<id>.sys.attributes`. `checkOldConstraints` hard-codes lookup as `"c0.$metaName"` — that call site assumes chain id 0 (`code-rell.md` §11.19).

Create/update/delete SQL text (`rr_interp_db_write.kt`) was **not fetched** in `code-rell.md` §14 — do not invent those statements.

---

## 3. Entity rules that bite in production

Sources: entity page (`study-rell.md` §5.1), `sql_init.kt` evolution table (`code-rell.md` §4.7), SKILL “Production rules that bite.”

### 3.1 Attributes cannot be nullable

Stated on https://docs.chromia.com/rell/language-features/types/complex-types (“to ensure data integrity”), **not** restated on the entity page (`study-rell.md` §5.1 / §17.3). Source: `allowedForEntityAttributes = false` always; DDL forces `.nullable(false)` (`r_type.kt`, `sql_gen.kt`). Use defaults or a sentinel. Optional lookup is `@?`, not `T?` columns.

### 3.2 An entity variable is a **rowid**, not the row

Entity page: a variable of entity type holds the **rowid** (primary key), not the attribute values. Access: `u.rowid` or `user @ { .name == 'Alice' } ( .rowid )`. Entity-typed attributes store the referenced rowid + FK (`code-rell.md` §4.2). Entity GTV encodes as rowid and `trackRecord`s so the runtime can verify the row exists (`rt_type_gtv.kt`). `==` on entities compares object IDs (`study-rell.md` §8.1).

### 3.3 `@log entity`

Adds an implicit `transaction` attribute; entity becomes **immutable and non-deletable** (entity page). Adding or removing `@log` after deploy is incompatible (`meta:entity:diff_log`). Confirmed in source (`code-rell.md` §12.12).

### 3.4 Schema evolution (entity page + `sql_init.kt`)

**Docs-compatible (entity page):**

- Add attributes **with defaults** (existing rows get the default)
- Add attributes to **empty** tables (defaults not required)
- Remove attributes
- Change mutability on entities that are **not** `@log`

**Docs-incompatible:**

- Change an attribute's **type**
- Add or remove `@log`

**Source fills the docs-silent cases** (`sql_init.kt`; `code-rell.md` §4.7):

| Change | Behavior |
|---|---|
| Entity ↔ object type flip | Error `meta:entity:diff_type` |
| `@log` flip | Error `meta:entity:diff_log` |
| Attribute SQL type change | Error `meta:attr:diff_type` (compares adapter `metaName`) |
| Removed attribute | Language ≥ 0.15.1 (`REMOVED_ATTRS_DROP_COLUMNS_SWITCH`): `ALTER TABLE DROP` columns + delete meta. Else: warning `dbinit:no_code:attrs` (columns left in place) |
| New attribute, table empty | Add column |
| New attribute, rows exist, has default | `ADD COLUMN` + `UPDATE` fill + `SET NOT NULL` |
| New attribute, rows exist, no default | Error `meta:attr:new_no_def_value` |
| New attribute is key/index | Error unless key/index-change switch is on (0.13.9+) |
| Size constraint tighten on non-empty | Error (`min` increased or `max` decreased or newly added) |
| Size constraint relax / drop | Drop + re-add CHECK |
| Keys / indexes | Drop missing, create new (after the 0.13.9 switch) |

**Adding** a size constraint to a non-empty table is an error (`size_constraint_added_records_exist`). Removing a constraint is allowed (drop CHECK) (`code-rell.md` §12.9). Docs size-constraint page: cannot tighten on non-empty; can relax.

Attribute `@mount` (since 0.15.1) names the **column** so a rename does not drop data (`study-rell.md` §4.4). Keys, indexes, metadata, and size constraints use the mapped column name.

Object: exactly one instance; auto-initialized; **cannot** `create`/`delete` from code; every attribute **requires a default** (`study-rell.md` §5.2).

### 3.5 Enums

`.name: text`, `.value: integer` (declaration index). Enum page is **silent** on schema evolution (`study-rell.md` §5.4). CLI 0.30.0+ `chr deployment update` treats reorder/remove as dangerous because `.value` is the stored form (`rell-cli.md` §1.9). Source: enum SQL = integer adapter; **ops take the integer `.value` only**; queries accept name or index (`code-rell.md` §6, §11.12). Reorder/remove changes the stored integer **and** the compact GTV form.

### 3.6 Keys / indexes / relations

`key` = unique + index; `index` = non-unique. Composite order is left-prefix. Combined `key first_name: text, last_name: text;` cannot also mark those fields `mutable`. 1-1: both sides `key`. 1-many: `index` on the many side. Many-many: junction with `key left, right`. Index only what you filter or join on; WHERE fields should be `key`; JOIN fields `index`; most selective predicate first (`study-rell.md` §2, §14; SKILL).

---

## 4. Module system

### 4.1 Kinds (`study-rell.md` §4.1; `c_module_loader.kt` / `c_module.kt`)

- **Single-file module:** a `.rell` file that starts with `module;`
- **Directory module:** all `.rell` files in a directory that do **not** themselves have a module header. Exception: `module.rell` always belongs to the directory module, even if it has a header. A directory module does **not** require `module.rell`.
- **Root module:** directory module of `.rell` files in the source-root; **empty name**.
- Every file of a directory module sees all other files in that module. A single-file module sees only its own definitions.

Recommended layout (`study-rell.md` §4.1): `app/module.rell` (imports + mounts), `entities.rell`, `operations.rell`, `queries.rell`, `functions.rell`, `structs.rell` when > ~3 structs.

In `chromia.yml`, the blockchain entry is the **module name** (`main`, `app`), **never a file path** (`rell-cli.md` §1.1; SKILL).

### 4.2 Runtime activity — only main + import closure

Only the **main module** (named when starting the app / in `blockchains.<name>.module`) and modules it imports, directly or indirectly, are **active**. Inactive modules contribute neither operations/queries nor tables (`study-rell.md` §4.1).

Source: selected modules = main module (+ optional submodules) plus imported modules. `C_CompilerModuleSelection.appSubModules` defaults **false**; `testSubModules` defaults **true** (`c_module_loader.kt`; `code-rell.md` §7).

**Anonymous import** (0.13.12+): `import _: foo;` activates the module (ops, queries, function extensions, overrides) **without adding any names**. No alias; cannot be referenced from code (`study-rell.md` §4.3).

### 4.3 Source-only loader rules (`code-rell.md` §7)

- Parent module is the **nearest existing ancestor** (not necessarily the immediate parent).
- **Test modules do not inherit a parent** (`parentName = null` when `header.test`).
- `disabled` is inherited: child is disabled if it or any ancestor is.
- `mountName` inherits from parent unless the module header sets `@mount`.
- Abstract module cannot be main (`module:main_abstract`) except in IDE mode.
- Test module cannot be main (`module:main_test`) when `appModuleInTestsError`.
- `include` definition: compile error “Include not supported since Rell 0.10.0” (`s_def.kt` `S_IncludeDefinition`).
- `module_args` is the struct named `module_args` (`C_Constants.MODULE_ARGS_STRUCT`); validated on `VALIDATION` pass.
- Mount conflicts across files/modules: first user entry wins after error; system entries always kept (`c_def_mount.kt`).

`@external` (docs: mentioned, **no syntax page** — `study-rell.md` §4.6 / §18.1). Source: `C_ModuleKey` carries optional `extChain`. `R_EntitySqlMapping_External` (`r_sql.kt`) reads the **other chain’s** table and adds `block_height <= <linked.height>` via join to that chain’s `transactions`/`blocks`. External entities `autoCreateTable() = false`. Tests: `lang/module/ExternalModuleTest.kt`, `lang/def/ExternalTest.kt`. **Do not invent the syntax** beyond what `r_sql.kt` implements (`code-rell.md` §12.8).

Mount names are fixed at the **definition** site, not at import. Relative: `.` appends, `^` pops one segment. Special ops require `__` prefix on the *operation name*; prefer dots in `@mount` (`@mount('icmf.message') operation __icmf_message() {}`) (`study-rell.md` §4.4, §11).

---

## 5. Ops vs queries vs functions; `op_context` / `chain_context`

### 5.1 Operation (write API)

Canonical: https://docs.chromia.com/rell/language-features/modules/operation (`study-rell.md` §5.5)

- Can modify the database
- Does **not** return a value
- Parameter types **must be GTV-compatible**
- Default parameters allowed; **new defaults must be appended** (op page + releases 0.14.3)
- Typical auth: take a `pubkey`, `require(op_context.is_signer(user_pubkey))`
- `@compound` — cannot be the only kind of op in a tx (0.14.13)
- `@singular` — at most once per tx (0.14.13)
- Both may be combined
- Size-constraint annotations on `text` / `byte_array` params (0.14.14)

**Guard** (`guard { … }`): read-only argument verification. Runs during Postchain `checkCorrectness()` **and** again during `apply()` — `print`/`log` execute **twice**. Releases 0.15.0: also during operation validation. Only *declarations* (not assignments) may appear before `guard`. Guard may **read** the DB, not write. `op_context.emit_event()` is illegal in a guard. Variables needed after the guard must be queried again in the body.

**GTV:** operations expect **compact-encoded** GTV (`Lib_Type_Gtv` stdlib comment; `code-rell.md` §6). Compact: struct / named-field tuple = **array of values**; op named tuple **rejects** dict (`ARRAY:DICT`); enum compact = **integer index only**.

### 5.2 Query (read API)

Canonical: https://docs.chromia.com/rell/language-features/modules/query (`study-rell.md` §5.6)

- Cannot modify the database (**compile-time check**)
- Must return a value (type inferred if omitted)
- Parameter types **and** return type must be GTV-compatible
- Short form `query q(x: integer): integer = x * x;` or full form with `return`
- Default parameters allowed. **Query page is silent on append-only**; releases 0.14.3 says both ops and queries “must be placed at the end of the parameter list.” Source 0.16.7: omitting defaulted op/query args is version-gated to 0.14.3, with **compilerVersion exemption** for older configs (`code-rell.md` §12.10)

**GTV:** queries expect **pretty-encoded** GTV. Pretty: struct / named-field tuple = **dict keyed by field name**. Enum query pretty accepts name `"A"` or index `0`. Query output of enum is **GtvString**; of struct is **GtvDict** (`rell-cli.md` §6.1). Same struct is an array in an op and a dict in a query (`code-rell.md` §11.11).

### 5.3 Function

Canonical: https://docs.chromia.com/rell/language-features/modules/function (`study-rell.md` §5.7)

- Reusable logic. Can modify the DB when called from an operation. Callable from queries, operations, and other functions.
- No explicit return type ⇒ `unit`
- Default parameters, named arguments, function values, partial application (`f(*)`, `f(123, *)`)
- `@extendable` / `@extend`: `unit` = all + base; `boolean` = first `true` else base; `T?` = first non-null; `list<T>` concat; `map<K,V>` union, **fails on key conflict**
- `@test` on a function: must be in a `@test` module; no parameters; cannot combine with `@extendable`/`@extend`/`abstract`/`override`. Violations are **compiler errors**. A misnamed `test_` prefix is silently ignored.
- `@native`: Java/Kotlin implementation. Mapped in `blockchains.<name>.config.gtx.rell.native` (module name → JVM class). Class public; constructor `()` or `(RellNativeEnvironment)`; **every public** `memberFunctions` entry becomes a native function (`rell-gtx/.../module/native.kt` `PostchainNativeUtils`). Call is `fn.call(self, *args)` — reflection. No further sandbox. Determinism is a docs comment, **not enforced** (`code-rell.md` §8). `RellNativeEnvironment { config: Gtv; blockchainRid: BlockchainRid }` (`rell-api-native/.../native_functions.kt`). Maven: `net.postchain.rell:rell-api-native:<RELL_VERSION>`.

### 5.4 `op_context`

Canonical: https://docs.chromia.com/rell/language-features/systemlib/namespaces/op_context (`study-rell.md` §9.6)

**Only legal in an operation or a function called from an operation — not in a query.** Use `op_context.exists` to test.

| Member | Role |
|---|---|
| `block_height` | height of the block being built |
| `last_block_time` | timestamp of the **last committed** block, milliseconds |
| `transaction` | the transaction being built |
| `exists` | running in an operation context? |
| `op_index` | index of this operation in the tx |
| `get_signers()` | all signing pubkeys |
| `is_signer(pubkey)` | did this key sign? |
| `get_all_operations()` | all ops in the current tx |
| `get_current_operation()` | this op (0.13.3) |
| `emit_event` | emit to Postchain components (illegal inside `guard`) |

**Do not** read `op_context.transaction.block` except `block_height` — other block attributes are null during the building block and throw. Use `op_context.block_height` and `op_context.last_block_time` (op_context page + SKILL).

### 5.5 `chain_context`

Canonical: https://docs.chromia.com/rell/language-features/systemlib/namespaces/chain_context (`study-rell.md` §9.5)

- `chain_context.args` — **this module's** `module_args` struct, filled from `chromia.yml` `moduleArgs`. Access is only possible if that module defines `struct module_args { … }`. You cannot read `args` if `module_args` is not defined. Defaults may be omitted; if every attribute has a default, the args section may be omitted entirely. Every module can have its own `module_args`; the type of `args` differs per module.
- `chain_context.blockchain_rid: byte_array`
- `chain_context.raw_config: gtv` — raw blockchain config

---

## 6. Docs vs source contradictions — source wins

`study-rell.md` is a faithful study of docs.chromia.com `/rell/` through **0.16.4**. Source 0.16.7 does not flip the language the docs describe; it fills gaps and dates the docs lag. Only items below are **real differences** (`code-rell.md` §12).

### 6.1 Version surface

- Docs latest: **0.16.4 (2026-08-02)**. GitLab tags: **0.16.5, 0.16.6, 0.16.7 (2026-08-14)**. `study-rell.md` §18.11 already flagged this as a gap.
- 0.16.5 (`doc/release-notes/0.16.5.txt`, 2026-08-04): formatter/linter/LSP/error-message only. **No language semantics.**
- 0.16.6 (2026-08-07): LSP. **Language:** lambdas, value-block `if`/`when` arms, jump expressions now require `gtx.rell.version ≥ 0.16.1` (they had shipped ungated). **Can break** a node with `gtx.rell.version < 0.16.1` that already used those constructs.
- 0.16.7 (2026-08-14): smaller deps (jackson-databind removed from compiler/runtime; jOOQ no longer re-exported). LSP honors `compile.rellVersion`. More constructs version-gated **retroactively without breaking old configs** (`compilerVersion` exemption). See §7.

### 6.2 “Compiler translates Rell into SQL”

Docs intro: compiler translates Rell into SQL. Source: compiler translates Rell into `R_` then `RR_`; the **interpreter** generates SQL. Same user-visible effect, **wrong layer**. Playground “SQL dry-run” is this interpreter path without a DB.

### 6.3 PostgreSQL types the docs left blank / got wrong

| Type | Docs | Source (wins) |
|---|---|---|
| `integer` / `rowid` | 64-bit; no jOOQ name | `BIGINT` |
| `boolean` | silent | `BOOLEAN` |
| `text` | silent | jOOQ `CLOB` (PG `text` in practice) |
| `byte_array` | silent | jOOQ `BLOB` (PG `bytea` in practice) |
| `json` | PostgreSQL **`JSON`** | jOOQ **`JSONB`**; bind `PGobject(type="json")` |
| `decimal` / `big_integer` | `NUMERIC` | `NUMERIC` (docs already right) |
| `enum` | silent | integer adapter (stored as `.value` index) |
| entity ref | “column holding the referenced rowid” | `BIGINT` FK to the other table’s `rowid` |

**Hard contradiction:** docs simple-types / gap list say `json` → PostgreSQL `JSON`. DDL uses `JSONB`. Treat JSONB as the schema type.

### 6.4 Table-name formula

`study-rell.md` §17.2 called `"c<iid>.<mount>"` an inference from the analyze-page log. Source **defines** it: `Rt_ChainSqlMapping.prefix = "c$chainId."`. Not an inference anymore.

### 6.5 Mount / attribute length

Docs: mount max 58 from releases 0.13.11; entity/attribute max lengths from 0.12.0 not restated. Source: mount **58** (reason: `c0.` + PG 63), attribute and attribute-`@mount` **63**, switch `0.12.0`. 0.13.11 release note is the docs date for mount; the constant lives next to the 0.12.0 switch.

### 6.6 GTV exclusion set (`study-rell.md` §18.3)

Docs: “some types are not GTV-compatible”; only `virtual<T>` called out as not a query return. Source `r_type.kt` `computeDirectGtvCompatibility`:

- **FULL** (from + to): boolean, integer, big_integer, decimal, text, byte_array, rowid, json, gtv, entity, enum, null, nullable, struct, tuple, list, set, map
- **FROM_ONLY** (Merkle proof in, cannot `to_gtv`): virtual list/set/map/struct/tuple — so virtual **can** be an **operation input** (virtual-types page already said this)
- **NONE:** `range`, function types, `unit`, `object`, `operation`, `nothing`, `guid`, `signer`

Entity **is** GTV (as rowid). Collections/tuples/structs are GTV iff **every** component is (`TypeFlags.combine`). `virtualable`: map is virtualable only if key type is `text`. `range` is not virtualable.

### 6.7 GTV wire table

`/rell/` pages do not publish it. Source tests + `Lib_Type_Gtv` comment **are** the table (`code-rell.md` §6, `rell-cli.md` §6.1). Compact vs pretty; boolean = GTV **integer** 0/1 (GTV has no boolean kind — `gtv_type` is NULL/BYTEARRAY/STRING/INTEGER/DICT/ARRAY/BIGINTEGER); decimal = **text**; json = GTV **string** of JSON text, not a dict (`json('{"x":123}')` query result is `"{\"x\":123,…}"`; passing a dict as `json` is `gtv_err:type:[json]:STRING:DICT`); enum name-vs-index; struct array-vs-dict. `byte_array` accepts GTV bytes **or** hex string on input; query output is hex string `"12EF"`. Mixed named+unnamed tuple always encodes as array even in pretty.

`rell-cli.md` table is consistent with source, **not** a contradiction with language docs (docs were silent). Note: `rell-cli.md` query-output `byte_array` as `GtvByteArray` vs source test pinning query output as hex string `"12EF"` — prefer source `GtvRtConversionTest` when they diverge (`code-rell.md` §6).

Strict mode (`compile.strictGtvConversion`, default true, since 0.13.9): `byte_array` `asByteArray(convert = !strict)`; integer→decimal and big_integer→decimal allowed only when `!strictGtvConversion` (`gtx_conversion.kt`). Leave `strictGtvConversion: true` in production (SKILL / `rell-cli.md` §3.3).

### 6.8 `@external`

Docs: mentioned, no syntax page. Source: `@external` modules, `C_ModuleKey.extChain`, `R_EntitySqlMapping_External` height-filtered joins. Do not reconstruct syntax beyond `r_sql.kt` + those tests.

### 6.9 Size-constraint relax-only

Docs: cannot tighten on non-empty; can relax. Source matches, **and**: **adding** a constraint to a non-empty table is an error (`size_constraint_added_records_exist`). Removing a constraint is allowed (drop CHECK).

### 6.10 Default params must be appended

Docs: operation page says append; query page silent; releases 0.14.3 says both. Source 0.16.7: omitting defaulted op/query args is version-gated to 0.14.3, with **compilerVersion exemption** for older configs — a pre-0.16.7 config keeps accepting omitted defaults even if its declared language version is older.

### 6.11 `list._sort()`

`study-rell.md` §18.13: collection-types page writes `._sort()`. **Not verified** in the files fetched for the source study. Do not rename.

### 6.12 Not contradictions (source confirms docs)

Cardinality (`@` / `@?` / `@+` / `@*`), `@log` immutability, object singleton + required defaults, entity attr non-null, key=unique+index, attribute `@mount` as column name, bulk `create`, guard dual-run, `@compound`/`@singular`, abstract/override, anonymous import, mount inheritance `.` / `^`, `MIN_COMPATIBILITY` 0.10.9, integer overflow throws, test `@test` / `@disabled`.

### 6.13 “Safe SQL translation” wording

Docs: compiler does a *safe* translation. Source: jOOQ + parameterized binds + `@OptIn(RawSqlAccess)` poison-pill for string SQL. Safety is “no string-concat of user values into SQL”, not “no SQL injection surface in identifiers” (identifiers come from `MountName` / `sqlMapping`, validated as `Name`).

### 6.14 Docs-vs-docs / CLI lag (not language contradictions)

- `https://docs.chromia.com/rell` is **404**. Use `/rell/rell-intro`, `/rell/language-features`, `/rell/tests`, `/build/cli` (SKILL / `rell-cli.md`).
- CLI docs on docs.chromia.com stop at **0.30.0** (2026-02-27, bundled Rell **0.15.2**). GitLab tags through **0.33.2**. 0.33.0 (2026-05-29) bumped bundled Rell to **0.16.0** (`rell-cli.md` header / §7).
- Cookbook keys `database.schema_version`, `test.timeout`, `test.parallel`, `build.output_dir`, `build.optimize` are **not** on the project-config page. Unverified. Do not rely on them (SKILL / `rell-cli.md` §3.9).
- `chr test --verbose` is **not** on the official `chr test` usage block. Unverified (`rell-cli.md` §4.4).

---

## 7. Versions — `RellVersions`, Java 21, what `chr` bundles

### 7.1 `RellVersions.kt` (source, tag 0.16.7)

File: `rell-base/utils/src/utils/RellVersions.kt` (`code-rell.md` §10)

- `VERSION_STR = "0.16.7"`
- `SUPPORTED_VERSIONS`: 0.6.0 … 0.16.7 (every listed patch)
- `MIN_COMPATIBILITY_VERSION = 0.10.9`
- `MIN_COMPILER_VERSION = 0.13.11` (`gtx.rell.compilerVersion` era)
- `MODULE_SYSTEM_VERSION_STR = 0.10.0`
- `RETROACTIVE_GATES_VERSION = VERSION` (currently 0.16.7)

`C_FeatureRestrictions.makeRetroactive` (`c_utils_restrictions.kt`): a feature that shipped **without** a version gate is not rejected when recompiling a historical config whose `compilerVersion` predates the gate. New compilations are checked. Exists so replaying a chain does not halt on configs that compiled cleanly when written.

0.16.7 retroactive gates (`doc/release-notes/0.16.7.txt`; `code-rell.md` §10) — **do not break historical configs**:

| Construct | Since |
|---|---|
| `+ - &` (and `+= -=`) on list/set/map | 0.14.16 |
| `[]` on json | 0.14.16 |
| Omitting defaulted op/query args from a chain call | 0.14.3 |
| `@compound` `@singular` | 0.14.13 |
| size annotations on struct attrs | 0.14.13 |
| size annotations on parameters | 0.14.14 |
| `@native` | 0.15.0 |
| size annotations on entity/object attrs | 0.15.1 |
| `@mount` on entity/object attrs | 0.15.1 |

Named copies (`add_all_copy` etc., `json.get`) were gated all along; only the operator spellings were not.

Runtime version queries (`study-rell.md` §9.4): `rell.get_rell_version()`, `rell.get_postchain_version()`, `rell.get_build()`, `rell.get_build_details()`. `chr version` prints the installed CLI and component versions — **that is the only way to know what a given machine actually has** (`rell-cli.md` §7).

`compile.rellVersion` is the language/compiler compatibility version written into the blockchain config (`gtx.rell.version` / `compilerVersion`). Deployment refuses a version higher than the target cluster supports (`rell-cli.md` §3.3). SKILL example: `rellVersion: "0.16.0"` — “must not exceed the target cluster.”

### 7.2 Java / PostgreSQL

- Rell `README.md` (tag 0.16.7): requires **Java 21** and Docker (PostgreSQL / Testcontainers) (`code-rell.md` §1).
- `chr` requires **Java 21+** (`RELL_JAVA` overrides) (`rell-cli.md` §4; SKILL).
- Local persistence: **PostgreSQL 16+** (Homebrew path `postgresql@16`; SKILL). Official default local DB: database/user/password all `postchain`. Env overrides: `CHR_DB_URL` / `CHR_DB_USER` / `CHR_DB_PASSWORD` / `CHR_DB_SCHEMA`.

### 7.3 What `chr` bundles (`rell-cli.md` §7; SKILL)

| CLI tag | CHANGELOG date | Bundled Rell (when stated) |
|---|---|---|
| 0.33.2 | tagged; no CHANGELOG section seen on `dev` | (not stated in the TOC fetched) |
| 0.33.1 | 2026-06-24 | (version-bump lists postchain 3.49.16, **not** Rell) |
| **0.33.0** | 2026-05-29 | **rell 0.16.0** |
| 0.32.0 | 2026-04-19 | rell 0.15.3 |
| 0.31.0 | 2026-03-04 | — ; **removed `chr test --sql-log`** |
| 0.30.0 | 2026-02-27 | rell 0.15.2 (docs-site latest CLI; also Postchain 3.49.2, postchain-chromia 3.39.3) |

**Do not assume an installed `chr` has Rell 0.16.7.** 0.33.0 is the last CLI release that *states* a Rell bump, and that bump is **0.16.0**. Confirm with `chr version`. Docs CLI pages describe the 0.30.x-era surface.

Install: Homebrew tap `https://gitlab.com/chromaway/core-tools/homebrew-chromia.git`; apt `https://apt.chromia.com`; Scoop `https://gitlab.com/chromaway/core-tools/scoop-chromia.git`; Docker `registry.gitlab.com/chromaway/core-tools/chromia-cli/chr`. Official code lives on **GitLab**, not GitHub.

---

## 8. What an expert must not get wrong when answering Or

These are the ones that produce wrong answers if guessed.

1. **Compile target is RR IR, not SQL.** A dapp is not a pile of stored SQL. Production compiles from source every time. FlatBuffers RR is tested, hardened, unused on the GTX path (`rr-serialization/README.md`).
2. **Table name is `c<chainIid>.<mount>`**, including dots inside the quoted identifier. Mount max 58; attribute/`@mount` max 63.
3. **Rowid is a counter table + function**, not a PG `SEQUENCE`. Bulk insert uses `make_rowids` with `n ∈ (0, 10^9)`.
4. **json column DDL is JSONB** + GIN `jsonb_path_ops` for a single-column json index; bind uses `PGobject type="json"`. Docs said `JSON`.
5. **Ops = compact GTV (arrays); queries = pretty GTV (dicts).** Same struct is an array in an op and a dict in a query. Enum: ops take integer `.value`; queries accept name or index; query output is the name string.
6. **boolean on the wire is 0/1 integer. decimal on the wire is text. json on the wire is a JSON string, not a dict.**
7. **Entity attributes cannot be nullable.** Entity variable = rowid, not the row. `@log` flip after deploy is a hard error.
8. **Only main module + import closure is active.** `import _: mod;` activates without names. `chromia.yml` `module:` is a module name, never a path. Chain names **cannot contain hyphens**.
9. **`op_context` is illegal in queries.** `chain_context.args` is only readable inside the module that declared `struct module_args`.
10. **Do not read `op_context.transaction.block` except `block_height`.** Use `op_context.block_height` and `op_context.last_block_time` (ms). `timestamp` is an alias for `integer` milliseconds. `pubkey` is an alias for `byte_array` (33-byte compressed in examples / `rell.test.keypair.pub`; priv is 32 bytes).
11. **Guards run twice** (checkCorrectness + apply). `@compound` cannot be the only op in a tx. `@singular` at most once.
12. **Do not reorder or remove production enum values.** `.value` is stored and is the compact GTV form.
13. **New default params append only.** New attributes on non-empty tables need defaults. Type change / `@log` flip = incompatible. Removed attrs drop columns only since 0.15.1.
14. **`include` is a hard error** since the module system (0.10.0). Test modules have **no parent**. Disabled inherits.
15. **Native functions = all public Kotlin methods** on a configured class, via reflection. Determinism not enforced. Constructor `()` or `(RellNativeEnvironment)` only.
16. **32767 SQL bind-parameter cap.** json `[]` index is truncated to PG INT (0..2147483647). Size CHECK uses `::BIGINT` length.
17. **`@sum 1` is a bound parameter**, not a literal. Two-entity from-list is `,` join; path access is `JOIN ON`. `if`/`when` inside at-expr → SQL `CASE`.
18. **System SQL functions are write-once by name**; globals created at **node** startup, not per-chain init. Snapshot init (`isSnapshot`) drops every entity table then recreates.
19. **`GUID` / `SIGNER` primitive kinds exist in `RR_Type` with no `Rt_*Value` class.** `NOTHING` is the bottom type of jump expressions. `list<T>` is **not** assignable to `list<T?>`.
20. **Lambdas / jump exprs / value-block arms** shipped in 0.16.1; 0.16.6 **retroactively gates** them to `gtx.rell.version ≥ 0.16.1` and can break older pins. 0.16.7 extra gates do **not** break historical configs.
21. **Docs site is 0.16.4; source is 0.16.7.** CLI docs are 0.30.x; GitLab CLI is 0.33.2 and last *stated* Rell bundle is **0.16.0**. Confirm with `chr version` / `rell.get_rell_version()`.
22. **`https://docs.chromia.com/rell` is 404.** Code is GitLab, not GitHub. Do not invent flags, YAML keys, or SQL column types the files are silent on (`text`/`byte_array`/`enum` were docs-silent; source now fills them — cite source).
23. **Cookbook-only YAML keys are unverified.** `merkle_hash_version: 2` (v1 deprecated, hash collisions). Chromia max block 26 MiB (`maxblocksize: 27262976`); `max_transaction_size: 26214400`.
24. **Test isolation:** no official fixtures API. `rell.test.*` only visible in `@test` modules. Test keys never ship. ICCF proofs are **not** verified in the unit-test framework (CLI 0.15.0 note). Ops do nothing until `.run()`.
25. **`code-rell.md` §14 still not read:** full `rr_interp_db_write.kt` (create/update/delete SQL text), `sql_meta.kt` meta schema, `c_module_ext.kt` `@external` syntax, Truffle backend semantics, `Lib_DecimalMath` exact NUMERIC precision, enum SQL adapter class body, linter rule implementations, special-operation dispatch, `rell.test` implementation constants. Those remain “not in this study,” not “do not exist.”

### Cardinality / at-expr (must not botch)

`FROM @ CARDINALITY { WHERE } ( WHAT ) TAIL`. `@` exactly one (abort + rollback if not); `@?` 0–1; `@+` 1+; `@*` 0+. Cardinality is tested **before** `limit`. Empty WHAT → entity reference (rowid). `@list`/`@set`/`@map` in a **database** at-expression: all matching rows are read, then grouped/aggregated **in memory**. Nested `@*` with `empty()`/`exists()` compiles to a **single** SQL query; `@`/`@?`/`@+` become separate queries. `@outer` join → type `T?`. Logical ops are words: `and` / `or` / `not` (not `&&` / `||`).

### Aliases (`study-rell.md` §6.1)

`pubkey` = `byte_array`; `name` = `text`; `timestamp` = `integer` (ms); `tuid` = `text`. There is **no** distinct datetime type.

### Special operations (`study-rell.md` §11)

`__begin_block(height: integer)`, `__end_block(height: integer)`, `__icmf_message`, `__evm_block`, `__timeb`. Global namespace — always `@mount('ext.name')` + `__` prefix. Parameter lists besides begin/end_block are **not** on the special-operations page (`study-rell.md` §18.14–15). Do not invent them.

### `chromia.yml` production subset (SKILL / `rell-cli.md` §3)

Required: `blockchains`. `moduleArgs` target must declare `struct module_args`. `compile.rellVersion` must not exceed the target cluster. `compile.strictGtvConversion: true` in production. Secrets: never commit keys or DB passwords. Allowed GTX modules on Chromia (blockchain-properties page): `RellPostchainModuleFactory`, `StandardOpsGTXModule`, `IcmfSenderGTXModule`, `IcmfReceiverGTXModule`, `IccfGTXModule`, `EifGTXModule`, `WebStaticGTXModuleFactory`.

---

## Source file map

Quoted names come from these fetched paths (`code-rell.md` §13) plus the docs URLs listed in `study-rell.md` §19.

| Path | Why |
|---|---|
| `doc/rell-architecture.md`, `rell-base/README.md`, `DEVELOPMENT.md` | Prefixes, pipeline |
| `rell-base/utils/src/utils/RellVersions.kt` | 0.16.7, supported set, min compat |
| `rell-base/frontend/.../c_compiler.kt` | 13 passes, `C_Compiler.compile` |
| `rell-base/frontend/.../c_app.kt` | App assembly hooks |
| `rell-base/frontend/.../c_module.kt`, `c_module_loader.kt` | Module load/select/parent/test |
| `rell-base/frontend/.../c_def_entity.kt`, `c_def_mount.kt`, `s_def.kt` | Attr sqlMapping, reserved mounts, 58/63 |
| `rell-base/frontend/.../c_utils_restrictions.kt` | Retroactive gates |
| `rell-base/frontend/.../r_sql.kt`, `r_sql_constants.kt`, `r_type.kt` | SQL mapping, GTV/SQL flags |
| `rell-base/rr-tree/src/main/rr_type.kt` | RR type union |
| `rell-base/rr-serialization/README.md` | FlatBuffers unused in prod |
| `rell-base/runtime-core/.../rt_common.kt` | `c<id>.` prefix |
| `rell-base/runtime-core/.../sql_gen.kt`, `sql_init.kt` | DDL + evolution |
| `rell-base/runtime-core/.../rt_sql_builder.kt` | 32767 param cap |
| `rell-base/runtime-interpreter/.../rr_interp_sql_gen.kt` | `DbSqlGen` |
| `rell-base/runtime-core/.../rt_value_{integer,boolean,text,bytearray,json,decimal,rowid}.kt` | SQL/GTV adapters |
| `rell-base/runtime-core/.../lib/type/lib_type_gtv.kt` | Compact vs pretty |
| `rell-gtx/.../module/native.kt` | Native reflection |
| `rell-base/src/test/kotlin/sql/SqlEmissionTest.kt` | Pinned SQL |
| `rell-base/src/test/kotlin/lang/misc/GtvRtConversionTest.kt` | Pinned GTV |
| `doc/release-notes/0.16.{5,6,7}.txt` | Post-docs-site releases |
