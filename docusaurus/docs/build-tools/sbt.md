---
id: sbt
title: sbt
sidebar_label: sbt
---

## Export the build

Install and learn how to export the build in the Installation Guide:

<a class="button installationButton" href="/bloop/setup">Follow the Installation Guide</a>

</br>

The following page only walks you through all the configuration options that the `sbt-bloop` plugin
allows, but it does not explain neither how to install the plugin nor the basic concepts around exporting the build.

## Enable custom configurations

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

## Enable sbt project references 

Source dependencies are not well supported in sbt. Nonetheless, if you use them in your build and
you want to generate bloop configuration files for them too, add the following to your `build.sbt`:

```scala
// Note that this task has to be scoped globally
bloopAggregateSourceDependencies in Global := true
```

## Download dependencies sources

// metals
// go to definition

```scala
bloopExportJarClassifiers in Global := Some(Set("sources"))
```

## Export main class from sbt

If you want bloop to export `mainClass` from your build definition, define either of the following
settings in your `build.sbt`:

```scala
bloopMainClass in (Compile, run) := Some("foo.App")
```

The build plugin doesn't intentionally populate the main class directly from sbt's `mainClass`
because the execution of such a task may trigger compilation of projects in the build.

## Speeding up build export

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

[sbt-configuration]: https://www.scala-sbt.org/1.x/docs/Multi-Project.html
[integration-test-conf]: https://www.scala-sbt.org/1.0/docs/offline/Testing.html#Integration+Tests
