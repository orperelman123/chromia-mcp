# Create user accounts

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Register accounts and assets](/courses/marketplace-course/module-ft4/)
- Lesson 2 - Create user accountsOn this page
# Create user accounts

For this course, we want to focus on building a marketplace, and token transfers are the main focus, so we will simplify
account creation a bit.

If you are interested in learning more about how to register accounts using EVM wallets, the process is described in
detail in our News feed dapp course, which you can find [here](/courses/my-news-feed/module-one/register-evm-accounts/).

## Configure FT4 account module​

To configure FT4 for accounts, we add the following configuration to the lib.ft4.core.accounts module.

chromia.yml
```yaml
blockchains:  rell_marketplace:    module: main    moduleArgs:      lib.ft4.core.accounts:        rate_limit:          max_points: 10          recovery_time: 5000          points_at_account_creation: 2compile:  rellVersion: 0.14.9database:  schema: schema_rell_marketplacetest:  modules:    - rell_marketplace_testlibs:  ft4:    registry: https://gitlab.com/chromaway/ft4-lib.git    path: rell/src/lib/ft4    tagOrBranch: v1.1.0r    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"    insecure: false  iccf:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/lib/iccf    tagOrBranch: 1.87.0    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"    insecure: false
```

Setting rate-limit points_at_account_creation means that a user can at most perform 2 operations per time unit. The
recovery time for these points can be set using recovery_time property which defaults to 5000ms. We can also configure
the max_points an account can have or disable rate limiting altogether by setting active to false.

Start the node to verify if the configuration file is set up correctly by running the command chr node start and
querying the config using chr query ft4.get_config.

## Create a user account​

Now that our configurations are set, we can continue registering a user account.

We will do this by creating an operation which creates and account with 1000 tokens. Let's add some imports to
src/rell_marketplace/module.rell

src/rell_marketplace/module.rell
```rell
import lib.ft4.core.accounts.{ account, create_account_with_auth, single_sig_auth_descriptor };
```

and create a new file src/rell_marketplace/accounts.rell and add the following:

src/rell_marketplace/accounts.rell
```rell
operation create_user(pubkey) {    val account = create_account_with_auth(single_sig_auth_descriptor(pubkey, set(["A", "T"])));    Unsafe.mint(account, dapp_meta.asset, 1000);}
```

The operation creates an account for a specific pubkey and adds an auth descriptor with flags A and T. This means
the registered key can be used to perform administrator commands such as adding and removing other auth descriptors and
performing transfers. Then, the operation mints 1000 tokens on the account. Note this this is just for testing and
learning purpose, so we dont have to take any other security measures when it comes to creating tokens and accounts.

If you want to learn more how to handle auth descriptors and accounts, you can take the course
[Create a simple app on Chromia using Rell and React](/courses/my-news-feed/introduction).

Next, we will look at an NFT model, the asset we want to trade in our marketplace.
