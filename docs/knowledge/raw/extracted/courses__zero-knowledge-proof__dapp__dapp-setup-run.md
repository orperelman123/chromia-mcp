# Dapp: setup and run

URL: https://learn.chromia.com

- [Home](/)
- [Module 2 – Dapp](/courses/zero-knowledge-proof/dapp/)
- Dapp: setup and runOn this page
# Dapp: setup and run

## Setup and run the dapp​

With the verification keys generated, you need to configure your local Chromia node to use them.

The generated verification keys must be configured in rell/chromia.yml.
The contents of the .json files generated in the previous step must be added into this chromia.yml file manually.

rell/chromia.yml
```yaml
gtx:  modules:    - "net.postchain.zkp.ZKPGTXModule"zkp:  plonk:    verification_keys:      shield_operation:        # Copy contents from shield_operation_verification_key.json      unshield_operation:        # Copy contents from unshield_operation_verification_key.json        private_transfer:        # Copy contents from verification_key.json
```

The already generated keys can be used from the example repository.

Next, run a local directory node using Docker:

```bash
docker run --rm -it -p 7740:7740/tcp registry.gitlab.com/chromaway/example-projects/directory1-example/managed-single:0.7.1
```

```bash
cd rell
```

Generate a provider keypair:

```bash
chr keygen -f provider-keypair
```

And deploy the dapp to your local network:

```bash
chr deployment create -y -d local --secret provider-keypair
```

Expected output:

```bash
deployments:  local:    chains:      zkp_demo: x"3E339D3D2AB109....EC3CD97681134"
```

DAPP_BRID = E339D3D2AB109....EC3CD97681134
