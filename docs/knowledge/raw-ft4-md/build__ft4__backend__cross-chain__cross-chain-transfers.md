On this page
# Transfer assets across chains

This topic provides information about the architecture that's designed to facilitate the seamless transfer of FT4 assets across multiple chains within the Chromia blockchain platform. It is the foundation for enabling cross-chain asset movements and handling internally and externally minted assets, orchestrating the entire transfer process from asset registration to finalization.

## Definitions​

To fully understand the architecture, here are some key definitions and concepts:

- Internal assets: Assets that are minted directly on the local chain. 
- External assets: Assets used within the local chain but minted on a different chain. 
- Origin chain: The specific chain from which an asset can be received. This may or may not be the same as the issuing chain. 
- Issuing chain: The chain where an asset is initially minted. 
### Registration of assets​

To start a cross-chain asset transfer, you must first complete asset registration on both the sending and receiving chains. When you register external assets, specifying the origin chain is crucial.

For example, if asset "X" mints on chain A, chain A is the issuing chain for asset X. You can only mint asset X on its issuing chain, A.

If you need to use asset X on chain B, chain B must register asset X and specify that chain A is the origin chain.

Once you complete this registration process, chains A and B can freely exchange asset X.

## Asset tree structure​

When chains register assets they become interconnected, resulting in a tree-like connection structure that outlines the flow of an asset across the network. This structure features a root (the issuing chain) and leaves (receiving chains).

Example 1:

- In this example, both chains B and C have registered an asset, with chain A set as the origin chain. Therefore, if chain B wants to transfer an asset to chain C, it must first send the asset to chain A, which will then route it to chain C. 
The following diagram illustrates two possible transfer scenarios:

Example 2:

- Suppose chain C wants to transfer an asset to chain D. Chain C has registered the asset with chain A, which is the issuing chain. Chain D has registered its asset with chain B, and chain B has also registered with chain A. Therefore, when C initiates the transfer to D, the asset is routed in this sequence: C -> A -> B -> D. 
The diagram provided below highlights two possible transfer scenarios where: B transfers an asset to C, and C transfers an asset to D:

Example 3:

- The following example illustrates the structure where element B serves as the connecting node between C and A. Consequently, when A wants to transfer an asset to C, the transfer must pass through B, which then routes it to C. 
Below you can find the process of the asset transfer from A to C:

## Validating origin chains​

The registration of any asset is a self-contained process, meaning that the origin chain is not notified about the registration on the source chain. Therefore, there is no way to verify whether the origin chain has ever registered the asset with the issuing chain.

For example, if chain A mints asset X and chain B lists A as its origin, and a new chain, C, wants to use asset X by registering it with chain Z as its origin, problems arise if Z has not registered asset X. In this case, when chain B attempts to send asset X to chain C, no connection path between the two chains is found, resulting in the transfer failing.
warning
Selecting trustworthy origin chains is crucial, as malicious or misconfigured chains could result in the user token being lost or stolen. 
## Operations in cross-chain transfers​

Cross-chain transfers involve three fundamental operations:

- init_transfer: Initiated on the source chain. 
- apply_transfer: Executed on all intermediate and target chains. 
- complete_transfer: Finalizes the transfer back on the source chain. 
The direction of asset flow determines whether assets are locked, minted, unlocked, or burned during the process.

## Anchoring and ICCF​

After the initial transfer, the following chain must verify the previous transaction's inclusion in a block. This verification process relies on the ICCF (Interchain Confirmation Facility) mechanism, which uses anchoring chains for proof verification. For a deeper understanding of ICCF, refer to the Interchain Confirmation Facility topic.

# Cross-chain transfer operations

## List of operations​

- init_transfer 
- apply_transfer 
- cancel_transfer 
- unapply_transfer 
### 1. `init_transfer`​

Description: Initializes a cross-chain transfer, creating a record in the system and setting up the necessary data for later steps in the transfer process.

#### Parameters:​

- `asset_id`: ID of the asset to be transferred. 
- `amount`: Amount of the asset to be transferred. 
- `destination`: The destination blockchain and account where the asset will be sent. 
- `hops`: Array of blockchains this transfer must go through. 
- `deadline`: Expiration time of the transfer, after which it can be canceled. 
#### Notes:​

- Throws if the sender does not have enough balance. 
- Throws if the destination blockchain or account is invalid. 
- Throws if the hops configuration is invalid. 
### 2. `apply_transfer`​

Description: Applies a cross-chain transfer on the receiving blockchain. This operation must be called at each step to transfer the asset from one chain to the next until it reaches the final destination.

#### Parameters:​

- `init_transfer_tx`: The transaction containing the `init_transfer` operation. 
- `init_tx_op_index`: Index of the `init_transfer` operation in `init_transfer_tx`. 
- `previous_hop_tx`: Transaction containing the `apply_transfer` operation of the previous hop. 
- `op_index`: Index of the `apply_transfer` operation in `previous_hop_tx`. 
- `hop_index`: The index in the hops array provided in the original transaction that this chain represents. 
#### Notes:​

- Throws if the transfer deadline has passed. 
- Throws if the operation sequence does not match the expected order in the hops array. 
- Throws if the sender's balance is insufficient. 
- Throws if there is an issue with the authorization steps. 
### 3. `cancel_transfer`​

Description: Cancels a cross-chain transfer that has expired. This operation must be called in place of `apply_transfer` to start the transfer canceling process. `unapply_transfer` must then be called on all previous chains to bring the funds back to the sender account.

#### Parameters:​

- `init_transfer_tx`: The transaction containing the `init_transfer` operation corresponding to the transfer to cancel. 
- `init_tx_op_index`: The index of the `init_transfer` operation in `init_transfer_tx`. 
- `previous_hop_tx`: The transaction containing the `apply_transfer` operation of the previous hop. 
- `op_index`: The index of the `apply_transfer` or `init_transfer` operation in `previous_hop_tx`. 
- `hop_index`: Index in the hops array provided in the original transaction that this chain represents. 
#### Notes:​

- Throws if the transfer has not yet expired. 
- Throws if there is a configuration error, such as mismatches in the operation indices or hop index. 
- Requires an `iccf_proof` operation to demonstrate that the `apply_transfer` was performed on the previous chain. 
- Anyone can call this operation with the required proof. 
#### Related operations:​

- See `core.auth.is_auth_op` for information on authorization operations. 
- See `core.crosschain.applied_transfers` and `core.crosschain.canceled_transfers` for details on the cross-chain transfer workflow. 
### 4. `unapply_transfer`​

Description: Unapplies a cross-chain transfer that has expired. This operation must be called if `apply_transfer` was already executed on this chain. It reverses the transfer on this chain and adjusts balances accordingly.

#### Parameters:​

- `init_transfer_tx`: The transaction containing the `init_transfer` operation corresponding to the transfer to unapply. 
- `init_tx_op_index`: The index of the `init_transfer` operation in `init_transfer_tx`. 
- `last_tx`: The last transaction in this transfer flow, as sent to the previous hop. It must contain one of the three supported operations (`cancel_transfer`, `recall_unclaimed_transfer`, or `unapply_transfer`). 
- `last_op_index`: The index of the supported operation in `last_tx`. 
- `hop_index`: Index in the hops array provided in the original transaction that this chain represents. 
#### Notes:​

- Throws if the transfer was never applied, or if it was already canceled or unapplied. 
- Throws for malformed inputs, such as mismatched arguments or invalid hop indices. 
- Throws if the operation in `last_tx` is not one of the supported types. 
- Throws if the transfer has not expired. 
- Requires no authorization flags to unapply a transfer. 
#### Related Operations:​

- See `apply_transfer` for edge cases where this operation might throw. 
- See `core.crosschain.applied_transfers`, `core.crosschain.unapplied_transfers`, `core.crosschain.canceled_transfers`, and `core.crosschain.recalled_transfers` for information on the cross-chain transfer workflow. 
## 5. `revert_transfer`​

Reverts a transfer that was initialized on this chain after it has expired. This operation is used when a cross-chain transfer must be rolled back after expiring and the assets need to be returned to the original sender.

### Throws:​

- if: 
- The transfer was never initialized on this chain. 
- The transfer has already been reverted on this chain. 
- if the input is malformed, such as: 
- Arguments in `init_transfer_tx` and `last_tx` do not match. 
- `last_tx` has not been sent to the chain that comes after this one. 
- if the operation found in `last_tx` is unsupported. 
- if the transfer has not expired yet. 
- Throws if `init_tx_op_index` does not point to `init_transfer` in `init_transfer_tx`. 
- Throws if the required assets cannot be transferred (e.g., sender’s balance is lower than `amount`). 
### Parameters:​

- `init_transfer_tx`: The transaction containing the `init_transfer` operation corresponding to the transfer to revert. 
- `init_tx_op_index`: The index of the `init_transfer` operation in `init_transfer_tx`. 
- `last_tx`: The last transaction in the transfer flow, sent to the previous hop. 
- `last_op_index`: The index of `apply_transfer` operation in `last_tx`. 
## 6. `recall_unclaimed_transfer`​

Recalls an unclaimed transfer. A cross-chain transfer does not need to be claimed unless account registration is configured on the target chain. If the transfer reaches the target chain and the recipient account does not exist, the transfer can be recalled.

### Throws:​

- if account registration on cross-chain transfers is not allowed. 
- if the transfer’s target chain is not the current chain. 
- if the transaction has: 
- Never been applied. 
- Already been recalled. 
- Throws if the conditions for recalling the transfer are not met. 
### Parameters:​

- `init_transfer_tx`: The initial transaction containing `init_transfer`, submitted to the source chain. 
- `init_tx_op_index`: The index of the `init_transfer` operation in `init_transfer_tx`. 
## 7. `validate_apply_transfer`​

Validates the parameters for an `apply_transfer` operation and is used by other operations that require similar validation, such as `cancel_transfer`. It does not check whether the transfer has expired.

### Throws:​

- if the transfer has already been applied, canceled, or unapplied on this chain. 
- if the input is malformed, such as: 
- Arguments in `init_transfer_tx` and `previous_hop_tx` do not match. 
- `hop_index` is out of bounds. 
- `previous_hop_tx` has not been sent to the correct chain. 
- if the chain at `hop_index` on the `init_transfer_tx` does not match this chain. 
- Throws if `op_index` does not point to `apply_transfer`. 
- Throws if `init_tx_op_index` does not point to `init_transfer` on `init_transfer_tx`. 
### Parameters:​

- `init_transfer_tx`: The initial transaction containing `init_transfer` submitted to the starting chain. 
- `init_tx_op_index`: The index of the `init_transfer` operation in `init_transfer_tx`. 
- `previous_hop_tx`: The transaction that applied the transfer on the previous chain. 
- `op_index`: The index of the `apply_transfer` operation in `previous_hop_tx`. 
- `hop_index`: The index of the hop in the hops array of the original transaction. 
The flowchart below is describing the possible scenarios for function calls above on 3 blockchains:

## Cross-chain Transfers Example​

This simple app demonstrates cross-chain transfers between two blockchains. On the first chain, the asset is registered dynamically; on the second chain, it is registered using the `init` operation. The sender account is set up with a custom `custom_register_account` operation, which mints 100 test assets to the user.

The recipient account is registered using the standard `register_account` operation with the `open` strategy, allowing free and unrestricted account creation.

Refer to the Github repository for more details and code examples.
- Definitions
- Registration of assets
- Asset tree structure
- Validating origin chains
- Operations in cross-chain transfers
- Anchoring and ICCF
- List of operations
- 1. `init_transfer`
- 2. `apply_transfer`
- 3. `cancel_transfer`
- 4. `unapply_transfer`
- 5. `revert_transfer`
- Throws:
- Parameters:
- 6. `recall_unclaimed_transfer`
- Throws:
- Parameters:
- 7. `validate_apply_transfer`
- Throws:
- Parameters:
- Cross-chain Transfers Example