On this page
# FT4 configuration values

This topic provides the key configuration options available for FT4 modules, providing descriptions, types, and default values for each parameter.
note
You only need to configure the modules that you import in your dapp. If a module is not imported, its configuration is not required. 
## How to configure FT4 properties​

You can configure these properties in your `chromia.yml` file under the `moduleArgs` section of each blockchain:

```
blockchains:
  <blockchain>:
    module: main
    moduleArgs:
      lib.ft4.core.admin:
        admin_pubkey: "03028A31DBA82E46DE26A608249147A6A1A88C62A1A65B640C9B4369D4CAD928BE"
      lib.ft4.core.accounts:
        rate_limit:
          active: true
          max_points: 20
          recovery_time: 5000
          points_at_account_creation: 1

```

## Admin module configuration​

The admin module provides administrative operations for development and chain initialization.
warning
The admin module should not be present in production dapps as it provides administrative access that could compromise security. info
For more information about the admin module and its operations, refer to the FT4 imports documentation. 
| Name | Description | Type | Required | Default |
````| admin_pubkey | Public key of the admin user authorized to call admin operations. Must be generated using chr keygen. | string | ✅ |  |

### Example configuration​

```
blockchains:
  <blockchain>:
    module: main
    moduleArgs:
      lib.ft4.core.admin:
        admin_pubkey: "03028A31DBA82E46DE26A608249147A6A1A88C62A1A65B640C9B4369D4CAD928BE"

```

## Accounts module configuration​

The accounts module handles account management, authentication descriptors, and rate limiting.
info
For detailed information about account management and auth descriptors, refer to the account management documentation. 
### Rate limiting configuration (under the key `rate_limit`)​

| Name | Description | Type | Default |
````| active | Enables or disables rate limiting globally. | boolean | true |
````| max_points | Maximum number of operation points that can accumulate (maximum transactions that can be done at once). | int | 10 |
````| recovery_time | Time in milliseconds for points to recover (cooling period before an account can receive one operation point). | int | 5000 |
````| points_at_account_creation | Points that an account has at the moment of its creation (0 is minimum). | int | 1 |

### Auth descriptor configuration (under the key `auth_descriptor`)​

| Name | Description | Type | Default |
````| max_number_per_account | Maximum number of auth descriptors an account can have. | int | 10 |

### Auth descriptor rules configuration (under the key `max_auth_descriptor_rules`)​

| Name | Description | Type | Default |
````| max_auth_descriptor_rules | Maximum number of rules that can be configured in an auth descriptor. | int | 8 |

### Example configuration​

```
blockchains:
  <blockchain>:
    module: main
    moduleArgs:
      lib.ft4.core.accounts:
        rate_limit:
          active: true
          max_points: 20
          recovery_time: 5000
          points_at_account_creation: 1
        auth_descriptor:
          max_number_per_account: 4
        max_auth_descriptor_rules: 4

```
info
For additional FT4 configuration, refer to the following documentation:
Transfer Strategy Configuration: 
- Transfer open strategy - Configuration for open transfer strategy 
- Transfer fee strategy - Configuration for fee-based transfer strategy 
- Transfer subscription strategy - Configuration for subscription-based transfer strategy 
- Account registration overview - General strategy comparison and configuration 
Cross-chain Configuration: 
- FT4 imports documentation - Cross-chain GTX module configuration 
- Client orchestrator - Cross-chain transfer setup 
Special Configuration Values: 
- Account registration overview - Special values (`*`, `$`, `X`) for transfer rules 
## Library dependencies​

FT4 requires specific library dependencies to be configured in the `libs` section.

| Name | Description | Type | Required | Default |
``| ft4 | FT4 library configuration with registry, path, tag/branch, and RID. | object | ✅ |  |
``| iccf | ICCF library for cross-chain functionality (recommended even if not using cross-chain). | object |  |  |

### Library configuration properties​

Each library object contains the following properties:

| Name | Description | Type | Required | Default |
``| registry | Git registry URL for the library. | string | ✅ |  |
``| path | Path within the repository to the library. | string | ✅ |  |
``| tagOrBranch | Git tag or branch to use for the library. | string | ✅ |  |
``| rid | Repository ID for the library. | byte_array | ✅ |  |
````| insecure | Whether to use insecure connections (not recommended for production). | boolean |  | false |

info
For detailed library configuration examples, refer to the FT4 setup documentation which provides complete configuration examples. 
## Complete configuration example​

Here's a complete example showing the core FT4 module configuration options:

```
blockchains:
  <blockchain>:
    module: main
    moduleArgs:
      lib.ft4.core.admin:
        admin_pubkey: "03028A31DBA82E46DE26A608249147A6A1A88C62A1A65B640C9B4369D4CAD928BE"
      lib.ft4.core.accounts:
        rate_limit:
          active: true
          max_points: 20
          recovery_time: 5000
          points_at_account_creation: 1
        auth_descriptor:
          max_number_per_account: 4
        max_auth_descriptor_rules: 4

```
tip
For complete configuration examples including library dependencies and transfer strategies, refer to: 
- FT4 setup documentation for library dependencies 
- Transfer strategy topics for strategy configurations 
- How to configure FT4 properties
- Admin module configuration
- Example configuration
- Accounts module configuration
- Rate limiting configuration (under the key `rate_limit`)
- Auth descriptor configuration (under the key `auth_descriptor`)
- Auth descriptor rules configuration (under the key `max_auth_descriptor_rules`)
- Example configuration
- Library dependencies
- Library configuration properties
- Complete configuration example