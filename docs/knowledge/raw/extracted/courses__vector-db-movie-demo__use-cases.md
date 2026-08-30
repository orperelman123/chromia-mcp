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
