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
