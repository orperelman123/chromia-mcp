# Frontend architecture

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 – Frontend](/courses/zero-knowledge-proof/frontend/)
- Frontend: architectureOn this page
# Frontend architecture

The frontend is built with Next.js, providing a complete user interface for interacting with zero-knowledge proof operations on Chromia.
Let's explore how it is implemented.

## Key dependencies​

The frontend relies on several important libraries for ZKP operations:

client/package.json
```json
{  "dependencies": {    "@chromia/ft4": "^2.0.0",          // FT4 token standard    "circomlibjs": "^0.1.7",           // Circom library for ZK circuits    "crypto-js": "^4.1.1",             // Cryptographic operations    "ethers": "^5.7.2",                // Ethereum wallet integration    "postchain-client": "^2.0.0",      // Chromia blockchain client    "snarkjs": "^0.7.0"                // zk-SNARK proof generation  }}
```

## Architecture overview​

The frontend is organized into several key components:

- SecureNoteManager: Core ZKP functionality for managing private notes

- ZKP utilities: Proof generation and verification

- Crypto utilities: Cryptographic operations and hashing

- FT4Client: Blockchain interaction layer

- React components: User interface for ZKP operations

## SecureNoteManager - core ZKP engine​

The SecureNoteManager class is the heart of the ZKP system, managing encrypted notes and cryptographic keys:

client/src/services/secureNoteManager.js
```javascript
class SecureNoteManager {    constructor() {        this.poseidon = null;           // Poseidon hash function        this.masterSeed = null;         // Master seed for key derivation        this.spendingKey = null;        // ZK spending key        this.viewingKey = null;         // ZK viewing key        this.encryptionPrivateKey = null; // Asymmetric encryption key        this.encryptionPublicKey = null;  // Asymmetric public key        this.notes = new Map();         // commitment -> note data        this.nullifiers = new Set();    // Used nullifiers        this.isUnlocked = false;        // Security state    }    // Generate secure deterministic master seed from EVM signature + password    async generateSecureMasterSeed(password, accountId, evmWallet) {        const message = `ZKP_WALLET_MASTER_SEED_${password}_${accountId}`;                // Get signature from EVM wallet (MetaMask)        const signature = await evmWallet.signMessage(message);                // Derive deterministic master seed from signature        const signatureSeed = ethers.utils.keccak256(signature);        const masterSeed = ethers.utils.keccak256(            ethers.utils.concat([                ethers.utils.arrayify(signatureSeed),                ethers.utils.toUtf8Bytes(password),                ethers.utils.toUtf8Bytes(accountId)            ])        );                return ethers.utils.arrayify(masterSeed);    }    // Derive ZK keys for zero-knowledge proofs    async deriveZKKeys() {        const masterKey = ethers.utils.keccak256(this.masterSeed);        // Derive spending key (for generating nullifiers and proving ownership)        this.spendingKey = this.poseidon.F.toString(            this.poseidon([BigInt(masterKey)])        );        // Derive viewing key (for ZK proofs)        this.viewingKey = this.poseidon.F.toString(            this.poseidon([BigInt(this.spendingKey), BigInt(1)])        );    }}
```

## ZKP utilities - proof generation​

The ZKP utilities handle the generation of zero-knowledge proofs for private transfers:

client/src/services/ft4Client.js
```javascript
// Generate a proof for a private transferexport const generateProof = async ({  privateSpendKey,  amount_input,  privateAddress_input,  blindingFactor_input,  transfer_amount,  blindingFactor_output_sender,  blindingFactor_output_recipient,  recipient_privateAddress}) => {  // Initialize crypto if needed  await initCrypto();    // Calculate input commitment (for verification)  const commitment_input = hashValues([    amount_input,    privateAddress_input,    blindingFactor_input  ]);    // Calculate sender's change amount  const sender_change = BigInt(amount_input) - BigInt(transfer_amount);    // Calculate output commitments  const commitment_output_sender = hashValues([    sender_change,    privateAddress_input,    blindingFactor_output_sender  ]);    const commitment_output_recipient = hashValues([    transfer_amount,    recipient_privateAddress,    blindingFactor_output_recipient  ]);    // Calculate nullifier (prevents double-spending)  const nullifier = hashValues([privateSpendKey, commitment_input]);    // Prepare inputs for the circuit  const inputs = {    // Private inputs (hidden from verifier)    privateSpendKey: privateSpendKey.toString(),    amount_input: amount_input.toString(),    privateAddress_input: privateAddress_input.toString(),    blindingFactor_input: blindingFactor_input.toString(),    transfer_amount: transfer_amount.toString(),    blindingFactor_output_sender: blindingFactor_output_sender.toString(),    blindingFactor_output_recipient: blindingFactor_output_recipient.toString(),        // Public inputs (visible to verifier)    commitment_input: commitment_input.toString(),    commitment_output_sender: commitment_output_sender.toString(),    commitment_output_recipient: commitment_output_recipient.toString(),    recipient_privateAddress: recipient_privateAddress.toString(),    nullifier: nullifier.toString()  };    try {    // Generate the ZK proof using snarkjs    const { proof, publicSignals } = await snarkjs.plonk.fullProve(      inputs,      '/private_transfer.wasm',  // Circuit WASM file      '/circuit.zkey'           // Proving key    );        // Format the proof for Postchain    const proofData = Object.values(proof).flat().map(value => value.toString());        return {      proof: proofData,      publicSignals: publicSignals.map(signal => signal.toString()),      commitment_input,      commitment_output_sender,       commitment_output_recipient,      recipient_privateAddress,      nullifier,      sender_change    };  } catch (error) {    console.error('Error generating proof:', error);    throw error;  }};
```

## Crypto utilities - cryptographic operations​

The crypto utilities provide essential cryptographic functions:

client/src/utils/crypto.js
```javascript
import CryptoJS from 'crypto-js';// Initialize circomlib for Poseidon hashinglet poseidon;export const initCrypto = async () => {  if (!poseidon) {    const circomlibjs = await import('circomlibjs');    poseidon = await circomlibjs.buildPoseidon();  }  return { poseidon };};// Generate a random blinding factor for commitmentsexport const generateBlindingFactor = () => {  const buf = new Uint8Array(32);  window.crypto.getRandomValues(buf);  return BigInt('0x' + Array.from(buf).map(b => b.toString(16).padStart(2, '0')).join(''));};// Derive private address from private spend keyexport const derivePrivateAddress = async (privateSpendKey) => {  await initCrypto();  return hashValues([privateSpendKey]);};// Hash values using Poseidon (simplified version)export function hashValues(values) {  const combined = values.map(v => v.toString()).join(',');  return BigInt('0x' + CryptoJS.SHA256(combined).toString().slice(0, 16));}// Encrypt note for recipientexport function encryptNote(note, privateAddress) {  // Convert private address to encryption key  const keyHex = BigInt(privateAddress).toString(16).padStart(64, '0');  const key = keyHex.slice(0, 64);    // Encrypt using AES  const noteJson = JSON.stringify(note);  const encrypted = CryptoJS.AES.encrypt(noteJson, key).toString();    return Buffer.from(encrypted, 'utf8');}// Decrypt note if intended for this addressexport function decryptNote(encryptedNote, privateAddress) {  try {    const keyHex = BigInt(privateAddress).toString(16).padStart(64, '0');    const key = keyHex.slice(0, 64);        const encryptedString = Buffer.isBuffer(encryptedNote)       ? encryptedNote.toString('utf8')       : encryptedNote;        const decrypted = CryptoJS.AES.decrypt(encryptedString, key);    const decryptedString = decrypted.toString(CryptoJS.enc.Utf8);        return decryptedString ? JSON.parse(decryptedString) : null;  } catch (error) {    return null; // Note not intended for this address  }}
```

## Frontend user interface​

The main React component provides the user interface for ZKP operations:

client/src/pages/index.js
```javascript
export default function Home() {  // State management for ZKP operations  const [ft4Client, setFt4Client] = useState(null);  const [isWalletUnlocked, setIsWalletUnlocked] = useState(false);  const [publicBalance, setPublicBalance] = useState('0');  const [privateBalance, setPrivateBalance] = useState('0');  const [showPasswordModal, setShowPasswordModal] = useState(false);    // Initialize FT4 Client with ZKP support  useEffect(() => {    const initializeClient = async () => {      const client = new FT4Client();      await client.initialize();      setFt4Client(client);            // Check for existing sessions      const existingSession = await client.checkExistingSession();      if (existingSession) {        // Restore session and sync private notes        if (client.isWalletUnlocked()) {          await client.syncPrivateNotes();        }      }    };        initializeClient();  }, []);  // Handle shielding tokens (public -> private)  const handleShield = async () => {    const performShield = async () => {      const loadingToast = toast.loading('Shielding tokens... Generating ZK proof...');            try {        // Convert to blockchain format        const shieldAmountFormatted = formatAmount(shieldAmount);                // Generate ZK proof and shield tokens        await ft4Client.shieldTokens(BigInt(shieldAmountFormatted));                // Refresh balances        await refreshBalances();                toast.success(`Successfully shielded ${shieldAmount} tokens!`, { id: loadingToast });      } catch (error) {        toast.error('Failed to shield tokens', { id: loadingToast });      }    };    // Prompt for password if wallet is locked    if (!isWalletUnlocked) {      promptForPassword('shield', performShield);    } else {      await performShield();    }  };  // Handle private transfers  const handleTransfer = async () => {    const performTransfer = async () => {      const loadingToast = toast.loading('Processing private transfer... Generating ZK proof...');            try {        const transferAmountFormatted = formatAmount(transferAmount);                // Generate ZK proof and perform private transfer        await ft4Client.privateTransfer(          BigInt(transferAmountFormatted),           transferRecipient        );                await refreshBalances();                toast.success(`Successfully transferred ${transferAmount} tokens privately!`, { id: loadingToast });      } catch (error) {        toast.error('Failed to complete private transfer', { id: loadingToast });      }    };    if (!isWalletUnlocked) {      promptForPassword('transfer', performTransfer);    } else {      await performTransfer();    }  };  // Handle unshielding tokens (private -> public)  const handleUnshield = async () => {    const performUnshield = async () => {      const loadingToast = toast.loading('Unshielding tokens... Generating ZK proof...');            try {        const unshieldAmountFormatted = formatAmount(unshieldAmount);                // Generate ZK proof and unshield tokens        await ft4Client.unshieldTokens(BigInt(unshieldAmountFormatted));                await refreshBalances();                toast.success(`Successfully unshielded ${unshieldAmount} tokens!`, { id: loadingToast });      } catch (error) {        toast.error('Failed to unshield tokens', { id: loadingToast });      }    };    if (!isWalletUnlocked) {      promptForPassword('unshield', performUnshield);    } else {      await performUnshield();    }  };}
```

## Note management and privacy​

The frontend manages private notes with strong encryption:

client/src/services/secureNoteManager.js
```javascript
// Create a new private noteasync createNote(amount, recipientPrivateAddress = null) {    this.requireUnlocked();    const privateAddress = recipientPrivateAddress || this.getPrivateAddress();    const blindingFactor = this.generateBlindingFactor();        // Create commitment: hash(amount, privateAddress, blindingFactor)    const commitment = this.createCommitment(amount, privateAddress, blindingFactor);    const noteData = {        amount: amount.toString(),        privateAddress: privateAddress.toString(),        blindingFactor,        commitment,        isOwned: privateAddress.toString() === this.getPrivateAddress().toString(),        createdAt: Date.now()    };    // Store note locally if we own it    if (noteData.isOwned) {        this.notes.set(commitment, noteData);        await this.saveNotesToStorage();    }    return noteData;}// Save notes with asymmetric encryptionasync saveNotesToStorage() {    const notesData = {        notes: Array.from(this.notes.entries()),        nullifiers: Array.from(this.nullifiers),        version: '2.0',        keypairId: this.keypairSeed.slice(0, 8),        publicEncryptionKey: this.encryptionPublicKey    };    const publicKeyBase64 = this.getPublicEncryptionKey();    const encrypted = await this.encryptDataAsymmetric(        this.safeStringify(notesData),         publicKeyBase64    );        const encryptedBase64 = btoa(String.fromCharCode(...encrypted));    localStorage.setItem(this.getStorageKey('encrypted_notes'), encryptedBase64);}
```

## ZKP operation flow​

The frontend implements the complete ZKP operation flow:

### 1. Shield operation (public → private)​

- User enters amount to shield

- Frontend generates ZK proof proving ownership of public tokens

- Proof is submitted to blockchain

- Public balance decreases, private note is created

### 2. Private transfer​

- User specifies recipient and amount

- Frontend finds suitable private notes to spend

- ZK proof is generated proving:

- Ownership of input notes

- Correct amount calculation

- Valid nullifiers (prevents double-spending)

- New private notes are created for recipient and change

### 3. Unshield operation (private → public)​

- User enters amount to unshield

- Frontend generates ZK proof proving ownership of private notes

- Private notes are nullified

- Public balance increases

## Security features​

The frontend implements several security measures:

- Password-Protected Wallet: Master seed derived from EVM signature + password

- Automatic Lock: Wallet locks after 30 minutes of inactivity

- Asymmetric Encryption: Notes encrypted with derived keypairs

- Session Management: Secure session handling with MetaMask integration

- Note Validation: Prevents double-spending through nullifier tracking

This architecture provides a complete, secure, and user-friendly interface for zero-knowledge proof operations on Chromia,
demonstrating how complex cryptographic operations can be made accessible through modern web technologies.
