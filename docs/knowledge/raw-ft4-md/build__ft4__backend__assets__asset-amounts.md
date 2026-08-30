On this page
# Manage asset balances

The FT4 library allows managing various assets, each potentially having its own precision and formatting requirements, which are governed by specific parameters and functions as outlined below.

## Asset structure​

An asset in this system is defined by several attributes:

- ID: A unique identifier (`byte_array`) required to have a fixed length of 32 bytes. 
- Name and Symbol: Display properties (`text`) with a maximum length of 1024 characters. 
- Decimals: Specifies the precision of the asset, affecting how raw amounts are interpreted. 
- Type: Indicates the type of the asset, with a default value of `ASSET_TYPE_FT4`. 
- Issuing blockchain: Identifies the origin blockchain of the asset. 
- Total supply: A mutable field representing the total supply (`big_integer`) of the asset, modifiable during minting or burning operations. 
## Number formatting of asset amounts​

### Decimals and amount representation​

The `decimals` attribute for each asset dictates how amounts should be displayed to users. Raw values are adjusted according to the number of decimal places set for the asset. For example, an amount of `10000`:

- Decimals = 1: Interpreted as `1000.0`
- Decimals = 2: Interpreted as `100.00`
### Formatting function​

The function `format_amount_with_decimals(amount: big_integer, decimals: integer)` is used to format raw amounts into readable strings by adjusting them according to the asset’s decimals:

- Input parameters: 
- `amount`: The raw value of the asset balance (`big_integer`). 
- `decimals`: The number of decimal places (`integer`) specified by the asset. 
- Output: A formatted `text` string representing the amount. 
If `decimals` is zero, the amount is returned as-is. If `decimals` is greater than zero, the function formats the amount by inserting a decimal point accordingly. This ensures amounts display correctly in a way that aligns with the asset's intended precision.

### Example usage​

- `format_amount_with_decimals(11, 2)` will output `"0.11"`. 
### Validation​

The `decimals` attribute is validated through `validate_asset_decimals(decimals: big_integer)`, ensuring it lies within the range `[0, 78]`.

## Amount validation​

The system uses specific constraints on amounts to ensure compliance with blockchain standards:

- Maximum value: `max_asset_amount`, which limits any asset-related amount to avoid overflow issues in EVM `int256` compatibility. 
- Positive amounts: `require_zero_exclusive_asset_amount_limits(value: big_integer, name: text)` enforces non-zero, positive values for any amount field. Attempts to operate with amounts outside this range will throw an error. 
## Balance management and amount calculation​

Balances are managed in the following ways:

- Increase bBalance: The `increase_balance(account, asset, amount)` function increases a given account's balance for a specified asset by a certain amount, adjusting the balance record accordingly. If the balance does not yet exist, it creates a new balance entity. 
- Deduct balance: The `deduct_balance(account, asset, amount)` function reduces the balance for a specified asset and account. It validates that the account holds sufficient assets before performing the deduction, deleting the balance record if the balance reaches zero. 
### Error handling in balance management​

Errors such as `"INSUFFICIENT BALANCE"` are thrown if an account attempts to deduct more than its current balance. Additionally, `"INVALID AMOUNT"` errors may occur if balance management functions are called with values outside allowed bounds.

## Cross-chain asset compatibility​

Cross-chain assets are recognized through the `issuing_blockchain_rid` field, identifying the original blockchain for assets. Transactions involving minting or burning on non-origin chains will throw `"UNAUTHORIZED MINTING"` or `"UNAUTHORIZED BURNING"` errors, enforcing asset integrity across chains.
- Asset structure
- Number formatting of asset amounts
- Decimals and amount representation
- Formatting function
- Example usage
- Validation
- Amount validation
- Balance management and amount calculation
- Error handling in balance management
- Cross-chain asset compatibility