package bloop.logging

sealed trait LoggerAction
object LoggerAction {
  sealed trait LogMessage
  final case class LogErrorMessage(msg: String) extends LoggerAction with LogMessage
  final case class LogWarnMessage(msg: String) extends LoggerAction with LogMessage
  final case class LogInfoMessage(msg: String) extends LoggerAction with LogMessage
  final case class LogDebugMessage(msg: String) extends LoggerAction with LogMessage
  final case class LogTraceMessage(msg: String) extends LoggerAction with LogMessage
  final case class HandleCompilationEvent(event: CompilationEvent) extends LoggerAction
}
