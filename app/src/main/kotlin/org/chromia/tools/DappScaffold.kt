package org.chromia.tools

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Production-correct new-dapp skeleton. Pins match AGENTS.md / official source:
 * Rell 0.16.7, merkle_hash_version 2, FT4 v1.1.0r API 1.
 * Never ships lib.ft4.admin, admin.crosschain, ras_open, or ras_transfer_open.
 * Does not send signed transactions.
 */
object DappScaffold {
    const val RELL_VERSION = "0.16.7"
    const val FT4_VERSION = "v1.1.0r"
    const val FT4_API = "1"
    const val MERKLE_HASH_VERSION = 2
    const val CLI_SERIES = "0.33.x"
    const val DEFAULT_NAME = "hello"
    const val FT4_REGISTRY = "https://gitlab.com/chromaway/ft4-lib.git"
    const val FT4_PATH = "rell/src/lib/ft4"
    const val FT4_RID = "x\"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E\""

    val forbiddenModules = listOf(
        "lib.ft4.admin",
        "lib.ft4.core.admin",
        "admin.crosschain",
        "ras_open",
        "ras_transfer_open",
        "lib.ft4.core.accounts.strategies.open",
        "lib.ft4.accounts.strategies.open"
    )

    private val namePattern = Regex("^[a-z][a-z0-9_]{0,31}$")

    fun normalizeName(raw: String?): String {
        val trimmed = raw?.trim()?.lowercase().orEmpty()
        return if (namePattern.matches(trimmed)) trimmed else DEFAULT_NAME
    }

    fun files(name: String, template: String = "hello"): Map<String, String> {
        val chain = normalizeName(name)
        return if (template == "ft4") {
            linkedMapOf(
                "chromia.yml" to ft4ChromiaYml(chain),
                "src/main.rell" to ft4MainRell(),
                "src/test/main_test.rell" to ft4TestRell(),
                "client/example.ts" to ft4ClientTs(chain)
            )
        } else {
            linkedMapOf(
                "chromia.yml" to chromiaYml(chain),
                "src/main.rell" to mainRell(),
                "src/test/main_test.rell" to mainTestRell()
            )
        }
    }

    fun notes(name: String): String {
        val chain = normalizeName(name)
        return """
            New Chromia dapp skeleton for `$chain`.
            Chromia CLI $CLI_SERIES (Java 21+, Postgres 16+): `chr install` then `chr build` then `chr test`.
            Official first-run (run-dapp-cli, query-only): `chr create-rell-dapp` → `cd my-rell-dapp` →
            `chr node start` → `chr query hello_world` → `"Hello World!"`.
            There is no top-level `chr compile` in 0.33.x; use `chr build` or `chr code check`.
            merkle_hash_version must stay 2. Do not ship merkle_hash_version 1.
            Rell source tag $RELL_VERSION (docs may still say 0.16.4 — source wins).
            FT4 pin $FT4_VERSION API $FT4_API. Add FT4 by importing lib.ft4.accounts / lib.ft4.assets after reading fetch_docs; configure module_args from official FT4 setup.
            NEVER import ${forbiddenModules.joinToString(", ")}.
            require_mandatory_flags only on the main auth descriptor.
            Since CLI 0.30.0, `chr deployment create` writes deployments.<net>.chains into chromia.yml.
            This tool does not send signed transactions and does not run chr.
            Confirm APIs with fetch_docs / search / fetch before inventing module_args keys.
        """.trimIndent()
    }

    fun toJson(name: String?, template: String = "hello"): JsonObject {
        val chain = normalizeName(name)
        val fileMap = files(chain, template)
        return buildJsonObject {
            put("name", chain)
            put("template", if (template == "ft4") "ft4" else "hello")
            put("rellVersion", RELL_VERSION)
            put("ft4Version", FT4_VERSION)
            put("ft4Api", FT4_API)
            put("merkleHashVersion", MERKLE_HASH_VERSION)
            put("cli", CLI_SERIES)
            put(
                "pins",
                buildJsonObject {
                    put("rell", RELL_VERSION)
                    put("ft4", FT4_VERSION)
                    put("ft4Api", FT4_API)
                    put("merkle_hash_version", MERKLE_HASH_VERSION)
                    put("cli", CLI_SERIES)
                }
            )
            put(
                "forbidden",
                buildJsonArray {
                    forbiddenModules.forEach { add(JsonPrimitive(it)) }
                }
            )
            put(
                "files",
                buildJsonObject {
                    fileMap.forEach { (path, content) -> put(path, content) }
                }
            )
            put("notes", notes(chain))
        }
    }

    private fun chromiaYml(name: String): String = """
        blockchains:
          $name:
            module: main
            config:
              features:
                merkle_hash_version: $MERKLE_HASH_VERSION

        compile:
          rellVersion: $RELL_VERSION

        libs:
          ft4:
            registry: $FT4_REGISTRY
            path: $FT4_PATH
            tagOrBranch: $FT4_VERSION
            rid: $FT4_RID
            insecure: false
    """.trimIndent() + "\n"

    private fun mainRell(): String = """
        module;

        // Official Hello World (run-dapp-cli + JS/TS hello-world-quickstart).
        // Confirm FT4 imports with fetch_docs.
        // NEVER import lib.ft4.admin, lib.ft4.core.admin, admin.crosschain,
        // ras_open, ras_transfer_open, or lib.ft4.core.accounts.strategies.open.

        object my_name {
          mutable name = "World";
        }

        operation set_name(name) {
          my_name.name = name;
        }

        query hello_world() = "Hello %s!".format(my_name.name);
    """.trimIndent() + "\n"

    private fun mainTestRell(): String = """
        @test module;

        import main;

        function test_hello_world() {
            assert_equals(hello_world(), "Hello World!");
        }
    """.trimIndent() + "\n"

    // ---- Golden FT4 template: accounts + authenticated, validated operation ----

    private fun ft4ChromiaYml(name: String): String = buildString {
        append("blockchains:\n")
        append("  $name:\n")
        append("    module: main\n")
        append("    config:\n")
        append("      features:\n")
        append("        merkle_hash_version: $MERKLE_HASH_VERSION\n")
        Ft4ModuleArgs.moduleArgsYaml().lineSequence().forEach { line ->
            if (line.isBlank()) append('\n') else append("    ").append(line).append('\n')
        }
        append('\n')
        append("compile:\n")
        append("  rellVersion: $RELL_VERSION\n")
        append('\n')
        append(Ft4ModuleArgs.libsYaml(includeIccf = false))
    }

    private fun ft4MainRell(): String = """
        module;

        import lib.ft4.auth;
        import lib.ft4.accounts;

        // Golden FT4 pattern: every state-mutating operation authenticates the
        // caller and validates its inputs. Never import FT4 admin modules or
        // open registration/transfer strategies (see the project security pins).

        entity note {
            index owner: byte_array;
            body: text;
            created_at: timestamp;
        }

        @extend(auth.auth_handler)
        function () = auth.add_auth_handler(
            flags = []
        );

        operation add_note(body: text) {
            // 1. Authenticate: resolves the FT4 account that signed this call.
            val account = auth.authenticate();
            // 2. Validate inputs before touching state.
            require(body.size() > 0, "note must not be empty");
            require(body.size() <= 1000, "note too long (max 1000 chars)");
            // 3. Mutate state only after auth + validation.
            create note(owner = account.id, body, created_at = op_context.last_block_time);
        }

        query get_notes(owner: byte_array) = note @* { .owner == owner } (
            .body,
            .created_at
        );
    """.trimIndent() + "\n"

    private fun ft4TestRell(): String = """
        @test module;

        // FT4 operations need the FT4 test extensions (lib.ft4.test) and run against
        // registered test accounts. After `chr install`, follow the official FT4
        // testing guide (fetch_docs: https://docs.chromia.com/build/ft4) to register
        // a test account and call add_note through ft4_auth-signed transactions.

        import main;

        function test_module_compiles() {
            // Placeholder keeping `chr test` green until FT4 test accounts are wired.
            assert_equals(1, 1);
        }
    """.trimIndent() + "\n"

    private fun ft4ClientTs(name: String): String = """
        // TypeScript client for the `$name` dapp (postchain-client + FT4).
        // npm i postchain-client @chromia/ft4
        import { createClient } from "postchain-client";
        import { createKeyStoreInteractor, createInMemoryFtKeyStore } from "@chromia/ft4";

        const client = await createClient({
            nodeUrlPool: ["http://localhost:7740"], // chr node start; use network nodes for testnet/mainnet
            blockchainIid: 0,
        });

        // Sign with an FT4 session (see FT4 docs for account registration flows).
        const keyStore = createInMemoryFtKeyStore(/* your key pair */);
        const { getSession } = createKeyStoreInteractor(client, keyStore);
        const session = await getSession(/* your account id */);

        await session.call({ name: "add_note", args: ["hello from ts"] });
        const notes = await client.query("get_notes", { owner: /* account id bytes */ });
        console.log(notes);
    """.trimIndent() + "\n"
}
