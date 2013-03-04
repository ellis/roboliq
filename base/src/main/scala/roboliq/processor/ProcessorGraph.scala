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

private case class EntityData(
	kco: KeyClassOpt,
	status: Status.Value
)

private class StepData(step_i: Int) {
	val json_l = new ArrayBuffer[EntityData]
	val object_l = new ArrayBuffer[EntityData]
	val conv_l = new ArrayBuffer[NodeData]
	val comp_l = new ArrayBuffer[NodeData]
}

class ProcessorGraph {
	private var step_i = -1
	private var stepData: StepData = null
	private val stepData_m = new HashMap[Int, StepData]
	private var index_i = 0
	/** Map tuple (step, node.id) to a node name in the graph */
	private val nodeName_m = new HashMap[(Int, String), String]
	
	def setStep(i: Int) {
		step_i = i
		stepData = stepData_m.getOrElseUpdate(step_i, new StepData(step_i))
	}
	
	def setNode(node: NodeState) {
		val name = s"n${step_i}_${index_i}"
		index_i += 1
		val nodeData = NodeData(step_i, name, node.node, node.status, node.child_l)
		
		node.node match {
			case n: Node_Conversion => stepData.conv_l += nodeData
			case _ => stepData.comp_l += nodeData
		}

		nodeName_m((step_i, node.node.id)) = name
	}
	
	def toDot(): String = {
		val stepData_l = stepData_m.toList.sortBy(_._1)
		
		val nl = nodeData_l.toList.groupBy(_.step_i).mapValues(l => l.sortBy(_.node.id)).toList.sortBy(_._1)
		val l = List[String](
			"digraph G {"
		) ++ stepData_l.flatMap(pair => {
			val (step_i, stepData) = pair
			step
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
