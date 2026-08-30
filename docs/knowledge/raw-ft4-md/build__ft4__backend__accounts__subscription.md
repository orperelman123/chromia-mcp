On this page
# Transfer subscription strategy

When implementing a transfer subscription strategy for your dapp, users will need to pay regularly to access its functionality. This approach helps generate a steady income to sustain and grow your dapp.

To set up the transfer subscription strategy, you'll need to go through several technical steps. First, configure the `chromia.yml` file to enable subscriptions. Then, when it comes to account registration, there are two approaches available: with EVM signatures and without EVM signatures.

If you register an account that includes an EVM signer, you need to call three operations in the following order:

- `evm_signatures`
- `ras_transfer_subscription`
- `register_account`
If the account is registered without EVM signatures, only two operations need to be called, in this order:

- `ras_transfer_subscription`
- `register_account`
Users must transfer a specific amount of tokens from an existing account to their non-existent account. A part of these tokens will be deducted as a fee. This non-existent account represents an empty account that needs to be filled with tokens and is a crucial part of the process.

You can set the desired subscription period by configuring the `subscription_period_days` in the `chromia.yml` file. When the subscription period ends, the account becomes inactive and cannot be used further. To reactivate the account, users must renew the subscription by sending a specific amount of tokens to the account.

#### Benefits of transfer subscriptions:​

- Recurring revenue: Provides a predictable income stream to support dapp sustainability. 
- Easy setup: Simple implementation offers clear advantages to users. 
### Getting started​

There are three ways to specify the asset in the config file for account registration.

Depending on the properties used, the asset configuration will be treated as either a local blockchain asset or a foreign blockchain asset.
The unique RID of the network where the foreign asset was registered can be acquired by using the
Chromia block explorer. :::
#### Option 1: specifying the foreign asset​

To specify a foreign asset in your configuration file, you must use both the `issuing_blockchain_rid` and `name` properties.
In this case, the configuration will use the specified foreign asset for the account registration process.
```
issuing_blockchain_rid: x"6403ccac0c67f7cb6af78e5e15b3aaebb2b42370f0d12e099ed01fa5a068f9fb"
name: test1
amount: 3L

```

#### Option 2: specifying the foreign asset by id​

We can simply use the `id` property and pass the unique identifier of the asset on the blockchain. Rell will then locate the asset using this identifier.
```
id: x"6403ccac0c67f7cb6af78e5e15b3aaebb2b42370f0d12e099ed01fa5a068f9fb"
min_amount: 3L

```

#### Option 3: specifying the local asset​

To specify the local asset you can simply use the `name` property alone and the Rell will treat the asset as the local asset.
```
name: test1
min_amount: 3L

```

A code example with tests is available here.
To begin, expand the app configuration as shown below.
```
blockchains:
  <my_blockchain_name>:
    module: <my_module_name>
    moduleArgs:
      lib.ft4.core.admin:
        admin_pubkey: "030F1B600A04BE704499CA4EDCEDD767FC16035522AB40AEB700D391412CD721EA"
      lib.ft4.core.accounts.strategies.transfer.subscription:
        asset:
          - name: "MyTestAsset" # issued by current blockchain
            amount: 10L
        subscription_period_days: 30
        free_operations:
          - some_free_operation
        subscription_account: x"66A117242F7CF1B1E6B830232FD1CA059A6E5111CEE99C8FDC2AFF8CE11BDDE4"
      lib.ft4.core.accounts.strategies.transfer:
        rules:
          - sender_blockchain: "*"
            sender: "*"
            recipient: "*"
            asset:
              - name: "MyTestAsset"
                min_amount: 100L
            timeout_days: 60
            strategy:
              - "subscription"
compile:
  rellVersion: 0.13.14
database:
  schema: schema_my_rell_dapp
test:
  modules:
    - test
  moduleArgs:
    lib.ft4.core.admin:
      admin_pubkey: "030F1B600A04BE704499CA4EDCEDD767FC16035522AB40AEB700D391412CD721EA"
    lib.ft4.core.accounts.strategies.transfer.subscription:
      asset:
        - name: "MyTestAsset" # issued by current blockchain
          amount: 10L
      subscription_period_days: 30
      free_operations:
        - some_free_operation
      subscription_account: x"66A117242F7CF1B1E6B830232FD1CA059A6E5111CEE99C8FDC2AFF8CE11BDDE4"
    lib.ft4.core.accounts.strategies.transfer:
      rules:
        - sender_blockchain: "*"
          sender: "*"
          recipient: "*"
          asset:
            - name: "MyTestAsset"
              min_amount: 100L
          timeout_days: 60
          strategy:
            - "subscription"
libs:
  ft4:
    registry: https://gitlab.com/chromaway/ft4-lib.git
    path: rell/src/lib/ft4
    tagOrBranch: v1.1.0r
    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"
    insecure: false
  iccf:
    registry: https://gitlab.com/chromaway/core/directory-chain
    path: src/iccf
    tagOrBranch: 1.32.2
    rid: x"1D567580C717B91D2F188A4D786DB1D41501086B155A68303661D25364314A4D"
    insecure: false

```

This configuration includes two main settings, `strategies.transfer` and `strategies.transfer.subscription`. The example above specifies the following: 
- Transfer strategy: For any sender and recipient on any blockchain, transactions involving the "MyTestAsset" asset with a minimum amount of 100 coins can request a subscription. The timeout period is set to 60 days. 
- Transfer subscription strategy: The subscription price is set at 10 "MyTestAsset" coins for a 30-day period. Users may perform `some_free_operation` even without an active subscription. 
To test the configuration above, refer to the provided tests.
- Getting started