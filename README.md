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

## Installation

### Prerequisites

- Java 21

> TODO: publish chromia-mcp-server on brew and scoop for Windows and MacOS

### Quick Start

The MCP server runs automatically when configured in your AI assistant. No manual installation is required as `brew` will handle package management.

### Enhancing AI agents for Documentation

For enhanced documentation and context support, install uvx: https://docs.astral.sh/uv/getting-started/installation/

**mcpdoc** provides additional documentation context and helps with:
- Enhanced code documentation for Chromia-related tools and infrastructure
- Context-aware suggestions

After installation, mcpdoc will automatically enhance Cursor/Windsurf context to be aware of Chromia documentation

### Cursor/Windsurf IDEs

1. Create or edit `.<cursor|windsurf>/mcp.json` in your project root

### Claude Desktop

1. Edit your Claude Desktop configuration file:
   - **macOS**: `~/Library/Application Support/Claude/claude_desktop_config.json`
   - **Windows**: `%APPDATA%/Claude/claude_desktop_config.json`

```json
{
   "mcpServers": {
      "chromia-mcp": {
         "command": "java",
         "args": [
            "-jar",
            "/Users/bart/Documents/Projects/Work/mcp/chromia-mcp/app/build/libs/app-all.jar"
         ],
         "env": {
            "CHROMIA_MCP_LOG_LEVEL": "INFO"
         }
      },
      "chromia-docs-mcp": {
         "command": "uvx",
         "args": [
            "--from",
            "mcpdoc",
            "mcpdoc",
            "--urls",
            "ChromiaCli(chr):https://gitlab.com/chromaway/core-tools/chromia_mcp/-/raw/main/docs-llm/llm_files/cli/llms-full.txt",
            "ChromiaCliUsage:https://gitlab.com/chromaway/core-tools/chromia_mcp/-/raw/main/docs-llm/llm_files/cli_usage/llms-full.txt",
            "ChromiaCodeSamplesAndCourses:https://gitlab.com/chromaway/core-tools/chromia_mcp/-/raw/main/docs-llm/llm_files/code_samples/llms-full.txt",
            "Rell:https://gitlab.com/chromaway/core-tools/chromia_mcp/-/raw/main/docs-llm/llm_files/rell/llms-full.txt",
            "FT4Library:https://gitlab.com/chromaway/core-tools/chromia_mcp/-/raw/main/docs-llm/llm_files/ft4/llms-full.txt",
            "ChromiaPlatformNetworkConfig:https://gitlab.com/chromaway/core-tools/chromia_mcp/-/raw/main/docs-llm/llm_files/network_config/llms-full.txt",
            "--transport",
            "stdio"
         ],
         "autoApprove": ["list_doc_sources", "fetch_docs"]
      }
   }
}
```

1. Restart AI agent
2. The MCP server will be available in the MCP panel


## Usage Examples

### Basic Network Statistics

```
Get network statistics for Mainnet
```

```     
What is the brid of my neighbor alice, and in which cluster and container it is deployed on
```

```
Find all assets containing "CHR" on Mainnet
```

```
Get the latest 10 transactions on Mainnet with operation type "transfer"
```

```
Get analytics for blockchain RID: ABC123...
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
Could you create a Rell application for managing books that includes FT4 account registration functionality?
```

```
how to use Chromia CLI to deploy my newly created dapp on testnet
```
## Networks

The server supports multiple Chromia networks:
- **Mainnet** - Production network
- **Testnet** - Testing network

Specify the network parameter in your queries to target the appropriate environment.

# TODO:
- [ ] Use Jdeploy to publish this application on NPM
- [ ] Remove mcpdoc and add tooling to the main app instead
- [ ] Implement server-sent events (SSE) transport for the MCP server"
