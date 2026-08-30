# Lesson 1 - Set up the Frontend Application

URL: https://learn.chromia.com

- [Home](/)
- [Module 3 - Frontend application](/courses/ft4-demo-app/module-frontend-application/)
- Lesson 1 - Set up the Frontend ApplicationOn this page
# Lesson 1 - Set up the Frontend Application

Stay in the root folder of the frontend application where package.json is located.

### Install dependencies​

Use pnpm to install all required packages:

```bash
pnpm install
```

### Configure the environment​

Create a .env file based on .env.example and fill it with the appropriate values:

```bash
NEXT_PUBLIC_NODE_URL=http://localhost:7740NEXT_PUBLIC_BRID=
```

- 
NEXT_PUBLIC_NODE_URL – URL of the Chromia blockchain node.

- 
NEXT_PUBLIC_BRID – BRID of your locally or remotely running dapp.

#### Running the application​

After installing dependencies and configuring the .env file, run the application locally:

```bash
pnpm dev
```

The app will be available at [http://localhost:3000](http://localhost:3000).
