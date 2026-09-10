package org.chromia.tools

import java.net.URLEncoder

/**
 * THE RUNNER'S DATABASE SESSIONS CARRY THEIR OWN EXPIRY.
 *
 * `run_rell_tests` executes user tests in-process against a real PostgreSQL. When the JVM around
 * it dies mid-run - the test task's 90-minute cap, an OOM, a hand kill - the session it opened
 * stays on the server, `idle in transaction`, holding the locks of the test it was running, and
 * every later run's per-test schema wipe queues behind it. On 2026-09-10 one such session was
 * 6 h 10 min old with ten wipes queued, and two gate partitions were red on an idle host for
 * what looked like load (docs/AGENT-LANE-BRIEF.md, "A killed build leaves a lock behind").
 * WSL2's NAT never delivered the dead Windows peer's reset, and PostgreSQL's keepalives are off
 * by default, so the server had no way to notice.
 *
 * Fixing the one cluster's configuration would have fixed the one cluster. These are the same
 * settings applied by the SESSION that can become the zombie, through pgjdbc's `options`
 * parameter, so they hold on every cluster the runner is pointed at - a CI service container,
 * a developer's laptop, the WSL cluster - without anyone configuring the server:
 *
 *  - `--idle_in_transaction_session_timeout=600000`: a session idle inside a transaction for ten
 *    minutes is terminated by the server; a live run never idles that long inside one. The
 *    abandoned session IS idle inside a transaction - that is what holds the locks - so this one
 *    setting is the whole cure, and it is the only one carried here. The Rell runner re-parses
 *    the URL with `java.net.URI` after decoding it, so the value cannot contain a space; the
 *    long `--name=value` form is one token, where `-c name=value` would be two;
 *  - `tcpKeepAlive=true`: the client asks its OS to probe the peer. The SERVER-side keepalive
 *    settings (`tcp_keepalives_idle/interval/count`) are a second defence and stay a cluster
 *    setting (docs/AGENT-LANE-BRIEF.md says which values the laptop cluster runs with).
 *
 * [withSessionGuards] is a pure function over the URL so that `DatabaseSessionGuardsTest` can
 * pin the merge cases and then prove, over a real connection built from the guarded URL, that
 * `SHOW idle_in_transaction_session_timeout` answers what was asked for.
 */
object DatabaseSessionGuards {

    const val IDLE_IN_TRANSACTION_TIMEOUT_MS = 600_000L

    /** The PostgreSQL session settings, as pgjdbc's `options` parameter carries them. */
    val SESSION_OPTIONS: List<String> = listOf(
        "--idle_in_transaction_session_timeout=$IDLE_IN_TRANSACTION_TIMEOUT_MS",
    )

    /**
     * [url] with the session guards appended. An `options` parameter already present is
     * extended, never replaced; every other parameter is kept as written; a URL that is not a
     * PostgreSQL JDBC URL is returned untouched.
     */
    fun withSessionGuards(url: String): String {
        if (!url.startsWith("jdbc:postgresql:")) return url
        val question = url.indexOf('?')
        val base = if (question < 0) url else url.substring(0, question)
        val params = if (question < 0) emptyList() else url.substring(question + 1).split('&').filter { it.isNotEmpty() }

        val existingOptions = params.firstOrNull { it.startsWith("options=") }
            ?.substringAfter("options=")
            ?.let { java.net.URLDecoder.decode(it, Charsets.UTF_8) }
        val mergedOptions = (listOfNotNull(existingOptions?.takeIf { it.isNotBlank() }) + SESSION_OPTIONS.filter { existingOptions?.contains(it) != true })
            .joinToString(" ")
        val kept = params.filterNot { it.startsWith("options=") || it.startsWith("tcpKeepAlive=") }
        val rebuilt = kept + listOf(
            "options=" + URLEncoder.encode(mergedOptions, Charsets.UTF_8).replace("+", "%20"),
            "tcpKeepAlive=true",
        )
        return base + "?" + rebuilt.joinToString("&")
    }
}
