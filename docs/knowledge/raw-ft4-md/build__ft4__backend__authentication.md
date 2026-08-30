
# Authentication

FT4 authentication utilizes auth descriptors to define key pairs, permissions, and multi-signature security. Custom auth handlers enable flexible user authorization across FT and EVM signers. Multi-signature transactions require multiple approvals and can be managed using chr commands for creation, signing, and sending.
Set up auth descriptors
Auth descriptors in FT4 define key pairs and permissions for blockchain accounts, enabling multi-signature security and access control. They include rules for when the descriptor is valid, ensuring secure account management.Auth handlers and authentication

FT4 authentication is enabled using auth.add_auth_handler, defining operation scopes and flags. Custom authentication messages and resolvers allow flexible and secure user authorization across different signers (FT and EVM).
Handle multi-sig transactions
Multi-signature transactions require multiple approvals, created using chr multi-signature create with a signers file. Signatures are added using chr multi-signature sign and sent with chr multi-signature send after all signatures are collected.