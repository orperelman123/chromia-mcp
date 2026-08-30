On this page
# Handle multi-signature transactions

Multi-signature (multi-sig) transactions provide an added layer of security and shared control over blockchain transactions. In a multi-signature setup, multiple parties must approve a transaction before it can be executed, reducing the risk of unauthorized actions. Multi-sig arrangements are commonly used for scenarios requiring heightened security, such as managing corporate funds, safeguarding shared assets, or establishing trust in decentralized systems.

In a multi-signature transaction, a predefined number of designated signers must authorize the transaction for it to be valid. This setup allows for flexible access control, as users can specify the minimum number of required signatures.

## Creating a new multi-signature transaction​

### Minimal command​

To initiate a new multi-signature transaction, use the following command:

```
chr multi-signature create --signers-file <path-to-signers-file> <op_name> <op_args>

```

This command reads the public keys from the specified signers file, adding them along with the initiator’s own key as signers for the transaction. The transaction will be signed with the configured keypair, while other required signatures will be left blank.

#### Signers file format​

The signers file should follow this format:

```
pubkey1=examplePublicKey1 pubkey2=examplePublicKey2

```

The public keys of the intended signers must be available to the transaction creator.

Additional command options, such as `--settings`, `--network`, `--blockchain-brid`, `--api-url`, or other configurations, can be used to specify transaction settings.

### Creating a transaction with FT auth​

To create a transaction using FT4 authentication, follow this approach:

```
@extend(auth.auth_handler)
function () = auth.add_auth_handler(
  scope = rell.meta(foo).mount_name,
  flags = []
);

operation foo() {
    val account = auth.authenticate();
    val a = 2;
}

operation create_user(signers: list<pubkey>) {
    val account = create_account_with_auth(multi_sig_auth_descriptor(signers, 3, set(["A", "T"])));
}

```

In this example, the `foo()` operation requires an auth descriptor. The descriptor is created using `multi_sig_auth_descriptor(signers, 3, set(["A", "T"]))`, requiring a minimum of three signers.

To create a transaction with FT Auth, use:

```
chr multi-signature create --signers-file <path-to-signers-file> --ft-auth --auth-descriptor-id <auth_descriptor_id> <op_name>

```

The `auth_descriptor_id` can be located in the Chromia Economy Chain Vault.

Ensure the signers file contains at least two public keys, creating a total of three signers when combined with the initiator’s key. This setup adds the `ft4.ft_auth` operation to the transaction with the designated auth descriptor. All keys in the signers file must be included in the specified auth descriptor.

### Transaction output​

Executing the multi-signature creation command generates a file with the transaction in hex format, which should be shared with the next signer.

## Adding signatures to a multi-wignature transaction​

To add a signature to an existing transaction, use:

```
chr multi-signature sign --file <path-to-transaction-file>

```

This command signs the transaction with the configured keypair. A different keypair can be specified using the `--secret` option.

### Signing transaction output​

The sign command produces a file containing the transaction in hex format with the new signature added, which should then be passed to the next signer.

## Sending a fully signed transaction​

After all necessary signatures have been collected, send the transaction using:

```
chr multi-signature send --file <path-to-transaction-file>

```

Additional options like `--settings`, `--network`, `--blockchain-brid`, `--api-url`, and other configurations may be used to determine the transaction destination.

## Viewing a transaction​

To view transaction details, use:

```
chr multi-signature view --file <path-to-transaction-file>

```

### Example output:​

```
{
  "blockchainRID": "7866A7DC9DA2E2075485DE8B877DED6F3AC28D1290D88A977D31C8C59DCDA7C6",
  "operations": [
    {
      "operation": "ft4.ft_auth",
      "arguments": [
        "byteArray: EBC8131DCBF77109409721CFA0031A2EF8A2A05FCACCCF533C537CA399CBC20A",
        "byteArray: 823EB58C2AE2AF05D5EB920983C2B4CE5046BF80B8A8B803EE450F0250E3A72B"
      ]
    },
    {
      "operation": "foo",
      "arguments": []
    },
    {
      "operation": "nop",
      "arguments": ["integer: 1729680747739"]
    }
  ],
  "signers": [
    "031C434E1C5C8FCF0DA097FABBFBADE61F0C694F163DD23C70884978EEFE8480AE",
    "030CB88BF43C4018E752E7ACB66BE271A4E72AA7283DDEA4BD44603FC4B4788DAA",
    "0333E98D20E784EF395131F8FC7B9112470420594EB59EBF217BC100C054D46212"
  ],
  "signatures": [
    "31314F6B30DD06DA9AD5A3781426081DB716DAE0420B8AE2FF4C5D2D1BDAB8631C127B6C56F8B2F3BC39FB7D6914E619E6D6EFBE83319F9A9DE28EAC244B006B",
    "",
    "207C87A336632DBB282C9045A4446EFF7C096E699CC91176691BD936CF84448F6AAE780FAFAC9617C94EDDE67743493F1BA82F44A33478A82AE750410B79EAFB"
  ]
}

```

- Creating a new multi-signature transaction
- Minimal command
- Creating a transaction with FT auth
- Transaction output
- Adding signatures to a multi-wignature transaction
- Signing transaction output
- Sending a fully signed transaction
- Viewing a transaction
- Example output: