# COURSE monetize-dapp — 5 pages


===== FILE: courses__monetize-dapp__account-registration.md =====

# Account registration

URL: https://learn.chromia.com

- [Home](/)
- Account registration
# Account registration

When users interact with a dapp, they typically need to create a new account. The account registration process relies on the [Chromia FT4 library](https://docs.chromia.com/ft4/intro) (Flexible Token 4), which sets a standard for managing accounts and tokens on Chromia.

There are three types of account registration: Custom, Open, and Transfer.

- Custom: In the custom strategy, developers need to create their own registration logic using the create_account_with_auth function.

- Open: The open strategy is often used in development environments as it allows swift account registration without fees, but it should be used cautiously in production environments to avoid potential exploitation for spamming the network. The register_account function is used for this strategy.

- Transfer: Transfer strategies include transfer_subscription, transfer_fee, and transfer_open, which provide templates for developers to configure their dapp. These strategies use the transfer function to transfer tokens to a new account, paying a fee and transferring a designated amount of tokens to initiate the account registration process. If the non-activated account remains unclaimed for a specific period, the sender can retrieve the tokens. It's important to note that the transfer_open strategy does not require paying a fee for account registration.


===== FILE: courses__monetize-dapp__introduction.md =====

# Monetize your dapp

URL: https://learn.chromia.com

- [Home](/)
- Course overviewOn this page
# Monetize your dapp

This comprehensive course will guide you in monetizing dapps on the Chromia platform, transforming your blockchain innovation into sustainable revenue streams.

The unique monetization strategies provided in this course will allow you to integrate them quickly, test your application, and improve it for further deployment.

Whether you're a seasoned blockchain developer or just starting, this course equips you with the strategies and tools to make your dapp functional and financially thriving.

Ready to put theory into practice? Let's dive into practical monetization strategies for your dapp, providing clear examples to guide you in choosing the best monetization approach.

## Repository link​

Find the complete code repository for this course here:
[Course repository](https://bitbucket.org/chromawallet/fee-samples/src/main/).


===== FILE: courses__monetize-dapp__open.md =====

# Open strategy

URL: https://learn.chromia.com

- [Home](/)
- Lesson 4 - Open strategyOn this page
# Open strategy

The open strategy allows completely free account creation without any token transfers. Users can register accounts directly without any prerequisites using the register_account() operation.

This strategy provides the simplest approach to account registration, requiring no token transfers or fees for account activation.

Production limitationsThe open strategy is prone to abuse and is not suitable for long-term production use. It allows unlimited account creation without any barriers, making it vulnerable to spam attacks.

Recommended use: Development and testing phases only.

To use safely in production:

- Implement additional rate limiting or validation mechanisms

- Monitor for potential spam patterns

- Consider implementing user verification systems

### Code example for open strategy​

To develop your dapp from the template, refer to the [code examples](https://bitbucket.org/chromawallet/fee-samples/src/main/open/) and corresponding tests. Refer to the [tests](https://bitbucket.org/chromawallet/fee-samples/src/main/open/rell/src/test/) for testing the open strategy implementation.

No configuration neededThe open strategy requires no additional configuration in chromia.yml. The FT4 library handles the open account creation automatically without any special setup. For the complete chromia.yml configuration including blockchain setup, FT4 library configuration, and other required settings, refer to the [actual configuration file](https://bitbucket.org/chromawallet/fee-samples/src/main/open/rell/chromia.yml) in the repository.


===== FILE: courses__monetize-dapp__setup.md =====

# Set up your project

URL: https://learn.chromia.com

- [Home](/)
- Set up your projectOn this page
# Set up your project

Before we start, please make sure you have the following prerequisites in place:

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

We will start by setting up a new project using Chromia CLI.

```shell
chr create-rell-dapp fee_strategies --template=plain-multi
```

```shell
cd fee_strategies
```

Then, we create a new admin keypair:

```shell
chr keygen --file .chromia/admin-keypair
```

To finalize the setup, we add the FT4 library to our project. Open your chromia.yml file and add ft4 to a new libs
section and configure the public key as the admin key for our dapp:

chromia.yml
```yaml
blockchains:  fee_strategies:    module: main    moduleArgs:      lib.ft4.core.admin:        admin_pubkey: x"" # Replace with previously generated public key here.compile:  rellVersion: 0.14.9database:  schema: schema_fee_strategiestest:  modules:    - fee_strategies_test  moduleArgs:    lib.ft4.core.admin:        admin_pubkey: x"" # Replace with previously generated public key here.libs:  ft4:    registry: https://gitlab.com/chromaway/ft4-lib.git    path: rell/src/lib/ft4    tagOrBranch: v1.1.0r    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"    insecure: false  iccf:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/lib/iccf    tagOrBranch: 1.87.0    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"    insecure: false
```

To install FT4, we run:

```shell
chr install
```

That's the basic setup for using FT4. In the upcoming lessons, we will use this to create assets, transfer tokens, and
register user accounts.


===== FILE: courses__monetize-dapp__transfer.md =====

# Transfer strategies

URL: https://learn.chromia.com

- [Home](/)
- Lesson 5 - Transfer strategiesOn this page
# Transfer strategies

FT4 provides three transfer strategies for account registration that require users to transfer tokens: transfer open, transfer fee, and transfer subscription. These strategies offer different monetization approaches while requiring token transfers for account activation.

## Transfer open strategy​

The transfer open strategy requires users to transfer a specific amount of tokens to a non-existent account, which they must then claim to activate it. The non-existent account represents an empty account that needs to be funded with tokens. Once activated, users can utilize the tokens sent to the account.

This strategy provides a middle ground between completely free account creation and paid strategies, requiring token transfer but no fees.

warningImportant considerations for production use:

The transfer open strategy can be used in production, but it comes with spam risks since there are no transfer fees (neither local nor cross-chain). Without proper safeguards, users could potentially create thousands of accounts by transferring tokens back and forth.

To use safely in production:

- Consider limiting account creation to same-address transfers (sender ID = recipient ID)

- Implement additional rate limiting or validation mechanisms

- Monitor for potential spam patterns

### Code example for transfer open strategy​

To develop your dapp from the template, refer to the [code examples](https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_open/) and corresponding tests.

The following configuration defines the account registration process and should be added to the chromia.yml file:

```rell
lib.ft4.core.accounts.strategies.transfer:  rules:    - sender_blockchain: x"0000000000000000000000000000000000000000000000000000000000000000"    sender: "*"    recipient: "*"    asset:        - name: "MyTestAsset"        min_amount: 100L    timeout_days: 60    strategy:    - "open"
```

noteFor the complete chromia.yml configuration including blockchain setup, FT4 library configuration, and other required settings, refer to the [actual configuration file](https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_open/rell/chromia.yml) in the repository.

## Transfer fee strategy​

The transfer fee strategy simplifies the process for users by requiring a one-time purchase of a specific amount of tokens to access your dapp's features. This system is ideal for users who prefer a clear and easy-to-follow process.

When implementing the transfer fee strategy for account registration, users need to transfer a specific amount of tokens to a non-existent account. Once the non-existent account receives the tokens, the fee is deducted from the transferred amount. The non-existent account represents an empty account that needs to be topped up with tokens.

### Code example for transfer fee strategy​

To build your dapp from the template, refer to the [code examples](https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_fee/) and corresponding tests.

You must add the following example configuration to your chromia.yml file:

```rell
lib.ft4.core.accounts.strategies.transfer:  rules:    - sender_blockchain: x"0000000000000000000000000000000000000000000000000000000000000000"    sender: "*"    recipient: "*"    asset:        - name: "MyTestAsset"        min_amount: 100L    timeout_days: 60    strategy:    - "fee"lib.ft4.core.accounts.strategies.transfer.fee:  asset:    - name: "MyTestAsset" # issued by current blockchain    amount: 40L  fee_account: x"YOUR_FEE_ACCOUNT_ADDRESS" # All fees will be collected into this account
```

noteFor the complete chromia.yml configuration including blockchain setup, FT4 library configuration, and other required settings, refer to the [actual configuration file](https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_fee/rell/chromia.yml) in the repository.

## Transfer subscription strategy​

When implementing a transfer subscription strategy for your dapp, users will need to pay regularly to access its functionality. This approach helps generate a steady income to sustain and grow your dapp.

To set up the transfer subscription strategy, you'll need to go through several technical steps. First, configure the chromia.yml file to enable subscriptions. Then, users must call the transfer function, pay a predefined fee, and transfer a specific amount of tokens to a non-existent account. This non-existent account represents an empty account that needs to be filled with tokens and is a crucial part of the process.

Afterward, users must invoke the claim function to claim the non-existent account. You can set the desired subscription period by configuring the subscription_period_days in the chromia.yml file. When the subscription period ends, the account becomes inactive and cannot be used further. To reactivate the account, users must renew the subscription by sending a specific amount of tokens to the account.

### Code example for transfer subscription strategy​

For practical guidance on building your dapp from the template, it's crucial that you refer to the [code examples](https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_subscription/) and corresponding tests. These resources will provide you with a clear understanding of the implementation process.

To make the transfer subscription available, extend your dapp configuration by copying and pasting the following configuration into your chromia.yml file:

```yml
lib.ft4.core.accounts.strategies.transfer:  rules:    - sender_blockchain: x"0000000000000000000000000000000000000000000000000000000000000000"      sender: "*"      recipient: "*"      asset:        - name: "MyTestAsset"        min_amount: 100L      timeout_days: 60      strategy:      - "subscription"lib.ft4.core.accounts.strategies.transfer.subscription:  asset:    - name: "MyTestAsset" # issued by current blockchain # OR id: x"C633343E4AA3213EA92158648F11BA8DFF606C6CAC80614CFA5F45E57367F823"    amount: 10L  subscription_period_days: 30  free_operations:    - some_free_operation
```

noteFor the complete chromia.yml configuration including blockchain setup, FT4 library configuration, and other required settings, refer to the [actual configuration file](https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_subscription/rell/chromia.yml) in the repository.

## Strategy comparison​

| 
| Aspect| transfer open| transfer fee| transfer subscription
| Fee required| No (but tokens must be transferred)| Yes (one-time)| Yes (recurring)
| Production use| Safe with proper safeguards| Recommended| Recommended
| Revenue model| No direct revenue| One-time payments| Recurring payments
| User experience| Requires token transfer| Simple one-time payment| Regular payments required
| Complexity| Low| Medium| High

## Configuration properties​

| 
| Property| Description
| sender_blockchain| The property sender_blockchain is set to receive tokens from the blockchain with the BRID: x"0000000000000000000000000000000000000000000000000000000000000000". To receive tokens from all chains in the Chromia network, use the asterisk sign "*" as the value.
| sender| The sender property defines who can send tokens to the dapp. The value "*" allows everyone to send tokens.
| recipient| The recipient property defines who can receive tokens in the dapp. The value "*" indicates that everyone can receive tokens.
| name| The name property represents the name of the asset required for account registration.
| min_amount| The min_amount property sets a minimum threshold for the transfer to a non-existent account for registration purposes.
| timeout_days| The timeout_days property specifies the period in days during which a user can claim a non-existing account with a balance.
| strategy| The strategy configures the strategy type used for the registration process.
notePlease refer to the [tests](https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_open/rell/src/test/) for testing the transfer open strategy, [tests](https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_fee/rell/src/test/) for testing the transfer fee strategy, and [tests](https://bitbucket.org/chromawallet/fee-samples/src/main/transfer_subscription/rell/src/test/) for testing the transfer subscription strategy.
