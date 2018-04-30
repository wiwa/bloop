package bloop.scalanative

import bloop.Project
import bloop.io.AbsolutePath
import bloop.logging.Logger

import java.nio.file.Path

import scala.scalanative.build.{Discover, Build, Config, GC, Mode, Logger => NativeLogger}
import scala.scalanative.testinterface.ScalaNativeFramework

import sbt.testing.Framework

object NativeBridge {

  def nativeLink(project: Project, entry: String, logger: Logger): Path = {
    val classpath = project.classpath.map(_.underlying)
    val workdir = project.out.resolve("native").underlying

    val clang = Discover.clang()
    val clangpp = Discover.clangpp()
    val linkopts = Discover.linkingOptions()
    val compopts = Discover.compileOptions()
    val triple = Discover.targetTriple(clang, workdir)
    val nativelib = Discover.nativelib(classpath).get
    val outpath = workdir.resolve("out")
    val nativeLogger = toNativeLogger(logger)

    val config =
      Config.empty
        .withGC(GC.default)
        .withMode(Mode.default)
        .withClang(clang)
        .withClangPP(clangpp)
        .withLinkingOptions(linkopts)
        .withCompileOptions(compopts)
        .withTargetTriple(triple)
        .withNativelib(nativelib)
        .withMainClass(entry)
        .withClassPath(classpath)
        .withWorkdir(workdir)
        .withLogger(nativeLogger)

    Build.build(config, outpath)
  }

  /**
   * Wrap the tests in `ScalaNativeFramework`. This replaces the runner with the
   * `ComRunner` that will communicate with the test agents via TCP. This is necessary because
   * our test agents don't know how to spawn a new process with the native binary.
   *
   * @param frameworks The frameworks to wrap.
   * @param testBinary The path to the binary file to test.
   * @param logger     The logger that should collect output when the tests run.
   * @return The frameworks wrapped in a `ScalaNativeFramework`.
   */
  def wrapFrameworks(frameworks: Array[Framework],
                     testBinary: Path,
                     logger: Logger): Array[Framework] = {
    frameworks.zipWithIndex.map {
      case (f, id) =>
        new ScalaNativeFramework(f, id, toNativeLogger(logger), testBinary.toFile, Map.empty)
    }
  }

  private def toNativeLogger(logger: Logger): NativeLogger = {
    NativeLogger(logger.debug _, logger.info _, logger.warn _, logger.error _)
  }

}
