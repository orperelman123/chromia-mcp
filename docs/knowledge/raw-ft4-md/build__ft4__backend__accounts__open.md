On this page
# Transfer open strategy

The transfer open strategy requires users to transfer a specific amount of tokens to a non-existent account, which they must then claim to activate it. The non-existent account represents an empty account that needs to be funded with tokens. Once activated, users can utilize the tokens sent to the account.

This strategy provides a middle ground between completely free account creation and paid strategies, requiring token transfer but no fees.

#### Important considerations for production use:​

The transfer open strategy can be used in production, but it comes with spam risks since there are no transfer fees (neither local nor cross-chain). Without proper safeguards, users could potentially create thousands of accounts by transferring tokens back and forth.

To use safely in production:

- Consider limiting account creation to same-address transfers (sender ID = recipient ID) 
- Implement additional rate limiting or validation mechanisms 
- Monitor for potential spam patterns 
The transfer open strategy requires basic configuration in your `chromia.yml` file to enable account registration via transfers.

### Configuration example​

```
blockchains:
  <my_blockchain_name>:
    module: <my_module_name>
    moduleArgs:
      lib.ft4.core.admin:
        admin_pubkey: "YOUR_ADMIN_PUBLIC_KEY"
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
              - "open"
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
            - "open"
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

### Cross-chain configuration example​

For cross-chain transfers with specific asset requirements:

```
blockchains:
  <my_blockchain_name1>:
    module: <my_module_name1>

    config:
      gtx:
        modules:
          - "net.postchain.d1.iccf.IccfGTXModule"

  <my_blockchain_name2>:
    module: <my_module_name2>

    moduleArgs:
      lib.ft4.core.accounts.strategies.transfer:
        rules:
          - sender_blockchain: x"87C9CC394D851114C0C90DA2F97EC4CAB7D86A73D2480EBC76DF98340FD8CA18"
            sender: "X"
            recipient: "X"
            asset:
              issuing_blockchain_rid: x"87C9CC394D851114C0C90DA2F97EC4CAB7D86A73D2480EBC76DF98340FD8CA18"
              name: "Test"
              min_amount: 10000000L
            timeout_days: 15
            strategy: "open"

    config:
      gtx:
        modules:
          - "net.postchain.d1.iccf.IccfGTXModule"

libs:
  ft4:
    registry: https://bitbucket.org/chromawallet/ft3-lib
    path: rell/src/lib/ft4
    tagOrBranch: v0.8.0r
    rid: x"B6AE6AC82AC735BFB9E4E412FFB76BF95380E94F371F5F6A14E71A3AA7D5FEF6"
    insecure: false

  iccf:
    registry: https://gitlab.com/chromaway/core/directory-chain
    path: src/lib/iccf
    tagOrBranch: 1.87.0
    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"

  iccf_test:
    registry: https://gitlab.com/chromaway/core/directory-chain
    path: src/lib/test/iccf
    tagOrBranch: 1.87.0
    rid: x"873D71557531E35E453587FA71C2D3CCB674F0EB49D18B30FA838C52C1155EB3"

compile:
  rellVersion: 0.14.5

database:
  schema: register_account_transfer_open

```

### Code examples​

You can find comprehensive examples and implementations of this strategy in the following repositories:

- GitLab repository - Complete cross-chain transfer example 
- Bitbucket Rell examples - Rell-side implementation 
- Bitbucket JavaScript examples - Client-side implementation 
These repositories provide working code examples, tests, and detailed implementation guides for the transfer open strategy.
- Configuration example
- Cross-chain configuration example
- Code examples