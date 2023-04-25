# Vortex

Solutions to the [Gossip Glomers](https://github.com/jepsen-io/maelstrom)
distributed systems challenges.

Each solution consists of a toy implementation of a distributed system that runs
over [Maelstrom](https://github.com/jepsen-io/maelstrom).

## Building and running

Install Maelstrom prerequisites:

```sh
brew install openjdk graphviz gnuplot
```

Download and extract Maelstrom:

```sh
curl -L https://github.com/jepsen-io/maelstrom/releases/download/v0.2.3/maelstrom.tar.bz2 | tar xf -
```

Build the project binaries:

```sh
./gradlew installDist
```

Run the application against the `echo` workload:

```sh
./maelstrom/maelstrom test -w echo --bin ./app/build/install/app/bin/app --node-count 1 --time-limit 10
```
