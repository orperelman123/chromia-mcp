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
