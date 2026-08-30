# FT4 accounts configuration

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- [Lesson 2 - Create accounts](/courses/my-news-feed/module-one/create-accounts/)
- FT4 accounts configurationOn this page
# FT4 accounts configuration

This section provides guidance on installing the FT4 library and configuring accounts.

## Install the FT4 library​

Before diving into account configuration, the FT4 library needs to be installed to manage accounts effectively. This is done through the following configuration in the chromia.yml file:

chromia.yml
```yaml
libs:  ft4:    registry: https://gitlab.com/chromaway/ft4-lib.git    path: rell/src/lib/ft4    tagOrBranch: v1.1.0r    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"    insecure: false  iccf:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/lib/iccf    tagOrBranch: 1.87.0    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"    insecure: false
```

## Configure FT4 accounts​

In this app, FT4 accounts will be added to the model, enhancing the authentication mechanism.

Start by importing the necessary modules:

src/registration/module.rell
```rell
import lib.ft4.core.accounts.strategies.open. { ras_open };import lib.ft4.accounts.strategies. { register_account };
```

Next, define an account within the user entity:

src/news_feed/model.rell
```rell
entity user {  mutable name;  key id: byte_array;  key account;}
```

This setup enforces a one-to-one mapping between an FT4 account and a user. Now, modify the create_user operation to also create an FT4 account:

src/registration/module.rell
```rell
operation register_user(name) {    val account = register_account();    val user = create user ( name, account.id, account );    create follower ( user = user, follower = user );}
```

For detailed information on account permissions and the FT4 framework, refer to the [FT4 Accounts and Tokens documentation](https://docs.chromia.com/ft4/intro).

Next, the test section is moved under the blockchain tag in chromia.yml and the admin public key is added:

chromia.yml
```yaml
blockchains:  newschain:    module: main    moduleArgs:      lib.ft4.core.auth:        evm_signatures_authorized_operations:          - register_user      lib.ft4.core.admin:          admin_pubkey: "0359A8F2CE1BEF95F583169B7DF053AA227A93B2652B0A9C22975FEED638032610"    test:      modules:        - testcompile:  rellVersion: 0.14.9database:  schema: schema_newschain  libs:  ft4:    registry: https://gitlab.com/chromaway/ft4-lib.git    path: rell/src/lib/ft4    tagOrBranch: v1.1.0r    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"    insecure: false  iccf:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/lib/iccf    tagOrBranch: 1.87.0    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"    insecure: false
```

For further details on configuring your chromia.yml file, refer to the [Chromia Project Configuration documentation](https://docs.chromia.com/intro/configuration/project-structure).
