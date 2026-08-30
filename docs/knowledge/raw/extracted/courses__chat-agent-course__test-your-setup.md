# Test your setup

URL: https://learn.chromia.com

- [Home](/)
- Test your setupOn this page
# Test your setup

Now that you have set up your project, let's test if everything works correctly. Follow these steps:

## Start the Chromia node​

To start the Chromia node, run the following command:

```shell
chr node start
```

Optional: If you want to start with a wiped database, run:

```shell
chr node start --wipe
```

This command resets the database to its initial state, which can be useful during development.

## Start the user interface (Optional)​

In a new terminal, start the user interface to make the chat agent accessible through a browser:

```shell
npm run ui
```

Or if you used Bun in the previous steps:

```shell
bun run ui
```

You can access the UI at:

[http://localhost:1234](http://localhost:1234)

## Start the AI agent​

In another terminal, run the AI agent:

```shell
npm run dev
```

Or if you used Bun in the previous steps:

```shell
bun run dev
```

After the agent starts, the terminal will display a URL similar to this:

```http
http://localhost:1234/?sessionId=
```

Replace <session_id> with the actual session ID displayed in the terminal output. This session ID is unique to your current instance of the chat agent.

## Interact with the chat agent​

Open the URL in your browser to interact with the chat agent. Type messages into the interface and verify the responses to ensure everything works as expected.

## Troubleshooting tips​

- 
Session ID conflicts:

The system automatically stores the session ID in the .env file. If you restart the Chromia node, the stored session ID may become invalid. To fix this, open the .env file and delete the line containing the SESSION_ID. A new session ID will be generated the next time you run the AI agent.

- 
Chat agent doesn't respond:

Make sure:

- The Chromia node is running.

- The .env file is correctly configured with a valid API key.

- You installed the dependencies correctly using bun install.

- 
Checking logs:

If issues persist, consult the console logs in each terminal for detailed error messages. These logs can help you identify the problem.
