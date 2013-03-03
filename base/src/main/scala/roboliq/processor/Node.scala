package roboliq.processor

import spray.json.JsValue
import spray.json.JsObject

import roboliq.core._
import roboliq.commands._

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

case class ComputationItem_Events(event_l: List[Event]) extends RqItem
case class ComputationItem_EntityRequest(id: String) extends RqItem
case class ComputationItem_Command(cmd: JsObject) extends RqItem
case class ComputationItem_Token(token: CmdToken) extends RqItem

case class ConversionItem_Object(obj: Object) extends RqItem

case class EventItem_State(key: TKP, jsval: JsValue) extends RqItem


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
	fnargs0: RqFunctionArgs
) extends Node {
	val path = Node_Command.getCommandPath(parent_?, index)
	val id = Node_Command.getCommandId(path)
	val label_? = None
	val index_? = Some(index)
	val time = path
	val contextKey = TKP("cmd", id, Nil)
	val contextKey_? = Some(contextKey)
	val fnargs = concretizeArgs
	
	println()
	println("Node_Command "+id)
	println("contextKey: "+contextKey.id)
	println("args: "+fnargs.arg_l)
	println()

	private def concretizeArgs: RqFunctionArgs = {
		fnargs0.copy(arg_l = concretizeArgs(fnargs0.arg_l))
	}

	private def concretizeArgs(
		kco_l: List[KeyClassOpt]
	): List[KeyClassOpt] = {
		val idPrefix = path.mkString("", "/", "#")
		kco_l.zipWithIndex.map(pair => {
			val (kco0, i) = pair
			// Set time if this is a state variables
			val kco = if (kco0.kc.key.table.endsWith("State")) kco0.changeTime(time) else kco0
			// Substitute in full path for "context" arguments
			if (kco.kc.key.key == "$") {
				val key2 = contextKey.copy(key = id, path = contextKey.path ++ kco.kc.key.path)
				kco.copy(kc = kco.kc.copy(key = key2))
			}
			else if (kco.kc.key.key == "#") kco.changeKey(idPrefix+(i+1))
			else kco
		})
	}
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
	fnargs0: RqFunctionArgs
) extends Node {
	val path = Node_Command.getCommandPath(parent_?, index)
	val id = Node_Command.getCommandId(path)
	val label_? = None
	val index_? = Some(index)
	val time = path
	val fnargs = concretizeArgs
	
	println()
	println("Node_Computation "+id)
	println("contextKey: "+contextKey_?.map(_.id))
	println("args: "+fnargs.arg_l)
	println()

	private def concretizeArgs: RqFunctionArgs = {
		fnargs0.copy(arg_l = concretizeArgs(fnargs0.arg_l))
	}

	private def concretizeArgs(
		kco_l: List[KeyClassOpt]
	): List[KeyClassOpt] = {
		val idPrefix = path.mkString("", "/", "#")
		kco_l.zipWithIndex.map(pair => {
			val (kco0, i) = pair
			// Set time if this is a state variables
			val kco = if (kco0.kc.key.table.endsWith("State")) kco0.changeTime(time) else kco0
			// Substitute in full path for "context" arguments
			if (kco.kc.key.key == "$" && contextKey_?.isDefined) {
				val contextKey = contextKey_?.get
				val key2 = contextKey.copy(key = id, path = contextKey.path ++ kco.kc.key.path)
				kco.copy(kc = kco.kc.copy(key = key2))
			}
			else if (kco.kc.key.key == "#") kco.changeKey(idPrefix+(i+1))
			else kco
		})
	}
}

case class Node_Conversion(
	parent_? : Option[Node],
	label_? : Option[String],
	index_? : Option[Int],
	time: List[Int],
	contextKey0_? : Option[TKP],
	fnargs0: RqFunctionArgs,
	kc: KeyClass
) extends Node {
	val path = parent_?.map(_.path).getOrElse(Nil) ++ index_?
	val id: String = parent_? match {
		case Some(parent: Node_Conversion) => parent.id + "/" + index_?.toList.mkString
		case Some(parent) => parent.id + "#" + index_?.toList.mkString
		case None => kc.id
	}
	/*val paramKey_? : Option[String] = parent_? match {
		case Some(parent: Node_Conversion) => parent.paramKey_?
		case Some(parent) => Some(parent.id + "#" + index_?.toList.mkString)
		case None => None
	}*/
	val contextKey_? : Option[TKP] = parent_? match {
		case Some(parent: Node_Conversion) => Some(parent.kc.key)
		case Some(parent) => parent.contextKey_?
		case None => Some(kc.key)
	}
	val fnargs = concretizeArgs
	
	println()
	println("Node_Conversion "+id)
	println("contextKey: "+contextKey_?)
	println("args0: "+fnargs0.arg_l)
	println("args: "+fnargs.arg_l)
	println()

	private def concretizeArgs: RqFunctionArgs = {
		fnargs0.copy(arg_l = concretizeArgs(fnargs0.arg_l))
	}

	private def concretizeArgs(
		kco_l: List[KeyClassOpt]
	): List[KeyClassOpt] = {
		val idPrefix = path.mkString("", "/", "#")
		kco_l.zipWithIndex.map(pair => {
			val (kco0, i) = pair
			val index = i + 1
			// Set time if this is a state variables
			val kco = if (kco0.kc.key.table.endsWith("State")) kco0.changeTime(time) else kco0
			// Substitute in full path for "context" arguments
			if (kco.kc.key.key == "$" && contextKey_?.isDefined) {
				val contextKey = contextKey_?.get
				val key2 = contextKey.copy(path = contextKey.path ++ kco.kc.key.path)
				kco.copy(kc = kco.kc.copy(key = key2))
			}
			else if (kco.kc.key.key == "#") kco.changeKey(id + "#" + index)
			else kco
		})
	}
}

case class Node_Events(
	parent_? : Option[Node],
	index: Int,
	event_l: List[Event]
) extends Node {
	import roboliq.events._
	
	val path = Node_Command.getCommandPath(parent_?, index)
	val id = Node_Command.getCommandId(path)
	val label_? = None
	val index_? = Some(index)
	val time = path
	val contextKey_? = None
	val fnargs = RqFunctionArgs(
		arg_l = Nil,
		fn = (_) => {
			val l: RqResult[List[RqFunctionArgs]] = RqResult.toResultOfList(event_l.map(event0 => {
				event0 match {
					case event: arm.PlateLocationEvent =>
						val handler = new arm.PlateLocationEventHandler
						RqSuccess(handler.fnargs(event))
					case event: TipAspirateEvent =>
						val handler = new TipAspirateEventHandler
						RqSuccess(handler.fnargs(event))
					case event: TipDispenseEvent =>
						val handler = new TipDispenseEventHandler
						RqSuccess(handler.fnargs(event))
					case _ =>
						RqError(s"No handler for event `$event0`")
				}
			}))
			l.map(_.map(fnargs => RqItem_Function(fnargs)))
		}
	)
	
	println()
	println("Node_Event "+id)
	println("event_l: "+event_l)
	println()
}
