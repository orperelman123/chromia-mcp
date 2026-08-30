# Register payment token

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Register accounts and assets](/courses/marketplace-course/module-ft4/)
- Lesson 1 - Register payment token
# Register payment token

For our marketplace, we need to register and mint a payment token, which will be used to buy and sell cards.

We first register an asset we will use as a payment token using the FT4 library, which has built-in operations for registering assets.

Let's create an asset with the following parameters:

- Name: Collector Card

- Symbol: CRD

- Decimals: 6

- URL to icon: [https://url-to-asset-icon](https://url-to-asset-icon)

This registration should occur immediately upon deploying our blockchain. To achieve this, we utilize an object that functions as a global singleton within our dapp. This object is initialized at the launch of the blockchain, ensuring the asset's registration is executed at startup.

src/rell_marketplace/module.rell
```rell
module;import lib.ft4.assets.{ asset, Unsafe };object dapp_meta {    asset = Unsafe.register_asset("Collector Card", "CRD", 6, chain_context.blockchain_rid, "https://url-to-asset-icon");}
```

Now we can use dapp_meta.asset from within the dapp to access the asset when doing transfers.
