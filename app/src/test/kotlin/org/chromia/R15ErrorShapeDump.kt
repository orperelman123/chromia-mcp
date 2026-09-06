package org.chromia

import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import org.chromia.tools.RunRellTests
import org.chromia.tools.VerifyGuardsStrategy
import org.chromia.tools.callToolRequest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

/** TEMPORARY (round 15): print the runner's raw error for each r15 frame probe. */
class R15ErrorShapeDump {
    private val repo = RecordingRepository()

    private fun dump(label: String, files: Map<String, String>, guard: String, replacement: String, test: String) {
        val result = runBlocking {
            VerifyGuardsStrategy().execute(
                callToolRequest(
                    name = "verify_guards",
                    arguments = buildJsonObject {
                        put("files", buildJsonObject { files.forEach { (k, v) -> put(k, v) } })
                        put(
                            "guards",
                            buildJsonArray {
                                add(
                                    buildJsonObject {
                                        put("guard", guard)
                                        put("test", test)
                                        put("replacement", replacement)
                                    }
                                )
                            }
                        )
                    }
                ),
                repo
            )
        }
        val text = (result.content.first() as TextContent).text
        val r = result.structuredContent?.get("results")?.jsonArray?.singleOrNull()?.jsonObject
        println("R15DUMP[" + label + "] verdict=" + r?.get("verdict"))
        println("R15DUMP[" + label + "] evidence=" + r?.get("evidence"))
        println("R15DUMP[" + label + "] raw=" + text)
    }

    private val clamp = "val paid = min(amount, p.balance);"
    private val unclamped = "val paid = amount;"

    private val functionMain = """
        module;
        entity pot { key id: integer; mutable balance: integer = 0; }
        function clamp_payment(p: pot, amount: integer): integer {
            val paid = min(amount, p.balance);
            return paid;
        }
        operation seed(amount: integer) {
            create pot(id = 1, balance = amount);
        }
        operation take(amount: integer) {
            val p = pot @ { .id == 1 };
            val paid = clamp_payment(p, amount);
            update p ( .balance -= paid );
        }
        operation audit() {
            val p = pot @ { .id == 1 };
            require(p.balance >= 0, "the pot has gone negative");
        }
        query left(): integer = pot @ { .id == 1 } ( .balance );
    """.trimIndent()

    private val auditTests = """
        @test module;
        import main;
        function test_overdraft_must_fail() {
            rell.test.tx().op(main.seed(10)).run();
            rell.test.tx().op(main.take(11)).run();
            rell.test.tx().op(main.audit()).run();
            assert_equals(main.left(), 0);
        }
    """.trimIndent()

    private val laterTxMain = """
        module;
        entity pot { key id: integer; mutable balance: integer = 0; }
        operation seed(amount: integer) {
            create pot(id = 1, balance = amount);
        }
        operation take(amount: integer) {
            val p = pot @ { .id == 1 };
            require(p.balance >= 0, "the pot has gone negative");
            val paid = min(amount, p.balance);
            update p ( .balance -= paid );
        }
        query left(): integer = pot @ { .id == 1 } ( .balance );
    """.trimIndent()

    private val laterTxTests = """
        @test module;
        import main;
        function test_overdraft_must_fail() {
            rell.test.tx().op(main.seed(10)).run();
            rell.test.tx().op(main.take(11)).run();
            rell.test.tx().op(main.take(1)).run();
            assert_equals(main.left(), 0);
        }
    """.trimIndent()

    private val queryMain = """
        module;
        entity pot { key id: integer; mutable balance: integer = 0; }
        operation seed(amount: integer) {
            create pot(id = 1, balance = amount);
        }
        query payout(amount: integer): integer {
            val p = pot @ { .id == 1 };
            val paid = min(amount, p.balance);
            require(paid <= p.balance, "the pot is short");
            return paid;
        }
    """.trimIndent()

    private val queryTests = """
        @test module;
        import main;
        function test_overdraft_must_fail() {
            rell.test.tx().op(main.seed(10)).run();
            assert_equals(main.payout(11), 10);
        }
    """.trimIndent()

    private val queryNoticesMain = """
        module;
        entity pot { key id: integer; mutable balance: integer = 0; }
        operation seed(amount: integer) {
            create pot(id = 1, balance = amount);
        }
        operation take(amount: integer) {
            val p = pot @ { .id == 1 };
            val paid = min(amount, p.balance);
            update p ( .balance -= paid );
        }
        query audit(): integer {
            val p = pot @ { .id == 1 };
            require(p.balance >= 0, "the pot has gone negative");
            return p.balance;
        }
    """.trimIndent()

    private val queryNoticesTests = """
        @test module;
        import main;
        function test_overdraft_must_fail() {
            rell.test.tx().op(main.seed(10)).run();
            rell.test.tx().op(main.take(11)).run();
            assert_equals(main.audit(), 0);
        }
    """.trimIndent()

    private val store = """
        module;
        entity pot { key id: integer; mutable balance: integer = 0; }
    """.trimIndent()

    private val crossMain = """
        module;
        import store.*;
        import x;
        operation seed(amount: integer) {
            create pot(id = 1, balance = amount);
        }
        operation take(amount: integer) {
            val p = pot @ { .id == 1 };
            val paid = min(amount, p.balance);
            update p ( .balance -= paid );
        }
        query left(): integer = pot @ { .id == 1 } ( .balance );
    """.trimIndent()

    private val crossX = """
        module;
        import store.*;
        @mount("x_take")
        operation take() {
            val p = pot @ { .id == 1 };
            require(p.balance >= 0, "the pot has gone negative");
        }
    """.trimIndent()

    private val crossTests = """
        @test module;
        import main;
        import x;
        function test_overdraft_must_fail() {
            rell.test.tx().op(main.seed(10)).run();
            rell.test.tx().op(main.take(11)).run();
            rell.test.tx().op(x.take()).run();
            assert_equals(main.left(), 0);
        }
    """.trimIndent()

    @Test
    fun dumpShapes() {
        assertNotNull(System.getenv(RunRellTests.DATABASE_URL_ENV), "needs CHROMIA_TEST_DATABASE_URL")
        dump("p15a-function", mapOf("main.rell" to functionMain, "main_test.rell" to auditTests), clamp, unclamped, "test_overdraft_must_fail")
        dump(
            "p15b-crossmodule",
            mapOf("store.rell" to store, "main.rell" to crossMain, "x.rell" to crossX, "main_test.rell" to crossTests),
            clamp, unclamped, "test_overdraft_must_fail"
        )
        dump("p15c-latertx", mapOf("main.rell" to laterTxMain, "main_test.rell" to laterTxTests), clamp, unclamped, "test_overdraft_must_fail")
        dump("p15d-querynotices", mapOf("main.rell" to queryNoticesMain, "main_test.rell" to queryNoticesTests), clamp, unclamped, "test_overdraft_must_fail")
        dump("p15e-querydefence", mapOf("main.rell" to queryMain, "main_test.rell" to queryTests), clamp, unclamped, "test_overdraft_must_fail")
    }
}
