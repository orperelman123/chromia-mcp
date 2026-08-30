# Comparing backends

URL: https://learn.chromia.com

- [Home](/)
- Lesson 3 - Comparing backendsOn this page
# Comparing backends

Now, it's time to get practical. Let's illustrate the differences and similarities between a traditional Web2 app and a Chromia dapp using real code examples. We'll learn some Chromia development concepts along the way. Still, for an actual deep dive into dapp development, you may later want to check out our [intro to dapp development course](https://learn.chromia.com/courses/book-review/introduction) or our [advanced dapp development course](https://learn.chromia.com/courses/my-news-feed/introduction).

For our code examples, we'll use a highly simplified version of the dapp you built in the advanced dapp development course; [a social media news feed dapp](https://learn.chromia.com/courses/my-news-feed/introduction/). To keep our examples short, all we'll do is create users, allow users to create posts, and fetch a list of posts.

Let's examine the practical differences between building this app for Web2 and Web3 on Chromia. We'll start with the backend, first reminding ourselves of what a simple Web2 backend looks like and then compare that to developing on Chromia.

## Web2 backend with Node.js, Express, and PostgreSQL​

There are countless tech stacks you can use to build a Web2 app. In this example, we'll use Node.js, Express, and PostgreSQL, but the same principles would apply if we used other frameworks or other languages.

To create our backend, we'd first set up a PostgreSQL database and host it somewhere, for example, on AWS RDS or Google Cloud SQL.

Once we have a database, we can create a Node.js service that connects to it using the Node.js PostgreSQL driver or an ORM (Object Relational Mapper) such as [Sequelize](https://sequelize.org/). With Sequelize, it would look something like this:

```javascript
// db.jsimport { Sequelize, DataTypes, Model } from "sequelize";const sequelize = new Sequelize(  "postgres://username:password@localhost:5432/mydatabase",  {    dialect: "postgres",    logging: false,  });// User Modelclass User extends Model {}User.init(  {    name: { type: DataTypes.STRING, allowNull: false },    id: { type: DataTypes.INTEGER, autoIncrement: true, primaryKey: true },  },  { sequelize, modelName: "User", timestamps: false });// Post Modelclass Post extends Model {}Post.init(  {    timestamp: { type: DataTypes.DATE, defaultValue: DataTypes.NOW },    userId: {      type: DataTypes.INTEGER,      references: { model: User, key: "id" },    },    content: { type: DataTypes.TEXT, allowNull: false },  },  {    sequelize,    modelName: "Post",    timestamps: false,    indexes: [{ fields: ["userId"] }],  });// RelationsUser.hasMany(Post, { foreignKey: "userId" });Post.belongsTo(User, { foreignKey: "userId" });export default { sequelize, User, Post };
```

Here, we've a User model and a Post model. This is a naïve example, of course. An actual app would typically have more models, but as mentioned, we'll keep things simple to compare Web2 and Web3 on Chromia.

Next, we need to expose an API that allows for creating users and posts and fetching a list of users. We'll use the popular JavaScript web framework [Express](https://expressjs.com/) for this:

```javascript
// server.jsimport express from "express";import bodyParser from "body-parser";import { User, Post } from "./db.js";const app = express();const PORT = 3000;app.use(bodyParser.json());// Create a userapp.post("/users", async (req, res) => {  try {    const user = await User.create(req.body);    res.status(201).send(user);  } catch (error) {    res.status(400).send(error.message);  }});// Create a postapp.post("/posts", async (req, res) => {  try {    const post = await Post.create(req.body);    res.status(201).send(post);  } catch (error) {    res.status(400).send(error.message);  }});// Fetch a list of users with simple paginationapp.get("/users", async (req, res) => {  try {    const limit = req.query.limit ? parseInt(req.query.limit, 10) : 10;    const offset = req.query.offset ? parseInt(req.query.offset, 10) : 0;    const { count, rows } = await User.findAndCountAll({      limit: limit,      offset: offset,    });    res.status(200).send({ total: count, data: rows });  } catch (error) {    res.status(500).send(error.message);  }});app.listen(PORT, () => {  console.log(`Server is running on port ${PORT}`);});
```

We'll skip authentication and authorization for now, and return to that later in this course.

Now, we can deploy our Node.js service to a cloud hosting provider such as AWS, Google Cloud, or Heroku.

## Web3 backend with Chromia entities and operations​

To do the same thing for Web3 on Chromia, we need to write a dapp backend in the Rell programming language. We don't need to worry about hosting a database. That's taken care of behind the scenes by the Chromia platform.

Let's define our data model in Rell:

```rell
entity user {  mutable name;  key id: byte_array;}entity post {  timestamp = op_context.last_block_time;  index user;  content: text;}
```

Deploying this code will automatically create the necessary tables in Chromia's distributed relational database. Chromia uses PostgreSQL under the hood, but you don't interact with it directly. Instead, servers (nodes) that run Chromia core software interpret Rell code and translate it into SQL queries. That way, the database is kept secure and individual dapps can only update their own data.

Now, let's expose an API that allows us to create users and posts, as well as fetch a list of users:

```rell
operation create_user(name, pubkey) {  create user(name, pubkey);}operation make_post(user_id: byte_array, content: text) {  create post(    user @ { user_id },    content  );}query get_users(pointer: integer, n_users: integer) {  val users = user @* {} ( .name ) offset pointer limit n_users;  return (    pointer = pointer + users.size(),    users = users  );}
```

In Rell, an operation mutates data in the database. A query is read-only and used to fetch data.

That's it! Operations and queries in Rell are automatically exposed. They can be called by sending transactions or queries to our news feed dapp on the Chromia network.

Learn more about RellIf you want a more in-depth explanation of this Rell code, there's a [whole course](/courses/my-news-feed/introduction) on building a Chromia Dapp that expands on this example.

Dapp code is deployed to the Chromia Network using the Chromia CLI. To read about the Chromia CLI and how to deploy to a network, check out the [Chromia Docs](https://docs.chromia.com/).
