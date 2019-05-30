package bloop.dap

import com.microsoft.java.debug.core.adapter.ISourceLookUpProvider

object SourceLookUpProvider extends ISourceLookUpProvider {
  override def supportsRealtimeBreakpointVerification(): Boolean = false

  override def getFullyQualifiedName(
      uri: String,
      lines: Array[Int],
      columns: Array[Int]
  ): Array[String] = Array()

  override def getSourceFileURI(fullyQualifiedName: String, sourcePath: String): String = sourcePath
  override def getSourceContents(uri: String): String = ""
}
