On this page
# FT4

The FT4 protocol offers a library that assists developers in creating complex blockchain solutions related to fungible tokens and accounts in the Chromia network. It supports features like token registration, token transfers, and account creation.

### Asset registration​

To create an asset on the chain, developers use the FT4 library. They specify the name, ticker, decimals, and values required to register the new asset and mint tokens to an account. Based on business logic, they distribute tokens as specified in the code—this process is known as on-chain asset registration.

Developers can also perform cross-chain asset registration, which involves initially minting an asset on chain A and then registering it with chain B. Once the cross-chain registration is complete, the asset becomes available for transfers.

### Account registration​

Unlike traditional blockchain networks that use an address for receiving and transferring digital assets, Chromia utilizes accounts as the primary unit for managing balances and performing network operations. Accounts on Chromia resemble traditional web2 accounts, but instead of passwords, they use private/public key pairs for management. Developers register accounts on demand, meaning that if an account has not yet been created, it cannot receive transfers. This contrasts with traditional blockchains, where all addresses exist simultaneously and are available for deposits.

To register an account, developers create the necessary functionality using the FT4 library. They can implement any required business logic related to the account registration process. Once an account exists, its owner can send and receive assets both on-chain and across blockchains within the Chromia network.

FT4 user accounts can also use Ethereum and Binance Chain account addresses, allowing dapp developers to enable users to authenticate with an existing wallet, such as MetaMask. Alternatively, Chromia provides a native wallet solution called the Chromia Vault.

Developers can transfer assets between accounts on the same blockchain or across blockchains within the Chromia network. They can also bridge assets to and from Ethereum or Binance Chain, outside the Chromia network, using Chromia's bridging framework.

### Account management​

Developers can manage accounts using either a single private/public key pair or multiple key pairs. In both cases, they must design the corresponding account registration and management processes.

For more detailed information, visit the Chromia FT4 documentation.
- Asset registration
- Account registration
- Account management