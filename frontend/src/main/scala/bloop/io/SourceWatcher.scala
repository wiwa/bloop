package bloop.io

import java.nio.file.Path

import bloop.Project
import bloop.bsp.BspServer
import bloop.engine.{ExecutionContext, State}
import bloop.logging.Logger

import scala.collection.JavaConverters._
import io.methvin.watcher.DirectoryChangeEvent.EventType
import io.methvin.watcher.{DirectoryChangeEvent, DirectoryChangeListener, DirectoryWatcher}
import monix.eval.Task
import monix.execution.Ack
import monix.execution.Ack.{Continue, Stop}
import monix.execution.exceptions.APIContractViolationException
import monix.reactive.observables.ObservableLike.Operator
import monix.reactive.observers.Subscriber
import monix.reactive.{Consumer, MulticastStrategy, Observable}

import scala.concurrent.Future

final class SourceWatcher(project: Project, dirs0: Seq[Path], logger: Logger) {
  private val dirs = dirs0.distinct
  private val dirsCount = dirs.size
  private val dirsAsJava: java.util.List[Path] = dirs.asJava

  // Create source directories if they don't exist, otherwise the watcher fails.
  import java.nio.file.Files
  dirs.foreach(p => if (!Files.exists(p)) Files.createDirectories(p) else ())

  def watch(state0: State, action: State => Task[State]): Task[State] = {
    val ngout = state0.commonOptions.ngout
    def runAction(state: State, event: DirectoryChangeEvent): Task[State] = {
      // Someone that wants this to be supported by Windows will need to make it work for all terminals
/*      if (!BspServer.isWindows)
        logger.info("\u001b[H\u001b[2J") // Clean the terminal before acting on the file event action*/
      logger.debug(s"A ${event.eventType()} in ${event.path()} has triggered an event.")
      action(state)
    }

    val fileEventConsumer = Consumer.foldLeftAsync[State, DirectoryChangeEvent](state0) {
      case (state, event) =>
        val task = event.eventType match {
          case EventType.CREATE => runAction(state, event)
          case EventType.MODIFY => runAction(state, event)
          case EventType.OVERFLOW => runAction(state, event)
          case EventType.DELETE => Task.now(state)
        }
        task.map { x => logger.info(s"Executed $state"); x }
    }

    val (observer, observable) =
      Observable.multicast[DirectoryChangeEvent](MulticastStrategy.publish)(
        ExecutionContext.ioScheduler)

    val watcher = DirectoryWatcher.create(
      dirsAsJava,
      new DirectoryChangeListener {
        override def onEvent(event: DirectoryChangeEvent): Unit = {
          val targetFile = event.path()
          val targetPath = targetFile.toFile.getAbsolutePath()
          if (Files.isRegularFile(targetFile) &&
              (targetPath.endsWith(".scala") || targetPath.endsWith(".java"))) {
            observer.onNext(event)
            ()
          }
        }
      }
    )

    val watchingTask = Task {
      logger.info(s"File watching $dirsCount directories...")
      try watcher.watch()
      finally watcher.close()
    }.doOnCancel(Task {
      observer.onComplete()
      watcher.close()
      ngout.println(
        s"File watching on '${project.name}' and dependent projects has been successfully cancelled.")
    })

    val watchHandle = watchingTask.materialize.runAsync(ExecutionContext.ioScheduler)

    observable
      .dump("Hello")
      .liftByOperator(respectMyAuthoritah)
      .consumeWith(fileEventConsumer)
      .doOnFinish(_ => Task(watchHandle.cancel()))
      .doOnCancel(Task(watchHandle.cancel()))
  }

  def respectMyAuthoritah[A]: Operator[A, A] =
    new Operator[A, A] {
      def apply(out: Subscriber[A]): Subscriber[A] =
        new Subscriber[A] {
          implicit val scheduler = out.scheduler
          private[this] var lastAck: Future[Ack] = Continue
          private[this] var isDone = false

          def onNext(elem: A): Future[Ack] = {
            val ack = lastAck.syncTryFlatten

            ack match {
              case Stop =>
                scheduler.reportFailure(err("onNext after Stop"))
                Stop
              case Continue =>
                lastAck = out.onNext(elem)
                lastAck
              case _ =>
                onError(err("onNext backpressure"))
                lastAck = Stop
                lastAck
            }
          }

          private def signalComplete(ex: Option[Throwable]): Unit = {
            if (!isDone) {
              isDone = true
              ex.fold(out.onComplete())(out.onError)
            } else {
              scheduler.reportFailure(err(s"complete($ex)"))
            }
          }

          override def onComplete(): Unit =
            signalComplete(None)
          override def onError(ex: Throwable): Unit =
            signalComplete(Some(ex))
          private def err(label: String) =
            new APIContractViolationException("Stop signaled before")
        }
    }
}
