On this page
# Introduction to Multisig

When handling on-chain operations as an organization, it is in general a good idea to use multisignature access, or multisig for short. The funds you receive are sent to an FT4 account, and using multisig access means that multiple people must cooperate to move those assets. This is different from the default behavior, where a single key is used to access funds: in this case, there's no single person using the key that could maliciously access funds.

In a multisig scenario, these concepts are very important to understand:

- signers: the keys used to access the account and/or its funds. As a general rule, each signer should correspond to one person. While a single person can hold multiple keys, it is strongly recommended not to assign multiple persons to a single key. 
- signatures required: how many of the above keys should sign before the operation they're trying to perform is accepted. This value must be less or equal to the number of signers. warning
Be very careful with the value you set as signatures required! If you require all signers to sign (as many signatures required as there are signers), a single person turning malicious or losing access to their keys will PERMANENTLY lock you out of your account. If you require too few signatures, a small number of people turning malicious can access your account and/or its funds. 
### Example scenario​

A company is setting up an account. Alice, the CEO, decides that it's not safe for her to own the keys to the FT4 account - if she was to lose all her backups of the key, all the funds would be locked forever. She then decides to split this access with two people in the finance department: Bob and Eve.

She sets up the account to have multisig access, with these parameters:

- signers: Alice, Bob, Eve 
- signatures required: 2 
This means that if Alice were to lose her key, Bob and Eve could still recover the funds. What's more, Bob and Eve could remove the access to the old key that Alice has lost, and give access to a new key that Alice will generate.

However, if Eve was to turn malicious or leave the company in bad faith, she wouldn't be able to steal any funds unless Alice or Bob cooperate with her.

# Setting up a multisig account

In FT4, all accounts are equal. There is no such thing as a multisig account. However, the accounts are accessed through auth descriptors, and an auth descriptor can be setup to be multisign or singlesig. Whenever this document refers to a multisig or singlesig account, it means an account that can only be accessed through a multisig or singlesig auth descriptor.

In reality, a single account can have a multisig auth descriptor and a single signature one, meaning that to access the account and/or its funds you must use either the single signature key or the cooperation of multiple keys from the multisig descriptor. However, that scenario will only be discussed later on in this guide, to keep things simple.

While it is technically possible to register an account with a multisignature auth descriptor from the start, it is easier to start with a single signature account registration and then convert it to multisig afterwards. What's more, some organization may prefer to start with a single signature account to handle things in a simpler way, so this guide will focus on converting a single signature account to a multisig one. There's no additional costs in converting the account, so feel free to create a singlesig account and then convert it to multisig.

To create such an account, it's very helpful to use the NodeJS REPL. An example of account creation is written below.

This guide will write commands with explicit arguments, like pubkeys and account IDs, to better show how such a command will be written. Remember, however, that the IDs and parameters that this guide will set will be different from the ones you want to use, so avoid blindly copying and pasting the commands into the console.

## Optional: Account registration​

This section of the document can be skipped if you already have an account with non-EVM signers. If you don't, follow these instructions to create such an account. You will have to pay an account creation fee, which on Economy Chain Mainnet is 10CHR, at the time of writing.

Not all chains will allow account creation for a fee, so be sure to check out what is allowed on the chain you're interested in creating your account on.

This guide will showcase how it can be done on Testnet, by paying 10tCHR fee.

We will use the NodeJS REPL.

### Setup​

Create a folder, and open a terminal window in it. Run `npm i @chromia/ft4` to install the required packages.

Create a keypair for the initial (singlesig) owner of the account. This key will be removed shortly, but you will need it at the start of the process. Use this command to do so:

```
chr keygen --file=temporary

```

In my case, the private key (which can be found by opening the file named `temporary`) is `A6A8CED4556C16A63D1BD78AAE4647251C5A82270D46E3E4E7E13586E8571E77`
note
It is generally highly recommended to use `--key-id` instead of `--file`. However, since this key is only needed in the short term, `--file` avoids it being added to the global key list on your system. 
Run `node` to open the REPL, then import the libraries:

```
> ft = await import("@chromia/ft4")
> pcl = await import("postchain-client")

```

Create a client (configure it for your chain and network):

```
> cl = await pcl.createClient({
... nodeUrlPool:"https://node0.testnet.chromia.com",
... blockchainRid: "090BCD47149FBB66F02489372E88A454E7A5645ADDE82125D40DF1EF0C76F874"
... })

```

Create a keypair for the owner of this new account. Notice that I used the temporary private key.

```
> kp = pcl.encryption.makeKeyPair("A6A8CED4556C16A63D1BD78AAE4647251C5A82270D46E3E4E7E13586E8571E77")
> ks = ft.createInMemoryFtKeyStore(kp)

```

Find out what is going to be your account ID.

```
> pcl.gtv.gtvHash(kp.pubKey).toString("hex")
'885a6648772ab9615ccd88d25201402f734b43ef20da959623591b0dca5098c5'

```

Now, you need to receive 10CHR on this account. Remember that, for other chains, this amount can vary. You can send the tokens to that account, if you already have a different account on this or another chain, or you can bridge those funds from outside of the Chromia ecosystem. Once the funds have been transferred, you will be able to create the account.

### Registration​

Create an auth descriptor that allows the temporary key to use the account:

```
> ad = ft.createSingleSigAuthDescriptorRegistration(["A", "T"], kp.pubKey)

```

Find the asset that you used to create the account. In this case, since the account was created on Testnet, the asset is tCHR:

```
> connection = ft.createConnection(cl)
> assets = await connection.getAssetsBySymbol("tCHR")
> tchr = assets.data[0]

```

Register the account:

```
> strat = ft.registrationStrategy.transferFee(tchr, ad)
> await ft.registerAccount(cl, ks, strat)

```

You now have an account controlled through a native keypair.

## Starting scenario​

I have an account on Chromia Testnet, which holds 2tCHR. These are the relevant pieces of information:

- Account ID: `885A6648772AB9615CCD88D25201402F734B43EF20DA959623591B0DCA5098C5`
- Owner pubkey: `022672944E1D542487601145FF42B2953F32CC7DA1167EBD3D0087954816EDD146`
We'll refer to this keypair as singlesig key. We want to add access to Alice, Bob, and Charlie.

## Key generation​

Alice, Bob and Charlie each create a new keypair to start the process. They can do so with:

```
chr keygen --key-id=<some ID>

```

Check The CLI page for more information on how to use these keys.

The pubkeys for Alice, Bob and Charlie should all be added to a single file, which we can name `signers`:

```
Alice=03F5A2656BC6DB03A8F85C2FD3FD32D72515D1435BB0869A28FFB49185E23E25D6
Bob=02AB13E8808E8AC7A8CAD65660A189FF3744063D6B48C4BC5236F44F98A333CD41
Charlie=03B7EF6C5948022298051E57521CE56075A16A883516240021BD337C8A6E7DDD13
singlesig=022672944E1D542487601145FF42B2953F32CC7DA1167EBD3D0087954816EDD146

```

The person owning this file should also have access to `singlesig`, for ease of use.

## Switching to multisig​

### Configure the CLI​

To send commands through the CLI, you will need to configure it. Make sure you have a `chromia.yml` in the same folder, with some configuration that looks like the following:

```
blockchains:
  a:
    module: a
deployments: 
  testnet: 
    brid: x"6F1B061C633A992BF195850BF5AA1B6F887AEE01BB3F51251C230930FB792A92" 
    url: 
      - "https://node0.testnet.chromia.com" 
    chains: 
      ec: x"090BCD47149FBB66F02489372E88A454E7A5645ADDE82125D40DF1EF0C76F874"

```

- `testnet` is a name you can freely give to the network. It does not need to be any specific value - keep it short as you'll have to type this a lot. 
- `url` defines what network this configuration points to. In this case, it is the Chromia Testnet. 
- `brid` must correspond to the BRID of the Directory Chain on the network, which can be found by going to the block explorer for the network you chose and searching for the keyword "directory_chain" on the "Blockchains" section. You can find the page for the Directory Chain on Mainnet here. 
- `ec` is a name you can freely give to the chain, which in this case corresponds to the Economy Chain. The hex value beside `ec` corresponds to its BRID. This value also does not need to be any specific value, but you'll again want keep it short as you'll have to type this one a lot too. note
The `blockchains` configuration does not need to refer to any file, but it must be present. Feel free to copy it exactly as it's found here if you're not sure what to put under that.
The `deployments` configuration, however, must be correct. 
### Find your current auth descriptor​

First of all, we need the auth descriptor that currently defines the account as a singlesig account. Notice that the values after `-d` and `--bc` are the ones that were used on the `chromia.yml`

```
$ chr query -d testnet -bc ec ft4.get_account_main_auth_descriptor account_id=885A6648772AB9615CCD88D25201402F734B43EF20DA959623591B0DCA5098C5
[
  "account_id": x"885A6648772AB9615CCD88D25201402F734B43EF20DA959623591B0DCA5098C5",
  "args": [
    [
      "A",
      "T"
    ],
    x"022672944E1D542487601145FF42B2953F32CC7DA1167EBD3D0087954816EDD146"
  ],
  "auth_type": "S",
  "created": 1731494455695,
  "id": x"E6D0B18FD6BEE8EBE8E514DF55B46E5977FC4B3E16569B9D5A3C62FE050124A3",
  "rules": null
]

```

Your output will be different. This output gave us the unique ID we need to use to refer to this auth descriptor: `E6D0B18FD6BEE8EBE8E514DF55B46E5977FC4B3E16569B9D5A3C62FE050124A3`

### Create your new auth descriptor​

You must first define the new auth descriptor, which will be used to define the access that Alice, Bob and Charlie have over your account. Follow this template:

```
[
  0 -> single sig OR 1 -> multi-sig,
  [
    ["A", "T"], -> flags, "A" and "T" are mandatory for the main auth descriptor ("owner")
    5, -> if multisig, how many signatures are required. If single sig, remove this line
    x"pubkey" if single sig OR [x"pubkey1", x"pubkey2"] if multisig
  ],
  null -> expiration rules, must be null ("never expire") for the main auth descriptor ("owner")
]

```

In my case, I chose to use:

```
[
  1,
  [
    ["A", "T"],
    2,
    [
        x"03F5A2656BC6DB03A8F85C2FD3FD32D72515D1435BB0869A28FFB49185E23E25D6",
        x"02AB13E8808E8AC7A8CAD65660A189FF3744063D6B48C4BC5236F44F98A333CD41",
        x"03B7EF6C5948022298051E57521CE56075A16A883516240021BD337C8A6E7DDD13"
    ]
  ],
  null
]

```

The pukeys are the same that appear in the file as Alice, Bob and Charlie. We want to remove access from `singlesig`, which is why the key does not appear in the list.

This auth descriptor is what would be called a "2 out of 3" multisig descriptor, which means that any combination of two people between Alice, Bob and Charlie can use this account without the third person being involved.
note
"A" and "T" are the authorization flags for this auth descriptor. Check out the auth flags section for info on what they mean. 
To be able to send this auth descriptor to the blockchain, write it all down in one line and between single quotes `'`. Whitespace is not important:

```
'[1, [["A", "T"], 2, [x"03F5A2656BC6DB03A8F85C2FD3FD32D72515D1435BB0869A28FFB49185E23E25D6", x"02AB13E8808E8AC7A8CAD65660A189FF3744063D6B48C4BC5236F44F98A333CD41", x"03B7EF6C5948022298051E57521CE56075A16A883516240021BD337C8A6E7DDD13"]], null]'

```

### Create the transaction​

To create the transaction, `singlesig` needs to run this command:

```
chr multi-signature create -d <network> -bc <chain name> \
    --ft-auth --signers-file <signers file with all pubkeys> \
    -id <singlesig auth descriptor ID> \
    ft4.update_main_auth_descriptor -- <the auth descriptor>

```

Using the values obtained above, this command becomes:

```
chr multi-signature create -d testnet -bc ec \
    --ft-auth --signers-file signers --secret temporary \
    -id E6D0B18FD6BEE8EBE8E514DF55B46E5977FC4B3E16569B9D5A3C62FE050124A3 \
    ft4.update_main_auth_descriptor -- \
    '[1, [["A", "T"], 2, [x"03F5A2656BC6DB03A8F85C2FD3FD32D72515D1435BB0869A28FFB49185E23E25D6", x"02AB13E8808E8AC7A8CAD65660A189FF3744063D6B48C4BC5236F44F98A333CD41", x"03B7EF6C5948022298051E57521CE56075A16A883516240021BD337C8A6E7DDD13"]], null]'

```
note
Having followed the "Optional: Account registration" chapter, my singlesig key could be found in a file named `temporary` in my working directory. This could not be the case on your system - refer to the CLI guides to know how to sign this transaction. 
This command will create a file, with a pretty long name containing a timestamp. I renamed the file to `update_tx` for ease of use.

### Sign the transaction​
note
The following commands will not specify `--secret file`, as they will suppose that all of Alice, Bob and Charlie are using key IDs to sign the transactions. 
Now Alice can sign with her new key:

```
chr multi-signature sign --file=update_tx --target .

```

This creates a new file, that has been signed by Alice AND `singlesig`. The old file `update_tx` can be removed, if needed.

Alice can now send this file to Bob, who will check out what he's signing:

```
chr multi-signature view --file=received_from_alice

```

If everything looks good in the file, Bob can sign it with a similar command to what Alice used:

```
chr multi-signature sign --file=received_from_alice --target .

```

Bob can send the output file to Charlie, who can then follow the same steps as Bob to view and sign the transaction.
info
While the auth descriptor is a "2 out of 3", all 3 signers must sign to add it. To execute operations, you only need 2 signers of the three, however. 
### Send the transaction​

Charlie now has a file signed by him, Alice, Bob, and `singlesig`, which he renamed `update_signed`. To execute the operation, he can call:

```
chr multi-signature send --file=update_signed -d testnet -bc ec

```

Note that, since the transaction is already signed, anybody could send it - it does not need to be Charlie, nor any of the other signers. The only requirement is to have a `chromia.yml` configured in the working directory.

The account is now a multisig. However, to check that everything went correctly, you can call:

```
chr query -bc ec -d testnet ft4.get_account_auth_descriptors id=<account ID>

```

This will return all auth descriptors for the account. If you followed the guide, you should only see one auth descriptor. It might be possible that you already had multiple auth descriptors on your account, which would mean that you now see multiple entries in the output of this command. The other auth descriptors can be removed, if you wish to do so. There is a guide to remove auth descriptors later in this page.

Take note of the auth descriptor ID in the output. In my case:

```
$ chr query -bc ec -d testnet ft4.get_account_auth_descriptors id=885A6648772AB9615CCD88D25201402F734B43EF20DA959623591B0DCA5098C5

[
  [
    "account_id": x"885A6648772AB9615CCD88D25201402F734B43EF20DA959623591B0DCA5098C5",
    "args": [
      [
        "A",
        "T"
      ],
      2,
      [
        x"03F5A2656BC6DB03A8F85C2FD3FD32D72515D1435BB0869A28FFB49185E23E25D6",
        x"02AB13E8808E8AC7A8CAD65660A189FF3744063D6B48C4BC5236F44F98A333CD41",
        x"03B7EF6C5948022298051E57521CE56075A16A883516240021BD337C8A6E7DDD13"
      ]
    ],
    "auth_type": "M",
    "created": 1731502619990,
    "id": x"CF59942E080205C7FE6CC3164D868D401EA2094A77104E19484363483E4808B5",
    "rules": null
  ]
]

```

The ID is `CF59942E080205C7FE6CC3164D868D401EA2094A77104E19484363483E4808B5`. You will need it to call every operation from now on.

## Using a multisig account​

To send any transaction from this account, you will need to follow a multi-step process:

- define the signers 
- build the transaction 
- sign the transaction 
- send the transaction 
The last three steps are always the same. To define the signers, however, you need some additional information:

- if the operation is adding a new auth descriptor, all signers for the new auth descriptor need to be added to the list of signers; 
- you always need at least two of Alice, Bob and Charlie in the list of signers. 
The requirement of 2 among the three owners was defined in the Create your new auth descriptor step.

We will see three example operations:

- a normal operation, transferring funds; 
- adding a new auth descriptor; 
- removing an auth descriptor. 
### Transferring funds​

The transfer operation is defined as:

```
ft4.transfer(receiver_id: byte_array, asset_id: byte_array, amount: big_integer)

```

We will send 1tCHR to account `31FCEE95ADC8D7E9806923C31A8DF3D38150623983BD96DBE87B5B653B977233`. This operation, on the `chr` CLI tool, will look like this:

```
ft4.transfer -- x\"31FCEE95ADC8D7E9806923C31A8DF3D38150623983BD96DBE87B5B653B977233" x\"2AF2053C9CFD1BA030E01EA501DFD86156BB6247E7981AF93E3364B0D2C4F2AC\" 1000000L

```

Note that, having six decimals, 1000000L means 1.000000 tCHR, or 1 tCHR.

Supposing Bob is not available to sign, edit your `signers` file or create a new one with the following content:

```
Alice=03F5A2656BC6DB03A8F85C2FD3FD32D72515D1435BB0869A28FFB49185E23E25D6
# Bob=02AB13E8808E8AC7A8CAD65660A189FF3744063D6B48C4BC5236F44F98A333CD41
Charlie=03B7EF6C5948022298051E57521CE56075A16A883516240021BD337C8A6E7DDD13

```

Either Alice or Charlie can now build the transaction. Notice how we added the new auth descriptor ID after the `-id` flag:

```
chr multi-signature create -d testnet -bc ec \
    --ft-auth --signers-file signers \
    -id CF59942E080205C7FE6CC3164D868D401EA2094A77104E19484363483E4808B5 \
    ft4.transfer -- x\"31FCEE95ADC8D7E9806923C31A8DF3D38150623983BD96DBE87B5B653B977233\" x\"2AF2053C9CFD1BA030E01EA501DFD86156BB6247E7981AF93E3364B0D2C4F2AC\" 1000000L

```

The file can be sent to the missing signer, who can view, sign and send the transaction with the usual commands:

```
chr multi-signature view --file=transfer
chr multi-signature sign --file=transfer --target .
chr multi-signature send --file=transfer_signed -d testnet -bc ec

```

### Adding a new auth descriptor​

I want to give Dan access to my account. Dan should be able to send tokens, but only after June 19th, 2029 (timestamp in millis: 1876543210123)

I first need to ask Dan what is his pubkey. He creates a keypair and sends me the pubkey Dan: `035C9040EB3FF858BDA4FB0FF8737A02EFDA2CD74AD07BB5A2F18217795BBF67AC`

Then I can create an auth descriptor as described above:

```
[
  0,
  [
    ["T"],
    x"035C9040EB3FF858BDA4FB0FF8737A02EFDA2CD74AD07BB5A2F18217795BBF67AC"
  ],
  ["gt", "block_height", 1876543210123]
]

```
note
Note that this auth descriptor has some differences: 
- `0` instead of `1`, as it allows Dan to access the account by himself; that means that this is a single signature auth descriptor; 
- `"T"`, but not `"A"`, as we don't want Dan to be able to edit the account settings. Dan is only allowed to access the funds of the account; 
- there's no "required signatures" line, as the auth descriptor is singlesig; 
- `["gt", "block_height", 1876543210123]` instead of `null`. This only allows Dan to access the account after the specified timestamp (in millis). 
Check out the FT4 section of the documentation if you need more informations about any of these changes. 
The signers file should now contain Dan as well, as we need him to prove that he owns the key he gave us:

```
Alice=03F5A2656BC6DB03A8F85C2FD3FD32D72515D1435BB0869A28FFB49185E23E25D6
# Bob=02AB13E8808E8AC7A8CAD65660A189FF3744063D6B48C4BC5236F44F98A333CD41
Charlie=03B7EF6C5948022298051E57521CE56075A16A883516240021BD337C8A6E7DDD13
Dan=035C9040EB3FF858BDA4FB0FF8737A02EFDA2CD74AD07BB5A2F18217795BBF67AC

```

Bob can still be left commented out, since we only need two out of three signers on the current auth descriptor.

Alice (or Charlie) needs to build this operation:

```
chr multi-signature create -d testnet -bc ec \
    --ft-auth --signers-file signers \
    -id CF59942E080205C7FE6CC3164D868D401EA2094A77104E19484363483E4808B5 \
    ft4.add_auth_descriptor -- '[0, [["T"],x"035C9040EB3FF858BDA4FB0FF8737A02EFDA2CD74AD07BB5A2F18217795BBF67AC"],["gt","block_height",1876543210123]]'

```

Dan cannot build this operation, as he's not (yet) allowed to operate for this account, and the `chr` CLI tool will prevent him from starting this operation.

After this, the steps are all the same. After Alice built the transaction, she can send it to either Charlie or Dan. They can view the transaction and sign it as discussed above, then send it to the last signer. After everyone has signed, the transaction can be broadcasted online.

The query defined at the end of the Send the transaction chapter can be used to verify that Dan can now access the account:

```
$ chr query -bc ec -d testnet ft4.get_account_auth_descriptors id=885A6648772AB9615CCD88D25201402F734B43EF20DA959623591B0DCA5098C5
[
  [
    "account_id": x"885A6648772AB9615CCD88D25201402F734B43EF20DA959623591B0DCA5098C5",
    "args": [
      ["A", "T"],
      2,
      [
        x"03F5A2656BC6DB03A8F85C2FD3FD32D72515D1435BB0869A28FFB49185E23E25D6",
        x"02AB13E8808E8AC7A8CAD65660A189FF3744063D6B48C4BC5236F44F98A333CD41",
        x"03B7EF6C5948022298051E57521CE56075A16A883516240021BD337C8A6E7DDD13"
      ]
    ],
    "auth_type": "M",
    "created": 1731502619990,
    "id": x"CF59942E080205C7FE6CC3164D868D401EA2094A77104E19484363483E4808B5",
    "rules": null
  ],
  [
    "account_id": x"885A6648772AB9615CCD88D25201402F734B43EF20DA959623591B0DCA5098C5",
    "args": [
      ["T"],
      x"035C9040EB3FF858BDA4FB0FF8737A02EFDA2CD74AD07BB5A2F18217795BBF67AC"
    ],
    "auth_type": "S",
    "created": 1731507011141,
    "id": x"C38D2D5EBD3835263148893C7F432CCF66D6D43A7BE031DA73630B2C9645426E",
    "rules": [
      "gt",
      "block_height",
      1876543210123
    ]
  ]
]

```

Dan's auth descriptor is the last one, with ID `C38D2D5EBD3835263148893C7F432CCF66D6D43A7BE031DA73630B2C9645426E`.

### Remove an auth descriptor​

If I want to remove Dan's access, I first need to know what is his auth descriptor ID. In the previous chapter we found out it was `C38D2D5EBD3835263148893C7F432CCF66D6D43A7BE031DA73630B2C9645426E`.

After making sure that the signer file only contains two of Alice, Bob and Charlie:

```
# Alice=03F5A2656BC6DB03A8F85C2FD3FD32D72515D1435BB0869A28FFB49185E23E25D6
Bob=02AB13E8808E8AC7A8CAD65660A189FF3744063D6B48C4BC5236F44F98A333CD41
Charlie=03B7EF6C5948022298051E57521CE56075A16A883516240021BD337C8A6E7DDD13

```

Bob can build this transaction to remove Dan's access:

```
chr multi-signature create -d testnet -bc ec \
    --ft-auth --signers-file signers \
    -id CF59942E080205C7FE6CC3164D868D401EA2094A77104E19484363483E4808B5 \
    ft4.delete_auth_descriptor -- 'x"C38D2D5EBD3835263148893C7F432CCF66D6D43A7BE031DA73630B2C9645426E"'

```

After Charlie approves it, it can be broadcasted to the chain.

If there were multiple auth descriptors to remove, a more drastic approach can be to call:

```
chr multi-signature create -d testnet -bc ec \
    --ft-auth --signers-file signers \
    -id CF59942E080205C7FE6CC3164D868D401EA2094A77104E19484363483E4808B5 \
    ft4.delete_all_auth_descriptors_except_main

```

The above query can again be used to verify what auth descriptors are present on this account.
- Example scenario
- Optional: Account registration
- Setup
- Registration
- Starting scenario
- Key generation
- Switching to multisig
- Configure the CLI
- Find your current auth descriptor
- Create your new auth descriptor
- Create the transaction
- Sign the transaction
- Send the transaction
- Using a multisig account
- Transferring funds
- Adding a new auth descriptor
- Remove an auth descriptor