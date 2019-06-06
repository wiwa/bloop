package bloop.dap

import java.io.{InputStream, OutputStream}
import java.net.{InetSocketAddress, Socket}

import bloop.dap.DebugAdapter.Adapter
import com.microsoft.java.debug.core.adapter.{IProviderContext, ProtocolServer}
import com.microsoft.java.debug.core.protocol.JsonUtils
import com.microsoft.java.debug.core.protocol.Messages.{Request, Response}
import com.microsoft.java.debug.core.protocol.Requests.Command._
import com.microsoft.java.debug.core.protocol.Requests.{AttachArguments, Command}

import scala.collection.mutable

final class DebugAdapter(adapters: Map[Command, Adapter])(
    in: InputStream,
    out: OutputStream,
    ctx: IProviderContext
) extends ProtocolServer(in, out, ctx) {
  private val requests = mutable.Map.empty[Int, Adapter]

  override def dispatchRequest(request: Request): Unit = {
    println(request.command)
    val command = Command.valueOf(request.command.toUpperCase())
    adapters.get(command) match {
      case Some(adapter) =>
        requests += (request.seq -> adapter)
        super.dispatchRequest(adapter.adapt(request))
      case None =>
        super.dispatchRequest(request)
    }
  }

  override def sendResponse(response: Response): Unit = {
    println("Sending response : " + response.command)
    val foo = requests.remove(response.request_seq) match {
      case Some(adapter) =>
        super.sendResponse(adapter.adapt(response))
        println("Adapted response : " + adapter.adapt(response))
      case None => super.sendResponse(response)
    }
  }
}

object DebugAdapter {
  def apply(address: InetSocketAddress)(socket: Socket, ctx: IProviderContext): DebugAdapter = {
    val adapters = List[Adapter](
      new LaunchRequestAdapter(address)
    ).map(adapter => (adapter.command, adapter))

    new DebugAdapter(adapters.toMap)(socket.getInputStream, socket.getOutputStream, ctx)
  }

  // TODO currently, adapted requests don't expect any response but a simple ACK.
  //  When adapting request which require actual response, this mechanism will have to be expanded
  protected sealed trait Adapter {
    def command: Command
    def adapt(request: Request): Request
    def adapt(response: Response): Response
  }

  private final class LaunchRequestAdapter(address: InetSocketAddress) extends Adapter {
    val command = LAUNCH
    override def adapt(request: Request): Request = {
      val arguments = new AttachArguments
      arguments.hostName = address.getHostName
      arguments.port = address.getPort

      val json = JsonUtils.toJsonTree(arguments, classOf[AttachArguments])
      val command = ATTACH.getName
      new Request(request.seq, command, json.getAsJsonObject)
    }

    override def adapt(response: Response): Response = {
      response.command = command.getName
      response
    }
  }
}
