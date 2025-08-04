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

## Networks

The server supports multiple Chromia networks:
- **Mainnet** - Production network
- **Testnet** - Testing network

Specify the network parameter in your queries to target the appropriate environment.

# TODO:
- [ ] Use Jdeploy to publish this application on NPM
- [x] Remove mcpdoc and add tooling to the main app instead
- [ ] Implement server-sent events (SSE) transport for the MCP server"
