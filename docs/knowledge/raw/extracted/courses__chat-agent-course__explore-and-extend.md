# Explore and extend

URL: https://learn.chromia.com

- [Home](/)
- Explore and extendOn this page
# Explore and extend

Congratulations on completing the setup and testing your chat agent! It’s time to dive deeper into the code and experiment with its features. This course provides a functional boilerplate to explore memory handling and strategies for enhancing chat interactions.

## Explore the codebase​

The project is structured to give you a solid foundation for experimenting with Chromia-based memory storage and retrieval. Here are some starting points:

### Memory strategies​

- Short-term memory: Examine how recent interactions store and retrieve data. Consider scenarios where you might need to adjust the amount of information in short-term memory.

- Long-term memory: Discover how the system preserves significant memories over sessions. Experiment with the criteria for transferring short-term memories to long-term memory.

### Agent behavior​

- Investigate how the agent generates responses using stored memories. Tweak the way the agent utilizes these memories to enhance context-rich responses.

### Try custom scenarios​

- Modify the prompts or objectives associated with the agent to observe how it affects its behaviour.

- Introduce additional memory fields or logs to capture specific interactions or metadata.

## Suggested experiments​

Here are a few ideas to kickstart your experimentation:

- 
Customize memory limits

Please adjust the number of short-term memories stored and see how it impacts the interaction flow. For instance, consider changing the logic to retain only the last five interactions instead of ten.

- 
Change memory utilization

Experiment with how long-term memories summarize and update. Explore what happens if you increase the update frequency or alter the stored content.

- 
Analyze agent logs

Dive into the LLM_LOG table to evaluate how requests and responses log. Analyze the data to gain insights into optimizing memory retrieval and response generation.

## Take it further​

Feel free to experiment beyond the initial setup:

- Modify the backend: Adjust the database schema or Rell operations to meet specific project needs.

- Integrate new features: Enhance the agent’s capabilities by integrating new APIs or adding tools.

- Refactor memory strategies: Implement advanced memory cleanup, prioritization, or tagging strategies to improve performance.

## Need an overview?​

If you need a refresher on how the project works, check out the [README in the repository](https://bitbucket.org/chromawallet/chat-agent-course/src/main/) for diagrams and a high-level explanation.

This is your playground—experiment, break things, and rebuild! The possibilities are endless when you work with Chromia’s relational blockchain and memory-centric chat agents.
