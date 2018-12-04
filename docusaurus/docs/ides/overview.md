---
id: overview
title: Overview
sidebar_label: Overview
---

IDEs can use bloop to compile, test and run Scala code fast via the [Build Server Protocol
(BSP)](https://github.com/scalacenter/bsp). The protocol allows clients to receive reliable
compilation diagnostics in text editors and IDEs.



There are two main editor integrations:

1. [IntelliJ](https://www.jetbrains.com/idea/): the most popular Scala and Java IDE has an experimental
   BSP integration that connects to bloop to fetch compiler diagnostics. IntelliJ users must export
   the build every time there is a build change.

1. [Metals](https://github.com/scalameta/metals): a language server for Scala. Metals features the best
   Bloop integration in the Scala community. It generates the configuration files automatically
   whenever a change is detected in sbt builds.

