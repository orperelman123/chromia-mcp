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

We will start by setting up a new project using Chromia CLI:

```shell
chr create-rell-dapp rell_marketplace --template=plain-multi
```

```shell
cd rell_marketplace
```

To finalize the setup, we add the FT4 library to our project. Open your chromia.yml file and add ft4 to a new libs
section and configure the public key as the admin key for our dapp:

chromia.yml
```yaml
blockchains:  rell_marketplace:    module: maincompile:  rellVersion: 0.14.9database:  schema: schema_rell_marketplacetest:  modules:    - rell_marketplace_testlibs:  ft4:    registry: https://gitlab.com/chromaway/ft4-lib.git    path: rell/src/lib/ft4    tagOrBranch: v1.1.0r    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"    insecure: false  iccf:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/lib/iccf    tagOrBranch: 1.87.0    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"    insecure: false
```

Delete the src/rell_marketplace_test directory.

To install FT4, we run:

```shell
chr install
```

That's the basic setup for using FT4. In the upcoming lessons, we will use this to create assets, transfer tokens, and
register user accounts.
