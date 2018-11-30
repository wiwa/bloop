---
id: usage
title: Usage Guide
sidebar_label: Usage Guide
---

> **Pre-requirement**: [install bloop](/setup) and [learn the Basics](what-is-bloop.md) before reading the
> usage guide.

To use bloop you first need to generate Bloop configuration files from your build tool. These
configuration files are JSON objects that define a build, its build graph, its inputs and its
outputs. Next is an example of the simplest bloop configuration file possible.

```json
{
    "version" : "1.0.0",
    "project" : {
        "name" : "foo",
        "directory" : "/disk/foo",
        "sources" : ["/disk/foo/src/main/scala"],
        "dependencies" : [],
        "classpath" : ["/disk/foo/library/scala-library.jar"],
        "out" : "/disk/foo/target",
        "classesDir" : "/disk/foo/target/classes"
    }
}
```

This information needs to be extracted from every build tool to make bloop work for any Scala
and Java build.

## Supported Build Tools

Bloop extracts build information from several build tools with varying degree of functionality.

|                          | sbt        | Gradle   | Maven    | mill       | Bazel | Pants | Fury |
| ------------------------ | ---------- | -------- | -------- | ---------- | ----- | ----- | ---- |
| **Build Export**         | ✅         | ✅        | ✅ __*__ | ✅         |  ❌    |   ❌  | ✅   |
| **Built-in Integration** | 📅 v1.1.1  |          |          | 📅 v1.1.1  |       |       |      |

> __*__ Bloop can export Maven build, however the build plugin is not fully developed yet. If you
want to help out, have a look [at the issue
tracker](https://github.com/scalacenter/bloop/issues?q=is%3Aissue+is%3Aopen+maven+label%3Amaven)
and say hi in our [Gitter channel](https://gitter.im/scalacenter/bloop).

### Exporting Your Build

"Build Export" is the simplest supported use case. It consists of two sequential steps:

1. Generating bloop configuration files from the build.
1. Running bloop CLI commands once the build generation is ready.

The completions of these steps provides a quick developer workflow, provided that clients manually
perform the first step whenever the files are missing or a change in the build tool occurs. The
second step can be manual or automatic depending on the build client leveraged by the users.

For example, once a build has already been exported, you can recompile a project via the CLI every
time there is a file change with the following command.

```bash
$ bloop test foo -w
Compiling foo (2 Scala sources)
Compiled foo

Watching 6 directories... (press Ctrl-C to interrupt)
```

### Built-in Integration

Exporting your build is supported by a large array of popular Scala and Java build tools. However,
it is a tedious task that users must remember to run from time to time.

Future versions of bloop will incorporate built-in integrations where running `compile`, `test` or
`run` on a supported build tool will dispatch a build request to bloop's build server. Such
integration will relieve users from exporting the build manually.

## Supported Editor Integrations

Bloop implements the [Build Server Protocol (BSP)](https://github.com/scalacenter/bsp) and provides
compilation diagnostics to editors and IDEs. There are two main editor integrations:

1. [IntelliJ](https://www.jetbrains.com/idea/): the most popular Scala and Java IDE has an experimental
   BSP integration that connects to bloop to fetch compiler diagnostics. IntelliJ users must export
   the build every time there is a build change.

1. [Metals](https://github.com/scalameta/metals): a language server for Scala. Metals features the best
   Bloop integration in the Scala community. It generates the configuration files automatically
   whenever a change is detected in sbt builds.

