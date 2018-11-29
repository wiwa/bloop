---
id: basics
title: Learn the Basics
sidebar_label: Learn the Basics
---

Bloop is a Scala build server developed by [the Scala Center][scalacenter]. It has three main goals:

1. Compiles, tests and runs Scala code as fast as possible
1. Is an independent long-running process that can be reused by multiple clients
1. Integrates easily with build tools, command-line applications, editors and custom tooling

## Features

1. Compiles a vast array of Java and Scala (2.10.x, 2.11.x, 2.12.x and 2.13.x) versions
1. Runs and tests on the Java, [Scala.js](https://www.scala-js.org/) and [Scala Native](https://github.com/scala-native/scala-native) runtimes
1. Integrates with common build tools in the Scala ecosystem (sbt, Gradle, etc)

## How does it work

The core component of bloop is the build server. The build server is responsible of implementing the
logic to compile, test and run Scala and Java projects. It is designed to run on the background for
long periods of time (as long as your machine is running) and assist all client's requests.

Clients can connect to the build server in two ways:

1. via the [Nailgun server protocol](https://github.com/facebook/nailgun), used by the built-in command-line application
1. via the [Build Server Protocol (BSP)](https://github.com/scalacenter/bsp), used by clients like [Metals](https://github.com/scalameta/metals) or [IntelliJ](https://www.jetbrains.com/idea/)

<div class="diagram">
  <p>
    <img src="/img/bloop-architecture-diagram.svg" alt="Bloop architecture diagram">
  </p>
</div>


<blockquote>
  Interested in integrating with Bloop? Read the integration guide.
</blockquote>

## The principles

The lack of clear these design principles in the past tooling has hindered progress in the Scala
community, complicated maintenance and worsened the Scala user experience with, for example, slower
compiles. Bloop aims to address such problems with the following principles.

### Implement Once, Optimize Once, Use Everywhere

Every developer tool supporting Scala needs to compile, test and run Scala code. These are basic
features that both old and new tools alike require, from build tools to IDEs/editors, in-house
tooling and custom scripts.

Maintaining custom integrations per client is a tedious task for the close-knit community of Scala
tooling contributors. In practice, contributors repeat the same learning process, duplicate the
integration logic per integration, have trouble optimizing and benchmarking compilation consistently
across all clients and have no way to measure how good the Scala user experience is.

By centralizing the implementation of basic Scala features in a build server, users benefit from the
best of the user experiences whereas maintainers can share one implementation across different
clients and have better tools to track performance, reliability and success metrics.

### Outlive build clients

There are many build clients and they are usually short-lived.

When JVM-based tooling lives in relatively short-lived processes

### Optimize for the most common scenarios

Edit, compile and test workflows are the bread and butter of software
development and a tight feedback loop helps developers be more productive.
Slow Scala compile times can often be attributed to the slugishness of our
build tool, and `bloop` aims to address that.

### Hot compilers

The primary reason why Bloop brings you a tight developer workflow is because
it caches hot compilers across all developer tools (IDEs, build tools,
terminals).

A hot compiler is a long-running compiler that has been optimized by
the JVM's [JIT](https://en.wikipedia.org/wiki/Just-in-time_compilation) and
therefore it reaches peak performance compiling code. The performance
difference between a cold and hot compiler varies, though the variation is usually
an order of magnitude in size — a hot compiler can be between 5x and 20x
faster than a cold compiler.

Keeping hot compilers alive is crucial to be productive. However, it is a
difficult task; compilers must outlive short- and long-running build
processes, and there is no existing way to share them across different build
tools and IDEs.

<span class="label success upper">Take away</span> Bloop is an
build-tool-agnostic build server that makes hot compilers accessible to all
your toolchain — IDEs, build tools and terminals get peak performance at all
times.

#### A better default for compiler lifetime

By supporting popular build tools out of the box and different scenarios,
Bloop enforces by design best practices to be productive. It therefore
addresses the limitations of many build tools in the Scala ecosystem whose
infrastructure does not allow them to keep hot compilers alive.

This is especially true for sbt, the most popular build tool in our
community, but it's also the case for other popular Scala build tools. Let's
focus on when this holds true in sbt's case.

1. `reload`: For every change in your `build.sbt`, you need to reload your
   shell to detect the changes in the sbt sources. In that process, sbt starts
   afresh and throws away all the compilers in its cache.

2. `sbt`: Every time you run `sbt` on your terminal, sbt starts a new
    compiler. This happens when:

    * You run sbt in different project directories, or have different
    builds running at the same time.

    * The sbt shell is killed or exits. Some examples: you <kbd>CTRL</kbd> +
    <kbd>C</kbd> a misbehaving application (like an http server), you want to
    kill a command that you mistyped or sbt runs out of memory.

<span class="label success upper">Take away</span> Bloop has been designed so
that hot compilers are thrown away only if you forcefully kill the bloop
server.

### Run only core tasks

The other main way bloop achieves a tight developer workflow is by doing less
work. Bloop only focuses on tasks related to compiling, testing and running
your code. Everything else is left to external tools so that only the
blocking tasks are in your way when editing code.

As a result, you won't resolve library dependencies in the middle of an
incremental compilation run (which makes it an excellent tool for offline
coding), nor run a task cache invalidation check, because these are orthogonal concerns.

<span class="label success upper">Take away</span> By only focusing on the
most critical set of tasks, bloop delivers the best build performance.

## Not a build tool

Bloop is not a new build tool.

A "Build tool" is anything that refers to a tool that developers use to build
and ship code. Developers communicate a series of logic steps to the tool
that need to be executed before the end user can consume the software.
Among these steps we usually find how a piece of software is resolved,
compiled, packaged, distributed, post-processed and anything in between.

Build tools usually have a task engine, a story for caching and incremental
execution, a configuration DSL and a plugin ecosystem. These all make a model
of what the build tool is.

Such a model is relatively general so that developers can support the vast
array of use cases that they need to build their code in different platform
and environments. But it has a price, and build tools eventually find a
tradeoff between speed and other properties like correctness, usability and
extensibility.

Bloop fundamentally lacks such a model because it doesn't aim to solve the
same problems a build tool does! The problems that bloop aims to address are:

* Compiling, testing and running code in the fastest way possible.
* Providing a backbone upon which other tools can build.

<span class="label success upper">Take away</span> Bloop can then be used by
Scala developers to be more productive and by tooling authors to write their
tools faster and more reliably.

### A note on the future of build tools

It's an exciting time for Scala tooling!

There is a lot of activity in the build tools field; many smart
people are working on how to make build tools in the Scala ecosystem faster,
easier to use and more correct.

Among these efforts we find organizations like Stripe or Wix working on
[better Scala support for Bazel][scala-bazel] or Twitter getting excellent
Scala support in [Pants][pants], and individuals like [Jan Christopher
Vogt][@cvogt] or [Li Haoyi][@lihaoyi] working on [cbt] and [mill]
respectively.

All of these are exciting projects. In the future, we expect some of them to
integrate with bloop and benefit from a faster cross build-tool workflow! But
we're also aware that they incidentally address the problem of compiling
faster in different ways.

With this in mind, it's important to note that we've created bloop to
primarily support all the existing build tools that companies and Scala
open-source developers use *today*.

We believe that it's possible for many of these new tooling efforts to use
bloop to get a faster development workflow, but that's not our focus;
the community around them is vibrant but small at the moment, and we aim to
address these problems on a larger scale.

By supporting reliable and hardened JVM build tools like sbt, Maven and
Gradle, together with IDEs (Intellij) and the ongoing tooling experiments,
we want to improve the life of every Scala developer.

## Other bloop features

### Concurrent

A single bloop server handles concurrent requests from multiple clients. This
enables several clients to reuse the same bloop instance without blocking
each other.

### Runtime independent (on the roadmap)

Test or run your Scala application on the JVM, Scala.js and Scala Native.
Bloop will integrate with the API of every runtime that the Scala ecosystem
supports so that external tools don't have to.

[There is ongoing effort in the Scala Native build
API](https://github.com/scala-native/scala-native/pull/1143), contributed by
the bloop team, that will make it possible to cross-compile to Scala Native,
Scala.js and the JVM at once!

### Script-friendly

The primary way to interact with bloop is directly from the command line
instead of through a shell or REPL. This workflow is more familiar to how
most developer tools work in other programming languages ecosystems like
Java's and Javascript's (for example, Maven, Gradle, npm) and allows you to
reuse the same terminal for other tasks.

If you have ever fancied writing a [make] or [ninja] build for Scala, or a
custom CI script, bloop also enables you to do it!

[scala/scala]: https://github.com/scala/scala
[sbt/zinc]: https://github.com/sbt/zinc
[@cvogt]: https://github.com/cvogt
[@lihaoyi]: https://github.com/lihaoyi
[pants]: https://github.com/pantsbuild/pants
[scala-bazel]: https://github.com/bazelbuild/rules_scala
[cbt]: https://github.com/cvogt/cbt
[mill]: https://github.com/lihaoyi/mill
[make]: https://www.gnu.org/software/make/
[ninja]: https://ninja-build.org/
[scalacenter]: https://scala.epfl.ch
