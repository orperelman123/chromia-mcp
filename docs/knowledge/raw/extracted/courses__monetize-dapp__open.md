# Open strategy

URL: https://learn.chromia.com

- [Home](/)
- Lesson 4 - Open strategyOn this page
# Open strategy

The open strategy allows completely free account creation without any token transfers. Users can register accounts directly without any prerequisites using the register_account() operation.

This strategy provides the simplest approach to account registration, requiring no token transfers or fees for account activation.

Production limitationsThe open strategy is prone to abuse and is not suitable for long-term production use. It allows unlimited account creation without any barriers, making it vulnerable to spam attacks.

Recommended use: Development and testing phases only.

To use safely in production:

- Implement additional rate limiting or validation mechanisms

- Monitor for potential spam patterns

- Consider implementing user verification systems

### Code example for open strategy​

To develop your dapp from the template, refer to the [code examples](https://bitbucket.org/chromawallet/fee-samples/src/main/open/) and corresponding tests. Refer to the [tests](https://bitbucket.org/chromawallet/fee-samples/src/main/open/rell/src/test/) for testing the open strategy implementation.

No configuration neededThe open strategy requires no additional configuration in chromia.yml. The FT4 library handles the open account creation automatically without any special setup. For the complete chromia.yml configuration including blockchain setup, FT4 library configuration, and other required settings, refer to the [actual configuration file](https://bitbucket.org/chromawallet/fee-samples/src/main/open/rell/chromia.yml) in the repository.
