# COURSE chat-agent-course — 5 pages


===== FILE: courses__chat-agent-course__configure-api-key.md =====

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


===== FILE: courses__chat-agent-course__explore-and-extend.md =====

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


===== FILE: courses__chat-agent-course__introduction.md =====

# Create your chat agent with Chromia

URL: https://learn.chromia.com

- [Home](/)
- Course overviewOn this page
# Create your chat agent with Chromia

This course equips you with boilerplate code to build a minimalistic AI-powered chat agent on Chromia. The goal is to give you a solid foundation that you can tinker with, allowing you to experiment with different agent strategies, memory management, and AI model integrations.

# What is Chromia?

Chromia is a Relational Blockchain that combines the capabilities of relational databases with blockchain technology, streamlining the process of building decentralized applications (dapps). With Chromia, you can develop dapps in a familiar way, regardless of whether your background is in enterprise or gaming.

A standout feature of Chromia is Rell, a powerful and concise blockchain and database language. Rell simplifies dapp development by enabling you to create efficient, secure, and expressive applications with minimal code while retaining the flexibility of relational databases.

To explore Chromia and its features, check out the [Chromia overview](https://docs.chromia.com).

### About the dapp​

In this course, you will start with a boilerplate chat agent powered by an AI model. This agent is designed to store and retrieve context using short-term and long-term memory, providing dynamic, context-aware conversations.

You'll have the opportunity to experiment with AI strategies, fine-tune memory management, and switch between various AI models (like Groq and OpenAI) to customize the agent's behavior.

Below is a demonstration of the chat agent in action:

### What will I learn?​

By completing this course, you’ll gain practical experience in:

- Setting up and running a Chromia-based dapp.

- Exploring memory management strategies to enhance contextual accuracy.

- Integrating multiple AI models and customizing agent responses.

- Using Rell for backend operations, including queries and transactions.

- Adapting the chat agent to suit unique requirements or projects.

The result will be a fully functional boilerplate you can extend and modify to create a personalized, interactive chatbot dapp.

## Repository link​

Find the complete code repository for this course here:
[Chat agent course repository](https://bitbucket.org/chromawallet/chat-agent-course).


===== FILE: courses__chat-agent-course__setup.md =====

# Set up your project

URL: https://learn.chromia.com

- [Home](/)
- Set up your projectOn this page
# Set up your project

To get started, ensure the following prerequisites are installed:

Set up PostgreSQL database
# Set up PostgreSQL database

Rell requires PostgreSQL 16.3. The IDE can work without it but can't run a node. A console or a remote postchain app can
run without a database.

The default database configuration for Rell is:

- database: postchain

- user: postchain

- password: postchain

## Install​

- Mac
- Linux
- Docker
- Windows

- 
Install Homebrew: [Homebrew installation guide](https://brew.sh/)

- 
Install PostgreSQL:

```shell
brew install postgresql@16brew services start postgresql@16createuser -s postgres
```

- 
Prepare the PostgreSQL database:

```plsql
psql -U postgres -c "CREATE DATABASE postchain WITH TEMPLATE = template0 LC_COLLATE = 'C.UTF-8' LC_CTYPE = 'C.UTF-8' ENCODING 'UTF-8';" -c "CREATE ROLE postchain LOGIN ENCRYPTED PASSWORD 'postchain'; GRANT ALL ON DATABASE postchain TO postchain;"
```

noteIf you get an error saying peer authentication failed, you must change the authentication method from peer to
md5. You can change it in the pg_hba.conf file of your psql database.

- 
Install PostgreSQL:

```bash
sudo apt install postgresql-16
```

- 
Prepare the PostgreSQL database:

```bash
sudo -u postgres psql -c "CREATE DATABASE postchain WITH TEMPLATE = template0 LC_COLLATE = 'C.UTF-8' LC_CTYPE = 'C.UTF-8' ENCODING 'UTF-8';" -c "CREATE ROLE postchain LOGIN ENCRYPTED PASSWORD 'postchain'; GRANT ALL ON DATABASE postchain TO postchain;"
```

- 
Install Docker: [Docker installation guide](https://docs.docker.com/engine/install/)

- 
Prepare the PostgreSQL database:

```dockerfile
docker run --name postgres -e POSTGRES_USER=postchain -e POSTGRES_PASSWORD=postchain -p 5432:5432 -d postgres:16.3-alpine3.20
```

noteWe use the Alpine version of PostgreSQL because it provides the correct collation settings by default. This can be
explicitly set using the environment variable:

```bash
POSTGRES_INITDB_ARGS="--lc-collate=C.UTF-8 --lc-ctype=C.UTF-8 --encoding=UTF-8"
```

- 
Download the PostgreSQL installer from the [official website](https://www.postgresql.org/download/windows/).

- 
Install the executable and add the PostgreSQL folder containing the binaries to your environment variables. Open the
Command Prompt (CMD) and run the following command, ensuring that you set the path to your binaries correctly and
replace the <version> placeholder with the actual version number:

```shell
setx POSTGRESQL "C:\Program Files\PostgreSQL\\bin"
```

- 
Reopen the Command Prompt (CMD) to update the environment variables. Prepare the PostgreSQL database by running the
following two commands sequentially:

```plsql
psql -U postgres -c "CREATE DATABASE postchain WITH TEMPLATE = template0 LC_COLLATE = 'en_US.UTF-8' LC_CTYPE = 'en_US.UTF-8' ENCODING 'UTF-8';"psql -U postgres -c "CREATE ROLE postchain LOGIN ENCRYPTED PASSWORD 'postchain'; GRANT ALL ON DATABASE postchain TO postchain;"
```

noteDepending on your Windows version, you may encounter various errors related to permissions or incorrect PostgreSQL
installations. If this happens, we recommend installing Docker and deploying the PostgreSQL container.

Install Chromia CLI
# Install Chromia CLI

This topic contains instructions to install and update the
[Chromia CLI](https://gitlab.com/chromaway/core-tools/chromia-cli).

## Prerequisite​

Before proceeding, make sure the following prerequisites are met:

- PostgreSQL database: See [Set up PostgreSQL database](/docs/install/database-setup).

- RELL_JAVA environment: Chromia CLI do requries a java runtime (version 21 or later) to execute. Through the
different package managers this has been abstracted away for you so you don't need to set this up. If you want to
control which java runtime you use to execute Chromia CLI with it is recomended to set RELL_JAVA variable in your
environment to point to a valid Java installation

## Installation​

You can install Chromia CLI using a package manager or by downloading it directly from
[Chromia CLI Packages](https://gitlab.com/chromaway/core-tools/chromia-cli/-/packages).

- macOS
- Linux/WSL
- WindowsTo install Chromia CLI (chr) on macOS, follow these steps:

- 
If Homebrew is not installed, install it by running:

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
```

- 
Add the Chromia repository to Homebrew by running the following command:

```bash
brew tap chromia/core https://gitlab.com/chromaway/core-tools/homebrew-chromia.git
```

- 
Install Chromia CLI with:

```bash
brew install chromia/core/chr
```

infoTo install a specific version of Chromia CLI, use the following commands:

```shell
brew install chromia/core/chr@brew unlink chrbrew link chr@
```
You can list available versions using: brew search chr.

- 
To verify the installation, check the version by running:

```bash
chr --version
```

To install Chromia CLI (chr) on Linux or WSL (Windows Subsystem for Linux), follow these steps:

- 
Download and add Chromia's apt-repo public key to your system’s trusted keyrings:

```bash
curl -fsSL https://apt.chromia.com/chromia.gpg | sudo tee /usr/share/keyrings/chromia.gpg
```

- 
Add the Chromia repository to your list of package sources:

```bash
echo "deb [arch=amd64 signed-by=/usr/share/keyrings/chromia.gpg] https://apt.chromia.com stable main" | sudo tee /etc/apt/sources.list.d/chromia.list
```

- 
Run the following command to update your package sources:

```bash
sudo apt-get update
```

infoIf you added apt.chromia.com before Chromia CLI version 0.16.0, run the following command to allow the repository
update:

```bash
sudo apt-get --allow-releaseinfo-change update
```

- 
Once the repository is updated, install Chromia CLI by running:

```bash
sudo apt-get install chr
```

- 
To verify that Chromia CLI is installed successfully, check the version:

```bash
chr --version
```

To install Chromia CLI (chr) on Windows using [Scoop](https://scoop.sh/), follow these steps:

- 
If Scoop is not installed, install it by running the following command in PowerShell (run as Administrator):

```powershell
iwr -useb get.scoop.sh | iex
```

- 
Add the Chromia repository (bucket) to Scoop by running:

```powershell
scoop bucket add chromia https://gitlab.com/chromaway/core-tools/scoop-chromia/
```

- 
Add the Java bucket to Scoop by running:

```powershell
scoop bucket add java
```

This will enable scoop to download the openjdk21 which chromia-cli depends on when installing

- 
Install Chromia CLI by running:

```powershell
scoop install chr
```

- 
To verify that Chromia CLI is installed successfully, check the version:

```powershell
chr --version
```

## Updating Chromia CLI​

You can download and install the latest Chromia CLI from
[here](https://gitlab.com/chromaway/core-tools/chromia-cli/-/packages), or if you have installed the Chromia CLI via a
package manager, you can update it with the following:

- macOS
- Linux
- Windows
```shell
brew updatebrew upgrade chr
```

```shell
sudo apt-get updatesudo apt-get install chr
```

```shell
scoop updatescoop update chr
```

## Docker​

Docker can run a standalone Linux container with the Chromia CLI pre-installed. Make sure that you have set up the
[PostgreSQL database](/docs/install/database-setup).

To use the published Docker images, you must first have Docker installed and configured on your host machine. Please
refer to the Docker documentation on how to [install Docker](https://docs.docker.com/get-docker/) on Windows, Mac, and
Linux.

### Start the Docker container with Chromia CLI pre-installed​

To run the latest version of the Chromia CLI, use the docker run command and specify the CLI Docker image name and
chr.

```shell
docker run --rm -v $(pwd):/usr/app registry.gitlab.com/chromaway/core-tools/chromia-cli/chr: chr
```

Windows Command Prompt UsersIf you're using Windows Command Prompt (cmd), the $(pwd) syntax will not work. Use one of these alternatives:

- PowerShell: Use $(Get-Location) or $(pwd)

- Command Prompt: Use %CD%
Example for Command Prompt:

```cmd
docker run --rm -v %CD%:/usr/app registry.gitlab.com/chromaway/core-tools/chromia-cli/chr: chr
```

noteMake sure to configure your chromia.yml file correctly:

- Mac: Use host.docker.internal for database:host.

- Windows: Set database:host to 172.17.0.1.

- Linux: Use the --network=host argument in Docker commands.
These configurations are crucial to ensure connectivity between Chromia CLI and the PostgreSQL instance.

See the [Docker command line reference](https://docs.docker.com/engine/reference/commandline/docker/) for more
information on updating or uninstalling the Docker image.

```bash
#!/bin/bash# Allocate a pseudo-TTY one when run in interactive modeif [ -t 0 ] && [ -t 1 ] ; then TTY="--tty"; else TTY=""; fidocker run \  # Sets the network to host to not need to change the database hostname (linux only)  --network=host \  # Set timezone based on system settings (linux only)  -e TZ=$(cat /etc/timezone) \  # Sets process ownership to current user  --user $(id -u):$(id -g) \  --mount type=bind,source="/etc/passwd",target=/etc/passwd,readonly \  --mount type=bind,source="/etc/group",target=/etc/group,readonly \  # Configures ssh-agent (only needed if chr install is called on non-public repositores)  -e SSH_AUTH_SOCK=$SSH_AUTH_SOCK \  --volume "$SSH_AUTH_SOCK:$SSH_AUTH_SOCK" \  --mount type=bind,source="${HOME}/.ssh",target=${HOME}/.ssh,readonly \  --mount type=bind,source="${HOME}/.config/jgit",target=${HOME}/.config/jgit \  # Mounts current folder into the container (Use `Get-Location` on PowerShell)  --mount type=bind,source="$(pwd)",target=/usr/app \  --interactive ${TTY} \  --rm \  registry.gitlab.com/chromaway/core-tools/chromia-cli/chr:${CHR_VERSION:-latest} chr "$@"
```

Windows Command Prompt UsersThis bash script uses $(pwd) which won't work in Windows Command Prompt. For Windows users:

- PowerShell: The script should work as-is

- Command Prompt: Replace $(pwd) with %CD% in the mount command
Modified mount line for Command Prompt:

```cmd
--mount type=bind,source="%CD%",target=/usr/app \
```

## Clone the repository​

Clone the course repository:

```shell
git clone https://bitbucket.org/chromawallet/chat-agent-course.git
```

## Navigate to the project directory​

Enter the project folder:

```shell
cd chat-agent-course
```

## Install the necessary libraries​

```shell
npm install
```

Alternatively, if npm install doesn't work, you can use [Bun](https://bun.sh/):

```shell
bun install
```

If you don't have Bun installed, follow the instructions from [here](https://bun.com/docs/installation/).

Now, you're ready to proceed with the course.


===== FILE: courses__chat-agent-course__test-your-setup.md =====

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
