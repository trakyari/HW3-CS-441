import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatestplus.mockito.MockitoSugar
import Main._
import NetGraphAlgebraDefs.{Action, NodeObject}
import com.google.common.graph.{MutableValueGraph, ValueGraphBuilder}
import scala.jdk.CollectionConverters._

class MainTest extends AnyFlatSpec with Matchers with MockitoSugar {
  behavior of "Calculating confidence score"
  it should "should return 0.0 if the node object is not in the original graph" in {
    val nodeObject = NodeObject(1, 1, 1, 1, 1, 1, 1, 1, 0.01, valuableData = false)
    val originalNodeObject = NodeObject(2, 1, 1, 1, 1, 1, 1, 1, 0.01, valuableData = false)
    val originalGraph: MutableValueGraph[NodeObject, Action] = ValueGraphBuilder.directed().build()
    val perturbedGraph: MutableValueGraph[NodeObject, Action] = ValueGraphBuilder.directed().build()
    originalGraph.addNode(originalNodeObject)
    perturbedGraph.addNode(nodeObject)
    val edges = perturbedGraph.incidentEdges(nodeObject).asScala.toList
    calculateConfidenceScore(nodeObject, edges, originalGraph) shouldBe 0.0
  }
  it should "should return 1.0 if the node neighbors all match" in {
    val secondNodeObject = NodeObject(2, 1, 1, 1, 1, 1, 1, 1, 0.01, valuableData = false)
    val nodeObject = NodeObject(1, 1, 1, 1, 1, 1, 1, 1, 0.01, valuableData = false)
    val edge = Action(1, nodeObject, secondNodeObject, nodeObject.id, secondNodeObject.id, Option(1), 0.0)
    val originalGraph: MutableValueGraph[NodeObject, Action] = ValueGraphBuilder.directed().build()
    val perturbedGraph: MutableValueGraph[NodeObject, Action] = ValueGraphBuilder.directed().build()
    originalGraph.addNode(nodeObject)
    originalGraph.putEdgeValue(nodeObject, secondNodeObject, edge)
    perturbedGraph.addNode(nodeObject)
    perturbedGraph.putEdgeValue(nodeObject, secondNodeObject, edge)
    val edges = perturbedGraph.incidentEdges(nodeObject).asScala.toList
    calculateConfidenceScore(nodeObject, edges, originalGraph) shouldBe 1.0
  }
  it should "should return 1.0 if the all original edges match but perturbed node has extra edge" in {
    val thirdNodeObject = NodeObject(3, 1, 1, 1, 1, 1, 1, 1, 0.01, valuableData = false)
    val secondNodeObject = NodeObject(2, 1, 1, 1, 1, 1, 1, 1, 0.01, valuableData = false)
    val nodeObject = NodeObject(1, 1, 1, 1, 1, 1, 1, 1, 0.01, valuableData = false)
    val edge = Action(1, nodeObject, secondNodeObject, nodeObject.id, secondNodeObject.id, Option(1), 0.0)
    val secondEdge = Action(1, nodeObject, thirdNodeObject, nodeObject.id, thirdNodeObject.id, Option(1), 0.0)
    val originalGraph: MutableValueGraph[NodeObject, Action] = ValueGraphBuilder.directed().build()
    val perturbedGraph: MutableValueGraph[NodeObject, Action] = ValueGraphBuilder.directed().build()
    originalGraph.addNode(nodeObject)
    originalGraph.putEdgeValue(nodeObject, secondNodeObject, edge)
    perturbedGraph.addNode(nodeObject)
    perturbedGraph.putEdgeValue(nodeObject, secondNodeObject, edge)
    perturbedGraph.putEdgeValue(nodeObject, thirdNodeObject, secondEdge)
    val edges = perturbedGraph.incidentEdges(nodeObject).asScala.toList
    calculateConfidenceScore(nodeObject, edges, originalGraph) shouldBe 1.0
  }
  it should "should return 0.5 if the half the original perturbed neighbors match" in {
    val thirdNodeObject = NodeObject(3, 1, 1, 1, 1, 1, 1, 1, 0.01, valuableData = false)
    val secondNodeObject = NodeObject(2, 1, 1, 1, 1, 1, 1, 1, 0.01, valuableData = false)
    val nodeObject = NodeObject(1, 1, 1, 1, 1, 1, 1, 1, 0.01, valuableData = false)
    val edge = Action(1, nodeObject, secondNodeObject, nodeObject.id, secondNodeObject.id, Option(1), 0.0)
    val secondEdge = Action(1, nodeObject, thirdNodeObject, nodeObject.id, thirdNodeObject.id, Option(1), 0.0)
    val originalGraph: MutableValueGraph[NodeObject, Action] = ValueGraphBuilder.directed().build()
    val perturbedGraph: MutableValueGraph[NodeObject, Action] = ValueGraphBuilder.directed().build()
    originalGraph.addNode(nodeObject)
    originalGraph.putEdgeValue(nodeObject, secondNodeObject, edge)
    originalGraph.putEdgeValue(nodeObject, thirdNodeObject, secondEdge)
    perturbedGraph.addNode(nodeObject)
    perturbedGraph.putEdgeValue(nodeObject, secondNodeObject, edge)
    val edges = perturbedGraph.incidentEdges(nodeObject).asScala.toList
    calculateConfidenceScore(nodeObject, edges, originalGraph) shouldBe 0.5
  }
  behavior of "Getting adjacent nodes"
  it should "return the adjacent nodes" in {
    val thirdNodeObject = NodeObject(3, 1, 1, 1, 1, 1, 1, 1, 0.01, valuableData = false)
    val secondNodeObject = NodeObject(2, 1, 1, 1, 1, 1, 1, 1, 0.01, valuableData = false)
    val nodeObject = NodeObject(1, 1, 1, 1, 1, 1, 1, 1, 0.01, valuableData = false)
    val edge = Action(1, nodeObject, secondNodeObject, nodeObject.id, secondNodeObject.id, Option(1), 0.0)
    val secondEdge = Action(1, nodeObject, thirdNodeObject, nodeObject.id, thirdNodeObject.id, Option(1), 0.0)
    val originalGraph: MutableValueGraph[NodeObject, Action] = ValueGraphBuilder.directed().build()
    val perturbedGraph: MutableValueGraph[NodeObject, Action] = ValueGraphBuilder.directed().build()
    originalGraph.addNode(nodeObject)
    originalGraph.putEdgeValue(nodeObject, secondNodeObject, edge)
    originalGraph.putEdgeValue(nodeObject, thirdNodeObject, secondEdge)
    perturbedGraph.addNode(nodeObject)
    perturbedGraph.putEdgeValue(nodeObject, secondNodeObject, edge)
    val policeman: Player = new Player(1, 1, "Policeman")
    var players: List[Player] = List()
    players = players :+ policeman
    val adjNodes = adjacentNodes(perturbedGraph, 1.toString, originalGraph, players)
    adjNodes.length should be > 0
  }
}
