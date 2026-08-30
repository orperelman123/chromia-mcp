On this page
# Manage key stores

In the FT4 client library, the `KeyStore` interface represents a general abstraction for managing cryptographic keys. This abstraction is crucial for implementing security and transaction signing mechanisms. Two key implementations extend this interface:

- `EvmKeyStore`: Designed for Ethereum-compatible keys. 
- `FtKeyStore`: Designed for FT4-specific keys. 
Both implementations provide methods to sign messages or transactions and to integrate with authentication handlers.

For an example implementation of these keystores, refer to the Chromia Keystore Demo repository.

---

### `KeyStore` interface​

The `KeyStore` interface defines the foundational structure and behavior for managing cryptographic keys:

- Properties: 
- `id: Buffer`: A unique identifier for the key. 
- `isInteractive: boolean`: Indicates if signing requires user interaction (e.g., MetaMask). 
---

### `EvmKeyStore` implementation​

#### Description​

The `EvmKeyStore` extends `KeyStore` and implements the `EvmSigner` interface. It is designed for managing Ethereum-compatible (EVM) keys and includes functionality to sign Ethereum messages and handle EVM-specific operations.

#### Properties and methods​

- Properties: 
- `address: Buffer`: The Ethereum address associated with the key (without the leading `0x`). 
- Methods: 
- `signMessage(message: string): Promise<Signature>`: Signs a given message using the EVM key. 
#### Instantiation options​

- 
In-memory `EvmKeyStore`

- Created with ephemeral keys stored in memory. 
- Method: `createInMemoryEvmKeyStore(keyPair: KeyPair): EvmKeyStore`
- Example: 
```
const keyPair = { privKey: Buffer.from("private_key_hex", "hex") };
const evmKeyStore = createInMemoryEvmKeyStore(keyPair);

```

- 
Generic `EvmKeyStore`

- Allows customization with a provided signing function and configuration. 
- Method: `createGenericEvmKeyStore(config: { address: string, signMessage: Function, isInteractive?: boolean }): Promise<EvmKeyStore>`
- Example: 
```
const evmKeyStore = await createGenericEvmKeyStore({
  address: "YourEthereumAddress",
  signMessage: async (message) => "signature_string",
  isInteractive: false,
});

```

- 
Web3 provider `EvmKeyStore`

- Integrates with external Web3 providers (e.g., MetaMask). 
- Method: `createWeb3ProviderEvmKeyStore(externalProvider: Eip1193Provider): Promise<EvmKeyStore>`
- Example: 
```
const evmKeyStore = await createWeb3ProviderEvmKeyStore(window.ethereum);

```

---

### `FtKeyStore` implementation​

The `FtKeyStore` extends `KeyStore` and implements the `FtSigner` interface. It manages FT4-specific keys and supports signing FT4 transactions.

#### Properties and methods​

- Properties: 
- `pubKey: Buffer`: The public key associated with the FT4 key. 
- Methods: 
- `sign(transaction: GTX | RawGtx): Promise<Buffer>`: Signs an FT4 transaction. 
#### Instantiation options​

- 
In-memory `FtKeyStore`

- Created with ephemeral keys stored in memory. 
- Method: `createInMemoryFtKeyStore(keyHolder: KeyPair | SignatureProvider): FtKeyStore`
- Example: 
```
const keyPair = { privKey: Buffer.from("private_key_hex", "hex") };
const ftKeyStore = createInMemoryFtKeyStore(keyPair);

```

---

### Storage options​

The FT4 client supports storing keys in:

- Memory: Keys are stored temporarily and cleared upon application restart. 
- Session storage: Keys persist only for the duration of the browser session using `createSessionStorageLoginKeyStore`. 
- Local storage: Keys persist across browser sessions and application restarts using `createLocalStorageLoginKeyStore`. 
#### Using in-memory storage​

In-memory storage is the default for ephemeral keys, especially for testing.
note
When login keys (private keys) are stored in an in-memory `KeyStore`, they are held in volatile memory (RAM) and are cleared upon page refresh. This also removes the disposable authentication descriptor associated with the session. While the FT4 session remains valid at the blockchain level—since the blockchain does not invalidate sessions upon a client refresh—you will lose the ability to perform authenticated actions.
This is because actions requiring authentication depend on both the disposable authentication descriptor and login keys, the latter being no longer accessible. Although the disposable authentication descriptor remains technically accessible on the blockchain, it cannot be used without the associated login keys.
To restore functionality, you need to reinitialize or reauthenticate the session by providing a new disposable authentication descriptor. 
#### Custom storage implementation​

For session or local storage, wrap the `KeyStore` creation logic to persist the keys using browser APIs or custom storage strategies.

---

### Key differences​

````| Feature | EvmKeyStore | FtKeyStore |
| Purpose | Ethereum-compatible signing | FT4-specific signing |
````| Identifier | address | pubKey |
````| Signing Method | signMessage | sign |
| Storage Options | Memory, Session, Browser | Memory |

---

### Example usage​

#### Case 1: Integrating with Ethereum using `Web3ProviderEvmKeyStore`​

```
const evmKeyStore = await createWeb3ProviderEvmKeyStore(window.ethereum);
const signature = await evmKeyStore.signMessage("Hello, Ethereum!");

```

#### Case 2: Managing keys in memory using `InMemoryFtKeyStore`​

```
const keyPair = { privKey: Buffer.from("private_key_hex", "hex") };
const ftKeyStore = createInMemoryFtKeyStore(keyPair);
const signedTx = await ftKeyStore.sign(someTransaction);

```

---

For a complete reference, please consult the FT4 Authentication module documentation.
- `KeyStore` interface
- `EvmKeyStore` implementation
- `FtKeyStore` implementation
- Storage options
- Key differences
- Example usage