# Circom circuits: compile

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 – Circom circuits](/courses/zero-knowledge-proof/circom-circuits/)
- Circom circuits: compileOn this page
# Circom circuits: compile

## Setup Circom circuits​

The first component to set up is the Circom circuits. These need to be compiled to generate proving and verification keys.

```bash
cd circom_circuit
```

Install dependencies

```bash
npm install
```

## Compile Circom circuits​

Compile all circuits and generate proving/verification keys

```bash
chmod +x compile_complete.sh
```

Compile circuits

```bash
./compile_complete.sh
```

The following files will be generated:

- shield_operation.zkey & shield_operation_verification_key.json

- unshield_operation.zkey & unshield_operation_verification_key.json

- private_transfer.zkey & verification_key.json

This compilation process does a few things:

- It converts the human-readable .circom files into a set of constraints (.r1cs).

- It performs a trusted setup to generate a proving key (.zkey) and a verification key (verification_key.json). The proving key is used by the client to generate proofs, and the verification key is used by the blockchain to verify them.

- It also generates WebAssembly (.wasm) versions of the circuits for efficient execution in the browser.
