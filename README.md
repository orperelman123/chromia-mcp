# Chromia MCP Server

A Model Context Protocol (MCP) server that provides access to Chromia blockchain infrastructure and deployed dApps through the Chromia Explorer GraphQL API.

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

The server includes built-in documentation tools that provide access to comprehensive Chromia documentation:

- **Chromia CLI Documentation** - Complete guide to using the Chromia command-line interface
- **Chromia CLI Usage Examples** - Practical examples and usage patterns for Chromia CLI
- **Rell Programming Language** - Complete documentation for the Rell programming language
- **FT4 Library Documentation** - Documentation for FT4 account management and authentication
- **Code Samples and Courses** - Code examples, tutorials, and learning resources
- **Network Configuration** - Platform network configuration and deployment guides

These tools allow AI assistants to access up-to-date Chromia documentation

## Installation

### Prerequisites

- Node.js v18 or higher, *or*
- Java Development Kit v21 or higher

### With Node.js

Install the chromia-mcp-server globally using npm:

```bash
npm install @chromia/chromia-mcp-server -g
```

### With JDK

Build it with Gradle:

```bash
./gradlew :app:shadowJar
```

## Setup

The MCP server runs automatically when configured in your AI assistant.

### Cursor/Windsurf IDEs

1. Create or edit `.<cursor|windsurf>/mcp.json` in your project root

### Claude Desktop

1. Edit your Claude Desktop configuration file:
   - **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
   - **Windows**: `%APPDATA%/Claude/claude_desktop_config.json`

### Jetbrains AI Assistant

1. Go to Settings → Tools → AI Assistant → MCP.
2. Click on `+` to open to add dialog.
3. In the dropdown switch from "Command" to "As JSON" and paste the JSON below.
4. A working directory, put the directory where this project is cloned.

#### With Node.js

```json
{
   "mcpServers": {
      "chromia-mcp": {
         "command": "chromia-mcp-server"
      }
   }
}
```

#### With JDK

```json
{
  "mcpServers": {
    "chromia": {
      "command": "java",
      "args": [
        "-jar",
        "app/build/libs/chromia-mcp-server.jar"
      ]
    }
  }
}
```

1. Restart AI agent
2. The MCP server will be available in the MCP panel


## Usage Examples

### Basic Network Statistics

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

# TODO:
- [ ] Use Jdeploy to publish this application on NPM
- [x] Remove mcpdoc and add tooling to the main app instead
- [ ] Implement server-sent events (SSE) transport for the MCP server"
- [x] Add support for executing queries 
- [ ] Add support for executing transactions 
