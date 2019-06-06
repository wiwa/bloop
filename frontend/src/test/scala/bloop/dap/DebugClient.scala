package bloop.dap

import java.net.{Socket, URI}

import com.microsoft.java.debug.core.protocol.Events
import com.microsoft.java.debug.core.protocol.Events.DebugEvent
import com.microsoft.java.debug.core.protocol.Requests._
import com.microsoft.java.debug.core.protocol.Responses._
import com.microsoft.java.debug.core.protocol.Types.Capabilities
import monix.eval.Task
import monix.execution.Scheduler

import scala.reflect.ClassTag

final class DebugClient(implicit proxy: Proxy) {
  def initialize(): Task[Capabilities] = {
    val arguments = new InitializeArguments()
    DAP.Initialize(arguments)
  }

  def configurationDone(): Task[Unit] = {
    DAP.ConfigurationDone(())
  }

  def launch(): Task[Unit] = {
    DAP.Launch(new LaunchArguments)
  }

  def disconnect(): Task[Unit] = {
    val arguments = new DisconnectArguments
    DAP.Disconnect(arguments)
  }

  def exited: Task[Events.ExitedEvent] =
    DAP.Exited.first

  def terminated: Task[Events.TerminatedEvent] = {
    DAP.Terminated.first
  }

  def output(): Task[String] = {
    DAP.OutputEvent.all.foldLeftL(new StringBuilder)(_.append(_)).map(_.toString())
  }
}

object DebugClient {
  def apply(uri: URI)(scheduler: Scheduler): DebugClient = {
    val socket = new Socket(uri.getHost, uri.getPort)

    val proxy = Proxy.apply(socket)
    proxy.listen(scheduler)

    new DebugClient()(proxy)
  }
}
