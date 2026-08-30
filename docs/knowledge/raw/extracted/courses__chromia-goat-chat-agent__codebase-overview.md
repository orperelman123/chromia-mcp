# Code walkthrough

URL: https://learn.chromia.com

- [Home](/)
- Lesson 3 - Code walkthroughOn this page
# Code walkthrough

The project is organized into two main files:

- 
index.ts: Main entry point

- Manages environment setup, blockchain connections, tool initialization, and user interactions.

- Demonstrates secure blockchain operations using MetaMask for signing and the GOAT SDK for blockchain management.

- 
tools.ts: Utility functions

- Contains reusable tools, including real-time CHR price fetching and assistant behavior configuration.

## Key components​

### 1. Environment and network setup​

The project is designed to seamlessly target either testnet or mainnet by using the CHROMIA_CONFIG object to load predefined constants for the chosen network:

```typescript
const chromiaNetwork = "testnet"; // Switch to "mainnet" for mainnet operationsconsole.log(chalk.green("✔ Using Chromia network:"), chromiaNetwork);const config = CHROMIA_CONFIG[chromiaNetwork];
```

Key points:

- Predefined configurations: The CHROMIA_CONFIG object simplifies setup by providing constants for both testnet and mainnet, including:

- NODE_URL_POOL: Node URLs for blockchain communication.

- ECONOMY_CHAIN_BRID: The chain identifier for the Economy Chain.

- CHR_ASSET_ID: The token identifier for CHR.

- Dynamic targeting: Specify either "testnet" or "mainnet" to load the corresponding values from CHROMIA_CONFIG.

- Customization: While CHROMIA_CONFIG provides convenience constants, developers can bypass it and supply custom values for advanced use cases.

### 2. Connect to Chromia and enable transactions​

The project connects to the Chromia blockchain and configures transactions, allowing for adaptation to custom chains and tokens.

```typescript
const chromiaClient = await createClient({    nodeUrlPool: config.NODE_URL_POOL,    blockchainRid: config.ECONOMY_CHAIN_BRID, // Specifies the target chain});const connection = createConnection(chromiaClient);const evmKeyStore = await createWeb3ProviderEvmKeyStore(window.ethereum);const keystoreInteractor = createKeyStoreInteractor(chromiaClient, evmKeyStore);const baseTools = await getOnChainTools({    wallet: chromia({        client: chromiaClient,        accountAddress,        keystoreInteractor,        assetId: config.CHR_ASSET_ID, // Specifies the token for transfer        connection,    }),    plugins: [sendCHR()], // Adds token transfer functionality});
```

Key points:

- Network targeting: The NODE_URL_POOL defines the target network. Developers can find an updated list of nodes for testnet in the [Chromia Explorer](https://explorer.chromia.com/testnet/cluster/system).

- Custom configuration:

- Chains: Replace blockchainRid to connect to a specific chain or decentralized application (dapp). Use the [Chromia Explorer](https://explorer.chromia.com/testnet) to find a chain’s brid.

- Tokens: Replace assetId with the unique identifier of the desired token. For example, the assetId for tCHR on the Economy Chain can be found under 'Assets' in the [Economy Chain Explorer](https://explorer.chromia.com/testnet/090BCD47149FBB66F02489372E88A454E7A5645ADDE82125D40DF1EF0C76F874).

- MetaMask integration: MetaMask securely handles signing transactions, while Chromia tools manage blockchain interactions.

### 3. Extend tools for real-time price fetching​

The getLiveTokenPrice utility fetches real-time price data from CoinGecko, and it is extended in index.ts to integrate with the assistant.

- 
Utility function in tools.ts:

```typescript
export const getLiveTokenPrice = async () => {    const response = await fetch("https://api.coingecko.com/api/v3/simple/price?vs_currencies=usd&ids=chromaway&include_market_cap=true&include_24hr_vol=true&include_24hr_change=true");    return await response.json();};
```

- 
Integration in index.ts:

```typescript
const enhancedTools = {    ...baseTools,    getLiveTokenPrice: {        name: "getLiveTokenPrice",        description: "Fetches the current CHR price in USD.",        parameters: z.object({}),        execute: async () => formatPriceData(await getLiveTokenPrice()),    },};
```

Key points:

- The utility function fetches price data and is designed for reuse.

- The extended tool dynamically integrates it into the assistant to facilitate price queries.

### 4. Setting up the AI assistant and configuring its behavior​

The assistant helps users by answering questions and performing actions on the blockchain. You set it up using environment variables and customize it with the MASTER_PROMPT in the tools.ts file.

- 
Setting up the assistant in index.ts:

```typescript
const openai = createOpenAI({    baseURL: import.meta.env.VITE_AI_BASE_URL,    apiKey: import.meta.env.VITE_AI_API_KEY,});
```

- 
Configuring behavior in tools.ts:

```typescript
export const MASTER_PROMPT = (priceData: string) => `You are CHRA, a helpful assistant for CHR tokens...`;
```

Key points:

- Flexibility: The assistant can work with any AI service that follows the OpenAI client standard.

- Customization: You can change the MASTER_PROMPT to match the assistant’s style and tone for different projects.

The Chromia GOAT demo shows how to combine blockchain functions with AI in one application. It has modular parts that include tools for blockchain and customizable AI assistants, making it adaptable and useful. This project is a great resource for developers looking to integrate similar features. It helps with secure transactions, working with blockchain assets, and creating user-friendly AI interfaces. It offers practical tips and reusable components to help you start your development journey.
