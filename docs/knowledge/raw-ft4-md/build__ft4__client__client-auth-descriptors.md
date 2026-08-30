On this page
# Use auth descriptors

The FT4 client library provides flexible tools for creating Single-Signature (`SingleSig`) and Multi-Signature (`MultiSig`) auth descriptors, along with optional rules to customize the permissions.

This document explains the implementation details and demonstrates how to create and instantiate auth descriptors for various use cases.

### Auth descriptor structure​

An auth descriptor is represented by the following TypeScript type:

#### `AuthDescriptor<T>`​

```
export type AuthDescriptor<T extends SingleSig | MultiSig> = {
  id: Buffer; // Unique identifier
  accountId: Buffer; // Account to which this auth descriptor belongs
  authType: AuthType; // "S" for SingleSig, "M" for MultiSig
  rules: AuthDescriptorRules | null; // Optional rules for descriptor
  created: Date; // Creation timestamp
  args: T; // Signature details (SingleSig or MultiSig)
};

```

#### Types of signatures​

- 
`SingleSig`: Used for single-key authentication.

```
export type SingleSig = {
  flags: string[]; // Permissions (e.g., "A", "T")
  signer: Buffer; // Public key for the single signer
};

```

- 
`MultiSig`: Used for multi-key authentication.

```
export type MultiSig = {
  flags: string[]; // Permissions
  signaturesRequired: number; // Number of signatures required for operation
  signers: Buffer[]; // List of public keys
};

```

### Creating auth descriptors​

The FT4 library provides two primary functions for creating auth descriptors:

- `createSingleSigAuthDescriptorRegistration`
- `createMultiSigAuthDescriptorRegistration`
#### Function: `createSingleSigAuthDescriptorRegistration`​

Creates a SingleSig auth descriptor with the specified permissions, public key, and optional rules.

- 
Parameters:

- `flags: string[]` - List of permissions (e.g., `["A"]` for account edits, `["T"]` for transfers). 
- `pubKey: Buffer` - Public key of the signer. 
- `rules?: AuthDescriptorRules` - Optional rules for the descriptor. 
- 
Example:

```
const authDesc = createSingleSigAuthDescriptorRegistration(["A", "T"], pubKey);

```

#### Function: `createMultiSigAuthDescriptorRegistration`​

Creates a MultiSig auth descriptor with the specified permissions, multiple public keys, the number of required signatures, and optional rules.

- 
Parameters:

- `flags: string[]` - List of permissions. 
- `pubKeys: Buffer[]` - List of public keys. 
- `signaturesRequired: number` - Number of required signatures. 
- `rules?: AuthDescriptorRules` - Optional rules for the descriptor. 
- 
Example:

```
const authDesc = createMultiSigAuthDescriptorRegistration(["T"], [pubKey1, pubKey2], 2);

```

### Using rules in auth descriptors​

Rules further refine the conditions under which auth descriptors can be used. These can be simple or complex.

#### Simple rule​

A simple rule specifies a condition for a single variable.

- 
Structure:

```
export type SimpleRule<T extends string> = {
  variable: T; // Variable being constrained (e.g., opCount)
  operator: RuleOperator; // Comparison operator (e.g., lessThan)
  value: number; // Value to compare against
};

```

- 
Example: "Expire after 2 uses"

```
const rule = { variable: "opCount", operator: "lessThan", value: 2 };

```

#### Complex rule​

A complex rule combines multiple simple rules using logical operators (e.g., "and").

- 
Structure:

```
export type ComplexRule<T extends string> = {
  operator: "and";
  rules: SimpleRule<T>[];
};

```

- 
Example: "Expire after 2 uses and only allow transfers"

```
const complexRule = {
  operator: "and",
  rules: [
    { variable: "opCount", operator: "lessThan", value: 2 },
    { variable: "permissions", operator: "equals", value: "T" },
  ],
};

```

### Examples of creating auth descriptors​

#### Single-sig example​

"Whenever the public key signs, they can edit the account or transfer funds."

```
const authDesc = createSingleSigAuthDescriptorRegistration(["A", "T"], pubKey);

```

#### Multi-sig example 1​

"Whenever both pubKey1 and pubKey2 sign, they can transfer funds."

```
const authDesc = createMultiSigAuthDescriptorRegistration(["T"], [pubKey1, pubKey2], 2);

```

#### Multi-sig example 2​

"The first time 3 out of 5 keys sign, they can edit the account."

```
const authDesc = createMultiSigAuthDescriptorRegistration(["A"], [pubKey1, pubKey2, pubKey3, pubKey4, pubKey5], 3, {
  variable: "opCount",
  operator: "lessThan",
  value: 2,
});

```

#### Instantiate rules​

Here's a few example of possible rules which can be created:

```
// Example rules
const lessThanThreeTimes = lessThan(opCount(3));
const untilBlock100 = lessOrEqual(blockHeight(100));
const during2024 = and(
  greaterOrEqual(blockTime(1704067200000)), /Jan 1st, 2024 (millisecond timestamp)
  lessThan(blockTime(1735689600000)) /Jan 1st, 2025
);
const onceDuring2024 = and(
  greaterOrEqual(blockTime(1704067200000)), /Jan 1st, 2024
  lessThan(blockTime(1735689600000)), /Jan 1st, 2025
  lessThan(opCount(2))
);

```

### Instantiation steps​

- 
Generate key pairs Create or load the public/private key pairs:

```
const pubKey1 = Buffer.from("public_key_1_hex", "hex");
const pubKey2 = Buffer.from("public_key_2_hex", "hex");

```

- 
Define permissions Use flags like `["A"]` for account editing or `["T"]` for transfers.

- 
Add rules (optional) Specify simple or complex rules for additional constraints.

- 
Create the auth descriptor Use the appropriate function for `SingleSig` or `MultiSig` auth descriptors.

---

### Key points to remember​

- Flags: Control what the auth descriptor allows (e.g., "A" for account editing, "T" for transfers). 
- Rules: Add constraints like expiration or operational limits. 
- Signature type: Choose `SingleSig` for single-key auth or `MultiSig` for multi-key requirements. 
- Auth descriptor structure
- Creating auth descriptors
- Using rules in auth descriptors
- Examples of creating auth descriptors
- Instantiation steps
- Key points to remember