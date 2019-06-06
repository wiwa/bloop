package bloop.exec

import monix.eval.Task

trait ExternalProcess {
  def result: Task[Int]

}
