package bloop.engine.tasks

import java.net.URLClassLoader
import java.nio.file.{Files, Path}
import java.util.Properties

import scala.annotation.tailrec

import bloop.{DependencyResolution, Project}
import bloop.cli.ExitStatus
import bloop.config.Config.TestArgument
import bloop.engine.State
import bloop.exec.{JavaProcess, Process}
import bloop.io.AbsolutePath
import bloop.logging.Logger
import bloop.reporter.ReporterConfig
import bloop.testing.{DiscoveredTests, TestExecutor, TestInternals}

import monix.eval.Task

import sbt.testing.{
  AnnotatedFingerprint,
  EventHandler,
  Framework,
  SubclassFingerprint,
  TaskDef,
  Task => TestTask
}

object ScalaNative extends TestExecutor {

  private final val TestMainPackage = "scala.scalanative.testinterface"
  private final val TestMainName = "TestMain"

  /**
   * Compile down to native binary using Scala Native's toolchain.
   *
   * @param project The project to link
   * @param entry   The fully qualified main class name
   * @param logger  The logger to use
   * @return The absolute path to the native binary.
   */
  def nativeLink(project: Project, entry: String, logger: Logger): AbsolutePath = {

    initializeToolChain(logger)

    val bridgeClazz = nativeClassLoader.loadClass("bloop.scalanative.NativeBridge")
    val paramTypes = classOf[Project] :: classOf[String] :: classOf[Logger] :: Nil
    val nativeLinkMeth = bridgeClazz.getMethod("nativeLink", paramTypes: _*)

    // The Scala Native toolchain expects to receive the module class' name
    val fullEntry = if (entry.endsWith("$")) entry else entry + "$"

    AbsolutePath(nativeLinkMeth.invoke(null, project, fullEntry, logger).asInstanceOf[Path])
  }

  /**
   * Link `project` to a native binary and run it.
   *
   * @param state The current state of Bloop.
   * @param project The project to link.
   * @param cwd     The working directory in which to start the process.
   * @param main    The fully qualified main class name.
   * @param args    The arguments to pass to the program.
   * @return A task that links and run the project.
   */
  def run(state: State,
          project: Project,
          cwd: AbsolutePath,
          main: String,
          args: Array[String]): Task[State] = Task {
    import scala.collection.JavaConverters.propertiesAsScalaMap
    val nativeBinary = nativeLink(project, main, state.logger)
    val env = propertiesAsScalaMap(state.commonOptions.env).toMap
    val exitCode = Process.run(cwd, nativeBinary.syntax +: args, env, state.logger)

    val exitStatus = {
      if (exitCode == Process.EXIT_OK) ExitStatus.Ok
      else ExitStatus.UnexpectedError
    }
    state.mergeStatus(exitStatus)
  }

  override def executeTests(state: State,
                            project: Project,
                            cwd: AbsolutePath,
                            discoveredTests: DiscoveredTests,
                            args: List[TestArgument],
                            eventHandler: EventHandler): Task[State] = {
    @tailrec def loop(tasks: List[TestTask]): Unit = {
      tasks match {
        case task :: rest =>
          val newTasks = task.execute(eventHandler, Array(state.logger)).toList
          loop(rest ::: newTasks)

        case Nil =>
          ()
      }
    }

    processTests(state, project, discoveredTests).map {
      case (state, processedTests) =>
        val status = processedTests.tests.iterator.foldLeft(ExitStatus.Ok) {
          case (status, (framework, taskDefs)) =>
            val runner = TestInternals.getRunner(framework, args, discoveredTests.classLoader)
            val tasks = runner.tasks(taskDefs.toArray).toList
            loop(tasks)
            val summary = runner.done()
            if (summary.nonEmpty) state.logger.info(summary)
            ExitStatus.Ok
        }
        state.mergeStatus(status)
    }
  }

  private def processTests(state: State,
                           project: Project,
                           tests: DiscoveredTests): Task[(State, DiscoveredTests)] = {
    val tempDirectory = project.out.resolve("native").resolve("tmp")
    Files.createDirectories(tempDirectory.underlying)
    val frameworks = tests.tests.keys.toSeq
    val testTasks = tests.tests.values.flatten.toSeq
    val testMainContent = makeTestMain(frameworks, testTasks)
    val testMainPath = tempDirectory.resolve("TestMain.scala")
    Files.write(testMainPath.underlying, testMainContent.getBytes("UTF-8"))
    state.logger.debug(s"Wrote Scala Native test main to '${testMainPath}':")
    state.logger.debug(testMainContent)

    Tasks
      .compile(state, project, ReporterConfig.defaultFormat, true, extraSources = Seq(testMainPath))
      .map { state =>
        val entry = s"$TestMainPackage.$TestMainName"
        val testBinary = nativeLink(project, entry, state.logger)
        state.logger.debug("Generated test binary: '$testBinary'.")

        val bridgeClazz = nativeClassLoader.loadClass("bloop.scalanative.NativeBridge")
        val paramTypes = classOf[Array[Framework]] :: classOf[Path] :: classOf[Logger] :: Nil
        val nativeLinkMeth = bridgeClazz.getMethod("wrapFrameworks", paramTypes: _*)
        val wrappedFrameworks = nativeLinkMeth
          .invoke(null, frameworks.toArray, testBinary.underlying, state.logger)
          .asInstanceOf[Array[Framework]]
        val remappedTests = tests.tests.map {
          case (framework, tasks) =>
            val index = frameworks.indexOf(framework)
            wrappedFrameworks(index) -> tasks
        }
        (state, DiscoveredTests(tests.classLoader, remappedTests))
      }
  }

  private def makeTestMain(frameworks: Seq[Framework], tests: Seq[TaskDef]): String = {
    val frameworksList = if (frameworks.isEmpty) {
      "Nil"
    } else {
      frameworks
        .map(_.getClass.getName)
        .mkString("List(new _root_.", ", new _root_.", ")")
    }
    val testsMap = makeTestsMap(tests)

    s"""package $TestMainPackage
       |object $TestMainName extends TestMainBase {
       |  override val frameworks = $frameworksList
       |  override val tests = Map[String, AnyRef]($testsMap)
       |  def main(args: Array[String]): Unit = {
       |    testMain(args)
       |  }
       |}""".stripMargin
  }

  private def makeTestsMap(tests: Seq[TaskDef]): String = {
    tests
      .map { taskDef =>
        val isModule = taskDef.fingerprint match {
          case af: AnnotatedFingerprint => af.isModule
          case sf: SubclassFingerprint => sf.isModule
        }

        val name = taskDef.fullyQualifiedName

        val inst =
          if (isModule) s"_root_.${name}" else s"new _root_.${name}"

        s""""${name}" -> $inst"""
      }
      .mkString(", ")
  }

  private def bridgeJars(logger: Logger): Array[AbsolutePath] = {
    val organization = bloop.internal.build.BuildInfo.organization
    val nativeBridge = bloop.internal.build.BuildInfo.nativeBridge
    val version = bloop.internal.build.BuildInfo.version
    logger.debug(s"Resolving Native bridge: $organization:$nativeBridge:$version")
    val files = DependencyResolution.resolve(organization, nativeBridge, version, logger)
    files.filter(_.underlying.toString.endsWith(".jar"))
  }

  private def bridgeClassLoader(parent: Option[ClassLoader], logger: Logger): ClassLoader = {
    val jars = bridgeJars(logger)
    val entries = jars.map(_.underlying.toUri.toURL)
    new URLClassLoader(entries, parent.orNull)
  }

  private[this] var nativeClassLoader: ClassLoader = _
  private[this] def initializeToolChain(logger: Logger): Unit = synchronized {
    if (nativeClassLoader == null) {
      nativeClassLoader = bridgeClassLoader(Some(this.getClass.getClassLoader), logger)
    }
  }

}
