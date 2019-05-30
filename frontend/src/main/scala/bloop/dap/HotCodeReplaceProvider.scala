package bloop.dap

import java.util
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer

import com.microsoft.java.debug.core.adapter.{HotCodeReplaceEvent, IHotCodeReplaceProvider}
import io.reactivex.Observable

private[dap] object HotCodeReplaceProvider extends IHotCodeReplaceProvider {
  override def onClassRedefined(consumer: Consumer[util.List[String]]): Unit = {}
  override def redefineClasses(): CompletableFuture[util.List[String]] =
    CompletableFuture.completedFuture(util.Collections.emptyList())
  override def getEventHub: Observable[HotCodeReplaceEvent] = Observable.empty()
}
