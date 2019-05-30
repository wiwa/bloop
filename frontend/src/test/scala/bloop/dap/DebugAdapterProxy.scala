package bloop.dap

import java.io.{InputStream, OutputStream}

import com.microsoft.java.debug.core.protocol.JsonUtils._
import com.microsoft.java.debug.core.protocol._
import monix.eval.Task

final class DebugAdapterProxy(in: InputStream, out: OutputStream)
    extends AbstractProtocolServer(in, out) {

  def initialize(arguments: Requests.InitializeArguments): Task[Types.Capabilities] =
    request("initialize", arguments, classOf[Types.Capabilities])

  def configurationDone(): Task[Unit] =
    request("configurationDone", (), classOf[Unit])

  def launch(): Task[Unit] =
    request("launch", (), classOf[Unit])

  def disconnect(arguments: Requests.DisconnectArguments): Task[Unit] =
    request("disconnect", arguments, classOf[Unit])

  protected override def dispatchRequest(request: Messages.Request): Unit =
    throw new UnsupportedOperationException("Requests are not supported")

  private def request[A, B](method: String, arg: A, targetType: Class[B]): Task[B] = {
    val json = toJsonTree(arg, arg.getClass).getAsJsonObject
    val request = new Messages.Request(method, json)

    val response = sendRequest(request)
    Task(response.get()).map(response => convert(response.body, targetType))
  }

  private def convert[B](arg: Any, targetType: Class[B]): B = {
    if (targetType == classOf[Unit]) {
      if (arg != null) throw new IllegalStateException("Unexpected response body: " + arg)
      ().asInstanceOf[B]
    } else {
      val json = toJson(arg)
      fromJson(json, targetType)
    }
  }
}
