# Purchase a card from the marketplace

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 - Build a marketplace](/courses/marketplace-course/module-assets/)
- Lesson 3 - Purchase a card from the marketplace
# Purchase a card from the marketplace

The marketplace is taking shape, and we now have functions to list NFTs that are up for sale. So, the next step is to create an operation to complete a purchase.

src/rell_marketplace/marketplace.rell
```rell
operation buy_nft(id: integer) {    val account = auth.authenticate();    val (nft, owner, price) = listed_nft @ {        .nft.rowid == rowid(id)    } ( .nft, .listed_by, .price );    Unsafe.transfer(account, owner, dapp_meta.asset, price);    delete listed_nft @ { .nft.rowid == rowid(id) };    nft.owner = account;}
```

When a user calls buy_nft, we verify that the user is authenticated. Then, we fetch the NFT and confirm it's listed by querying listed_nft. Next, we transfer the amount specified by the price from the account buying the NFT to the owner who is selling it. Lastly, we delete the NFT from being listed on the marketplace and set the new owner of the NFT.

Our marketplace now supports the following:

- A user can buy an NFT, which is then minted in our dapp.

- NFTs can be listed on the marketplace at a set price.

- Users can buy a listed NFT at the asked price.
