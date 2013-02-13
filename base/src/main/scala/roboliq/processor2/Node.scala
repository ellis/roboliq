package roboliq.processor2

import spray.json.JsValue
import spray.json.JsObject

import roboliq.core._

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

case class RqItem_Function(fnargs: RqFunctionArgs) extends RqItem

case class ComputationItem_Event(event: Event) extends RqItem
case class ComputationItem_EntityRequest(id: String) extends RqItem
case class ComputationItem_Command(cmd: JsObject) extends RqItem
case class ComputationItem_Token(token: CmdToken) extends RqItem

case class ConversionItem_Object(obj: Object) extends RqItem


sealed trait Node {
	val id: String
	val parent_? : Option[Node]
	val label_? : Option[String]
	val index_? : Option[Int]
	val path: List[Int]
	val time: List[Int]
	val contextKey_? : Option[TKP]
	val fnargs: RqFunctionArgs
	
	// REFACTOR: remove this
	def input_l = fnargs.arg_l
}

case class Node_Command(
	parent_? : Option[Node],
	index: Int,
	contextKey: TKP,
	fnargs: RqFunctionArgs
) extends Node {
	val path = Node_Command.getCommandPath(parent_?, index)
	val id = Node_Command.getCommandId(path)
	val label_? = None
	val index_? = Some(index)
	val time = path
	val contextKey_? = Some(contextKey)
}

object Node_Command {
	def getCommandPath(parent_? : Option[Node], index: Int): List[Int] =
		parent_?.map(_.path).getOrElse(Nil) ++ List(index)
	def getCommandId(path: List[Int]): String =
		path.mkString("/")
	def getCommandId(parent_? : Option[Node], index: Int): String =
		getCommandId(getCommandPath(parent_?, index))
}

case class Node_Computation(
	parent_? : Option[Node],
	index: Int,
	contextKey_? : Option[TKP],
	fnargs: RqFunctionArgs
) extends Node {
	val path = Node_Command.getCommandPath(parent_?, index)
	val id = Node_Command.getCommandId(path)
	val label_? = None
	val index_? = Some(index)
	val time = path
}

case class Node_Conversion(
	parent_? : Option[Node],
	label_? : Option[String],
	index_? : Option[Int],
	time: List[Int],
	contextKey_? : Option[TKP],
	fnargs: RqFunctionArgs,
	kc: KeyClass
) extends Node {
	val path = parent_?.map(_.path).getOrElse(Nil) ++ index_?
	val id: String = parent_? match {
		case Some(parent: Node_Conversion) => parent.id + "/" + index_?.toList.mkString
		case Some(parent) => parent.id + "#" + index_?.toList.mkString
		case None => kc.id
	}
}
