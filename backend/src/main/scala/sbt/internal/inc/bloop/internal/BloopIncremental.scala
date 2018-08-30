// scalafmt: { maxColumn = 250 }
package sbt.internal.inc.bloop.internal

import java.io.File
import java.net.URI
import java.util.Optional
import java.util.concurrent.CompletableFuture

import monix.eval.Task
import sbt.internal.inc.{Analysis, Lookup, Stamper, Stamps, UnderlyingChanges, AnalysisCallback => AnalysisCallbackImpl}
import sbt.util.Logger
import xsbti.AnalysisCallback
import xsbti.api.AnalyzedClass
import xsbti.compile.analysis.{ReadStamps, Stamp}
import xsbti.compile._

object BloopIncremental {
  type CompileFunction =
    (Set[File], DependencyChanges, AnalysisCallback, ClassFileManager) => Task[Unit]
  def compile(
      sources: Iterable[File],
      lookup: Lookup,
      compile: CompileFunction,
      previous0: CompileAnalysis,
      output: Output,
      log: Logger,
      options: IncOptions,
      picklePromise: CompletableFuture[Optional[URI]]
  ): Task[(Boolean, Analysis)] = {
    def getExternalAPI(lookup: Lookup): (File, String) => Option[AnalyzedClass] = { (_: File, binaryClassName: String) =>
      lookup.lookupAnalysis(binaryClassName) flatMap {
        case (analysis: Analysis) =>
          val sourceClassName =
            analysis.relations.productClassName.reverse(binaryClassName).headOption
          sourceClassName flatMap analysis.apis.internal.get
      }
    }

    val externalAPI = getExternalAPI(lookup)
    val current = Stamps.initial(Stamper.forLastModified, Stamper.forHash, Stamper.forLastModified)

    val previous = previous0 match { case a: Analysis => a }
    val previousRelations = previous.relations
    val internalBinaryToSourceClassName = (binaryClassName: String) => previousRelations.productClassName.reverse(binaryClassName).headOption
    val internalSourceToClassNamesMap: File => Set[String] = (f: File) => previousRelations.classNames(f)

    val builder = new AnalysisCallbackImpl.Builder(internalBinaryToSourceClassName, internalSourceToClassNamesMap, externalAPI, current, output, options, picklePromise)
    // We used to catch for `CompileCancelled`, but we prefer to propagate it so that Bloop catches it
    compileIncremental(sources, lookup, previous, current, compile, builder, log, options)
  }

  private def compileIncremental(
      sources: Iterable[File],
      lookup: Lookup,
      previous: Analysis,
      stamps: ReadStamps,
      compile: CompileFunction,
      callbackBuilder: AnalysisCallbackImpl.Builder,
      log: sbt.util.Logger,
      options: IncOptions,
/*      // TODO(jvican): Enable profiling of the invalidation algorithm down the road
      profiler: InvalidationProfiler = InvalidationProfiler.empty*/
  )(implicit equivS: Equiv[Stamp]): Task[(Boolean, Analysis)] = {
    def manageClassfiles[T](options: IncOptions)(run: ClassFileManager => T): T = {
      import sbt.internal.inc.{ClassFileManager => ClassFileManagerImpl}
      val classfileManager = ClassFileManagerImpl.getClassFileManager(options)
      val result =
        try run(classfileManager)
        catch { case e: Throwable => classfileManager.complete(false); throw e }
      classfileManager.complete(true)
      result
    }

    log.info(s"\nPrevious ${previous}\n")
    val setOfSources = sources.toSet
    sources.foreach { source =>
      log.info(s"Timestamp ${source.getAbsolutePath} ${source.lastModified()}")
    }

    val sourceChanges = lookup.changedSources(previous).getOrElse {
      val previousSources = previous.relations.allSources.toSet
      log.info(s"Previous sources ${previousSources}")
      new UnderlyingChanges[File] {
        private val inBoth = previousSources & setOfSources
        val removed = previousSources -- inBoth
        val added = setOfSources -- inBoth
        val (changed, unmodified) =
          inBoth.partition(f => !equivS.equiv(previous.stamps.source(f), stamps.source(f)))
        log.info(s"Changed ${changed}, added ${added}, ${removed}")
      }
    }

    val incremental = new BloopNameHashing(log, options)
    val initialChanges = incremental.changedInitial(setOfSources, previous, stamps, lookup)
    val binaryChanges = new DependencyChanges {
      val modifiedBinaries = initialChanges.binaryDeps.toArray
      val modifiedClasses = initialChanges.external.allModified.toArray
      def isEmpty = modifiedBinaries.isEmpty && modifiedClasses.isEmpty
    }
    val (initialInvClasses, initialInvSources) =
      incremental.invalidateInitial(previous.relations, initialChanges)

    if (initialInvClasses.nonEmpty || initialInvSources.nonEmpty) {
      if (initialInvSources == sources) incremental.log.debug("All sources are invalidated.")
      else {
        incremental.log.debug(
          "All initially invalidated classes: " + initialInvClasses + "\n" +
            "All initially invalidated sources:" + initialInvSources + "\n")
      }
    }

    import sbt.internal.inc.{ClassFileManager => ClassFileManagerImpl}
    val classfileManager = ClassFileManagerImpl.getClassFileManager(options)
    val analysisTask = {
      val doCompile = (srcs: Set[File], changes: DependencyChanges) => {
        for {
          callback <- Task.now(callbackBuilder.build())
          _ <- compile(srcs, changes, callback, classfileManager)
        } yield callback.get
      }

      try incremental.entrypoint(initialInvClasses, initialInvSources, setOfSources, binaryChanges, lookup, previous, doCompile, classfileManager, 1)
      catch { case e: Throwable => classfileManager.complete(false); throw e }
    }

    analysisTask.map { analysis =>
      classfileManager.complete(true)
      (initialInvClasses.nonEmpty || initialInvSources.nonEmpty, analysis)
    }
  }
}
