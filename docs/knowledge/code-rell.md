# Rell compiler / runtime — source study

**Written:** 2026-08-26 (Asia/Jerusalem)  
**Repo:** https://gitlab.com/chromaway/rell (GitLab project id `32802097`)  
**Method:** GitLab API + raw files only. No `git clone`. Tree: 3232 entries / 2499 blobs at tag `0.16.7`.  
**Default branch:** `dev` (project `default_branch`).  
**Latest tag:** `0.16.7` (commit `5db26c32`, 2026-08-14 10:45 +02:00 = 11:45 IDT). Tags after docs-site 0.16.4: `0.16.5`, `0.16.6`, `0.16.7`.  
**Version in source:** `RellVersions.VERSION_STR = "0.16.7"` in `rell-base/utils/src/utils/RellVersions.kt`.  
**Do not invent.** Every behavior below is quoted from a file that was fetched. Paths are repo-relative.

Related notes: `/workspace/chromia-knowledge/study-rell.md` (official `/rell/` docs through 0.16.4). Contradictions vs that file are in §12.

---

## 1. What this repository is

`README.md` (tag 0.16.7): Rell is “the relational blockchain programming language for Chromia.” It “combines a SQL-like data model with a statically typed, imperative syntax, and compiles against a PostgreSQL-backed runtime.” License GPLv3. Requires Java 21 and Docker (PostgreSQL / Testcontainers).

`DEVELOPMENT.md`: Postchain is the container that runs blockchain modules; Chromia is Postchain + Rell + extras; Rell is the language. Two key features: blockchain integration (operations, queries, `chain_context` / `op_context`) and SQL-like capabilities (entity/object, at-expressions, DB writes).

Canonical repo is GitLab. GitHub is a read-only mirror.

Gradle multi-module (`settings.gradle.kts`): `rell-base` (split), `rell-api-*`, `rell-gtx`, `rell-tools`, `rell-toolbox/*`, `rell-codegen/*`, `rell-dokka-plugin`, `performance`, `regression`.

---

## 2. Architecture (layers and prefixes)

`rell-base/README.md` and `doc/rell-architecture.md` agree on the prefixes:

| Prefix | Layer | Role |
|---|---|---|
| `S_` | AST | Parsed source |
| `G_` | Grammar helpers | better-parse wrappers (positions) |
| `C_` | Compilation | AST → model, 13 ordered passes |
| `V_` | Value-expression | Intermediate compiled expressions |
| `R_` | Compiler model | Mutable working model (lazy fields, IDE baggage) |
| `RR_` | Resolved runtime IR | Immutable, serializable; **only** model the runtime consumes |
| `Rt_` | Runtime | Interpreter, values, SQL generation |
| `M_` | Type lattice | Compile-time generics, variance, bounds |
| `L_` | Library framework | Stdlib declarations (Kotlin DSL) |
| `Db_` | DB expressions | At-expression compilation artifacts; become `RR_DbExpr` |

`doc/rell-architecture.md` pipeline:

```
Source → Parsing (S_) → Compilation (C_/V_) → Compiler model (R_) → resolve() → Resolved IR (RR_) → Interpreter (Rt_)
                                                                                     ↕ FlatBuffers
```

Key invariant (`doc/rell-architecture.md`, `rell-base/rr-serialization/README.md`):

```
interpret(compile(code)) ≡ interpret(deserialize(serialize(compile(code))))
```

`rell-base/rr-serialization/README.md` also states: **`deserializeRellApp` is not used from any production path in `rell-gtx` or `rell-api-gtx`. Production compiles from source each time.** FlatBuffers exist and are tested (`testRoundTrip` on every `./gradlew check`) but are not the on-chain deploy format today.

Sub-module dependency (`rell-base/README.md`, `doc/rell-architecture.md`):

```
utils ← rr-tree ← rr-serialization
              ↑
         frontend (parser, passes, R_, resolve)
              ↑
         runtime (interpreter, values, SQL, stdlib)
```

`runtime` is split in Gradle into `runtime-core`, `runtime-interpreter`, `runtime-truffle`. The official architecture doc still says “runtime.” Truffle is a second backend; the interpreter is the production one described below.

`DEVELOPMENT.md` naming: calculations in the implementation **must be deterministic**. Expressions must produce the same results in interpreted and database contexts. Locale-sensitive Java APIs are a consensus hazard (`-PwithLocales` runs `tr_TR`, `ar_SA`, `ja_JP`).

---

## 3. Compiler pipeline

### 3.1 Entry

`rell-base/frontend/src/main/kotlin/compiler/base/core/c_compiler.kt` — `C_Compiler.compile`:

1. `C_LibBridge.ensureInitialized()`
2. Load mid-modules (`C_ModuleLoader`)
3. Check main modules (abstract cannot be main; test module cannot be main unless `ide` / `appModuleInTestsError` is off)
4. Compile mid → ext modules
5. `C_AppContext` + `C_ExtModuleCompiler.compileModules()`
6. `controller.run()` — drain the 13-pass queue
7. If no errors: `resolve(rApp, resolverRuntime)` → `RR_App`
8. Return `C_CompilationResult(rrApp, rApp, messages, files, …, compilationSysFns)`

### 3.2 Passes (`C_CompilerPass`)

Same file, in order:

`DEFINITIONS` → `NAMESPACES` → `MODULES` → `MEMBERS` → `ABSTRACT` → `APPDEFS` → `EXPRESSIONS` → `FRAMES` → `DOCS` → `COMPLETIONS` → `VALIDATION` → `APPLICATION` → `FINISH`

`C_CompilerController.run()` executes each pass’s queue to empty, then close callbacks. `onPass` that targets a pass later than `current+1` is re-queued onto the next pass so later-scheduled work still runs in definition order (comment in that file).

`c_app.kt` hooks:

- `NAMESPACES`: assemble namespace graph
- `ABSTRACT`: `C_AbstractCompiler.compile`
- `APPDEFS`: build app defs + `C_StructGraphUtils.processStructs`
- `VALIDATION`: global constants
- `APPLICATION`: `createApp()` → `R_App`

### 3.3 Parser

`rell-base/frontend/src/main/antlr` exists. Docs 0.16.0 said “ANTLR mainline parser.” `DEVELOPMENT.md` still mentions grammar helpers wrapping better-parse, and `rell-toolbox/ast` has a separate ANTLR grammar checked by `:rell-toolbox:ast:grammarTest` against a corpus recorded by the runtime tests. The compiler frontend itself lives under `rell-base/frontend/src/main/kotlin/compiler/parser/` (`grammar.kt`, `tokenizer.kt`, `RellAntlrVisitor.kt`).

### 3.4 What compilation does **not** produce

Compilation does **not** emit SQL. It emits `R_App` then `RR_App`. SQL is generated at runtime from `RR_DbExpr` trees (`doc/rell-architecture.md`: “The interpreter generates parameterized SQL directly from these trees”).

---

## 4. How Rell becomes SQL

Three separate SQL surfaces:

1. **DDL / schema init** — `SqlGen` + `SqlInit` (create tables, functions, meta, evolve schema)
2. **DQL / DML at runtime** — `DbSqlGen` in the interpreter (at-expressions, create/update/delete)
3. **System SQL helpers** — PL/pgSQL functions named `rell_*` plus per-chain `c<id>.make_rowid` / `make_rowids`

### 4.1 Table name formula (confirmed in source)

`rell-base/runtime-core/src/main/kotlin/runtime/rt_common.kt` `Rt_ChainSqlMapping`:

```
prefix = "c" + chainId + "."
fullName(mountName) = prefix + mountName.str()
```

Examples pinned by `rell-base/src/test/kotlin/sql/SqlEmissionTest.kt`: `"c0.user"`, `"c0.company"`, `"c0.emp"`, `"c0.data"`. Docs analyze page showed `"c0.housekey"` — same formula.

System tables on the same prefix (`rt_common.kt`): `c<id>.rowid_gen`, `c<id>.make_rowid`, `c<id>.make_rowids`, `c<id>.blocks`, `c<id>.transactions`, `c<id>.sys.classes`, `c<id>.sys.attributes`. Plus `R_SqlConstants.SYSTEM_CHAIN_TABLES`: `events`, `states`, `event_pages`, `snapshot_pages`, `configurations`, `gtx_module_version`.

`rt_entity_sql_table.kt`: regular/external entities use `chainMapping.fullName(mountName)`; `transaction` / `block` use `transactionsTable` / `blocksTable` (`c<id>.transactions`, `c<id>.blocks`). Rowid column for user entities is `"rowid"`; for system tx/block it is `tx_iid` / `block_iid` (`r_sql.kt`).

### 4.2 Entity → `CREATE TABLE`

`rell-base/runtime-core/src/main/kotlin/sql/sql_gen.kt` `SqlGen.genEntity`:

- Table name = `entity.sqlMapping.table(sqlCtx)`
- Column `rowid BIGINT NOT NULL`
- Constraint `PK_<tableName> PRIMARY KEY (rowid)`
- One column per attribute: name = `attr.sqlMapping` (attribute `@mount` or the attribute name — `c_def_entity.kt` line `sqlMapping = mainDef.sqlMapping ?: mainDef.name.str`)
- SQL type from `interpreter.resolveType(attr.type).sqlAdapter.sqlType`, forced `.nullable(false)`
- Entity-typed attributes → `FOREIGN KEY (col) REFERENCES <refTable>(rowid)` named `<table>_<col>_FK` (when `addFkConstraints`)
- Size constraints → `CHECK` on `length(col)` (text) or `octet_length(col)` (byte_array), compared as `BIGINT` (comment: a previous `toInt()` truncated `@max_size` > 2^31 and inverted the predicate)
- Keys → `UNIQUE` named `K_<table>_%d`
- Indexes → `CREATE INDEX IDX_<table>_%d`. Single-column **json** attributes get `USING gin (<col> jsonb_path_ops)` instead of a B-tree.

All columns are `NOT NULL`. That is the SQL-level reason entity attributes cannot be nullable.

### 4.3 Rell type → PostgreSQL / jOOQ type

From the `Rt_*Value` companions (`sqlType`):

| Rell | jOOQ `sqlType` | File |
|---|---|---|
| `boolean` | `SQLDataType.BOOLEAN` | `rt_value_boolean.kt` |
| `integer` | `SQLDataType.BIGINT` | `rt_value_integer.kt` |
| `rowid` | `SQLDataType.BIGINT` | `rt_value_rowid.kt` |
| `text` | `SQLDataType.CLOB` | `rt_value_text.kt` |
| `byte_array` | `SQLDataType.BLOB` | `rt_value_bytearray.kt` |
| `json` | `SQLDataType.JSONB` | `rt_value_json.kt` |
| `decimal` | `Lib_DecimalMath.DECIMAL_SQL_TYPE` | `rt_value_decimal.kt` |
| `big_integer` | (numeric; companion not re-read here; docs + `sql_gen` `rell_biginteger_*` functions use `NUMERIC`) | `sql_gen.kt` |
| entity / enum | via SQL adapter (entity = rowid BIGINT; enum = integer adapter in `rt_type_gtv.kt` stub) | |

`rt_value_json.kt` binds through `PGobject` with `type = "json"` (not `"jsonb"`) even though the DDL type is `JSONB`. That split is in source; do not collapse it.

`r_type.kt` `computeSqlInfo()`: SQL-compatible **and** allowed as entity attributes: boolean, integer, big_integer, decimal, text, byte_array, rowid, json, entity, enum. Nullable is SQL-compatible only if language ≥ 0.13.10 (`NULLABLE_SQL_SWITCH`) but **`allowedForEntityAttributes = false` always**. Everything else (`range`, `gtv`, collections, tuples, structs, functions, virtual, unit, …) is `R_TypeSqlInfo.NONE`.

### 4.4 Rowid allocation

`sql_gen.kt` `genRowidSql`:

- Table `c<id>.rowid_gen` with `last_value BIGINT NOT NULL`, inserted as `0`
- Function `c<id>.make_rowid()`: `UPDATE … SET last_value = last_value + 1 RETURNING last_value` (SQL language)
- Function `c<id>.make_rowids(n BIGINT)`: rejects `n <= 0 OR n >= 1000000000`; increments by `n`; returns first new id (`v - n + 1`)

This is a per-chain counter table, not a PostgreSQL `SEQUENCE`. Deletes do not rewind it. Matches the docs “strictly greater than last allocated” claim.

`ParameterizedSql` (`rt_sql_builder.kt`) refuses more than **32767** bind parameters (PSQL driver limit; connection becomes invalid past that).

### 4.5 Runtime SELECT (at-expressions)

`rell-base/runtime-interpreter/src/runtime/rr_interp_sql_gen.kt` `DbSqlGen`:

- Walks `RR_DbExpr` to jOOQ `Field` / `Condition`
- Aliases `A00`, `A01`, … (`"A%02d"`)
- Relationship hops (`a.b.c`) become JOINs tracked per root alias
- Binds collected in tree-traversal order; SQL is parameterized (`?`)
- `LIMIT` / `OFFSET` appended after render
- Aliases are **unquoted** in `FROM "c0.user" A00` so that `A00."name"` matches PG case-folding (comment in `columnField`)

Pinned SQL from `SqlEmissionTest.kt` (USER category only — SYS/DDL is not captured):

```
select A00."rowid" from "c0.user" A00 where A00."name" = ?
select A00."rowid" from "c0.emp" A00 join "c0.company" A01 on A00."company" = A01."rowid" where A01."name" = ?
select A00."rowid", A01."rowid" from "c0.emp" A00, "c0.company" A01 where A00."company" = A01."rowid" …
select COALESCE(SUM(A00."k"),0) from "c0.data" A00
select A00."k" from "c0.data" A00 order by A00."k", A00."rowid" LIMIT ?
select A00."rowid" from "c0.data" A00 where case when A00."k" = ? then A00."v" = ? else A00."v" = ? end …
```

`@sum 1` becomes a bound long (`binds=[long]`), not a SQL literal `1`. `when`/`if` in WHERE becomes `CASE WHEN`. Path access becomes JOIN. Two-entity from-list becomes a comma join (old-style), not `INNER JOIN` syntax, unless it is a path/outer join.

### 4.6 System SQL functions (created once, never upgraded)

`sql_gen.kt` comment: “When changing a function, change its name e.g. to fn_v2. Functions in the database are not upgraded — a function is created only once, if there is no function with the same name.”

`sql_init.kt` `processFunctions`: chain-specific `make_rowids` is still created per chain. **Global** `rell_*` functions “are now created by `RellGlobalStorageInitializer` at node startup, in a committed transaction before any blockchain starts, to avoid deadlocks when multiple blockchains initialize concurrently.”

Named functions (`r_sql_constants.kt` / `sql_gen.kt`): `rell_integer_power`, `rell_biginteger_from_text`, `rell_biginteger_power`, `rell_decimal_from_text`, `rell_decimal_to_text`, `rell_bytea_substr1/2`, `rell_text_repeat`, `rell_text_substr1/2`, `rell_text_getchar`, `rell_json_array_get` / `_or_null`, `rell_json_object_get`, `rell_json_as_{integer,big_integer,boolean_or_null,text}[_or_null]`, `rell_json_size`.

Behaviors encoded in those bodies:

- `integer.pow` / `big_integer.pow`: negative exponent → exception
- `text`/`byte_array` `sub`/`[]`: 0-based; out of range → exception (not SQL `SUBSTR` silent clip)
- `text.repeat`: negative count → exception
- `json` array get: index must be in `0..2147483647` (PG `INT` max, not Rell `integer`/`BIGINT`)
- `decimal.to_text` in SQL: strip trailing zeros via regexp
- `json` integer test: `(value :: TEXT) ~ '^-?\d+$'` (integral JSON number only)

### 4.7 Schema init / evolution (`sql_init.kt`)

`SqlInit.init` **must** be wrapped in a transaction (comment: mid-step failure can otherwise leave partial DDL).

Plan:

1. Optionally drop all entity tables + wipe meta (`isSnapshot`)
2. Discover existing chain tables + functions
3. Create `rowid_gen` / `make_rowid` / meta tables if empty DB
4. Per entity (topological): create table or evolve
5. Per object: same + insert the singleton row if new
6. Warn on tables whose meta exists but code no longer defines them (`dbinit:no_code`)

Evolution of an **existing** entity:

| Change | Behavior |
|---|---|
| Entity ↔ object type flip | Error `meta:entity:diff_type` |
| `@log` flip | Error `meta:entity:diff_log` |
| Attribute SQL type change | Error `meta:attr:diff_type` (compares adapter `metaName`) |
| Removed attribute | If language ≥ 0.15.1 (`REMOVED_ATTRS_DROP_COLUMNS_SWITCH`): `ALTER TABLE DROP` columns + delete meta. Else: warning `dbinit:no_code:attrs` (columns left in place) |
| New attribute, table empty | Add column |
| New attribute, rows exist, has default | `ADD COLUMN` + `UPDATE` fill + `SET NOT NULL` |
| New attribute, rows exist, no default | Error `meta:attr:new_no_def_value` |
| New attribute is key/index | Error unless key/index-change switch is on (0.13.9+) |
| Size constraint tighten on non-empty | Error (`min` increased or `max` decreased or newly added) |
| Size constraint relax / drop | Drop + re-add CHECK |
| Keys / indexes | Drop missing, create new (after the 0.13.9 switch) |

Meta lives in `c<id>.sys.classes` / `c<id>.sys.attributes`. Size-constraint lookup in `checkOldConstraints` hard-codes the table as `"c0.$metaName"` — that call site assumes chain id 0.

Reserved mount names (`c_def_mount.kt`): `R_SqlConstants.SYSTEM_OBJECTS` plus `"block"` and `"transaction"` (back-compat), and any name starting with `sys`. Conflict is a compiler error when `mountConflictError` is on.

---

## 5. Type system (what source actually encodes)

### 5.1 `RR_Type` (`rell-base/rr-tree/src/main/rr_type.kt`)

Sealed: `Primitive(kind)`, `Null`, `Entity(defIndex)`, `Struct`, `Enum`, `Object`, `Nullable`, `List`, `Set`, `Map`, `Tuple`, `Function`, `VirtualList/Set/Map/Struct/Tuple`, `Generic`, `Operation`, `Error`.

`RR_PrimitiveKind`: `BOOLEAN INTEGER BIG_INTEGER DECIMAL TEXT BYTE_ARRAY ROWID GUID SIGNER JSON GTV RANGE UNIT NOTHING`.

`GUID` and `SIGNER` have **no** `Rt_*Value` companion (`rt_primitive_types.kt` returns null). `NOTHING` is the bottom type of jump expressions (`return`/`break`/`continue` as expr); “no value of this kind ever exists at runtime.”

### 5.2 GTV compatibility (`r_type.kt` `computeDirectGtvCompatibility`)

**FULL** (from + to): boolean, integer, big_integer, decimal, text, byte_array, rowid, json, gtv, entity, enum, null, nullable, struct, tuple, list, set, map.

**FROM_ONLY** (Merkle proof in, cannot `to_gtv`): virtual list/set/map/struct/tuple.

**NONE**: everything else — including `range`, `function`, `unit`, `object`, `operation`, `nothing`, `guid`, `signer`.

This is the exclusion set the docs page does not list.

Combinators (`identifiers.kt` `TypeFlags.combine`): a composite is GTV-compatible only if **every** component is.

`virtualable`: map is virtualable only if key type is `text` (`r_builtin_types.kt`). `range` is not virtualable.

### 5.3 Assignability extras

`R_NothingType` is assignable to every type (`r_type.kt` `getTypeAdapter`). `integer` → `big_integer` / `decimal`; `big_integer` → `decimal` (`r_builtin_types.kt`).

`list<T>` is not assignable to `list<T?>` in the usual sense: `isAssignableFrom` requires `elementType.isAssignableArg` — confirm before treating as covariant. Source uses `isAssignableArg` on the element.

### 5.4 Interning / value representation (runtime)

- `integer`: interned cache −1000…1000 (`rt_value_integer.kt`)
- `rowid`: interned 0…1000; constructor rejects negative (`rt_value_rowid.kt`)
- `boolean`: `TRUE` / `FALSE` singletons
- `text`: empty interned; `like` is regex with `_` / `%` (SQL LIKE), not Rell regex
- `byte_array`: iterates as **unsigned** 0–255 integers
- `decimal`: `BigDecimal` scaled by `Lib_DecimalMath`; overflow message “allowed range is −10^p..10^p, exclusive”

SQL null decoding treats JDBC `0`/`false` as maybe-null via `Rt_SqlNull.check` for integer/rowid/boolean (because JDBC primitive getters cannot distinguish 0 from NULL).

---

## 6. GTV wire behavior (from tests + stdlib comments)

`rell-base/runtime-core/src/main/kotlin/lib/type/lib_type_gtv.kt` (stdlib comment, shipped as generated docs too):

> Rell operations expect their arguments as **compact-encoded** GTV, whereas queries expect **pretty-encoded** GTV arguments.

Compact vs pretty:

- Struct / named-field tuple: compact = **array of values**; pretty = **dict keyed by field name**
- Unnamed tuple: always array
- `map<text, V>`: dict; `map<K,V>` with non-text key: list of `[k,v]` pairs
- `set` / `list`: array
- `decimal`: **text** (`"123.456"`, `"123"` for `123.0`)
- `boolean`: GTV **integer** 0/1 (`rt_value_boolean.kt` `GtvInteger`)
- `json`: GTV **string** of JSON text, not a dict (`GtvRtConversionTest`: `json('{"x":123}')` query result is `"{\"x\":123,…}"`; passing a dict as `json` is `gtv_err:type:[json]:STRING:DICT`)
- `byte_array`: GTV bytes **or** hex string on input (`12EF` / `12ef`); query output is hex string `"12EF"`
- `enum`: **query** pretty accepts name `"A"` or index `0`; **operation** compact accepts **integer index only** (`"A"` → `INTEGER:STRING`)
- Mixed named+unnamed tuple always encodes as array even in pretty (`(x=123,[4,5,6])` → `[123,[4,5,6]]`)

`GtvRtConversionTest.kt` also pins:

- Query named tuple `{"x":123,"y":"Hello"}` works; missing key `tuple_nokey`; wrong count `tuple_count`
- Op named tuple **rejects** dict (`ARRAY:DICT`)
- `set` duplicate → `gtv_err:set_dup`
- `map<text,integer>` JSON object: **last value wins** on duplicate keys (`"A":1` then `"A":3` → 3). JSON object `{1:2}` coerces key `1` to text `"1"`
- Struct query: missing attr `struct_noattr`; extra key `struct_badkey`; type errors include `attr:[Struct]:field`
- Struct op: size mismatch `struct_size:foo:2:2:1`
- `boolean` GTV `2` is `bad_value:2`
- Cyclic structs are allowed (null-terminated)

`gtx_conversion.kt` `GtvToRtContext` carries `pretty` and `strictGtvConversion`. `byte_array` `asByteArray(convert = !strict)`. Integer→decimal and big_integer→decimal allowed only when `!strictGtvConversion`.

Entity GTV: `rt_type_gtv.kt` encodes as rowid and `trackRecord`s the entity so the runtime can verify the row exists (conversion tracks records, then `finish`).

Virtual types: fromGtv only (Merkle proofs). Cannot be a query return (`GtvCompatibility.FROM_ONLY` + docs).

---

## 7. Module system (source)

`c_module_loader.kt` / `c_module.kt`:

- File module vs directory module. Parent module is the nearest existing ancestor (not necessarily the immediate parent).
- **Test modules do not inherit a parent** (`parentName = null` when `header.test`).
- `disabled` is inherited: child is disabled if it or any ancestor is.
- `mountName` inherits from parent unless the module header sets `@mount`.
- Selected modules = main module (+ optional submodules) plus imported modules. `C_CompilerModuleSelection.appSubModules` defaults **false**; `testSubModules` defaults **true**.
- Abstract module cannot be main (`module:main_abstract`) except in IDE mode.
- Test module cannot be main (`module:main_test`) when `appModuleInTestsError`.
- `include` definition: compile error “Include not supported since Rell 0.10.0” (`s_def.kt` `S_IncludeDefinition`).
- `module_args` is the struct named `module_args` (`C_Constants.MODULE_ARGS_STRUCT`); validated on `VALIDATION` pass.
- Mount conflicts across files/modules: first user entry wins after error; system entries always kept (`c_def_mount.kt`).

External modules: `C_ModuleKey` carries optional `extChain`. `R_EntitySqlMapping_External` (`r_sql.kt`) reads the **other chain’s** table and adds `block_height <= <linked.height>` via join to that chain’s `transactions`/`blocks`. External entities `autoCreateTable() = false`. This is the `@external` implementation the docs sitemap does not document.

---

## 8. Native functions

Docs describe `@native` + `blockchains.<name>.config.gtx.rell.native`. Source:

- `rell-api-native/src/main/kotlin/native_functions.kt`: public `RellNativeEnvironment { config: Gtv; blockchainRid: BlockchainRid }`
- `rell-gtx/src/main/kotlin/module/native.kt` `PostchainNativeUtils`:
  - Config dict: module name → JVM class name
  - Class must have a public constructor `()` or `(RellNativeEnvironment)`
  - Every **public** `memberFunctions` entry becomes a native function named as the Kotlin method (`FullName(module, QualifiedName.of(it.name))`)
  - Parameters: no optional, no vararg, must be `VALUE` kind
  - Call is `fn.call(self, *args)` — reflection
  - Type match is checked at bind time against `RR_FunctionHeader` (`rt_context.kt` `Rt_NativeFunctionHeader.check`)

There is no further sandbox in this file. Determinism is a comment in docs, not enforced.

---

## 9. Tests (what they lock)

Under `rell-base/src/test/kotlin/`:

| Area | Paths | What they pin |
|---|---|---|
| SQL emission | `sql/SqlEmissionTest.kt` | Exact USER SQL + bind **types** (consensus-stable SQL) |
| SQL init | `sql/SqlInitTest.kt`, `SqlInitVersionControlTest.kt`, `SqlSizeConstraintTest.kt` | Schema evolution, version switches, size CHECKs |
| GTV | `lang/misc/GtvRtConversionTest.kt`, `runtime/RtTypeGtvConversionParityTest.kt`, `lib/LibGtvTest.kt` | Compact vs pretty, strict mode |
| Modules | `lang/module/{Module,Import,ModuleDir,Abstract,External,Test}ModuleTest.kt` | Loader rules, `@external` |
| At-expr | `lang/expr/atexpr/*` | Cardinality, joins, group, exists, collection-at |
| Types | `lang/type/*`, `mtype/*` | Lattice, lambdas, virtual, decimal SQL |
| Lib | `lib/Lib*.kt` | Stdlib, crypto, time, require, test DSL |
| RR | `model/rr/RR_ResolveTest.kt`, `RR_InterpreterTest.kt` | resolve() / interpret parity |
| Locale | `check -PwithLocales` | Determinism |
| Grammar | `:rell-toolbox:ast:grammarTest` | ANTLR vs compiler parser corpus (~6 min, not in `check`) |
| FlatBuffers | `testRoundTrip`, `DeserializerFuzzTest.kt` (2000 random + flips) | IR serdes + hardening |

`SqlEmissionTest` comment: “Rell runs in consensus contexts where SQL byte-stability matters.”

---

## 10. Versions and 0.16.5–0.16.7 (source release notes)

`RellVersions.kt`:

- `SUPPORTED_VERSIONS`: 0.6.0 … 0.16.7 (every listed patch)
- `MIN_COMPATIBILITY_VERSION = 0.10.9`
- `MIN_COMPILER_VERSION = 0.13.11` (`gtx.rell.compilerVersion` era)
- `MODULE_SYSTEM_VERSION_STR = 0.10.0`
- `RETROACTIVE_GATES_VERSION = VERSION` (currently 0.16.7)

`C_FeatureRestrictions.makeRetroactive` (`c_utils_restrictions.kt`): a feature that shipped **without** a version gate is not rejected when recompiling a historical config whose `compilerVersion` predates the gate. New compilations are checked. This exists so replaying a chain does not halt on configs that compiled cleanly when written.

`doc/release-notes/0.16.5.txt` (2026-08-04): formatter/linter/LSP/error-message only. No language semantics.

`doc/release-notes/0.16.6.txt` (2026-08-07): LSP code actions + highlighting. **Language:** lambdas, value-block `if`/`when` arms, jump expressions now require `gtx.rell.version ≥ 0.16.1` (they had shipped ungated). Breaking for nodes pinned older than 0.16.1 that already used those constructs.

`doc/release-notes/0.16.7.txt` (2026-08-14): smaller deps (jackson-databind removed from compiler/runtime; jOOQ no longer re-exported). LSP honors `compile.rellVersion`. **Language:** more constructs version-gated **retroactively without breaking old configs** (uses `compilerVersion` exemption):

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

Docs site (`study-rell.md`) still lists **0.16.4** as newest. That lag is real.

---

## 11. Undocumented (or under-documented) production behaviors

These are in source and either absent from `/rell/` docs or only implied.

1. **Compile target is RR IR, not SQL.** SQL is generated per query/statement at runtime. A dapp is not a pile of stored SQL.
2. **Production still compiles from source** every time. FlatBuffers RR is tested, hardened, and unused on the GTX path (`rr-serialization/README.md`).
3. **Table name is `c<chainIid>.<mount>`**, including dots inside the quoted identifier (`"c0.foo.user"`). Mount max **58** because PG identifier max is 63 and the shortest prefix is `c0.` (3 chars) — `s_def.kt` `MAX_ENTITY_MOUNT_NAME_LEN = 58`. Attribute / attribute-mount max **63**. Gated since 0.12.0 (`NAME_LEN_SWITCH`).
4. **Rowid is a counter table + function**, not a `SEQUENCE`. Bulk insert uses `make_rowids` with `n ∈ (0, 10^9)`.
5. **System SQL functions are write-once** by name. Changing behavior requires a new function name. Globals are created at **node** startup, not per-chain init.
6. **32767 SQL bind-parameter cap** (`rt_sql_builder.kt`).
7. **json column DDL is JSONB** + GIN `jsonb_path_ops` for a single-column json index; bind uses `PGobject` `type = "json"`.
8. **text → CLOB, byte_array → BLOB** in jOOQ (PG `text` / `bytea` in practice).
9. **boolean on the wire is 0/1 integer**, not GTV boolean (GTV has no boolean kind — `gtv_type` enum is NULL/BYTEARRAY/STRING/INTEGER/DICT/ARRAY/BIGINTEGER).
10. **decimal on the wire is text.**
11. **Operations = compact GTV; queries = pretty GTV.** Same struct is an array in an op and a dict in a query.
12. **Enum: ops take the integer `.value`; queries accept name or index.** Reorder/remove of enum values changes the stored integer and the compact GTV form.
13. **`@sum 1` is a bound parameter**, not a literal (SQL emission test).
14. **Two-entity from-list is `,` join**, path access is `JOIN ON`. Outer join is the `@outer` form only.
15. **SQL `CASE` for `if`/`when` inside at-expressions** (pinned).
16. **json `[]` index is truncated to PG INT** (0..2147483647); Rell `integer` is BIGINT.
17. **Size CHECK uses `::BIGINT` length** so `@max_size` > 2^31 is not silently inverted.
18. **Snapshot init drops every entity table** then recreates (`isSnapshot`).
19. **`checkOldConstraints` looks up `"c0.$metaName"`** — chain-id 0 baked into that helper call.
20. **Disabled modules inherit** from parents; **test modules do not have a parent module**.
21. **`include` is a hard error** since the module system (0.10.0).
22. **Native functions = all public Kotlin methods** on a configured class, via reflection. Constructor `()` or `(RellNativeEnvironment)` only.
23. **Integer intern cache −1000..1000; rowid 0..1000.**
24. **Truffle backend exists** (`runtime-truffle`) alongside the tree-walk interpreter. Decimal has Truffle-specific fast paths in comments.
25. **Deserializer hardening** (if RR binaries are ever used): 100 MB byte-array cap, 10 MB GTV, 10 M vector, 500 MB buffer, recursion cap, `associateByFailOnDup` for op/query maps (duplicate mount = consensus-divergence lever).
26. **Retroactive version gates** (0.16.6–0.16.7): old configs keep compiling ungated features; new compiles do not. Controlled by `gtx.rell.compilerVersion` vs `RETROACTIVE_GATES_VERSION`.
27. **SQL interceptor categories SYS vs USER.** `chr repl --sql-log` is the USER stream `SqlEmissionTest` captures.
28. **Entity FK constraints** are generated for entity-typed attributes when not snapshotting.
29. **Mount `sys.*`, `block`, `transaction`, and Postchain system table names are reserved.**
30. **`GUID` / `SIGNER` primitive kinds exist in RR** with no runtime value class.

---

## 12. Contradictions / corrections vs `/workspace/chromia-knowledge/study-rell.md`

`study-rell.md` is a faithful study of **docs.chromia.com/rell** through **0.16.4**. Source 0.16.7 does not flip the language the docs describe; it fills gaps and dates the docs lag. Only items below are real differences.

### 12.1 Version surface

- Docs latest: **0.16.4 (2026-08-02)**. GitLab tags: **0.16.7 (2026-08-14)**. `study-rell.md` already flagged this as a gap (§18.11). Confirmed.
- 0.16.5–0.16.7 language-visible change is **version-gating**, not new syntax. 0.16.6 can **break** a node with `gtx.rell.version < 0.16.1` that already used lambdas / jump exprs / value-block arms. 0.16.7’s extra gates do **not** break historical configs.

### 12.2 “Compiler translates Rell into SQL”

Docs intro: compiler translates Rell into SQL. Source: compiler translates Rell into `R_` then `RR_`; the **interpreter** generates SQL. Same user-visible effect, wrong layer in the docs. Playground “SQL dry-run” is this interpreter path without a DB.

### 12.3 PostgreSQL types the docs left blank (`study-rell.md` §18.5)

Source fills:

| Type | SQL |
|---|---|
| `integer` / `rowid` | `BIGINT` |
| `boolean` | `BOOLEAN` |
| `text` | jOOQ `CLOB` |
| `byte_array` | jOOQ `BLOB` |
| `json` | jOOQ `JSONB` (docs said PostgreSQL `JSON`) |
| `decimal` / `big_integer` | `NUMERIC` (docs already said this) |
| `enum` | integer adapter (stored as the `.value` index) |
| entity ref | `BIGINT` FK to the other table’s `rowid` |

**Contradiction:** docs simple-types / gap list say `json` → PostgreSQL `JSON`. DDL uses `JSONB`. Bind uses `PGobject(type="json")`. Treat JSONB as the schema type.

### 12.4 Table-name formula

`study-rell.md` §17.2 called `"c<iid>.<mount>"` an inference from the analyze-page log. Source **defines** it: `Rt_ChainSqlMapping.prefix = "c$chainId."`. Not an inference anymore.

### 12.5 Mount / attribute length

Docs: mount max 58 from releases 0.13.11; entity/attribute max lengths from 0.12.0 not restated. Source: mount **58** (reason: `c0.` + PG 63), attribute and attribute-`@mount` **63**, switch `0.12.0`. 0.13.11 release note is the docs date for mount; the constant lives next to the 0.12.0 switch.

### 12.6 GTV exclusion set (`study-rell.md` §18.3)

Docs: “some types are not GTV-compatible”; only `virtual<T>` called out as not a query return. Source `computeDirectGtvCompatibility`: virtual is **fromGtv only** (so it *can* be an **operation input** — which docs virtual-types page already said). Not GTV at all: `range`, function types, `unit`, `object`, `operation`, `nothing`, `guid`, `signer`. Entity **is** GTV (as rowid). Collections/tuples/structs are GTV iff components are.

### 12.7 GTV wire table (`study-rell.md` §18.4)

Docs `/rell/` pages do not publish it. Source tests + `Lib_Type_Gtv` comment **are** the table: compact vs pretty, boolean=0/1, decimal=text, json=string, enum name-vs-index, struct array-vs-dict. `rell-cli.md` having that table is consistent with source, not a contradiction with language docs.

### 12.8 `@external`

Docs: mentioned, no syntax page. Source: `@external` modules, `C_ModuleKey.extChain`, `R_EntitySqlMapping_External` height-filtered joins to the other chain’s tables. Tests: `lang/module/ExternalModuleTest.kt`, `lang/def/ExternalTest.kt`. Syntax is in those tests; this note does not reconstruct it beyond what `r_sql.kt` implements.

### 12.9 Size-constraint relax-only

Docs (size-constraint page): cannot tighten on non-empty; can relax. Source matches, and also: **adding** a constraint to a non-empty table is an error (`size_constraint_added_records_exist`). Removing a constraint is allowed (drop CHECK).

### 12.10 Default params must be appended

Docs: operation page says append; query page silent; releases 0.14.3 says both. Source 0.16.7: omitting defaulted op/query args is version-gated to 0.14.3, with **compilerVersion exemption** for older configs — so a pre-0.16.7 config keeps accepting omitted defaults even if its declared language version is older.

### 12.11 `list._sort()`

`study-rell.md` §18.13: collection-types page writes `._sort()`. Not verified in the files fetched for this study. Do not rename.

### 12.12 Not contradictions (source confirms docs)

Cardinality, `@log` immutability, object singleton + required defaults, entity attr non-null, key=unique+index, attribute `@mount` as column name, bulk `create`, guard dual-run, `@compound`/`@singular`, abstract/override, anonymous import, mount inheritance `.` / `^`, `MIN_COMPATIBILITY` 0.10.9, integer overflow throws (runtime, not re-read here but 0.9.0 + `Rt_Exception`), test `@test` / `@disabled`.

### 12.13 Wording

Docs “safe SQL translation”: source uses jOOQ + parameterized binds + `@OptIn(RawSqlAccess)` poison-pill for string SQL. Safety is “no string-concat of user values into SQL”, not “no SQL injection surface in identifiers” (identifiers come from `MountName` / `sqlMapping`, validated as `Name`).

---

## 13. File map (fetched)

| Path | Why |
|---|---|
| `README.md`, `DEVELOPMENT.md`, `rell-base/README.md`, `doc/rell-architecture.md` | Layout, prefixes, pipeline |
| `rell-base/utils/src/utils/RellVersions.kt` | 0.16.7, supported set, min compat |
| `rell-base/frontend/src/main/kotlin/compiler/base/core/c_compiler.kt` | Passes, compile entry |
| `rell-base/frontend/src/main/kotlin/compiler/base/core/c_app.kt` | App assembly |
| `rell-base/frontend/src/main/kotlin/compiler/base/module/c_module.kt` | `R_Module` fields |
| `rell-base/frontend/src/main/kotlin/compiler/base/module/c_module_loader.kt` | Load/select/parent/test |
| `rell-base/frontend/src/main/kotlin/compiler/base/def/c_def_entity.kt` | Attr compile, sqlMapping, types |
| `rell-base/frontend/src/main/kotlin/compiler/base/def/c_def_mount.kt` | Mount conflicts, reserved names |
| `rell-base/frontend/src/main/kotlin/compiler/ast/s_def.kt` | Name lengths 58/63, `@mount`/`@size` since |
| `rell-base/frontend/src/main/kotlin/compiler/base/utils/c_utils_restrictions.kt` | Retroactive gates |
| `rell-base/frontend/src/main/kotlin/model/r_sql.kt` | Entity SQL mapping kinds |
| `rell-base/frontend/src/main/kotlin/model/r_sql_constants.kt` | Function/table names |
| `rell-base/frontend/src/main/kotlin/model/r_type.kt` | GTV + SQL flags |
| `rell-base/frontend/src/main/kotlin/model/r_builtin_types.kt` | Primitive / collection types |
| `rell-base/rr-tree/src/main/rr_type.kt` | RR type union |
| `rell-base/rr-serialization/README.md` | FlatBuffers, unused in prod, security |
| `rell-base/runtime-core/src/main/kotlin/runtime/rt_common.kt` | `c<id>.` prefix |
| `rell-base/runtime-core/src/main/kotlin/runtime/rt_entity_sql_table.kt` | table() dispatch |
| `rell-base/runtime-core/src/main/kotlin/sql/sql_gen.kt` | DDL + sys functions |
| `rell-base/runtime-core/src/main/kotlin/sql/sql_init.kt` | Schema evolution |
| `rell-base/runtime-core/src/main/kotlin/sql/sql.kt` | Executor, RawSqlAccess |
| `rell-base/runtime-core/src/main/kotlin/runtime/rt_sql_builder.kt` | 32767 param cap |
| `rell-base/runtime-interpreter/src/runtime/rr_interp_sql_gen.kt` | At-expr SQL |
| `rell-base/runtime-core/src/main/kotlin/runtime/rt_value_{integer,boolean,text,bytearray,json,decimal,rowid}.kt` | SQL/GTV/native adapters |
| `rell-base/runtime-core/src/main/kotlin/runtime/rt_type_gtv.kt` | GTV from R_Type |
| `rell-base/runtime-core/src/main/kotlin/lib/type/lib_type_gtv.kt` | Compact vs pretty (stdlib text) |
| `rell-base/runtime-core/src/main/kotlin/runtime/gtx_conversion.kt` | pretty / strict flags |
| `rell-gtx/src/main/kotlin/module/native.kt` | Native reflection loader |
| `rell-api-native/src/main/kotlin/native_functions.kt` | `RellNativeEnvironment` |
| `rell-base/src/test/kotlin/sql/SqlEmissionTest.kt` | Pinned SQL |
| `rell-base/src/test/kotlin/lang/misc/GtvRtConversionTest.kt` | Pinned GTV |
| `doc/release-notes/0.16.{5,6,7}.txt` | Post-docs-site releases |

---

## 14. Gaps still not read in this pass

Not fetched / not claimed: full `rr_interp_db_write.kt` (create/update/delete SQL text), `sql_meta.kt` meta schema, `c_module_ext.kt` `@external` syntax, Truffle backend semantics, `Lib_DecimalMath` exact NUMERIC precision, enum SQL adapter class body, linter rule implementations, special-operation dispatch, `rell.test` implementation constants. Those remain “not in this study” rather than “do not exist.”
