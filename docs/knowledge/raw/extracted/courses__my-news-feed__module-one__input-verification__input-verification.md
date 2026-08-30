# Verify inputs

URL: https://learn.chromia.com

- [Home](/)
- [Module 1 - Create a Rell backend app with FT accounts](/courses/my-news-feed/module-one/)
- [Lesson 4 - Input verification and validation](/courses/my-news-feed/module-one/input-verification/)
- Verify inputsOn this page
# Verify inputs

In this section, input verification will be explored and operations will be secured by preventing impersonation. The [require function](https://docs.chromia.com/rell/language-features/systemlib/require-function) will be used as part of this process.

## Input validation​

To prevent impersonation, input verification will be added to the make_post and follow_user operations

A length requirement will be enforced on the content provided to make_post:

src/news_feed/operations.rell
```rell
require(content.size() 
Next, it needs to be verified that the user exists in the database. The database query will be made nullable and the result required to exist:

```rell
val user = require(user @? { user_id }, "User with id %s does not exist".format(user_id));
```

To further reduce duplication, a function will be created for this as well:

src/news_feed/module.rell
```rell
function require_user(id: byte_array) = require(user @? { id }, "User with id %b does not exist".format(id));
```

The modified operations are shown below:

src/news_feed/operations.rell
```rell
operation make_post(user_id: byte_array, content: text) {    val account = auth.authenticate();    require(content.size() 
The require_user function will also be added to follower queries:

src/news_feed/queries.rell
```rell
query get_followers_count(user_id: pubkey): integer {  return follower @ { .user == require_user(user_id) } (@sum 1);}query get_following_count(user_id: pubkey): integer {  return follower @ { .follower == require_user(user_id) } (@sum 1);}
```

To use the require_user function in get_user_name and get_users, the following function is defined:

src/news_feed/module.rell
```rell
function format_user(user) = "%s#%s".format(user.name, user.id.to_hex().sub(0, 5));
```

Then, the queries will be adjusted accordingly:

src/news_feed/queries.rell
```rell
query get_user_name(user_id: byte_array): text {  return format_user(require_user(user_id));}query get_users(pointer: integer, n_users: integer) {  val users = user @* {} (name = format_user($), id = .id) offset pointer limit n_users;  return (    pointer = pointer + users.size(),    users = users  );}
```

The final query, get_posts, does not require additional handling because the join statement returns zero values if a user is not found.
