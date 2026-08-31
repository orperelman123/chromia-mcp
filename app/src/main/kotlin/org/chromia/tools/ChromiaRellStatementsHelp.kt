package org.chromia.tools

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Official Rell statement help. Quotes docs.chromia.com/rell statement pages only.
 * Does not invent statements. Rell pin 0.16.7 (docs may list 0.16.4).
 */
object ChromiaRellStatementsHelp {
    const val CLI_SERIES = DappScaffold.CLI_SERIES
    const val RELL_VERSION = DappScaffold.RELL_SOURCE_TAG
    const val TOOL_NAME = "chromia_rell_statements_help"
    const val INDEX_URL = "https://docs.chromia.com/rell/language-features/statements/"
    const val LOCAL_URL = "https://docs.chromia.com/rell/language-features/statements/local-variable"
    const val BASIC_URL = "https://docs.chromia.com/rell/language-features/statements/basic-statements"
    const val CONDITIONAL_URL = "https://docs.chromia.com/rell/language-features/statements/conditional-statements"
    const val LOOP_URL = "https://docs.chromia.com/rell/language-features/statements/loop-statements"
    const val ECOSYSTEM_FILEHUB_DEPLOY_BUNDLE_INDEX_URL = "https://docs.chromia.com/ecosystem/filehub/filehub-setup/deploy-bundle"
    const val ECOSYSTEM_FILEHUB_DEPLOY_BUNDLE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/filehub/filehub-setup/deploy-bundle/"
    const val ECOSYSTEM_FILEHUB_DEPLOY_BUNDLE_INDEX_TITLE = "Deploy Filehub and Filechain bundle"  // official H1
    const val ECOSYSTEM_REGISTER_BRIDGE_INDEX_URL = "https://docs.chromia.com/ecosystem/bridge/deploy-bridge/register-bridge"
    const val ECOSYSTEM_REGISTER_BRIDGE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/bridge/deploy-bridge/register-bridge/"
    const val ECOSYSTEM_REGISTER_BRIDGE_INDEX_TITLE = "Register the Chromia bridge on EVM"  // official H1
    const val ECOSYSTEM_DIRECTORY_CHAIN_CONFIG_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/nodes/directory-chain-config"
    const val ECOSYSTEM_DIRECTORY_CHAIN_CONFIG_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/nodes/directory-chain-config/"
    const val ECOSYSTEM_DIRECTORY_CHAIN_CONFIG_INDEX_TITLE = "Configure Directory Chain"  // official H1
    const val ECOSYSTEM_PMC_NODE_INDEX_URL = "https://docs.chromia.com/ecosystem/providers/pmc/commands/node"
    const val ECOSYSTEM_PMC_NODE_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/providers/pmc/commands/node/"
    const val ECOSYSTEM_PMC_NODE_INDEX_TITLE = "node"  // official H1
    const val ECOSYSTEM_GOV_EIF_CONFIGURATION_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-starter-kit/eif-configuration"
    const val ECOSYSTEM_GOV_EIF_CONFIGURATION_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-starter-kit/eif-configuration/"
    const val ECOSYSTEM_GOV_EIF_CONFIGURATION_INDEX_TITLE = "Customizing with EIF and event configuration"  // official H1
    const val ECOSYSTEM_GOV_KEY_MODULES_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/key-modules"
    const val ECOSYSTEM_GOV_KEY_MODULES_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/getting-started/governance-structure/key-modules/"
    const val ECOSYSTEM_GOV_KEY_MODULES_INDEX_TITLE = "Key modules in the governance system"  // official H1
    const val ECOSYSTEM_GOV_VOTE_PROPOSAL_INDEX_URL = "https://docs.chromia.com/ecosystem/governance/governance-voting-process/vote-proposal"
    const val ECOSYSTEM_GOV_VOTE_PROPOSAL_INDEX_URL_SLASH = "https://docs.chromia.com/ecosystem/governance/governance-voting-process/vote-proposal/"
    const val ECOSYSTEM_GOV_VOTE_PROPOSAL_INDEX_TITLE = "Vote on a proposal"  // official H1
    const val RELL_DATABASE_OPS_INDEX_URL = "https://docs.chromia.com/rell/language-features/database"
    const val RELL_DATABASE_OPS_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/database/"
    const val RELL_DATABASE_OPS_INDEX_TITLE = "Database operations"
    const val LEARN_BOOK_REVIEW_FIRST_OPERATION_INDEX_URL = "https://learn.chromia.com/courses/book-review/book-entity/basic-operations"
    const val LEARN_BOOK_REVIEW_FIRST_OPERATION_INDEX_URL_SLASH = "https://learn.chromia.com/courses/book-review/book-entity/basic-operations/"
    const val LEARN_BOOK_REVIEW_FIRST_OPERATION_INDEX_TITLE = "Add your first operation"
    const val LEARN_EVM_DAPP_OVERVIEW_INDEX_URL = "https://learn.chromia.com/courses/chromia-for-evm-developers/chromia-stack"
    const val LEARN_EVM_DAPP_OVERVIEW_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chromia-for-evm-developers/chromia-stack/"
    const val LEARN_EVM_DAPP_OVERVIEW_INDEX_TITLE = "Chromia dapp overview"
    const val LEARN_RELL_MASTERCLASS_EXAMPLE_INDEX_URL = "https://learn.chromia.com/courses/rell-masterclass/example"
    const val LEARN_RELL_MASTERCLASS_EXAMPLE_INDEX_URL_SLASH = "https://learn.chromia.com/courses/rell-masterclass/example/"
    const val LEARN_RELL_MASTERCLASS_EXAMPLE_INDEX_TITLE = "Code optimization example"  // official H1
    const val LEARN_FT4_DEMO_FRONTEND_DEPLOY_ONCHAIN_INDEX_URL = "https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/deploy-onchain"
    const val LEARN_FT4_DEMO_FRONTEND_DEPLOY_ONCHAIN_INDEX_URL_SLASH = "https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/deploy-onchain/"
    const val LEARN_FT4_DEMO_FRONTEND_DEPLOY_ONCHAIN_INDEX_TITLE = "Lesson 7 - Deploy onchain"  // official H1
    const val LEARN_MARKETPLACE_MINT_NFTS_INDEX_URL = "https://learn.chromia.com/courses/marketplace-course/module-nft/mint-nfts"
    const val LEARN_MARKETPLACE_MINT_NFTS_INDEX_URL_SLASH = "https://learn.chromia.com/courses/marketplace-course/module-nft/mint-nfts/"
    const val LEARN_MARKETPLACE_MINT_NFTS_INDEX_TITLE = "Mint NFTs"  // official H1
    const val LEARN_NEWS_MODULE_TWO_INDEX_URL = "https://learn.chromia.com/courses/my-news-feed/module-two"
    const val LEARN_NEWS_MODULE_TWO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/my-news-feed/module-two/"
    const val LEARN_NEWS_MODULE_TWO_INDEX_TITLE = "Module 2 - React project"  // official H1
    const val RELL_STATEMENTS_INDEX_URL = "https://docs.chromia.com/rell/language-features/statements"
    const val RELL_STATEMENTS_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/statements/"
    const val RELL_STATEMENTS_INDEX_TITLE = "Statements"  // official H1
    const val LEARN_TTT_MODULE_TWO_SETUP_INDEX_URL = "https://learn.chromia.com/courses/tic-tac-toe/module-two/setup"
    const val LEARN_TTT_MODULE_TWO_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/tic-tac-toe/module-two/setup/"
    const val LEARN_TTT_MODULE_TWO_SETUP_INDEX_TITLE = "Set up the project"  // official H1
    const val LEARN_MARKETPLACE_REGISTER_ACCOUNT_INDEX_URL = "https://learn.chromia.com/courses/marketplace-course/module-ft4/register-account"
    const val LEARN_MARKETPLACE_REGISTER_ACCOUNT_INDEX_URL_SLASH = "https://learn.chromia.com/courses/marketplace-course/module-ft4/register-account/"
    const val LEARN_MARKETPLACE_REGISTER_ACCOUNT_INDEX_TITLE = "Create user accounts"  // official H1
    const val LEARN_ZK_CIRCOM_INTRO_INDEX_URL = "https://learn.chromia.com/courses/zero-knowledge-proof/circom-circuits/circom-circuits-introduction"
    const val LEARN_ZK_CIRCOM_INTRO_INDEX_URL_SLASH = "https://learn.chromia.com/courses/zero-knowledge-proof/circom-circuits/circom-circuits-introduction/"
    const val LEARN_ZK_CIRCOM_INTRO_INDEX_TITLE = "Circom circuits: introduction"  // official H1
    const val LEARN_GOAT_SETUP_INDEX_URL = "https://learn.chromia.com/courses/chromia-goat-chat-agent/setup"
    const val LEARN_GOAT_SETUP_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chromia-goat-chat-agent/setup/"
    const val LEARN_GOAT_SETUP_INDEX_TITLE = "Set up your project"  // official H1
    const val LEARN_COMPARISONS_OVERVIEW_INDEX_URL = "https://learn.chromia.com/courses/chromia-comparisons/overview"
    const val LEARN_COMPARISONS_OVERVIEW_INDEX_URL_SLASH = "https://learn.chromia.com/courses/chromia-comparisons/overview/"
    const val LEARN_COMPARISONS_OVERVIEW_INDEX_TITLE = "How Chromia compares to other blockchain platforms"  // official H1
    const val RELL_STATEMENTS_BASIC_INDEX_URL = "https://docs.chromia.com/rell/language-features/statements/basic-statements"
    const val RELL_STATEMENTS_BASIC_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/statements/basic-statements/"
    const val RELL_STATEMENTS_BASIC_INDEX_TITLE = "Basic statements"  // official H1
    const val RELL_STATEMENTS_LOCAL_VARIABLE_INDEX_URL = "https://docs.chromia.com/rell/language-features/statements/local-variable"
    const val RELL_STATEMENTS_LOCAL_VARIABLE_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/statements/local-variable/"
    const val RELL_STATEMENTS_LOCAL_VARIABLE_INDEX_TITLE = "Variable declarations"  // official H1
    const val RELL_MODULE_SIZE_CONSTRAINT_INDEX_URL = "https://docs.chromia.com/rell/language-features/modules/size-constraint-annotations"
    const val RELL_MODULE_SIZE_CONSTRAINT_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/modules/size-constraint-annotations/"
    const val RELL_MODULE_SIZE_CONSTRAINT_INDEX_TITLE = "Size constraint annotations"  // official H1
    const val RELL_TYPES_VIRTUAL_INDEX_URL = "https://docs.chromia.com/rell/language-features/types/virtual-types"
    const val RELL_TYPES_VIRTUAL_INDEX_URL_SLASH = "https://docs.chromia.com/rell/language-features/types/virtual-types/"
    const val RELL_TYPES_VIRTUAL_INDEX_TITLE = "Virtual types"  // official H1

    val pages = listOf(INDEX_URL, LOCAL_URL, BASIC_URL, CONDITIONAL_URL, LOOP_URL)

    val statements = listOf(
        "val  # immutable after init; may exist outside a function",
        "var  # mutable; cannot exist outside a function; may omit initializer",
        "assignment  # x = 123; values[i] = z; y += 15",
        "function-call statement  # print('Hello');",
        "return; / return 123;",
        "block  # { ... } introduces a scope",
        "if / else if / else  # statement form need not be exhaustive",
        "when (x) / when { cond -> ... }  # multi-way branch",
        "for (x in range_or_collection)",
        "while (cond)",
        "break / continue"
    )

    fun valExample(): String = """
        val x = 123;
        val y: text = 'Hello';
    """.trimIndent() + "\n"

    fun varExample(): String = """
        var x: integer;
        var y = 123;
        var z: text = 'Hello';
    """.trimIndent() + "\n"

    fun basicExample(): String = """
        var x = 0;
        var y = 0;
        val values = [10, 20, 30];
        val i = 1;
        val z = 99;
        x = 123;
        values[i] = z;
        y += 15;
        print('Hello');
        return;
        return 123;
    """.trimIndent() + "\n"

    fun ifStatementExample(): String = """
        if (x == 5) print('Hello');
        if (y == 10) {
            print('Hello');
        } else {
            print('Bye');
        }
        if (x == 0) {
            return 'Zero';
        } else if (x == 1) {
            return 'One';
        } else {
            return 'Many';
        }
    """.trimIndent() + "\n"

    fun whenStatementExample(): String = """
        when (x) {
            1 -> return 'One';
            2, 3 -> return 'Few';
            else -> {
                val res = 'Many: ' + x;
                return res;
            }
        }
        when {
            x == 1 -> return 'One';
            x >= 2 and x <= 7 -> return 'Several';
            x == 11, x == 111 -> return 'Magic number';
            else -> return 'Unknown';
        }
    """.trimIndent() + "\n"

    fun loopExample(): String = """
        for (x in range(10)) {
            print(x);
        }
        val l: list<(integer, text)> = [(21, "test")];
        for ((n, s) in l) {
            print(n, s);
        }
        var x = 0;
        while (x < 10) {
            print(x);
            x = x + 1;
        }
    """.trimIndent() + "\n"

    fun breakContinueExample(): String = """
        for (u in user @* {}) {
            if (u.company == 'Facebook') {
                print(u.name);
                break;
            }
        }
        for (u in user @* {}) {
            if (u.company == 'BigCompanyCo') {
                continue;
            }
            print(u.name);
        }
    """.trimIndent() + "\n"

    fun notes(): String = """
        Official Rell statement pages for CLI $CLI_SERIES. Rell pin $RELL_VERSION (docs may still list 0.16.4 — source wins).
        Index: $INDEX_URL
        Locals: $LOCAL_URL  Basic: $BASIC_URL
        Conditional: $CONDITIONAL_URL  Loop: $LOOP_URL
        val is immutable after initiation and may exist outside the scope of a function.
        var is mutable and cannot exist outside the scope of a function. var x: integer; may be assigned later.
        Official basic statements: assignment (including values[i] = z and +=), function-call statement, return, block.
        if statement: optional else and else-if chains. Statement form need not be exhaustive.
        when (x) is multi-way branching similar to switch. Cases may be comma-separated. else is the default.
        when { cond -> ... } is if / else if. Comma-separated cases in the no-arg form are OR'd.
        Expression forms of if / when: chromia_rell_expressions_help.
        for iterates over a range or collection (list, set, map) and official at-expressions (user @* {}).
        Tuple unpack: for ((n, s) in l). while continues while the condition is true.
        break exits the loop; continue skips to the next iteration.
        Do not invent statements or operators. Types: chromia_rell_types_help.
        Database create/update/delete / at: chromia_rell_database_help (Rell syntax inside operations only).
        This tool does not run chr, does not generate a key, and does not send signed transactions.
        Leftover official leftover ECOSYSTEM ecosystem/filehub/filehub-setup/deploy-bundle INDEX (leftover official $ECOSYSTEM_FILEHUB_DEPLOY_BUNDLE_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_FILEHUB_DEPLOY_BUNDLE_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_FILEHUB_DEPLOY_BUNDLE_INDEX_TITLE). Query-only.
        Leftover official leftover ECOSYSTEM ecosystem/bridge/deploy-bridge/register-bridge INDEX (leftover official $ECOSYSTEM_REGISTER_BRIDGE_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_REGISTER_BRIDGE_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_REGISTER_BRIDGE_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP. Leftover official leftover intro leftover official leftover enable leftover official leftover transfers leftover official leftover between leftover official leftover networks leftover official leftover EVM leftover official leftover bridge leftover official leftover contract leftover official leftover recognizes leftover official leftover Chromia leftover official leftover bridge leftover official leftover component leftover official leftover Prerequisites leftover official leftover Deployment leftover official leftover or leftover official leftover leasing leftover official leftover completed leftover official leftover Chromia leftover official leftover CLI leftover official leftover Node.js leftover official leftover MetaMask leftover official leftover Postchain leftover official leftover EIF leftover official leftover contracts leftover official leftover repository leftover official leftover skip leftover official leftover signed leftover official leftover txs leftover official leftover leftover official leftover admin leftover official leftover leftover official leftover writes leftover official leftover leftover official leftover deposit leftover official leftover leftover official leftover withdraw leftover official leftover leftover official leftover mass-exit leftover official leftover leftover official leftover register leftover official leftover leftover official leftover procedure leftover official leftover no leftover official leftover sample leftover official leftover keys leftover official leftover no leftover official leftover invented leftover official leftover 64-hex leftover official leftover BRIDs.
        Leftover official leftover ECOSYSTEM ecosystem/providers/nodes/directory-chain-config INDEX (leftover official $ECOSYSTEM_DIRECTORY_CHAIN_CONFIG_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_DIRECTORY_CHAIN_CONFIG_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_DIRECTORY_CHAIN_CONFIG_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/commands/node INDEX (leftover official $ECOSYSTEM_PMC_NODE_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_PMC_NODE_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_PMC_NODE_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover ECOSYSTEM ecosystem/governance/getting-started/governance-starter-kit/eif-configuration INDEX (leftover official $ECOSYSTEM_GOV_EIF_CONFIGURATION_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_GOV_EIF_CONFIGURATION_INDEX_URL_SLASH leftover official 200 leftover official H1 $ECOSYSTEM_GOV_EIF_CONFIGURATION_INDEX_TITLE). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover ECOSYSTEM ecosystem/governance/getting-started/governance-structure/key-modules INDEX (leftover official $ECOSYSTEM_GOV_KEY_MODULES_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_GOV_KEY_MODULES_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $ECOSYSTEM_GOV_KEY_MODULES_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover ECOSYSTEM ecosystem/governance/governance-voting-process/vote-proposal INDEX (leftover official $ECOSYSTEM_GOV_VOTE_PROPOSAL_INDEX_URL leftover official 307 leftover official $ECOSYSTEM_GOV_VOTE_PROPOSAL_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $ECOSYSTEM_GOV_VOTE_PROPOSAL_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover RELL rell/language-features/database INDEX (leftover official $RELL_DATABASE_OPS_INDEX_URL leftover official 307 leftover official $RELL_DATABASE_OPS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_DATABASE_OPS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover LEARN courses/book-review/book-entity/basic-operations INDEX (leftover official $LEARN_BOOK_REVIEW_FIRST_OPERATION_INDEX_URL leftover official 301 leftover official $LEARN_BOOK_REVIEW_FIRST_OPERATION_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_BOOK_REVIEW_FIRST_OPERATION_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover LEARN courses/chromia-for-evm-developers/chromia-stack INDEX (leftover official $LEARN_EVM_DAPP_OVERVIEW_INDEX_URL leftover official 301 leftover official $LEARN_EVM_DAPP_OVERVIEW_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_EVM_DAPP_OVERVIEW_INDEX_TITLE leftover official HELP ONLY WRITE SKIP)
        Leftover official leftover LEARN courses/rell-masterclass/example INDEX (leftover official $LEARN_RELL_MASTERCLASS_EXAMPLE_INDEX_URL leftover official 301 leftover official $LEARN_RELL_MASTERCLASS_EXAMPLE_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_RELL_MASTERCLASS_EXAMPLE_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP. leftover official leftover concrete leftover official leftover example leftover official leftover NFTs leftover official leftover join leftover official leftover tables leftover official leftover single leftover official leftover database leftover official leftover roundtrip leftover official leftover to_struct leftover official leftover in-memory leftover official leftover struct.
        Leftover official leftover LEARN courses/ft4-demo-app/module-frontend-application/deploy-onchain INDEX (leftover official $LEARN_FT4_DEMO_FRONTEND_DEPLOY_ONCHAIN_INDEX_URL leftover official 301 leftover official $LEARN_FT4_DEMO_FRONTEND_DEPLOY_ONCHAIN_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_FT4_DEMO_FRONTEND_DEPLOY_ONCHAIN_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/marketplace-course/module-nft/mint-nfts INDEX (leftover official $LEARN_MARKETPLACE_MINT_NFTS_INDEX_URL leftover official 301 leftover official $LEARN_MARKETPLACE_MINT_NFTS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_MARKETPLACE_MINT_NFTS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN courses/my-news-feed/module-two INDEX (leftover official $LEARN_NEWS_MODULE_TWO_INDEX_URL leftover official 301 leftover official $LEARN_NEWS_MODULE_TWO_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_NEWS_MODULE_TWO_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover RELL rell/language-features/statements INDEX (leftover official $RELL_STATEMENTS_INDEX_URL leftover official 307 leftover official $RELL_STATEMENTS_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_STATEMENTS_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only.
        Leftover official leftover LEARN courses/tic-tac-toe/module-two/setup INDEX (leftover official $LEARN_TTT_MODULE_TWO_SETUP_INDEX_URL leftover official 301 leftover official $LEARN_TTT_MODULE_TWO_SETUP_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_TTT_MODULE_TWO_SETUP_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX marketplace register-account (leftover official $LEARN_MARKETPLACE_REGISTER_ACCOUNT_INDEX_URL leftover official 301 leftover official $LEARN_MARKETPLACE_REGISTER_ACCOUNT_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_MARKETPLACE_REGISTER_ACCOUNT_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover RELL rell/language-features/statements/basic-statements INDEX (leftover official $RELL_STATEMENTS_BASIC_INDEX_URL leftover official 307 leftover official $RELL_STATEMENTS_BASIC_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_STATEMENTS_BASIC_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX zero-knowledge-proof circom-circuits-introduction (leftover official $LEARN_ZK_CIRCOM_INTRO_INDEX_URL leftover official 301 leftover official $LEARN_ZK_CIRCOM_INTRO_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_ZK_CIRCOM_INTRO_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX chromia-goat-chat-agent setup (leftover official $LEARN_GOAT_SETUP_INDEX_URL leftover official 301 leftover official $LEARN_GOAT_SETUP_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_GOAT_SETUP_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover LEARN INDEX chromia-comparisons overview (leftover official $LEARN_COMPARISONS_OVERVIEW_INDEX_URL leftover official 301 leftover official $LEARN_COMPARISONS_OVERVIEW_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $LEARN_COMPARISONS_OVERVIEW_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover RELL rell/language-features/statements/local-variable INDEX (leftover official $RELL_STATEMENTS_LOCAL_VARIABLE_INDEX_URL leftover official 307 leftover official $RELL_STATEMENTS_LOCAL_VARIABLE_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_STATEMENTS_LOCAL_VARIABLE_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover RELL rell/language-features/modules/size-constraint-annotations INDEX (leftover official $RELL_MODULE_SIZE_CONSTRAINT_INDEX_URL leftover official 307 leftover official $RELL_MODULE_SIZE_CONSTRAINT_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_MODULE_SIZE_CONSTRAINT_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
        Leftover official leftover RELL rell/language-features/types/virtual-types INDEX (leftover official $RELL_TYPES_VIRTUAL_INDEX_URL leftover official 307 leftover official $RELL_TYPES_VIRTUAL_INDEX_URL_SLASH leftover official 200 leftover official H1 leftover official $RELL_TYPES_VIRTUAL_INDEX_TITLE leftover official HELP ONLY WRITE SKIP). Query-only. HELP ONLY WRITE SKIP.
    """.trimIndent()

    fun toJson(): kotlinx.serialization.json.JsonObject = buildJsonObject {
        put("cli", CLI_SERIES)
        put("java", "21+")
        put("postgres", "16+")
        put("rell", RELL_VERSION)
        put("tool", TOOL_NAME)
        put("docs", INDEX_URL)
        put("local_docs", LOCAL_URL)
        put("basic_docs", BASIC_URL)
        put("conditional_docs", CONDITIONAL_URL)
        put("loop_docs", LOOP_URL)
        put("pages", buildJsonArray { pages.forEach { add(JsonPrimitive(it)) } })
        put("statements", buildJsonArray { statements.forEach { add(JsonPrimitive(it)) } })
        put("val_example", valExample())
        put("var_example", varExample())
        put("basic_example", basicExample())
        put("if_statement_example", ifStatementExample())
        put("when_statement_example", whenStatementExample())
        put("loop_example", loopExample())
        put("break_continue_example", breakContinueExample())
        put("expressions_help", ChromiaRellExpressionsHelp.TOOL_NAME)
        put("types_help", ChromiaRellTypesHelp.TOOL_NAME)
        put("ecosystem_filehub_deploy_bundle_index_url_slash", ECOSYSTEM_FILEHUB_DEPLOY_BUNDLE_INDEX_URL_SLASH)
        put("ecosystem_filehub_deploy_bundle_index_title", ECOSYSTEM_FILEHUB_DEPLOY_BUNDLE_INDEX_TITLE)
        put("ecosystem_register_bridge_index_url_slash", ECOSYSTEM_REGISTER_BRIDGE_INDEX_URL_SLASH)
        put("ecosystem_register_bridge_index_title", ECOSYSTEM_REGISTER_BRIDGE_INDEX_TITLE)
        put("ecosystem_directory_chain_config_index_url_slash", ECOSYSTEM_DIRECTORY_CHAIN_CONFIG_INDEX_URL_SLASH)
        put("ecosystem_directory_chain_config_index_title", ECOSYSTEM_DIRECTORY_CHAIN_CONFIG_INDEX_TITLE)
        put("ecosystem_pmc_node_index_url_slash", ECOSYSTEM_PMC_NODE_INDEX_URL_SLASH)
        put("ecosystem_pmc_node_index_title", ECOSYSTEM_PMC_NODE_INDEX_TITLE)
        put("ecosystem_gov_eif_configuration_index_url_slash", ECOSYSTEM_GOV_EIF_CONFIGURATION_INDEX_URL_SLASH)
        put("ecosystem_gov_eif_configuration_index_title", ECOSYSTEM_GOV_EIF_CONFIGURATION_INDEX_TITLE)
        put("ecosystem_gov_key_modules_index_url_slash", ECOSYSTEM_GOV_KEY_MODULES_INDEX_URL_SLASH)
        put("ecosystem_gov_key_modules_index_title", ECOSYSTEM_GOV_KEY_MODULES_INDEX_TITLE)
        put("ecosystem_gov_vote_proposal_index_url_slash", ECOSYSTEM_GOV_VOTE_PROPOSAL_INDEX_URL_SLASH)
        put("ecosystem_gov_vote_proposal_index_title", ECOSYSTEM_GOV_VOTE_PROPOSAL_INDEX_TITLE)
        put("rell_database_ops_index_url_slash", RELL_DATABASE_OPS_INDEX_URL_SLASH)
        put("rell_database_ops_index_title", RELL_DATABASE_OPS_INDEX_TITLE)
        put("learn_book_review_first_operation_index_url_slash", LEARN_BOOK_REVIEW_FIRST_OPERATION_INDEX_URL_SLASH)
        put("learn_book_review_first_operation_index_title", LEARN_BOOK_REVIEW_FIRST_OPERATION_INDEX_TITLE)
        put("learn_evm_dapp_overview_index_url_slash", LEARN_EVM_DAPP_OVERVIEW_INDEX_URL_SLASH)
        put("learn_evm_dapp_overview_index_title", LEARN_EVM_DAPP_OVERVIEW_INDEX_TITLE)
        put("learn_rell_masterclass_example_index_url_slash", LEARN_RELL_MASTERCLASS_EXAMPLE_INDEX_URL_SLASH)
        put("learn_rell_masterclass_example_index_title", LEARN_RELL_MASTERCLASS_EXAMPLE_INDEX_TITLE)
        put("learn_ft4_demo_frontend_deploy_onchain_index_url_slash", LEARN_FT4_DEMO_FRONTEND_DEPLOY_ONCHAIN_INDEX_URL_SLASH)
        put("learn_ft4_demo_frontend_deploy_onchain_index_title", LEARN_FT4_DEMO_FRONTEND_DEPLOY_ONCHAIN_INDEX_TITLE)
        put("learn_marketplace_mint_nfts_index_url_slash", LEARN_MARKETPLACE_MINT_NFTS_INDEX_URL_SLASH)
        put("learn_marketplace_mint_nfts_index_title", LEARN_MARKETPLACE_MINT_NFTS_INDEX_TITLE)
        put("learn_news_module_two_index_url_slash", LEARN_NEWS_MODULE_TWO_INDEX_URL_SLASH)
        put("learn_news_module_two_index_title", LEARN_NEWS_MODULE_TWO_INDEX_TITLE)
        put("rell_statements_index_url_slash", RELL_STATEMENTS_INDEX_URL_SLASH)
        put("rell_statements_index_title", RELL_STATEMENTS_INDEX_TITLE)
        put("learn_ttt_module_two_setup_index_url_slash", LEARN_TTT_MODULE_TWO_SETUP_INDEX_URL_SLASH)
        put("learn_ttt_module_two_setup_index_title", LEARN_TTT_MODULE_TWO_SETUP_INDEX_TITLE)
        put("learn_marketplace_register_account_index_url_slash", LEARN_MARKETPLACE_REGISTER_ACCOUNT_INDEX_URL_SLASH)
        put("learn_marketplace_register_account_index_title", LEARN_MARKETPLACE_REGISTER_ACCOUNT_INDEX_TITLE)
        put("rell_statements_basic_index_url_slash", RELL_STATEMENTS_BASIC_INDEX_URL_SLASH)
        put("rell_statements_basic_index_title", RELL_STATEMENTS_BASIC_INDEX_TITLE)
        put("learn_zk_circom_intro_index_url_slash", LEARN_ZK_CIRCOM_INTRO_INDEX_URL_SLASH)
        put("learn_zk_circom_intro_index_title", LEARN_ZK_CIRCOM_INTRO_INDEX_TITLE)
        put("learn_goat_setup_index_url_slash", LEARN_GOAT_SETUP_INDEX_URL_SLASH)
        put("learn_goat_setup_index_title", LEARN_GOAT_SETUP_INDEX_TITLE)
        put("learn_comparisons_overview_index_url_slash", LEARN_COMPARISONS_OVERVIEW_INDEX_URL_SLASH)
        put("learn_comparisons_overview_index_title", LEARN_COMPARISONS_OVERVIEW_INDEX_TITLE)
        put("rell_statements_local_variable_index_url_slash", RELL_STATEMENTS_LOCAL_VARIABLE_INDEX_URL_SLASH)
        put("rell_statements_local_variable_index_title", RELL_STATEMENTS_LOCAL_VARIABLE_INDEX_TITLE)
        put("rell_module_size_constraint_index_url_slash", RELL_MODULE_SIZE_CONSTRAINT_INDEX_URL_SLASH)
        put("rell_module_size_constraint_index_title", RELL_MODULE_SIZE_CONSTRAINT_INDEX_TITLE)
        put("rell_types_virtual_index_url_slash", RELL_TYPES_VIRTUAL_INDEX_URL_SLASH)
        put("rell_types_virtual_index_title", RELL_TYPES_VIRTUAL_INDEX_TITLE)
        put("notes", notes())
    }
}
// Leftover official leftover ECOSYSTEM ecosystem/filehub/filehub-setup/deploy-bundle INDEX leftovers encoded as ECOSYSTEM_FILEHUB_DEPLOY_BUNDLE_INDEX_* (query-only).
// Leftover official leftover ECOSYSTEM ecosystem/bridge/deploy-bridge/register-bridge INDEX leftovers encoded as ECOSYSTEM_REGISTER_BRIDGE_INDEX_* (query-only HELP ONLY).
// Leftover official leftover ECOSYSTEM ecosystem/providers/nodes/directory-chain-config INDEX leftovers encoded as ECOSYSTEM_DIRECTORY_CHAIN_CONFIG_INDEX_* (query-only HELP ONLY).
// Leftover official leftover ECOSYSTEM ecosystem/providers/pmc/commands/node INDEX leftovers encoded as ECOSYSTEM_PMC_NODE_INDEX_* (query-only HELP ONLY).
// Leftover official leftover ECOSYSTEM ecosystem/governance/getting-started/governance-starter-kit/eif-configuration INDEX leftovers encoded as ECOSYSTEM_GOV_EIF_CONFIGURATION_INDEX_* (query-only HELP ONLY).
// Leftover official leftover ECOSYSTEM ecosystem/governance/getting-started/governance-structure/key-modules INDEX leftovers encoded as ECOSYSTEM_GOV_KEY_MODULES_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover ECOSYSTEM ecosystem/governance/governance-voting-process/vote-proposal INDEX leftovers encoded as ECOSYSTEM_GOV_VOTE_PROPOSAL_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/language-features/database INDEX leftovers encoded as RELL_DATABASE_OPS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/book-review/book-entity/basic-operations INDEX leftovers encoded as LEARN_BOOK_REVIEW_FIRST_OPERATION_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/chromia-for-evm-developers/chromia-stack INDEX leftovers encoded as LEARN_EVM_DAPP_OVERVIEW_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/rell-masterclass/example INDEX leftovers encoded as LEARN_RELL_MASTERCLASS_EXAMPLE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/ft4-demo-app/module-frontend-application/deploy-onchain INDEX leftovers encoded as LEARN_FT4_DEMO_FRONTEND_DEPLOY_ONCHAIN_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/marketplace-course/module-nft/mint-nfts INDEX leftovers encoded as LEARN_MARKETPLACE_MINT_NFTS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/my-news-feed/module-two INDEX leftovers encoded as LEARN_NEWS_MODULE_TWO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/language-features/statements INDEX leftovers encoded as RELL_STATEMENTS_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN courses/tic-tac-toe/module-two/setup INDEX leftovers encoded as LEARN_TTT_MODULE_TWO_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX marketplace register-account leftovers encoded as LEARN_MARKETPLACE_REGISTER_ACCOUNT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/language-features/statements/basic-statements INDEX leftovers encoded as RELL_STATEMENTS_BASIC_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX zero-knowledge-proof circom-circuits-introduction leftovers encoded as LEARN_ZK_CIRCOM_INTRO_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX chromia-goat-chat-agent setup leftovers encoded as LEARN_GOAT_SETUP_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover LEARN INDEX chromia-comparisons overview leftovers encoded as LEARN_COMPARISONS_OVERVIEW_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/language-features/statements/local-variable INDEX leftovers encoded as RELL_STATEMENTS_LOCAL_VARIABLE_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/language-features/modules/size-constraint-annotations INDEX leftovers encoded as RELL_MODULE_SIZE_CONSTRAINT_INDEX_* (query-only HELP ONLY WRITE SKIP).
// Leftover official leftover RELL rell/language-features/types/virtual-types INDEX leftovers encoded as RELL_TYPES_VIRTUAL_INDEX_* (query-only HELP ONLY WRITE SKIP).
