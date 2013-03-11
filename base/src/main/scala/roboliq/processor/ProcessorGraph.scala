package roboliq.processor

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import roboliq.core.RqResult

private case class NodeData(
	step_i: Int,
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
		//val name = s"n${step_i}_${index_i}"
		index_i += 1
		val nodeData = NodeData(step_i, node.node, node.status, node.child_l)
		
		node.node match {
			case n: Node_Conversion => stepData.conv_l += nodeData
			case _ => stepData.comp_l += nodeData
		}

		//nodeName_m((step_i, node.node.id)) = name
	}
	
	def setEntities(kcoToValue_m: Map[KeyClassOpt, RqResult[Object]]) {
		for ((kco, value) <- kcoToValue_m) {
			val entityData = EntityData(kco, if (value.isSuccess) Status.Success else Status.NotReady)
			stepData.json_l += entityData
		}
	}
	
	/*def toDs3Json(): String = {
		
		val node_l = conv_l.toList ++ comp_l.toList
		
	}*/

	private val htmlStatusToColor = Map[Status.Value, String](
		Status.NotReady -> "grey",
		Status.Ready -> "green",
		Status.Success -> "blue",
		Status.Error -> "red"
	)
	
	def toHtmlTable(): String = {
		val stepData_l = stepData_m.toList.sortBy(_._1)

		val line_l = new ArrayBuffer[String]
		
		//val nl = nodeData_l.toList.groupBy(_.step_i).mapValues(l => l.sortBy(_.node.id)).toList.sortBy(_._1)
		line_l ++= List("<!DOCTYPE html>", "<html>", "<body>")
		line_l ++= List("<table border='1'>")
		line_l ++= List("<tr><th>time</th><th>node</th><th>inputs</th><th>outputs</th></tr>")
		for ((step_i, stepData) <- stepData_l) {
			val convData_l = stepData.conv_l.toList.sortBy(_.node.id)
			val compData_l = stepData.comp_l.toList.sortBy(_.node.id)
			val kcoToStatus_m = (stepData.json_l ++ stepData.object_l).map(data => data.kco -> data.status).toMap

			line_l += s"<tr><td colspan='4' style='font-weight: bold; font-size: 150%'>Step ${step_i}</td></tr>"
				
			val node_l = (convData_l ++ compData_l).sortBy(_.node.time)(ListIntOrdering)
			for (data <- node_l) {
				line_l += "<tr>"
				val color = htmlStatusToColor(data.status)
				line_l += s"<td>${data.node.time.mkString("/")}</td>"
				line_l += s"<td style='color: $color'>${escape(data.node.id)}</td>"
				line_l += "<td>"
				// Inputs
				for (kco <- data.node.input_l) {
					val color = htmlStatusToColor(kcoToStatus_m(kco))
					line_l += s"<span style='color: $color'>${escape(kco.kc.id)}<span> "
				}
				line_l += "</td>"
				line_l += "</tr>"
			}
			/*
			// Conversion children relationships
			for (data <- convData_l; child <- data.child_l) {
				line_l += s"    ${nodeName_m(data.node.id)} -> ${nodeName_m(child.id)};"
			}
			line_l += "    }"
				
			// Entity nodes
			line_l += "    subgraph {"
			for (data <- stepData.json_l) {
				val id = data.kco.kc.id
				val label = "\""+id+"\""
				val color = dotStatusToColor(data.status)
				line_l += s"    ${entityName_m(id)} [label=$label,fillcolor=$color,style=filled,shape=box];"
			}
			line_l += "    }"

			// Computation nodes
			line_l += "    subgraph {"
			for (data <- compData_l) {
				val label = "\""+data.node.id+"\""
				val color = dotStatusToColor(data.status)
				line_l += s"    ${nodeName_m(data.node.id)} [label=$label,fillcolor=$color,style=filled];"
			}
			// Computation children relationships
			for (data <- compData_l; child <- data.child_l) {
				line_l += s"    ${nodeName_m(data.node.id)} -> ${nodeName_m(child.id)};"
			}
			line_l += "    }"
				
			// Inputs
			for (data <- (convData_l ++ compData_l); kco <- data.node.input_l) {
				line_l += s"    ${entityName_m(kco.kc.id)} -> ${nodeName_m(data.node.id)} [style=dotted];"
			}
			line_l += "  }"
			*/
		}
		line_l += "</table>"
		line_l ++= List("</body>", "</html>")
		line_l.mkString("\n")
	}
	
	private def escape(s: String): String = {
		s.replace("<", "&lt;").replace(">", "&gt;")
	}

	private val dotStatusToColor = Map[Status.Value, String](
		Status.NotReady -> "grey",
		Status.Ready -> "green",
		Status.Success -> "white",
		Status.Error -> "red"
	)

	def toDot(): String = {
		val stepData_l = stepData_m.toList.sortBy(_._1)

		val line_l = new ArrayBuffer[String]
		
		//val nl = nodeData_l.toList.groupBy(_.step_i).mapValues(l => l.sortBy(_.node.id)).toList.sortBy(_._1)
		line_l += "digraph G {"
		line_l += "  rankdir=LR;"
		for ((step_i, stepData) <- stepData_l) {
			val convData_l = stepData.conv_l.toList.sortBy(_.node.id)
			val compData_l = stepData.comp_l.toList.sortBy(_.node.id)
			val nodeName_m: Map[String, String] =
				convData_l.zipWithIndex.map(pair => pair._1.node.id -> s"s${step_i}o${pair._2}").toMap ++
				compData_l.zipWithIndex.map(pair => pair._1.node.id -> s"s${step_i}f${pair._2}").toMap
			val entityName_m: Map[String, String] = stepData.json_l.toList.zipWithIndex.map(pair => pair._1.kco.kc.id -> s"s${step_i}e${pair._2}").toMap
			line_l += s"  subgraph cluster_${step_i} {"
			line_l += "    label=\"Step "+step_i+"\";"
			line_l += "    color=blue;"
			line_l += "    rank=same;"
				
			// Conversion nodes
			line_l += "    subgraph {"
			for (data <- convData_l) {
				val label = "\""+data.node.id+"\""
				val color = dotStatusToColor(data.status)
				line_l += s"    ${nodeName_m(data.node.id)} [label=$label,fillcolor=$color,style=filled];"
			}
			// Conversion children relationships
			for (data <- convData_l; child <- data.child_l) {
				line_l += s"    ${nodeName_m(data.node.id)} -> ${nodeName_m(child.id)};"
			}
			line_l += "    }"
				
			// Entity nodes
			line_l += "    subgraph {"
			for (data <- stepData.json_l) {
				val id = data.kco.kc.id
				val label = "\""+id+"\""
				val color = dotStatusToColor(data.status)
				line_l += s"    ${entityName_m(id)} [label=$label,fillcolor=$color,style=filled,shape=box];"
			}
			line_l += "    }"

			// Computation nodes
			line_l += "    subgraph {"
			for (data <- compData_l) {
				val label = "\""+data.node.id+"\""
				val color = dotStatusToColor(data.status)
				line_l += s"    ${nodeName_m(data.node.id)} [label=$label,fillcolor=$color,style=filled];"
			}
			// Computation children relationships
			for (data <- compData_l; child <- data.child_l) {
				line_l += s"    ${nodeName_m(data.node.id)} -> ${nodeName_m(child.id)};"
			}
			line_l += "    }"
				
			// Inputs
			for (data <- (convData_l ++ compData_l); kco <- data.node.input_l) {
				line_l += s"    ${entityName_m(kco.kc.id)} -> ${nodeName_m(data.node.id)} [style=dotted];"
			}
			line_l += "  }"
		}
		line_l += "}"
		line_l.mkString("\n")
	}
}
