# Vortex

Solutions to the [Gossip Glomers](https://github.com/jepsen-io/maelstrom)
distributed systems challenges.

Each solution consists of a toy implementation of a distributed system that runs
over [Maelstrom](https://github.com/jepsen-io/maelstrom).

## Building and running

Install Maelstrom's [system dependencies](https://github.com/jepsen-io/maelstrom/blob/main/doc/01-getting-ready/index.md):

```sh
brew install openjdk graphviz gnuplot
```

Build and run the solution to the Echo challenge:

```sh
./gradlew :echo:runMaelstrom
```
