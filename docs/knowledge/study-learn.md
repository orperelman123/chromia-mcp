# Chromia Learn — end-to-end study notes

Studied 26 Aug 2026 from official Chromia Learn only (`https://learn.chromia.com/`) plus official docs/repos those pages link to (`docs.chromia.com`, `bitbucket.org/chromawallet/*`, `gitlab.com/chromaway/*`).

**Access:** none of the 14 homepage courses is login-gated. Sitemap lists 168 public lesson URLs for these courses; all 168 returned HTTP 200 and the full lesson body. No syllabus-only / gated pages.

**How URLs were found:** homepage nav slugs → Docusaurus `/courses/<slug>/…` → live `https://learn.chromia.com/sitemap.xml` (247 URLs). `robots.txt` 404; WebFetch on sitemap returned 500, curl succeeded.

**Platform source:** [learn-chromia Bitbucket](https://bitbucket.org/chromawallet/learn-chromia/). Official docs hub: [docs.chromia.com](https://docs.chromia.com/).

**Shared local toolchain (almost every hands-on course repeats this):**
- PostgreSQL 16.3, DB/user/password `postchain`, collation `C.UTF-8` (Alpine image recommended).
- Chromia CLI `chr` (Java 21+). Install: Homebrew tap `chromia/core`, apt `https://apt.chromia.com`, Scoop bucket, or Docker `registry.gitlab.com/chromaway/core-tools/chromia-cli/chr`.
- Project file `chromia.yml` (blockchains, `moduleArgs`, `libs`, `compile.rellVersion`, tests).
- Typical loop: `chr install` → `chr build` → `chr test` → `chr node start` / `--wipe` → `chr tx` / `chr query`.
- Rell layout used as a production habit: `src/<module>/{module,entities,functions,operations,queries}.rell` plus `src/test/`.
- Official libs pinned via `libs:` in `chromia.yml`:
  - FT4: `https://gitlab.com/chromaway/ft4-lib.git` (course pins `v1.0.0r` or `v1.1.0r` with a content RID).
  - ICCF: `https://gitlab.com/chromaway/core/directory-chain` path `src/lib/iccf` (pin `1.87.0`).
- Clients: `postchain-client` (TS) and `postchain-client-py` (Python).

---

## Coverage

| # | Course | Level | Slug / intro URL | Lessons fetched | Status |
|---|--------|-------|------------------|-----------------|--------|
| 1 | Semantic search with Vector DB on Chromia | Advanced | [/courses/vector-db-movie-demo/introduction](https://learn.chromia.com/courses/vector-db-movie-demo/introduction) | 15/15 | Fully studied |
| 2 | Create your chat agent with Chromia | Advanced | [/courses/chat-agent-course/introduction](https://learn.chromia.com/courses/chat-agent-course/introduction) | 5/5 | Fully studied |
| 3 | Chat agent for native Chromia transactions with GOAT | Advanced | [/courses/chromia-goat-chat-agent/introduction](https://learn.chromia.com/courses/chromia-goat-chat-agent/introduction) | 4/4 | Fully studied |
| 4 | Zero-Knowledge Proofs on Chromia | Advanced | [/courses/zero-knowledge-proof/introduction](https://learn.chromia.com/courses/zero-knowledge-proof/introduction) | 19/19 | Fully studied |
| 5 | Build your first app with Rell on Chromia (BookView) | Beginner | [/courses/book-review/introduction](https://learn.chromia.com/courses/book-review/introduction) | 26/26 | Fully studied |
| 6 | Web3 for Web2 developers | Beginner | [/courses/web3-for-web2-devs/introduction](https://learn.chromia.com/courses/web3-for-web2-devs/introduction) | 11/11 | Fully studied |
| 7 | Big Data | Intermediate | [/courses/big-data/introduction](https://learn.chromia.com/courses/big-data/introduction) | 6/6 | Fully studied |
| 8 | FT4 Asset Management | Advanced | [/courses/ft4-asset/introduction](https://learn.chromia.com/courses/ft4-asset/introduction) | 6/6 | Fully studied |
| 9 | Build a decentralized marketplace using FT4 | Advanced | [/courses/marketplace-course/introduction](https://learn.chromia.com/courses/marketplace-course/introduction) | 15/15 | Fully studied |
| 10 | Monetize your dapp | Advanced | [/courses/monetize-dapp/introduction](https://learn.chromia.com/courses/monetize-dapp/introduction) | 5/5 | Fully studied |
| 11 | Confirm events across blockchains | Intermediate | [/courses/iccf-course/introduction](https://learn.chromia.com/courses/iccf-course/introduction) | 6/6 | Fully studied |
| 12 | Build an event-driven multi-blockchain dapp | Advanced | [/courses/icmf-course/introduction](https://learn.chromia.com/courses/icmf-course/introduction) | 7/7 | Fully studied |
| 13 | Create a simple app on Chromia using Rell and React | Beginner | [/courses/my-news-feed/introduction](https://learn.chromia.com/courses/my-news-feed/introduction) | 27/27 | Fully studied |
| 14 | Build an Asset Management System With React and FT4 | Intermediate | [/courses/ft4-demo-app/introduction](https://learn.chromia.com/courses/ft4-demo-app/introduction) | 16/16 | Fully studied |

**Blocked:** none.

Each course section lists the public intro URL, official repo, linked docs.chromia.com pages, every fetched lesson URL, the official teaching summary, and production-relevant patterns taken from those lessons. None of the 14 homepage courses is login-gated.

---
## 1. Semantic search with Vector DB on Chromia (Advanced)
**URL:** https://learn.chromia.com/courses/vector-db-movie-demo/introduction
**Repo:** https://bitbucket.org/chromawallet/vector-db-movie-demo/src/main/

**Official / repo links:**
- https://docs.chromia.com/clients/postchain-clients/python-client
- https://bitbucket.org/chromawallet/vector-db-movie-demo/src/main/
- https://docs.chromia.com/intro/getting-started/testnet/get-container

**Lessons fetched:** 15/15 (public, not login-gated)

### Syllabus
- [Module 4 – Code deep dive](https://learn.chromia.com/courses/vector-db-movie-demo/code-deep-dive)
- [Interacting with Chromia using the Python client](https://learn.chromia.com/courses/vector-db-movie-demo/code-deep-dive/python-client)
- [Connecting your data to the Vector DB (Rell)](https://learn.chromia.com/courses/vector-db-movie-demo/code-deep-dive/rell-interface)
- [Module 2 – Run the data pipeline](https://learn.chromia.com/courses/vector-db-movie-demo/data-pipeline)
- [Generate sentence embeddings](https://learn.chromia.com/courses/vector-db-movie-demo/data-pipeline/generate-embeddings)
- [Preprocess movie data](https://learn.chromia.com/courses/vector-db-movie-demo/data-pipeline/preprocess)
- [Upload vectors to the blockchain](https://learn.chromia.com/courses/vector-db-movie-demo/data-pipeline/upload-vectors)
- [Semantic movie search on Chromia](https://learn.chromia.com/courses/vector-db-movie-demo/introduction)
- [Search the vector database](https://learn.chromia.com/courses/vector-db-movie-demo/search)
- [Module 1 – Set up your project](https://learn.chromia.com/courses/vector-db-movie-demo/setup)
- [Configure your Rell module](https://learn.chromia.com/courses/vector-db-movie-demo/setup/configure.rell)
- [Deploy your Rell module](https://learn.chromia.com/courses/vector-db-movie-demo/setup/deploy-rell-module)
- [Choose an embedding model](https://learn.chromia.com/courses/vector-db-movie-demo/setup/embedding-model)
- [Finalize your Python environment](https://learn.chromia.com/courses/vector-db-movie-demo/setup/finalize-python-env)
- [Use cases and extensions](https://learn.chromia.com/courses/vector-db-movie-demo/use-cases)

### What it teaches

In this course, you will build a full-stack app that converts movie plot summaries into vector embeddings and stores them on Chromia. We'll store the comprehensive movie metadata on-chain and index the embeddings using the vector_db_extension. This powerful setup allows you to perform semantic searches, letting you query by meaning and retrieve detailed results directly from the blockchain.

- Set up your project environment and run the backend on the Chromia testnet

- Generate and upload vector embeddings along with movie metadata

### Production-relevant patterns (from lesson bodies)

_Curated notes:_


- GTX module plus dimensions must match the model. chromia.yml enables net.postchain.gtx.extensions.vectordb.VectorDbGTXModule and sets vector_db_extension.dimensions (MiniLM 384 / mpnet-base 768 / mpnet-large, e5, bge 1024). A mismatch fails uploads or queries.
- Lease a Vector-DB-capable testnet container and put the container ID plus unique chain name into deployments.testnet. Testnet Directory Chain BRID in the lesson: x"6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92", node https://node0.testnet.chromia.com:7740.
- Deploy then persist chain RID: chr keygen --key-id=... then chr install / chr build then chr deployment create --network testnet then paste generated chains: block back into chromia.yml. Verify with chr deployment update.
- Vector context IDs isolate domains. CONTEXT_MOVIE = 0; one integer context per corpus so the same embedding model does not mix result sets.
- Link vectors to rowids, batch write. add_movies creates movie rows then store_vectors(CONTEXT_MOVIE, vector_ids) in one op. Do not insert one movie per tx.
- Query templates, not ad-hoc SQL. Extension query query_closest_objects takes context, q_vector, max_distance, max_vectors, query_template. Templates get_movies_with_distance and get_movies_with_filter join closest IDs back to metadata and attach distance (lower = closer).
- Same model at index and query time. Encode query with the exact EMBEDDING_MODEL used at upload; serialize as a bracketed float list string.
- Python client: BlockchainClient.create({node_url_pool, blockchain_rid}), Operation add_movies, Transaction plus secp256k1 sign, send_transaction with status polling.
- Upload resilience: batches of 50, retry 5x with exponential backoff (2s, 4s, 8s).
- Filter cheap data first. BOX_OFFICE_THRESHOLD default 100000000 about 1k movies; 0 is about 42k. CPU vs GPU drives threshold.
- RAG extension: embed prompt, vector query, feed hits to an LLM. Course points at the GOAT chat-agent course for the chat loop.
- Upload movie data using the add_movies operation in batches
- Encode a user query into a vector using the same model as during the upload
- Query the chain using Rell query templates
- The distance value indicates how close the vector is to your query — a lower distance signifies greater similarity
- Use add_movies to upload movie metadata and vectors in a single batch operation
- Fetch movies based on their vector similarity or filter by genre as needed
- Ensure consistency in embedding models for accurate results
- Store movie metadata alongside vectors
- Batch insert movies and their vectors
- Utilize Rell query templates to retrieve rich search results
- Store movie metadata in an entity, linking vectors via rowid
- Use add_movies to insert both movies and vectors in a single operation
- Efficiently write all vectors at once using store_vectors
- Use Rell query templates to retrieve rich search results with optional filtering and distance scores
- A "vector" field that stores the embedded plot
- plot – the full plot summary (used for vector embedding)
- Load data from data/movie_vectors.jsonl


---
## 2. Create your chat agent with Chromia (Advanced)
**URL:** https://learn.chromia.com/courses/chat-agent-course/introduction
**Repo:** https://bitbucket.org/chromawallet/chat-agent-course

**Official / repo links:**
- https://bitbucket.org/chromawallet/chat-agent-course/src/main/
- https://docs.chromia.com
- https://bitbucket.org/chromawallet/chat-agent-course
- https://gitlab.com/chromaway/core-tools/chromia-cli
- https://gitlab.com/chromaway/core-tools/chromia-cli/-/packages
- https://gitlab.com/chromaway/core-tools/homebrew-chromia.git
- https://gitlab.com/chromaway/core-tools/scoop-chromia/
- https://bitbucket.org/chromawallet/chat-agent-course.git

**Lessons fetched:** 5/5 (public, not login-gated)

### Syllabus
- [Configure your API key](https://learn.chromia.com/courses/chat-agent-course/configure-api-key)
- [Explore and extend](https://learn.chromia.com/courses/chat-agent-course/explore-and-extend)
- [Create your chat agent with Chromia](https://learn.chromia.com/courses/chat-agent-course/introduction)
- [Set up your project](https://learn.chromia.com/courses/chat-agent-course/setup)
- [Test your setup](https://learn.chromia.com/courses/chat-agent-course/test-your-setup)

### What it teaches

This course equips you with boilerplate code to build a minimalistic AI-powered chat agent on Chromia. The goal is to give you a solid foundation that you can tinker with, allowing you to experiment with different agent strategies, memory management, and AI model integrations.

Chromia is a Relational Blockchain that combines the capabilities of relational databases with blockchain technology, streamlining the process of building decentralized applications (dapps). With Chromia, you can develop dapps in a familiar way, regardless of whether your background is in enterprise or gaming.

A standout feature of Chromia is Rell, a powerful and concise blockchain and database language. Rell simplifies dapp development by enabling you to create efficient, secure, and expressive applications with minimal code while retaining the flexibility of relational databases.

### Production-relevant patterns (from lesson bodies)

- Long-term memory: Discover how the system preserves significant memories over sessions. Experiment with the criteria for transferring short-term memories to long-term memory.
- Investigate how the agent generates responses using stored memories. Tweak the way the agent utilizes these memories to enhance context-rich responses.
- Modify the backend: Adjust the database schema or Rell operations to meet specific project needs.
- Integrate new features: Enhance the agent’s capabilities by integrating new APIs or adding tools.
- Refactor memory strategies: Implement advanced memory cleanup, prioritization, or tagging strategies to improve performance.
- Exploring memory management strategies to enhance contextual accuracy.
- Using Rell for backend operations, including queries and transactions.
- Adapting the chat agent to suit unique requirements or projects.
- The .env file is correctly configured with a valid API key.


---
## 3. Chat agent for native Chromia transactions with GOAT (Advanced)
**URL:** https://learn.chromia.com/courses/chromia-goat-chat-agent/introduction
**Repo:** https://bitbucket.org/chromawallet/chromia-goat-demo

**Official / repo links:**
- https://github.com/goat-sdk/goat
- https://bitbucket.org/chromawallet/chromia-goat-demo

**Lessons fetched:** 4/4 (public, not login-gated)

### Syllabus
- [Code walkthrough](https://learn.chromia.com/courses/chromia-goat-chat-agent/codebase-overview)
- [Explore the chat agent](https://learn.chromia.com/courses/chromia-goat-chat-agent/explore-agent)
- [AI chat agent for Chromia transactions](https://learn.chromia.com/courses/chromia-goat-chat-agent/introduction)
- [Set up your project](https://learn.chromia.com/courses/chromia-goat-chat-agent/setup)

### What it teaches

In this course, you will build an AI-powered chat agent using the [GOAT SDK](https://github.com/goat-sdk/goat) for Chromia. This project shows you how to leverage AI for seamless blockchain interactions, including CHR token transfers, balance inquiries, and real-time price data—all through a conversational interface.

- Set up the project environment and prerequisites

- Use the chat agent to transfer testnet tokens (tCHR)

### Production-relevant patterns (from lesson bodies)

- Manages environment setup, blockchain connections, tool initialization, and user interactions.
- Demonstrates secure blockchain operations using MetaMask for signing and the GOAT SDK for blockchain management.
- Predefined configurations: The CHROMIA_CONFIG object simplifies setup by providing constants for both testnet and mainnet, including:
- NODE_URL_POOL: Node URLs for blockchain communication.
- ECONOMY_CHAIN_BRID: The chain identifier for the Economy Chain.
- Dynamic targeting: Specify either "testnet" or "mainnet" to load the corresponding values from CHROMIA_CONFIG.
- Network targeting: The NODE_URL_POOL defines the target network. Developers can find an updated list of nodes for testnet in the [Chromia Explorer](https://explorer.chromia.com/testnet/cluster/system).
- Chains: Replace blockchainRid to connect to a specific chain or decentralized application (dapp). Use the [Chromia Explorer](https://explorer.chromia.com/testnet) to find a chain’s brid.
- Tokens: Replace assetId with the unique identifier of the desired token. For example, the assetId for tCHR on the Economy Chain can be found under 'Assets' in the [Economy Chain Explorer](https://explorer.chromia.com/testnet/090BCD47149FBB66F02489372E88A454E7A5645ADDE82125D40DF1EF0C76F874).
- MetaMask integration: MetaMask securely handles signing transactions, while Chromia tools manage blockchain interactions.
- The utility function fetches price data and is designed for reuse.
- The extended tool dynamically integrates it into the assistant to facilitate price queries.
- Use the chat agent to transfer testnet tokens (tCHR)
- Query balances and fetch real-time CHR price data


---
## 4. Zero-Knowledge Proofs on Chromia (Advanced)
**URL:** https://learn.chromia.com/courses/zero-knowledge-proof/introduction
**Repo:** https://bitbucket.org/chromawallet/zkp-demo.git

**Official / repo links:**
- https://docs.chromia.com/rell/rell-intro
- https://docs.chromia.com/intro/about/architecture/node#overview-of-postchain
- https://docs.chromia.com/ft4/intro
- https://docs.chromia.com/intro/getting-started/installation/cli-installation
- https://bitbucket.org/chromawallet/zkp-demo.git

**Lessons fetched:** 19/19 (public, not login-gated)

### Syllabus
- [Architecture overview](https://learn.chromia.com/courses/zero-knowledge-proof/architecture-overview)
- [Module 1 – Circom circuits](https://learn.chromia.com/courses/zero-knowledge-proof/circom-circuits)
- [Circom circuits: compile](https://learn.chromia.com/courses/zero-knowledge-proof/circom-circuits/circom-circuits-compile)
- [Circom circuits: introduction](https://learn.chromia.com/courses/zero-knowledge-proof/circom-circuits/circom-circuits-introduction)
- [Circom files overview](https://learn.chromia.com/courses/zero-knowledge-proof/circom-circuits/circom-circuits-project)
- [Module 2 – Dapp](https://learn.chromia.com/courses/zero-knowledge-proof/dapp)
- [Dapp entities](https://learn.chromia.com/courses/zero-knowledge-proof/dapp/dapp-entities)
- [Dapp operations overview](https://learn.chromia.com/courses/zero-knowledge-proof/dapp/dapp-operations)
- [Dapp overview](https://learn.chromia.com/courses/zero-knowledge-proof/dapp/dapp-overview)
- [Dapp queries overview](https://learn.chromia.com/courses/zero-knowledge-proof/dapp/dapp-queries)
- [Dapp: setup and run](https://learn.chromia.com/courses/zero-knowledge-proof/dapp/dapp-setup-run)
- [PLONK verification](https://learn.chromia.com/courses/zero-knowledge-proof/dapp/dapp-verification)
- [Module 3 – Frontend](https://learn.chromia.com/courses/zero-knowledge-proof/frontend)
- [Frontend architecture](https://learn.chromia.com/courses/zero-knowledge-proof/frontend/frontend-explore)
- [Frontend: setup and run](https://learn.chromia.com/courses/zero-knowledge-proof/frontend/frontend-setup-run)
- [Frontend: test](https://learn.chromia.com/courses/zero-knowledge-proof/frontend/frontend-test)
- [Introduction to the course](https://learn.chromia.com/courses/zero-knowledge-proof/introduction)
- [Project setup](https://learn.chromia.com/courses/zero-knowledge-proof/setup)
- [Zero-knowledge proof](https://learn.chromia.com/courses/zero-knowledge-proof/zero-knowledge-proof)

### What it teaches

Welcome to the Zero-Knowledge Proof (ZKP) course on Chromia.
This course will guide you through a production-ready demonstration of private token transfers using zero-knowledge proofs on the Chromia blockchain.
This demo showcases confidential transactions where transfer amounts
remain hidden from the public ledger while maintaining full transactional integrity.

This course is based on a demo application that implements a complete private token lifecycle with four core operations:

- Balance creation (faucet): Initialize public token balances for demonstration purposes.

### Production-relevant patterns (from lesson bodies)

- Public tokens (FT4) exist transparently on the blockchain
- Private tokens exist as cryptographic commitments with hidden amounts
- Shield operations convert public tokens to private commitments
- Private transfers move private tokens between users with complete anonymity
- Unshield operations convert private tokens back to public tokens
- PLONK proofs for efficient verification
- Nullifier generation to prevent double-spending
- Commitment schemes for amount hiding
- ZKP verification using ZKPGTXModule for all private operations
- Double-spending prevention through nullifier tracking
- Input validation ensuring only valid commitments can be spent
- State consistency maintaining accurate UTXO sets
- SecureNoteManager for client-side key management and note encryption
- FT4Client integration for seamless blockchain interaction
- MetaMask wallet connection for user authentication and signing
- shield_operation.zkey & shield_operation_verification_key.json
- unshield_operation.zkey & unshield_operation_verification_key.json
- private_transfer.zkey & verification_key.json
- It performs a trusted setup to generate a proving key (.zkey) and a verification key (verification_key.json). The proving key is used by the client to generate proofs, and the verification key is used by the blockchain to verify them.
- It also generates WebAssembly (.wasm) versions of the circuits for efficient execution in the browser.
- Signals: These are the variables of the circuit, which can be inputs, outputs, or intermediate values. Inputs can be public or private.
- Constraints: These are equations that define the relationships between the signals. All constraints in Circom must be reduced to a quadratic form: A * B + C = 0, where A, B, and C are linear combinations of the signals.


---
## 5. Build your first app with Rell on Chromia (BookView) (Beginner)
**URL:** https://learn.chromia.com/courses/book-review/introduction
**Repo:** https://bitbucket.org/chromawallet/book-course

**Official / repo links:**
- https://docs.chromia.com/cli/key-pair-management#key-pair-reading-flow
- https://docs.chromia.com/rell/language-features/systemlib/system-entities
- https://docs.chromia.com/cli/commands/keygen
- https://docs.chromia.com/rell/language-features/systemlib/require-function
- https://docs.chromia.com
- https://docs.chromia.com/intro/about/architecture/node
- https://docs.chromia.com/intro/about/dapp
- https://docs.chromia.com/rell/language-features/
- https://bitbucket.org/chromawallet/book-course
- https://docs.chromia.com/rell/rell-intro
- https://docs.chromia.com/rell/language-features/modules/entity
- https://gitlab.com/chromaway/core-tools/chromia-cli
- https://gitlab.com/chromaway/core-tools/chromia-cli/-/packages
- https://gitlab.com/chromaway/core-tools/homebrew-chromia.git
- https://gitlab.com/chromaway/core-tools/scoop-chromia/
- https://docs.chromia.com/intro/getting-started/installation/cli-installation#updating-chromia-cli
- https://docs.chromia.com/intro/configuration/project-config

**Lessons fetched:** 26/26 (public, not login-gated)

### Syllabus
- [Lesson 5 - Understand blockchain state and transactions](https://learn.chromia.com/courses/book-review/blockchain-transactions)
- [Let&#x27;s look at an example](https://learn.chromia.com/courses/book-review/blockchain-transactions/example)
- [Understanding blockchain state and transactions](https://learn.chromia.com/courses/book-review/blockchain-transactions/query-transaction)
- [Lesson 1 - Create your first entity](https://learn.chromia.com/courses/book-review/book-entity)
- [Add your first operation](https://learn.chromia.com/courses/book-review/book-entity/basic-operations)
- [Create your first entity](https://learn.chromia.com/courses/book-review/book-entity/tables)
- [Write a query to retrieve all books](https://learn.chromia.com/courses/book-review/book-entity/write-queries)
- [Lesson 2 - Create a related entity](https://learn.chromia.com/courses/book-review/book-review-entity)
- [Adding an operation to create a book review](https://learn.chromia.com/courses/book-review/book-review-entity/basic-operations)
- [Defining the book review entity](https://learn.chromia.com/courses/book-review/book-review-entity/tables)
- [Write a query to retrieve all reviews of a book](https://learn.chromia.com/courses/book-review/book-review-entity/write-queries)
- [Lesson 6 - Build the client](https://learn.chromia.com/courses/book-review/build-client)
- [Complete the example](https://learn.chromia.com/courses/book-review/build-client/complete-example)
- [Prerequisites](https://learn.chromia.com/courses/book-review/build-client/prerequisites)
- [Querying the blockchain with postchain-client](https://learn.chromia.com/courses/book-review/build-client/query-blockchain)
- [Connecting to the Chromia blockchain](https://learn.chromia.com/courses/book-review/build-client/sign-transaction)
- [Lesson 3 - Verify and validate inputs](https://learn.chromia.com/courses/book-review/input-verification)
- [Verify inputs](https://learn.chromia.com/courses/book-review/input-verification/input-verification)
- [Adding structured results from queries](https://learn.chromia.com/courses/book-review/input-verification/structure)
- [Build your first app with Rell on Chromia](https://learn.chromia.com/courses/book-review/introduction)
- [Rell project structure](https://learn.chromia.com/courses/book-review/rell-structure)
- [Set up your project](https://learn.chromia.com/courses/book-review/setup)
- [Lesson 4 - Sign a transaction and filter queries](https://learn.chromia.com/courses/book-review/sign-transaction)
- [Sign a transaction](https://learn.chromia.com/courses/book-review/sign-transaction/sign-transaction)
- [Using filters and sorting in queries](https://learn.chromia.com/courses/book-review/sign-transaction/structure)
- [What’s next?](https://learn.chromia.com/courses/book-review/what-next)

### What it teaches

This course is your starting point for building applications on Chromia using Rell, the programming language behind Chromia decentralized applications (dapps). You will create BookView, a simple book review app, while learning how to model data, write queries, and sign transactions.

Chromia is a relational blockchain that merges the features of a relational database with blockchain capabilities. With Chromia, dapps can be developed in a way that feels familiar to developers from various backgrounds, whether they are working on enterprise applications, games, or smaller projects.

A unique feature of Chromia is Rell, a specialized language designed for both blockchain and database use. Rell offers static typing, increased expressiveness, enhanced database security, and requires up to 10 times fewer lines of code compared to other blockchains.

### Production-relevant patterns (from lesson bodies)

- The user signs the transaction create_book("ISBN1234", "1984", "George Orwell").
- The transaction is sent to the Chromia Node, where it is validated and then sent to the blockchain.
- Inside the operation, the call create_book(isbn = isbn, title = title, author = author); is applied to the DApp table state using Rell.
- If the transaction is confirmed, it is added to a block on the blockchain, and the DApp table state is updated.
- The Chromia Node forwards the query to Rell.
- Rell performs a query on the state to fetch all books.
- The state executes the query and returns the result to Rell.
- The blockchain RID the transaction belongs to
- All operations and their arguments
- Signers of the transaction
- Associated signatures
- operation keyword: This defines a new operation. It functions like a public function that can be called to perform actions that change the state of your dapp.
- Parameters: The create_book operation takes three parameters: isbn, title, and author. These parameters correspond to the attributes of the book entity.
- create command: Inside the operation, the create command inserts a new book entry into the dApp’s state using the provided parameters.
- User signs a transaction: The user initiates a transaction to execute the create_book operation.
- Transaction validation: The Chromia node validates the transaction and forwards it to the blockchain.
- State update: The create_book operation updates the dApp’s state by creating a new book entry.
- Blockchain confirmation: If the transaction is confirmed, it is added to the blockchain, and the dApp’s state is updated accordingly.


---
## 6. Web3 for Web2 developers (Beginner)
**URL:** https://learn.chromia.com/courses/web3-for-web2-devs/introduction

**Official / repo links:**
- https://docs.chromia.com/
- https://bitbucket.org/chromawallet/postchain-client/
- https://gitlab.com/chromaway/
- https://docs.chromia.com/intro/architecture/platform-architecture
- https://docs.chromia.com/ft4/intro

**Lessons fetched:** 11/11 (public, not login-gated)

### Syllabus
- [Chromia dapp overview](https://learn.chromia.com/courses/web3-for-web2-devs/chromia-web3-stack)
- [Traditional web app overview](https://learn.chromia.com/courses/web3-for-web2-devs/classic-web2-stack)
- [Authentication](https://learn.chromia.com/courses/web3-for-web2-devs/compare-authentication)
- [Comparing backends](https://learn.chromia.com/courses/web3-for-web2-devs/compare-backend)
- [Comparing frontends](https://learn.chromia.com/courses/web3-for-web2-devs/compare-frontend)
- [Web3 for Web2 developers](https://learn.chromia.com/courses/web3-for-web2-devs/introduction)
- [Revenue models and operational costs](https://learn.chromia.com/courses/web3-for-web2-devs/revenues-and-op-costs)
- [Scalability](https://learn.chromia.com/courses/web3-for-web2-devs/scalability)
- [Security](https://learn.chromia.com/courses/web3-for-web2-devs/security)
- [Summary](https://learn.chromia.com/courses/web3-for-web2-devs/summary)
- [Benefits and challenges of Web3](https://learn.chromia.com/courses/web3-for-web2-devs/web3-benefits)

### What it teaches

Welcome to our course on making the jump from Web2 development to building decentralized applications (dapps) in the new world of Web3.
The course is designed to fill knowledge gaps and equip you with practical skills to understand the future of the web as a developer.

We will compare the architecture of traditional Web2 applications with that of Web3 dapps on the Chromia platform.
Additionally, we will explore code examples to understand how familiar concepts from Web2 app development correspond to dapp development on Chromia.

At the end of the course, we will look at concepts beyond just programming, like scalability, security, and operational costs on Web3 platforms.

### Production-relevant patterns (from lesson bodies)

- Acts as the brains of the app, processing user requests, performing operations, and returning the desired data.
- The backend also manages authentication, ensuring data security and integrity.
- Key Technologies: HTML, CGI
- Business Model: Banner ads, online directories, subscriptions
- Key Technologies: AJAX, RSS, Blogs, APIs
- Architecture: Decentralized platforms and applications, often built on blockchain technology
- Key Technologies: Blockchain, smart contracts, decentralized applications (dapps), tokens, cryptocurrencies
- Protected Data Ownership: Data is publicly stored, yet ownership is privately controlled. Cryptographic keys ensure that while anyone can see data on the network, only owners with the keys can access or modify it, ensuring user control over personal data.
- Subscription models and one-time purchases are also prevalent.
- Licensing fees for certain software or platforms.
- Using Chromia, there is always the possibility of using a subscription model, freemium model, pay-by feature, or even collecting money off-chain, just like in Web2.
- Early Web3 platforms like Ethereum use gas fees that are unpredictable and make the end-user pay for every action.
- Chromia features a predictable hosting fee where you pay a recurring subscription for a set unit of resources, much like on modern Cloud services today.
- User 1 and User 2 pay a subscription fee and get access to premium features.
- User 1 and User 2's subscription fees cover operation costs; part of it can be earnings for the dapp developer.
- Potential scalability issues due to consensus mechanisms and design.
- Well-established authentication and security solutions that are integrated and widely available in most Web2 stacks.
- Use of cryptography as an inherent part of the system ensures data integrity and user authentication.
- Decentralized infrastructure: Information and control are distributed across nodes, ensuring no single point of authority.
- Token monetization: Ability to integrate native tokens or cryptocurrencies for new revenue models.
- Immutable data: Once data is written to a blockchain, it cannot be changed, ensuring data integrity.
- New concepts: Requires understanding of new concepts like consensus algorithms, dapps, operational costs, and fees.


---
## 7. Big Data (Intermediate)
**URL:** https://learn.chromia.com/courses/big-data/introduction
**Repo:** https://bitbucket.org/chromawallet/big-data-spark/src/main/

**Official / repo links:**
- https://docs.chromia.com/ft4/setup/ft4-setup
- https://gitlab.com/chromaway/ft4-lib.git
- https://docs.chromia.com/rell/core-concepts#entity-definitions
- https://docs.chromia.com/rell/core-concepts#operations
- https://docs.chromia.com/rell/core-concepts#queries
- https://docs.chromia.com/rell/rell-intro
- https://bitbucket.org/chromawallet/big-data-spark/src/main/
- https://gitlab.com/chromaway/core-tools/chromia-cli
- https://gitlab.com/chromaway/core-tools/chromia-cli/-/packages
- https://gitlab.com/chromaway/core-tools/homebrew-chromia.git
- https://gitlab.com/chromaway/core-tools/scoop-chromia/

**Lessons fetched:** 6/6 (public, not login-gated)

### Syllabus
- [Blockchain components](https://learn.chromia.com/courses/big-data/blockchain-side-description)
- [Big data analysis with Chromia blockchain and PySpark](https://learn.chromia.com/courses/big-data/introduction)
- [Prepare the project](https://learn.chromia.com/courses/big-data/project-launch)
- [Run the project](https://learn.chromia.com/courses/big-data/project-run)
- [Python components](https://learn.chromia.com/courses/big-data/python-side-description)
- [Set up your project](https://learn.chromia.com/courses/big-data/setup)

### What it teaches

By the end of this course, you will be able to:

- Understand how to integrate the Chromia blockchain with PySpark.

- Query data from the Chromia blockchain.

### Production-relevant patterns (from lesson bodies)

- Understand how to integrate the Chromia blockchain with PySpark.
- Query data from the Chromia blockchain.
- Asynchronous execution: Utilizes asyncio to handle blockchain transactions asynchronously, ensuring non-blocking operations.
- Blockchain interaction: Facilitates transaction creation and signing with postchain-client-py.
- Environment variables: Employs a .env file for managing sensitive data, such as private keys and configuration values.
- Randomized data generation: Generates random quantities and prices for products.
- Implement pagination to retrieve large amounts of data from the node's database.
- Incorporate error handling for specific blockchain-related errors.
- The complete code repository for this course can be accessed here: [The project repository](https://bitbucket.org/chromawallet/big-data-spark/src/main/).


---
## 8. FT4 Asset Management (Advanced)
**URL:** https://learn.chromia.com/courses/ft4-asset/introduction
**Repo:** https://bitbucket.org/chromawallet/ft4-course/src/main/

**Official / repo links:**
- https://docs.chromia.com/ft4/intro
- https://docs.chromia.com/ft4/backend/accounts/
- https://docs.chromia.com/intro/getting-started/testnet/
- https://docs.chromia.com/intro/deployment/
- https://docs.chromia.com/ft4/client/client-setup
- https://docs.chromia.com/ft4/backend/assets/
- https://bitbucket.org/chromawallet/ft4-course/src/main/
- https://gitlab.com/chromaway/core-tools/chromia-cli
- https://gitlab.com/chromaway/core-tools/chromia-cli/-/packages
- https://gitlab.com/chromaway/core-tools/homebrew-chromia.git
- https://gitlab.com/chromaway/core-tools/scoop-chromia/
- https://gitlab.com/chromaway/ft4-lib.git
- https://gitlab.com/chromaway/core/directory-chain
- https://docs.chromia.com/ft4/account-management/auth-descriptors#built-in-authorization-flags
- https://docs.chromia.com/ft4/backend/assets/locking-assets#lock-account-overview

**Lessons fetched:** 6/6 (public, not login-gated)

### Syllabus
- [Asset functions, operations &amp; queries](https://learn.chromia.com/courses/ft4-asset/asset-operations)
- [Considerations and recommendations](https://learn.chromia.com/courses/ft4-asset/consideration-recomendations)
- [Asset basics](https://learn.chromia.com/courses/ft4-asset/ft4-basics)
- [Asset management](https://learn.chromia.com/courses/ft4-asset/introduction)
- [Project setup and configuration](https://learn.chromia.com/courses/ft4-asset/setup)
- [Testing](https://learn.chromia.com/courses/ft4-asset/testing)

### What it teaches

Welcome to the Asset management course!

This course is designed to teach you how to create and manage digital assets on the Chromia blockchain using the
[FT4 library](https://docs.chromia.com/ft4/intro). You will build a minimal DeFi-oriented dapp that includes a pool
account, admin minting, admin burning, asset locking, and a user-accessible faucet.

In this course, you will:

### Production-relevant patterns (from lesson bodies)

- Creates a pool account with admin privileges
- Generates a unique account ID, derived from blockchain RID and account type
- Only admins can mint new assets
- Assets are minted to the pool account
- Only admins can burn assets
- Users must authenticate
- Transfers are conducted from the pool account
- The sender must be authenticated
- Only admins can lock or unlock assets
- Assets remain in the account but cannot be transferred
- The lock type can be used to categorize different kinds of restrictions (e.g. vesting, escrow)
- Explore more advanced [FT4 features](https://docs.chromia.com/ft4/intro)
- Implement more secure strategies for creating user [accounts](https://docs.chromia.com/ft4/backend/accounts/)
- Consider declaring asset configuration parameters in the chromia.yml file.
- Deployment to the [testnet](https://docs.chromia.com/intro/getting-started/testnet/)
- Deployment to the [mainnet](https://docs.chromia.com/intro/deployment/)
- Build a [frontend interface](https://docs.chromia.com/ft4/client/client-setup)
- Create additional end-to-end (e2e) tests
- Every asset must be registered on the blockchain
- Minting: Creating new assets
- Transferring: Moving assets between accounts
- Locking: Temporarily restricting asset movement


---
## 9. Build a decentralized marketplace using FT4 (Advanced)
**URL:** https://learn.chromia.com/courses/marketplace-course/introduction
**Repo:** https://bitbucket.org/chromawallet/marketplace-course

**Official / repo links:**
- https://docs.chromia.com/ft4/intro
- https://bitbucket.org/chromawallet/marketplace-course
- https://gitlab.com/chromaway/ft4-lib.git
- https://gitlab.com/chromaway/core/directory-chain
- https://docs.chromia.com/cli/introduction
- https://gitlab.com/chromaway/core-tools/chromia-cli
- https://gitlab.com/chromaway/core-tools/chromia-cli/-/packages
- https://gitlab.com/chromaway/core-tools/homebrew-chromia.git
- https://gitlab.com/chromaway/core-tools/scoop-chromia/

**Lessons fetched:** 15/15 (public, not login-gated)

### Syllabus
- [Build a decentralized marketplace using FT4](https://learn.chromia.com/courses/marketplace-course/introduction)
- [Module 3 - Build a marketplace](https://learn.chromia.com/courses/marketplace-course/module-assets)
- [Purchase a card from the marketplace](https://learn.chromia.com/courses/marketplace-course/module-assets/buy-listed-card)
- [Add a fee for buying a mystery card](https://learn.chromia.com/courses/marketplace-course/module-assets/buy-mystery-card)
- [List a card for sale on the marketplace](https://learn.chromia.com/courses/marketplace-course/module-assets/list-card)
- [Test using Chromia CLI](https://learn.chromia.com/courses/marketplace-course/module-assets/test-cli)
- [Test our marketplace](https://learn.chromia.com/courses/marketplace-course/module-assets/test-marketplace)
- [Module 1 - Register accounts and assets](https://learn.chromia.com/courses/marketplace-course/module-ft4)
- [Create user accounts](https://learn.chromia.com/courses/marketplace-course/module-ft4/register-account)
- [Register payment token](https://learn.chromia.com/courses/marketplace-course/module-ft4/register-token)
- [Module 2 - Build NFT model in Rell](https://learn.chromia.com/courses/marketplace-course/module-nft)
- [Mint NFTs](https://learn.chromia.com/courses/marketplace-course/module-nft/mint-nfts)
- [Define the NFT model](https://learn.chromia.com/courses/marketplace-course/module-nft/nft)
- [Add randomness to the card](https://learn.chromia.com/courses/marketplace-course/module-nft/randomness)
- [Set up your project](https://learn.chromia.com/courses/marketplace-course/setup)

### What it teaches

This course will guide you through building a decentralized marketplace where users can buy and sell game trading cards.

What we will cover:

- Registering a payment token to be used in the marketplace

### Production-relevant patterns (from lesson bodies)

- Defining and minting NFTs using a model built in rell
- Handling transactions and transfering NFTs and Payment tokens between users
- A user can buy an NFT, which is then minted in our dapp.
- Dapp account pubkey from moduleArgs settings
- Flags are "A" for managing accounts and "T" to allow token transfers using this account.
- First, this operation requires the transaction to be signed by an FT4 account, which we verify using the authenticate method.
- Then, we query for the owner of the NFT, and we fetch the NFT entity.


---
## 10. Monetize your dapp (Advanced)
**URL:** https://learn.chromia.com/courses/monetize-dapp/introduction
**Repo:** https://bitbucket.org/chromawallet/fee-samples/src/main/

**Official / repo links:**
- https://docs.chromia.com/ft4/intro
- https://bitbucket.org/chromawallet/fee-samples/src/main/
- https://bitbucket.org/chromawallet/fee-samples/src/main/open/
- https://bitbucket.org/chromawallet/fee-samples/src/main/open/rell/src/test/
- https://bitbucket.org/chromawallet/fee-samples/src/main/open/rell/chromia.yml
- https://gitlab.com/chromaway/core-tools/chromia-cli
- https://gitlab.com/chromaway/core-tools/chromia-cli/-/packages
- https://gitlab.com/chromaway/core-tools/homebrew-chromia.git
- https://gitlab.com/chromaway/core-tools/scoop-chromia/
- https://gitlab.com/chromaway/ft4-lib.git
- https://gitlab.com/chromaway/core/directory-chain
- https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_open/
- https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_open/rell/chromia.yml
- https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_fee/
- https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_fee/rell/chromia.yml
- https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_subscription/
- https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_subscription/rell/chromia.yml
- https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_open/rell/src/test/
- https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_fee/rell/src/test/
- https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_subscription/rell/src/test/

**Lessons fetched:** 5/5 (public, not login-gated)

### Syllabus
- [Account registration](https://learn.chromia.com/courses/monetize-dapp/account-registration)
- [Monetize your dapp](https://learn.chromia.com/courses/monetize-dapp/introduction)
- [Open strategy](https://learn.chromia.com/courses/monetize-dapp/open)
- [Set up your project](https://learn.chromia.com/courses/monetize-dapp/setup)
- [Transfer strategies](https://learn.chromia.com/courses/monetize-dapp/transfer)

### What it teaches

This comprehensive course will guide you in monetizing dapps on the Chromia platform, transforming your blockchain innovation into sustainable revenue streams.

The unique monetization strategies provided in this course will allow you to integrate them quickly, test your application, and improve it for further deployment.

Whether you're a seasoned blockchain developer or just starting, this course equips you with the strategies and tools to make your dapp functional and financially thriving.

### Production-relevant patterns (from lesson bodies)

- Custom: In the custom strategy, developers need to create their own registration logic using the create_account_with_auth function.
- Open: The open strategy is often used in development environments as it allows swift account registration without fees, but it should be used cautiously in production environments to avoid potential exploitation for spamming the network. The register_account function is used for this strategy.
- Transfer: Transfer strategies include transfer_subscription, transfer_fee, and transfer_open, which provide templates for developers to configure their dapp. These strategies use the transfer function to transfer tokens to a new account, paying a fee and transferring a designated amount of tokens to initiate the account registration process. If the non-activated account remains unclaimed for a specific period, the sender can retrieve the tokens. It's important to note that the transfer_open strategy does not require paying a fee for account registration.
- Implement additional rate limiting or validation mechanisms
- Monitor for potential spam patterns
- Consider limiting account creation to same-address transfers (sender ID = recipient ID)


---
## 11. Confirm events across blockchains (Intermediate)
**URL:** https://learn.chromia.com/courses/iccf-course/introduction
**Repo:** https://bitbucket.org/chromawallet/iccf-course

**Official / repo links:**
- https://gitlab.com/chromaway/core/directory-chain
- https://docs.chromia.com/intro/about/protocols/icmf
- https://docs.chromia.com/intro/cross-chain/icmf
- https://docs.chromia.com/ft4/intro
- https://docs.chromia.com/intro/getting-started/create-dapp/
- https://bitbucket.org/chromawallet/iccf-course
- https://gitlab.com/chromaway/core-tools/chromia-cli
- https://gitlab.com/chromaway/core-tools/chromia-cli/-/packages
- https://gitlab.com/chromaway/core-tools/homebrew-chromia.git
- https://gitlab.com/chromaway/core-tools/scoop-chromia/

**Lessons fetched:** 6/6 (public, not login-gated)

### Syllabus
- [Digital warehouse chain](https://learn.chromia.com/courses/iccf-course/digital-warehouse-chain)
- [Confirm Events Across Blockchains](https://learn.chromia.com/courses/iccf-course/introduction)
- [Testing the dapp](https://learn.chromia.com/courses/iccf-course/manual-testing)
- [Set up your project](https://learn.chromia.com/courses/iccf-course/setup)
- [Subscription chain](https://learn.chromia.com/courses/iccf-course/subscription-chain)
- [System overview](https://learn.chromia.com/courses/iccf-course/system-overview)

### What it teaches

In this course, you’ll learn how to build a simple digital warehouse app that lets users prove they made a payment on
one blockchain, and then use that proof on another blockchain. This is done using Chromia’s Inter-Chain Confirmation
Facility (ICCF).

What is ICCF?

ICCF (Inter-Chain Confirmation Facility) is a tool that lets you prove that a transaction happened on one blockchain
and have another blockchain recognize that proof. It’s ideal for situations where you need to confirm an event.

### Production-relevant patterns (from lesson bodies)

- User-driven and asynchronous: Users can collect and present proofs at any time after their transaction is
- Security: The event is anchored and the proof is verified by a trusted chain.
- Users make a fictional payment on one blockchain.
- They receive a proof of payment.
- They use that proof to gain access or perform actions on another blockchain.
- Cross-chain subscriptions: Prove that a user paid for a subscription on one chain to unlock services on another.
- Multi-chain access control: Grant access to digital goods or services on one blockchain based on actions (like
- Decentralized identity verification: Prove identity or credentials issued on one chain to another chain’s
- Gaming: Unlock in-game items or features on one blockchain based on achievements or purchases on another.
- The source chain confirms the transaction and includes it in a block.
- The block is sent to the Cluster Anchoring Chain as a transaction.
- The Cluster Anchoring Chain verifies the block (anchors) and includes it in its own block.
- The user constructs a proof by obtaining a confirmation proof for the transaction.


---
## 12. Build an event-driven multi-blockchain dapp (Advanced)
**URL:** https://learn.chromia.com/courses/icmf-course/introduction
**Repo:** https://bitbucket.org/chromawallet/icmf-course

**Official / repo links:**
- https://docs.chromia.com/intro/cross-chain/icmf
- https://docs.chromia.com/intro/cross-chain/iccf
- https://docs.chromia.com/ft4/intro
- https://docs.chromia.com/intro/getting-started/
- https://bitbucket.org/chromawallet/icmf-course
- https://gitlab.com/chromaway/core-tools/chromia-cli
- https://gitlab.com/chromaway/core-tools/chromia-cli/-/packages
- https://gitlab.com/chromaway/core-tools/homebrew-chromia.git
- https://gitlab.com/chromaway/core-tools/scoop-chromia/
- https://gitlab.com/chromaway/core/directory-chain

**Lessons fetched:** 7/7 (public, not login-gated)

### Syllabus
- [Define messages](https://learn.chromia.com/courses/icmf-course/defining-messages)
- [Delivery chain (receive message)](https://learn.chromia.com/courses/icmf-course/delivery-chain)
- [Factory chain (send and receive)](https://learn.chromia.com/courses/icmf-course/factory-chain)
- [Build an event-driven multi-blockchain dapp](https://learn.chromia.com/courses/icmf-course/introduction)
- [Test the dapp](https://learn.chromia.com/courses/icmf-course/manual-testing)
- [Order chain (send message)](https://learn.chromia.com/courses/icmf-course/order-chain)
- [Set up your project](https://learn.chromia.com/courses/icmf-course/setup)

### What it teaches

In this course, you’ll learn how to build a decentralized application (dapp) that consists of three blockchains: Order,
Factory, and Delivery. These blockchains will interact with each other using Chromia’s Inter-Chain Messaging Facility
(ICMF).

What is ICMF?

The Inter-Chain Messaging Facility (ICMF) is a system in the Chromia network that allows different blockchains to send
and receive messages or events between each other automatically, without user intervention.

### Production-relevant patterns (from lesson bodies)

- OC forwards the production requirements to the Factory Chain (FC).
- Event-driven communication: Chains can broadcast messages (events) on specific topics, and other chains can
- Scalable and flexible: New chains can be added by simply subscribing to the topics they care about, without
- Supply chain management: Each stage (ordering, manufacturing, delivery) is managed by a separate chain. When an
- Decentralized finance (DeFi): Separate chains for lending, trading, and collateral management can communicate
- The sender dapp calls the function send_message from the ICMF Rell library. This could be triggered by a user or by
- The receiver node polls for messages on the subscribed topics before each block is built.
- When a message is found, the node calls the __icmf_message special operation on the dapp.
- The ICMF library calls the function receive_icmf_message, which triggers any logic defined by the dapp.
- Entity Relationsrc/order_chain/entities.rell


---
## 13. Create a simple app on Chromia using Rell and React (Beginner)
**URL:** https://learn.chromia.com/courses/my-news-feed/introduction
**Repo:** https://bitbucket.org/chromawallet/news-course

**Official / repo links:**
- https://bitbucket.org/chromawallet/news-course
- https://docs.chromia.com/rell/language-features/
- https://docs.chromia.com/ft4/intro
- https://docs.chromia.com/intro/getting-started/
- https://docs.chromia.com/ft4/backend/authentication/auth#the-authenticate-function
- https://gitlab.com/chromaway/ft4-lib.git
- https://gitlab.com/chromaway/core/directory-chain
- https://docs.chromia.com/intro/configuration/project-structure
- https://docs.chromia.com/rell/language-features/systemlib/require-function
- https://docs.chromia.com/rell/language-features/modules/struct
- https://docs.chromia.com/rell/language-features/modules/struct#structmutable-t
- https://docs.chromia.com/rell/language-features/types/complex-types#tuple
- https://docs.chromia.com/ft4/backend/accounts/overview#account-registration-framework
- https://docs.chromia.com/ft4/backend/authentication/
- https://bitbucket.org/chromawallet/news-course/src/main/frontend/
- https://gitlab.com/chromaway/core-tools/chromia-cli
- https://gitlab.com/chromaway/core-tools/chromia-cli/-/packages
- https://gitlab.com/chromaway/core-tools/homebrew-chromia.git
- https://gitlab.com/chromaway/core-tools/scoop-chromia/

**Lessons fetched:** 27/27 (public, not login-gated)

### Syllabus
- [A simple app on Chromia is created using Rell, React, and FT4](https://learn.chromia.com/courses/my-news-feed/introduction)
- [Module 1 - Create a Rell backend app with FT accounts](https://learn.chromia.com/courses/my-news-feed/module-one)
- [Lesson 2 - Create accounts](https://learn.chromia.com/courses/my-news-feed/module-one/create-accounts)
- [Authentication with FT4 accounts](https://learn.chromia.com/courses/my-news-feed/module-one/create-accounts/authentication)
- [FT4 accounts configuration](https://learn.chromia.com/courses/my-news-feed/module-one/create-accounts/install-configure-ft4)
- [Lesson 1 - Database schema](https://learn.chromia.com/courses/my-news-feed/module-one/data-modeling)
- [Implement the model in Rell](https://learn.chromia.com/courses/my-news-feed/module-one/data-modeling/model)
- [The data model](https://learn.chromia.com/courses/my-news-feed/module-one/data-modeling/tables)
- [Lesson 4 - Input verification and validation](https://learn.chromia.com/courses/my-news-feed/module-one/input-verification)
- [Verify inputs](https://learn.chromia.com/courses/my-news-feed/module-one/input-verification/input-verification)
- [Run unit tests](https://learn.chromia.com/courses/my-news-feed/module-one/input-verification/tests)
- [Lesson 3 - Explore operations and queries](https://learn.chromia.com/courses/my-news-feed/module-one/operations-queries)
- [Basic operations](https://learn.chromia.com/courses/my-news-feed/module-one/operations-queries/basic-operations)
- [Basic queries](https://learn.chromia.com/courses/my-news-feed/module-one/operations-queries/write-queries)
- [Lesson 5 - Project structure of the dapp](https://learn.chromia.com/courses/my-news-feed/module-one/project-structure)
- [Incorporate modules in the dapp](https://learn.chromia.com/courses/my-news-feed/module-one/project-structure/incorporate-modules)
- [Work with Rell modules](https://learn.chromia.com/courses/my-news-feed/module-one/project-structure/modules)
- [Lesson 6 - Register users using EVM wallet](https://learn.chromia.com/courses/my-news-feed/module-one/register-evm-accounts)
- [Register accounts using EVM Wallets](https://learn.chromia.com/courses/my-news-feed/module-one/register-evm-accounts/register-evm-accounts)
- [Test the registration](https://learn.chromia.com/courses/my-news-feed/module-one/register-evm-accounts/test-registration)
- [Module 2 - React project](https://learn.chromia.com/courses/my-news-feed/module-two)
- [Connect the client](https://learn.chromia.com/courses/my-news-feed/module-two/connecting-the-client)
- [Project scaffold](https://learn.chromia.com/courses/my-news-feed/module-two/scaffold)
- [Set up the project](https://learn.chromia.com/courses/my-news-feed/module-two/setup)
- [Summary and manual testing](https://learn.chromia.com/courses/my-news-feed/module-two/summary-and-tests)
- [The project is set up](https://learn.chromia.com/courses/my-news-feed/setup)
- [What is next?](https://learn.chromia.com/courses/my-news-feed/what-next)

### What it teaches

This comprehensive course is designed to guide you through the development of a decentralized news feed app similar to Twitter using Chromia.

By the end of this course, Chromia's features will be understood, and the skills needed to build decentralized applications will be gained.

Various fundamental topics are covered in this course, providing a solid foundation for creating decentralized applications. The key areas to be explored are:

### Production-relevant patterns (from lesson bodies)

- Database model and simple queries: A database model for your news feed app was designed, and basic queries were performed to retrieve and display data.
- Input verification: The validation and securing of user inputs were demonstrated to prevent potential vulnerabilities.
- Accounts: User accounts within your decentralized app were managed, including user registration and authentication.
- Frontend integration (React app): The backend was connected with a React frontend to create a complete user interface for your news feed app.
- Authentication with FT4 accounts
- index user;: This field links to the user entity and creates a one-to-many relationship since a user can have multiple followers. By indexing this field, you enhance the speed of SQL queries.
- index follower: user;: The second field, named "follower," represents the user who follows the specified user in the first field. You also index this field for efficient querying.
- key user, follower;: This combined key ensures that each user can follow another user only once, maintaining the uniqueness of the follower relationship.
- timestamp = op_context.last_block_time;: This field captures the timestamp with an implicit integer data type, defaulting to the time of the last known block's creation. This setup removes the need for the timestamp to be set explicitly in code.
- Use a byte_array with a length of 32 or 64 for the public key.
- Navigate to the test folder in your project's directory.
- There is a file named news_feed_test.rell inside the test folder. This file contains your test cases.
- The @test module declaration indicates that this module contains test code.
- The import^^.news_feed.*; line imports everything from the news_feed module, making your dapp code accessible for testing.


---
## 14. Build an Asset Management System With React and FT4 (Intermediate)
**URL:** https://learn.chromia.com/courses/ft4-demo-app/introduction
**Repo:** https://bitbucket.org/chromawallet/dapp-templates/src/main/asset_management/

**Official / repo links:**
- https://bitbucket.org/chromawallet/dapp-templates/src/main/asset_management/
- https://docs.chromia.com/ft4/intro
- https://docs.chromia.com/ft4/account-management/
- https://docs.chromia.com/ft4/asset-management/
- https://docs.chromia.com/intro/dapp
- https://docs.chromia.com/ft4/backend/accounts/overview#account-registration-framework
- https://docs.chromia.com/rell/language-features/modules/namespace
- https://gitlab.com/chromaway/ft4-lib.git
- https://gitlab.com/chromaway/core/directory-chain
- https://docs.chromia.com/intro/deployment/frontend-application/deploy-on-chain
- https://docs.chromia.com/ft4/client/client-key-store#keystore-interface
- https://gitlab.com/chromaway/core-tools/chromia-cli
- https://gitlab.com/chromaway/core-tools/chromia-cli/-/packages
- https://gitlab.com/chromaway/core-tools/homebrew-chromia.git
- https://gitlab.com/chromaway/core-tools/scoop-chromia/

**Lessons fetched:** 16/16 (public, not login-gated)

### Syllabus
- [Build an asset management system with FT4](https://learn.chromia.com/courses/ft4-demo-app/introduction)
- [Module 2 - Blockchain dapp](https://learn.chromia.com/courses/ft4-demo-app/module-blockchain)
- [Account management](https://learn.chromia.com/courses/ft4-demo-app/module-blockchain/account-management)
- [Asset Registration and Minting](https://learn.chromia.com/courses/ft4-demo-app/module-blockchain/asset-registration)
- [Lesson 1 - Configure the Blockchain dapp](https://learn.chromia.com/courses/ft4-demo-app/module-blockchain/setup)
- [Testing the Asset Management System](https://learn.chromia.com/courses/ft4-demo-app/module-blockchain/test)
- [Module 3 - Frontend application](https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application)
- [Lesson 3 - Account Registration](https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/account-regisration)
- [Lesson 5 - Burn Tokens](https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/burn)
- [Lesson 7 - Deploy onchain](https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/deploy-onchain)
- [Lesson 4 - Register and Mint](https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/register-and-mint)
- [Lesson 1 - Set up the Frontend Application](https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/setup)
- [Lesson 2 - Chromia tools](https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/tools)
- [Lesson 6 - Transfer asset](https://learn.chromia.com/courses/ft4-demo-app/module-frontend-application/transfer)
- [Module 1 - Init Fullstack application](https://learn.chromia.com/courses/ft4-demo-app/module-init)
- [Set up the Fullstack application](https://learn.chromia.com/courses/ft4-demo-app/module-init/setup-application)

### What it teaches

In this course, we will see how to build a digital asset management system using Chromia's FT4 token standard. You'll learn how to create, manage, and transfer digital assets while implementing account management functionality.

What we will cover:

- Setting up an FT4-based project

### Production-relevant patterns (from lesson bodies)

- Setting up an FT4-based project
- Registering and minting digital assets
- Implementing asset transfers between accounts
- Conduct comprehensive testing of the asset management system.
- Creating operations and queries.
- Authenticates the account owner
- Registers the asset and mints initial supply
- Generates a unique asset ID
- Mints the initial supply to the owner's account
- You can spot chromia.yml file to have a FT4 library as a dependency. Update this file with the following configuration:
- Run chr install to download the FT4 library.
- Asset Registration: The asset must be registered and minted
- Transfer Execution: Using FT4's transfer operation
- The essential tools and concepts required to interact with the FT4 token standard.
- Basic interaction with the FT4 token standard.
- Register and mint digital assets.
- Implement asset transfers between accounts.


---

## Cross-cutting production checklist

1. Validate in Rell with require / require_not_exists. Pair with run_must_fail tests.
2. Authenticate with op_context.is_signer or auth.authenticate plus handler flags (A admin, T transfer, S/MySession).
3. Treat Unsafe mint/burn/transfer and open registration as dangerous defaults. Admin-gate mint/burn. Use transfer-fee or subscription before mainnet.
4. Pin FT4 and ICCF by RID with insecure: false.
5. Rate-limit FT4 accounts (points_at_account_creation, recovery_time, max_points).
6. Timestamps from op_context.last_block_time.
7. Deterministic IDs: tx_rid for mints; (name + blockchain_rid).hash() for pool/asset IDs.
8. Money/audit tables use @log and store IDs not live entity refs.
9. ICCF for user-presented proofs (bind BRID, make_transaction_unique). ICMF for automatic L_ scoped events.
10. Clients: directory node pool plus blockchain RID. FT4 session for wallet UX. Never embed production private keys.
11. Extensions need GTX modules: Vector DB, ICCF, ICMF sender/receiver, ZKP/PLONK keys.
12. Hosting is a leased container. Unique testnet chain names. Persist issued chain RID.

## Also on Learn, not on the homepage list

Sitemap/nav also expose short guides and extra courses (not requested): continuous-integration, rell-integration-test, latest-known-time, associate-function, random-number-generation, chromia-comparisons, chromia-for-evm-developers, relationships-course, rell-masterclass, tic-tac-toe.
