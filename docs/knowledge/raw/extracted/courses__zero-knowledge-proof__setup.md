# Project setup

URL: https://learn.chromia.com

- [Home](/)
- Project setupOn this page
# Project setup

This section will guide you through setting up the project and understanding how it works.
Following these steps will allow you to run the application and interact with it directly.

## Prerequisites​

Before you begin, make sure you have the following installed:

- Node.js: Version 16 or higher.

- MetaMask: The browser extension should be installed and set up in your browser.

- Docker: To run a Chromia Postchain node locally.

- Rell: rell version 0.14.10. [Install or upgrade.](https://docs.chromia.com/intro/getting-started/installation/cli-installation)

- Circom and snarkjs: Circom is used to write and compile zero-knowledge circuits that define the cryptographic constraints for our proofs.
snarkjs handles the generation and verification of zero-knowledge proofs based on these compiled circuits.
[These tools](https://docs.circom.io/getting-started/installation/) need to be installed globally.

## Clone the demo project​

Please clone the source code for the ZKP demo.

```bash
git clone https://bitbucket.org/chromawallet/zkp-demo.git
```

```bash
cd zkp-demo
```
