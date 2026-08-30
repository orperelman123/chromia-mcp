# Define the NFT model

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 - Build NFT model in Rell](/courses/marketplace-course/module-nft/)
- Lesson 1 - Define the NFT model
# Define the NFT model

We will fully define our own NFT model on-chain and build it in Rell for this project.

We'll start by defining the base entity nft, whose purpose is to represent an NFT with a unique ID and a mutable attribute for the owner of the NFT. Lets create a new file src/rell_marketplace/nft.rell and add the following:

src/rell_marketplace/nft.rell
```rell
entity nft {    key id: byte_array = op_context.transaction.tx_rid;    mutable owner: account;    index owner;}
```

This definition has an id property set to the Transaction RID of a minted NFT. There is also a mutable property owner typed as an account. This means that we can change the account owner of the NFT and, in practice, transfer the NFT to a new account.

The entity account is included in the FT4 lib, representing an account registered in FT4. You can read more about FT4 accounts [here](https://docs.chromia.com/ft4/intro).

We also have an index on the owner to optimize performance when looking up owners of NFTs. Note that this will make the operation of changing owner heavier. With this base entity in place, we can create our Gaming Trade Card NFT, adding attributes specific to our NFT.

src/rell_marketplace/nft.rell
```rell
entity nft_card {    key nft;    strength: integer;    health: integer;}
```

Let's break it down

- 
The property key nft creates a relation to our NFT entity with a unique constraint.

- 
The strength and health properties give our cards unique attributes.

That's it. We have created a simple NFT model in just a few steps, and this is a diagram showing our entities.
