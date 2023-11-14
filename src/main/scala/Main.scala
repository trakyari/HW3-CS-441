import ConfigReader.getConfigEntry
import Constants._
import NetGraphAlgebraDefs.{Action, NetGraphComponent, NodeObject}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.google.common.graph.{MutableValueGraph, ValueGraphBuilder}
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol._
import scala.jdk.CollectionConverters._


import java.net.InetAddress
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.sys.exit
import scala.util._

class Main
object Main {
  private val originalGraphFileName = getConfigEntry(globalConfig, ORIGINAL_GRAPH_FILE_NAME, DEFAULT_ORIGINAL_GRAPH_FILE_NAME)
  private val perturbedGraphFileName = getConfigEntry(globalConfig, PERTURBED_GRAPH_FILE_NAME, DEFAULT_PERTURBED_ORIGINAL_GRAPH_FILE_NAME)
  private val RANDOM = new Random

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

    val policeman: Player = new Player(RANDOM.nextInt(perturbedGraph.nodes().size()))
    val thief: Player = new Player(RANDOM.nextInt(perturbedGraph.nodes().size()))
    logger.info(s"Policeman will start at node with ID: ${policeman.nodeId}")
    logger.info(s"Thief will start at node with ID: ${thief.nodeId}")

    implicit val system = ActorSystem("PolicemanThiefGameServer")
    implicit val nodeObjectFormat = jsonFormat10(NodeObject)
    val route: Route = path("nodes") {
     get {
        onComplete(Future(adjacentNodes(perturbedGraph, policeman).map(_.id))) {
          case Success(res) => complete(res)
          case Failure(ex) => complete(ex.toString)
        }
      }
    }

    val server = Http().newServerAt("localhost", 9090).bind(route)
    server.map { _ => println("Successfully started on localhost:9090")
    } recover {
      case ex => println("Failed to start the server due to: " + ex.getMessage)
    }
  }

  private def adjacentNodes(graph: MutableValueGraph[NodeObject, Action], player: Player): List[NodeObject] = {
    val playerCurrentNode = graph.nodes().toArray.filter(_.asInstanceOf[NodeObject].id == player.nodeId)
    getAdjacentNodes(graph, playerCurrentNode.head.asInstanceOf[NodeObject]).asScala.toList
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
