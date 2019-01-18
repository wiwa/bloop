package bloop.reporter

import bloop.data.Project
import bloop.logging.{Logger, ObservableLogger}
import bloop.io.AbsolutePath

case class ReporterInputs[UseSiteLogger <: Logger](
    project: Project,
    cwd: AbsolutePath,
    logger: ObservableLogger[UseSiteLogger]
)
