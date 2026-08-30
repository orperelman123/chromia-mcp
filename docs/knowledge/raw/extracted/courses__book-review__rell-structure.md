# Rell project structure

URL: https://learn.chromia.com

- [Home](/)
- Rell project structureOn this page
# Rell project structure

Rell is the programming language used by Chromia, designed specifically for building decentralized applications (dapps). It seamlessly integrates relational data models, providing an efficient and intuitive way to develop robust applications on the blockchain. For more details, visit the [Rell introduction](https://docs.chromia.com/rell/rell-intro).

## Organizing your project​

To structure your project effectively, we will organize the code into dedicated files and folders. This approach ensures clarity, ease of management, and scalability.

### Project setup​

- 
Create the main module folder:

- Since this project will consist of a single module, create a new folder named main within the src directory. This folder will hold all module-specific files.

- 
Set up essential files:

- Inside the main folder, create the following files:

- module.rell

- entities.rell

- functions.rell

- operations.rell

- queries.rell

The entities.rell file defines your data model. In Rell, each entity corresponds to a SQL table, with keywords such as key, index, and mutable influencing how the table is stored and accessed. For more details, see [Entity](https://docs.chromia.com/rell/language-features/modules/entity).

- 
Initialize the module (module.rell):

- In the module.rell file, add the following code:

src/main/module.rell
```rell
module;
```

- The module; declaration is required in Rell. It indicates the presence of a module and instructs Rell to treat the contents of this folder as a single module, thereby enhancing organization and code management.

- 
Delete the existing main.rell file:

- The src folder currently contains a file named main.rell. While it is possible to place all your code in this file, adopting a modular structure with multiple files offers clearer organization. As part of the restructuring process, delete the existing main.rell file.

Your project structure should now look like this:

```text
book-review/├── src/│   ├── main/│   │   ├── entities.rell│   │   ├── functions.rell│   │   ├── module.rell│   │   ├── operations.rell│   │   └── queries.rell│   └── test/│       └── book_review_test.rell├── .gitignore└── chromia.yml
```

Each file serves a specific purpose within the project, promoting clear organization and efficient management as your application evolves. In the following sections, we will explore each component in detail, starting with defining entities in Rell and progressively building out the decentralized application.
