import NetGraphAlgebraDefs.{Action, NetGraphComponent, NodeObject}

import java.io.{FileInputStream, ObjectInputStream}
import scala.util.Try

object GraphLoader {
  def load(fileName: String, dir: String): Option[List[NetGraphComponent]] = {
    Try(new FileInputStream(s"$dir$fileName"))
      .map(fis => (fis, new ObjectInputStream(fis)))
      .map { case (fis, ois) =>
        val ng = ois.readObject.asInstanceOf[List[NetGraphComponent]]
        ois.close()
        fis.close()
        ng
      }.toOption
  }
}
