# Lesson 1 - Configure the Blockchain dapp

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 - Blockchain dapp](/courses/ft4-demo-app/module-blockchain/)
- Lesson 1 - Configure the Blockchain dapp
# Lesson 1 - Configure the Blockchain dapp

- You can spot chromia.yml file to have a FT4 library as a dependency. Update this file with the following configuration:

chromia.yml
```yaml
blockchains:  asset_management:    module: main    moduleArgs:      main:        basic: 5      lib.ft4.core.admin:        admin_pubkey: x"023BEE5A479CE5AF31F6F64EDE7BEAD394E92E4D973E1727782DB577A55E878563"compile:  rellVersion: 0.14.9  source: rell/srcdatabase:  schema: schema_asset_managementtest:  modules:    - test  moduleArgs:    lib.ft4.core.admin:      admin_pubkey: x"023BEE5A479CE5AF31F6F64EDE7BEAD394E92E4D973E1727782DB577A55E878563"libs:  ft4:    registry: https://gitlab.com/chromaway/ft4-lib.git    path: rell/src/lib/ft4    tagOrBranch: v1.1.0r    rid: x"FEEB0633698E7650D29DCCFE2996AD57CDC70AA3BDF770365C3D442D9DFC2A5E"    insecure: false  iccf:    registry: https://gitlab.com/chromaway/core/directory-chain    path: src/lib/iccf    tagOrBranch: 1.87.0    rid: x"9C359787B75927733034EA1CEE74EEC8829D2907E4FC94790B5E9ABE4396575D"    insecure: false
```

- Run chr install to download the FT4 library.

This configuration sets up your project with the FT4 library and necessary dependencies for building the asset management system.
