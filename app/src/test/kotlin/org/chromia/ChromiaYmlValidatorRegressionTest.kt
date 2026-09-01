package org.chromia

import org.chromia.tools.ChromiaYmlValidator
import org.chromia.tools.SimpleYaml
import org.chromia.tools.YamlNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Regression tests for audited SimpleYaml / ChromiaYmlValidator bugs:
 * same-indent sequences, ':' in plain scalars, document markers,
 * comment/apostrophe stripping, forbidden-module matching, webStatic warning.
 */
class ChromiaYmlValidatorRegressionTest {

    private val brid = "6F1B2C3D4E5F60718293A4B5C6D7E8F90A1B2C3D4E5F60718293A4B5C6D7E8F9"

    private fun baseYml(extra: String = ""): String = """
        blockchains:
          hello:
            module: main
            config:
              features:
                merkle_hash_version: 2
        compile:
          rellVersion: 0.16.1
    """.trimIndent() + (if (extra.isEmpty()) "" else "\n$extra")

    // A1: sequence items at the SAME indent as the parent key are legal YAML.
    @Test
    fun sequenceAtSameIndentAsParentKeyParses() {
        val yaml = "url:\n- https://node0.example.com\n- https://node1.example.com\nother: x"
        val root = SimpleYaml.parse(yaml) as YamlNode.Mapping
        val url = root.entries["url"] as YamlNode.Sequence
        assertEquals(2, url.items.size)
        assertEquals(YamlNode.Scalar("https://node0.example.com"), url.items[0])
        assertEquals("x", root.scalar("other"))
    }

    // A1 + A2: custom deployment with same-indent url list and ':' inside the URL validates.
    @Test
    fun customDeploymentWithSameIndentUrlListAndPortIsAccepted() {
        val yaml = baseYml(
            """
            deployments:
              my_net:
                brid: x"$brid"
                url:
                - https://node0.testnet.chromia.com:7740
                container: my_container
            """.trimIndent()
        )
        val result = ChromiaYmlValidator.validate(yaml)
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        assertTrue(result.ok)
    }

    // A2: an unquoted URL with a port must stay a scalar, not become a mapping.
    @Test
    fun urlScalarWithPortIsNotSplitIntoMapping() {
        val yaml = baseYml(
            """
            deployments:
              my_net:
                brid: x"$brid"
                url:
                  - https://node0.testnet.chromia.com:7740
                container: my_container
            """.trimIndent()
        )
        val result = ChromiaYmlValidator.validate(yaml)
        assertFalse(
            result.errors.any { it.contains("custom names require brid and url") },
            result.errors.toString()
        )
        assertTrue(result.ok, result.errors.toString())
        val root = SimpleYaml.parse("v:\n  - https://host:7740") as YamlNode.Mapping
        val seq = root.entries["v"] as YamlNode.Sequence
        assertEquals(YamlNode.Scalar("https://host:7740"), seq.items[0])
    }

    // A3: leading --- and trailing ... document markers are skipped.
    @Test
    fun documentMarkersAreSkipped() {
        val yaml = "---\n" + baseYml() + "\n...\n"
        val result = ChromiaYmlValidator.validate(yaml)
        assertTrue(result.errors.isEmpty(), result.errors.toString())
        assertTrue(result.ok)
    }

    // A4: apostrophes inside plain scalars do not open quotes, so trailing comments are stripped.
    @Test
    fun apostropheInPlainScalarDoesNotKeepComment() {
        val root = SimpleYaml.parse("name: or's-lease # comment") as YamlNode.Mapping
        assertEquals("or's-lease", root.scalar("name"))
    }

    // A4: '#' not preceded by whitespace is part of the scalar (URL fragments survive).
    @Test
    fun hashWithoutLeadingWhitespaceIsNotAComment() {
        val root = SimpleYaml.parse("url: https://h/x#frag") as YamlNode.Mapping
        assertEquals("https://h/x#frag", root.scalar("url"))
    }

    // A5: the allowed lib/ft4/crosschain path must not false-positive on admin.crosschain.
    @Test
    fun allowedCrosschainPathIsNotForbidden() {
        val yaml = baseYml(
            """
            libs:
              ft4:
                registry: https://gitlab.com/chromaway/ft4-lib.git
                path: rell/src/lib/ft4/crosschain
                tagOrBranch: v1.1.0r
            """.trimIndent()
        )
        val result = ChromiaYmlValidator.validate(yaml)
        assertFalse(
            result.errors.any { it.contains("forbidden FT4 production module") },
            result.errors.toString()
        )
        assertTrue(result.ok, result.errors.toString())
    }

    // A5: names merely containing ras_open (extras_open_config) are not forbidden hits.
    @Test
    fun rasOpenRequiresWordBoundaries() {
        val yaml = baseYml().replace(
            "module: main",
            "module: main\n    moduleArgs:\n      extras_open_config:\n        a: 1"
        )
        val result = ChromiaYmlValidator.validate(yaml)
        assertFalse(result.errors.any { it.contains("ras_open") }, result.errors.toString())
        assertTrue(result.ok, result.errors.toString())
    }

    // A5: real forbidden modules are still flagged after tightening.
    @Test
    fun realForbiddenModulesAreStillFlagged() {
        val yaml = baseYml().replace(
            "module: main",
            "module: main\n    moduleArgs:\n      ras_open:\n        a: 1"
        )
        val result = ChromiaYmlValidator.validate(yaml)
        assertTrue(result.errors.any { it.contains("ras_open") }, result.errors.toString())

        val libYaml = baseYml(
            """
            libs:
              bad:
                registry: https://gitlab.com/chromaway/ft4-lib.git
                path: rell/src/lib/ft4/admin
                tagOrBranch: v1.1.0r
            """.trimIndent()
        )
        val libResult = ChromiaYmlValidator.validate(libYaml)
        assertTrue(
            libResult.errors.any { it.contains("forbidden FT4 production module") },
            libResult.errors.toString()
        )
    }

    // A6: the policed pattern is the scalar form webStatic: out, which must warn.
    @Test
    fun scalarWebStaticWarns() {
        val yaml = baseYml().replace("module: main", "module: main\n    webStatic: out")
        val result = ChromiaYmlValidator.validate(yaml)
        assertTrue(result.ok, result.errors.toString())
        assertTrue(result.warnings.any { it.contains("webStatic") }, result.warnings.toString())
    }

    @Test
    fun nonScalarWebStaticDoesNotWarn() {
        val yaml = baseYml().replace("module: main", "module: main\n    webStatic:\n      dir: out")
        val result = ChromiaYmlValidator.validate(yaml)
        assertFalse(result.warnings.any { it.contains("webStatic") }, result.warnings.toString())
    }

    // Audit F3: a {...} flow mapping fell through to an opaque scalar, so a
    // CLI-valid `config: { features: { merkle_hash_version: 2 } }` lost the key
    // and hard-errored with "merkle_hash_version must be 2".
    @Test
    fun flowMappingMerkleConfigValidatesClean() {
        val yaml = """
            blockchains:
              hello:
                module: main
                config: { features: { merkle_hash_version: 2 } }
            compile:
              rellVersion: 0.16.1
        """.trimIndent()
        val result = ChromiaYmlValidator.validate(yaml)
        assertTrue(result.ok, result.errors.toString())
        assertFalse(
            result.errors.any { it.contains("merkle_hash_version") },
            result.errors.toString()
        )
    }

    // Audit F3: a WRONG flow merkle value must still be flagged (the keys inside
    // the flow mapping are actually seen, not just no-longer-erroring).
    @Test
    fun flowMappingWrongMerkleValueIsStillFlagged() {
        val yaml = """
            blockchains:
              hello:
                module: main
                config: { features: { merkle_hash_version: 1 } }
            compile:
              rellVersion: 0.16.1
        """.trimIndent()
        val result = ChromiaYmlValidator.validate(yaml)
        assertTrue(
            result.errors.any { it.contains("merkle_hash_version must be 2") },
            result.errors.toString()
        )
    }

    // Audit F3: flow-style moduleArgs no longer dodge the forbidden-module scan.
    @Test
    fun flowMappingModuleArgsForbiddenModuleIsCaught() {
        val yaml = baseYml().replace(
            "module: main",
            "module: main\n    moduleArgs: { ras_open: { a: 1 } }"
        )
        val result = ChromiaYmlValidator.validate(yaml)
        assertTrue(
            result.errors.any { it.contains("forbidden FT4 production module") },
            result.errors.toString()
        )
    }

    // Audit F3: flow mappings parse into the same node shape as block mappings,
    // nested flow nodes are not split at inner commas, and an unsplittable brace
    // blob degrades to a scalar instead of a wrong hard error.
    @Test
    fun flowMappingParsesLikeBlockMapping() {
        val root = SimpleYaml.parse("a: { x: 1, y: [1, 2], z: { w: v } }\nb: {}") as YamlNode.Mapping
        val a = root.entries["a"] as YamlNode.Mapping
        assertEquals("1", a.scalar("x"))
        val y = a.entries["y"] as YamlNode.Sequence
        assertEquals(listOf(YamlNode.Scalar("1"), YamlNode.Scalar("2")), y.items)
        assertEquals("v", a.mapping("z")?.scalar("w"))
        assertEquals(YamlNode.Mapping(), root.entries["b"])
        val opaque = SimpleYaml.parse("a: {not yaml}") as YamlNode.Mapping
        assertTrue(opaque.entries["a"] is YamlNode.Scalar, opaque.entries["a"].toString())
    }
}
