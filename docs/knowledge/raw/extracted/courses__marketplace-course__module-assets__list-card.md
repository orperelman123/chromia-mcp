# List a card for sale on the marketplace

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 - Build a marketplace](/courses/marketplace-course/module-assets/)
- Lesson 2 - List a card for sale on the marketplace
# List a card for sale on the marketplace

Now, we have the capability to buy mystery cards, in which you will receive a randomly minted collector card.

The next step is to extend the marketplace with functions for listing cards on the market and for users to buy and sell cards.

We start by adding the listed_nft entity.

src/rell_marketplace/nft.rell
```rell
entity listed_nft {    key nft: nft;    mutable price: big_integer;    index listed_by: account;    listed_date: timestamp = op_context.last_block_time;}
```

This entity represents an NFT that's listed on the marketplace. It has a set price for the NFT for sale and a date when it was listed. Next, we will add an operation where users can list an NFT card they own.

src/rell_marketplace/marketplace.rell
```rell
operation list_nft(id: integer, price: big_integer) {    val account = auth.authenticate();    val (nft, owner) = nft @ { .rowid == rowid(id) } ( $, .owner );    require(owner == account, "User must be owner of NFT");    create listed_nft ( nft, price, owner );}
```

What happens here is that the user sends a transaction to list_nft with the card NFT ID and the price.

- First, this operation requires the transaction to be signed by an FT4 account, which we verify using the authenticate method.

- Then, we query for the owner of the NFT, and we fetch the NFT entity.

- A check is done to make sure that the user listing the NFT is the owner of the NFT.

- If everything is okay, a new instance of listed_nft is created with NFT, price, and owner stored.

That's it. This way, users can list their NFTs on the marketplace. We can also add a query to ensure that we can fetch the listed NFTs.

src/rell_marketplace/queries.rell
```rell
enum card_sorting { NONE, PRICE_HIGH, PRICE_LOW }query get_cards(amount: integer, card_sorting)    = (listed_nft, nft_card) @* {    listed_nft.nft == nft_card.nft} (    @omit @sort_desc when (card_sorting) {          PRICE_HIGH -> .price;          PRICE_LOW -> -.price;          else -> nft_card.rowid.to_integer()        },    listed_nft_card_dto (        price = listed_nft.price,        id = nft_card.nft.rowid.to_integer(),        card = nft_card.to_struct()    )) limit amount;struct listed_nft_card_dto {    id: integer;    price: big_integer;    card: struct;}
```

Let's break this down a bit.

This query fetches NFTs listed on our marketplace. We can sort the result by price. The result is then structured using listed_nft_card_dto, where we specify which attributes to return from the result set.
