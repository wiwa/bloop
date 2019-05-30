package bloop.dap

import java.net.{InetSocketAddress, URI}

import bloop.ConnectionHandle
import monix.execution.Scheduler

import scala.util.{Failure, Success}

object DebugAdapterServer {
  def createAdapter(address: InetSocketAddress)(ioScheduler: Scheduler): Either[String, URI] = {
    DebugAdapterFactory.create match {
      case Failure(error) => Left(error.getMessage)
      case Success(factory) =>
        val server = createServer()

        ioScheduler.executeAsync { () =>
          val adapter = factory.create(server, address)
          adapter.run()
        }

        Right(server.uri)
    }
  }

  private def createServer(): ConnectionHandle = {
    ConnectionHandle.tcp(backlog = 1)
  }
}
