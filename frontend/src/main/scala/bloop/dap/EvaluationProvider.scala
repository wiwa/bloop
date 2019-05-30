package bloop.dap

import java.util.concurrent.CompletableFuture

import com.microsoft.java.debug.core.IEvaluatableBreakpoint
import com.microsoft.java.debug.core.adapter.IEvaluationProvider
import com.sun.jdi.{ObjectReference, ThreadReference, Value}

object EvaluationProvider extends IEvaluationProvider {
  override def isInEvaluation(thread: ThreadReference): Boolean = false

  override def evaluate(
      expression: String,
      thread: ThreadReference,
      depth: Int
  ): CompletableFuture[Value] = ???

  override def evaluate(
      expression: String,
      thisContext: ObjectReference,
      thread: ThreadReference
  ): CompletableFuture[Value] = ???

  override def evaluateForBreakpoint(
      breakpoint: IEvaluatableBreakpoint,
      thread: ThreadReference
  ): CompletableFuture[Value] = ???

  override def invokeMethod(
      thisContext: ObjectReference,
      methodName: String,
      methodSignature: String,
      args: Array[Value],
      thread: ThreadReference,
      invokeSuper: Boolean
  ): CompletableFuture[Value] = ???

  override def clearState(thread: ThreadReference): Unit = {}
}
