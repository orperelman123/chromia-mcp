# COURSE vector-db-movie-demo — 15 pages


===== FILE: courses__vector-db-movie-demo__code-deep-dive.md =====

# Module 4 – Code deep dive

URL: https://learn.chromia.com

- [Home](/)
- Module 4 – Code deep dive
# Module 4 – Code deep dive

You have witnessed the system in action—covering everything from preprocessing and embedding to uploading and searching vector data and metadata on-chain.

In this module, we will dive deeper into the architecture and implementation. You will discover how the Rell backend integrates with the Vector DB extension, how we store full movie metadata alongside vectors, and how the Python client interacts with your Chromia chain.

We will walk through the actual code used in the demo while highlighting areas where the design offers flexibility, allowing you to adapt it to suit your specific use case.

## Lessons
[Lesson 1 – Connecting your data to the Vector DB (Rell)](/courses/vector-db-movie-demo/code-deep-dive/rell-interface)[Lesson 2 – Interacting with Chromia using the Python client](/courses/vector-db-movie-demo/code-deep-dive/python-client)[Start module »](/courses/vector-db-movie-demo/code-deep-dive/rell-interface)


===== FILE: courses__vector-db-movie-demo__code-deep-dive__python-client.md =====

# Interacting with Chromia using the Python client

URL: https://learn.chromia.com

- [Home](/)
- [Module 4 – Code deep dive](/courses/vector-db-movie-demo/code-deep-dive/)
- Lesson 2 – Interacting with Chromia using the Python clientOn this page
# Interacting with Chromia using the Python client

This lesson demonstrates how to use the [Postchain Python client](https://docs.chromia.com/clients/postchain-clients/python-client) for semantic vector searches and to upload your movie data to your Chromia chain.

You will:

- Initialize the Python client

- Upload movie data using the add_movies operation in batches

- Encode a user query into a vector using the same model as during the upload

- Query the chain using Rell query templates

If you haven’t configured your .env file and installed the necessary dependencies yet, refer to [the setup module](/courses/vector-db-movie-demo/setup/).

## 1. Initialize the Python client​

To connect to your deployed chain, load your environment and initialize the client as follows:

```python
from postchain_client_py import BlockchainClientfrom dotenv import load_dotenvimport os, jsonload_dotenv()async def get_client():    settings = {        "node_url_pool": json.loads(os.getenv("NODE_URL_POOL", "[]")),        "blockchain_rid": os.getenv("BLOCKCHAIN_RID")    }    return await BlockchainClient.create(settings)
```

## 2. Upload movies and vectors​

Upload your movie data to the chain by calling the add_movies operation from your Rell module. This operation expects a list of flat movie structs, each containing metadata and a vector string.

Here’s how one movie entry looks (all fields are strings, except for wiki_id, which is an integer):

```python
[    12345,    "The Matrix",    "A man discovers reality is a simulation...",    "1999-03-31",    "466000000",    "136",    "English",    "USA",    "Science Fiction, Action",    "[0.1, 0.2, ...]"]
```

To upload in batches:

```python
from postchain_client_py.blockchain_client import Operation, Transactionfrom secp256k1 import PrivateKeyasync def store_batch(client, batch):    private_key = PrivateKey(bytes.fromhex(os.getenv("PRIV_KEY")), raw=True)    public_key = private_key.pubkey.serialize()    operation = Operation(op_name="add_movies", args=[batch])    transaction = Transaction(        operations=[operation],        signers=[public_key],        blockchain_rid=os.getenv("BLOCKCHAIN_RID")    )    signed = await client.sign_transaction(transaction, private_key.private_key)    receipt = await client.send_transaction(signed, do_status_polling=True)    if receipt.status != 0:        raise Exception(f"Upload failed: {receipt.message}")
```

This mirrors the movie_input struct in Rell and effectively stores both metadata and vectors using store_vectors(...) under the hood.

## 3. Run a semantic search​

Your Rell module defines get_movies_with_distance, a query template that returns both movie metadata and the vector distance.

You can call it from Python like this:

```python
async def search_movies(query_vector, max_results=10):    payload = {        "context": 0,        "q_vector": query_vector,        "max_distance": "1.0",        "max_vectors": max_results,        "query_template": {"type": "get_movies_with_distance"}    }    client = await get_client()    return await client.query("query_closest_objects", payload)
```

## 4. Filter search results by genre​

To add a genre filter, use the get_movies_with_filter template. It works similarly but requires an additional argument:

```python
async def search_with_filter(query_vector, genre, max_results=10):    payload = {        "context": 0,        "q_vector": query_vector,        "max_distance": "1.0",        "max_vectors": max_results,        "query_template": {            "type": "get_movies_with_filter",            "args": {"genre_filter": genre}        }    }    client = await get_client()    return await client.query("query_closest_objects", payload)
```

## 5. Create the query vector​

Encode the query vector using the same model selected during preprocessing. For example:

```python
from sentence_transformers import SentenceTransformerimport osmodel = SentenceTransformer(os.getenv("EMBEDDING_MODEL"))def create_query_vector(prompt):    raw_vector = model.encode(prompt)    return "[" + ", ".join(f"{float(v)}" for v in raw_vector) + "]"
```

Ensure model consistencyTo obtain meaningful results, use the exact same embedding model when uploading vectors to Chromia. A mismatch in dimensionality or encoding will lead to query failures or irrelevant results.

## 6. Interpret the results​

Query responses provide a list of matching movies, each with complete metadata and a similarity score:

```python
{    "movie": {        "wiki_id": 12345,        "title": "The Matrix",        "plot": "...",        ...    },    "distance": "0.042"}
```

- The movie field contains all the Rell metadata for the match

- The distance value indicates how close the vector is to your query — a lower distance signifies greater similarity

## Summary​

- Use add_movies to upload movie metadata and vectors in a single batch operation

- Fetch movies based on their vector similarity or filter by genre as needed

- Ensure consistency in embedding models for accurate results


===== FILE: courses__vector-db-movie-demo__code-deep-dive__rell-interface.md =====

# Connecting your data to the Vector DB (Rell)

URL: https://learn.chromia.com

- [Home](/)
- [Module 4 – Code deep dive](/courses/vector-db-movie-demo/code-deep-dive/)
- Lesson 1 – Connecting your data to the Vector DB (Rell)On this page
# Connecting your data to the Vector DB (Rell)

In this lesson, you'll learn to connect your movie data to the Vector DB extension using Rell. You will:

- Store movie metadata alongside vectors

- Batch insert movies and their vectors

- Utilize Rell query templates to retrieve rich search results

The design associates each vector with a movie’s rowid — a pattern you can adapt to any other domain.

## Movie entity structure​

The core entity holds relevant movie metadata:

```rell
entity movie {    wiki_id: integer;    title: text;    plot: text;    release_date: text;    box_office: text;    runtime: text;    languages: text;    countries: text;    genres: text;}
```

These fields enable you to filter and present search results effectively.

## Adding vectors alongside movies​

Link vectors to movies using their rowid (primary key). Here’s how to add both in one operation.

### movie_input struct​

This structure defines the expected format when uploading movies from the client side:

```rell
struct movie_input {    wiki_id: integer;    title: text;    plot: text;    release_date: text;    box_office: text;    runtime: text;    languages: text;    countries: text;    genres: text;    vector: text;}
```

### add_movies: batch insert movies and vectors​

Avoid uploading each movie individually; instead, use this operation to insert an entire list of movies and their corresponding vectors in one call:

```rell
val CONTEXT_MOVIE = 0; // vector context ID for moviesoperation add_movies(movies: list) {    val vector_ids = list();    for (movie in movies) {        val created = create movie(            wiki_id = movie.wiki_id,            title = movie.title,            plot = movie.plot,            release_date = movie.release_date,            box_office = movie.box_office,            runtime = movie.runtime,            languages = movie.languages,            countries = movie.countries,            genres = movie.genres         );        vector_ids.add(vector_db_data(movie.vector, created.rowid.to_integer()));    }    store_vectors(CONTEXT_MOVIE, vector_ids);}
```

This approach uses store_vectors to write all vectors in a single batch, enhancing performance for large datasets.

## Vector context​

The CONTEXT_MOVIE value defines the context for this set of vectors.

Each context ID represents a distinct vector domain — for example, one context for movies, another for legal documents, another for support tickets, etc.

This lets you use the same embedding model across different types of data without cross-contaminating results.

You control this integer ID. In this demo, we use 0 for movies, but you can define other IDs for different content types in your own project.

## Querying search results​

Once your data and vectors are on-chain, leverage Rell query templates to search by meaning, returning results with similarity scores and optional filters.

### get_movies_with_distance: include similarity score​

```rell
struct movie_with_distance {    movie: struct;    distance: decimal;}query get_movies_with_distance(closest_results: list): list {    val closest_result_ids = closest_results @ {} ( @set(rowid(.id)) );    val movies_map = movie @ { .rowid in closest_result_ids } (        @map(.rowid.to_integer(), .to_struct())    );    val results = list();    for (closest_result in closest_results) {        results.add(movie_with_distance(            movies_map[closest_result.id],            closest_result.distance        ));    }    return results;}
```

This template returns both the full movie metadata and its distance from the query vector.

### get_movies_with_filter: filter by genre​

```rell
query get_movies_with_filter(    closest_results: list,    genre_filter: text): list {    val closest_result_ids = closest_results @ {} ( @set(rowid(.id)) );    val movies_map = movie @ { .rowid in closest_result_ids } (        @map(.rowid.to_integer(), .to_struct())    );    val results = list();    for (closest_result in closest_results) {        val m = movies_map[closest_result.id];        if (m.genres.contains(genre_filter)) {            results.add(movie_with_distance(m, closest_result.distance));        }    }    return results;}
```

This version filters results based on a specific genre, but you can easily adapt it to filter by other fields such as release date, language, runtime, etc.

## Summary​

- Store movie metadata in an entity, linking vectors via rowid

- Use add_movies to insert both movies and vectors in a single operation

- Efficiently write all vectors at once using store_vectors

- Use Rell query templates to retrieve rich search results with optional filtering and distance scores

This setup lays the groundwork for semantic vector search on-chain and remains fully extensible to your own domain and data.


===== FILE: courses__vector-db-movie-demo__data-pipeline.md =====

# Module 2 – Run the data pipeline

URL: https://learn.chromia.com

- [Home](/)
- Module 2 – Run the data pipeline
# Module 2 – Run the data pipeline

Now that you have fully configured your environment, it’s time to process and upload the movie data.

In this module, you will download the dataset, clean and normalize the text, generate embeddings using the model you selected earlier, and upload both the vectors and full movie metadata to your deployed Chromia chain.

By the end of this module, you will have populated your on-chain database, making it ready to support semantic search.

Activate vector_demo_env if it's not already activeIf you're in the rell/ folder:

```bash
cd ../python
```
Or from the root of the project:

```bash
cd python
```
Then activate the environment:

```bash
source vector_demo_env/bin/activate
```

## Lessons
[Lesson 1 – Preprocess movie data](/courses/vector-db-movie-demo/data-pipeline/preprocess)[Lesson 2 – Generate sentence embeddings](/courses/vector-db-movie-demo/data-pipeline/generate-embeddings)[Lesson 3 – Upload vectors to the blockchain](/courses/vector-db-movie-demo/data-pipeline/upload-vectors)[Start module »](/courses/vector-db-movie-demo/data-pipeline/preprocess)


===== FILE: courses__vector-db-movie-demo__data-pipeline__generate-embeddings.md =====

# Generate sentence embeddings

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 – Run the data pipeline](/courses/vector-db-movie-demo/data-pipeline/)
- Lesson 2 – Generate sentence embeddingsOn this page
# Generate sentence embeddings

In this lesson, you will use the cleaned movie dataset to create vector embeddings from the plot summaries. You will leverage the model you selected earlier in your python/.env file.

Each embedding will be stored alongside its metadata, preparing it for the next step of writing to the blockchain.

## Filter by box office revenue (optional)​

To streamline processing, the script filters out movies with low box office revenue.

The default threshold is set at $100,000,000, yielding around 1,000 movies. You can adjust this value in your python/.env file by modifying:

BOX_OFFICE_THRESHOLD=100_000_000

- Setting a lower threshold includes more movies (e.g., 0 allows access to the complete dataset of approximately 42,000)

- Raising the threshold to a higher value includes fewer movies, speeding up processing (for example, 1_000_000_000 returns only a handful)

## Run the script​

Navigate to the python/ folder and execute the command:

```bash
python vectorize.py
```

This command loads the data/movie_data.csv, filters the rows based on box office revenue, encodes each plot into a vector using your specified model in .env, and saves the results to a JSONL file.

## Output file​

The output will be saved to:

data/movie_vectors.jsonl

Each line in this file represents a JSON object that contains:

- Complete movie metadata (title, plot, release date, etc.)

- A "vector" field that stores the embedded plot

This file will facilitate the storage of both the vectors and metadata on-chain in the upcoming step.

## What’s next?​

In the next step, you will upload the vectors along with the movie metadata to your Chromia chain, enabling rich and semantic search capabilities.


===== FILE: courses__vector-db-movie-demo__data-pipeline__preprocess.md =====

# Preprocess movie data

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 – Run the data pipeline](/courses/vector-db-movie-demo/data-pipeline/)
- Lesson 1 – Preprocess movie dataOn this page
# Preprocess movie data

In this lesson, you'll actively download a real-world movie dataset and transform it into a clean, structured CSV file. We'll utilize the plot summaries from this file for vector embedding in the next step, while keeping the rest of the metadata for displaying search results.

## 1. Download the dataset​

Navigate to the python/ folder and run:

```bash
python download_data.py
```

This script fetches the [CMU Movie Summary Corpus](https://www.cs.cmu.edu/~ark/personas/) and extracts it into a folder called MovieSummaries/ in the root of your project.

## 2. Preprocess the data​

After downloading, execute:

```bash
python preprocess.py
```

This script reads the raw plot summaries and movie metadata, merges them, and produces a cleaned CSV file:

- data/movie_data.csv

## What’s in the dataset?​

Each row in the cleaned CSV contains:

- wiki_id – a unique movie ID to link the data

- title – the movie title

- plot – the full plot summary (used for vector embedding)

- release_date

- box_office

- runtime

- languages

- countries

- genres

We embed the plot into a vector and store it on-chain alongside the movie metadata, enabling a searchable, semantic, and decentralized system.

## What’s next?​

In the next step, you'll generate sentence embeddings from the movie plots using the model you selected earlier.


===== FILE: courses__vector-db-movie-demo__data-pipeline__upload-vectors.md =====

# Upload vectors to the blockchain

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 – Run the data pipeline](/courses/vector-db-movie-demo/data-pipeline/)
- Lesson 3 – Upload vectors to the blockchainOn this page
# Upload vectors to the blockchain

In this lesson, you will upload vector embeddings along with full movie metadata to your Chromia chain, making the data searchable on-chain through semantic queries.

## Run the script​

Navigate to the python/ folder and execute the following command:

```bash
python upload_vectors.py
```

This script will:

- Load data from data/movie_vectors.jsonl

- Send batches of movie vectors and metadata to the blockchain using the add_movie operation

- Display progress updates as it uploads each movie

## How it works?​

The script processes movies in batches (default: 50 per batch) to maintain transaction efficiency and prevent overload.

Each movie uploads with a single blockchain operation that includes:

- The vector embedding generated from the plot

- Full movie metadata such as title, release date, box office earnings, and more

The script signs and submits each batch to your Chromia chain.

## Retry logic​

If the script encounters an error during a batch upload, it will automatically retry up to 5 times, applying exponential backoff (2s, 4s, 8s, etc.).
This strategy effectively manages network issues and temporary transaction failures.

## Output and confirmation​

As the script runs, you will see live progress in the terminal, indicating how many movies have successfully uploaded.

Once the process concludes, your on-chain database will be fully populated and ready for semantic search.

## What’s next?​

In the next module, you will run actual queries and discover the possibilities of on-chain semantic search.


===== FILE: courses__vector-db-movie-demo__introduction.md =====

# Semantic movie search on Chromia

URL: https://learn.chromia.com

- [Home](/)
- Course overviewOn this page
# Semantic movie search on Chromia

In this course, you will build a full-stack app that converts movie plot summaries into vector embeddings and stores them on Chromia. We'll store the comprehensive movie metadata on-chain and index the embeddings using the vector_db_extension. This powerful setup allows you to perform semantic searches, letting you query by meaning and retrieve detailed results directly from the blockchain.

## Key learning objectives​

- Set up your project environment and run the backend on the Chromia testnet

- Generate and upload vector embeddings along with movie metadata

- Utilize natural language to search for semantically similar movies and obtain detailed results

This hands-on course introduces you to on-chain semantic search using the Carnegie Mellon University (CMU) Movie Summary Corpus and Python-based tools.

### Repository link​

Access the complete code repository for this course here: [Vector DB Movie Demo repository](https://bitbucket.org/chromawallet/vector-db-movie-demo/src/main/).


===== FILE: courses__vector-db-movie-demo__search.md =====

# Search the vector database

URL: https://learn.chromia.com

- [Home](/)
- Module 3 – Search the vector databaseOn this page
# Search the vector database

Now that you have stored your movie data on-chain, including vector embeddings and full metadata, you can perform semantic queries using natural language to retrieve the most relevant matches.

Activate vector_demo_env if it's not already activeIf you're in the rell/ folder:

```bash
cd ../python
```
Or from the root of the project:

```bash
cd python
```
Then activate the environment:

```bash
source vector_demo_env/bin/activate
```

## Run the script​

Navigate to the python/ folder and run the following command:

```bash
python search_movies.py
```

When prompted, enter a search query. For example:

```text
A man wakes up in a world controlled by machines.
```

The script will:

- Embed your query using the model specified in your .env file

- Search the on-chain vector index for the closest matching embeddings

- Retrieve the top matching movies along with their metadata

## Filter your search by genre (optional)​

You can also use a version of the script that allows you to filter results by genre:

```bash
python search_movies_filtered.py
```

This script functions like the standard search but adds an extra prompt for genre filtering before executing the query.

For example:

- Query: A robot gains self-awareness

- Genre: Science Fiction

The script returns only semantically relevant matches within that genre.

## What’s included in the results?​

Each result contains:

- Title

- Release date

- Plot summary

- Genres, languages, and countries

- Similarity score (distance — lower = more similar)

The script calculates similarity based on vector distance, ensuring that the matches reflect meaning rather than just wording.

## Try your own queries​

Here are some sample prompts you can try:

- A woman leads a rebellion against a dystopian regime

- Movies about AI taking control of humanity

- Stories involving time travel and paradoxes

- A team of explorers enters another dimension

You can run these queries in the standard script or combine them with a genre filter using search_movies_filtered.py.

## What’s next?​

In the next module, you will explore how the system operates behind the scenes, including the storage, querying, and linking of vectors to movie metadata using Rell.


===== FILE: courses__vector-db-movie-demo__setup.md =====

# Module 1 – Set up your project

URL: https://learn.chromia.com

- [Home](/)
- Module 1 – Set up your project
# Module 1 – Set up your project

In this first module, you will configure everything needed to prepare your environment for the Vector DB Movie Demo.

You will select a sentence embedding model, set the correct dimensionality for the vector_db_extension, configure your Rell backend, and deploy it to the Chromia testnet. Finally, you will set up your Python environment.

By the end of this module, your backend will be deployed and ready, and your Python scripts will be fully configured, enabling you to start working with data in the next module.

## Lessons
[Lesson 1 – Embedding model](/courses/vector-db-movie-demo/setup/embedding-model)[Lesson 2 – Configure your Rell module](/courses/vector-db-movie-demo/setup/configure.rell)[Lesson 3 – Deploy your Rell module](/courses/vector-db-movie-demo/setup/deploy-rell-module)[Lesson 4 – Finalize your Python environment](/courses/vector-db-movie-demo/setup/finalize-python-env)[Start module »](/courses/vector-db-movie-demo/setup/embedding-model)


===== FILE: courses__vector-db-movie-demo__setup__configure.rell.md =====

# Configure your Rell module

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 – Set up your project](/courses/vector-db-movie-demo/setup/)
- Lesson 2 – Configure your Rell moduleOn this page
# Configure your Rell module

In this lesson, you will configure your chromia.yml file with the correct dimensionality for the Vector Database Extension and prepare it for deployment to the Chromia testnet.

## Open the rell/ folder​

If you’re still inside the python/ folder from the previous step, navigate back to the project root:

```bash
cd ..
```

Next, move into the Rell backend folder:

```bash
cd rell
```

## Set the correct embedding dimensions​

Open the chromia.yml file and find the following section:

rell/chromia.yml
```yaml
blockchains:  vector_demo:  # Update this to a unique name before deployment    module: vector_interface    config:      gtx:        modules:          - "net.postchain.gtx.extensions.vectordb.VectorDbGTXModule"      vector_db_extension:        dimensions: 768  # Match your selected embedding model
```

Update the dimensions: value to match the embedding model you selected earlier:

| 
| Model| Dimensions
| MiniLM| 384
| mpnet-base| 768
| mpnet-large / e5 / bge| 1024
noteEnsure this number matches the vector size produced by your selected model exactly.

A mismatch will lead to failed vector uploads or queries.

## Set a unique chain name​

Chain name must be globally unique on testnetUpdate the following line in chromia.yml to a unique name. This name will serve as the public identifier of your chain on the testnet:

```yaml
blockchains:  vector_demo:  # Update this to a unique name before deployment
```
For example:

```yaml
blockchains:  vector_demo_42:
```
If another user has already deployed a chain with the same name, your deployment will not succeed. So, be sure to choose a name that is unique across the testnet.

## Generate a deployment key​

To sign the deployment transaction, you’ll need a deployment key. If you haven’t generated one yet, run the following command:

```bash
chr keygen --key-id="vector_db_demo_key"
```

This will create a new key pair saved in your local .chromia directory.

After generating the key, you will find your private key saved at:

```bash
~/.chromia/vector_db_demo_key
```

Open this file, copy the full hex-encoded private key, and paste it into the PRIV_KEY field in your Python .env file, located in the python/ folder:

```env
PRIV_KEY=
```

This key is essential for signing transactions when you upload or query vectors on-chain.

## Lease a container on testnet​

Before you deploy, lease a container that supports the Vector Database Extension.

Follow this guide to lease a container and obtain your container ID:

👉 [Get a container on Chromia testnet](https://docs.chromia.com/intro/getting-started/testnet/get-container)

Using your public keyDuring the leasing process, you will need to provide a public key. Use the public key generated earlier with chr keygen.

You will see this public key printed in your terminal upon key creation, and it’s also saved in the following file:

```bash
~/.chromia/.pubkey
```

Once you’ve leased your container, paste the container ID into the chromia.yml file as shown:

```yaml
deployments:  testnet:    brid: x"6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92"    url: https://node0.testnet.chromia.com:7740    container:     # After deployment, insert the generated `chains:` section here
```

Leave the brid and url as they are; they point to the testnet Directory Chain and node0.

## What’s next?​

After configuring your chromia.yml file with the correct container ID, embedding dimensions, and a unique chain name, along with generating your deployment key, you’re ready to deploy the Rell module.

We will cover that in the next lesson.


===== FILE: courses__vector-db-movie-demo__setup__deploy-rell-module.md =====

# Deploy your Rell module

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 – Set up your project](/courses/vector-db-movie-demo/setup/)
- Lesson 3 – Deploy your Rell moduleOn this page
# Deploy your Rell module

With your chromia.yml configured, your container leased, and a deployment key created, you are now prepared to deploy the Rell module to the Chromia testnet.

## Install and build​

Navigate to the rell/ folder and run the following commands:

```bash
chr install      # Installs the vector_db library declared in chromia.ymlchr build        # Compiles your Rell module and validates the syntax
```

These commands install all necessary dependencies and compile your module before deployment.

## Deploy the module​

Execute the following command to deploy your module:

```bash
chr deployment create --settings chromia.yml --network testnet --blockchain  --key-id="vector_db_demo_key"
```

- Ensure that <your-chain-name> matches what you specified under blockchains: in your chromia.yml.

- The --key-id must correspond with the key you created earlier (vector_db_demo_key).

## Update chromia.yml​

After deploying, the CLI will output a chains: section. Copy this section directly into your chromia.yml. It will look something like this:

```yaml
deployments:  testnet:    brid: x""    url: https://node0.testnet.chromia.com:7740    container:     # After deployment, insert the generated `chains:` section here    chains:      : x""
```

Paste this chains: block under the testnet deployment entry without making any changes or using placeholders.

infoYou will need the blockchain RID again soon for your Python .env file. Feel free to copy it now or retrieve it later from chromia.yml.

## Verify the setup​

Run the following command to confirm that your configuration is valid:

```bash
chr deployment update --settings chromia.yml --network testnet --blockchain  --key-id="vector_db_demo_key"
```

If everything is set up correctly, the command should succeed even if there are no changes. This verifies that your deployment and configuration are ready for future updates.

## What’s next?​

You have successfully deployed your Rell module and configured your chromia.yml correctly. Now, you are ready to finalize your Python environment.


===== FILE: courses__vector-db-movie-demo__setup__embedding-model.md =====

# Choose an embedding model

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 – Set up your project](/courses/vector-db-movie-demo/setup/)
- Lesson 1 – Embedding modelOn this page
# Choose an embedding model

Before configuring your backend or running any scripts, decide which embedding model you want to use.

The embedding model will convert each movie plot summary into a fixed-size vector. The dimension of that vector depends on your chosen model, and you must configure this number in your backend.

## Clone the repo​

If you haven’t done so already, start by cloning the project repository:

```bash
git clone https://bitbucket.org/chromawallet/vector-db-movie-demo.gitcd vector-db-movie-demo
```

## Create your .env file​

Navigate to the python/ folder and create your .env file by copying the template:

```bash
cd pythoncp .env.template .env
```

You will edit this file in the next step.

## Select your model​

Open the .env file in your editor and pick one of the available EMBEDDING_MODEL options by uncommenting the relevant line.

For example:

```env
EMBEDDING_MODEL=sentence-transformers/all-mpnet-base-v2
```

This field controls which sentence embedding model will be used when vectorizing movie summaries.

noteMake sure to note the dimensions of the model you choose, as you'll need it in the next lesson to set up your Rell module.

Feel free to substitute any other model from the Hugging Face Hub, provided it's compatible with the sentence-transformers library and produces dense vectors.

## Model overview​

| 
| Model| Dimensions| Notes
| all-MiniLM-L6-v2| 384| Small and fast
| all-mpnet-base-v2| 768| Balanced, a good default choice
| all-mpnet-large-v2| 1024| High quality but slower
| intfloat/e5-large-v2| 1024| Instruction-tuned, best for search
| BAAI/bge-large-en-v1.5| 1024| Alternative model with excellent performance

## Choosing the right model​

Larger models can give better semantic results but require more memory and processing time.

If you're using a GPU, you can likely use any of the models listed above without issues.

If you're running on CPU, you may want to stick to a smaller model to avoid slow performance.

Next, you’ll use the model’s dimensions to configure your Rell module in chromia.yml.


===== FILE: courses__vector-db-movie-demo__setup__finalize-python-env.md =====

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


===== FILE: courses__vector-db-movie-demo__use-cases.md =====

# Use cases and extensions

URL: https://learn.chromia.com

- [Home](/)
- Module 5 – Use casesOn this page
# Use cases and extensions

You have built a powerful semantic search system using Chromia’s Vector Database Extension.

Your movie embeddings are stored on-chain, and your Python client performs real-time semantic queries against them.

Now, let’s explore how to take this foundation further.

## Real-world applications​

This vector search pipeline works seamlessly with any type of content, not just movies.

You can integrate:

- Product descriptions for semantic ecommerce search

- News articles for on-chain curation and retrieval

- Support tickets for automated help agents

- User posts or comments for recommendation engines

- Knowledge bases for RAG-powered chatbots

All of these are backed by Chromia’s decentralized data layer and are searchable via vector embeddings.

## Retrieval-augmented generation (RAG)​

The typical RAG workflow includes the following steps:

- A user submits a query.

- The system embeds the query.

- A vector database returns relevant results.

- A language model uses the results for context.

You have already implemented steps 1–3 on Chromia.

Now, complete the loop by connecting your semantic search results to a language model — for instance, in a chat interface.

The model can receive relevant matches as context and generate responses based on them.

The [GOAT SDK course](/courses/chromia-goat-chat-agent/introduction) showcases a chat agent that interacts with Chromia, including tools, queries, and blockchain calls.

You can adopt a similar approach by:

- Embedding the user prompt

- Executing a vector query using the Python client

- Feeding the result back into the agent as contextual input

This enhances your agent by making it retrieval-augmented, powered by fully on-chain data and semantic understanding.

## 🎉 Congratulations​

You have successfully built a decentralized semantic search system on Chromia.

From transforming raw text into on-chain vectors to enabling real-time querying, you now possess a complete and extensible pipeline, ready for real-world applications.

## What’s next?​

With your pipeline established, you are prepared to:

- Explore different models or embedding strategies

- Add a chat or web interface

- Combine with other on-chain logic or user actions

- Apply the same semantic search structure across various domains

You’ve already laid the groundwork — everything else is an exciting extension!
