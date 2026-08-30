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
