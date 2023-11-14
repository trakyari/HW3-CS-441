import scala.util.{Failure, Success, Try}
import org.slf4j.{Logger, LoggerFactory}

object CreateLogger {
  def apply[T](class4Logger: Class[T]): Logger = {
    val LOGBACKXML = "logback.xml"
    val logger = LoggerFactory.getLogger(class4Logger.getClass)
    Try(getClass.getClassLoader.getResourceAsStream(LOGBACKXML)) match {
      case Failure(exception) => logger.error(s"Failed to locate $LOGBACKXML for reason $exception")
      case Success(inStream) => if (inStream != null) inStream.close()
    }
    logger
  }
}