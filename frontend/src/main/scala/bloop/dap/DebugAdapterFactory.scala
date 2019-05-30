package bloop.dap

import java.net.{InetSocketAddress, URL, URLClassLoader}

import bloop.ConnectionHandle
import bloop.exec.JavaEnv
import bloop.io.{AbsolutePath, RelativePath}
import com.microsoft.java.debug.core.adapter._
import com.microsoft.java.debug.core.protocol.AbstractProtocolServer

import scala.util.{Failure, Try}

private[dap] final class DebugAdapterFactory {
  def create(connection: ConnectionHandle, address: InetSocketAddress): AbstractProtocolServer = {
    val socket = connection.serverSocket.accept()
    val context = new ProviderContext
    context.registerProvider(classOf[IHotCodeReplaceProvider], HotCodeReplaceProvider)
    context.registerProvider(classOf[IVirtualMachineManagerProvider], VirtualMachineManagerProvider)
    context.registerProvider(classOf[ISourceLookUpProvider], SourceLookUpProvider)
    context.registerProvider(classOf[IEvaluationProvider], EvaluationProvider)
    context.registerProvider(classOf[ICompletionsProvider], CompletionsProvider)

    DebugAdapter(address)(socket, context)
  }
}

private[dap] object DebugAdapterFactory {
  private val toolsJarIsOnClasspath: Try[_] = checkToolsJarIsOnClasspath().recoverWith {
    case _: ClassNotFoundException =>
      addToolsJarToClasspath().flatMap(_ => checkToolsJarIsOnClasspath())
  }

  def create: Try[DebugAdapterFactory] =
    toolsJarIsOnClasspath.map(_ => new DebugAdapterFactory)

  private def checkToolsJarIsOnClasspath(): Try[_] =
    Try(Class.forName("com.sun.jdi.Value"))

  private def addToolsJarToClasspath(): Try[_] =
    JvmTools.jar match {
      case None =>
        val error = new IllegalStateException(
          "Cannot find 'tools.jar'. Please add it to the classpath"
        )
        Failure(error)
      case Some(jar) =>
        getClass.getClassLoader match {
          case urlClassLoader: URLClassLoader =>
            Try {
              val method = classOf[URLClassLoader].getDeclaredMethod("addURL", classOf[URL])
              method.setAccessible(true)
              method.invoke(urlClassLoader, jar.toBspUri.toURL)
            }
          case _ =>
            val error = new IllegalStateException(
              s"Cannot inject 'tools.jar'. Please add it to the classpath: $jar"
            )
            Failure(error)
        }
    }

  private object JvmTools {
    private val tools = RelativePath("lib/tools.jar")

    private val javaHome = JavaEnv.DefaultJavaHome
    private val jdkPath = javaHome.resolve(tools)
    private val jrePath = javaHome.getParent.resolve(tools)
    private val windowsJrePath = {
      val version = javaHome.underlying.getFileName.toString.drop("jre".length)
      javaHome.getParent.resolve("jdk" + version).resolve(tools)
    }

    val jar: Option[AbsolutePath] =
      List(jdkPath, jrePath, windowsJrePath).find(_.exists)
  }
}
