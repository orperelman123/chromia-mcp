# Circom circuits: introduction

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 – Circom circuits](/courses/zero-knowledge-proof/circom-circuits/)
- Circom circuits: introductionOn this page
# Circom circuits: introduction
A Circom circuit is a program written in the Circom language that defines a set of constraints for a zero-knowledge proof. Circom is a domain-specific language (DSL) for creating arithmetic circuits, which are fundamental for building zk-SNARKs (Zero-Knowledge Succinct Non-Interactive Arguments of Knowledge).

In essence, you use Circom to describe a computation in a way that can be proven without revealing all the inputs. The circuit is composed of:

- Signals: These are the variables of the circuit, which can be inputs, outputs, or intermediate values. Inputs can be public or private.

- Constraints: These are equations that define the relationships between the signals. All constraints in Circom must be reduced to a quadratic form: A * B + C = 0, where A, B, and C are linear combinations of the signals.

### Example of a Circom circuit​

Here is a simple example of a Circom circuit that proves knowledge of two numbers that multiply to a given output.

```circom
pragma circom 2.0.0;template Multiplier2() {   // Private inputs   signal input a;   signal input b;   // Public output   signal output c;   // Constraint   c 
In this example:

- a and b are private input signals. The prover knows their values, but they are not revealed to the verifier.

- c is a public output signal. Its value is known to both the prover and the verifier.

- c <== a * b; is a constraint that forces c to be the product of a and b. The <== operator both assigns the value and creates a constraint.

### How to use a Circom circuit​

Using a Circom circuit involves a multi-step process to create and verify a zero-knowledge proof.

- 
Writing the circuit: First, you write your circuit in a .circom file, like the Multiplier2 example above.

- 
Compilation: You compile the .circom file using the Circom compiler. This checks for syntax errors and generates two files:

- A file containing the circuit's constraints in a format called R1CS (Rank-1 Constraint System).

- A program (often in WebAssembly) to calculate the witness.

```bash
circom multiplier.circom --r1cs --wasm --sym
```

- 
Witness calculation: The witness is a special file that contains all the signal values for a specific execution of the circuit (a valid assignment of values to all signals). To generate the witness, you need to provide an input file with the values for the private and public inputs.

- 
Proof generation: Once you have the R1CS file and the witness, you can use a library like snarkjs to generate the actual zero-knowledge proof. This step also requires a "proving key" which is generated from a trusted setup ceremony.

- 
Proof verification: Finally, another party can use the proof, the public inputs/outputs, a "verification key" (also from the trusted setup), and the original circuit's constraints (R1CS) to verify that the proof is valid. If it is, they can be sure that the prover knew a set of private inputs that satisfy the circuit's constraints, without learning what those inputs were.
