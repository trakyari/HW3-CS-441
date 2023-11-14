
import com.typesafe.config.{Config, ConfigFactory}

import scala.util.Failure

// Adapted from NetGameSim
object Constants {
  private val config: Config = ConfigFactory.load()
  case class EnumeratedLoopParameters(ps: List[Double])
  case class FromToWithStepParameters(from: Double, to: Double, step: Double)

  private val CONFIGENTRYNAME: String = "HW3"

  val DEFAULT_ORIGINAL_GRAPH_FILE_NAME = "NetGameSimNetGraph.ngs"
  val ORIGINAL_GRAPH_FILE_NAME = "original_graph_file_name"

  val DEFAULT_PERTURBED_ORIGINAL_GRAPH_FILE_NAME = "NetGameSimNetGraph.ngs.perturbed"
  val PERTURBED_GRAPH_FILE_NAME = "perturbed_graph_file_name"

  val DEFAULT_JAR_OUTPUT_DIRECTORY = "target\\scala-2.13\\HW2.jar"
  val JAR_OUTPUT_DIRECTORY = "jarOutputDirectory"

  val OUTPUT_DIRECTORY = "outputDirectory"

  val globalConfig: Config = obtainConfigModule(config, CONFIGENTRYNAME)

  def obtainConfigModule(cf: Config, moduleName: String): Config = {
    scala.util.Try(cf.getConfig(moduleName)) match {
      case scala.util.Success(cfg) => cfg.asInstanceOf[Config]
      case Failure(exception) => throw new Exception(s"No config entry found for $moduleName: ${exception.getMessage}")
    }
  }
}
