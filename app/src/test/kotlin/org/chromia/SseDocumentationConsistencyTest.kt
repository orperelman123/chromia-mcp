package org.chromia

import org.chromia.tools.ChromiaLanguageClientsHelp
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files

/**
 * ONE STATEMENT ABOUT TRANSPORTS, ENFORCED.
 *
 * The documentation used to say four incompatible things at once, and one of
 * them was wrong in a way a user would hit on their first try:
 *
 *  - README told ChatGPT users to enter `https://<tunnel>/sse` "if the connector
 *    insists", and `serve-public.ps1` PRINTED `$publicUrl/sse` as the connector
 *    URL - a path this server does not serve. A connector configured from that
 *    line 404s.
 *  - `docs/Setup.md` told MCP Inspector users to enter
 *    `http://127.0.0.1:3001/sse`. Same 404.
 *  - README elsewhere said, correctly, that the legacy endpoint is the ROOT.
 *  - `https://mcp.chromia.dev/sse` - ChromaWay's hosted instance, a different
 *    server - appeared as though it documented this one's routing.
 *
 * The statement now in README's "Transports" section is the single truth:
 * stdio is the local product; `--sse` is a FLAG NAME kept for compatibility,
 * not a mode; the server it starts serves Streamable HTTP at `<base>/mcp`
 * (preferred) AND the legacy HTTP+SSE at the ROOT; this server has no `/sse`
 * path; ChromaWay's hosted instance is a different server.
 *
 * This test greps for the two contradictory forms so they cannot come back.
 */
class SseDocumentationConsistencyTest {

    /**
     * The prose and the launchers a user actually reads. Everything under
     * `docs/knowledge/` is vendored third-party documentation and is
     * deliberately not included -
     * we do not get to rewrite Chromia's own docs to agree with us.
     */
    private fun documentationFiles(): List<java.nio.file.Path> {
        val root = RepoFiles.root
        val fixed = listOf(
            "README.md", "serve-local.ps1", "serve-public.ps1", "serve-local.cmd", "serve-public.cmd"
        ).map { root.resolve(it) }.filter { Files.isRegularFile(it) }
        val docs = Files.list(root.resolve("docs")).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".md") }.sorted().toList()
        }
        return (fixed + docs)
    }

    private fun relative(path: java.nio.file.Path) = RepoFiles.root.relativize(path).toString().replace('\\', '/')

    /**
     * A line may mention `/sse` only while making clear whose path it is: either
     * it names ChromaWay's hosted host on the same line, or it says in the same
     * breath that this server does not serve one (a negation, or the 404 it
     * produces). "The same line" is the point - a reader skimming, and a
     * connector being configured from one line of a printed banner, do not get
     * the paragraph.
     */
    private val attributed = Regex("""mcp\.chromia\.dev/sse""")
    private val denied = Regex("""(?i)\bno\s+/sse\b|\bnot\s+/sse\b|\bnever\s+/sse\b|404""")

    @Test
    fun noDocumentOrLauncherAttributesAnSsePathToThisServer() {
        val offenders = mutableListOf<String>()
        documentationFiles().forEach { file ->
            Files.readAllLines(file).forEachIndexed { index, raw ->
                // Backticks are markup, not content: `/sse` and /sse are the
                // same claim to whoever reads the rendered page.
                val line = raw.replace("`", "")
                if (!line.contains("/sse")) return@forEachIndexed
                if (attributed.containsMatchIn(line) || denied.containsMatchIn(line)) return@forEachIndexed
                offenders += "${relative(file)}:${index + 1}: ${raw.trim()}"
            }
        }
        assertTrue(
            offenders.isEmpty(),
            "a `/sse` path is attributed to THIS server. It has none: the legacy HTTP+SSE transport " +
                "is at the ROOT (`GET <base>/`), and `GET /sse` is a 404 - serve-public.ps1 printed " +
                "exactly this URL as the ChatGPT connector address until 2026-09-07. Either name " +
                "ChromaWay's hosted host (mcp.chromia.dev/sse, a different server) on the same line, " +
                "or say on the same line that this server serves no /sse:\n  " +
                offenders.joinToString("\n  ")
        )
    }

    @Test
    fun noDocumentCallsTheUrlServerSseMode() {
        val offenders = mutableListOf<String>()
        documentationFiles().forEach { file ->
            Files.readAllLines(file).forEachIndexed { index, raw ->
                if (Regex("""(?i)\bsse\s+mode\b""").containsMatchIn(raw)) {
                    offenders += "${relative(file)}:${index + 1}: ${raw.trim()}"
                }
            }
        }
        assertTrue(
            offenders.isEmpty(),
            "\"SSE mode\" describes the URL server as though SSE were what it is. It is not: `--sse` " +
                "is a FLAG NAME kept for compatibility, and the server it starts serves Streamable " +
                "HTTP at /mcp (preferred) alongside the legacy HTTP+SSE at the root. Calling it SSE " +
                "mode is how readers end up looking for an /sse path:\n  " +
                offenders.joinToString("\n  ")
        )
    }

    @Test
    fun theReadmeCarriesTheSingleTransportStatement() {
        val readme = RepoFiles.text("README.md")
        listOf(
            "`--sse` is the name of the URL-server flag, not a mode",
            "Streamable HTTP at `<base>/mcp`",
            "legacy HTTP+SSE at the ROOT",
            "This server has no `/sse` path",
            "https://mcp.chromia.dev/sse` is a **different server**"
        ).forEach { fragment ->
            assertTrue(
                readme.contains(fragment),
                "README's Transports section must carry the single transport statement; missing: '$fragment'"
            )
        }
    }

    @Test
    fun deploymentAndArchitectureAgreeTheLegacyEndpointIsTheRoot() {
        val deployment = RepoFiles.text("docs/Deployment.md")
        val architecture = RepoFiles.text("docs/Architecture.md")
        assertTrue(
            deployment.contains("`GET <base>/` + `POST <base>/?sessionId=…` | legacy HTTP+SSE"),
            "docs/Deployment.md must keep the endpoint table saying the legacy transport is the ROOT"
        )
        assertTrue(
            architecture.contains("legacy HTTP+SSE at the root"),
            "docs/Architecture.md must say the legacy transport is at the root path"
        )
        assertTrue(
            architecture.contains("`GET /` opens the stream"),
            "docs/Architecture.md must keep installMcpSse documented as GET / , not GET /sse"
        )
    }

    @Test
    fun theToolHelpTextSaysThisServersSsePathIs404() {
        // The help text an agent reads has to make the same distinction the
        // prose does: the official/upstream README's URL is not this tree's.
        val help = ChromiaLanguageClientsHelp.notes()
        assertTrue(
            help.contains("GET / is the SSE endpoint") && help.contains("GET /sse 404"),
            "chromia_language_clients_help must keep saying that in THIS tree the legacy endpoint is " +
                "GET / and GET /sse is a 404 - it is the only place an agent is told the difference " +
                "between our routing and the upstream README's"
        )
    }
}
