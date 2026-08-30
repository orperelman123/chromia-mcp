# Claude Code with Chromia development tools and MCP

This is a Docker image with [Claude Code](https://www.anthropic.com/claude-code) packaged with 
Chromia development tools and MCP to enable AI powered development of Chromia dapps with Rell.

You need a regular subscription from Anthropic to use it, Chromia tools does not cost anything extra.

## Installation

Place the script `scripts/${YOUR_OS}/claude` somewhere in your `PATH`.

## Setup

Run this command once to install Chromia MCP server in Claude Code:

```bash
claude mcp add chromia --scope user -- java -jar /opt/chromia/chromia-mcp-server.jar
```

## Usage

Run `claude` in your terminal in the project directory.
