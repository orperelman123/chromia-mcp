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
