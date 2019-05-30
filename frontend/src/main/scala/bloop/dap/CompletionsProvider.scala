package bloop.dap

import java.util
import java.util.Collections

import com.microsoft.java.debug.core.adapter.ICompletionsProvider
import com.microsoft.java.debug.core.protocol.Types
import com.sun.jdi.StackFrame

object CompletionsProvider extends ICompletionsProvider {
  override def codeComplete(
      frame: StackFrame,
      snippet: String,
      line: Int,
      column: Int
  ): util.List[Types.CompletionItem] = Collections.emptyList()
}
