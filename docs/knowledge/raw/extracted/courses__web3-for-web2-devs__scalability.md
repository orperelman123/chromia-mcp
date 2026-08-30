# Scalability

URL: https://learn.chromia.com

- [Home](/)
- Lesson 7 - ScalabilityOn this page
# Scalability

Scaling in Web3 looks different than scaling in Web2, and the decentralized nature of dapps introduces new challenges. In this section, we'll briefly outline some of these challenges and look at how they're more easily alleviated on the Chromia platform than on other Web3 platforms.

## Potential scalability issues in Web3​

On Web3 platforms like Ethereum, the network gets congested during peaks in activity. Limited block space means that high demand can lead to a backlog of unconfirmed transactions and a fee surge. This can happen due to ICOs (Initial Coin Offerings) unrelated to your dapp, new token launches, or other events that lead to a sudden increase in transactions being broadcast to the network.

## How Chromia solves network congestion​

Every dapp on Chromia exists on its own sidechain, capable of handling hundreds of transactions per second. Each sidechain runs in a so-called container, part of a cluster. If a dapp grows, it can deploy additional sidechains to its container or even run multiple containers on multiple clusters.
This means your dapp can scale and keep throughput high as it becomes more popular.

The sidechain architecture also means that your dapp is protected from surges in activity from other dapps. Since your dapp is isolated, with its own blockchain and resources, you don't need to worry about congestion due to activity on the rest of the network.

## Scaling in Web2​

- Applications can often scale by adding more servers or resources to centralized infrastructure.

- Mature infrastructure makes handling high amounts of traffic and data easier.

- A bottleneck at a single point can lead to system-wide slowdowns.

## Scaling in Web3​

- Network congestion occurs on some platforms when there's high demand.

- Potential scalability issues due to consensus mechanisms and design.

- Chromia's sidechain architecture solves the network congestion problem, allowing dapps to scale.

## Scalability example​

Let's take the example of a social media app like Reddit. As the number of users, views, and posts increases, the load on the system increases. In Web2, we can add resources on the backend to handle the load, but in Web3, it can be challenging. On platforms like Ethereum, you can work to optimize your smart contract, but that can only go so far. We're limited to the network's resources and the throughput it can handle.

On Chromia, on the other hand, we've the option to scale horizontally. We can deploy additional sidechains as the number of users and posts increases. These are logical replicas of our dapp, but they each have their own blockchain.

To use this strategy, we need to build our dapp architecture to consider this. In our Reddit-like example, we can put different subreddits (forum topics) on different sidechains, each with its own resources. The client can then manage and direct requests for specific subreddits to their respective sidechains. This way, we've load-balanced our dapp and have much better prospects of scaling than on other Web3 platforms.

Next, we'll look at security and see how Web3 is inherently more secure than Web2.
