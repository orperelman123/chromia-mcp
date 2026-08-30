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
