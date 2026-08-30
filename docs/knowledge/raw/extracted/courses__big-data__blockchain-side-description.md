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
