package roboliq.processor2

import spray.json.JsValue
import spray.json.JsObject

import roboliq.core.CmdToken

/**
 * There are several basic types of nodes.
 * 
 * Inner nodes in the computation hierarchy:
 * 
 * Command node: this node represents a JsObject command.  The object's field "cmd" is
 * read, and we lookup a handler for the cmd.  If one is found, the ComputationResult
 * is associated with the command node.
 * 
 * Computation node: child of a command node or a computation node.  Inputs are a list of
 * IdClasses.  Outputs a ComputationResult.
 * 
 * Leaf nodes in the computation hierarchy:
 * 
 * Token node: represents a Token.  Is a leaf node in the computation hierarchy.
 * 
 * Event node: ...
 * 
 * Items in the conversion queue:
 * 
 * Conversion: Input is a list of IdClasses.  Outputs another Conversion or an Object.
 * 
 */


sealed trait RqItem

sealed trait ComputationItem extends RqItem
case class ComputationItem_Event(event: Event) extends ComputationItem
case class ComputationItem_EntityRequest(id: String) extends ComputationItem
case class ComputationItem_Computation(
	entity_l: List[KeyClassOpt],
	fn: (List[Object]) => ComputationResult
) extends ComputationItem
case class ComputationItem_Command(cmd: JsObject) extends ComputationItem
case class ComputationItem_Token(token: CmdToken) extends ComputationItem

sealed trait ConversionItem extends RqItem
case class ConversionItem_Conversion(
	input_l: List[KeyClassOpt],
	fn: (List[Object]) => ConversionResult
) extends ConversionItem
case class ConversionItem_Object(obj: Object) extends ConversionItem


sealed trait Node {
	val parent_? : Option[Node]
	val label_? : Option[String]
	val index_? : Option[Int]
	val time: List[Int]
	val input_l: List[KeyClassOpt]
	//val idCmd: List[Int]
	
	val id: String
	
	lazy val path_r = getPath_r
	lazy val path = path_r.reverse
	
	private def getPath: List[Int] = {
		getPath_r.reverse
	}
	
	private def getPath_r: List[Int] = {
		index_?.map(List(_)).getOrElse(Nil) ++ parent_?.map(_.path_r).getOrElse(Nil)
	}
	
	/*lazy val label: String = {
		(getRootLabel :: path.map(n => Some(n.toString))).flatten.mkString("-") + "@" + time.mkString("-")
	}*/
	
	private def getRootLabel: Option[String] = {
		parent_? match {
			case None => label_?
			case Some(parent) => parent.getRootLabel
		}
	}
}

case class Node_Command(
	parent_? : Option[Node],
	index: Int,
	cmd: JsObject
) extends Node {
	val label_? = None
	val index_? = Some(index)
	val time = path
	val input_l: List[KeyClassOpt] = Nil
	
	val id = path.mkString("/")
	//val idCmd: List[Int] = id

	/*override def toString(): String = {
		s"Node_Command($label, ${cmd})"
	}*/
}

case class Node_Computation(
	parent_? : Option[Node],
	index: Int,
	input_l: List[KeyClassOpt],
	fn: (List[Object]) => ComputationResult
	//idCmd: List[Int]
) extends Node {
	val label_? = None
	val index_? = Some(index)
	val time = path
	
	val id = path.mkString("/")
	
	//override def toString(): String =
	//	s"Node_Computation($label, ${input_l})"
}

/*case class Node_Token(
	parent_? : Option[Node],
	index: Int,
	token: Token
) extends Node {
	val parent_? = Option(parent)
	val label_? = None
	val idCmd = Nil
}*/

/*
class Node_Result(
	parent: Node,
	index: Int,
	val result: ComputationItem
) extends Node(parent, index)
*/

case class Node_Conversion(
	parent_? : Option[Node],
	label_? : Option[String],
	index_? : Option[Int],
	time: List[Int],
	kc: KeyClass,
	input_l: List[KeyClassOpt],
	fn: (List[Object]) => ConversionResult
) extends Node {
	//val idCmd = Nil
	val id: String = parent_? match {
		case Some(parent: Node_Conversion) => parent.id + "/" + index_?.toList.mkString
		case Some(parent) => parent.id + "#" + index_?.toList.mkString
		case None => kc.id
	}
	
	//override def toString(): String = {
	//	s"Node_Conversion($label, ${input_l.mkString("+")})"
}
