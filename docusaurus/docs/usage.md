---
id: usage
title: Usage Guide
sidebar_label: Usage Guide
---

> **Pre-requirement**: [install bloop](/setup) and [learn the Basics](basics.md) before reading the
> usage guide.

To compile, test and run your projects, Bloop needs to understand your build. The build information
is the source of the truth and defines your project dependencies, their build inputs and their build
outputs.

The first step to use bloop is to generate Bloop configuration files from your build tool. These
configuration files are JSON objects that define a project's build as the build tool understands it.
Next is an example of the simplest bloop configuration file possible.

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

## Supported build tools

Bloop supports several build tools with varying degree of functionality.

|                    | sbt      | Gradle   | Maven    | mill | Bazel | Pants | Fury |
| ------------------ | -------- | -------- | -------- | ---- | ----- | ----- | ---- |
| **Supported**      | ✅       | ✅ __*__  | ✅ __*__ | ✅   |  ❌    |   ❌   | ----- | ---- | ----- | ----- |

> Build tools with 

<table>
<thead>
<tr>
  <td>Editor</td>
  <td align=center>Installation</td>
  <td align=center>Build import</td>
  <td align=center>Diagnostics</td>
  <td align=center>Goto definition</td>
  <td align=center>Metals Extensions</td>
</tr>
</thead>
<tbody>
<tr>
  <td>Visual Studio Code</td>
  <td align=center>Compile from source</td>
  <td align=center>Built-in</td>
  <td align=center>✅</td>
  <td align=center>✅</td>
  <td align=center>✅</td>
</tr>
<tr>
  <td>Atom</td>
  <td align=center>Single click</td>
  <td align=center>Built-in</td>
  <td align=center>✅</td>
  <td align=center>✅</td>
  <td align=center></td>
</tr>
<tr>
  <td>Vim</td>
  <td align=center>Few steps</td>
  <td align=center>Built-in</td>
  <td align=center>✅</td>
  <td align=center>✅</td>
  <td align=center>Status bar</td>
</tr>
<tr>
  <td>Sublime Text 3</td>
  <td align=center>Few steps</td>
  <td align=center>Requires browser</td>
  <td align=center>✅</td>
  <td align=center>✅</td>
  <td align=center></td>
</tr>
</tbody>
</table>
