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
