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
