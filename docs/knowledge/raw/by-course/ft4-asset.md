# COURSE ft4-asset — 6 pages


===== FILE: courses__ft4-asset__asset-operations.md =====

# Asset functions, operations &amp; queries

URL: https://learn.chromia.com

- [Home](/)
- Lesson 2 - Asset functions, operations & queriesOn this page
# Asset functions, operations & queries

In this lesson, we will implement the core operations for the pool account and manage FT4 assets, including minting,
burning, transferring, and locking assets.

## Functions​

Let’s define a few functions we’ll use to identify the asset and the pool account. These will be used throughout the module.

Add the following to src/main/functions.rell:

src/main/functions.rell
```rell
import lib.ft4.accounts;import lib.ft4.auth;function create_pool_account() =    accounts.create_account_without_auth(get_pool_account_id(), account_type);function get_pool_account_id() =    (account_type + chain_context.blockchain_rid).hash();function get_pool_account() =    accounts.account_by_id(get_pool_account_id());function get_asset_id() =    (asset_name, chain_context.blockchain_rid).hash();@extend(auth.auth_handler)function () = auth.add_auth_handler(flags = ["T"]);
```

We require the "T" flag so only users who can transfer assets can use operations like the faucet — ensuring they can use the assets they receive.

## Operations​

Now we can define the operation to initialize the dapp by creating the pool account.

Add the following to src/main/operations.rell:

src/main/operations.rell
```rell
import lib.ft4.core.admin;operation initialize_dapp() {    admin.require_admin();    create_pool_account();}
```

Key points:

- Creates a pool account with admin privileges

- Generates a unique account ID, derived from blockchain RID and account type

## Minting assets​

Minting creates new assets and adds them to the pool account.

Add the following import the top of the file:

src/main/operations.rell
```rell
import lib.ft4.assets.{ Unsafe }; // For minting and burning
```

Now define the minting operation:

src/main/operations.rell
```rell
operation mint() {    admin.require_admin();    Unsafe.mint(get_pool_account(), dapp_meta.asset, asset_amount_to_mint);}
```

Key points:

- Only admins can mint new assets

- Assets are minted to the pool account

- The amount is specified in the smallest unit (considering decimals)

## Burning assets​

Burning permanently removes assets from circulation.

Add the following import the top of the file:

src/main/operations.rell
```rell
import lib.ft4.core.assets.{ Asset };
```

Now define the burning operation:

src/main/operations.rell
```rell
operation burn() {    admin.require_admin();    val pool_account = get_pool_account();    Unsafe.burn(pool_account, Asset(get_asset_id()), asset_amount_to_burn);}
```

Key points:

- Only admins can burn assets

- Assets must be present in the pool account

- Burning reduces the total supply of the asset

## Faucet implementation​

Creating a faucet for testing purposes:

src/main/operations.rell
```rell
operation faucet() {    val receiver = auth.authenticate(); // extended to require the "T" flag, ensuring assets can be used    val pool_account = get_pool_account();    Unsafe.transfer(pool_account, receiver, Asset(get_asset_id()), asset_amount_to_faucet);}
```

Key points:

- Users must authenticate

- There is a fixed amount per request

- Transfers are conducted from the pool account

## Transferring assets​

Implementing asset transfers between accounts:

src/main/operations.rell
```rell
operation transfer(receiver: byte_array, amount: big_integer) {    val sender = auth.authenticate();    Unsafe.transfer(sender, accounts.Account(receiver.hash()), Asset(get_asset_id()), amount);}
```

Key points:

- The sender must be authenticated

- The amount must be positive

## Asset locking​

Implementing asset locking for temporary restrictions.

Add the following import the top of the file:

src/main/operations.rell
```rell
import lib.ft4.core.assets.locking;
```

Now define the locking operations:

src/main/operations.rell
```rell
operation lock_asset(type: text, account_id: byte_array, amount: big_integer) {    admin.require_admin();    locking.lock_asset(type, accounts.Account(account_id.hash()), Asset(get_asset_id()), amount);}operation unlock_asset(type: text, account_id: byte_array, amount: big_integer) {    admin.require_admin();    locking.unlock_asset(type, accounts.Account(account_id.hash()), Asset(get_asset_id()), amount);}
```

Key points:

- Only admins can lock or unlock assets

- Assets remain in the account but cannot be transferred

- The lock type can be used to categorize different kinds of restrictions (e.g. vesting, escrow)

## Queries​

Implementing queries to check balances and account statuses:

src/queries.rell
```rell
import lib.ft4.core.assets.{ get_asset_balance };query get_pool_account_balance() =  get_asset_balance(get_pool_account(), Asset(get_asset_id()));query get_user_account_balance(    pubkey) =  get_asset_balance(accounts.account_by_id(pubkey.hash()), Asset(get_asset_id()));query get_user_lock_account_balance(    pubkey) = locking.get_locked_asset_balance(        accounts.account_by_id(pubkey.hash()),        Asset(get_asset_id()),        ["FT4_LOCK"],        5,        null    );
```


===== FILE: courses__ft4-asset__consideration-recomendations.md =====

# Considerations and recommendations

URL: https://learn.chromia.com

- [Home](/)
- Considerations and recommendationsOn this page
# Considerations and recommendations

## Best practices:​

- Validate all inputs

- Check account existence

- Verify sufficient balances

- Handle edge cases

## Recommendations​

- Explore more advanced [FT4 features](https://docs.chromia.com/ft4/intro)

- Implement more secure strategies for creating user [accounts](https://docs.chromia.com/ft4/backend/accounts/)

- Consider declaring asset configuration parameters in the chromia.yml file.

- Deployment to the [testnet](https://docs.chromia.com/intro/getting-started/testnet/)

- Deployment to the [mainnet](https://docs.chromia.com/intro/deployment/)

- Build a [frontend interface](https://docs.chromia.com/ft4/client/client-setup)

- Create additional end-to-end (e2e) tests

## Post-Deployment Tasks​

- Initialize the dapp

- Set up the pool account

- Mint the assets

- Use the faucet

Congratulations! You have successfully completed the FT4 asset management course. Now adapt this project to suit your
own use case.


===== FILE: courses__ft4-asset__ft4-basics.md =====

# Asset basics

URL: https://learn.chromia.com

- [Home](/)
- Lesson 1 - Asset basicsOn this page
# Asset basics

In this lesson, we will explore the fundamental concepts of FT4 assets and their functionality within the Chromia
ecosystem.

## What are FT4 assets?​

FT4 assets are digital tokens that can represent any item of value on the Chromia blockchain. They are created using the
FT4 library, which provides a standardized approach for creating and managing assets.

## Key components of FT4 assets​

Account management

- Pool accounts for asset management

- User accounts for holding assets

- Different account types and permissions

Asset registration

- Every asset must be registered on the blockchain

- Assets have unique identifiers

- Assets are characterized by properties such as name, symbol, and decimal places

Asset operations

- Minting: Creating new assets

- Burning: Destroying assets

- Transferring: Moving assets between accounts

- Locking: Temporarily restricting asset movement

## Account types of the dapp​

In our project, we utilize two primary types of accounts:

Pool account

- Used for initial asset distribution

- Managed by the dapp

User accounts

- Created by users

- Used for holding and transferring assets

FT4 accounts can have varying permissions based on their intended use. This project follows a simplified structure with
user and admin roles.

## Asset registration​

Now, let's implement a straightforward asset configuration and registration.

Add the following to src/main/module.rell:

src/main/module.rell
```rell
import lib.ft4.assets.{ asset, Unsafe };import lib.ft4.core.accounts.strategies.open;// Asset propertiesval account_type = "PoolAccount";val asset_name = "TestAsset";val asset_symbol = "TAT";val asset_decimals = 6;val asset_icon_url = "https://url-to-asset-icon";val asset_amount_to_mint = 1000000000L;val asset_amount_to_faucet = 100000L;val asset_amount_to_burn = 100L;object dapp_meta {    asset = Unsafe.register_asset(        asset_name,        asset_symbol,        asset_decimals,        chain_context.blockchain_rid,        asset_icon_url    );}
```

This defines the configuration of the asset and registers it during blockchain initialization.

In the next lesson, we’ll implement the operations to mint, burn, and transfer this asset.


===== FILE: courses__ft4-asset__introduction.md =====

# Asset management

URL: https://learn.chromia.com

- [Home](/)
- Course overviewOn this page
# Asset management

Welcome to the Asset management course!

This course is designed to teach you how to create and manage digital assets on the Chromia blockchain using the
[FT4 library](https://docs.chromia.com/ft4/intro). You will build a minimal DeFi-oriented dapp that includes a pool
account, admin minting, admin burning, asset locking, and a user-accessible faucet.

## What you'll learn​

In this course, you will:

- Understand the fundamentals of [FT4 assets](https://docs.chromia.com/ft4/backend/assets/)

- Set up a pool account for asset management

- Learn how to create and manage digital assets

- Implement asset operations such as minting, burning, and transferring

- Implement mechanisms for locking and unlocking assets

- Create a faucet system for testing purposes

- Understand security considerations in asset management

## Course structure​

The course is divided into several lessons, each focusing on a specific aspect of FT4 asset development:

- Project setup and configuration

- Understanding the basics of FT4 assets

- Creating and managing assets

- Implementing asset operations

- Testing

- Security considerations and recommendations for next steps

## Repository link​

You can find the complete code repository for this course here:
[Asset management course](https://bitbucket.org/chromawallet/ft4-course/src/main/).


===== FILE: courses__ft4-asset__setup.md =====

# Project setup and configuration

URL: https://learn.chromia.com

- [Home](/)
- Project setup and configurationOn this page
# Project setup and configuration

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
chr create-rell-dapp asset_management
```

```shell
cd asset_management
```

Delete the src/main.rell file and its corresponding src/test/data_test.rell file, and instead create a folder called main inside src.

Inside src/main, create the following files:

- module.rell

- entities.rell

- functions.rell

- operations.rell

- queries.rell

This is a common best practice that improves clarity by organizing code into separate files based on their purpose.

In src/main/module.rell, add:

src/main/module.rell
```rell
module;
```

Then, create an admin keypair:

```shell
chr keygen --file .chromia/admin_keypair
```

This keypair is used to authorize any operation protected by admin.require_admin();.

To finalize the setup, open your chromia.yml file:

- add dependencies to a new libs section

- configure the admin_pubkey for the dapp. The key can be fetched by cat .chromia/admin_keypair command.

chromia.yml
```yaml
blockchains:  asset_management:    module: main    moduleArgs:      lib.ft4.core.admin:        admin_pubkey: x"---YOUR_ADMIN_PUBKEY---"compile:  rellVersion: 0.14.8database:  schema: schema_asset_managementtest:  modules:    - test  moduleArgs:    lib.ft4.core.admin:      admin_pubkey: x"033112FB1F0DF70D1EF4098E2F5F7DCB79BB9AD1570513F6CBDF6F9F4EDAC63771"libs:  ft4:    registry: https://gitlab.com/chromaway/ft4-lib.git    path: rell/src/lib/ft4    tagOrBranch: v1.1.0r    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"    insecure: false  iccf:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/lib/iccf    tagOrBranch: 1.87.0    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"    insecure: false
```

To install the dependencies, run:

```shell
chr install
```


===== FILE: courses__ft4-asset__testing.md =====

# Testing

URL: https://learn.chromia.com

- [Home](/)
- Lesson 3 - TestingOn this page
# Testing

In this lesson, we will cover important security considerations and how to test our FT4 asset implementation.

### 1. End-to-end tests​

The test cases can be found in the [repo](https://bitbucket.org/chromawallet/ft4-course/src/main/), under src/test/assets_test.rell. Copy this file into your own src/test/ folder.

To run the tests, use the following command:

```bash
chr build & chr test
```

### 2. Manual testing on local node​

#### Start the node locally​

```bash
chr node start
```

#### Generate keypairs of the test users​

Generate key pairs for the test users:

Alice:

```bash
chr keygen --file .chromia/alice_keypair
```

Trudy:

```bash
chr keygen --file .chromia/trudy_keypair
```

#### Initialize the dapp​

Initialize the dapp using the admin key pair to create the pool account (initializing once is enough, but not strictly mandatory):

```bash
chr tx initialize_dapp --secret .chromia/admin_keypair
```

#### Mint initial assets​

Mint initial assets to the pool account:

```bash
chr tx mint --secret .chromia/admin_keypair
```

Expected output:

```bash
transaction with rid D6A1E290E9E00E25453D182742CB92D6707BD38B89712C8AE6A53EF9452BF55A was posted and confirmed
```

Note: The transaction RID will be different for each transaction execution.

#### Check pool account balance​

To verify the pool account's balance, use:

```bash
chr query get_pool_account_balance
```

Output:

```bash
1000000000L
```

#### Create user accounts​

User accounts are required for authentication in calling operations.

Alice:

```bash
chr tx ft4.ras_open '[0, [["A","T"],x"ALICE_PUBKEY"], null]' 'null' --ft-register-account --secret .chromia/alice_keypair --await
```

The authorization flags for the auth descriptor are described
[there](https://docs.chromia.com/ft4/account-management/auth-descriptors#built-in-authorization-flags).

Trudy:

```bash
chr tx ft4.ras_open '[0, [["A","T"],x"TRUDY_PUBKEY"], null]' 'null' --ft-register-account --secret .chromia/trudy_keypair --await
```

#### Faucet assets​

To get test assets from the faucet:

Alice:

```bash
chr tx faucet --ft-auth --secret .chromia/alice_keypair
```

Trudy:

```bash
chr tx faucet --ft-auth --secret .chromia/trudy_keypair
```

#### Transfer assets​

To transfer assets to another account, use:

```bash
chr tx transfer 'x"TRUDY_PUBKEY"' 100L --ft-auth --secret .chromia/alice_keypair
```

#### Lock/Unlock assets​

To lock assets for a specific account, run:

```bash
chr tx lock_asset "FT4_LOCK" 'x"ALICE_PUBKEY"' 10L --secret .chromia/admin_keypair
```

FT4_LOCK is a default
[FT4 lock type](https://docs.chromia.com/ft4/backend/assets/locking-assets#lock-account-overview).

To unlock assets, use:

```bash
chr tx unlock_asset "FT4_LOCK" 'x"ALICE_PUBKEY"' 10L --secret .chromia/admin_keypair
```

#### Check user balances​

To check a user's asset balance, run:

```bash
chr query get_user_account_balance pubkey='x"ALICE_PUBKEY"'
```

Output:

```bash
99900L
```

Note: The actual balance amount may vary depending on the current state of the user's account and any previous transactions.

To check a user's locked asset balance, use:

```bash
chr query get_user_lock_account_balance pubkey='x"ALICE_PUBKEY"'
```

Output:

```bash
[]
```

Example with locked balances:

```bash
[  [    "data": [      "amount": 20L,      "type": "FT4_LOCK"    ],    "rowid": 31  ]]
```

Note:

- An empty array [] indicates no locked balances in the account

- When locked balances exist, each entry contains:

- amount: The locked amount in the smallest unit (with L suffix)

- type: The type of lock (e.g., "FT4_LOCK")

- rowid: A unique identifier for the lock record
