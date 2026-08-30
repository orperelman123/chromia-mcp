On this page
# Prioritize transactions

The prioritization is used to determine which transactions should be processed first on the blockchain. The FT4 library offers a basic default implementation which calculates priority based on account activity, configured rate limits, and transaction cost in terms of points. If dapps require a more complex way of calculating priority, the `priority_check` function can be extended for a custom implementation as described below.

### Core concepts​

- Priority value: A zero or posizive decimal value that determines a transaction’s priority within a queue. A higher value indicates a higher priority. 
- Account points: Points assigned to each account, representing its rate-limited transaction capacity. 
- Transaction cost points: The cost in points for each transaction. Transactions are rejected if their cost exceeds the account’s current points. 
## Priority function details​

By importing the `lib.ft4.core.prioritization.default` module, transaction prioritization can be enabled so that the rate-limiting rules are applied to transactions in the transaction queue, which will have the following effects:

- Accounts with more points will get higher priority for their transactions. 
- Accounts with disabled rate limiting will get the highest priority for all their transactions. 
- Transactions that the rate limiter would have rejected will be rejected immediately upon submission. 
- If the queue becomes full, transactions can be removed from the queue to make room for transactions with higher priority. 
This can be customized by importing `lib.ft4.core.prioritization` instead of `lib.ft4.core.prioritization.default` and extending the function `priority_check(tx_body: gtx_transaction_body, tx_size: integer, tx_enter_timestamp: timestamp, current_timestamp: timestamp): priority_state_v1?`.

The FT4 library has its own implementation on the priority check function.

```
import lib.ft4.core.prioritization.*;

@extend(priority_check) function(tx_body: gtx_transaction_body, tx_size: integer, tx_enter_timestamp: timestamp, current_timestamp: timestamp): priority_state_v1 {
    // custom logic here

    return no_op_priority_state();
}

```

More information on extending the function can be found here.

### Purpose​

The `priority_check` function assigns a priority value to transactions based on account activity and rate-limiting rules. It ensures that transactions with sufficient points and higher priority values are processed before others.

### Parameters​

The `priority_check` function accepts the following parameters:

- `tx_body` (gtx_transaction_body): Contains the transaction body and operations. 
- `tx_size`(integer): Size of the transaction in bytes. 
- `tx_enter_timestamp`(timestamp): Timestamp indicating when the transaction entered the queue. 
- `current_timestamp`(timestamp): The current time, used for calculating rate-limiting and priority adjustments. 
### Priority calculation steps​

- Auth operation extraction: 
- Filters for auth operations within `tx_body` to identify and validate relevant accounts. 
- For each auth operation, extracts `account_id` and `auth_descriptor_id`. 
- Account and descriptor validation: 
- Attempts to retrieve account details and corresponding auth descriptor. 
- Returns `no_op_priority_state()` (lowest priority state) if: 
- No valid auth operation exists. 
- Account or descriptor information is missing. 
- Rate limit configuration and points check: 
- If rate limiting is enabled for the account, the function: 
- Retrieves the rate limit configuration and current points for the account. 
- Calculates the priority as the ratio of `current_points` to `max_points`, capped within ([0, 1]). 
- The function returns this calculated priority along with account information. 
- Handling special cases: 
- If the account is not rate-limited (i.e., lacks a rate limit configuration, is exempt, or if rate limiting is disabled): 
- Returns `no_account_priority_state(1.0)`, representing a high priority with no associated account information. 
- If multiple accounts are involved, the priority is assigned based on the first valid account. 
### Return types and states​

The function can produce several specific states based on the account and rate-limiting status:

- 
`no_op_priority_state()`: - Represents the lowest priority state with a priority of `0.0` and no associated account information. - Triggered when no valid account or auth operation is found.

- 
`no_account_priority_state(priority: decimal)`: - Returns a configurable priority (e.g., `1.0`) without account details if: - Rate limiting is disabled globally or bypassed for the account. - The account lacks rate limiter configuration. - Useful for high-priority transactions that bypass rate limits.

- 
`priority_state_v1`: - Struct containing the following fields: - `account_id`: Account ID pushing the transaction. - `account_points`: Current points of the account. - `tx_cost_points`: Transaction cost in points; used to ensure the account can cover the transaction cost. - `priority`: Decimal value, where a higher value indicates higher priority within the queue.

### Example scenario: priority calculation​

For an account with the following values:

- Max points: 100 
- Current points: 50 
- Transaction cost points: 20 
The priority would be calculated as:

priority = current_points / max_points = 50 / 100 = 0.5

Key Components and Definitions `priority_state_v1` Struct: Contains account ID, account points, transaction cost points, and priority value. `priority_check_v1` Query: Standard query used to fetch the priority of each transaction, calling priority_check for its calculation. `no_op_priority_state` and `no_account_priority_state` Functions: Define default priority states for cases without specific account or priority information.
- Core concepts
- Priority function details
- Purpose
- Parameters
- Priority calculation steps
- Return types and states
- Example scenario: priority calculation