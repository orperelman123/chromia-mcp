On this page
# Use auth descriptors for accounts

Accounts can have multiple account descriptors connected to them. Each can be made to serve a different purpose, but the most important one is the main auth descriptor.

The auth descriptor itself is a simple struct:

```
struct auth_descriptor {
    auth_type;
    args: list<gtv>;
    rules: gtv;
}

```

The struct is then connected to the account using the account_auth_descriptor entity.

```
entity account_auth_descriptor {
    /** a unique identifier for the auth descriptor */
    id: byte_array;
    /** the account it allows access to */
    key account, id;
    index id;
    /** whether it's single-sig or multi-sig. It will influence the `args` property. */
    auth_type;
    /**
     * It specifies signers and level of access for the auth descriptor.
     * It should be either of the following structs, encoded into a byte array:
     * - `single_sig_args`, if `auth_type` is `S`
     * - `multi_sig_args`, if `auth_type` is `M`
     */
    args: byte_array;
    /**
     * Must be one of these values, converted to gtv and then encoded into a byte array:
     * - **no rules**: `null` (when encoded, it will be converted to `GTV_NULL_BYTES`)
     * - **a simple rule**: a `rule_expression`
     * - **a complex rule**: a list starting with the value `"and"` and followed by
     *   `rule_expression`s
     *
     * The amount of rule expressions should always be less than or equal to
     * `auth_descriptor_config.max_rules`.
     *
     * After the expiration conditions are reached, this auth descriptor
     * will automatically be deleted as soon as the account that owns it sends an
     * operation.
     *
     * Must be `GTV_NULL_BYTES` if this is the `main_auth_descriptor` for the account.
     *
     * @see `rule_expression` explains what it means to expire
     */
    rules: byte_array;
    /**
     * Used for the `op_count` expiration rule, counts how many operations this auth
     * descriptor has authenticated.
     */
    mutable ctr: integer;
    /** When was this auth descriptor registered */
    created: timestamp;
}

```

See the auth descriptor section for more information on auth descriptors.

## Main auth descriptor​

Each account has a main account auth descriptor which is set during account creation. The main account descriptor can only be substituted, it can't be deleted.

```
entity main_auth_descriptor {
    /** the account of which this auth descriptor is the manager of */
    key account;
    /** the auth descriptor that manages the account */
    key auth_descriptor: account_auth_descriptor;
}

```

The main auth descriptor can be updated to another one using the update_main_auth_descriptor function, which can only be called from an operation.

```
function update_main_auth_descriptor(account, auth_descriptor)

```

The main auth descriptor cannot be bound by any rules, meaning it's always valid until replaced. As other descriptors it needs to have manadatory flags set before the update. The previous main auth descriptor is always deleted during the update, and the new one is set as the main auth descriptor if none of the checks above fail.

## Adding other auth descriptors​

As the update_main_auth_descriptor function, the add_auth_descriptor function can only be called from an operation.

```
function add_auth_descriptor(account, auth_descriptor): account_auth_descriptor

```

There is also an operation ready to be used with the same name. The only argument it has is the account auth descriptor which is then used in the function call above:

```
operation add_auth_descriptor(new_desc: accounts.auth_descriptor)

```

The checks which are performed are:

- check if the number of maximum configured auth descriptors has been exceeded 
- check if the auth descriptor args correspond to `single_sig_args` or `multi_sig_args`, which is a set of required flags and number of signers in case of multi_sig_args 
- check auth descriptor rules (to see if the auth descriptor is initially valid) 
If the checks pass, the account_auth_descriptor will be created, and the signer(s) will be associated with the account by calling `add_signers` which will create an `auth_descriptor_signer` entity.

```
entity auth_descriptor_signer {
    /** Either the pubkey or, for EVM signers, the EVM address (without `0x`) */
    id: byte_array;
    /** the auth descriptor this signer can access */
    key account_auth_descriptor, id;
}

```
note
If the signer is using an EVM wallet like MetaMask, only store the EVM address without the leading 0x here. 
#### Creating an account with an auth descriptor​

The most typical way of creating a new account is by calling `create_account_with_auth` which takes a before-created auth descriptor and optionally an id for the account.

```
function create_account_with_auth(auth_descriptor, account_id: byte_array? = null): account

```

As is the case with updating the `main_auth_descriptor`, during account creation, the auth descriptor needs to be without any rules and have mandatory flags. If those 2 checks are passed, the account is created.
note
If native Postchain pubkeys are used, the account_id will be `hash(pubkey)`. In case EVM pubkeys are used, the account_id will be `hash(evm_address)`. 
After the id is derived and the account created, the auth descriptor is added as the main auth descriptor. Finally, the rate limiter state (`rl_state`) is created for the account, which is set from default values configured on the chain.
- Main auth descriptor
- Adding other auth descriptors