# COURSE chromia-goat-chat-agent — 4 pages


===== FILE: courses__chromia-goat-chat-agent__codebase-overview.md =====

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


===== FILE: courses__chromia-goat-chat-agent__explore-agent.md =====

# Explore the chat agent

URL: https://learn.chromia.com

- [Home](/)
- Lesson 2 - Explore the chat agentOn this page
# Explore the chat agent

This lesson will guide you on how to run and interact with the chat agent, which allows you to manage Chromia accounts and test blockchain operations using natural language through a browser-based interface.

## Steps to run the agent​

- 
Start the development server: In your project directory, run:

```bash
npm run dev
```

Then, open your browser and navigate to the provided URL (e.g., http://localhost:3000).

- 
Connect MetaMask: When prompted, connect an EVM account through MetaMask, ensuring that this account is linked to a Chromia account funded with tCHR.

tipTo retrieve your Chromia account address, connect MetaMask to the Economy Chain in Vault: [Vault - Chromia Economy Chain](https://vault.testnet.chromia.com/en/dapps/dapp/?dapp=1-Chromia+Economy+Chain).

## Use the chat agent​

The agent supports commands for various blockchain operations, so try the following examples:

### Check your balance​

Ask the agent:

```plaintext
What is my balance?
```

The agent will display your tCHR balance for the connected Chromia account.

### Transfer tCHR tokens​

To send tCHR tokens to another Chromia account, use the following command:

```plaintext
Transfer [amount] to 
```

The agent will ask for confirmation of the transfer details, after which you will be prompted to sign the transfer in MetaMask. Once signed, the agent will complete the transaction and provide a link to the Chromia Explorer to verify it.

### Verify updated balances​

To check your updated balance, ask:

```plaintext
What is my balance now?
```

You can then query the balance of the recipient account using:

```plaintext
What is the balance of ?
```

### Get real-time CHR price​

To inquire about the current price of CHR, you can ask:

```plaintext
What is the price of CHR?
```

### Transfer a dollar-equivalent amount​

If you want to send CHR tokens worth a specific amount in USD, you can use:

```plaintext
Transfer 10 dollars' worth of CHR to 
```

The agent will calculate the equivalent CHR amount based on the current price, and you should confirm the transfer before signing the message in MetaMask to complete the transaction.

## Experiment and explore​

Feel free to try additional commands, such as querying balances for other Chromia accounts, checking token price trends, or initiating transfers with different amounts or currencies, as experimentation will help you discover the full capabilities of the agent and explore its potential for customization.


===== FILE: courses__chromia-goat-chat-agent__introduction.md =====

# AI chat agent for Chromia transactions

URL: https://learn.chromia.com

- [Home](/)
- Course overviewOn this page
# AI chat agent for Chromia transactions

In this course, you will build an AI-powered chat agent using the [GOAT SDK](https://github.com/goat-sdk/goat) for Chromia. This project shows you how to leverage AI for seamless blockchain interactions, including CHR token transfers, balance inquiries, and real-time price data—all through a conversational interface.

## Key learning objectives​

- Set up the project environment and prerequisites

- Use the chat agent to transfer testnet tokens (tCHR)

- Query balances and fetch real-time CHR price data

This course provides a practical introduction to the capabilities of the GOAT SDK, highlighting how an AI-powered chat agent simplifies blockchain interactions.

### Repository link​

Access the complete code repository for this course here: [Chromia Goat Demo repository](https://bitbucket.org/chromawallet/chromia-goat-demo).


===== FILE: courses__chromia-goat-chat-agent__setup.md =====

# Set up your project

URL: https://learn.chromia.com

- [Home](/)
- Lesson 1 - Set up your projectOn this page
# Set up your project

Follow these steps to prepare and run the Chromia GOAT demo project.

### Prerequisites​

- 
Node.js: Install version 22 or higher by downloading it from [this link](https://nodejs.org).

- 
MetaMask extension: Install the [MetaMask browser extension](https://metamask.io/) to manage your EVM accounts and connect them to Chromia.

- 
Two Chromia accounts: Create two separate EVM accounts in MetaMask, and for each account, visit the [Chromia testnet faucet](https://faucet.testnet.chromia.com) and follow the steps on the faucet page to fund and create a new Chromia account.

### Setup instructions​

- 
Clone the repository: Open your terminal and run the following:

```bash
git clone https://bitbucket.org/chromawallet/chromia-goat-demo.gitcd chromia-goat-demo
```

- 
Install dependencies: Run npm install to install all the required packages.

- 
Set up your .env file: Copy the .env.template file provided in the repository, rename it to .env, and replace the placeholder values with your actual AI service credentials like this:

```env
VITE_AI_API_KEY=your-api-key-hereVITE_AI_BASE_URL=your-base-url-hereVITE_AI_MODEL_NAME=your-model-name-here
```

- 
Start the development server: Launch the Vite development server by running npm run dev.

- 
Connect MetaMask: Open your browser and navigate to the URL provided by the Vite development server (e.g., http://localhost:3000), and use MetaMask to connect your EVM account.
