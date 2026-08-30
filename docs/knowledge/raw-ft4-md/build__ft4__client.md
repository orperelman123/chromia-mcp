
# Client

The FT4 Client provides the interface through which applications interact with the blockchain. It facilitates operations such as querying assets, signing transactions, transferring assets, and performing cross-chain transfers.
Set up FT4 client
To set up the FT4 client with TypeScript, install the necessary packages using npm install @chromia/ft4. Initialize the client by creating a connection to the Postchain network, and use it to interact with the blockchain (e.g., querying assets).Use auth descriptors

FT4 supports Single-Signature (SingleSig) and Multi-Signature (MultiSig) auth descriptors, allowing customizable permissions and rules for secure access control in decentralized applications.
Register an account

Register accounts using Chromia-native or MetaMask (EVM-compatible) keys with examples for key pair management and the open registration strategy.
Implement user login

FT4 enables non-interactive signing with disposable keys. These keys can be managed via the login function, with care needed when assigning sensitive auth flags. Keys can be stored with different keystore options and cleared on logout for security.
Manage key stores

FT4's KeyStore interface manages cryptographic keys for signing transactions. It includes implementations for Ethereum-compatible keys (EvmKeyStore) and FT4-specific keys (FtKeyStore), supporting in-memory, session, and local storage options.
Transfer assets with FT4 client

To transfer assets with FT4, use the Chromia CLI with the ft4.transfer command or the Postchain client library to initiate a transfer using a session. Ensure proper setup of assets, accounts, and key pairs before transferring.
Perform cross-chain transfers
Use the orchestrator for cross-chain asset transfers, ensuring multichain configuration and ICCF module inclusion. Transfer assets between source and target chains with proper error handling and asset registration.