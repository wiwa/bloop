package bloop.dap

import java.net.InetSocketAddress

import monix.eval.Task

final case class DebugParameters(address: InetSocketAddress, task: Task[_])
