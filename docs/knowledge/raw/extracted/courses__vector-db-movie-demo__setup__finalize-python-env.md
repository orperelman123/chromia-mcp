# Finalize your Python environment

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 – Set up your project](/courses/vector-db-movie-demo/setup/)
- Lesson 4 – Finalize your Python environmentOn this page
# Finalize your Python environment

In this lesson, you’ll prepare your Python environment using the provided installation script.

This sets up a virtual environment, installs the correct version of PyTorch (with or without GPU support), and pulls in all required dependencies.

## Navigate to the Python folder​

If you're in the rell/ folder, move up and then into the Python folder:

```bash
cd ../python
```

Alternatively, from the root of the project:

```bash
cd python
```

Make sure you're inside the python/ folder for the next steps.

## Run the install script​

The install script handles everything:

- Creates a vector_demo_env virtual environment (if not already created)

- Detects your platform and GPU availability

- Installs PyTorch with the correct backend (CUDA, MPS, or CPU-only)

- Installs all project dependencies listed in requirements.txt

💡 If venv is not available on your system, you may need to install it.

On Ubuntu/Debian: sudo apt install python3-venv

To run the installer:

```bash
python3 install.py
```

Once it finishes, you can activate the virtual environment:

```bash
source vector_demo_env/bin/activate
```

## Update your .env file​

Open the python/.env file and update it with your deployment values:

```env
# A list of Postchain node URLs (must be valid JSON)NODE_URL_POOL='["https://node0.testnet.chromia.com:7740", "https://node1.testnet.chromia.com:7740", "https://node2.testnet.chromia.com:7740", "https://node3.testnet.chromia.com:7740"]'# The blockchain RID of your deployed chainBLOCKCHAIN_RID=# Your private key (for signing transactions)PRIV_KEY=# 💰 Box office threshold for vectorization (movies below this are skipped)BOX_OFFICE_THRESHOLD=100_000_000# 🧠 Embedding model selectionEMBEDDING_MODEL=sentence-transformers/all-mpnet-base-v2
```

- The PRIV_KEY is the private key you generated earlier using chr keygen

- The EMBEDDING_MODEL must match the model you selected in [Lesson 1](/courses/vector-db-movie-demo/setup/embedding-model)

- The BOX_OFFICE_THRESHOLD controls how many movies are embedded and stored on-chain:

- A higher value includes only top-grossing movies

→ Fewer vectors = faster embedding (text-to-vector conversion)

- A lower value includes more titles

→ More vectors = slower embedding, especially on CPU

If you're on CPU only, consider a higher threshold (e.g. 500_000_000) for speed.

On GPU, you can lower it — or set it to 0 to embed everything (which may take ~15–20 minutes).

The default (100_000_000) is a good balance for quick results on GPU.

## What’s next?​

With your Python environment finalized and .env fully configured, you’re ready to start working with your data.
