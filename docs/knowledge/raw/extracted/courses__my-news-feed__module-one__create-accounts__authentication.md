# Authentication with FT4 accounts

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- [Lesson 2 - Create accounts](/courses/my-news-feed/module-one/create-accounts/)
- Authentication with FT4 accounts
# Authentication with FT4 accounts

In this section, authentication with FT4 accounts will be explored.

Authentication with FT4 accounts is simple and efficient. First, import the auth module:

src/news_feed/module.rell
```rell
import lib.ft4.auth;
```

Next, a call to auth.authenticate is added in the operations:

src/news_feed/operations.rell
```rell
operation make_post(content: text) {  val account = auth.authenticate();  require(content.size() 
The auth.authenticate function handles authentication, streamlining the process and enhancing security. The user’s public key no longer needs to be passed to the operations.

To activate authentication, register an [auth handler](https://docs.chromia.com/ft4/backend/authentication/auth#the-authenticate-function). Add the following code to your Rell module:

src/news_feed/auth.rell
```rell
@extend(auth.auth_handler)function () = auth.add_auth_handler(  flags = ["S"]);
```

This extension function registers a new handler with the "S" flag,
matching the flag used during account creation.
It registers globally, enabling it to work with all operations that utilize the auth.authenticate function
in your app. If you wish to assign a specific handler for a particular operation or module,
you can include a scope argument in the auth.add_auth_handler.
