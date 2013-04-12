package roboliq.processor

import scala.reflect.runtime.universe.Type

import spray.json.JsValue
import spray.json.JsObject

import roboliq.core._
import roboliq.entity.Entity
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

case class ComputationItem_Events(event_l: List[Event[Entity]]) extends RqItem
case class ComputationItem_EntityRequest(id: String) extends RqItem
case class ComputationItem_Entity(tpe: Type, entity: Entity) extends RqItem
case class ComputationItem_Command(cmd: Cmd) extends RqItem
case class ComputationItem_Token(token: CmdToken) extends RqItem

case class ConversionItem_Object(obj: Entity) extends RqItem

case class EventItem_State(tpe: Type, state: Entity) extends RqItem


/**
 * @param path path to node in tree? (not sure)
 * @param time time of this node for the purpose of state reading/writing
 */
sealed trait Node {
	val id: String
	val parent_? : Option[Node]
	val label_? : Option[String]
	val index_? : Option[Int]
	val path: List[Int]
	val time: List[Int]
	val fnargs: RqFunctionArgs
	val desc: String
}

case class Node_Command(
	parent_? : Option[Node],
	index: Int,
	fnargs: RqFunctionArgs,
	desc: String,
	cmd: Cmd
) extends Node {
	val path = Node_Command.getCommandPath(parent_?, index)
	val id = Node_Command.getCommandId(path)
	val label_? = None
	val index_? = Some(index)
	val time = path
	
	println()
	println("Node_Command "+id)
	println()
}

object Node_Command {
	def getCommandPath(parent_? : Option[Node], index: Int): List[Int] =
		parent_?.map(_.path).getOrElse(Nil) ++ List(index)
	def getCommandId(path: List[Int]): String =
		path.mkString("/")
	def getCommandId(parent_? : Option[Node], index: Int): String =
		getCommandId(getCommandPath(parent_?, index))
}

// REFACTOR: Rename to Node_Entity
case class Node_Entity(
	key: Key,
	fnargs: RqFunctionArgs
) extends Node {
	val parent_? = None
	val label_? = None
	val index_? = None
	val path = Nil
	val id = key.name
	val desc = fnargs.arg_l.map(_.name).mkString("(", ", ", ") => " + key.name)
	
	println()
	println("Node_Conversion "+id)
	println("desc: "+desc)
	println()
}

// REFACTOR: Rename to Node_Entity
case class Node_Lookup(
	parent: Node,
	index: Int,
	lookupKey: LookupKey,
	fnargs: RqFunctionArgs
) extends Node {
	val parent_? = Some(parent)
	val label_? = None
	val index_? = Some(index)
	val path = parent_?.map(_.path).getOrElse(Nil) ++ index_?
	val id = s"${parent.id}#$index"
	val desc = fnargs.arg_l.map(_.name).mkString("(", ", ", ") => " + lookupKey.name)
	
	println()
	println("Node_Lookup "+id)
	println("desc: "+desc)
	println()
}

case class Node_Computation(
	parent_? : Option[Node],
	index: Int,
	fnargs0: RqFunctionArgs
) extends Node {
	val path = Node_Command.getCommandPath(parent_?, index)
	val id = Node_Command.getCommandId(path)
	val label_? = None
	val index_? = Some(index)
	val time = path
	val fnargs = concretizeArgs
	val desc = fnargs.arg_l.map(_.name).mkString("(", ", ", ") => Result")
	
	println()
	println("Node_Computation "+id)
	println("desc: "+desc)
	println()
}

/*
// REFACTOR: Rename to Node_Entity
case class Node_Conversion(
	parent_? : Option[Node],
	label_? : Option[String],
	index_? : Option[Int],
	key: Key,
	fnargs: RqFunctionArgs
) extends Node {
	val path = parent_?.map(_.path).getOrElse(Nil) ++ index_?
	val id: String = parent_? match {
		case Some(parent: Node_Conversion) => parent.id + "/" + index_?.toList.mkString
		case Some(parent) => parent.id + "#" + index_?.toList.mkString
		case None => entityId
	}
	val desc = fnargs.arg_l.map(_.keyName).mkString("(", ", ", ") => Object")
	
	println()
	println("Node_Conversion "+id)
	println("contextKey: "+contextKey_?)
	println("args: "+fnargs.arg_l)
	println()
}
*/

// REFACTOR: HACK: this is rather hacky approach to translating events into states.  It'd be better to have something more centralized in Processor.
case class Node_Events(
	parent_? : Option[Node],
	index: Int,
	event_l: List[Event[Entity]],
	eventHandler_m: Map[Class[_], EventHandler]
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
			// RqReturn = RqResult[List[RqItem]]
			val l1: List[RqReturn] = event_l.map(event => {
				eventHandler_m.get(event.getClass) match {
					case None => RqError(s"No handler for event `$event`, class `${event.getClass}`")
					case Some(handler) => handler.fnargs(event)
				}
			})
			val l2: RqResult[List[List[RqItem]]] = RqResult.toResultOfList(l1)
			val l3: RqReturn = l2.map(_.flatten)
			l3
		}
	)
	val desc = "events: "+event_l
	
	println()
	println("Node_Event "+id)
	println("event_l: "+event_l)
	println()
}
