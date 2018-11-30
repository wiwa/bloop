---
id: sbt
title: Exporting sbt builds to bloop
sidebar_label: sbt
---

Bloop supports sbt **0.13.x** and **1.x**.

## Install the Plugin

Install bloop in `project/plugins.sbt`:

```scala
addSbtPlugin("ch.epfl.scala" % "sbt-bloop" % "1.1.0")
```

And start up or reload your sbt shell to add the plugin to your working build.

## Export the Build

The sbt command `bloopInstall` exports your sbt build to bloop.

In bloop, an sbt project is repesented as a pair of `(sbt project, sbt configuration)` and it's
written to a configuration directory. The default location of this directory in your workspace is
`.bloop/` (you may want to add `.bloop/` to your `.gitignore` file).

For example, a build with a single project `foo` generates two configuration files by
default (one per [sbt configuration][sbt-configuration]):

```bash
$ sbt bloopInstall
[info] Loading global plugins from /Users/John/.sbt/1.0/plugins
(...)
[success] Generated '/disk/foo/.bloop/foo.json'.
[success] Generated '/disk/foo/.bloop/foo-test.json'.
```

where:
1. `foo` comes from the `Compile` sbt configuration; and,
1. `foo-test` comes from the `Test` sbt configuration and depends on `foo`

Any change in the build affecting the semantics of a project (such as adding a new library
dependency, a resource or changing the project name) requires the user to run `bloopInstall`. When a
project is removed in the build, `bloopInstall` will automatically remove its old configuration from
the `.bloop` directory.

## Verify Installation and Export

> Remember that the build server must be running in the background, as suggested by the [Setup
page](/setup).

Verify your installation by running `bloop projects` in the root of the sbt workspace directory.

```bash
$ bloop projects
foo
foo-test
```

If the results of `bloop projects` is empty, check that:

1. You are running the command-line invocation in the root base directory (e.g. `/disk/foo`).
1. The sbt build export process completed successfully.
1. The `.bloop/` configuration directory contains bloop configuration files.

If you suspect bloop is loading the configuration files from somewhere else, use `--verbose`:

```bash
$ bloop projects --verbose
[D] Projects loaded from '/my-project/.bloop':
foo
foo-test
```

Here's a list of bloop commands you can run next to start playing with bloop:

1. `bloop compile --help`: shows the help section for compile.
1. `bloop compile foo-test`: compiles foo's `src/main` and `src/test`.
1. `bloop test foo-test -w`: runs foo tests repeatedly with file watching enabled.

After verifying the export, you can continue using Bloop's command-line application or any build
client integrating with Bloop, such as [Metals](https://scalameta.org/metals/).

## Advanced Configuration

### Speeding Up Build Export

`bloopInstall` typically completes in 15 seconds for a medium-sized projects once all dependencies
have already been downloaded. However, it can happen that running `bloopInstall` in your project is
slower than that. This long duration can usually be removed by making some changes in the build.

If you want to speed up this process, here's a list of things you can do.

Ensure that no compilation is triggered during `bloopInstall`. `bloopInstall` is intentionally
configured to skip project compilations to make the export process fast. If compilations are
triggered, then it means your build adds certain runtime dependencies in your build graph.

> For example, your build may be forcing the `publishLocal` of a project `a` whenever the classpath of
`b` is computed. Identify this kind of dependencies and prune them.

Another rule of thumb is to make sure that source and resource generators added to your build by
either you or sbt plugin are incremental and complete as soon as possible.

Lastly, make sure you keep a hot sbt session around as much time as possible. Running `bloopInstall`
a second time in the sbt session is *really* fast.

### Export Main Class from sbt

If you want bloop to export `mainClass` from your build definition, define either of the following
settings in your `build.sbt`:

```scala
bloopMainClass in (Compile, run) := Some("foo.App")
```

The build plugin doesn't intentionally populate the main class directly from sbt's `mainClass`
because the execution of such a task may trigger compilation of projects in the build.

### Support for source dependencies

Source dependencies are not well supported in sbt. Nonetheless, if you use them in your build and
you want to generate bloop configuration files for them too, add the following to your `build.sbt`:

```scala
// Note that this task has to be scoped globally
bloopAggregateSourceDependencies in Global := true
```

### Export Projects For Additional Configurations

By default, `bloopInstall` exports projects for the standard `Compile` and `Test` sbt
configurations. If your build defines additional configurations in a project, such as
[`IntegrationTest`][integration-test-conf], you might want to export these configurations to Bloop
projects too.

Exporting projects for additional sbt configuration requires changes in the build definition in
`build.sbt`:

```scala
import bloop.integrations.sbt.BloopDefaults

// Example of a project configured with an additional `IntegrationTest` configuration
val foo = project
  .configs(IntegrationTest)
  .settings(
    // Scopes bloop configuration settings in `IntegrationTest`
    inConfig(IntegrationTest)(BloopDefaults.configSettings)
  )
```

When you reload your build, you can check that `bloopInstall` exports a new project called
`foo-it.json`.

```
sbt> bloopInstall
[info] Loading global plugins from /Users/John/.sbt/1.0/plugins
(...)
[success] Generated '/disk/foo/.bloop/foo.json'.
[success] Generated '/disk/foo/.bloop/foo-test.json'.
[success] Generated '/disk/foo/.bloop/foo-it.json'.
```

If you want to avoid using Bloop-specific settings in your build definition, add the previous
`inConfig` line in another file (e.g. a `local.sbt` file) and add this local file to your global
`.gitignore`.

[sbt-configuration]: https://www.scala-sbt.org/1.x/docs/Multi-Project.html
[integration-test-conf]: https://www.scala-sbt.org/1.0/docs/offline/Testing.html#Integration+Tests
