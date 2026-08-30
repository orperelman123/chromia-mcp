# Configure your API key

URL: https://learn.chromia.com

- [Home](/)
- Configure your API keyOn this page
# Configure your API key

To enable the chat agent, set up your API key. The project is pre-configured to use xAI, but feel free to switch to Groq or any OpenAI-compatible API.

infoIf the .env file does not exist, create it using the .env.sample file and update it with your chosen API key.

Make sure your key actually worksAll listed providers (xAI/Grok, Groq, etc.) are third-party services outside our control. Availability, pricing, and free-credit policies can change at any time. Your setup will only work if the API key you use is currently active and usable (free or paid).

If the default (xAI/Grok) doesn’t work for you, switch to another supported provider (e.g., Groq) and follow the steps below.

## Default option: xAI​

- 
Go to [xAI API](https://x.ai/api).

- 
Sign in or create an account.

- 
Obtain your API key.

- 
Open the .env file and set the following:

```plaintext
XAI_API_KEY=your_api_key_here
```

After configuration, proceed to [Test your setup](/courses/chat-agent-course/test-your-setup).

## Alternative option: Groq​

- 
Go to [Groq console](https://console.groq.com/keys).

- 
Sign in or create an account.

- 
Obtain your API key.

- 
Open the .env file and set the following:

```plaintext
XAI_API_KEY=your_api_key_here
```

- 
Update the baseURL in /agent/services/openai.ts to:

```typescript
baseURL: "https://api.groq.com/openai/v1",
```

- 
Update the model in the following locations:

- agent/config.yml: Replace the model field with your chosen model, e.g., llama-3.1-8b-instant.

- agent/tools/memory.ts: Replace the model variable with your chosen model.

Example in agent/config.yml:

```yaml
model: "llama-3.1-8b-instant"
```

Example in agent/tools/memory.ts:

```typescript
model: string = "llama-3.1-8b-instant";
```

Check the [Groq documentation](https://console.groq.com/docs/models) for a list of available models.

After updating, proceed to [Test your setup](/courses/chat-agent-course/test-your-setup).

## Using other OpenAI-compatible APIs​

- 
Obtain an API key from the chosen provider.

- 
Open the .env file and set the following:

```plaintext
XAI_API_KEY=your_api_key_here
```

- 
Update the baseURL in /agent/services/openai.ts to match the provider's endpoint.

- 
Update the necessary configuration files, such as agent/config.yml, and modify any hardcoded values, like the model variable in agent/tools/memory.ts, to match the specific model or API requirements.
