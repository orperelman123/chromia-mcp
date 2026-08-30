# Frontend: setup and run

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 – Frontend](/courses/zero-knowledge-proof/frontend/)
- Frontend: setup and runOn this page
# Frontend: setup and run

## Start the client application​

Finally, let's set up and start the front-end application.

```bash
cd client
```

```bash
npm install
```

Copy .env_example file to .env.

```bash
cp .env.example .env
```

Update the configuration.

.env
```javascript
NEXT_PUBLIC_BLOCKCHAIN_RID= // the brid from the deploy the dapp stepNEXT_PUBLIC_NODE_API_URL=http://localhost:7740
```

DAPP_BRID - the brid from the deploy the dapp step

Start the development server:

```bash
npm run dev
```

## Access the application​

Once the server is running, you can access the application in your browser:

- Open [http://localhost:3000](http://localhost:3000).

- Connect your MetaMask wallet.

- Login or register a new account.

- Create a wallet password for private key encryption.

- You are now ready to use private transfers!

In the next sections, we will explore the different parts of the application in more detail.
