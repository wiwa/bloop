package bloop.logging

import bloop.reporter.ReporterAction
import monix.reactive.{Observer, Observable, MulticastStrategy}
import monix.execution.Scheduler

/**
 * Defines a logger that forwards any event to the underlying logger and that
 * can be subscribed to by different clients. To subscribe to a client, you can
 * use the [[subscribe]] method that returns an `Observable[LoggerAction]`.
 */
final class ObservableLogger[L <: Logger] private (
    val underlying: L,
    observer: Observer[Either[ReporterAction, LoggerAction]]
) extends Logger {
  override val name: String = s"observable-${underlying.name}"
  override def isVerbose: Boolean = underlying.isVerbose
  override def asDiscrete: Logger = new ObservableLogger(underlying.asDiscrete, observer)
  override def asVerbose: Logger = new ObservableLogger(underlying.asVerbose, observer)
  override def ansiCodesSupported: Boolean = underlying.ansiCodesSupported

  override def debugFilter: DebugFilter = underlying.debugFilter
  override def debug(msg: String)(implicit ctx: DebugFilter): Unit =
    if (debugFilter.isEnabledFor(ctx)) printDebug(msg)

  /**
   * Replay an action that was produced during a bloop execution by another
   * logger.
   *
   * This method handles actions differently for BSP loggers and non-BSP
   * loggers, such as the one we use in the CLI. In BSP loggers, replaying a
   * compilation event translates into sending a BSP notification to the client
   * whereas in a CLI logger it's not actionable -- a compilation event has
   * already been translated to log messages by the reporter, so there's no
   * need to handle it.
   */
  def replay(action: LoggerAction): Unit = {
    action match {
      //   case LoggerAction.HandleCompilationEvent(event) =>
      //     underlying.handleCompilationEvent(event)
      case LoggerAction.LogErrorMessage(msg) => error(msg)
      case LoggerAction.LogWarnMessage(msg) => warn(msg)
      case LoggerAction.LogInfoMessage(msg) => info(msg)
      case LoggerAction.LogDebugMessage(msg) => printDebug(msg)
      case LoggerAction.LogTraceMessage(msg) => printDebug(msg)
    }
  }

  override def trace(t: Throwable): Unit = {
    underlying.trace(t)
    //observer.onNext(ObservableLogger.LogTraceMessage(msg))
    ()
  }

  override private[logging] def printDebug(msg: String): Unit = {
    underlying.printDebug(msg)
    observer.onNext(Right(LoggerAction.LogDebugMessage(msg)))
    ()
  }

  override def error(msg: String): Unit = {
    underlying.error(msg)
    observer.onNext(Right(LoggerAction.LogErrorMessage(msg)))
    ()
  }

  override def warn(msg: String): Unit = {
    underlying.warn(msg)
    observer.onNext(Right(LoggerAction.LogWarnMessage(msg)))
    ()
  }

  override def info(msg: String): Unit = {
    underlying.info(msg)
    observer.onNext(Right(LoggerAction.LogInfoMessage(msg)))
    ()
  }
}

object ObservableLogger {
  def apply[L <: Logger](
      underlying: L,
      observer: Observer[Either[ReporterAction, LoggerAction]]
  ): ObservableLogger[L] =
    new ObservableLogger(underlying, observer)
}
