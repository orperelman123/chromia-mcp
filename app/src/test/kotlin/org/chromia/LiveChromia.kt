package org.chromia

import net.postchain.common.BlockchainRid
import org.chromia.data.ChromiaRepositoryImpl
import org.chromia.data.client.HttpClientService
import org.chromia.data.client.PostchainClientService
import org.chromia.data.config.ChromiaConfig

/**
 * THE LIVE CHROMIA NETWORK, wired exactly as production wires it.
 *
 * This replaces every double that stood in for a chain node, the explorer's
 * GraphQL API, or the testnet Economy Chain. There is no seam here: the
 * services below are the production ones with the production [ChromiaConfig],
 * talking to the real endpoints over the real network.
 *
 * Three properties make that safe to depend on, and they are the reason the
 * fixtures could go:
 *
 *  - **read-only.** Everything in here is a query. Nothing signs, nothing
 *    spends, and no key material is involved; the account id below is public
 *    chain data, the same kind of thing as a wallet address.
 *  - **enabled everywhere.** [LiveEnv.LIVE_PROVISIONING] is set in CI and
 *    required by `scripts/loop-gate.mjs` before the suite starts, so the normal
 *    state is that these RUN. An outage is a red, and retry is the remedy - a
 *    fixture that keeps passing through an outage is the failure mode this
 *    whole pass exists to remove.
 *  - **the shapes are the chain's.** A recorded response cannot notice that
 *    FT4 renamed a field or that `get_balance` stopped returning a
 *    `big_integer`; these calls do, on the run after it happens.
 *
 * The one gate is [requireLive], which goes through [LiveEnv] like every other
 * abstention in the suite and is pinned by [AssumptionLedgerTest].
 */
object LiveChromia {

    /**
     * The testnet Economy Chain. Public infrastructure - the same BRID
     * `TestnetProvisioningLiveTest` posts its throwaway-key transaction to, and
     * the chain the provisioning tools talk to in production.
     */
    const val ECONOMY_CHAIN_BRID_HEX =
        "090BCD47149FBB66F02489372E88A454E7A5645ADDE82125D40DF1EF0C76F874"

    val economyChainRid: BlockchainRid = BlockchainRid.buildFromHex(ECONOMY_CHAIN_BRID_HEX)

    /**
     * A BRID of the right SHAPE that no chain answers for. Used where a test
     * needs the chain-not-found path: the failure is the network's real answer,
     * not a fixture's idea of one.
     */
    val unknownChainRid: BlockchainRid = BlockchainRid.buildFromHex(
        "0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF0123456789ABCDEF"
    )

    /**
     * A funding account known to be registered on the testnet Economy Chain.
     * Public chain data, used only as the subject of read-only queries; no key
     * material for it exists anywhere in this repository. Shared with
     * [TestnetProvisioningLiveTest], which documents why it may not be deleted.
     */
    const val KNOWN_REGISTERED_ACCOUNT_ID_HEX =
        "7CBC0F013C10CC8BBBA85CD947A8A8E18140FB9D3C2546114C5B8F0EFB5C30A7"

    /** The network name the Economy Chain lives on, as `resolveUrls` understands it. */
    const val NETWORK = "testnet"

    /** The network the public explorer serves; it answers 400 for every other one. */
    const val EXPLORER_NETWORK = "mainnet"

    /**
     * The single gate. Everything live in the suite comes through here, so the
     * assumption ledger carries ONE row for the whole live surface rather than
     * one per test.
     */
    fun requireLive(what: String) = LiveEnv.requireLiveNetwork(what)

    /** The production config; no overrides. */
    fun config(): ChromiaConfig = ChromiaConfig()

    /** The production Postchain client service - no queryClient, no clientFactory. */
    fun postchain(): PostchainClientService = PostchainClientService(config())

    /** The production HTTP client service against the real explorer. */
    fun http(): HttpClientService = HttpClientService(config())

    /** The production repository, wired the way `App` wires it. */
    fun repository(): ChromiaRepositoryImpl {
        val config = config()
        return ChromiaRepositoryImpl(
            config = config,
            httpClientService = HttpClientService(config),
            postchainClientService = PostchainClientService(config)
        )
    }

    /**
     * A config whose explorer and node URLs point at a port nothing listens on.
     *
     * This is NOT a double: it is a real address, reached by the real client
     * over a real socket, and it fails with a real connection error. It exists
     * for the in-process MCP sessions, which must prove they answer WITHOUT
     * reaching the network - a MockEngine that throws used to make that point,
     * and a closed port makes it more honestly, because the failure comes from
     * the operating system rather than from the test.
     */
    fun unreachableConfig(): ChromiaConfig = ChromiaConfig(
        explorerUrl = "http://127.0.0.1:1/explorer-service",
        predefinedNetworks = mapOf(
            "mainnet" to listOf("http://127.0.0.1:1"),
            "testnet" to listOf("http://127.0.0.1:1")
        )
    )
}
