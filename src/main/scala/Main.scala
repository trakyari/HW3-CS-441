import ConfigReader.getConfigEntry
import Constants._
import NetGraphAlgebraDefs.{Action, NetGraphComponent, NodeObject}
import com.google.common.graph.{MutableValueGraph, ValueGraphBuilder}
import com.typesafe.config.ConfigFactory
import org.slf4j.Logger

import java.net.InetAddress
import scala.sys.exit

class Main
object Main {
  private val originalGraphFileName = getConfigEntry(globalConfig, ORIGINAL_GRAPH_FILE_NAME, DEFAULT_ORIGINAL_GRAPH_FILE_NAME)
  private val perturbedGraphFileName = getConfigEntry(globalConfig, PERTURBED_GRAPH_FILE_NAME, DEFAULT_PERTURBED_ORIGINAL_GRAPH_FILE_NAME)

  val logger: Logger = CreateLogger(classOf[Main])
  val ipAddr: InetAddress = InetAddress.getLocalHost
  val hostName: String = ipAddr.getHostName
  val hostAddress: String = ipAddr.getHostAddress

  def main(args: Array[String]): Unit = {
    logger.info(s"The Apache Spark program is running on the host $hostName with the following IP addresses:")
    logger.info(hostAddress)

    val originalGraphFile = GraphLoader.load(originalGraphFileName, "")
    val perturbedGraphFile = GraphLoader.load(perturbedGraphFileName, "")
    checkGraphValidity(originalGraphFile)
    checkGraphValidity(perturbedGraphFile)
    val originalGraphComponents = createGraphComponents(originalGraphFile)
    val perturbedGraphComponents = createGraphComponents(perturbedGraphFile)

    val originalGraph: MutableValueGraph[NodeObject, Action] = ValueGraphBuilder.directed().build()
    originalGraphComponents.head.head.map(node => originalGraph.addNode(node.asInstanceOf[NodeObject]))
    originalGraphComponents.head.last.map(action => {
      val edge = action.asInstanceOf[Action]
      originalGraph.putEdgeValue(edge.fromNode, edge.toNode, edge)
    })

    val perturbedGraph: MutableValueGraph[NodeObject, Action] = ValueGraphBuilder.directed().build()
    perturbedGraphComponents.head.head.map(node => perturbedGraph.addNode(node.asInstanceOf[NodeObject]))
    perturbedGraphComponents.head.last.map(action => {
      val edge = action.asInstanceOf[Action]
      perturbedGraph.putEdgeValue(edge.fromNode, edge.toNode, edge)
    })


  }

  private def getAdjacentNodes(graph: MutableValueGraph[NodeObject, Action], nodeObject: NodeObject) = {
    graph.adjacentNodes(nodeObject)
  }

  def checkGraphValidity(graph: Option[List[NetGraphComponent]]): Unit =
    graph match {
      case Some(graph) => logger.info(s"Graph file not empty, continuing.")
      case None =>
        logger.error(s"Graph file empty, exiting...")
        exit(1)
    }

  // Creates graph components based on the graph file provided by NetGameSim.
  // This has been adapted from NetGameSim as well.
  def createGraphComponents(graph: Option[List[NetGraphComponent]]): Option[List[List[NetGraphComponent with Product]]] =
    graph map { lstOfNetComponents =>
      val nodes = lstOfNetComponents.collect { case node: NodeObject => node }
      val edges = lstOfNetComponents.collect { case edge: Action => edge }
      List(nodes, edges)
    }
}
