---
id: integration
title: Integrate with the Build Server
sidebar_label: Integration guide
---

> Skip this guide if you're not interested in integrating with Bloop's build server.

<div class="diagram">
  <p>
    <img src="/bloop/img/bloop-architecture-diagram.svg" alt="Bloop architecture diagram">
  </p>
</div>

The above diagram shows the only two ways to integrate with bloop:

1. via the **Build Server Protocol**
1. via the **Command-Line Application**

## Build Server Protocol (BSP)

The [Build Server Protocol][bsp] is a work-in-progress protocol designed by the [Scala
Center](https://scala.epfl.ch) and [JetBrains](https://www.jetbrains.com/). Bloop is the first build
server to implement the protocol and provide build supports to clients such as
[Metals](https://scalameta.org/metals/) and [IntelliJ](https://www.jetbrains.com/idea/)

At the moment, Bloop v1.1.0 partially implements version 2.0.0-M2 and Bloop v1.0.0 implements
version 1.0.0. If you want to implement a compatible build client, check out [the protocol
specification](https://github.com/scalacenter/bsp/blob/master/docs/bsp.md). As a client, the
protocol gives you fine-grained build and action information and it's more suitable for rich clients
such as build tools or editors.

There exist two published libraries to implement a BSP client.

1. A Java implementation ([ch.epfl.scala:bsp4j:2.0.0-M2](https://github.com/scalacenter/bsp/tree/master/bsp4j/src/main))
1. A Scala implementation ([ch.epfl.scala:bsp4s:2.0.0-M2](https://github.com/scalacenter/bsp/tree/master/bsp4s/src/main))

## Command-Line Application (CLI)

Build clients interested in integrating with bloop can shell out to the bloop CLI to implement a
quick and simple integration. The CLI exposes [a simple command interface][cli-reference] to cover most of the needs
of a client. It's suitable for lightweight clients such as custom tools or scripts, even though it
can also be leveraged directly from build tools.

## Reading or Writing Bloop Configuration Files

If you want to use bloop from your own build tool or create a custom integration (for example,
writing build scripts or relying on bloop to compile source code in unit or integration tests), you
will need to learn to generate bloop configuration files.

The bloop configuration file format has a well-specified JSON schema describing all the inputs
required by the build server. Its contents are machine-dependent and the file is not designed to
be shared by different machines or developers.

<script src="/scripts/docson/widget.js" data-schema="/bloop-schema.json">
</script>

> Download the JSON schema from [this file](/bloop-schema.json).

### Generating Configuration Files

To manage the configuration file format in Scala, you can use the `bloop-config` module. This module
is published to Maven Central and implements encoders and decoders to read and write configuration
files.

<pre><code class="language-scala hljs scala">libraryDependencies += <span class="hljs-string">"ch.epfl.scala"</span> % <span class="hljs-string">"bloop-config"</span> % <span class="hljs-string">"<span class="latest-version">1.1.0</span>"</span></code></pre>

If, for example, you want to generate configuration files:

1. Create an instance of `bloop.config.Config.File` and populate all its fields.
2. Write the json file to a `target` path with `bloop.config.Config.File.write(config, target)`.

## Manipulating JSON Files with Scripts

As the build is exported to well-documented JSON configuration files, developers can write build
scripts to quickly perform a build task locally or in the CI using the same project model. To
manipulate bloop configuration files via scripts, we recommend using either of the following tools:

1. [gron](https://github.com/tomnomnom/gron) to simply grep JSON files.
2. [jq](https://stedolan.github.io/jq/) to process and extract in-depth JSON data.

[bsp]: https://github.com/scalacenter/bsp
