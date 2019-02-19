package bloop.util

import bloop.logging.Logger

import java.nio.file.{Path, Files}
import java.util.concurrent.Executor
import java.util.concurrent.ConcurrentHashMap
import java.nio.file.attribute.BasicFileAttributes

import scala.collection.JavaConverters._

import monix.eval.Task
import monix.execution.{Scheduler, Cancelable}
import monix.reactive.{Observable, Consumer}
import monix.reactive.MulticastStrategy

import io.methvin.watcher.DirectoryChangeEvent.EventType
import io.methvin.watcher.{DirectoryChangeEvent, DirectoryChangeListener, DirectoryWatcher}

object CopyProducts {
  def apply(
      origin: Path,
      target0: Path,
      scheduler: Scheduler,
      executor: Executor,
      logger0: Logger
  ): Task[Unit] = {
    val target = Files.createDirectories(target0).toRealPath()
    println(s"Copying from ${origin}")
    println(s"        to ${target}")
    var watchingEnabled: Boolean = true
    val logger = new bloop.logging.Slf4jAdapter(logger0)

    val (observer, observable) =
      Observable.multicast[DirectoryChangeEvent](MulticastStrategy.publish)(scheduler)

    val listener = new DirectoryChangeListener {
      override def isWatching: Boolean = watchingEnabled
      override def onException(e: Exception): Unit = {
        logger.error(s"File watching threw an exception: ${e.getMessage}")
      }

      override def onEvent(event: DirectoryChangeEvent): Unit = {
        println(s"Event ${event.path}")
        observer.onNext(event)
        ()
      }
    }

    val watcher = DirectoryWatcher
      .builder()
      .paths(List(origin).asJava)
      .listener(listener)
      .fileHashing(true)
      .build()

    val watcherHandle = watcher.watchAsync(executor)
    val watchController = Task {
      try watcherHandle.get()
      finally watcher.close()
      logger.debug("File watcher was successfully closed")
    }

    val watchCancellation = Cancelable { () =>
      watchingEnabled = false
      watcherHandle.complete(null)
      observer.onComplete()
      logger.debug("Cancelling file watcher")
    }

    val createdDirs = ConcurrentHashMap.newKeySet[Path]()
    val dirAttrs = new ConcurrentHashMap[Path, Either[Boolean, BasicFileAttributes]]()
    val fileEventConsumer = {
      Consumer.foreachParallelAsync(10) { (event: DirectoryChangeEvent) =>
        val eventPath = event.path().toRealPath()
        println(s"Processing event ${eventPath}")
        val attrs = Files.readAttributes(eventPath, classOf[BasicFileAttributes])
        println(s"Relativized ${origin.relativize(eventPath).toRealPath()}")
        val rebasedPath = target.resolve(origin.relativize(eventPath).toRealPath()).toRealPath()
        if (attrs.isDirectory) {
          event.eventType match {
            case EventType.CREATE =>
              Task {
                dirAttrs.computeIfAbsent(
                  rebasedPath,
                  (path: Path) => {
                    Files.createDirectories(rebasedPath)
                    Left(true)
                  }
                )
                ()
              }
            case EventType.MODIFY => Task.now(()) // TODO: Set attributes for modified dir
            case EventType.OVERFLOW => Task.now(())
            case EventType.DELETE => Task { Files.deleteIfExists(rebasedPath); () }
          }
        } else if (attrs.isRegularFile) {
          event.eventType match {
            case EventType.CREATE =>
              Task {
                val parentPath = rebasedPath.getParent
                dirAttrs.computeIfAbsent(
                  rebasedPath,
                  (path: Path) => {
                    Files.createDirectories(parentPath)
                    Left(true)
                  }
                )
                Files.copy(eventPath, rebasedPath)
                ()
              }
            case EventType.MODIFY => Task { Files.copy(eventPath, rebasedPath); () }
            case EventType.OVERFLOW => Task.now(())
            case EventType.DELETE => Task { Files.deleteIfExists(rebasedPath); () }
          }
        } else if (attrs.isSymbolicLink) {
          ???
        } else {
          // it's other
          ???
        }
      }
    }

    observable
      .consumeWith(fileEventConsumer)
      .doOnCancel(Task(watchCancellation.cancel()))
  }
}
