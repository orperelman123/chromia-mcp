# COURSE big-data — 6 pages


===== FILE: courses__big-data__blockchain-side-description.md =====

# Blockchain components

URL: https://learn.chromia.com

- [Home](/)
- Lesson 4 - Explore the blockchain componentsOn this page
# Blockchain components

## Navigate to the blockchain folder​

```shell
cd rell/src
```

## The key components of the blockchain dapp​

Import the [ft4](https://docs.chromia.com/ft4/setup/ft4-setup) library to utilize the pagination utility.

./chroma.yml
```rell
libs:  ft4:    registry: https://gitlab.com/chromaway/ft4-lib.git    path: rell/src/lib/ft4    tagOrBranch: v1.0.0r    rid: x"FA487D75E63B6B58381F8D71E0700E69BEDEAD3A57D1E6C1A9ABB149FAC9E65F"    insecure: false
```

The [entity](https://docs.chromia.com/rell/core-concepts#entity-definitions) named product defines a structure similar to a relational database table for storing data that will be analyzed.

./main.rell
```rell
entity product {    name: text;    description: text;    quantity: integer;    price: integer;}
```

The [operation](https://docs.chromia.com/rell/core-concepts#operations) named seed_data is responsible for creating a list of products. The key feature here is the batch creation of entities, which is executed with the statement create product (batch);.

./main.rell
```rell
operation seed_data() {    // 100 bytes    val name = "UltraPure Hydro Flask ProMax: Advanced Insulated Stainless Steel Bottle for Active and Everyday Use.";    // 500 bytes    val description = "Introducing the EcoSmart Water Bottle – your ultimate hydration companion! Made from premium, BPA-free materials, this bottle is designed for both functionality and style. With double-wall insulation, it keeps your beverages icy cold for up to 24 hours or piping hot for up to 12 hours. Its sleek, ergonomic design fits perfectly in your hand or cup holder. Featuring a leak-proof lid and a wide  mouth for easy cleaning or adding ice cubes, it’s perfect for gym sessions, hikes, or daily  commutes. Stay refreshed and eco-friendly with EcoSmart!";    // in total a size of 1 object is going to be 616 bytes    val batch_amount = 10; // amount of batches created    val batch_size = 10000; // the batch will have this number of entities to be created as a batch    // total data size can be calculated     // total_size = batch_amount * batch_size * object_size    // the  total size can be tuned with changing any parameter listed above.    var batch = list>();    for (x in range(batch_amount)) {        val shift_index = x * batch_size;        for (y in range(batch_size)) {            batch.add(                struct(                    name = name + shift_index + y,                    description = description,                    quantity = shift_index + y,                    price = shift_index + y                )            );        }        create product ( batch );        batch.clear();        print("saved", x);          }}
```

The [query](https://docs.chromia.com/rell/core-concepts#queries) named get_products_paginated retrieves paginated products from the node. The pagination is based on the cursor implemented in the FT4 library.

./main.rell
```rell
query get_products_paginated(page_size: integer?, page_cursor: text?): paginator.paged_result {    val before_rowid = paginator.before_rowid(page_cursor);    val paginated_result = product @* {        .rowid > (before_rowid ?: rowid(0))    } (        paginator.pagination_result(            data = map_product($).to_gtv_pretty(),            rowid = .rowid        )    ) limit paginator.fetch_data_size(page_size);    return paginator.make_page(paginated_result, page_size);}
```

Data Modelling

```code
product {    name: text;    description: text;    quantity: integer;    price: integer;}
```


===== FILE: courses__big-data__introduction.md =====

# Big data analysis with Chromia blockchain and PySpark

URL: https://learn.chromia.com

- [Home](/)
- Course overviewOn this page
# Big data analysis with Chromia blockchain and PySpark

## Course objectives​

By the end of this course, you will be able to:

- Understand how to integrate the Chromia blockchain with PySpark.

- Query data from the Chromia blockchain.

- Perform data transformations and aggregations using PySpark.

- Analyze and visualize data to extract meaningful insights.

## Key features​

- Asynchronous execution: Utilizes asyncio to handle blockchain transactions asynchronously, ensuring non-blocking operations.

- Blockchain interaction: Facilitates transaction creation and signing with postchain-client-py.

- Environment variables: Employs a .env file for managing sensitive data, such as private keys and configuration values.

- Randomized data generation: Generates random quantities and prices for products.

## Potential enhancements​

- Implement pagination to retrieve large amounts of data from the node's database.

- Incorporate error handling for specific blockchain-related errors.

- Log transactions to a file for debugging or auditing purposes.

- Validate environment variables and inputs before execution.

## Links​

- Documentation on [Rell](https://docs.chromia.com/rell/rell-intro).

- The complete code repository for this course can be accessed here: [The project repository](https://bitbucket.org/chromawallet/big-data-spark/src/main/).


===== FILE: courses__big-data__project-launch.md =====

# Prepare the project

URL: https://learn.chromia.com

- [Home](/)
- Lesson 2 - Prepare your projectOn this page
# Prepare the project

## Prepare the blockchain component​

### Navigate to the blockchain directory​

```shell
cd rell
```

### Start the blockchain component​

```shell
chr node start
```

## Prepare the PySpark component​

### Navigate to the PySpark directory​

```shell
cd pyspark
```

### Populate the .env file with the following values​

```env
POSTCHAIN_TEST_NODE=http://localhost:7740BLOCKCHAIN_TEST_RID=brid # This can be found in the terminal of the running chr nodePRIV_KEY=your_private_key # Enter the private key of the signer here
```


===== FILE: courses__big-data__project-run.md =====

# Run the project

URL: https://learn.chromia.com

- [Home](/)
- Lesson 3 - Run your projectOn this page
# Run the project

## Run PySpark​

### Navigate to the PySpark directory​

```shell
cd pyspark
```

### Generate dummy data​

The dummy data will be generated in the database of the active node.

```shell
python seed_products.py
```

### Execute PySpark functionality​

This process retrieves all data from the node, converts it into a PySpark DataFrame, and performs various analyses.

Note: When working with large datasets, you may encounter a Java OutOfMemoryError. This is a common issue when PySpark runs out of heap memory. To prevent this, add the following environment variables to your .env file before running the script:

For Linux/Mac/WSL users, use:

```env
# Set these environment variables before running your scriptexport SPARK_DRIVER_MEMORY=2gexport SPARK_EXECUTOR_MEMORY=2gexport SPARK_DRIVER_MAXRESULTSIZE=1g
```

For Windows users, use:

```env
# Set these environment variables before running your scriptset SPARK_DRIVER_MEMORY=2gset SPARK_EXECUTOR_MEMORY=2gset SPARK_DRIVER_MAXRESULTSIZE=1g
```

Now run the script:

```shell
python get_products_paginated.py
```


===== FILE: courses__big-data__python-side-description.md =====

# Python components

URL: https://learn.chromia.com

- [Home](/)
- Lesson 5 - Explore Python componentsOn this page
# Python components

## Navigate to the PySpark folder​

```shell
cd pyspark
```

## Client-side interaction with the blockchain​

The BlockchainClient class utilizes the NetworkSettings class to establish a connection to the blockchain node. This includes parameters such as node URLs, retry intervals, and failover strategies.

./seed_products.py
```python
settings = NetworkSettings(            node_url_pool=[os.getenv("POSTCHAIN_TEST_NODE", "http://localhost:7740")],            status_poll_interval=int(os.getenv("STATUS_POLL_INTERVAL", "500")), # Opitional parameter            status_poll_count=int(os.getenv("STATUS_POLL_COUNT", "5")), # Opitional parameter            verbose=True, # Opitional parameter            attempt_interval=int(os.getenv("ATTEMPT_INTERVAL", "5000")), # Opitional parameter            attempts_per_endpoint=int(os.getenv("ATTEMPTS_PER_ENDPOINT", "3")), # Opitional parameter            failover_strategy=FailoverStrategy.ABORT_ON_ERROR, # Opitional parameter            unreachable_duration=int(os.getenv("UNREACHABLE_DURATION", "30000")), # Opitional parameter            use_sticky_node=False, # Opitional parameter            blockchain_iid=int(os.getenv("BLOCKCHAIN_IID", "0")) # Opitional parameter        )        # Create blockchain client        client = await BlockchainClient.create(settings)}
```

seed_products function builds and sends a blockchain transaction to create a product in the database of the node .

./seed_products.py
```python
async def seed_products(client: BlockchainClient):    """Example of how to create and send a transaction"""    try:        # Create private/public key pair        private_bytes = bytes.fromhex(os.getenv("PRIV_KEY"))        private_key = PrivateKey(private_bytes, raw=True)        public_key = private_key.pubkey.serialize()                # Create operation        operation = Operation(            op_name="seed_data",            args=[]        )                # Create transaction        transaction = Transaction(            operations=[operation],            signers=[public_key],            signatures=None,            blockchain_rid=client.config.blockchain_rid        )                # Sign the transaction        signed_transaction = await client.sign_transaction(            transaction,            private_bytes        )                # Send transaction and wait for confirmation        receipt = await client.send_transaction(            signed_transaction,            do_status_polling=True        )                print(f"\nTransaction status: {receipt.status}")        if receipt.status == ResponseStatus.CONFIRMED:            print("Transaction confirmed!")        else:            print(f"Transaction failed: {receipt.message}")                except Exception as e:        print(f"Transaction failed: {str(e)}")
```

get_products_paginated function retrieves products from the database paginataed.

./get_products_paginated.py
```python
async def get_products_paginated(client: BlockchainClient, cursor: None):    """Example of how to query the blockchain"""    try:        return await client.query("get_products_paginated",             {"page_size": 1000, "page_cursor": cursor})    except Exception as e:        print(f"Query failed: {str(e)}")
```

This codesnippet demomstates how to use the query with pagination.
The main idea is to call the query in the loop until cursor equals to None.

./get_products_paginated.py
```python
products_all = []        products = await get_products_paginated(client, None)        products_all = products_all + products['data']        while products['next_cursor'] != None:            products = await get_products_paginated(client, products['next_cursor'])            products_all = products_all + products['data']
```


===== FILE: courses__big-data__setup.md =====

# Set up your project

URL: https://learn.chromia.com

- [Home](/)
- Lesson 1 - Set up your projectOn this page
# Set up your project

## Prerequisites​

- Basic knowledge of Python programming.

- Familiarity with blockchain concepts.

- Basic understanding of PySpark and Big Data processing.

- Python 3.8 or higher, along with related libraries.

- Use a virtual environment for Python.

## Set up the Chromia development environment​

Before we begin, ensure you have the following prerequisites in place:

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

## Clone the project repository​

```shell
git clone https://bitbucket.org/chromawallet/big-data-spark/src/main/
```

## Navigate to blockchain components​

```shell
cd main/rell/
```

## Install Rell dependencies​

```shell
chr install
```

## Navigate to PySpark components​

```shell
cd pyspark
```

## Install PySpark dependencies​

### Set up a virtual environment​

For Linux/WSL users:

```shell
# Create virtual environmentpython3 -m venv venv# Activate virtual environmentsource venv/bin/activate# Install dependenciespip install -r requirements.txt
```

For Windows users:

```shell
# Create virtual environmentpython -m venv venv# Activate virtual environmentvenv\Scripts\activate# Install dependenciespip install -r requirements.txt
```

Note: It is recommended to activate your virtual environment before running Python scripts in this project.
