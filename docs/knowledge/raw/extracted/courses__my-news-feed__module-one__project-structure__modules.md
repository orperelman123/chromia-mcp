# Work with Rell modules

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- [Lesson 5 - Project structure of the dapp](/courses/my-news-feed/module-one/project-structure/)
- Work with Rell modules
# Work with Rell modules

In this section, project structuring and modularization of the dapp will be explored to enhance the organization and maintainability of the code.

In Chromia, code can be organized into modules to improve structure and ease of maintenance. To define a module, the module; keyword is simply added at the top of the file. By default, the module name is derived from the file name, unless the file is named module.rell. In that case, the entire folder becomes a module with the name of the folder itself. Sub-modules can be created by adding folders within modules or by declaring another module; in a file within the same folder.

For example:

src/main.rell
```rell
module;
```

This defines a module called main.

src/main/module.rell
```rell
module;
```

This also defines a module called main.

If a second file, src/main/foo.rell, is added without a module declaration, it automatically becomes part of the main module. However, if it is declared as a module:

src/main/bar.rell
```rell
module;
```

A sub-module called main.bar will be created.

By following this pattern, a modular structure for the code can be built, promoting better organization and maintainability.
