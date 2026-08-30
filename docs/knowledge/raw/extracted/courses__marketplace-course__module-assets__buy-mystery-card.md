# Add a fee for buying a mystery card

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 - Build a marketplace](/courses/marketplace-course/module-assets/)
- Lesson 1 - Add a fee for buying a mystery card
# Add a fee for buying a mystery card

In the minting lesson, we added the operation buy_mystery_card, which a user calls to mint a new random card. But for it to actually be a purchase operation, we need to set a price and withdraw this amount from the user when the operation is called.

The card cost will be withdrawn from the user and added to a particular dapp account, which holds all revenues from card sales.

This dapp economy account doesn't exist yet, so let's use FT4 again to create this account. We need to add a new entity, dapp_account, to store our economy account, which gives us easy access to this account from our Rell code.

We define a structure in our Rell code to reference the account.

src/rell_marketplace/module.rell
```rell
struct module_args {    dapp_account_signer: pubkey;}
```

Then we create the account in the dapp_meta object we declared earlier.

src/rell_marketplace/module.rell
```rell
object dapp_meta {    asset = Unsafe.register_asset("Collector Card", "CRD", 6, chain_context.blockchain_rid, "https://url-to-asset-icon");    account = create_account_with_auth(single_sig_auth_descriptor(chain_context.args.dapp_account_signer, set(["A", "T"])));}
```

Let's break it down: we use the function create_account_with_auth to create a new account, and we pass a single signature auth descriptor with these values:

- Dapp account pubkey from moduleArgs settings

- Flags are "A" for managing accounts and "T" to allow token transfers using this account.

Then, we create a new keypair which we will use as "account owner" for our dapp:

```shell
chr keygen --file .chromia/dapp-account
```

Then we open chromia.yaml add this as a module argument, and add the pubkey to the economy account as value.

chromia.yml
```yaml
blockchains:  rell_marketplace:    module: main    moduleArgs:      lib.ft4.core.accounts:        rate_limit:          max_points: 10          recovery_time: 5000          points_at_account_creation: 2        lib.ft4.core.admin:          admin_pubkey: x"" # Replace with previously generated public key here.        rell_marketplace:          dapp_account_signer: x"" # Replace with previously generated public key here.compile:  rellVersion: 0.14.9database:  schema: schema_rell_marketplacetest:  modules:    - rell_marketplace_test  moduleArgs:    rell_marketplace:      dapp_account_signer: x"" # Replace with previously generated public key here.    lib.ft4.core.admin:        admin_pubkey: x"" # Replace with previously generated public key here.libs:  ft4:    registry: https://gitlab.com/chromaway/ft4-lib.git    path: rell/src/lib/ft4    tagOrBranch: v1.1.0r    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"    insecure: false  iccf:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/lib/iccf    tagOrBranch: 1.87.0    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"    insecure: false
```

Now, we have an account to which we can transfer charges. For example, when someone buys a card, we transfer the amount from the user to this account.

Let's put this to use by adding a transfer when a user buys a mystery card. We add a transfer with the following parameters:

- From account

- To account

- Asset

- Amount

Our operation buy_mystery_card will now look like this

src/rell_marketplace/marketplace.rell
```rell
operation buy_mystery_card() {    val account = auth.authenticate();    Unsafe.transfer(account, dapp_meta.account, dapp_meta.asset, 100);    mint_card(account);}
```

In this operation we fixed the price of purchasing a card to 100 CRD, which will transfer 100 CRD from the authenticated account to our dapp_account.
