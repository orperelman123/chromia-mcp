# Dapp overview

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 – Dapp](/courses/zero-knowledge-proof/dapp/)
- Dapp: overviewOn this page
# Dapp overview

The zero-knowledge proof (ZKP) dapp implements a privacy-preserving token system on Chromia that enables confidential transactions using the blockchain's built-in ZKP verification capabilities.

## Dapp architecture​

The dapp is built using the Rell programming language and leverages Chromia's native ZKP infrastructure:

- ZKPGTXModule: Built-in Chromia module for PLONK proof verification

- FT4 integration: Seamless integration with Chromia's token standard

- State management: On-chain tracking of commitments and nullifiers

- Event logging: Encrypted note storage for wallet synchronization

## ZKP integration on Chromia​

### Transaction flow​

- Client submits transaction with zkp_plonk_verify operation

- ZKPGTXModule verifies the proof against stored verification keys

- Public signals extracted and passed to subsequent operations

- Dapp operations validate constraints and update state

## Privacy model​

### UTXO-based privacy​

- Commitments: Private notes represented as Pedersen commitments

- Nullifiers: Prevent double-spending without revealing note details

The dapp demonstrates how Chromia's built-in ZKP capabilities enable sophisticated privacy features
directly at the blockchain protocol level,
providing enterprise-grade confidential transactions with minimal complexity.
