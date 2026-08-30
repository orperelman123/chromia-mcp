On this page
# Import FT4 into your project

When you import the entire FT4 library, you might unintentionally expose many operations. It's crucial to consider whether you want to import the admin module, which allows registering new assets and accounts, minting tokens, and giving account rate limit points.

For this reason, the library is split into modules that can be imported separately to have granular control.

## FT4 library modules​

These are the modules that compose the FT4 library. Getting to know these might require some time, but it allows you to know exactly what operations the users will have access to.

- accounts: Handles accounts, auth descriptors, and rate limiting. 
- admin: Facilitates development and chain initialization. 
- admin.crosschain: Registers cross-chain assets. 
- assets: Manages assets, transfers, and transfer history. 
- auth: Enables account authentication, used by the login function. 
- cross-chain: Manages transactions across chains within a single network. 
- prioritization: Provides utility for transaction prioritization during network congestion. 
- test: For testing purposes only; should not be used in production. It exposes no external functions. 
- utils: Basic utility functions, particularly for pagination. It exposes no external functions. 
The `admin` module requires additional configuration, detailed below.

## Risks of importing all modules​

When you develop a production dapp, carefully check the imported modules of every library you're using:

- Imported entities and queries: Every `import` statement could introduce entities, queries, and operations. These could impact performance and potentially expose security vulnerabilities. 
- Exported queries and operations: Examine these to ensure they don't pose risks. Queries processing long lists of entities can be slow, and certain operations might allow undesired actions. 
FT4 mitigates some risks by:

- Paginating large entity queries to avoid performance issues. 
- Allowing modules to be imported without exposing external functions by using `ft4.core.` instead of `ft4.`. 
- Restricting dangerous operations to require an admin key, minimizing the risk of exposure. 
## Normal imports vs `core` imports​

To import a specific module, use:

```
import lib.ft4.<module_name>

```

For example, to work with assets:

```
import lib.ft4.assets

```

This assumes you want to import everything related to assets, including the queries and operations that allow users to interact with them. These are generally useful to the end users and are expected to be needed. The functionality they introduce includes checking balances, transferring assets, and much more.

However, which operations are safe and which aren't depends heavily on your dapp structure, and there's no way to predict if a specific one could create issues in your case. In case you want to exclude queries and operations of a module from your dapp, you can use this import statement:

```
import lib.ft4.core.assets

```
info
Thoroughly check your imports before launching a production dapp. Only include what you need, and avoid importing unnecessary modules. 
## The `admin` module​

The `admin` module adds operations that grant a specific user (the admin) exceptional rights, such as minting tokens, creating accounts, registering new assets, and more. Avoid using this module in production dapps as it introduces significant security risks.

While some dapps might need an admin with special permissions, the `admin` module is too simple and generalized to be used safely in production environments. Instead, consider implementing more robust access control mechanisms, such as multi-signature schemes, to manage your dapp securely. Also, consider the potential consequences if the admin account keys are compromised or lost.

If you import the `admin` module, you'll need to configure it in the `chromia.yml` file:

```
blockchains:
 hello:
  module: main
  moduleArgs:
   lib.ft4.core.admin:
    admin_pubkey: 03028A31DBA82E46DE26A608249147A6A1A88C62A1A65B640C9B4369D4CAD928BE

```

The `admin_pubkey` you set up here will be the only authorized key to call the admin operations. Remember to change the provided public key with one you created yourself using `chr keygen`, as shown in the FT4 setup.
warning
Never use the `admin` module in production dapps. It introduces significant security risks, and there are better ways to manage admin users tailored to your specific use case. 
## The `crosschain` module​

The `crosschain` module provides access to the operations that are needed to transfer assets across the network. This allows users on two different dapps to send tokens to each other.

To use it, it's necessary to have three different pieces of configuration in place.

- 
The first one, as for every FT4 module, is to import it in any of your files:

```
import lib.ft4.crosschain;

```

- 
In the `chromia.yml` file, you'll need a GTX module that allows your chain to interact with the rest of the network:

```
blockchains:
  hello:
    module: main
    moduleArgs:
      <your module args>
    config:
      gtx:
        modules:
          - "net.postchain.d1.iccf.IccfGTXModule"

```

- In the `chromia.yml` file, you'll also need a small library that allows the Rell code to interact with the GTX module, and it should look like this: 
```
blockchains:
 hello:
    module: main
    moduleArgs:
      <your module args>
    config:
      <the gtx module from step 2>

libs:
  ft4:
    registry: https://gitlab.com/chromaway/ft4-lib.git
    path: rell/src/lib/ft4
    tagOrBranch: v1.1.0r
    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"
    insecure: false
  iccf:
    registry: https://gitlab.com/chromaway/core/directory-chain
    path: src/lib/iccf
    tagOrBranch: 1.87.0
    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"
    insecure: false

```

To get the latest version of ICCF, please check on the ICCF documentation.
info
It's important not to import the ICCF functionality unless you plan to use it, but there's no risk in importing the ICCF Rell module on step 3 if you avoid the first two steps. Since the FT4 code depends on the ICCF Rell module, your IDE might complain if this module is not present.
For this reason, we suggest keeping the `libs` part of the configuration even if you don't want to use the cross-chain functionality, although it's also safe to remove it. 
After understanding how to import the FT4 library, we'll explore the methods for registering assets in Chromia. You can register assets using either the FT4 admin operation or by writing a custom operation. The register assets section provides detailed explanations for both approaches.
- FT4 library modules
- Risks of importing all modules
- Normal imports vs `core` imports
- The `admin` module
- The `crosschain` module