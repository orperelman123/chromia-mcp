On this page
# Automate cross-chain asset registration
caution
This topic provides an example of automating cross-chain FT4 asset registration. It is not intended as a best practices guide. Carefully assess the risks associated with admin key exposure and implement strong security measures tailored to your setup.
If in doubt, do not implement this functionality. 
Automating cross-chain FT4 asset registration can streamline workflows in controlled environments, but it also introduces potential risks. Proper validation of assets and careful handling of admin credentials are essential.

---

## Set up a client​

Instantiate a `chromiaClient` for your chain:

```
import { createClient } from "postchain-client";

const chromiaClient = await createClient({
  directoryNodeUrlPool: ["http://localhost:7740"],//Replace with your node URL(s)
  blockchainRid: Buffer.from("your-blockchain-rid", "hex"),//Replace with your chain's RID
});

```

---

## Set up the admin signature provider​

Instantiate the `adminSignatureProvider` using the private key:

```
import { encryption, newSignatureProvider } from "postchain-client";

const adminPrivateKey = process.env.ADMIN_PRIVATE_KEY;
if (!adminPrivateKey) {
  throw new Error("ADMIN_PRIVATE_KEY is not set.");
}

const adminKeyPair = encryption.makeKeyPair(adminPrivateKey);
const adminSignatureProvider = newSignatureProvider(adminKeyPair);

```
danger
Prevent admin key exposure
Admin keys must be kept secure. This example is not a security guide; consider using encryption, hardware security modules (HSMs), or manual admin input.
Any misuse of admin credentials can compromise your system. Handle with the utmost caution. 
---

## Register a cross-chain asset​

```
import {
  createClientToBlockchain,
  registerCrosschainAsset,
  getAssetDetailsForCrosschainRegistration,
} from "@chromia/ft4";

export async function registerAssetWithValidation(
  assetId: BufferId,
  originBlockchainRid: BufferId
): Promise<TransactionCompletion> {
  // Create a client for the origin blockchain
  const originBlockchainClient = await createClientToBlockchain(chromiaClient, originBlockchainRid);

  // Fetch asset details from the origin blockchain
  const asset = await getAssetDetailsForCrosschainRegistration(originBlockchainClient, assetId);

  // Ensure the issuing blockchain matches the origin chain
  if (Buffer.compare(asset.blockchainRid, originBlockchainRid) !== 0) {
    throw new Error(
      `Validation error: Issuing blockchain (${asset.blockchainRid.toString(
        "hex"
      )}) does not match the origin (${originBlockchainRid.toString("hex")}).`
    );
  }

  // Register the validated asset on the destination chain
  return await registerCrosschainAsset(chromiaClient, adminSignatureProvider, assetId, originBlockchainRid);
}

```
info
Manual validation exceptions
This function ensures that the origin and issuing chains match to prevent broken transfer paths or potential misuse. In legitimate cases where the chains differ, manual admin registration is advised.
- Set up a client
- Set up the admin signature provider
- Register a cross-chain asset