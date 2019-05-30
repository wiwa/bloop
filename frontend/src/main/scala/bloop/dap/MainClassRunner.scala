package bloop.dap

import java.net.{InetSocketAddress, ServerSocket}

import bloop.data.{Platform, Project}
import bloop.engine.State
import bloop.engine.tasks.Tasks
import ch.epfl.scala.bsp.ScalaMainClass
import monix.eval.Task
import monix.execution.Scheduler

final class MainClassRunner(mainClass: ScalaMainClass) {
  def run(state: State, projects: Seq[Project])(
      scheduler: Scheduler
  ): Either[String, DebugParameters] = {
    val project = projects.head
    val env = project.platform match {
      case p: Platform.Jvm => Right(p.env)
      case platform => Left(s"Unsupported platform: ${platform.getClass.getSimpleName}")
    }

    env map { env =>
      val workingDir = state.commonOptions.workingPath
      val (args, address) = injectDebugOptions(mainClass)
      val task = Task
        .now {
          println("Hello")
        }
        .flatMap { _ =>
          Tasks.runJVM(
            state,
            project,
            env,
            workingDir,
            mainClass.`class`,
            args.toArray,
            skipJargs = false
          )
        }

      task.runAsync(scheduler)
      DebugParameters(address, task)
    }
  }

  private def injectDebugOptions(mainClass: ScalaMainClass): (Seq[String], InetSocketAddress) = {
    val address: InetSocketAddress = {
      val server = new ServerSocket(0)
      server.close()
      new InetSocketAddress(server.getInetAddress, server.getLocalPort)
    }

    val debugOpt =
      s"-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=${address.getHostName}:${address.getPort}"

    val args = mainClass.arguments ++ (mainClass.jvmOptions :+ debugOpt).map(opt => "-J" + opt)
    (args, address)
  }
}
