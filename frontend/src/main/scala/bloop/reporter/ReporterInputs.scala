package bloop.reporter

import bloop.data.Project
import bloop.logging.Logger
import bloop.io.AbsolutePath

case class ReporterInputs(
    project: Project,
    cwd: AbsolutePath,
    logger: Logger
)
