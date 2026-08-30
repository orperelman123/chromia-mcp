On this page
# Transfer fee strategy

The transfer fee strategy simplifies the process for users by requiring a one-time purchase of a specific amount of tokens to access your dapp's features. This system is ideal for users who prefer a clear and easy-to-follow process.

When implementing the transfer fee strategy for account registration, users need to transfer a specific amount of tokens to a non-existent account. Once the non-existent account receives the tokens, the fee is deducted from the transferred amount. The non-existent account represents an empty account that needs to be topped up with tokens.

#### Key details:​

- Users pay a fixed amount (similar to a one-time purchase) using a specific token to access the dapp's features. 
- This approach provides a clear and easy-to-understand system, suitable for users who prefer straightforward access. 
- Simplifies the dapp's financial management and encourages brand loyalty among satisfied users. 
#### Benefits of transfer fees:​

- Clarity: Users know exactly what they're paying for, fostering trust and transparency. 
- Convenience: Eliminates the need for recurring payments, making it user-friendly. 
- Brand Loyalty: Encourages users to explore all features, potentially increasing engagement. 
### Getting started​

A code example with tests is available here.

To begin, here is an example configuration:

```
blockchains:
  <my_blockchain_name>:
    module: <my_module_name>
    moduleArgs:
      lib.ft4.core.admin:
        admin_pubkey: "YOUR_ADMIN_PUBLIC_KEY"
      lib.ft4.core.accounts.strategies.transfer.fee:
        asset:
          - name: "MyTestAsset" # issued by current blockchain
            amount: 40L
        fee_account: x"YOUR_FEE_ACCOUNT_ADDRESS" # All fees will be collected into this account
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
              - "fee"
compile:
  rellVersion: 0.13.14
database:
  schema: schema_my_rell_dapp
test:
  modules:
    - test
  moduleArgs:
    lib.ft4.core.admin:
      admin_pubkey: "YOUR_ADMIN_PUBLIC_KEY"
    lib.ft4.core.accounts.strategies.transfer.fee:
      asset:
        - name: "MyTestAsset" # issued by current blockchain
          amount: 40L
      fee_account: x"YOUR_FEE_ACCOUNT_ADDRESS" # All fees will be collected into this account
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
            - "fee"
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

This configuration includes two main settings: `strategies.transfer` and `strategies.transfer.fee`. In the example above:

- Transfer strategy: For any user to any recipient from any blockchain using the `MyTestAsset` asset with a minimum amount of 100 coins, a fixed fee can be requested. The timeout is 60 days. 
- Transfer fee strategy: The fee is set at 40 `MyTestAsset` coins. tip
A clear and straightforward fee structure is essential. Transparency ensures users know exactly what they’re paying for. 
To test the configuration above, refer to the available tests.
- Getting started