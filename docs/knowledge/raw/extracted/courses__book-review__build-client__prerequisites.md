# Prerequisites

URL: https://learn.chromia.com

- [Home](/)
- [Lesson 6 - Build the client](/courses/book-review/build-client/)
- PrerequisitesOn this page
# Prerequisites

This tutorial will guide you through each step, from setting up your development environment to understanding the code.

Before we begin coding, let's ensure your development environment is properly configured.

## Step 1: Install Node.js and set up the project​

- 
Install Node.js: If you don't have Node.js installed, download and install it from the [official website](https://nodejs.org/). Node.js allows you to run JavaScript on the server side.

- 
Create a project directory: Open your terminal, navigate to the directory where you want to create your project, and run the following commands:

```shell
mkdir chromia-book-review-democd chromia-book-review-demo
```

- 
Initialize a Node.js project: Run the following command to create a package.json file:

```shell
npm init -y
```

## Step 2: Install required packages​

Now that your project is set up, you'll need to install some essential packages:

```shell
npm install typescript postchain-client readline @types/node
```

- typescript: This package provides TypeScript support.

- postchain-client: This package contains tools for interacting with the Chromia blockchain.

- readline: This core Node.js module helps read user input from the command line.

## Step 3: Configure TypeScript​

To use TypeScript in your project, you need to create a configuration file named tsconfig.json. You can generate one with the following command:

```shell
npx tsc --init
```

This command creates a default tsconfig.json file, which you can customize according to your project's requirements.

## Step 4: Integrate the code​

Now that you have installed the necessary packages, it's time to integrate the code into your project. Follow these steps:

- 
Create a TypeScript file named book_review.ts in your project directory. For now, this file will be empty, but we will add code to it in the upcoming sections.

- 
To run your TypeScript code, it must first be transpiled to JavaScript. Execute the following command:

```shell
npx tsc
```

This command generates a book_review.js file containing the compiled JavaScript code.

- 
Finally, run your code using Node.js:

```shell
node book_review.js
```

This will execute your TypeScript code and interact with the Chromia blockchain as specified.

Congratulations! You have successfully set up your development environment, installed the necessary packages, and integrated TypeScript into your project. In the next section, we will explore an example of our client.
