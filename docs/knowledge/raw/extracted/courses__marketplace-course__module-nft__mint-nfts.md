# Mint NFTs

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 - Build NFT model in Rell](/courses/marketplace-course/module-nft/)
- Lesson 2 - Mint NFTs
# Mint NFTs

With our NFT model in place, we can examine how to mint new NFTs.

# Minting

We will set up operations where users can buy random cards in the marketplace. This will result in the minting of new cards with attributes defined by our game rules in combination with a random element. So, let's add a function to mint a new card.

We start by creating a nft_card and setting fixed properties. We will change these to be calculated later, but we want to focus on creating the NFT for now. Let's add the following function:

src/rell_marketplace/nft.rell
```rell
function mint_card(account) {    val nft = create nft(account);    val health = 10;    val strength = 8;    create nft_card(.nft = nft, .strength = strength, .health = health);}
```

What happens here is that we first create an instance of nft registered to an account.

We then declare our card attributes and create the specific gaming card nft_card, where we pass our fixed attributes.

Now that we've a minting process, we can start with the marketplace. To make the authentication work in our dapp, we need to register an auth_handler. An auth handler is a function which describes the rules for which any call to auth.authenticate function should follow. Lets add the following handler:

src/rell_marketplace/module.rell
```rell
import lib.ft4.auth;@extend(auth.auth_handler)function () = auth.add_auth_handler(  flags = ["T"]);
```

This handler says that all operations within the marketplace module should require the T flag, which is what we used when we registered the account. One can also set a scope on the handler, if the handler should only work for a specific operation, but we will make this a global handler. All operations that uses ft4 authentication will use this handler as a fallback.

We first need a way to buy a new card, so let's add an operation for this in a new file src/rell_marketplace/marketplace.rell:

src/rell_marketplace/marketplace.rell
```rell
operation buy_mystery_card() {    val account = auth.authenticate();    mint_card(account);}
```

We will use the FT4 function auth.authenticate() to verify if the user has permission to perform the operation. If authentication is successful, we retrieve the account and use it to mint a new card.

Next up, we will add some randomness to our generated card to add some flavor to them.
