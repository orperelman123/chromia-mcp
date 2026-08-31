package org.chromia.data.config

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class ChromiaConfig(
    val explorerUrl: String = "https://explorer.chromia.com/api/explorer-service",
    val defaultNetwork: String = "mainnet",
    val predefinedNetworks: Map<String, List<String>> = mapOf(
        "mainnet" to listOf(
            "https://system.chromaway.com",
            "https://chromia.validatrium.club",
            "https://chromia-mainnet-systemnode-1.stakin-nodes.com:7740",
            "https://chroma.node.monster:7741",
            "https://chromia.mainnet-system.nodeops.ninja:7740",
            "https://chromia-mainnet-1.dappradar.com:7740",
            "https://sys-main.chromia.coinhall.org:7740",
            "https://chromia-sp.bwarelabs.com:7740",
            "https://chromia-api.hashkey.cloud:7740",
            "https://chromia-mainnet-system-node.asymm.ventures:7740",
            "https://chr.bbbnnnbbb.net:443",
            "https://chromia-system-node.moca-services.xyz:7740",
            "https://chromia-mainnet-system.dwellir.com:443",
            "https://chromia-mainnet.everstake.one:7740"
        ),
        "testnet" to listOf(
            "https://node0.testnet.chromia.com",
            "https://node1.testnet.chromia.com",
            "https://chromia-testnet.everstake.one:7740",
            "https://node2.testnet.chromia.com",
        ),
        "devnet1" to listOf(
            "https://node0.devnet1.chromia.dev",
            "https://node1.devnet1.chromia.dev",
            "https://node2.devnet1.chromia.dev",
            "https://node3.devnet1.chromia.dev",
        ),
        "devnet2" to listOf(
            "https://node0.devnet2.chromia.dev:7740",
            "https://node1.devnet2.chromia.dev:7740",
            "https://node2.devnet2.chromia.dev:7740",
            "https://node3.devnet2.chromia.dev:7740",
        )
    ),
    val httpTimeouts: HttpTimeouts = HttpTimeouts()
)

data class HttpTimeouts(
    // Heavy explorer analytics (blockchainAnalytics, monthlyActiveAccountsPerChain)
    // were observed exceeding 30s under load in the e2e coverage sweep.
    val requestTimeout: Duration = 60.seconds,
    val connectTimeout: Duration = 10.seconds
)
