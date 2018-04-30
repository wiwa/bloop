package bloop.testing

import java.util.Properties

import bloop.Project
import bloop.config.Config.TestArgument
import bloop.cli.ExitStatus
import bloop.engine.State
import bloop.exec.JavaProcess
import bloop.io.AbsolutePath
import bloop.logging.Logger

import sbt.testing.EventHandler

import monix.eval.Task

/**
 * Defines the capability to run tests.
 */
trait TestExecutor {

  /**
   * Execute the test tasks and return a new state.
   *
   * @param state           The current state of Bloop
   * @param project         The project to test
   * @param cwd             The current working directory from which to run the tests.
   * @param discoveredTests The tests that were detected.
   * @param args            The arguments to pass to the test frameworks.
   * @param eventHandler    Handler that reacts on messages from the testinf frameworks.
   * @return The state after running the tests.
   */
  def executeTests(state: State,
                   project: Project,
                   cwd: AbsolutePath,
                   discoveredTests: DiscoveredTests,
                   args: List[TestArgument],
                   eventHandler: EventHandler): Task[State]
}
