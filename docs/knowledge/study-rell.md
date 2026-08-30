# Rell language — official-docs study notes

**Written:** 2026-08-26  
**Scope:** Official Chromia Rell docs only. Entry: https://docs.chromia.com/rell/rell-intro plus every URL under https://docs.chromia.com/rell/ listed in https://docs.chromia.com/sitemap.xml.  
**Method:** WebFetch of each page; Cloudflare/timeout pages retrieved with `curl` from the box (releases, analyze-rell-dapp-code, crypto, time, statements index). Nothing invented. Where a page is silent, that is stated.  
**Do not treat this as a substitute for the pages.** Each section starts with its canonical URL.

**Related file:** `/workspace/chromia-knowledge/rell-cli.md` already covers CLI, chromia.yml, GTV wire tables, and a compressed language brief. Differences and extra claims in that file are called out in §17.

**Docs site version surface:** https://docs.chromia.com/rell/releases lists Rell **0.16.4 (2026-08-02)** as the newest release on the docs site. Features below are dated from that page when the feature page itself is silent.

---

## 1. What Rell is

Canonical: https://docs.chromia.com/rell/rell-intro  
Also: https://docs.chromia.com/rell/core-concepts

Rell (Relational language) is Chromia's dapp language. It is language-centric, not VM-centric: the compiler translates Rell into SQL (documented as a *safe* translation) and executes it against the chain's PostgreSQL. One language describes:

1. The data model / schema (`entity`, `object`)
2. Queries (read API)
3. Procedural / write code (`operation`, `function`)

Stated design goals:

- Completely type-safe: query result types match procedural types.
- Arithmetic overflow protection; explicit authorization checks are mandatory (intro wording). Integer overflow throws (releases 0.9.0).
- Concise vs SQL (claimed up to 7× more compact for data definition).
- Meta-programming via reusable templates (abstract modules / extendable functions).
- Syntax deliberately close to JavaScript / Kotlin; relational idioms plus normal procedural code.

A dapp is: client → Postchain client → transaction → Rell operations; clients also call Rell queries. Playground: runnable snippets open in the Rell Playground.

Release notes live at https://docs.chromia.com/rell/releases (not on the intro page itself).

---

## 2. Core mental model

Canonical: https://docs.chromia.com/rell/core-concepts

Three layers:

1. Relational modelling and queries (SQL-like, new syntax).
2. Normal programming: variables, loops, functions, collections.
3. Blockchain / backend constructs: request routing, authorization.

Blockchain here means securely synchronizing databases on system nodes. The backend's job is: describe the model, handle write requests (operations), handle read requests (queries).

Relational expression pipeline (matches SQL *logical* processing order, not SQL written order):

```
FROM          CARDINALITY   WHERE            WHAT   TAIL
entity_name   @             { condition }    ()     ;
```

Cardinality (the query *fails* if the count does not match):

| Operator | Meaning |
|---|---|
| `@` | exactly one |
| `@?` | zero or one |
| `@+` | one or more |
| `@*` | zero or more |

`create` ≈ SQL INSERT. Arguments can be matched to attributes by **type** when types are unique; order then does not matter. Hex `byte_array` literal: `x"…"`.

`val` = read-only binding. `var` = reassignable.

`user @ { .name == "Alice" }` retrieves exactly one row or aborts (and rolls back) the operation. That abort-on-cardinality is the documented way to encode "this row must exist uniquely."

Comments: `//` rest-of-line; `/* … */` block. (The core-concepts fetch rendered the `//` marker poorly as `/`; every example on the page and elsewhere uses `//`.)

Formatting practices stated on that page: spaces around operators and after commas; indent blocks; put long parameter lists on separate lines.

Keys vs indexes (same page): `key` = unique + index; `index` = non-unique. Both cost extra on write. Composite order is left-prefix (same as SQL). Place the most selective column first. PostgreSQL B-tree, lookup described as O(log n).

---

## 3. Identifiers and syntax

Canonical: https://docs.chromia.com/rell/language-features/identifiers-syntax  
Also: https://docs.chromia.com/rell/language-features/expressions/values

An identifier:

- Starts with `_`, `A–Z`, or `a–z`
- Then `_`, letters, digits
- Case-sensitive (`myVar` ≠ `MyVar`)
- Keywords (`function`, `entity`, `if`, `while`, …) cannot be identifiers
- Must be unique in its scope (cannot have an entity and a function of the same name in the same module)

The identifiers page does **not** list the full reserved-word set. Releases 0.13.12 added further reserved names for future keywords; the list is not on the identifiers page.

**Literals** (https://docs.chromia.com/rell/language-features/expressions/values):

- `null` (own type `null`)
- `true` / `false`
- integer: `123`, `0`, `-456`
- text: `'Hello'` or `"World"`
- byte array: `x'1234'` or `x"ABCD"`
- big integer: suffix `L` (`9223372036854775832L`)
- decimal: `123.456`, scientific `55.77e-5`

Text escapes: `\r \n \t \b`, `\" \' \\`, Unicode `\u003A`.

**Trailing commas** are allowed in any comma-separated list (collections, function parameters, enums). Added in 0.13.12.

---

## 4. Modules, imports, namespaces, mount, abstract

### 4.1 Module kinds and runtime activity

Canonical: https://docs.chromia.com/rell/modules

A Rell dapp is a tree of modules. A module is either:

- **Single-file module:** a `.rell` file that starts with `module;`
- **Directory module:** all `.rell` files in a directory that do *not* themselves have a module header. Exception: `module.rell` always belongs to the directory module, even if it has a header. A directory module does **not** require `module.rell`.

**Root module:** directory module of `.rell` files in the source-root; empty name.

Every file of a directory module sees all other files in that module. A single-file module sees only its own definitions.

Recommended layout for a larger module (official example tree):

```
app/
  module.rell          # imports + custom mount names
  entities.rell        # entities, enums, small structs; entity-by-id helpers
  operations.rell
  queries.rell
  functions.rell
  structs.rell         # when there are more than ~3 structs
```

**Runtime:** only the **main module** (the one named when starting the app / in `blockchains.<name>.module`) and modules it imports, directly or indirectly, are **active**. Inactive modules contribute neither operations/queries nor tables.

### 4.2 Import forms

```rell
import app.single;            // alias = last path segment
import alias: app.multi;      // custom alias
import .d;                    // relative: current.d
import alias: ^;              // parent
import alias: ^^;             // grandparent
import ^.e;                   // uncle
import foo.*;                 // wildcard into current namespace
import foo.{ns.*};            // one namespace from a module
import sub: foo.{ns.*};       // those names under a new namespace `sub`
import foo.{f};               // selected definition
import foo.{g, h};
import ns: foo.{f, g};        // nested namespace
import foo.{a: f, b: g};      // rename on import
```

Relative names are resolved from the *current* module name. If current is `a.b.c`: `.d` → `a.b.c.d`; `^` → `a.b`; `^^` → `a`; `^.e` → `a.b.e`.

Tip on the page: if the same function name exists in two modules, import the specific module and call via the alias — wildcard + bare name is ambiguous.

**Namespace-grouped imports** (FT4-style, on this page):

```rell
namespace ft4 {
  import lib.ft4.core.accounts.{ft4_account: account};
  import lib.ft4.core.admin;
  import lib.ft4.core.auth;
  import .utils;
}
// then: ft4.admin.require_admin();
```

### 4.3 Namespaces

Canonical: https://docs.chromia.com/rell/language-features/modules/namespace

- Inside a namespace, use short names (`country` not `foo.country`).
- SQL table names include the full namespace path. Example given: table for entity `foo.user` is `c0.foo.user`.
- The same namespace name may be declared multiple times (split across files).
- Nested short form: `namespace x.y.z { … }` ≡ nested `namespace x { namespace y { namespace z { … } } }`.
- **Anonymous namespace:** `namespace { … }` — useful to apply an annotation to a group:

```rell
@mount('foo.bar')
namespace {
    entity user {}
    entity company {}
}
```

**Anonymous imports** (also on this page; added 0.13.12):

```rell
import _: foo;
import _: bar;
```

Activates the module (operations, queries, function extensions, overrides) without adding any names. There is no alias, so the module cannot be referenced from code.

### 4.4 Mount names

Canonical: https://docs.chromia.com/rell/language-features/modules/mount

Mount names identify:

- entities / objects → **SQL table name**
- operations / queries → **the name clients invoke**

Default = fully qualified definition name. `namespace foo { namespace bar { entity user {} } }` → mount `foo.bar.user`.

Override: `@mount('desired_mount_name')` on entity, object, operation, query, namespace, or module.

**Attribute-level `@mount` (since 0.15.1):** names the **database column**. Rename in code without dropping data:

```rell
entity user {
    @mount('fname') first_name: text;
    age: integer;
}
```

No schema change is performed. Keys, indexes, metadata, and size constraints use the mapped column name.

**Relative mount shortcuts** when a parent namespace/module already has a mount context:

- `.` appends to the current context (`@mount('.d.user')` under `@mount('a.b.c')` → `a.b.c.d.user`)
- `^` pops one segment (`@mount('^.user')` → `a.b.user`; `@mount('^^.x.user')` → `a.x.user`)

`@mount("foo.")` on an entity → `foo.user`. `@mount("foo")` → mount name is exactly `foo`.

**Import does not change mount names.** They are fixed at the definition site.

**Special operations** require the `__` prefix on the *operation name*. Prefer dots in the `@mount` name (`@mount('icmf.message') operation __icmf_message() {}`). The `__` prefix is required and is what Postchain recognizes as lifecycle / reserved operations.

**Not on this page, but in official releases 0.13.11:** mount names are limited to **58 characters**.

### 4.5 Abstract modules

Canonical: https://docs.chromia.com/rell/language-features/modules/abstract

```rell
abstract module;
abstract function customize(x: integer): text;                 // must be overridden
abstract function is_4wd(): boolean = false;                   // default body
```

A client module `import`s the abstract module and writes `override function lib.customize(x: integer): text { … }`. All abstract functions that lack a body **must** be overridden.

The official example: `abstract_vehicle` with `is_4wd()` defaulting to `false` and `has_four_doors()` abstract; `sportscar` overrides only `has_four_doors`; `main` imports `sportscar` and reads both.

### 4.6 External modules (no dedicated page)

The modules index (https://docs.chromia.com/rell/language-features/modules/) states: "external modules enable cross-blockchain access." There is **no** `/rell/language-features/modules/external` URL in the sitemap.

From official **releases**:

- 0.8.0: external block functionality for accessing classes from other blockchains.
- 0.10.0: `@mount`; classes → entities, records → structs.
- 0.10.1: `@external` annotation replaces external blocks; modules can be marked `@external` and imported as such.

The current language-feature pages do not document the syntax. Do not invent it from the release one-liners.

---

## 5. Definitions that persist or shape the API

### 5.1 Entity

Canonical: https://docs.chromia.com/rell/language-features/modules/entity  
Also relations/examples: https://docs.chromia.com/rell/core-concepts

Persistent. Mapped to a database table. Created / deleted with `create` / `delete`. A variable of entity type holds the **rowid** (primary key), not the attribute values.

```rell
entity user {
    first_name: text;
    last_name: text;
    year_of_birth: integer;
    mutable salary: integer;
}
```

Rules on the entity page:

- If the attribute name is omitted, it defaults to the type name (`company;` means `company: company`; `name;` means built-in type `name`).
- Attributes may have default values used when omitted from `create`.
- Implicit `rowid` attribute is the primary key. Access: `u.rowid` or `user @ { .name == 'Alice' } ( .rowid )`.
- **`key`** = unique constraint + index. **`index`** = non-unique index. Both may be composite. Combined definition `key first_name: text, last_name: text;` cannot also specify `mutable`.
- Mutability and defaults *are* allowed inside a standalone `index` / `key` clause: `index mutable city: text = 'Rome';`. "Mutating an indexed field may be slow if there are many rows."
- Entity attributes **cannot be nullable** — stated on https://docs.chromia.com/rell/language-features/types/complex-types ("to ensure data integrity"), not restated on the entity page.

Relations (modelled as entity-typed attributes + uniqueness):

| Relation | Pattern |
|---|---|
| 1-1 | both sides `key` |
| 1-many | `index` on the many side, `key` on the unique side |
| many-many | junction entity with `key left, right` |

`@log entity user { name: text; }` — adds an implicit `transaction` attribute; entity becomes immutable and non-deletable.

**Schema evolution (entity page):**

Compatible:

- Add attributes with defaults (existing rows get the default)
- Add attributes to empty tables (defaults not required)
- Remove attributes
- Change mutability on entities that are **not** `@log`

Incompatible:

- Change an attribute's type
- Add or remove `@log`

The entity page does **not** mention: automatic column drop since 0.15.1 (that is on the releases page); adding/removing keys and indexes at DB init since 0.13.9 (releases); size-constraint annotations (own page); attribute `@mount` (mount page).

**rowid generation** (https://docs.chromia.com/rell/language-features/types/simple-types): 64-bit integer from a **per-blockchain sequence**. New inserts get a value **strictly greater** than the last allocated rowid on that chain. Deletes do not reuse. `rowid` supports only comparison. `rowid(integer)` rejects negatives. `.to_integer()`.

### 5.2 Object (singleton)

Canonical: https://docs.chromia.com/rell/language-features/modules/object

- Exactly one instance; stored in the DB
- Auto-initialized at blockchain init
- **Cannot** be `create`d or `delete`d from code
- Every attribute **requires a default value**

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

### 5.3 Struct

Canonical: https://docs.chromia.com/rell/language-features/modules/struct

In-memory only. Attributes immutable unless `mutable`. Defaults allowed. GC'd. Construct by calling the struct name. Attribute matching by name or type; argument order does not matter.

```rell
struct user {
    name: text;
    address: text;
    mutable balance: integer = 0;
}
val u = user(name = 'Bob', address = 'New York');
```

- `.to_struct()` on an entity/object → `struct<entity>`
- `.to_mutable_struct()` → `struct<mutable T>`
- `create user(s)` where `s` is `struct<user>`
- `.to_mutable()` / `.to_immutable()` convert between `struct<T>` and `struct<mutable T>`
- Safe access on nullable structs: `u?.balance += 100`

**`struct<operation>`:** attributes = that operation's parameters. Used to type-check / pre-validate an op from a query.

Functions on GTV-compatible structs (table on the struct page):

| Function | Role |
|---|---|
| `T.from_bytes(byte_array): T` | decode binary GTV |
| `T.from_gtv(gtv): T` | decode GTV |
| `T.from_gtv_pretty(gtv): T` | decode pretty GTV |
| `.to_bytes()` / `.to_gtv()` / `.to_gtv_pretty()` | encode |
| `.to_gtx_operation(): gtx_operation` | `struct<operation>` → GTX op |
| `.to_test_op(): rell.test.op` | for tests |

Built-in structs shown on the page:

```rell
struct gtx_operation { name: text; args: list<gtv>; }
struct gtx_transaction_body {
    blockchain_rid: byte_array;
    operations: list<gtx_operation>;
    signers: list<gtv>;
}
struct gtx_transaction { body: gtx_transaction_body; signatures: list<gtv>; }
struct rell.test.keypair {
    priv: byte_array; // 32 bytes
    pub: byte_array;  // 33 bytes
}
```

**On the official struct page but easy to miss:** use a struct instead of a long parameter list.

**In official releases, not on the struct page:**

- 0.14.12: optional struct attributes may be omitted when calling operations/queries and in `from_gtv` / `from_gtv_pretty` / `from_bytes`
- 0.14.13: size-constraint annotations on struct attributes
- 0.14.16: `struct.copy()`

### 5.4 Enum

Canonical: https://docs.chromia.com/rell/language-features/modules/enum

```rell
enum currency { USD, EUR, GBP }
```

Each value has `.name: text` and `.value: integer` (declaration index, `USD` = 0).

| Function | Role |
|---|---|
| `T.values(): list<T>` | all values, declaration order |
| `T.value(text): T` | by name; exception if missing |
| `T.value(integer): T` | by index; exception if missing |

`rell-cli.md` production warning (from CLI 0.30.0+ `chr deployment update`, **not** on this language page): adding/removing/reordering enum values is detected; reorder or remove is dangerous because `.value` is the stored form. The enum page itself is silent on schema evolution.

### 5.5 Operation (write API)

Canonical: https://docs.chromia.com/rell/language-features/modules/operation

- Can modify the database
- Does **not** return a value
- Parameter types **must be GTV-compatible**

```rell
operation create_user(name: text) {
    create user(name = name);
}
```

**Default parameters:** optional for external clients. **New parameters with defaults must be appended** so existing clients keep working (stated on this page; added 0.14.3).

Typical auth pattern on this page:

```rell
require(op_context.is_signer(user_pubkey));
```

Specify the actor as a parameter; verify they signed. `require` fails the operation if the condition is false. `create` persists if the operation succeeds. `user @ { .pubkey == user_pubkey }` fails the op if the user does not exist.

**Size-constraint annotations** on `text` / `byte_array` parameters: `@size(n)`, `@size(a, b)`, `@min_size(n)`, `@max_size(n)`. See §5.8. If an op parameter is annotated, the corresponding `struct<operation>` attribute behaves as if it had the same annotation.

**`@compound`:** cannot be the only kind of operation in a transaction (a tx of only compound ops is invalid).  
**`@singular`:** at most once per transaction.  
Both may be combined. Added 0.14.13.

**Guard blocks** — read-only argument verification, declared with `guard { … }`:

- Run during Postchain `checkCorrectness()` **and** again during `apply()`. `print` / `log` in a guard therefore execute **twice**.
- Releases 0.15.0: guard also runs during operation validation (the operation page describes the dual run without dating when validation started).
- Only *declarations* (not assignments) may appear before `guard`. `val x: integer;` is legal; `val x = y + 1;` before the guard is not.
- Guard may **read** the DB, not write.
- `op_context` is visible in the guard during validation, but `op_context.emit_event()` is illegal and errors at validation time.

Variables needed after the guard must be queried again in the body (official transfer example does this).

### 5.6 Query (read API)

Canonical: https://docs.chromia.com/rell/language-features/modules/query

- Cannot modify the database (compile-time check)
- Must return a value (type inferred if omitted)
- Parameter types **and** return type must be GTV-compatible

Short form: `query q(x: integer): integer = x * x;`  
Full form: block + `return`.

Default parameters allowed (clients may omit them). **This page does not say they must be appended.** The operation page does. Releases 0.14.3 says default params on operations *and* queries, "must be placed at the end of the parameter list."

Size-constraint annotations: same as operations.

### 5.7 Function

Canonical: https://docs.chromia.com/rell/language-features/modules/function

Reusable logic. Can modify the DB when called from an operation. Callable from queries, operations, and other functions. No explicit return type ⇒ `unit`.

Short form `function f(x: integer): integer = x * x;` and full form with `return`.

Also:

- Default parameters (`f()`, `f('Alice')`, `f(score=456)`)
- Named arguments
- Size-constraint annotations on parameters
- Function values: `(integer) -> text`, `(byte_array, decimal) -> integer`
- Partial application: `f(*)`, `f(123, *)`, `f(*, 456)`, `f(x = *, y = *)`

Partial-application rules (this page, important):

- If any `*` is present, unspecified parameters **without** defaults are also wildcards.
- A nameless `*` as the **last** argument is special: it marks partial application without binding a specific parameter. `f(*)` works even for zero-parameter `f`. If that form is used, there must be no other wildcard parameters.
- Defaults that are **not** listed as wildcards are bound **at the moment of partial application**.
- Named-wildcard order determines the resulting function signature.
- Most system/member functions can be partially applied. Overloads need a type annotation (`val f: (integer) -> integer = abs(*);`).
- **Cannot** partially apply: `print()`, `log()`, `require()`, `text.format()`, "and others."
- Member partial application captures the receiver: `val f = l.size(*);` — later `l.add(…)` is visible to `f()`.

**`@extendable` / `@extend`:** calling the base runs extensions then (depending on return type) the base.

| Return type | Behavior |
|---|---|
| `unit` | all extensions + base, unconditionally |
| `boolean` | extensions until one returns `true`; base only if all returned `false`; last result is returned |
| `T?` | like boolean, but stop on first non-null |
| `list<T>` | all run; lists concatenated |
| `map<K,V>` | all run; maps unioned; **fails on key conflict** |

**`@test` on a function** (preferred over `test_` prefix):

- Must be in a `@test` module
- No parameters
- Cannot combine with `@extendable` / `@extend` / `abstract` / `override`
- Violations are **compiler errors**. A misnamed `test_` prefix is silently ignored.
- Strongly recommended on this page.

**`@native`:** Java/Kotlin implementation. Mapped in `blockchains.<name>.config.gtx.rell.native` (one class per Rell module). Class must be public; constructor empty or `RellNativeEnvironment`; one public instance method per `@native` function; names and types must match.

`RellNativeEnvironment`: `config: Gtv`, `blockchainRid: BlockchainRid`.

Type map (this page):

| Rell | Kotlin | Java |
|---|---|---|
| `big_integer` | `java.math.BigInteger` | same |
| `boolean` | `Boolean` | `boolean` |
| `byte_array` | `ByteArray` | `byte[]` |
| `decimal` | `java.math.BigDecimal` | same |
| `gtv` | `net.postchain.gtv.Gtv` | same |
| `integer` / `rowid` | `Long` | `long` |
| `text` | `String` | `String` |
| `unit` (return) | `Unit` | `void` |
| `T?` | `T?` | nullable `T` |

Maven: `net.postchain.rell:rell-api-native:<RELL_VERSION>`.

**Must be deterministic.** Non-determinism (RNG, system time, external APIs) can break consensus. Rell cannot enforce this.

Best-practice snippet on this page: `require(user @? { … }, "User not found…")` for entity-by-id helpers; snake_case names.

### 5.8 Size-constraint annotations

Canonical: https://docs.chromia.com/rell/language-features/modules/size-constraint-annotations

Apply to: function / query / operation **parameters**, **struct attributes**, and (since 0.15.1) **entity and object attributes**. Types: `text` and `byte_array`.

| Annotation | Meaning |
|---|---|
| `@size(n)` | exact size `n` |
| `@size(min, max)` | inclusive range |
| `@min_size(n)` | minimum |
| `@max_size(n)` | maximum |

Parameter annotations are checked at the call; default parameters are checked at compile time where possible.

On entity/object attributes they compile to PostgreSQL `CHECK` constraints and are enforced on `create`, `update`, and attribute `=` / `+=`.

You **cannot** tighten size constraints on a non-empty entity. You **can relax** them (increase max or decrease min) because that cannot invalidate existing rows.

Versions: struct attributes 0.14.13; function/query/operation parameters 0.14.14; entity/object attributes 0.15.1.

---

## 6. Type system

Index: https://docs.chromia.com/rell/language-features/types/

### 6.1 Simple types

Canonical: https://docs.chromia.com/rell/language-features/types/simple-types

**boolean** — `true` / `false`.

**integer** — 64-bit signed. `integer.MIN_VALUE` = −2^63, `integer.MAX_VALUE` = 2^63−1. Parse: `integer(text, radix=10)`, `integer.from_text`, `integer.from_hex`. Convert: `integer(decimal)` truncates toward 0, throws if out of range. Members include `.abs()`, `.min`/`.max` (also vs `decimal`), `.to_text(radix)`, `.to_hex()`, `.sign()`, `.signum()`, `.pow(exponent)`, `.to_big_integer()`, `.to_decimal()`. Aliases on the page: `.hex()`, `.parseHex()`, `.str()`.

**big_integer** — suffix `L`. Interpreter: `java.math.BigInteger`. SQL: `NUMERIC`. Precision 131072 digits. `MIN_VALUE` = −(10^131072)+1, `MAX_VALUE` = (10^131072)−1. Convert to/from `integer` (fails if out of range), `decimal` (truncates fraction), signed/unsigned bytes, text (radix 2–36), hex. `.abs()`, `.min`/`.max`, `.sign()`, `.pow()`.

**decimal** — **not** IEEE float. Accurate within range. Implicitly rounded to **20** decimal places (`decimal('1E-20')` nonzero, `decimal('1E-21')` zero). Stored decimal, not binary — string conversions are lossless except that rounding. Max 131072 digits before the point, 20 after. Interpreter: `java.math.BigDecimal`. SQL: `NUMERIC`. Operations "at least ten times slower" than integer. Large values use a lot of space (~0.41 bytes/digit in memory, ~0.5 in DB; ~54 KiB for 1E+131071). Literals: `123.456`, `3.45633E+10`. `==` / `!=` compare numeric value, so `1.0E+2 == 10.0E+1` is `true`. Members: `.abs()`, `.ceil()`, `.floor()`, `.min`/`.max`, `.round(scale=0)`, `.sign()`, `.to_integer()` (toward 0), `.to_text(scientific=false)`, `.to_big_integer()` (truncate).

**text** — string. Members used in production: `.empty()`, `.size()`, `.compare_to`, `.starts_with` / `.ends_with` / `.contains`, `.index_of` / `.last_index_of`, `.sub(start[, end])`, `.repeat`, `.replace`, `.reversed`, `.upper_case` / `.lower_case` (locale-independent since 0.14.13), `.split` (literal separator, **not** regex), `.trim`, `.matches` (regex), `.match_groups` / `.match_named_groups` (full-match, 0.14.13), `.regex_replace` (0.14.16; `$1` and `${name}`), `.to_bytes()` UTF-8, `.char_at`, `.format(...)`, `.like()` (SQL LIKE), aliases `.len()`, `.encode()`, `.charAt()`, `.compareTo()`.

`text.format` specifiers on the page: `%b`/`%B` boolean, `%s` text, `%f` decimal (6 places), `%d`/`%o`/`%x`/`%X` integer.

Operators: `+` concat, `[]` character access (returns single-character `text`). Most members translate to SQL inside at-expressions.

**byte_array** — binary. `byte_array(text)` / `.from_hex` / `.from_base64` / `.from_list` (0–255). Members: `.empty()`, `.repeat`, `.reversed`, `.size()`, `.sub`, `.to_hex()`, `.to_base64()`, `.to_list()`, `.sha256()`, `.len()`, `.join_to_text()`. Operators: `+`, `[]`. Most members translate to SQL in at-expressions.

Best practice on this page: validate `text` / `byte_array` length at the start of the operation (`require(x.size() <= MAX_TEXT_LENGTH)`). Prefer `@max_size` when the limit is a schema/API contract (§5.8).

**rowid** — see §5.1.

**json** — PostgreSQL `JSON`. `json(text)` fails if invalid. `.to_text()` / `.str()`.

Retrieval (0.14.16+): `get(index)` / `get(key)` throw; `get_or_null` returns null. Conversions: `as_integer` / `as_big_integer` / `as_text` / `as_boolean` and `*_or_null`. Type checks: `is_object` / `is_array` / `is_text` / `is_integer` / `is_big_integer` / `is_boolean` / `is_null`. `is_integer` ⇒ `is_big_integer`, not vice versa. `.size()` (array length or object pair count; throws otherwise). `.keys(): set<text>` (objects only). `[]` ≡ `get`. Chainable.

**unit** — no value; cannot be written explicitly.  
**null** — type of the `null` expression; cannot be written as a type name.

**Aliases** (this page):

| Alias | Means |
|---|---|
| `pubkey` | `byte_array` |
| `name` | `text` |
| `timestamp` | `integer` |
| `tuid` | `text` |

`timestamp` is **milliseconds** in every official time API (`op_context.last_block_time`, `rell.time`, `__timeb`). There is no distinct datetime type.

### 6.2 Complex types

Canonical: https://docs.chromia.com/rell/language-features/types/complex-types

**`T?` nullable**

- Entity attributes cannot be nullable.
- Operators: `?:` (elvis), `?.` (safe call), `!!` (assert non-null).
- `require(y)` also asserts non-null and returns `T`.
- Assign `T` → `T?` and `null` → `T?`; not the reverse.
- `(T)` assignable to `(T?)`; `list<T>` is **not** assignable to `list<T?>`.
- Smart cast: after `if (x != null)`, `x` is `T` in that branch.

**Tuple**

- `(16,)` single-element; `(26, "Bob")`; named `(x=32, y=26)` accessed as `.x`.
- Access by index `t[0]` or by name.
- Compatibility is structural **and** name-sensitive: `(x:integer, y:integer)` is **not** compatible with `(a:integer, b:integer)` or with `(integer, integer)`.
- Unpack: `val (n, s) = t;` ignore with `_`.
- `for ((x, y) in list_of_tuples)`.

**range** — `range(start=0, end, step=1)`, start-inclusive, end-exclusive (Python-like). Negative step allowed. `in` tests membership considering `step`.

**gtv** — encoded arguments/results of remote op/query calls. Simple value, array, or string-keyed dict.

Functions: `gtv.from_json(text|json)`, `gtv.from_bytes`, `gtv.from_bytes_or_null`, `.to_json()`, `.to_bytes()`, `.hash()`.

`gtv_type` enum since 0.15.3: `NULL`, `BYTEARRAY`, `STRING`, `INTEGER`, `DICT`, `ARRAY`, `BIGINTEGER`. Read `.type`.

Every GTV-compatible type: `T.from_gtv` / `from_gtv_pretty`, `.to_gtv()` / `.to_gtv_pretty()`, `.hash()` (same as `.to_gtv().hash()`). `null.to_gtv()` exists.

**Some Rell types are not GTV-compatible** (this page). The exclusion set is **not listed**. `virtual<T>` cannot convert to GTV and therefore cannot be a query return type (virtual-types page).

The official Rell type pages do **not** publish the operation-input / query-input / query-output conversion table that appears in `rell-cli.md` (that table is sourced outside `/rell/`).

### 6.3 Collections

Canonical: https://docs.chromia.com/rell/language-features/types/collection-types

`list<T>` (ordered, duplicates), `set<T>` (no duplicates), `map<K,V>`.

Always mutable. **Only non-mutable types** may be map keys or set elements.

What counts as mutable (this page): collections themselves; a nullable type if the inner type is mutable; a struct with a mutable field or a mutable-typed field; a tuple if an element type is mutable.

Creation: `[1, 2, 3]`, `list<integer>()`, `set<integer>()`, `['Bob': 123]`, `map<text, integer>()`. Map literal: **last value wins** if a key is repeated.

**Combining (since 0.14.16)** — operands are shallow-copied:

| Op | list / set | map |
|---|---|---|
| `a + b` | concat / union (`add_all_copy`) | merge; `b` wins on conflict (`put_all_copy`) |
| `a - b` | remove those in `b` (`remove_all_copy`) | — |
| `a & b` | intersection (`retain_all_copy`) | — |

`collection.retain_all(values)` mutates in place (same release).

**list** members (official table): `.add` / `.add(pos, T)` / `.add_all` (list or set, optional pos), `.clear`, `.contains` / `.contains_all`, `.empty`, `.index_of`, `.remove` / `.remove_all` / `.remove_at`, `.repeat`, `.size`, `.reverse()` (in place) / `.reversed()` (copy), `._sort()` (in place; name is `_sort` on the page) / `.sorted()`, `.to_text()`, `.sub(start[, end])`. Operators: `[]`, `in`.

**set** members: `.add` / `.add_all` (true if anything added), `.clear`, `.contains` / `.contains_all`, `.empty`, `.remove` / `.remove_all`, `.size`, `.sorted()` → list, `.to_text()`. Operator: `in`.

**map** members: `.clear`, `.contains(K)`, `.empty`, `.get` / `.get_or_null` / `.get_or_default`, `.put` / `.put_all`, `.remove` / `.remove_or_null`, `.keys()` → set copy, `.values()` → list copy, `.size`, `.to_text()`. Operators: `[]`, `in` (key). Constructor `map<K,V>(iterable<(K,V)>)`.

### 6.4 Iterables

Canonical: https://docs.chromia.com/rell/language-features/types/iterables

Internal type `iterable<T>` — cannot be written in user code. Usable as iterable: `range`, `list`, `set`, `map`, and the corresponding `virtual<…>` collections.

Used by: `for`, collection-at expressions, `list`/`set`/`map` constructors (`map(list_of_pairs)`, `set(list_with_dupes)`, `list(map)` → list of pairs).

### 6.5 Subtypes

Canonical: https://docs.chromia.com/rell/language-features/types/sub-types

If `B` is a subtype of `A`, `B` is assignable to `A`.

- `T` is a subtype of `T?`; `null` is a subtype of `T?`
- `(T, P)` is a subtype of `(T?, P?)`, `(T?, P)`, `(T, P?)`

That is the entire page.

### 6.6 Virtual types (Merkle / ICCF-style proofs)

Canonical: https://docs.chromia.com/rell/language-features/types/virtual-types

`virtual<T>` where inner types of `T` must be GTV-compatible; map keys must be `text`.

- Immutable after creation
- Member access returns virtual values for nested structured types
- `.to_full(): T` — throws if the value is not fully present
- **Cannot** convert to GTV → **cannot** be a query return type
- Typical use: operation parameter `virtual<list<Record>>`

`virtual<list<T>>`: `.empty`, `.get(i)`, `.size`, `.to_full`, `.to_text`, `.join_to_text`; `[]`, `in` (index present).  
`virtual<set<T>>`: `.empty`, `.size`, `.to_full`, `.to_text`, `.join_to_text`; `in`.  
`virtual<map<text,T>>`: `.contains`, `.empty`, `.get` / `.get_or_default` / `.get_or_null`, `.keys`, `.values` (list of virtual values), `.size`, `.to_full`, `.to_text`, `.join_to_text`; `[]`, `in`.

---

## 7. Database operations and at-expressions

Index: https://docs.chromia.com/rell/language-features/database/  
Overview (the real at-operator spec): https://docs.chromia.com/rell/language-features/database/overview

### 7.1 Where-clause notations

Comma notation allows a bare variable as a shortcut (matched by name or type):

```rell
val name = 'Bill';
val company = 'Microsoft';
return user @ { name, company };
```

`and` notation requires full expressions: `user @ { .name == name and .company == company }`. They are **not** completely equivalent.

Playground has an **SQL dry-run** mode that compiles entities + `query main()` and shows the SQL without a database.

### 7.2 At-operator parts

`FROM @ CARDINALITY { WHERE } ( WHAT ) TAIL`

**From:** one entity `user @* { … }` or several `(user, company) @* { … }` with optional aliases `(u: user, c: company)`.

**Where:** zero or more comma-separated expressions (all must match). `.name` or `user.name` / `u.name`. Implicit match by name or type (local `name: text` matches attribute `name`; local `ms: company` matches the `company`-typed attribute).

**What:** attributes to return. Empty → the entity reference (rowid). One expression → that type. Several → a tuple.

WHAT annotations:

| Annotation | Effect |
|---|---|
| `@sort` / `@sort_desc` | order |
| named field `x = .company.name` | named tuple field |
| implicit name from a single attribute | `.first_name` → field `first_name` |
| `_ = .first_name` | unnamed field |
| `@omit` | compute (e.g. sort key) but drop from the result |
| `@group` | GROUP BY |
| `@min` `@max` `@sum` | aggregates; count = `@sum 1` |
| `@list` `@set` `@map` | collect into a collection (since 0.13.9) |

`@list` / `@set` / `@map` in a **database** at-expression: all matching rows are read, then grouped/aggregated **in memory**. `data @ {} ( @list .v )` → `list<T>`; `data @* {} ( @list .v )` → `list<list<T>>` with one element.

Grouping + map-of-lists: `data @* {} ( @group .k, @list .v ) @ {} ( @map $ )` → `map<K, list<V>>`.

**Tail:** `limit N`, `offset N`. Cardinality is tested **before** `limit`, so `user @ { .company == 'Microsoft' } limit 1` cannot fail with "more than one."

**Result type** = (from × what) as `T`, then cardinality wraps as `T` / `T?` / `list<T>`.

Nested at-operators are legal. `@*` in a nested at used with `empty()` / `exists()` compiles to a **single** SQL query; `@`, `@?`, `@+` become separate queries (global-functions page).

**Joins (since 0.13.10):**

```rell
(u: user, c: contract @* { c.user == u }) @* {} ( u, c )          // INNER JOIN
(u: user, @outer c: contract @* { c.user == u }) @* {} ( u, c )  // LEFT OUTER JOIN
```

Outer-joined entity has type `T?` — use `?.` / `?:`.

**`$`:** current item in an at-expression. `$.name` when the item has attributes. Used heavily for collection-at (`values @* { predicate($) }`).

### 7.3 create

Canonical: https://docs.chromia.com/rell/language-features/database/create

Must specify every attribute that has no default. Name may be omitted when matched by name or type. The created object can be used immediately.

**Bulk insert:** `create MyEntity(list<struct<MyEntity>>): list<MyEntity>` — one SQL statement; empty list → no SQL; returned list same size and order. Releases 0.13.5 introduced it; 0.14.9 allocates rowids in bulk.

### 7.4 update

Canonical: https://docs.chromia.com/rell/language-features/database/update

Cardinality `@` / `@?` / `@*` / `@+` — runtime error if the number of updated rows does not match. Only `mutable` attributes. Implicit match by name or type. Multi-entity form: **first** entity is updated; others are for the where-part. Also accepts an expression that yields an entity, `T?`, or a collection of entities. Single-attribute form: `u.salary += 5000;` (assignment is translated to `update`).

### 7.5 delete

Canonical: https://docs.chromia.com/rell/language-features/database/delete

Same cardinality operators. Multi-entity form deletes only the **first** entity. Also accepts an entity / `T?` / collection expression: `delete u;`.

`@log` entities cannot be deleted (entity page).

---

## 8. Expressions, operators, statements

Index: https://docs.chromia.com/rell/language-features/expressions/  
Operators: https://docs.chromia.com/rell/language-features/expressions/operators

### 8.1 Operators

- Member `.` — `user.name`, `s.sub(5, 10)`
- Call `()` — `print('Hello')`
- Index `[]`
- Arithmetic: `+ - * / % ++ --` (`+` also concatenates text)
- Relational: `< > <= >=`, `in`, `not in`
- Equality: `==` `!=` compare **values** (recursive for collections/tuples/structs; entity values compare **object IDs**). `===` `!==` compare **references**, only for tuple, struct, list, set, map, GTV, range.
- Null check: `??` ≡ `!= null` (unary)
- Logical: `and`, `or`, `not` (words, not `&&` / `||`)
- Assignment: `=` `+=` `-=` `*=` `/=` `%=` — on an entity attribute this becomes `update`
- At-operator `@`
- `if` expression — see below

### 8.2 Conditional expressions (value-block arms since 0.16.1)

Canonical: https://docs.chromia.com/rell/language-features/expressions/conditional-expressions  
Statement forms: https://docs.chromia.com/rell/language-features/statements/conditional-statements

```rell
val max = if (a >= b) a else b;
val size = when (n) {
    0 -> 'zero';
    1, 2 -> 'small';
    else -> 'big';
};
```

`if` expression **always** needs `else`. `when` expression must be exhaustive (`else` makes it so). Only the chosen arm evaluates. Inside an at-expression, `when` becomes SQL `CASE WHEN … THEN`.

**Value-block arms** (0.16.1): statements then a trailing expression **without** a semicolon.

- Enclosing locals are visible and assignable; nullability analysis flows through.
- `return` returns from the enclosing **function**. An arm that always returns contributes no value; the other arms determine the type.
- `break` / `continue` bind to the enclosing loop.
- At-expression scope (`$`, attributes) applies inside a block arm of a collection-at projection.
- A statements-only block is `unit`-typed and illegal as a conditional arm unless it always returns.
- In at-expression bodies and default-value expressions, `return`/`break`/`continue` cannot escape the block.
- Illegal in a global constant (statements cannot be validated as constant).

### 8.3 Jump expressions (since 0.16.1)

Canonical: https://docs.chromia.com/rell/language-features/expressions/jump-expressions

`return`, `break`, `continue` as expressions. They never produce a value; type is an internal bottom type (unwritable) assignable to every type.

Useful:

```rell
val y = x ?: return -1;                 // y: integer; x smart-cast non-null afterwards
val u = user @? { .name == n } ?: return 'no such user';  // u: user
val v = if (n < 0) return -n else n * 2;
```

`return` is greedy: `x ?: return 1 + 2` returns `3`. Same placement rules as the statement. Illegal in lambda bodies. Illegal where there is no enclosing body/loop. Cannot appear in a database at-expression's SQL. A conditional whose arms all jump is bottom-typed; code after it is dead (`stmt_deadcode`). Consuming a jump as a real value warns `expr:unreachable`. `1 + (return 2)` is still a type error.

### 8.4 Lambdas (since 0.16.1)

Canonical: https://docs.chromia.com/rell/language-features/expressions/lambda-expressions

```rell
x -> x * 2
(x, y) -> x + y
() -> 42
n -> { val doubled = n * 2; doubled + 1 }
```

- Body is an expression or a value block. **`return` is illegal** inside a lambda; the result is the final expression.
- Parameter types from annotation **or** expected function type. Annotate all parameters or none. Return type always inferred.
- Untyped parameters with no expected function type = compile error. Parameterless lambdas need neither.
- Capture is **by value at creation** for the binding. Later reassignment of the outer variable is **not** seen. A captured mutable object is captured **by reference**, so in-place mutation **is** seen. Captured variables are read-only inside the lambda.

```rell
var n = 7;
val g = x -> x + n;
n = 100;
g(1);   // 8
```

### 8.5 Statements

Index: https://docs.chromia.com/rell/language-features/statements/  
Basic: https://docs.chromia.com/rell/language-features/statements/basic-statements  
Locals: https://docs.chromia.com/rell/language-features/statements/local-variable  
Loops: https://docs.chromia.com/rell/language-features/statements/loop-statements

- Assignment / compound assignment / `values[i] = z`
- Function-call statement
- `return;` / `return 123;`
- Block `{ … }` (scope)
- `val` — immutable after init; **may exist outside a function** (module-level constants)
- `var` — mutable; **cannot** exist outside a function. May be declared without an initializer (`var x: integer;`) and assigned later
- `if` / `else if` / `else` (statement form need not be exhaustive)
- `when (x) { 1 -> …; 2, 3 -> …; else -> … }` — Kotlin-like. Cases may be constants (compiler checks uniqueness) or arbitrary expressions. Enum cases use simple names. `when { cond -> … }` is `if / else if`. Comma-separated cases in the no-arg form are OR'd.
- `for (x in range_or_collection)` — range, list, set, map; tuple unpack `for ((n, s) in l)`
- `while (cond) { … }`
- `break` / `continue`

---

## 9. System library

Index: https://docs.chromia.com/rell/language-features/systemlib/  
These pages are summaries; they all point at the generated Standard Library (https://docs.chromia.com/pages/rell-stdlib/ — **not** under `/rell/` in the sitemap). Full signatures live there.

### 9.1 Global functions

Canonical: https://docs.chromia.com/rell/language-features/systemlib/global-functions

No namespace prefix:

- Math: `abs`, `min`, `max` (integer, big_integer, decimal)
- Collection/null checks: `empty`, `exists` (nullable, list, set, map). In at-expressions, nest `@*` for a single SQL query
- Output: `print`, `log`
- Hashes: `sha256`, `keccak256`
- `verify_signature`
- `eth_ecrecover`
- Error handling: `require`, `rell.error`, `try_call`, `try_call_catch` — own page

### 9.2 require and error handling

Canonical: https://docs.chromia.com/rell/language-features/systemlib/require-function

```rell
require(boolean, text)                 // throw if false
require(T?, text): T                   // throw if null, else return T
require_not_empty(T?, text): T
require_not_empty(list|set|map, text)  // throw if empty
```

Nullable collection + `require_not_empty` fails on null **or** empty.

**`try_call`:** catch exceptions; optional fallback. `try_call(integer.from_hex(s, *))` → `T?`; `try_call(fn, default)` → default on failure. Since 0.14.2, caught exceptions are logged with stack traces.

**`try_call_catch` (0.14.16):** catches **only** `require` exceptions; returns `try_call_result` with the error message; rethrows other exceptions.

Both restore database state on failure (writes rolled back).

**`rell.error()` (0.14.15):** fail unconditionally. Unlike `require(false, msg)`, it is treated as end of control flow (like `return`), so the compiler smart-casts afterwards.

### 9.3 System entities

Canonical: https://docs.chromia.com/rell/language-features/systemlib/system-entities

Immutable. Cannot be created, modified, or deleted from code.

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

### 9.4 System queries

Canonical: https://docs.chromia.com/rell/language-features/systemlib/system-queries

| Query | Returns |
|---|---|
| `rell.get_rell_version()` | e.g. `"0.10.1"` |
| `rell.get_postchain_version()` | e.g. `"3.0.0"` |
| `rell.get_build()` | one-line build string |
| `rell.get_build_details()` | `map<text, text>` (rell.*, postchain.version, kotlin.version) |
| `rell.get_app_structure()` | `map<text, gtv>` of modules and definitions; entity/object/struct/enum attributes as **arrays** (order-preserving since 0.13.3) |
| `rell.get_mount_names(kinds, modules)` | `map<text, list<text>>` keyed `queries`/`operations`/`entities`/`objects`. Empty `kinds` = all; empty `modules` = all. Invalid kind/module name throws; unknown module name is ignored. Since 0.13.13. |
| `rell.get_module_args(modules)` | `map<text, map<text, gtv>>`. Same empty-list / error rules. Since 0.13.13. |

### 9.5 `chain_context`

Canonical: https://docs.chromia.com/rell/language-features/systemlib/namespaces/chain_context

- `chain_context.args` — this **module's** `module_args` struct, filled from `chromia.yml` `moduleArgs`. Access is only possible if that module defines `struct module_args { … }`. Defaults may be omitted in YAML; if every attribute has a default, the args section may be omitted entirely. Every module can have its own `module_args`; the type of `args` differs per module.
- `chain_context.blockchain_rid: byte_array`
- `chain_context.raw_config: gtv` — raw blockchain config, e.g. `{"gtx":{"rell":{"mainFile":"main.rell"}}}`

### 9.6 `op_context`

Canonical: https://docs.chromia.com/rell/language-features/systemlib/namespaces/op_context

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

**Warning on the page:** do **not** read `op_context.transaction.block` except `block_height` — other block attributes are null during the building block and throw. Use `block_height` and `last_block_time`.

### 9.7 `crypto`

Canonical: https://docs.chromia.com/rell/language-features/systemlib/namespaces/crypto  
(Full signatures: Standard Library `crypto` reference, not under `/rell/`.)

Named on this page:

- Hashing: `crypto.sha256`, `crypto.keccak256` (32-byte hashes). Global `sha256` / `keccak256` also exist.
- Key derivation: `crypto.privkey_to_pubkey`
- Signatures: `crypto.verify_signature`, `crypto.get_signature` (0.13.11)
- Ethereum: `crypto.eth_sign`, `crypto.eth_ecrecover`, `crypto.eth_privkey_to_address`, `crypto.eth_pubkey_to_address`
- Encoding: `crypto.pubkey_encode`, `crypto.pubkey_to_xy`, `crypto.xy_to_pubkey`

All functions that accept a public key accept compressed **33-byte**, uncompressed **65-byte**, and **64-byte** (as returned by `eth_ecrecover`).

```rell
val ok = crypto.verify_signature(message, pubkey, signature);
val (r, s, rec_id) = crypto.eth_sign(hash, privkey);
val recovered = x'04' + crypto.eth_ecrecover(r, s, rec_id, hash);
```

### 9.8 `rell.meta`

Canonical: https://docs.chromia.com/rell/language-features/systemlib/namespaces/meta

`rell.meta(definition)` for an entity, object, operation, or query.

| Field | Meaning |
|---|---|
| `simple_name` | last segment (`bar` of `lib:foo.bar`) |
| `full_name` | `<module>:[<ns>.]<simple>` ; root module is `:foo` |
| `module_name` | empty string for the root module |
| `mount_name` | mount name; runtime error if the definition has none — "all supported definitions have mount names" |

```rell
query get_op_name() = rell.meta(my_op).mount_name;
```

### 9.9 `rell.time` (since 0.14.14)

Canonical: https://docs.chromia.com/rell/language-features/systemlib/namespaces/time

UTC format/parse. Central type `rell.time.format`.

Specifiers: `y M w W D d E u a H h m s S`. Quoted literals `'…'`; escape a quote as `''`.

- `rell.time.format(pattern)`
- `.ms_to_text(ms)`, `.text_to_ms(text)` (throws), `.text_to_ms_or_null(text)`, `.to_text()` (returns the pattern)
- Static equivalents: `rell.time.ms_to_text(pattern, ms)`, `rell.time.text_to_ms(pattern, text)`, `rell.time.text_to_ms_or_null(pattern, text)`

Unix timestamps are **milliseconds**.

---

## 10. Testing

Canonical: https://docs.chromia.com/rell/tests  
`@test` function rules also: https://docs.chromia.com/rell/language-features/modules/function  
Run: `chr test --settings chromia.yml --modules my_test_module` (this page). CLI details belong in `rell-cli.md`.

A test module is annotated `@test module;`. Functions executed:

- name `test` or prefix `test_`
- or `@test function foo()` (preferred)

If the module name ends in `_test`, it is the companion of the same name without the suffix (`program` ↔ `program_test`).

**`@disabled` (0.15.1):** on a test module, skips every test in that module and its submodules. On a test function, skips that function. Using `@disabled` on a non-test is a compile error.

Each test function runs **independently**. Output includes per-test and suite duration.

**Builders (fluent):**

- `rell.test.block` — block of transactions
- `rell.test.tx` — transaction of operations
- `rell.test.op` — produced by calling an operation in test scope
- `.run()` execute; `.run_must_fail()` / `.run_must_fail("message")` assert failure

```rell
val tx = rell.test.tx(data.add_user('Bob'));
// entities unchanged until:
tx.run();
```

Multi-tx: `rell.test.block().tx(tx1).tx(tx2).tx(tx3).run();`

**Assertions named on this page:** `assert_equals`, `assert_not_equals`, `assert_true`, `assert_false`, `assert_null`, `assert_not_null`, `assert_gt` / `ge` / `lt` / `le`, range forms `assert_ge_le` / `assert_ge_lt` / `assert_gt_le` / `assert_gt_lt`, `assert_fails`, `assert_events`.

**Test keys — never production:** `rell.test.keypairs` / `.pubkeys` / `.privkeys` with names `alice`, `bob`, `charlie`, `dave`, `eve`, `frank`, `grace`, `heidi`, `trudy`. `rell.test.keypair` is `{ priv: 32 bytes, pub: 33 bytes }`.

**Block time (deterministic):**

- `rell.test.last_block_time`
- `rell.test.next_block_time`
- `rell.test.block_interval` — default **10 seconds**
- `rell.test.set_next_block_time`
- `rell.test.set_next_block_time_delta`
- `rell.test.set_block_interval`

This page does **not** name `DEFAULT_BLOCK_INTERVAL` or `DEFAULT_FIRST_BLOCK_TIME`. Those names appear in `rell-cli.md` (likely from the stdlib API, which is outside `/rell/`).

**Other:** `rell.test.nop()` (unique no-op so identical txs differ), `rell.test.get_events()`.

Best practices on this page: name tests after the business scenario (`test_get_full_user_profile_by_id`); always pass the expected string to `run_must_fail`; one failure mode per negative test; helper functions for user setup.

There is **no** official `setUp` / `tearDown` / fixtures keyword.

---

## 11. Special operations

Canonical: https://docs.chromia.com/rell/special-operations  
Mount rules: https://docs.chromia.com/rell/language-features/modules/mount

Auto-invoked lifecycle / integration hooks. **Global namespace** — collisions are possible; always put the extension name in `@mount('ext.name')` and keep the `__` prefix on the operation.

| Operation | Location | When |
|---|---|---|
| `__begin_block(height: integer)` | on-chain | before regular txs in the block |
| `__end_block(height: integer)` | on-chain | after regular txs |
| `__icmf_message` | cross-chain | ICMF inbound |
| `__evm_block` | cross-chain | EIF has processed an EVM block |
| `__timeb` | on-chain | tx valid only inside a UTC time window. Client clock must be reasonably in sync with the signer; timezone/DST skew fails it. All timestamps UTC. |

Examples on the page also show `__stork_oracle_prices` and `__bridge_deposit` as extension-specific special ops (same `__` + dotted mount convention).

---

## 12. Security (official Rell security page)

Canonical: https://docs.chromia.com/rell/security

This page is mostly **product/config** advice, not language semantics.

Against malicious dapps:

- Delayed config updates: `config.directory_chain.config_delay` in ms (example 86400000 = 24h)
- Governance starter kit (`lib.governance.proposals` / `lib.governance.votes` moduleArgs: option limits, min/max duration, veto period)

Against malicious users:

- FT4 rate limiting (`lib.ft4.core.accounts.rate_limit`: `active`, `max_points`, `recovery_time`, `points_at_account_creation`)
- `require` for business-rule validation (example: `from != to`, `amount > 0`)

Language-level security that actually lives on other official pages:

- Overflow-throwing integer arithmetic (intro + releases 0.9.0)
- `op_context.is_signer` as the typical authorization pattern
- Guard blocks for validation-time checks
- Size-constraint annotations / early `require(x.size() <= MAX)`
- `@log` for append-only history
- Native functions must be deterministic
- Test keypairs must never ship

---

## 13. Best practices (official)

Canonical: https://docs.chromia.com/rell/rell-best-practices

Stated, with FT4-shaped examples (the FT4 types/functions are examples, not language rules):

- Composite keys for contextual uniqueness (`key accounts.account, asset`)
- Index what you filter / `@*` / join on; do not over-index (write cost)
- Pagination for list queries (page points at cookbook, not specified here)
- Negative tests: `run_must_fail("Amount must be positive")` then `assert_true(failure.message.contains(…))`
- Validate inputs (`name.size()`, `symbol.matches("^[A-Z0-9_]+$")`, decimals 0–18)
- Missing balance → `@?` then `if (record != null) amount else 0`, not a crash
- Index fields used for lookup / expiry cleanup
- Batch validation **before** writes; group by asset and `@sum .amount` once
- Formatting: spaces, indented blocks, multi-line parameters for complex ops
- Helpers: `require_account_exists(id) = account @ { .id == id }`, `get_account_balance` via `@?`
- Rate limiting (pointer only)
- Account/asset IDs in the FT4 examples are 32 bytes and not all-zero — **example**, not a language rule
- RellDoc on non-obvious costs (indexing, state size)

---

## 14. Analyzing / optimizing generated SQL

Canonical: https://docs.chromia.com/rell/analyze-rell-dapp-code

Tool on this page: `chr repl --sql-log --use-db --module main`, then run a query and read `SqlConnectionLogger`. Observed table names in the official log: `"c0.housekey"`, `"c0.owner"` (blockchain-id prefix + mount name).

Advice the page actually gives:

1. Fields in WHERE should be `key`; fields in JOIN should be `index`.
2. Put the most selective predicate first in WHERE (and consider FROM/join order). The generated SQL order follows the Rell FROM/WHERE order.
3. Reduce join count: put entities you traverse in the FROM list instead of walking `a.b.c` in WHERE/WHAT (each hop becomes a join).

The page's worked example ends with `(house, housekey, house_owner) @* { house.address == address, house_owner.house == house, house_owner.owner == housekey.owner }`.

This is **not** the same as `chr test --sql-log` (removed from the CLI in 0.31.0 per `rell-cli.md`). The analyze page only documents `chr repl --sql-log`.

---

## 15. RellDoc

Canonical: https://docs.chromia.com/rell/rell-doc  
Generator: `chr generate docs-site` (this page points at "Generating Documentation Sites" under the CLI).

`/** … */` with leading `*`. Markdown body. Tags:

| Tag | Role |
|---|---|
| `@param name description` | parameter |
| `@return description` | return value (`@returns` was the 0.13.14 name; replaced by `@return` in 0.14.0) |
| `@throws exception description` | exception |
| `@see reference description` | pointer |
| `@since version` | introduced-in |
| `@author name` | author (0.14.0) |

Introduced 0.13.14.

---

## 16. Releases that change production language behavior

Canonical: https://docs.chromia.com/rell/releases  
Docs-site latest: **0.16.4 (2026-08-02)**.

Production-relevant language changes (from that table; not a full changelog):

| Version | Date | What matters |
|---|---|---|
| 0.16.4 | 2026-08-02 | Linter: prefer `??`; flag row-by-row entity attribute reads in loops (`rule_prefer_at_projection`). Formatter/LSP/error-message work. |
| 0.16.3–0.16.2 | 2026-07 | Linter on by default; SQL generation locale-independent. |
| 0.16.1 | 2026-07-15 | Lambdas; value-block `if`/`when` arms; jump expressions. Named-arg fix for multiple optionals. |
| 0.16.0 | 2026-05-28 | Resolved runtime model; jOOQ SQL; ANTLR mainline parser. |
| 0.15.3 | 2026-03-30 | `gtv_type` / `gtv.type`. |
| 0.15.2 | 2026-02-24 | Snapshots; drops mixed-version attributes compatibility mode. |
| 0.15.1 | 2026-02-09 | Size annotations on entity/object attrs; `@mount` on attributes; `@disabled` tests; **dropped columns** for removed attrs. |
| 0.15.0 | 2025-12-09 | `@native`; guard during validation; **GTV `big_integer` JSON serialization change**. |
| 0.14.16 | 2025-11-13 | JSON `[]`/getters; `text.regex_replace`; `try_call_catch`; `struct.copy()`; collection `+ - &`. |
| 0.14.15 | 2025-10-03 | `rell.error()`. |
| 0.14.14 | 2025-09-19 | `rell.time`; size annotations on parameters. |
| 0.14.13 | 2025-09-09 | `@compound` `@singular`; function `@test`; `match_groups`; size annotations on struct attrs; locale-independent case conversion. |
| 0.14.12 | 2025-06-19 | Optional struct attrs omitable in op/query calls and `from_gtv*`. |
| 0.14.9 | 2025-04-17 | Bulk rowid allocation for multi-row insert. |
| 0.14.8 | 2025-03-27 | Backward compat down to language version **0.10.9**. |
| 0.14.5 | 2025-01-24 | `T.hash()` follows chain merkle version; `gtv.legacy_hash(value, version)`. |
| 0.14.3 | 2024-11-28 | Default params on ops/queries from chain; new params at the **end**. |
| 0.14.0 | 2024-08-19 | Nested-property null analysis; `?.` multi-part; at-expressions influence nullability; `@author` `@return` `@throws`. |
| 0.13.13 | 2024-05-29 | `rell.get_mount_names`, `rell.get_module_args`. |
| 0.13.12 | 2024-05-05 | Anonymous imports; trailing commas; more reserved names. |
| 0.13.11 | 2024-04-15 | Mount names max **58** chars; `crypto.get_signature()`; `decimal.to_text(true)` scientific; `gtx.rell.compilerVersion`. |
| 0.13.10 | 2024-04-02 | Inner/outer join syntax; nullable comparisons; `iterable.join_to_text()`; enhanced `rell.meta`; `try_call()`; language-version gates. |
| 0.13.9 | 2024-02-23 | Add/remove keys/indexes at DB init; `@list` `@set` `@map`; **strict GTV** mode. |
| 0.13.5 | 2023-11-14 | Crypto key/address helpers; `rell.meta`; module_args defaults; **bulk `create`**. |
| 0.13.4 | 2023-10-30 | `gtx_operation` / `struct<operation>` / `rell.test.op` conversions. |
| 0.13.3 | 2023-10-25 | `get_app_structure` attributes as arrays; deterministic test block timestamps; `op_context.get_current_operation()`. |
| 0.13.0 | 2023-05-23 | `try_call()`; `gtv.from_bytes_or_null()`; event assertions. |
| 0.12.0 | 2023-03-22 | `big_integer`; max name lengths for entities/attributes (lengths not restated on current feature pages). |
| 0.10.7 | 2021-09-21 | Extendable functions. |
| 0.10.6 | 2021-09-02 | Function types + partial application; `iterable<T>`; `null.to_gtv()`. |
| 0.10.4 | 2021-06-25 | Test tx API; collection-at; `continue`; `offset`; `struct<entity/object/operation/mutable T>`; `not in`; `text.like()`. |
| 0.10.3 | 2020-09-24 | Guard blocks; named args; default params; group-by; `sha256`/`keccak256`/`eth_ecrecover`. |
| 0.10.1 | 2019-12-10 | `@external`; abstract modules; `@omit`; `@sort`/`@sort_desc`; system queries. |
| 0.10.0 | 2019-11-04 | Module system; class→entity, record→struct; `@mount`. |
| 0.9.1 | 2019-08-30 | `decimal`. |
| 0.9.0 | 2019-07-24 | `gtv` rename; `virtual<T>`; `??`; **integer overflow throws**; `rowid`. |
| 0.7.0 | 2019-02-20 | `object`; `enum`; `if` expression; cardinality on update/delete; `block`/`transaction`; `@log`; table names prefixed by blockchain ID. |

Older rows (0.8.0 and below) are historical (include directive, GTXValue, class-not-entity). Compatibility floor cited on 0.14.8: **0.10.9**.

The releases page says: "For more detailed changes in each version, please refer to the specific release file." Those files are not hosted under `/rell/` on the docs site.

---

## 17. Contradictions / extras vs `/workspace/chromia-knowledge/rell-cli.md`

`rell-cli.md` exists and was read. Most of its language brief matches the official `/rell/` pages. The differences:

### 17.1 Not contradictions — `rell-cli.md` synthesized from official releases

These facts are **true on official docs**, but live on https://docs.chromia.com/rell/releases (or another `/rell/` page) rather than on the feature page `rell-cli.md` cites:

- Removed entity attributes drop columns automatically — **releases 0.15.1**, not the entity page.
- Mount names max 58 characters — **releases 0.13.11**, not the mount page.
- Keys/indexes addable/removable at DB init — **releases 0.13.9**, not the entity page.
- Guard runs during validation since 0.15.0 — **releases 0.15.0**; the operation page describes the dual run without that date.
- `@test` function annotation since 0.14.13 — **releases 0.14.13**; the function page recommends it without dating it.
- Default params on queries must be appended — **releases 0.14.3** (ops *and* queries). The **query** page documents defaults but is silent on append-only. The **operation** page states the append rule. `rell-cli.md` applying the same rule to queries is justified by the releases page, not by the query page.

### 17.2 Extra claims in `rell-cli.md` that the `/rell/` sitemap pages do not state

These are **not disproven**. They come from pages outside `/rell/` (CLI, GTV protocol, generated stdlib) or from GitLab:

- Full GTV conversion table (operation input / query input / query output, strict vs non-strict). Official Rell type pages only say "must be GTV-compatible" and list `from_gtv` / `to_gtv`. The table is not on any `/rell/` page crawled.
- `rell.test.DEFAULT_BLOCK_INTERVAL` (10000 ms) and `rell.test.DEFAULT_FIRST_BLOCK_TIME` (2020-01-01 00:00 UTC). Official tests page says the interval default is 10 seconds and does not name those constants or the first-block date.
- `set_block_interval` "does not override an already-scheduled next time"; `set_next_block_time_delta` "no-op if there is no previous block." Official tests page names the functions without those caveats.
- Enum reorder/remove is treated as dangerous by `chr deployment update` (CLI 0.30.0+). Official enum page is silent on deployment.
- Exact PostgreSQL identifier form `"c<iid>.<mount>"` — observed in the official analyze-page SQL log (`"c0.housekey"`), so the *example* is official; the generalized formula is `rell-cli.md` inference.
- GitLab Rell tags through **0.16.7** (2026-08-14) vs docs-site latest **0.16.4**. `rell-cli.md` already records this lag. This study of docs.chromia.com cannot confirm 0.16.5–0.16.7 language changes.
- CLI `--sql-log` removed from `chr test` in CLI 0.31.0. The Rell analyze page only documents `chr repl --sql-log`, so there is **no conflict** with the language docs.

### 17.3 Wording mismatches (not semantic conflicts)

- Entity-nullable: both agree (complex-types page + `rell-cli.md`). The entity page itself does not say it.
- `rell-cli.md` §1.5 says "Since 0.15.0 the guard also runs during operation validation." Official operation page says guards run in `checkCorrectness()` and `apply()`. Same behavior, different dating.
- Core-concepts WebFetch rendered the comment marker as `/`; `rell-cli.md` and all examples use `//`. Treat `//` as official.

### 17.4 Language features on official `/rell/` pages that `rell-cli.md` compresses or omits

Worth knowing if you only read `rell-cli.md`:

- Lambdas, value-block arms, jump expressions (0.16.1) — not in the `rell-cli.md` language brief that was read.
- Collection operators `+ - &` and `*_copy` (0.14.16).
- `struct.copy()` (0.14.16) — not on the struct page either; only in releases.
- JSON `[]` / `get` / `as_*` / `is_*` (0.14.16).
- `text.match_groups` / `match_named_groups` / `regex_replace`.
- `rell.error()`, `try_call_catch`.
- Linter rules `rule_prefer_null_check_operator` and `rule_prefer_at_projection` (0.16.4).
- `@outer` join; `@list`/`@set`/`@map` in-memory aggregation caveat.
- Partial-application default-binding-at-creation and named-wildcard ordering.
- `virtual<T>` API surface.
- `rell.time` specifier table and static helpers.
- `crypto.*` pubkey format note (33/65/64-byte).
- RellDoc tag set.
- Size-constraint **relax-only** on non-empty entities.

---

## 18. Gaps (official pages silent or thin)

Do not invent answers for these. They are gaps after reading every `/rell/` sitemap URL.

1. **`@external` / cross-chain module syntax.** Mentioned on the modules index and in releases 0.10.1. No dedicated page, no current syntax examples under `/rell/`.
2. **Full reserved-keyword list.** Identifiers page names a few; 0.13.12 added more unnamed reserved words.
3. **Complete GTV-compatibility exclusion set.** Complex-types says some types are not GTV-compatible. Only `virtual<T>` is explicitly excluded (cannot be a query return). Function types, `range`, entity types as query results, etc. are not listed.
4. **GTV wire table** (how enum/struct/decimal/boolean encode on op input vs query output). Not on any `/rell/` page.
5. **Exact PostgreSQL column types** for `text`, `byte_array`, `boolean`, `enum`, `timestamp`. Official statements: `big_integer`/`decimal` → SQL `NUMERIC`; `json` → PostgreSQL `JSON`; `rowid` is 64-bit; tables named like `"c0.foo.user"`.
6. **`CREATE TABLE` dump.** None. Only runtime SQL logs on the analyze page.
7. **Enum schema evolution** on the language pages. Silent. CLI behavior lives outside `/rell/`.
8. **`struct.copy()` signature.** Named in releases 0.14.16; not documented on the struct page.
9. **Max name lengths** for entities/attributes (releases 0.12.0). The lengths are not restated on current feature pages.
10. **Generated Standard Library** (https://docs.chromia.com/pages/rell-stdlib/ and https://docs.chromia.com/pages/rell/index.html). Linked from every systemlib page. **Not** in the `/rell/` sitemap. Full member lists for `crypto`, `rell.test`, `op_context`, collection methods beyond the tables on the feature pages live there.
11. **Rell 0.16.5–0.16.7.** Present on GitLab per `rell-cli.md`; absent from docs.chromia.com/rell/releases.
12. **Playground / Learn Rell courses** linked from the nav (relationships, first dapp, React). Not in the `/rell/` sitemap set this study fetched as primary sources.
13. **`list._sort()`** — the collection-types page writes the in-place sort as `._sort()`. No other page confirms whether the public name is `_sort` or `sort`. Do not rename it.
14. **Special-op parameter lists** besides `__begin_block(height: integer)` / `__end_block(height: integer)`. `__icmf_message`, `__evm_block`, `__timeb` are named without official parameter signatures on the special-operations page.
15. **`__timeb` window arguments.** The page describes the purpose (UTC ms window, clock sync) but does not show the operation signature.

---

## 19. Sitemap URL checklist (all fetched)

From https://docs.chromia.com/sitemap.xml on 2026-08-26. Every URL below was read (WebFetch and/or curl).

| URL | Role |
|---|---|
| https://docs.chromia.com/rell/rell-intro | Language pitch, safety claims, playground |
| https://docs.chromia.com/rell/releases | Version table through 0.16.4 |
| https://docs.chromia.com/rell/core-concepts | Entities, ops, queries, at-pipeline, keys, comments |
| https://docs.chromia.com/rell/modules | Module kinds, imports, runtime activity |
| https://docs.chromia.com/rell/special-operations | `__begin_block` / `__end_block` / `__icmf_message` / `__evm_block` / `__timeb` |
| https://docs.chromia.com/rell/tests | `@test` modules, builders, asserts, keys, time |
| https://docs.chromia.com/rell/rell-doc | RellDoc tags |
| https://docs.chromia.com/rell/analyze-rell-dapp-code | `chr repl --sql-log`, index/join/order advice |
| https://docs.chromia.com/rell/rell-best-practices | FT4-shaped modelling, validation, batching |
| https://docs.chromia.com/rell/security | config_delay, governance, FT4 rate limit, require |
| https://docs.chromia.com/rell/language-features/ | Language-reference hub |
| https://docs.chromia.com/rell/language-features/identifiers-syntax | Identifier rules |
| https://docs.chromia.com/rell/language-features/modules/ | Definitions hub (mentions external modules) |
| https://docs.chromia.com/rell/language-features/modules/entity | Entity, keys, relations, `@log`, schema change |
| https://docs.chromia.com/rell/language-features/modules/object | Singleton object |
| https://docs.chromia.com/rell/language-features/modules/struct | Structs, GTV helpers, `struct<operation>` |
| https://docs.chromia.com/rell/language-features/modules/enum | Enum API |
| https://docs.chromia.com/rell/language-features/modules/function | Functions, partial app, extendable, `@test`, `@native` |
| https://docs.chromia.com/rell/language-features/modules/operation | Ops, defaults, `@compound`/`@singular`, guard |
| https://docs.chromia.com/rell/language-features/modules/query | Queries, defaults, size annotations |
| https://docs.chromia.com/rell/language-features/modules/namespace | Namespaces, anonymous import |
| https://docs.chromia.com/rell/language-features/modules/mount | Mount names, attribute `@mount`, special-op mounts |
| https://docs.chromia.com/rell/language-features/modules/abstract | Abstract modules / override |
| https://docs.chromia.com/rell/language-features/modules/size-constraint-annotations | `@size` / `@min_size` / `@max_size` |
| https://docs.chromia.com/rell/language-features/types/ | Types hub |
| https://docs.chromia.com/rell/language-features/types/simple-types | boolean…json, aliases |
| https://docs.chromia.com/rell/language-features/types/complex-types | `T?`, tuple, range, gtv |
| https://docs.chromia.com/rell/language-features/types/collection-types | list/set/map, `+ - &` |
| https://docs.chromia.com/rell/language-features/types/iterables | iterable concept |
| https://docs.chromia.com/rell/language-features/types/sub-types | `T` <: `T?` |
| https://docs.chromia.com/rell/language-features/types/virtual-types | `virtual<T>` |
| https://docs.chromia.com/rell/language-features/database/ | DB hub |
| https://docs.chromia.com/rell/language-features/database/overview | At-operator spec, joins, aggregates |
| https://docs.chromia.com/rell/language-features/database/create | create + bulk |
| https://docs.chromia.com/rell/language-features/database/update | update + cardinality |
| https://docs.chromia.com/rell/language-features/database/delete | delete + cardinality |
| https://docs.chromia.com/rell/language-features/expressions/ | Expressions hub |
| https://docs.chromia.com/rell/language-features/expressions/operators | Operator table |
| https://docs.chromia.com/rell/language-features/expressions/values | Literals, trailing commas |
| https://docs.chromia.com/rell/language-features/expressions/conditional-expressions | if/when expressions, value blocks |
| https://docs.chromia.com/rell/language-features/expressions/jump-expressions | return/break/continue as expr |
| https://docs.chromia.com/rell/language-features/expressions/lambda-expressions | Lambdas |
| https://docs.chromia.com/rell/language-features/statements/ | Statements hub |
| https://docs.chromia.com/rell/language-features/statements/basic-statements | assign, call, return, block |
| https://docs.chromia.com/rell/language-features/statements/conditional-statements | if / when statements |
| https://docs.chromia.com/rell/language-features/statements/local-variable | val / var |
| https://docs.chromia.com/rell/language-features/statements/loop-statements | for / while / break / continue |
| https://docs.chromia.com/rell/language-features/systemlib/ | Systemlib hub |
| https://docs.chromia.com/rell/language-features/systemlib/global-functions | abs/min/max/empty/exists/print/log/hashes |
| https://docs.chromia.com/rell/language-features/systemlib/require-function | require, try_call, rell.error |
| https://docs.chromia.com/rell/language-features/systemlib/system-entities | block, transaction |
| https://docs.chromia.com/rell/language-features/systemlib/system-queries | rell.get_* |
| https://docs.chromia.com/rell/language-features/systemlib/namespaces/ | Namespaces hub |
| https://docs.chromia.com/rell/language-features/systemlib/namespaces/chain_context | args, blockchain_rid, raw_config |
| https://docs.chromia.com/rell/language-features/systemlib/namespaces/op_context | signer, time, events |
| https://docs.chromia.com/rell/language-features/systemlib/namespaces/crypto | hash, sign, eth_* |
| https://docs.chromia.com/rell/language-features/systemlib/namespaces/meta | rell.meta |
| https://docs.chromia.com/rell/language-features/systemlib/namespaces/time | rell.time.format |

Linked from those pages but **out of this crawl's sitemap scope:**

- https://docs.chromia.com/pages/rell-stdlib/ (generated API)
- https://docs.chromia.com/pages/rell/index.html
- https://docs.chromia.com/build/cli/generating-doc-site
- https://docs.chromia.com/get-started/about/protocols/gtv (GTV wire format; used by `rell-cli.md`)
- Rell Playground / Chromia Learn course URLs

`https://docs.chromia.com/rell` itself is **404** (already noted in `rell-cli.md`).
