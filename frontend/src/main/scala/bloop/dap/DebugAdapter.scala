package bloop.dap

import java.io.{InputStream, OutputStream}
import java.net.{InetSocketAddress, Socket}

import bloop.dap.DebugAdapter.Identity
import com.microsoft.java.debug.core.adapter.{IProviderContext, ProtocolServer}
import com.microsoft.java.debug.core.protocol.JsonUtils
import com.microsoft.java.debug.core.protocol.Messages.Request
import com.microsoft.java.debug.core.protocol.Requests.Command._
import com.microsoft.java.debug.core.protocol.Requests.{AttachArguments, Command}

final class DebugAdapter(adapters: Map[String, Request => Request])(
    in: InputStream,
    out: OutputStream,
    ctx: IProviderContext
) extends ProtocolServer(in, out, ctx) {
  override def dispatchRequest(request: Request): Unit = {
    val command = request.command
    val handler = adapters.getOrElse(command, Identity).andThen(super.dispatchRequest)
    handler.apply(request)
  }
}

object DebugAdapter {
  protected val Identity: Request => Request = x => x

  def apply(address: InetSocketAddress)(socket: Socket, ctx: IProviderContext): DebugAdapter = {
    val adapterList = List(
      new LaunchRequestAdapter(address)
    )

    val adapters = adapterList.map(adapter => adapter.targetedCommand.getName -> adapter).toMap
    new DebugAdapter(adapters)(socket.getInputStream, socket.getOutputStream, ctx)
  }

  // TODO currently, adapted requests don't expect any response but a simple ACK.
  //  When adapting request which require actual response, this mechanism will have to be expanded
  private abstract class Adapter(val targetedCommand: Command) extends (Request => Request)

  private final class LaunchRequestAdapter(address: InetSocketAddress) extends Adapter(LAUNCH) {
    override def apply(request: Request): Request = {
      val arguments = new AttachArguments
      arguments.hostName = address.getHostName
      arguments.port = address.getPort

      val json = JsonUtils.toJsonTree(arguments, classOf[AttachArguments])
      val command = ATTACH.getName
      new Request(request.seq, command, json.getAsJsonObject)
    }
  }
}
