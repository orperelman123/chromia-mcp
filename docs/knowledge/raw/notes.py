NOTES = {}

NOTES["vector-db-movie-demo"] = {
  "teaches": "On-chain semantic search: CMU movie plots to sentence-transformers embeddings, store metadata plus vectors via vector_db_extension, query by meaning with optional genre filter.",
  "docs": ["https://docs.chromia.com/clients/postchain-clients/python-client", "https://docs.chromia.com/intro/getting-started/testnet/get-container"],
  "patterns": [
    "Enable VectorDbGTXModule and set vector_db_extension.dimensions to the embedding size (384/768/1024). Mismatch fails upload/query.",
    "Lease a Vector-DB testnet container; unique chain name; persist issued chain RID after chr deployment create. Testnet directory BRID x6F1B061C... node0.testnet.chromia.com:7740.",
    "CONTEXT_MOVIE=0 isolates one vector domain; store_vectors in one batch; link vectors to movie.rowid.",
    "query_closest_objects with query_template get_movies_with_distance or get_movies_with_filter. Distance lower = closer.",
    "Same EMBEDDING_MODEL at index and query. Python BlockchainClient + secp256k1 signed add_movies. Batches of 50, 5 retries exponential backoff.",
    "BOX_OFFICE_THRESHOLD filters dataset size. RAG: embed prompt, vector query, feed hits to an LLM (see GOAT course).",
  ],
}

# next
