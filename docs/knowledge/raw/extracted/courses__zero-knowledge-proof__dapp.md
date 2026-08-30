# Module 2 – Dapp

URL: https://learn.chromia.com

- [Home](/)
- Module 2 – Dapp
# Module 2 – Dapp

The Rell dapp implements a privacy-preserving token system that enables confidential transactions
using zero-knowledge proofs, with three core operations:
shield (converting public tokens to private commitments),
unshield (converting private notes back to public tokens),
and private transfers (transferring tokens between users without revealing amounts or identities).

The dapp manages unspent commitments and spent nullifiers to prevent double-spending
while maintaining complete transaction privacy, storing encrypted notes on-chain for wallet synchronization.

At its core, the dapp leverages Chromia's ZKPGTXModule to verify PLONK proofs for each operation,
ensuring cryptographic integrity while keeping sensitive transaction details hidden from the public ledger.

## Lessons
[Dapp: setup and run](/courses/zero-knowledge-proof/dapp/dapp-setup-run)[Dapp: overview](/courses/zero-knowledge-proof/dapp/dapp-overview)[Dapp: entities](/courses/zero-knowledge-proof/dapp/dapp-entities)[Dapp: PLONK verification](/courses/zero-knowledge-proof/dapp/dapp-verification)[Dapp: operations](/courses/zero-knowledge-proof/dapp/dapp-operations)[Dapp: queries](/courses/zero-knowledge-proof/dapp/dapp-queries)[Start module »](/courses/zero-knowledge-proof/dapp/dapp-setup-run)
