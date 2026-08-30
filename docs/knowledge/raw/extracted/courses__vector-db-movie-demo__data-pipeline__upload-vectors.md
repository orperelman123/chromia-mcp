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
