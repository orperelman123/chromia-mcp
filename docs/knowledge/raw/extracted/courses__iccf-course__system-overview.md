# System overview

URL: https://learn.chromia.com

- [Home](/)
- Lesson 1 - System overviewOn this page
# System overview

The decentralized application (dapp) we are developing comprises two blockchains: the Subscription Chain and the Digital
Warehouse Chain.

The user initiates a payment on the Subscription Chain to establish a 'subscription'. This involves deducting a
specified value from the user's account. This process can be further customized and expanded using the
[FT-library](https://docs.chromia.com/ft4/intro) at a later stage. Subsequently, the user presents a payment receipt to
the Digital Warehouse Chain, enabling access to the system. This access facilitates the user in managing inventory
updates within a fictional warehouse.

This architecture facilitates the management of multiple warehouses through the deployment of multiple warehouse chains.
By delegating payment processing to another chain, this component could potentially integrate into a broader, more
versatile system.

## Subscription model​

To model our subscription, we will create a structure that can be passed to the Subscription Chain and later verified on
the Digital Warehouse chain. This structure will serve as metadata to be extracted by the warehouse during payment
verification. Let’s create a new directory called subscription_chain and add a file named module.rell to it.

src/subscription_chain/module.rell
```rell
module;
```

Create a file named entities.rell in the src/subscription_chain directory and insert the following code:

src/subscription_chain/entities.rell
```rell
struct subscription {    account_id: pubkey;    period;}enum period {    WEEK,    MONTH,}val MILLIS_IN_A_DAY = 24 * 60 * 60 * 1000;
```

Create another file called functions.rell in the src/subscription_chain and insert the following code:

src/subscription_chain/functions.rell
```rell
function period_price(period): integer {    return when (period) {        MONTH -> 90;        else -> 30;    };}function period_to_millis(period): integer {    return MILLIS_IN_A_DAY * when (period) {        MONTH -> 30;        else -> 7;    } ;}
```

When a user requests a new subscription, we set the account ID and period. Additionally, utility functions are provided
to convert the period to an associated price and duration.
