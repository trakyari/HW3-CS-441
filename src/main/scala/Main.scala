import ConfigReader.getConfigEntry
import Constants._
import NetGraphAlgebraDefs.{Action, NetGraphComponent, NodeObject}
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.google.common.graph.{EndpointPair, Graphs, MutableValueGraph, Traverser, ValueGraphBuilder}
import org.slf4j.Logger
import spray.json.DefaultJsonProtocol._
import org.jgrapht._
import org.jgrapht.alg.connectivity._
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm._
import org.jgrapht.alg.interfaces._
import org.jgrapht.alg.shortestpath._
import org.jgrapht.graph._
import org.jgrapht.graph.guava._
import org.jgrapht.traverse.BreadthFirstIterator

import scala.jdk.CollectionConverters._
import java.net.InetAddress
import scala.collection.IterableOnce.iterableOnceExtensionMethods
import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.math.BigDecimal.RoundingMode
import scala.sys.exit
import scala.util._

class Main

object Main {
  val logger: Logger = CreateLogger(classOf[Main])
  val ipAddr: InetAddress = InetAddress.getLocalHost
  val hostName: String = ipAddr.getHostName
  val hostAddress: String = ipAddr.getHostAddress
  private val originalGraphFileName = getConfigEntry(globalConfig, ORIGINAL_GRAPH_FILE_NAME, DEFAULT_ORIGINAL_GRAPH_FILE_NAME)
  private val perturbedGraphFileName = getConfigEntry(globalConfig, PERTURBED_GRAPH_FILE_NAME, DEFAULT_PERTURBED_ORIGINAL_GRAPH_FILE_NAME)
  private val RANDOM = new Random
  private var players: List[Player] = List()
  private var moves: List[(Int, Int)] = List()

  def main(args: Array[String]): Unit = {
    logger.info(s"The Policeman-Thief game is running on the host $hostName with the following IP addresses:")
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

    val policeman: Player = new Player(RANDOM.nextInt(perturbedGraph.nodes().size()), 1, "Policeman")
    val thief: Player = new Player(RANDOM.nextInt(perturbedGraph.nodes().size()), 2, "Thief")
    if (perturbedGraph.nodes.asScala.toList.filter(_.valuableData).exists(_.id == thief.nodeId)) {
      logger.info("The thief has started a node with valuable data, restarting the game.")
      while (perturbedGraph.nodes.asScala.toList.filter(_.valuableData).exists(_.id == thief.nodeId)) {
        thief.nodeId = RANDOM.nextInt(perturbedGraph.nodes().size())
      }
    }
    players = players :+ policeman
    players = players :+ thief
    moves = moves :+ (policeman.nodeId, thief.nodeId)
    logger.info(s"Policeman will start at node with ID: ${policeman.nodeId}")
    logger.info(s"Thief will start at node with ID: ${thief.nodeId}")

    implicit val system = ActorSystem("PolicemanThiefGameServer")
    implicit val nodeObjectFormat = jsonFormat10(NodeObject)
    implicit val moveFormat = jsonFormat2(Move)
    implicit val responseFormat = jsonFormat1(Response)
    implicit val nodeFormat = jsonFormat2(Node)
    implicit val nearestNodeFormat = jsonFormat2(NearestNode)

    val route: Route = path("nodes") {
      get {
        parameter("id") { (id) =>
          onComplete(Future(adjacentNodes(perturbedGraph, id, originalGraph))) {
            case Success(res) => complete(res)
            case Failure(ex) => complete(ex.toString)
          }
        }
      }
    } ~ path("move") {
      post {
        entity(as[Move]) { move =>
          onComplete(Future(updatePlayerPosition(move, perturbedGraph, originalGraph))) {
            case Success(res) => complete(res)
            case Failure(ex) => complete(ex.toString)
          }
        }
      }
    } ~ path("closest-node-distance") {
      parameter("id") { id =>
        onComplete(Future(findNearestNodeWithValuableData(perturbedGraph, id))) {
          case Success(res) => complete(res)
          case Failure(ex) => complete(ex.toString)
        }
      }
    }

    val server = Http().newServerAt("localhost", 9090).bind(route)
    server.map { _ => logger.info("Successfully started on localhost:9090")
    } recover {
      case ex => logger.error("Failed to start the server due to: " + ex.getMessage)
    }
  }

  def findNearestNodeWithValuableData(perturbedGraph: MutableValueGraph[NodeObject, Action], playerId: String): NearestNode = {
    val initNode = perturbedGraph.nodes().asScala.filter(_.id == 1).head
    val action = Action(1, initNode, initNode, initNode.id, initNode.id, Some(1), 0.0)
    val jgrapht: Graph[NodeObject, Action] = new MutableValueGraphAdapter[NodeObject, Action](perturbedGraph, action, (x: Action) => x.cost).asInstanceOf[Graph[NodeObject, Action]]
    val player = players.filter(_.id == playerId.toInt).head
    val startNode = perturbedGraph.nodes().asScala.filter(_.id == player.nodeId).head

    val bfsIterator = new BreadthFirstIterator(jgrapht, startNode)
    val bfsNearestValuableNode = bfsIterator.asScala.find(_.valuableData)
    val bfsNearestValuableNodeDistance = bfsNearestValuableNode.map(bfsIterator.getDepth)

    NearestNode(bfsNearestValuableNode.map(_.id) getOrElse(-1), bfsNearestValuableNodeDistance getOrElse(-1))
  }

  def calculateConfidenceScore(nodeObject: NodeObject, nodeEdges: List[EndpointPair[NodeObject]], originalGraph: MutableValueGraph[NodeObject, Action]): Double = {
    if (!originalGraph.nodes().asScala.toList.contains(nodeObject)) {
      return 0.0
    }

    val originalGraphNodeEdges = originalGraph.incidentEdges(nodeObject).asScala.toList
    val confidenceScore: Double = nodeEdges.map(originalGraphNodeEdges.contains(_)).map(boolean => if (boolean) 1.0 else 0.0).sum / nodeEdges.length.toDouble
    BigDecimal(confidenceScore).setScale(2, RoundingMode.HALF_UP).toDouble
  }

  def updatePlayerPosition(move: Move, perturbedGraph: MutableValueGraph[NodeObject, Action], originalGraph: MutableValueGraph[NodeObject, Action]): Response = {
    val neighboringNodes = adjacentNodes(perturbedGraph, move.playerId.toString, originalGraph)
    val nodeObject = perturbedGraph.nodes().asScala.filter(_.id == move.nodeId).head
    val player = players.filter(_.id == move.playerId).head
    player.nodeId = move.nodeId

    if (!originalGraph.nodes().asScala.contains(nodeObject)) {
      Response("The node does not exist in the original graph!")
    }
    else if (adjacentNodes(perturbedGraph, move.playerId.toString, originalGraph).isEmpty) {
      Response("You lose!")
    }
    else if (player.role == "Thief" && players.filter(_.role == "Policeman").head.nodeId == move.nodeId) {
      Response("You lose!")
    }
    else if (player.role == "Thief" && nodeObject.valuableData) {
      Response("You win!")
    } else {
      Response("OK")
    }
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

  private def adjacentNodes(perturbedGraph: MutableValueGraph[NodeObject, Action], id: String, originalGraph: MutableValueGraph[NodeObject, Action]): List[Node] = {
    val player = players.filter(_.id == id.toInt).head
    val playerCurrentNode = perturbedGraph.nodes().toArray.filter(_.asInstanceOf[NodeObject].id == player.nodeId)
    getAdjacentNodes(perturbedGraph, playerCurrentNode.head.asInstanceOf[NodeObject]).asScala.toList.map(nodeObject => {
      val edges = perturbedGraph.incidentEdges(nodeObject).asScala.toList
      Node(nodeObject.id, calculateConfidenceScore(nodeObject, edges, originalGraph))
    })
  }

  private def getAdjacentNodes(graph: MutableValueGraph[NodeObject, Action], nodeObject: NodeObject) = {
    graph.adjacentNodes(nodeObject)
  }

  case class NearestNode(id: Int, distance: Int)

  case class Move(playerId: Int, nodeId: Int)

  case class Node(id: Int, confidenceScore: Double)

  case class Response(message: String)
}
