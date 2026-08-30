# Incorporate modules in the dapp

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- [Lesson 5 - Project structure of the dapp](/courses/my-news-feed/module-one/project-structure/)
- Incorporate modules in the dapp
# Incorporate modules in the dapp

Let’s put this into practice by organizing code into smaller modules. First, a folder named news_feed is created and a module.rell file is added inside it:

src/news_feed/module.rell
```rell
module;
```

Next, everything from this module is imported into main.rell by adding the following line:

src/main.rell
```rell
import news_feed.*;
```

Then, a file named model.rell is created in the src/news_feed folder and the entity definitions from the main file are moved into this new file:

src/news_feed/model.rell
```rell
entity user {  mutable name;  key id: byte_array;  key account;}entity follower {  index user;  index follower: user;  key user, follower;}entity post {  timestamp = op_context.last_block_time;  index user;  content: text;}
```

Now, the import statement for the account entity from the ft-library is shifted to the module.rell file. This arrangement allows all files within the module to access it:

src/news_feed/module.rell
```rell
module;import lib.ft4.accounts.{ account };import lib.ft4.auth;
```

The auth_handler is moved to src/news_feed/auth.rell and post_dto to src/news_feed/dto.rell. If preferred, the require_user and format_user utility functions can also be added directly to module.rell. The queries are placed in src/news_feed/queries.rell, and the operations are relocated to src/news_feed/operations.rell.

After these changes, the module.rell file in the news_feed module should look like this:

src/news_feed/module.rell
```rell
module;import lib.ft4.accounts.{ account };import lib.ft4.auth;function require_user(id: byte_array) = require(user @? { id }, "User with id %s does not exist".format(id));function format_user(user) = "%s#%s".format(user.name, user.id.to_hex().sub(0, 5));
```

With the proper imports in place, the main.rell file should now only include the create_user operation, which is beneficial for testing purposes.

Next, a new module called test_operations is created in the test folder by creating a file there and declaring it as a module:

src/test/test_operations.rell
```rell
module;import lib.ft4.core.accounts. { create_account_with_auth };import lib.ft4.accounts.{ single_sig_auth_descriptor };import ^^.news_feed.{ user };operation create_user(name, pubkey) {    val account = create_account_with_auth(single_sig_auth_descriptor(pubkey, set(["A", "T", "S"])));    create user ( name, pubkey, account );}
```

In the test file, this module is explicitly imported, possibly using relative imports:

src/test/news_feed_test.rell
```rell
import ^.test_operations.{ create_user };
```

It has now been shown how to effectively structure the project using Rell modules. The file structure should resemble the following:

```text
rell/src├─ main.rell├─ news_feed│  ├─ auth.rell│  ├─ dto.rell│  ├─ model.rell│  ├─ module.rell│  ├─ operations.rell│  └─ queries.rell├─ registration│  └─ module.rell└─ test   ├─ news_feed_test.rell   ├─ registration_test.rell   └─ test_operations.rell
```
