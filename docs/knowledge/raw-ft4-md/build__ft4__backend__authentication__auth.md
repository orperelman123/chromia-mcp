On this page
# Use auth handlers for authentication

Authentication in decentralized applications (dapps) verifies if a user has permission to perform specific actions. While the process is straightforward with regular Rell applications, it becomes more complex with FT4 due to support for native and EVM signatures and multiple keys for an FT4 account. However, the complexity is abstracted, and authentication can be easily implemented using the `authenticate` operation.

## The `authenticate` function​

To authenticate a user within an operation, use the following code:

```
operation foo() {
    val account = auth.authenticate();
}

```

Calling this operation from the client side without additional configuration will result in the following error:

`Cannot find auth handler for operation <foo>`

To enable FT4 authentication, define an auth handler for the operation using the `auth_handler` extension function. This function allows specification of the authentication requirements for the operation, such as required auth flags or custom authentication logic. For more information on extendable functions, refer to the extendable functions topic.

```
@extend(auth.auth_handler)
function () = auth.add_auth_handler(
  scope = rell.meta(foo).mount_name,
  flags = []
);

operation foo() {
    val account = auth.authenticate();
}

```

In this example, the `auth_handler` function is defined with an empty list of `flags`, indicating that no specific auth flags are required to call the `foo` operation. The `scope` parameter specifies the scope of the auth handler, which in this case is limited to the `foo` operation.

### Overridable auth handlers​

When defining an auth handler for an operation, for example, if developing a library on top of FT4, it may be preferable to allow developers to override that auth handler with their implementation to suit their use case better. For this, an alternative function for registering an auth handler called `add_overridable_auth_handler` is provided. This function works like `add_auth_handler` but allows a user to call `add_auth_handler` with the same scope as was registered with the overridable auth handler—an action that would cause an error if `add_auth_handler` alone were used.

```
@extend(auth.auth_handler)
function () = auth.add_overridable_auth_handler(
  scope = rell.meta(foo).mount_name,
  flags = []
);

@extend(auth.auth_handler)
function () = auth.add_auth_handler(
  scope = rell.meta(foo).mount_name,
  flags = ["T"]
);

```

If a user adds their version of an auth handler, it will authorize the operation. If not, the original version will be used.
note
While `add_overridable_auth_handler` allows users to add another auth handler with the same name, it only works once. For example, if overriding auth handlers for one of the built-in FT4 auth handlers, users cannot override that auth handler again. 
## Custom auth messages​

If the transaction is being signed with an EVM key, there will be a generic message suited for better UX on clients like Metamask. it is automatically generated from the operation name and argument values, e.g. for account registration it's going to look like this:

```
"Blockchain: %s\n\nPlease sign the message to register account\n\nAccount ID:\n%s\n\n"

```

To display friendlier messages, define a custom message formatter. The following example demonstrates how to create a custom message formatter for the `bar` operation:

```
function bar_message(gtv) {
    val args = struct<bar>.from_gtv(gtv);
    val arg1 = args.arg1;
    val arg2 = args.arg2;
    return "Please sign the message\nin order to call BAR operation\nwith arguments:\n\n- arg1: %s\n- arg2: %s".format(arg1, arg2);
}

@extend(auth.auth_handler)
function () = auth.add_auth_handler(
  scope = rell.meta(bar).mount_name,
  flags = [],
  message = bar_message(*)
);

operation bar(arg1: text, arg2: integer) {
    val account = auth.authenticate();
}

```

The custom message formatter `bar_message` expects a GTV-encoded list of operation arguments, which can be easily decoded using `struct<op_name>.from_gtv(gtv)`. In this case, arguments for the `bar` operation are decoded using `struct<bar>.from_gtv(gtv)`.

This custom message formatter now also displays argument names. The message is decorated to contain the blockchain rid of the current blockchain and a nonce value to prevent replay attacks.

Additionally, placeholders `{account_id}` and `{auth_descriptor_id}` can be used in custom auth messages, which are replaced by the account ID or auth_descriptor_id during signature verification.

```
function bar_message(gtv) {
    val args = struct<bar>.from_gtv(gtv);
    val arg1 = args.arg1;
    val arg2 = args.arg2;
    return "Please sign the message\nin order to call BAR operation\nwith arguments:\n\n- arg1: %s\n- arg2: %s. This is your account id: {account_id} this is your auth descriptor id: {auth_descriptor_id}".format(arg1, arg2);
}

```

To apply the custom message formatter, specify it in the auth handler definition:

```
@extend(auth.auth_handler)
function () = auth.add_auth_handler(
  scope = rell.meta(bar).mount_name,
  flags = [],
  message = bar_message(*)
);

```

### Account ID and signer types​

An account ID on the Chromia blockchain is derived as the hash of the signer(s) associated with the account. There are two types of signers:

- FT signers (Postchain signers) 
- EVM signers 
### FT (Postchain) signers​

FT signers use Chromia's native Postchain signatures. When an FT signer is used, it is added directly to the signers field of the transaction. The transaction is then signed, and the signature of the entire transaction is added to the `signatures` field before the transaction is submitted.

### EVM signers​

EVM signers operate through Ethereum-compatible wallets, such as MetaMask. Due to limited support for signing custom data structures in these wallets, data structures used for signing are often not very readable for end users.

For example, instead of signing the entire transaction structure as shown below:

```
transaction: {
  "blockchain_rid": {"type": "Buffer", "data": [0, 0, 123, 53, 119, ...]},
  "operations": {
    "name": "my_op",
    "arguments": [1, "abc", 87]
  },
  "signers": [
    "11111111111",
    "22222222222"
  ]
}

```

The user will see a simpler message to sign, like:

```
Do you want to call "my_op"?
It will create a token named ABC
with 1 decimal, and give you
8.7 ABC of balance.

Blockchain RID: 0034df12...
nonce: <something>

```

This message format ensures that EVM users can interact with transactions in a more understandable way.

### Signer type detection​

To distinguish which type of signature the Rell language should expect, it checks if the signer is an EVM address or a public key.

An account created with a public key will have a different ID than an account created with an EVM address, even if they share the same public key.

### Account ID calculation​

- FT signers: The account ID is generated as `hash(pubkey)`. 
- EVM signers: The account ID is generated as `hash(evm_address)`. 
For EVM signers, the EVM address is derived from the public key by taking the first 40 characters of the hashed public key. Specifically:

- The EVM signer account ID is calculated as `hash(evm_address without "0x")`. 
- This corresponds to `hash(hash(pubkey).sub(0, 40))`, where the EVM address is the first 40 characters of the hashed public key. 
## Custom resolver​

In cases where authorization requirements are complex and static flags cannot determine if a user can act, a resolver function is useful. For instance, in the FT4 library, the `ft4.delete_auth_descriptor` operation requires the `A` flag unless the auth descriptor to be deleted is the same as the one authorizing the operation. The resolver will always take the same arguments: args are the same for as for the message (in gtv format), account_id (byte array) and auth_descriptor_ids (list). If the resolver returns null, it means that none of the auth descriptor ids from the list is not authorized for the action.

```
function bar_resolver(
  args: gtv,
  account_id: byte_array,
  auth_descriptor_ids: list<byte_array>
): byte_array? {
    // Only the main auth descriptor for the account can perform this operation
    for (ad_id in auth_descriptor_ids) {
        val main_ad_id = accounts.main_auth_descriptor @ {
            .account.id == account_id
        } .auth_descriptor.id;
        return if (main_ad_id in auth_descriptor_ids) main_ad_id else null;
    }

    return null;
}

@extend(auth.auth_handler)
function () = auth.add_auth_handler(
  scope = rell.meta(bar).mount_name,
  flags = [],
  message = bar_message(*),
  resolver = bar_resolver(*)
);

```

More details about the partial function call (*) can be found here

## Application scope auth handler​

When multiple operations within an application share the same authentication requirements (e.g., auth flags), define an application scope auth handler. If the `scope` property is omitted when defining an auth handler, it becomes an application scope handler and applies to operations without operation scope auth handlers.

```
module;

import lib.ft4.accounts;
import lib.ft4.auth;

query get_all_accounts() = accounts.account @* {} (.id);

@extend(auth.auth_handler)
function () = auth.add_auth_handler(
  flags = []
);

operation foo() {
    val account = accounts.authenticate();
}

operation bar(arg1: text, arg2: integer) {
    val account = accounts.authenticate();
}

```
caution
Defining multiple application scope auth handlers results in a runtime error during authentication. 
## Mount name scope auth handler​

In addition to operation and application scope auth handlers, a mount name scope auth handler is also available. This enables use of a common auth handler for a mount name.

For example, consider this folder structure in a project:

```
src/
|-dir1/
| |-dir2/
| | |-inner.rell
| |
| |-mid.rell
|
|-outer.rell

```

With this code structure:
inner.rell
```
@mount("mid.inner")
module;

operation foo() {
    // ...
}

operation bar() {
    // ...
}

```
mid.rell
```
@mount("mid")
module;
import dir2.inner;

```
outer.rell
```
module;
import dir1.mid;

```
chromia.yml
```
blockchains:
  hello:
    module: outer
#...

```

The operations are now mounted as `mid.inner.foo` and `mid.inner.bar`.

To define an auth handler for all operations in scope `inner`:

```
@extend(auth.auth_handler)
function () = auth.add_auth_handler(
  scope = "mid.inner",
  flags = ["T"]
);

```

In this example, both `foo` and `bar` require the `T` flag to be called.

Detailed information about mount name scope auth handlers, including support for `{operation}` and `{args}` placeholders in the auth message template, will be added later.

## SSO Metamask Example​

This project aims to demonstrate SSO (Single Sign-On) with MetaMask, using Chromia's FT4 and postchain-client libraries. The example shows how to authenticate users with their MetaMask wallet and interact with the Chromia blockchain.

Refer to the Github repository for more details and code examples.
- The `authenticate` function
- Overridable auth handlers
- Custom auth messages
- Account ID and signer types
- FT (Postchain) signers
- EVM signers
- Signer type detection
- Account ID calculation
- Custom resolver
- Application scope auth handler
- Mount name scope auth handler
- SSO Metamask Example