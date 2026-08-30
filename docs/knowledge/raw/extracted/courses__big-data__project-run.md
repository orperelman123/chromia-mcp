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
