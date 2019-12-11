package bloop.io

object Bracket {

  def withResource[Res <: AutoCloseable, Out](resource: Res)(op: Res => Out): Out = {
    try {
      op(resource)
    } finally {
      resource.close()
    }
  }
}
