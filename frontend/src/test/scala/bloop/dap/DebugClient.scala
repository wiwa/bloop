package bloop.dap

import java.net.{Socket, URI}

import com.microsoft.java.debug.core.protocol.{Requests, Types}
import monix.eval.Task
import monix.execution.Scheduler

final class DebugClient(implicit proxy: DebugAdapterProxy) {
  def initialize(): Task[Types.Capabilities] = {
    val arguments = new Requests.InitializeArguments()
    proxy.initialize(arguments)
  }

  def configurationDone(): Task[Unit] = {
    proxy.configurationDone()
  }

  def launch(): Task[Unit] = {
    proxy.launch()
  }

  def disconnect(): Task[Unit] = {
    val arguments = new Requests.DisconnectArguments
    proxy.disconnect(arguments)
  }
}

object DebugClient {
  def apply(uri: URI)(scheduler: Scheduler): DebugClient = {
    val socket = new Socket(uri.getHost, uri.getPort)

    val proxy = new DebugAdapterProxy(socket.getInputStream, socket.getOutputStream)
    Task(proxy.run()).runAsync(scheduler)

    new DebugClient()(proxy)
  }
}
