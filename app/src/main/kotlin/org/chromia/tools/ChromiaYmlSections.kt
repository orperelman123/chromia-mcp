package org.chromia.tools

/**
 * Official chromia.yml `database` / `test` snippets (CLI 0.33.x schema).
 * Source: docs.chromia.com/build/configuration/project-config and
 * /workspace/chromia-knowledge/rell-cli.md §3.4–3.5.
 * Java 21+, Postgres 16+. Do not emit cookbook-only keys
 * (test.timeout, test.parallel, database.schema_version).
 */
object ChromiaYmlSections {
    const val PROJECT_CONFIG_URL = "https://docs.chromia.com/build/configuration/project-config"
    const val DATABASE_OVERVIEW_URL = "https://docs.chromia.com/build/database/overview"
    const val DATABASE_GETTING_STARTED_URL = "https://docs.chromia.com/build/database/getting-started"
    const val DATABASE_ARCHITECTURE_404_URL = "https://docs.chromia.com/build/database/architecture"
    const val DATABASE_SCALING_404_URL = "https://docs.chromia.com/build/database/scaling"
    const val TABLE_NAME_SOURCE = "\"c\${chainId}.\${mount}\" / \"c{chainId}.{name}\""
    const val DRIVER = "org.postgresql.Driver"
    const val DEFAULT_DB = "postchain"
    const val DEFAULT_HOST = "localhost"
    const val DEFAULT_SCHEMA = "rell_app"

    fun databaseYaml(): String = """
        database:
          host: $DEFAULT_HOST
          database: $DEFAULT_DB
          username: $DEFAULT_DB
          password: ${'$'}{CHR_DB_PASSWORD:-$DEFAULT_DB}
          schema: $DEFAULT_SCHEMA
          driver: $DRIVER
          logSqlErrors: true
    """.trimIndent() + "\n"

    fun testYaml(name: String = DappScaffold.DEFAULT_NAME): String {
        val chain = DappScaffold.normalizeName(name)
        return """
            test:
              modules:
                - test.main_test
              failOnError: true
              # moduleArgs:            # optional; args for app or test modules during chr test
              #   test.arg_test:
              #     value: 4
              # Per-chain override: blockchains.$chain.test.{modules,moduleArgs,failOnError}
        """.trimIndent() + "\n"
    }

    fun notes(): String = """
        Official chromia.yml database / test sections (CLI ${DappScaffold.CLI_SERIES}).
        Schema: $PROJECT_CONFIG_URL. Local Postgres defaults: $DATABASE_OVERVIEW_URL.
        Official BUILD getting-started (200): $DATABASE_GETTING_STARTED_URL
        Official BUILD overview (200): $DATABASE_OVERVIEW_URL
        Official $DATABASE_ARCHITECTURE_404_URL is 404. Official $DATABASE_SCALING_404_URL is 404.
        Official getting-started says `chromia start` — that is NOT a chr command. Official local dapp loop is `chr node start` (Postgres 16+).
        Source-observed dapp table names (Postchain SQLDatabaseAccess / official sql-log): $TABLE_NAME_SOURCE in the current schema. Do not invent a SQL schema.
        Java 21+, Postgres 16+. Driver must be $DRIVER.
        Official local defaults: database/user/password all `$DEFAULT_DB` (install docs).
        Official 0.33.2 `chr create-rell-dapp` default chromia.yml only sets database.schema schema_my_rell_dapp (no host/user/password); local node still uses postchain database/user/password defaults / CHR_DB_*.
        Env overrides win over YAML: CHR_DB_URL, CHR_DB_USER, CHR_DB_PASSWORD, CHR_DB_SCHEMA.
        Do not commit production DB passwords. Substitution: ${'$'}{MY_VAR:-default}.
        test.modules are module names (e.g. test.main_test), not file paths.
        failOnError is overridable by `chr test --fail-on-error`.
        Cookbook-only keys test.timeout, test.parallel, database.schema_version are not on the official schema — do not use them.
        Official deploy-frontend-dapp (200) blockchain key webStatic (printed value out) is and accepted by validate_chromia_yml. Official project-config does not list webStatic. Do not invent siblings.
        This helper does not run chr and does not send signed transactions.
    """.trimIndent()
}
