package roboliq.processor

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

private case class NodeData(
	step_i: Int,
	name: String,
	node: Node,
	status: Status.Value,
	child_l: List[Node]
)

class ProcessorGraph {
	private var step_i = 0
	private var index_i = 0
	private val nodeData_l = new ArrayBuffer[NodeData]
	/** Map tuple (step, node.id) to a node name in the graph */
	private val nodeName_m = new HashMap[(Int, String), String]
	
	def setStep(i: Int) {
		step_i = i
	}
	
	def setNode(node: NodeState) {
		val name = s"n${step_i}_${index_i}"
		index_i += 1
		val nodeData = NodeData(step_i, name, node.node, node.status, node.child_l)
		nodeData_l += nodeData
		nodeName_m((step_i, node.node.id)) = name
	}
	
	def toDot(): String = {
		val nl = nodeData_l.toList.groupBy(_.step_i).mapValues(l => l.sortBy(_.node.id)).toList.sortBy(_._1)
		val l = List[String](
			"digraph G {"
		) ++ nl.flatMap(pair => {
			val (step_i, nodeData_l) = pair
			val l2: List[String] = 
				List[List[String]](
					(s"subgraph step${step_i} {") :: 
						("label=\"step "+step_i+"\";") ::
						("color=blue;") :: Nil,
					nodeData_l.map(data => data.name+"[label=\""+data.node.id+"\"];"),
					nodeData_l.flatMap(data => {
						data.child_l.map(child => s"${data.name} -> ${nodeName_m((step_i, child.id))}")
					}),
					"}" :: Nil
				).flatten
			l2
		}) ++ List("}")
		l.mkString("\n")
	}
}
