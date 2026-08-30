# Add randomness to the card

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 - Build NFT model in Rell](/courses/marketplace-course/module-nft/)
- Lesson 3 - Add randomness to the cardOn this page
# Add randomness to the card

Generating true randomness on the blockchain is challenging because blockchains are natural deterministic systems designed to ensure that operations can be replicated across all nodes. This means that every transaction or function call must produce the same result on every node, which contradicts the unpredictable nature of true randomness.

However, there are ways to simulate randomness using information from block creation. This isn't random but can be enough for simple use cases. This lesson is a bit of a sidetrack. If you want to continue focusing on the marketplace, skip this part and work with fixed attributes for our cards.

### Add random functions​

Let's add random strength and health to our buy_mystery_card operation by first creating a function which generates a pseudo-random number:

src/rell_marketplace/nft.rell
```rell
// Generate a random integer within a specified rangefunction random(high: integer, seed: integer): integer {    // Ensure high is not zero to avoid division by zero    if (high == 0) return 0;    // Calculate the random value using the provided seed    return (op_context.last_block_time - op_context.block_height - op_context.op_index + seed) % high;}
```

We take the Transaction RID to generate a seed and then substring the seed for each random number we need. These seeds are then used to generate random health and strength.

src/rell_marketplace/nft.rell
```rell
function extract_seeds() {    // Extract seeds for randomness from the transaction RID    val seeds = op_context.transaction.tx_rid.to_hex();    val health_seed: integer = integer.from_hex(seeds.sub(0, 4));    val strength_seed: integer = integer.from_hex(seeds.sub(4, 8));    return (health_seed, strength_seed);}
```

With our seed and random functions in place, we add two functions to generate random health and strength. The generate_health and generate_strength functions utilize the extracted seeds to produce random values for health and strength, respectively.

src/rell_marketplace/nft.rell
```rell
// Generate random health within a specified rangefunction generate_health(seed: integer): integer {    val rand = random(30, seed) + 1; // Generate a random value between 1 and 30    val baseline = 30; // Consistent baseline for all cards    return baseline + rand;}// Generate random strength within a specified rangefunction generate_strength(seed: integer): integer {    val rand = random(30, seed) + 1; // Generate a random value between 1 and 30    val baseline = 50; // Consistent baseline for all cards    return baseline + rand * 2; // Adjust range to 50-110}
```

The mint_card function creates a new NFT card with random attributes (strength and health) using the generated values.

src/rell_marketplace/nft.rell
```rell
// Mint a new card with random attributesfunction mint_card(account) {    // Create a new NFT for the specified account    val nft = create nft(account);    // Extract seeds for randomness    val (health_seed, strength_seed) = extract_seeds();    // Generate random health and strength based on the provided seeds    val health = generate_health(health_seed);    val strength = generate_strength(strength_seed);    // Create the NFT card with the generated attributes    create nft_card(        .nft = nft,        .strength = strength,        .health = health    );}
```
