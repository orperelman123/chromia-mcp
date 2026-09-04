package org.chromia.tools

import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.impl.PostchainClientImpl
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.crypto.KeyPair
import net.postchain.crypto.Secp256K1CryptoSystem
import net.postchain.crypto.secp256k1_derivePubKey
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import net.postchain.gtv.merkle.makeMerkleHashCalculator
import net.postchain.gtv.merkleHash
import java.nio.file.Files
import java.nio.file.Path

/**
 * Pure helpers and server-side infrastructure behind the agent-headless testnet
 * provisioning tools (`provision_testnet_container`, `claim_testnet_tchr`,
 * `deploy_testnet_chain`).
 *
 * Ground truth (verified against the LIVE testnet Economy Chain and the
 * directory-chain Rell source, 2026-09-02):
 *  - The Economy Chain BRID is resolved at runtime from the testnet Directory
 *    Chain query `get_economy_chain_rid` (live value
 *    090BCD47149FBB66F02489372E88A454E7A5645ADDE82125D40DF1EF0C76F874).
 *  - `create_container_with_subnode_image(provider_pubkey: byte_array,
 *    container_units: integer, duration_weeks: integer, extra_storage_gib:
 *    integer, cluster_name: text, auto_renew: boolean, subnode_image_name:
 *    text, extra_compute_requests: integer)` leases a container. FT4 auth,
 *    flags ["T"] (live `ft4.get_auth_flags` = ["T"]). Cost is deducted
 *    immediately; the container is created asynchronously via an ICMF ticket -
 *    poll `get_create_container_ticket_by_transaction(tx_rid)` for the
 *    container name (`ticket_state` PENDING=0 / SUCCESS=1 / FAILURE=2).
 *  - `create_container_cost(container_units, duration_weeks, extra_storage_gib,
 *    cluster_name, subnode_image_name, extra_compute_requests)` prices the
 *    lease (live: 1 SCU x 2 weeks on "blue" = 70 tCHR). Lease duration is
 *    bounded by `get_min_lease_duration` (1) / `get_max_lease_duration` (12).
 *  - `faucet()` (module economy_chain_test_claim_tchr, deployed on the live
 *    testnet EC) mints 1000 tCHR to the authenticated FT4 account, once per 7
 *    days per account. FT4 auth with NO flag requirement (live auth flags []).
 *    No captcha exists at the chain level - only on the faucet website.
 *  - Account bootstrap: `create_account` is gated on Chromia's own faucet
 *    backend key, but the live FT4 transfer rules allow the "fee" account
 *    registration strategy from ANY sender: transfer >= 10 tCHR to a
 *    not-yet-existing account id, then within 1 day the new account registers
 *    with `ft4.ras_transfer_fee` + `ft4.register_account` signed by its own
 *    key (fee: 10 tCHR, live module args). A single-sig FT4 account id is the
 *    GTV merkle hash of its pubkey (FT4 `get_account_id_from_signers`).
 *
 * KEY POLICY (owner-approved): all private keys are SERVER-side only - read
 * from env or generated into the server keystore - and must never appear in
 * any tool output, note, or error. [sanitizeText] sweeps every outgoing string.
 */
object TestnetProvisioning {

    /** Env var holding the hex secp256k1 private key of the funding account. */
    const val FUNDING_KEY_ENV = "CHROMIA_TESTNET_FUNDING_PRIVKEY"

    /** Env var naming a key id in the chr keystore (~/.chromia) to fund from. */
    const val FUNDING_KEY_ID_ENV = "CHROMIA_TESTNET_FUNDING_KEY_ID"

    /** Env var overriding the chr keystore directory (default ~/.chromia). */
    const val CHROMIA_DIR_ENV = "CHROMIA_DIR"

    /** Optional env override for the deploy key used by deploy_testnet_chain. */
    const val DEPLOY_KEY_ENV = "CHROMIA_TESTNET_DEPLOY_PRIVKEY"

    /** Env var overriding where server-side ephemeral deploy keys are stored. */
    const val KEYSTORE_DIR_ENV = "CHROMIA_MCP_KEYSTORE_DIR"

    /** Optional env override for the chr binary used by deploy_testnet_chain. */
    const val CHR_BIN_ENV = "CHROMIA_CHR_BIN"

    const val DEFAULT_CLUSTER = "blue"
    const val DEFAULT_SCU = 1
    const val DEFAULT_DURATION_WEEKS = 2
    const val DEFAULT_EXTRA_STORAGE_GIB = 0
    const val TCHR_DECIMALS = 6

    /** Fee strategy registration fee + minimum transfer, from live module args (10 tCHR each). */
    const val REGISTRATION_FEE_RAW = 10_000_000L
    const val MIN_STRATEGY_TRANSFER_RAW = 10_000_000L

    /** Live faucet() parameters (chromia-testnet.yml economy_chain_test_claim_tchr args). */
    const val FAUCET_AMOUNT_RAW = 1_000_000_000L
    const val FAUCET_COOLDOWN_MS = 604_800_000L

    /** ticket_state enum indexes (economy_chain_model.rell). */
    const val TICKET_PENDING = 0
    const val TICKET_SUCCESS = 1
    const val TICKET_FAILURE = 2

    val cryptoSystem = Secp256K1CryptoSystem()

    private val HEX64 = Regex("^[0-9a-fA-F]{64}$")
    private val HEX66 = Regex("^[0-9a-fA-F]{66}$")

    // ---- keys ---------------------------------------------------------------

    /** Parses a 32-byte hex private key; null when absent/malformed (never echoes the value). */
    fun parsePrivKey(hex: String?): ByteArray? {
        val trimmed = hex?.trim()?.removePrefix("0x") ?: return null
        if (!HEX64.matches(trimmed)) return null
        return trimmed.hexToBytes()
    }

    fun derivePubKey(privKey: ByteArray): ByteArray = secp256k1_derivePubKey(privKey)

    /** Validates a 33-byte compressed secp256k1 pubkey in hex form. */
    fun parsePubKey(hex: String?): ByteArray? {
        val trimmed = hex?.trim()?.removePrefix("0x") ?: return null
        if (!HEX66.matches(trimmed)) return null
        return trimmed.hexToBytes()
    }

    /**
     * FT4 single-sig account id: gtv merkle hash (v1) of the signer pubkey -
     * mirrors FT4 `get_account_id_from_signers([signer])` = `signer.hash()`.
     */
    fun accountIdForSingleSigner(pubKey: ByteArray): ByteArray =
        gtv(pubKey).merkleHash(makeMerkleHashCalculator(1))

    // ---- funding key resolution --------------------------------------------

    /** Where a funding key came from - safe to show; never contains key material. */
    data class FundingKey(val privKey: ByteArray, val sourceLabel: String)

    /**
     * Resolves the server-side funding key, in order:
     *  1. env [FUNDING_KEY_ENV] - raw 64-hex private key;
     *  2. env [FUNDING_KEY_ID_ENV] - a key id in the chr keystore;
     *  3. the chr keystore's own default: `key.id` in `<chromiaDir>/config`.
     * The chr keystore directory is env [CHROMIA_DIR_ENV] or `~/.chromia`.
     * A key id resolves to `<id>` (bare 64-hex file, chr's format) or
     * `<id>.secret` (properties file with a `privkey=` line).
     * Returns null when nothing usable is configured. Never logs or returns
     * raw key material beyond the ByteArray needed for signing.
     */
    fun resolveFundingKey(env: Map<String, String>): FundingKey? {
        parsePrivKey(env[FUNDING_KEY_ENV])?.let {
            return FundingKey(it, "env:$FUNDING_KEY_ENV")
        }
        val dir = env[CHROMIA_DIR_ENV]?.takeIf { it.isNotBlank() }?.let { Path.of(it) }
            ?: Path.of(System.getProperty("user.home"), ".chromia")
        val keyId = env[FUNDING_KEY_ID_ENV]?.takeIf { it.isNotBlank() }
            ?: defaultKeyId(dir)
            ?: return null
        // A key id is a file name inside the keystore - refuse separators so a
        // crafted id cannot read arbitrary files.
        if (!Regex("^[A-Za-z0-9_.-]+$").matches(keyId)) return null
        val priv = loadKeystoreKey(dir, keyId) ?: return null
        return FundingKey(priv, "chr-keystore:$keyId")
    }

    private fun defaultKeyId(dir: Path): String? {
        val config = dir.resolve("config")
        if (!Files.exists(config)) return null
        return runCatching {
            Files.readAllLines(config).firstNotNullOfOrNull { line ->
                Regex("""^\s*key\.id\s*=\s*(\S+)\s*$""").find(line)?.groupValues?.get(1)
            }
        }.getOrNull()
    }

    private fun loadKeystoreKey(dir: Path, keyId: String): ByteArray? {
        val bare = dir.resolve(keyId)
        if (Files.isRegularFile(bare)) {
            parsePrivKey(runCatching { Files.readString(bare).trim() }.getOrNull())?.let { return it }
        }
        val secret = dir.resolve("$keyId.secret")
        if (Files.isRegularFile(secret)) {
            val privLine = runCatching {
                Files.readAllLines(secret).firstNotNullOfOrNull { line ->
                    Regex("""^\s*privkey\s*=\s*(\S+)\s*$""").find(line)?.groupValues?.get(1)
                }
            }.getOrNull()
            parsePrivKey(privLine)?.let { return it }
        }
        return null
    }

    // ---- gtv builders -------------------------------------------------------

    /**
     * FT4 `auth_descriptor` struct in compact GTV: [auth_type, args, rules].
     * auth_type S is enum index 0 in compact GTV (Rell enum toGtv non-pretty is
     * the integer index); args for S = [flags, signer_pubkey]; rules = null.
     */
    fun singleSigAuthDescriptorGtv(pubKey: ByteArray, flags: List<String> = listOf("A", "T")): Gtv =
        gtv(
            gtv(0L),
            gtv(gtv(flags.map { gtv(it) }), gtv(pubKey)),
            GtvNull
        )

    fun ftAuthOp(accountId: ByteArray, authDescriptorId: ByteArray): TxOp =
        TxOp("ft4.ft_auth", listOf(gtv(accountId), gtv(authDescriptorId)))

    fun createContainerOp(
        providerPubKey: ByteArray,
        scu: Int,
        durationWeeks: Int,
        extraStorageGib: Int,
        cluster: String,
        autoRenew: Boolean
    ): TxOp = TxOp(
        "create_container_with_subnode_image",
        listOf(
            gtv(providerPubKey),
            gtv(scu.toLong()),
            gtv(durationWeeks.toLong()),
            gtv(extraStorageGib.toLong()),
            gtv(cluster),
            gtv(autoRenew),
            gtv(""),
            gtv(0L)
        )
    )

    fun faucetOp(): TxOp = TxOp("faucet", emptyList())

    fun registerAccountOps(assetId: ByteArray, pubKey: ByteArray): List<TxOp> = listOf(
        TxOp("ft4.ras_transfer_fee", listOf(gtv(assetId), singleSigAuthDescriptorGtv(pubKey), GtvNull)),
        TxOp("ft4.register_account", emptyList())
    )

    // ---- formatting / parsing ----------------------------------------------

    /** 70000000 -> "70" ; 10500000 -> "10.5" (6-decimal tCHR units). */
    fun formatTchr(raw: Long): String {
        val negative = raw < 0
        val abs = if (negative) -raw else raw
        val whole = abs / 1_000_000
        val frac = (abs % 1_000_000).toString().padStart(6, '0').trimEnd('0')
        val s = if (frac.isEmpty()) "$whole" else "$whole.$frac"
        return if (negative) "-$s" else s
    }

    fun ticketStateName(state: Int): String = when (state) {
        TICKET_PENDING -> "PENDING"
        TICKET_SUCCESS -> "SUCCESS"
        TICKET_FAILURE -> "FAILURE"
        else -> "UNKNOWN($state)"
    }

    /**
     * Ticket state from a query result that may carry the enum as compact GTV
     * (integer) or pretty JSON (name string) - both observed forms are handled.
     */
    fun parseTicketState(raw: Any?): Int? = when (raw) {
        is Int -> raw
        is Long -> raw.toInt()
        is String -> when (raw.trim().uppercase()) {
            "PENDING" -> TICKET_PENDING
            "SUCCESS" -> TICKET_SUCCESS
            "FAILURE" -> TICKET_FAILURE
            else -> raw.trim().toIntOrNull()
        }
        else -> null
    }

    // ---- key-material hygiene ----------------------------------------------

    /**
     * Sweeps a string for the given secrets (hex private keys, case-insensitive,
     * with or without 0x). Every string that reaches a tool result MUST pass
     * through this - including error messages from the chain or subprocesses,
     * which may echo inputs.
     */
    fun sanitizeText(text: String, secrets: Collection<String>): String {
        var out = text
        for (secret in secrets) {
            if (secret.isBlank()) continue
            out = out.replace(secret, REDACTED, ignoreCase = true)
        }
        return out
    }

    const val REDACTED = "[REDACTED-KEY]"

    /** The one-time human bootstrap step, stated precisely (no-fake rule). */
    fun bootstrapHumanStep(accountIdHex: String, needRaw: Long): String {
        val total = needRaw + REGISTRATION_FEE_RAW
        return "One-time bootstrap: this server's funding account $accountIdHex does not exist on the " +
            "testnet Economy Chain yet. Send at least ${formatTchr(maxOf(total, MIN_STRATEGY_TRANSFER_RAW + REGISTRATION_FEE_RAW))} tCHR " +
            "to account ID $accountIdHex on the Chromia TESTNET Economy Chain from any funded testnet " +
            "account (e.g. Chromia Vault at https://vault.chromia.com - Send - paste the account ID; " +
            "or ask anyone with a testnet account to transfer). The transfer creates a pending " +
            "account-creation entry valid for 1 DAY; re-run this tool within that window and the server " +
            "completes registration headlessly (10 tCHR registration fee is deducted). After that, " +
            "everything - tCHR top-ups via the on-chain faucet (1000 tCHR/week), container leasing, and " +
            "deployment - is fully automatic with no human involved."
    }

    fun noFundingKeyMessage(): String =
        "No funding key is available, so this tool runs in dryRun only. The server looks for keys in " +
            "this order: env $FUNDING_KEY_ENV (raw private key hex), env $FUNDING_KEY_ID_ENV (a key id " +
            "in the chr keystore), then the chr keystore's own default key.id " +
            "($CHROMIA_DIR_ENV or ~/.chromia/config). Server operator: point one of those at a key whose " +
            "FT4 account exists on the testnet Economy Chain - a zero balance is fine, the tools top up " +
            "from the on-chain faucet automatically. The key itself is never shown to agents."

    // ---- hex ---------------------------------------------------------------

    fun String.hexToBytes(): ByteArray {
        val clean = trim().removePrefix("0x")
        require(clean.length % 2 == 0) { "hex string must have even length" }
        return ByteArray(clean.length / 2) { i ->
            ((Character.digit(clean[2 * i], 16) shl 4) + Character.digit(clean[2 * i + 1], 16)).toByte()
        }
    }

    fun ByteArray.toHex(): String = joinToString("") { "%02X".format(it) }
}

/** One GTX operation: mount name + compact-GTV args. */
data class TxOp(val name: String, val args: List<Gtv>)

/** Outcome of posting a signed transaction. */
data class TxOutcome(val txRidHex: String, val confirmed: Boolean, val rejectReason: String?)

/**
 * Posts a signed GTX transaction to a chain. Test seam - unit tests never
 * construct the production implementation, so no live network or key use.
 */
fun interface TxPoster {
    fun post(urls: List<String>, bridHex: String, ops: List<TxOp>, privKey: ByteArray): TxOutcome
}

/**
 * Production poster: postchain-client GTX build + secp256k1 sign + post,
 * awaiting confirmation. The private key never leaves this process.
 */
object RealTxPoster : TxPoster {
    override fun post(urls: List<String>, bridHex: String, ops: List<TxOp>, privKey: ByteArray): TxOutcome {
        val pubKey = TestnetProvisioning.derivePubKey(privKey)
        val brid = BlockchainRid.buildFromHex(bridHex)
        val config = PostchainClientConfig(
            blockchainRid = brid,
            endpointPool = EndpointPool.default(urls),
            signers = listOf(KeyPair(pubKey, privKey))
        )
        PostchainClientImpl(config).use { client ->
            var builder = client.transactionBuilder()
            for (op in ops) builder = builder.addOperation(op.name, *op.args.toTypedArray())
            val result = builder.sign().postAwaitConfirmation()
            return TxOutcome(
                txRidHex = result.txRid.rid.uppercase(),
                confirmed = result.status == net.postchain.common.tx.TransactionStatus.CONFIRMED,
                rejectReason = result.rejectReason
            )
        }
    }
}

/**
 * Server-side store for ephemeral deploy keys. Files live outside any
 * agent-readable output: `<pubkeyHex>.key` holds the private key hex;
 * `container-<name>.pub` and `tx-<txRid>.pub` map lease artifacts to the
 * pubkey. Never returns private material to callers other than as ByteArray
 * for signing.
 */
class DeployKeyStore(private val dir: Path) {

    private fun ensureDir(): Path {
        Files.createDirectories(dir)
        return dir
    }

    /**
     * Removes an ephemeral key that never got used: the lease tx it was minted
     * for was REJECTED (not included), so no container can ever reference the
     * pubkey. Without this every rejected lease left a private key on disk
     * forever, unreachable by any tx/container lookup (QA concurrency and
     * resource-lifecycle lens 2026-09-02). Only for explicit rejections - a
     * post that THREW may still have reached the chain, and that key must
     * stay so a container created late is not orphaned. Returns whether a
     * file was removed.
     */
    fun discardEphemeral(pubHex: String): Boolean =
        runCatching { Files.deleteIfExists(dir.resolve("$pubHex.key")) }.getOrDefault(false)

    fun storeEphemeral(pubHex: String, privHex: String) {
        val file = ensureDir().resolve("$pubHex.key")
        Files.writeString(file, privHex)
        runCatching {
            // Best-effort POSIX perms; on Windows the keystore dir location is the boundary.
            Files.setPosixFilePermissions(
                file,
                java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")
            )
        }
    }

    fun recordTx(txRidHex: String, pubHex: String) {
        Files.writeString(ensureDir().resolve("tx-${txRidHex.uppercase()}.pub"), pubHex)
    }

    fun recordContainer(containerName: String, pubHex: String) {
        Files.writeString(ensureDir().resolve("container-${sanitizeName(containerName)}.pub"), pubHex)
    }

    fun pubKeyForTx(txRidHex: String): String? =
        readIfExists(dir.resolve("tx-${txRidHex.uppercase()}.pub"))

    fun pubKeyForContainer(containerName: String): String? =
        readIfExists(dir.resolve("container-${sanitizeName(containerName)}.pub"))

    fun privKeyFor(pubHex: String): ByteArray? {
        val hex = readIfExists(dir.resolve("$pubHex.key")) ?: return null
        return TestnetProvisioning.parsePrivKey(hex)
    }

    private fun readIfExists(file: Path): String? =
        if (Files.exists(file)) runCatching { Files.readString(file).trim() }.getOrNull() else null

    private fun sanitizeName(name: String): String = name.replace(Regex("[^A-Za-z0-9_.-]"), "_")

    companion object {
        fun defaultDir(env: Map<String, String> = System.getenv()): Path =
            env[TestnetProvisioning.KEYSTORE_DIR_ENV]?.let { Path.of(it) }
                ?: Path.of(System.getProperty("user.home"), ".chromia-mcp", "keys")
    }
}

/** Subprocess seam for deploy_testnet_chain's `chr` invocation. */
data class ProcOut(val exitCode: Int, val stdout: String, val stderr: String)

fun interface ProcessRunner {
    fun run(command: List<String>, workDir: Path, extraEnv: Map<String, String>, timeoutMs: Long): ProcOut
}

object RealProcessRunner : ProcessRunner {
    override fun run(command: List<String>, workDir: Path, extraEnv: Map<String, String>, timeoutMs: Long): ProcOut {
        val pb = ProcessBuilder(command).directory(workDir.toFile())
        pb.environment().putAll(extraEnv)
        val proc = pb.start()
        // The previous implementation drained stdout to EOF BEFORE waitFor, so
        // timeoutMs was unenforceable dead code: a hung child (chr waiting on
        // input, a wedged subprocess) kept stdout open and run() never
        // returned - deploy_testnet_chain blocked forever and the child was
        // orphaned. It also deadlocked against any child that wrote more
        // stderr than the OS pipe buffer while stdout was being drained.
        // Both streams are drained concurrently and waitFor(timeout) is the
        // only wait; on timeout the child is killed and the partial output
        // returned honestly.
        val stdout = StringBuffer()
        val stderr = StringBuffer()
        val outDrainer = drain(proc.inputStream, stdout)
        val errDrainer = drain(proc.errorStream, stderr)
        val finished = proc.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS)
        if (!finished) proc.destroyForcibly()
        // Killing the child closes its pipe ends, so the drainers see EOF; the
        // bounded join is a guard against a grandchild still holding the pipe.
        outDrainer.join(5_000)
        errDrainer.join(5_000)
        return if (!finished) {
            ProcOut(-1, stdout.toString(), stderr.toString() + "\n[timed out after ${timeoutMs}ms]")
        } else {
            ProcOut(proc.exitValue(), stdout.toString(), stderr.toString())
        }
    }

    private fun drain(stream: java.io.InputStream, sink: StringBuffer): Thread =
        Thread {
            runCatching {
                val buf = CharArray(8192)
                stream.bufferedReader().use { reader ->
                    while (true) {
                        val n = reader.read(buf)
                        if (n < 0) break
                        sink.append(buf, 0, n)
                    }
                }
            }
        }.apply {
            isDaemon = true
            name = "proc-drain"
            start()
        }
}

/**
 * How the `chr` executable was resolved - [command] is the argv prefix to
 * prepend before chr's own arguments, [source] a human-readable account of
 * the resolution path (surfaced in tool output so a failure is diagnosable).
 */
data class ChrCommand(val command: List<String>, val source: String)

/**
 * Resolves the `chr` executable in a way that actually works on Windows.
 *
 * Java's ProcessBuilder only appends `.exe` when resolving a bare command
 * name, so a bare `chr` fails where the CLI is installed as a `chr.cmd` /
 * `chr.bat` shim (scoop does exactly that - the live 2026-09-02 testnet run
 * failed on this with a misleading "chr is not available" message).
 *
 * Order: explicit CHROMIA_CHR_BIN override first, then (Windows only) a PATH
 * search honoring PATHEXT, then a `cmd /c chr` fallback. `.cmd`/`.bat` hits
 * are wrapped in `cmd /c` because ProcessBuilder cannot launch scripts
 * directly. On Linux/macOS a bare `chr` is returned unchanged - the OS
 * resolves it via PATH exactly as before.
 */
object ChrLocator {
    fun isWindows(): Boolean =
        System.getProperty("os.name").orEmpty().lowercase().contains("win")

    fun resolve(
        env: Map<String, String>,
        windows: Boolean = isWindows(),
        fileExists: (Path) -> Boolean = { runCatching { Files.isRegularFile(it) }.getOrDefault(false) }
    ): ChrCommand {
        val override = env[TestnetProvisioning.CHR_BIN_ENV]?.trim().orEmpty()
        if (override.isNotEmpty()) {
            return if (windows && isWindowsScript(override)) {
                ChrCommand(
                    listOf("cmd", "/c", override),
                    "env ${TestnetProvisioning.CHR_BIN_ENV}=$override (.cmd/.bat script, invoked via `cmd /c`)"
                )
            } else {
                ChrCommand(listOf(override), "env ${TestnetProvisioning.CHR_BIN_ENV}=$override")
            }
        }
        if (!windows) return ChrCommand(listOf("chr"), "bare `chr`, resolved from PATH by the OS")

        val pathValue = env.entries.firstOrNull { it.key.equals("PATH", ignoreCase = true) }?.value.orEmpty()
        val pathext = env.entries.firstOrNull { it.key.equals("PATHEXT", ignoreCase = true) }?.value
            ?: DEFAULT_PATHEXT
        val exts = pathext.split(';').map { it.trim() }.filter { it.startsWith(".") }
            .ifEmpty { DEFAULT_PATHEXT.split(';') }
        for (rawDir in pathValue.split(';')) {
            val dir = rawDir.trim().trim('"')
            if (dir.isEmpty()) continue
            for (ext in exts) {
                val candidate = runCatching { Path.of(dir, "chr$ext") }.getOrNull() ?: continue
                if (!fileExists(candidate)) continue
                return if (isWindowsScript(ext)) {
                    ChrCommand(
                        listOf("cmd", "/c", candidate.toString()),
                        "PATH search honoring PATHEXT: $candidate (.cmd/.bat script, invoked via `cmd /c`)"
                    )
                } else {
                    ChrCommand(listOf(candidate.toString()), "PATH search honoring PATHEXT: $candidate")
                }
            }
        }
        return ChrCommand(
            listOf("cmd", "/c", "chr"),
            "chr not found on PATH (searched with PATHEXT); falling back to `cmd /c chr`"
        )
    }

    private fun isWindowsScript(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".cmd") || lower.endsWith(".bat")
    }

    const val DEFAULT_PATHEXT = ".COM;.EXE;.BAT;.CMD"
}

/** Versions parsed from `chr --version` output. */
data class ChrVersions(val cli: String?, val rell: String?) {
    companion object {
        /**
         * `chr --version` prints one component per line, e.g. (chr 0.29.10):
         *   chr version 0.29.10
         *   rell version 0.15.0
         */
        fun parse(output: String): ChrVersions {
            fun grab(label: String): String? =
                Regex("(?im)^\\s*$label\\s+version[:\\s]+v?(\\d+\\.\\d+\\.\\d+)")
                    .find(output)?.groupValues?.get(1)
            return ChrVersions(cli = grab("chr"), rell = grab("rell"))
        }
    }
}

/**
 * The oldest chr whose `chr deployment create` writes the documented
 * `deployments.<net>.chains` layout (CLI 0.30.0 changelog). Older CLIs also
 * bundle a Rell behind the production pin.
 */
internal const val MIN_DOCUMENTED_CHR = "0.30.0"

/** Null when [cli] is unknown or at least [MIN_DOCUMENTED_CHR]; otherwise the one-line warning. */
internal fun outdatedChrNote(cli: String?): String? {
    val parts = cli?.split('.')?.mapNotNull { it.toIntOrNull() } ?: return null
    val min = MIN_DOCUMENTED_CHR.split('.').map { it.toInt() }
    if (parts.size != 3) return null
    val older = parts.zip(min).firstOrNull { (a, b) -> a != b }?.let { (a, b) -> a < b } ?: false
    if (!older) return null
    return "WARNING: the installed chr $cli predates $MIN_DOCUMENTED_CHR - it writes the pre-0.30 deployment layout" +
        " (no deployments.<net>.chains) and bundles an older Rell than the production pin, so templates that use" +
        " newer language features will not build with it. Upgrade chr to a 0.33.x release before a real deploy."
}

/** The chain names under a chromia.yml `blockchains:` block, in file order; empty when the yml has none. */
internal fun declaredChainNames(yml: String): List<String> {
    val root = runCatching { SimpleYaml.parse(yml) }.getOrNull() as? YamlNode.Mapping ?: return emptyList()
    return root.mapping("blockchains")?.entries?.map { it.key }.orEmpty()
}

/** The 64-hex RID under `deployments.<net>.chains.<blockchain>`, uppercased, or null when absent/not a RID. */
internal fun chainsEntryRid(yml: String, blockchain: String, net: String = "testnet"): String? {
    val root = runCatching { SimpleYaml.parse(yml) }.getOrNull() as? YamlNode.Mapping ?: return null
    val raw = root.mapping("deployments")?.mapping(net)?.mapping("chains")?.scalar(blockchain) ?: return null
    return ChromiaYmlValidator.normalizeDirectoryBrid(raw.trim())?.takeIf { it.length == 64 }?.uppercase()
}

/**
 * Return [yml] with `chains: { blockchain: x"rid" }` under `deployments.<net>`
 * (appended to an existing `chains:` block, else added after the last line of
 * the `deployments.<net>` mapping). Text-level on purpose: the yml is the
 * agent's file and every other line must come back byte-identical.
 */
internal fun withChainsEntry(yml: String, blockchain: String, rid: String, net: String = "testnet"): String {
    val lines = yml.lines().toMutableList()
    val deployments = lines.indexOfFirst { Regex("^deployments\\s*:\\s*$").matches(it) }
    if (deployments < 0) return yml
    val netIdx = (deployments + 1 until lines.size).firstOrNull { Regex("^(\\s+)" + Regex.escape(net) + "\\s*:\\s*$").matches(lines[it]) }
        ?: return yml
    val netIndent = lines[netIdx].takeWhile { it == ' ' }.length
    // the deployments.<net> mapping ends at the first non-blank line indented <= netIndent
    var end = netIdx + 1
    while (end < lines.size && (lines[end].isBlank() || lines[end].takeWhile { it == ' ' }.length > netIndent)) end++
    val keyIndent = " ".repeat(netIndent + 2)
    val chainsIdx = (netIdx + 1 until end).firstOrNull { Regex("^" + keyIndent + "chains\\s*:\\s*$").matches(lines[it]) }
    val entry = "$keyIndent  $blockchain: x\"$rid\""
    if (chainsIdx != null) {
        lines.add(chainsIdx + 1, entry)
    } else {
        // insert before trailing blank lines of the block
        var at = end
        while (at > netIdx + 1 && lines[at - 1].isBlank()) at--
        lines.add(at, entry)
        lines.add(at, "${keyIndent}chains:")
    }
    return lines.joinToString("\n")
}

/**
 * The library names under a chromia.yml `libs:` block. Each one must be
 * vendored by `chr install` before `chr build`/`chr deployment` can compile
 * the project ("Library ft4 is not installed, install before building").
 */
internal fun declaredLibNames(yml: String): List<String> {
    val root = runCatching { SimpleYaml.parse(yml) }.getOrNull() as? YamlNode.Mapping ?: return emptyList()
    return root.mapping("libs")?.entries?.map { it.key }.orEmpty()
}
