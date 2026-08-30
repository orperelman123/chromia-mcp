# Dapp queries overview

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 – Dapp](/courses/zero-knowledge-proof/dapp/)
- Dapp: queriesOn this page
# Dapp queries overview

This guide provides a comprehensive breakdown of the queries implemented in the zkp-demo workspace, which demonstrates a private token transfer system on Chromia using zero-knowledge proofs.

## Queries​

The system provides comprehensive queries for:

### Transaction queries​

rell/src/zkp_demo/operations.rell
```rell
// Check nullifier statusquery is_nullifier_spent(nullifier_hash: big_integer) {    return spent_nullifier @? { .nullifier_hash == nullifier_hash } != null;}// Get unspent commitmentsquery get_all_unspent_commitments() {    return unspent_commitment @* {} (.commitment_hash);}// Get transfer eventsquery get_private_transfer_events() {    return private_transfer_event @* {} ($.to_struct());}// Get test assetquery get_test_asset() {    return init.test_asset.to_struct();}
```

### Shield/unshield queries​

rell/src/zkp_demo/operations.rell
```rell
// Get shield logsquery get_shield_logs(): list> {    return shield_log @* {} ($.to_struct());}// Get unspent shield logs by accountquery get_all_unspent_shield_logs_by_account_id(account_id: byte_array): list> {    return (uc: zkp_demo.unspent_commitment, sl: shield_log) @* {         uc.commitment_hash == sl.commitment,         sl.account_id == account_id     } (sl.to_struct());}// Get unshield logsquery get_unshield_logs(): list> {    return unshield_log @* {} ($.to_struct());}
```

### Address registry queries​

rell/src/zkp_demo/operations.rell
```rell
// Get private address by account IDquery get_private_address_by_account_id(account_id: byte_array): big_integer? {    return private_address_registry @? { account_id } ( .private_address );}// Get encryption key by account IDquery get_public_encryption_key_by_account_id(account_id: byte_array): text? {    return private_address_registry @? { account_id } ( .public_encryption_key );}// Get both private address and public encryption key by account IDquery get_user_keys_by_account_id(account_id: byte_array): (private_address: big_integer?, public_encryption_key: text?) {    val user = private_address_registry @? { account_id };    return (        private_address = user?.private_address,        public_encryption_key = user?.public_encryption_key    );}// Get all registered usersquery get_all_registered_users() {    return private_address_registry @* {} ( $.to_struct() );}// Get EVM wallet address from account IDquery get_user_wallet_address(account_id: byte_array): byte_array? {    val account = ft4.accounts.account @? { .id == account_id };    if (account == null) return null;        var evm_wallet_address: byte_array? = null;    val auth_descriptor_signers = (        mad: ft4.accounts.main_auth_descriptor,        ads: ft4.accounts.auth_descriptor_signer    ) @* {        account == mad.account,        ads.account_auth_descriptor == mad.auth_descriptor    } ( ads );    for (auth_descriptor_signer in auth_descriptor_signers) {        if (auth_descriptor_signer.id.size() == 20) {            evm_wallet_address = auth_descriptor_signer.id;            break;        }    }    return evm_wallet_address;}
```

## Integration with FT4​

The ZKP demo seamlessly integrates with Chromia's FT4 framework:

- Account authentication: Uses FT4's authentication system

- Asset management: Leverages FT4's asset handling for public tokens

- Balance operations: Integrates with FT4's balance management
