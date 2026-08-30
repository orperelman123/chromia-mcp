# Chromia MCP Server

A Model Context Protocol (MCP) server that provides access to Chromia blockchain infrastructure and deployed dApps through the Chromia Explorer GraphQL API.

## Documentation

- [Introduction](./docs/Introduction.md)
- [Architecture](./docs/Architecture.md)
- [Functionality](./docs/Functional.md)
- [Setup & Development](./docs/Setup.md)
- [Deployment](./docs/Deployment.md)

## Overview

The Chromia MCP Server enables AI assistants to query and analyze Chromia blockchain data, including:

- Network statistics and analytics
- Blockchain information and metadata
- Transaction data and analysis
- Asset information and distribution
- Account activity and analytics
- Node performance monitoring
- dApp deployment information
- **Documentation retrieval and search**

## Documentation Tools

The server includes **RAG-powered (Retrieval-Augmented Generation) semantic documentation search** that uses vector embeddings to find relevant documentation based on meaning, not just keywords.
The AI assistant will automatically use semantic search to find and return the most relevant documentation sections.

## Installation

### Prerequisites

- Java Development Kit v21 or higher

#### For local development With JDK

Clone the [chromia-mcp repository](https://gitlab.com/chromaway/core-tools/chromia-mcp):

```bash
git clone https://gitlab.com/chromaway/core-tools/chromia-mcp.git
cd chromia-mcp
```

Run the application using gradle run in sse mode:

```bash
./gradlew :app:runSse
```

This will start the MCP server in SSE mode on `127.0.0.1:3001` by default.

> **Note for local development**: When running locally, configure your MCP client to use `http://127.0.0.1:3001/sse` instead of `https://mcp.chromia.dev/sse`.

## Setup

The MCP server runs automatically when configured in your AI assistant.

### All AI Assistants (Cursor, Claude Desktop, JetBrains AI Assistant)

All AI assistants use the same MCP configuration format. Add the following JSON configuration:

**For production/remote server:**

```json
{
  "mcpServers": {
    "chromia-mcp": {
      "url": "https://mcp.chromia.dev/sse"
    }
  }
}
```

**For local development:**

```json
{
  "mcpServers": {
    "chromia-mcp": {
      "url": "http://127.0.0.1:3001/sse"
    }
  }
}
```

#### Platform-specific setup locations

**Cursor/Windsurf IDEs:**

1. Open Cursor settings and navigate to **MCP & Integration** → **MCP TOOLS**
2. Add the JSON configuration above

**Claude Desktop:**

1. Edit your Claude Desktop configuration file:
   - **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
   - **Windows**: `%APPDATA%/Claude/claude_desktop_config.json`
2. Add the JSON configuration above

**JetBrains AI Assistant:**

1. Go to Settings → Tools → AI Assistant → MCP
2. Click on `+` to open the add dialog
3. In the dropdown switch from "Command" to "As JSON" and paste the JSON configuration above
4. Set the working directory to where this project is cloned

**ChatGPT:**

1. Enable developer mode: Go to Settings → Connectors → Advanced → Developer mode
2. Import Chromia MCP:
   - Open Workspace settings → Connectors → Create
   - Enter the following:
     - MCP Server URL: `https://mcp.chromia.dev/sse` (or `http://127.0.0.1:3001/sse` for local development)
     - Authentication: No authentication
   - Click Create
   - In connector details, new tool lists and descriptions from the MCP server will be shown

> The MCP server will be available in the MCP panel

## Rell LSP Integration

For enhanced Rell code understanding and analysis, you can also install the Chromia LSP MCP server alongside this blockchain MCP server. The LSP MCP server provides Claude with direct access to Rell language server capabilities for better code comprehension and analysis.

### Installation

```bash
npm install @chromia/chromia-lsp-mcp -g
```

### Configuration

Add the LSP MCP server to your MCP configuration

```json
{
  "mcpServers": {
    "lsp-mcp": {
      "command": "npx",
      "args": ["chromia-lsp-mcp", "0.8.8"]
    }
  }
}
```

> **Note**: The second argument `"0.8.8"` is optional and specifies the Rell LSP version to use. If omitted, the latest version will be used.

This combination provides Claude with both blockchain data access and deep Rell code understanding capabilities.

## Usage Examples

### Basic Network Statistics

> **Example Query: What is the BRID of My Neighbor Alice, and in which cluster and container is it deployed?**
>
> <details>
> <summary>Click to expand answer</summary>
>
> Based on the search results, here's the information about My Neighbor Alice blockchains on Chromia mainnet:
>
> **My Neighbor Alice Blockchain Information**
>
> I found **4 My Neighbor Alice blockchains** on mainnet:
>
> **Active/Running Blockchains:**
>
> 1. **Main My Neighbor Alice Blockchain**
>    - **BRID (Blockchain RID)**: `F31D7A38B33D12A5D948EE9CF170983A7CA5EFFFAAA31094C5B9CF94442D9FA2`
>    - **Name**: `my_neighbor_alice`
>    - **Cluster**: `pink`
>    - **Container**: `611062cdf1047b265b652d229858e5a18dbf487afbb62f0f6d30ec4fd81366f9`
>    - **State**: `RUNNING` ✅
> 2. **My Neighbor Alice BS2**
>    - **BRID**: `8BC5E4DAA94410BEC640DAEE87CE5C546465C2E7E8D654668BA6267200C97560`
>    - **Name**: `my_neighbor_alice_bs2`
>    - **Cluster**: `pink`
>    - **Container**: `9f49e091ff1929bc6438a9ee9c5387ec7991eb5eda4154cd7509dd878aa9f70e`
>    - **State**: `RUNNING` ✅
>
> **Removed/Legacy Blockchains:**
>
> 3. **Legacy Instance 1**
>    - **BRID**: `9F0CCB09BBB441E840035086F5326294F51FFA92344D39FFF54B223C7547D81D`
>    - **State**: `REMOVED`
> 4. **Legacy Instance 2**
>    - **BRID**: `AEBD8E8CD9FCC268D7B5AC3DAC8855F1F9A817B8DD1A98C0F13E60D01DECB90C`
>    - **State**: `REMOVED`
>
> **Summary**
>
> The **currently active My Neighbor Alice** deployment is:
>
> - **Primary BRID**: `F31D7A38B33D12A5D948EE9CF170983A7CA5EFFFAAA31094C5B9CF94442D9FA2`
> - **Deployed in**: `pink` cluster
> - **Container ID**: `611062cdf1047b265b652d229858e5a18dbf487afbb62f0f6d30ec4fd81366f9`
>
> Both active blockchains are running in the **pink cluster**, which suggests this is a multi-blockchain deployment for the My Neighbor Alice game ecosystem.
>
> </details>

```
Get network statistics and analytics for the Chromia Mainnet environment
```

```
What is the brid of my neighbor alice, and in which cluster and container it is deployed on
```

```
Get the latest 10 transactions on Mainnet with operation type "transfer"
```

```
Show activity for account ID: DEF456... across all blockchains
```

```
Which account holds most of CHR asset on mainnet
```

```
How many active users does my neighbor Alice's dApp have on mainnet?
```

```
How to register an account using FT4 open strategy in Rell, can you provide TypeScript client code as well
```

```
what is the difference between query and operation in Rell
```

```
Could you create a Rell application for managing books that includes FT4 account registration functionality
```

```
how to use Chromia CLI to deploy my newly created dapp on testnet
```

```
What are the steps to deploy a dApp on Chromia testnet using CLI?
```

```
How do I configure network settings for a Chromia blockchain?
```

### Advanced Blockchain Querying

You can query specific dApps deployed on Chromia networks and execute their custom queries:

```
Can you query all the libraries on library chain dApp on testnet?
```

**Expected workflow:**

1. The AI will search for blockchains with "library chain" on testnet
2. Retrieves the blockchain RID
3. Fetches the dApp structure to discover available queries and operations using `Postchain Client`
4. Executes the `get_all_libraries` query inside Cursor/Junie...
5. Returns the results from the library dApp

**Other examples of blockchain-specific queries:**

```
Get the deployed dApp structure for My Neighbor Alice on mainnet
```

```
Run the get_user_balance query on MNA blockchain for account ABC123
```

```
Can you list all the queries available for My Neighbor Alice on mainnet ?
```

### Rell Query Translation to SQL and vice versa

- Simple SELECT translation:

```
"Can you translate this SQL query to Rell?
SELECT name, genre FROM plays WHERE duration_minutes > 120;"
```

- JOIN queries:

```
How would I write this SQL join in Rell?
SELECT p.name, t.name, b.timestamp
FROM bookings b
JOIN performances p ON b.performance_id = p.id
JOIN theater_halls t ON p.theater_id = t.id
WHERE b.status = 'CONFIRMED';"
```

- Aggregation queries:

```
Convert this SQL aggregation to Rell syntax:
SELECT play_name, COUNT(*) as total_bookings, SUM(price) as revenue
FROM bookings b
JOIN performances p ON b.performance_id = p.id
GROUP BY play_name
HAVING COUNT(*) > 5;"
```

- Subqueries:

```
How do I write this SQL subquery in Rell?
SELECT name FROM plays
WHERE id IN (
    SELECT play_id FROM performances
    WHERE timestamp > NOW()
);"
```

- Complex conditions:

```
Translate this SQL query with multiple conditions to Rell:
SELECT DISTINCT p.name, t.name
FROM plays p
JOIN performances pf ON p.id = pf.play_id
JOIN theater_halls t ON pf.theater_id = t.id
WHERE p.genre = 'DRAMA'
AND pf.timestamp BETWEEN ? AND ?
AND EXISTS (
    SELECT 1 FROM bookings b
    WHERE b.performance_id = pf.id
);"
```

- From Rell to SQL

```
Can you translate this Rell query to SQL?
(b: bookings, p: performances) @* {
    b.performance_id == p.id
} (
    @group play_name = p.play_name,
    total_bookings = @sum 1,
    revenue = @sum b.price
) @* {
    .total_bookings > 5
}
```

When asking it's helpful to:

1. Provide the complete SQL/Rell query with proper formatting
2. Specify any relevant entity structures when working with an external project
3. Mention any special requirements (e.g., sorting, limiting, null handling)
4. Include context about the data model if it's not obvious

Example complete prompt:

```
I have these entities in my Rell code:

entity play {
    name: text;
    genre: text;
    duration: integer;
}

entity performance {
    play: play;
    date: timestamp;
    status: text;
}

Can you help me translate this SQL query to Rell?

SELECT p.name, COUNT(pf.id) as performance_count
FROM plays p
LEFT JOIN performances pf ON p.id = pf.play_id
WHERE p.genre = 'DRAMA'
GROUP BY p.name
HAVING COUNT(pf.id) > 5
ORDER BY performance_count DESC;"
```

This format provides all the necessary context for accurate translation. The AI can understand:

1. The exact data structure
2. The relationships between entities
3. The desired query logic
4. Any special requirements for the output

## Networks

The server supports multiple Chromia networks:

- **Mainnet** - Production network
- **Testnet** - Testing network

Specify the network parameter in your queries to target the appropriate environment.

## Out of scope

This server is a **query / documentation expert**. It does **not** send signed transactions, hold keys, or act as a wallet. Use `chromia_dapp_query` for read-only dApp queries. Transaction *inspection* (`get_all_transactions`) is supported; transaction *execution* is not. There is no OpenAPI spec.

MCP resources are the existing health JSON, `docs-repositories.json`, and `prompt_templates.json` (not a generated library). Prompt templates are the `get_prompts` tool; the server does not advertise MCP `prompts`.

## Local extras

- Stdio mode: `./gradlew :app:run` or `java -jar app/build/libs/chromia-mcp-server.jar --stdio`
- Fat JAR: `./gradlew :app:shadowJar` (do not run `jib` and `shadowJar` as concurrent Gradle tasks; they both write under `app/build/libs`)
- Embeddings refresh (does not run on server boot): `./gradlew :app:generateEmbeddingsNoUpload` persists `embeddings.json` to `app/build/embeddings.json` (`CHROMIA_EMBEDDINGS_PATH`). Runtime `RagStore` loads that local file first (`CHROMIA_EMBEDDINGS_PATH`, else the first existing of `build/embeddings.json` or `app/build/embeddings.json` relative to cwd, so `java -jar app/build/libs/chromia-mcp-server.jar` from the repo root finds the Gradle file) and falls back to the published GitLab package only if the local file is missing. Local no-upload ingest 2026-08-26 19:54 IDT: 3084 documents / 25555 segments with `DocumentSplitters.recursive(1000, 150)` + heading markdown. Persisted `app/build/embeddings.json` (140.66 MiB, 25555 vectors; gitignored under `build/`). Upload still needs `GITLAB_ACCESS_TOKEN`.
