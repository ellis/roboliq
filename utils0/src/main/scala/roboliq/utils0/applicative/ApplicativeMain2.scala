package roboliq.utils0.applicative

import scala.language.implicitConversions
import scala.language.postfixOps
import scala.collection.mutable.ArrayBuffer
import scala.collection._
import scalaz._
import spray.json.JsObject
import spray.json.JsString
import spray.json.JsValue
import spray.json.JsNull
import spray.json.JsonParser
import scala.collection.mutable.HashMap
import scala.collection.mutable.MultiMap
import scala.math.Ordering


/**
 * A command is a JSON object that represents a command and its parameters.
 * A command handler produces a Computation which requires the command's 
 * json parameters and objects in the database or programmatically generated objects,
 * and it returns a list of computations, events, commands, and tokens.  
 */

sealed trait ComputationResult
case class ComputationResult_Event(event: Event) extends ComputationResult
case class ComputationResult_EntityRequest(id: String) extends ComputationResult
case class ComputationResult_Computation(
	entity_l: List[String],
	fn: (List[JsValue]) => RqResult[List[ComputationResult]]
) extends ComputationResult
case class ComputationResult_Command(command: Command) extends ComputationResult
case class ComputationResult_Token(token: Token) extends ComputationResult

case class ComputationSpec(
	entity_l: List[String],
	fn: (List[JsValue]) => RqResult[List[ComputationResult]]
)

case class Computation(
	id_r: List[Int],
	entity_l: List[String],
	fn: (List[JsValue]) => RqResult[List[ComputationResult]]
) {
	val id: List[Int] = id_r.reverse
}

case class ComputationNode(
	computation: Computation
)

/*
case class Data(
	table: String,
	key: String,
	field: String
)
class Obj
*/
class Event
class Command
trait CommandHandler {
	val cmd_l: List[String]
	def getResult: RqResult[List[ComputationResult]]
}
trait Token

case class Token_Comment(s: String) extends Token

case class Node(parent: Node, index: Int, result: ComputationResult) {
	def getId: List[Int] = {
		getId_r.reverse
	}
	
	def getId_r: List[Int] = {
		index :: (if (parent != null) parent.getId else Nil)
	}
}

//private class Message(id: List[Int], level: Int, message: String)

class ProcessorData {
	// key is a Computation.id_r
	val root = Node(null, 0, null)
	val message_m = new HashMap[Node, List[String]]
	val children_m = new HashMap[Node, List[Node]]
	val status_m = new HashMap[Node, Int]
	val dep_m: MultiMap[String, Node] = new HashMap[String, scala.collection.mutable.Set[Node]] with MultiMap[String, Node]
	//val node_l = new scala.collection.mutable.HashSet[Node]
	
	val cmdToJs_m = new HashMap[List[Int], JsObject]
	val cn_l = new ArrayBuffer[Computation]
	val cn_m = new HashMap[List[Int], Computation]
	val cnQueue = new scala.collection.mutable.LinkedHashSet[Computation]
	val cnWaiting = new scala.collection.mutable.LinkedHashSet[Computation]()
	val entity_m = new HashMap[String, JsValue]
	val dependency_m = new HashMap[String, List[Computation]]
	val token_l = new ArrayBuffer[Token]
	// Number of commands 
	val cnToCommands_m = new HashMap[List[Int], Integer]

	def setComputationResult(node: Node, result: RqResult[List[ComputationResult]]) {
		val child_l: List[Node] = result match {
			case RqSuccess(l, warning_l) =>
				setMessages(node, warning_l)
				l.zipWithIndex.map { case (r, index) => Node(node, index, r) }
			case RqError(error_l, warning_l) =>
				setMessages(node, error_l ++ warning_l)
				Nil
		}
		setChildren(node, child_l)
	}
	
	private def setMessages(node: Node, message_l: List[String]) {
		if (message_l.isEmpty)
			message_m -= node
		else
			message_m(node) = message_l
	}
	
	private def setChildren(node: Node, child_l: List[Node]) {
		if (child_l.isEmpty)
			children_m -= node
		else
			children_m(node) = child_l
		
		for (child <- child_l) {
			child.result match {
				case ComputationResult_Computation(input_l, _) =>
					addDependencies(child, input_l)
					updateComputationStatus(child, input_l)
				case ComputationResult_EntityRequest(idEntity) =>
					setEntity(idEntity, JsString("my "+idEntity))
				case _ =>
			}
		}
		//node_l ++= child_l
	}
	
	private def setEntity(id: String, jsval: JsValue) {
		entity_m(id) = jsval
		// Queue the computations for which all inputs are available 
		dep_m.get(id).map(_.foreach(updateComputationStatus))
	}

	// Add node to dependency set for each of its inputs
	private def addDependencies(node: Node, input_l: List[String]) {
		input_l.foreach(s => dep_m.addBinding(s, node))
	}
	
	private def updateComputationStatus(node: Node) {
		node.result match {
			case ComputationResult_Computation(input_l, _) =>
				updateComputationStatus(node, input_l)
			case _ =>
				System.err.println("INTERNAL: updateComputationStatus() called on non-computation node")
		}
	}
	
	private def updateComputationStatus(node: Node, input_l: List[String]) {
		status_m(node) = if (input_l.forall(entity_m.contains)) 1 else 0
	}
	/*
	
	def addCommand(idParent: List[Int], cmd: JsObject, handler: CommandHandler) {
		val cn = addComputation(idParent, Nil, (l: List[JsValue]) => handler.getResult)
		cmdToJs_m(cn.id_r) = cmd
	}
	
	def getComputationChildren(idParent: List[Int]): List[Computation] = {
		cn_l.toList.filter(_.id.tail == idParent)
	}
	
	def getComputationDecendents(id: List[Int]): List[Computation] = {
		cn_l.filter(_.id.startsWith(id)).toList
	}
	
	def getNextChildId_r(idParent: List[Int]): List[Int] = {
		val i  = cn_l.filter(_.id.tail == idParent).map(_.id_r.head).foldLeft(0)(_ max _) + 1
		i :: idParent
	}
	
	def setComputationChildren(parent: Computation, children: List[Computation]) {
		
	}
	
	def addComputation(
		idParent: List[Int],
		input_l: List[String],
		fn: (List[JsValue]) => RqResult[List[ComputationResult]]
	): Computation = {
		val id_r = getNextChildId_r(idParent)
		val id = id_r.reverse
		val cn = Computation(id_r, input_l, fn)
		addComputation(cn)
		cn
	}
	
	private def addComputation(cn: Computation) {
		cn_l += cn
		cn_m(cn.id) = cn
		// Add dependencies
		for (id <- cn.entity_l) {
			val l: List[Computation] = dependency_m.get(id).getOrElse(Nil)
			dependency_m(id) = l ++ List(cn)
		}
		addToQueueOrWaiting(cn)
	}

	private def addToQueueOrWaiting(cn: Computation) {
		if (cn.entity_l.forall(entity_m.contains)) {
			cnQueue += cn
			cnWaiting -= cn
		}
		else {
			cnQueue -= cn
			cnWaiting += cn
		}
	}
	
	def addEntity(id: String, jsval: JsValue) {
		entity_m(id) = jsval
		// Queue the computations for which all inputs are available 
		dependency_m.get(id).map(_.foreach(addToQueueOrWaiting))
	}
	
	def addToken(token: Token) {
		token_l += token
	}
	
	def addComputationResult(cn: Computation, result: RqResult[List[ComputationResult]]) {
		result match {
			case RqSuccess(l, warning_l) =>
				addMessages(cn.id, warning_l)
				l.foreach(_ match {
					case ComputationResult_Token(t) => addToken(t)
					case ComputationResult_Computation(l, fn) => addComputation(cn, l, fn)
					case ComputationResult_EntityRequest(idEntity) => addEntity(idEntity, JsString("my "+idEntity))
					case _ =>
				})
			case RqError(error_l, warning_l) =>
				addMessages(cn.id, error_l ++ warning_l)
				None
		}
	}
	
	def addMessages(id: List[Int], message_l: List[String]) {
		if (message_l.isEmpty)
			message_m -= id
		else
			message_m(id) = message_l
	}
	*/
	
	def makeComputationList: List[Node] = {
		status_m.filter(_._1 == 1).keys.toList.sorted(NodeOrdering)
	}
	
	def handleComputation(node: Node) {
		val input_l: List[JsValue] = cn.entity_l.map(entity_m)
		addComputationResult(cn.fn(entity_l), cn.id)
		true
	}
	
	def makeMessagesForMissingInputs() {
		for (cn <- cnWaiting) {
			val message_l = cn.entity_l.filterNot(entity_m.contains).map(entity => s"ERROR: missing command parameter `$entity`")
			addMessages(cn.id, message_l) 
		}
	}
	
	def getMessages(): List[String] = {
		message_m.toList.sortWith(compMessage).flatMap { pair =>
			val id = getIdString(pair._1)
			pair._2.map(s => s"$id: $s")
		}
	}
	
	def getIdString(id: List[Int]) = id.mkString(".")

	def compComputation(cn1: Computation, cn2: Computation): Boolean =
		compId(cn1.id, cn2.id)
	def compMessage(cn1: (List[Int], List[String]), cn2: (List[Int], List[String])): Boolean =
		compId(cn1._1, cn2._1)
	def compId(l1: List[Int], l2: List[Int]): Boolean = {
		if (l2.isEmpty) true
		else if (l1.isEmpty) true
		else {
			val n = l1.head - l2.head
			if (n < 0) true
			else if (n > 0) false
			else compId(l1.tail, l2.tail)
		}
	}
	
	implicit object ListIntOrdering extends Ordering[List[Int]] {
		def compare(a: List[Int], b: List[Int]): Boolean = {
			if (a.isEmpty) true
			else if (b.isEmpty) false
			else {
				val n = a.head - b.head
				if (n < 0) true
				else if (n > 0) false
				else compare(a.tail, b.tail)
			}
		}
	}
	
	implicit object NodeOrdering extends Ordering[Node] {
		def compare(a: Node, b: Node): Boolean = {
			ListIntOrdering.compare(a.getId, b.getId)
		}
	}
}

class PrintCommandHandler extends CommandHandler {
	val cmd_l = List[String]("print") 
	
	def getResult: RqResult[List[ComputationResult]] = {
		RqSuccess(
			List(
				ComputationResult_Computation(List("!text"), (l) => l match {
					case List(JsString(text)) =>
						RqSuccess(List(
								ComputationResult_Token(Token_Comment(text))
						))
					case _ => RqError("invalid parameters")
				})
			)
		)
	}
}

/**
 * Steps to test:
 * 
 * - nodes with dependencies, but no children
 * - caching errors and producing a sensible list of errors and warnings
 * - cache transformations of JsValues (e.g. Plate objects)
 * - deal with states
 */

object ApplicativeMain2 extends App {
	val cn1 = ComputationSpec(List("a", "b"), (l) => RqSuccess(
			List(
				ComputationResult_Token(Token_Comment(l.mkString)),
				ComputationResult_EntityRequest("d")
			)))
	val cn2 = ComputationSpec(List("c", "d"), (l) => RqSuccess(
			List(
				ComputationResult_Token(Token_Comment(l.mkString)),
				ComputationResult_Computation(List("d"), (l) => RqSuccess(
					List(
						ComputationResult_Token(Token_Comment(l.mkString * 3))
					)
				))
			)))
	val cmd1 = JsonParser("""{ "cmd": "print", "text": "Hello, World" }""").asJsObject
	
	val p = new ProcessorData
	
	p.addEntity("a", JsString("1"))
	p.addEntity("b", JsString("2"))
	p.addEntity("c", JsString("3"))
	p.addEntity("cmd[1].text", JsString("Hello, World"))
	//println(p.cnQueue)
	p.addComputation(cn1.entity_l, cn1.fn, Nil)
	//println(p.cnQueue)
	p.addComputation(cn2.entity_l, cn2.fn, Nil)
	//println(p.cnQueue)
	val h1 = new PrintCommandHandler
	p.addCommand(cmd1, h1)
	
	while (p.handleNextComputation) {
		println(p.cnQueue)
	}
	p.makeMessagesForMissingInputs()

	def compComputation(cn1: Computation, cn2: Computation): Boolean =
		compId(cn1.id, cn2.id)
	def compMessage(cn1: (List[Int], List[String]), cn2: (List[Int], List[String])): Boolean =
		compId(cn1._1, cn2._1)
	def compId(l1: List[Int], l2: List[Int]): Boolean = {
		if (l2.isEmpty) true
		else if (l1.isEmpty) true
		else {
			val n = l1.head - l2.head
			if (n < 0) true
			else if (n > 0) false
			else compId(l1.tail, l2.tail)
		}
	}

	println()
	println("Computations:")
	p.cn_l.toList.sortWith(compComputation).map(cn => cn.id.mkString(".")+" "+cn.entity_l).foreach(println)

	println()
	println("Entities:")
	p.entity_m.toList.sortBy(_._1).foreach(pair => println(pair._1+" "+pair._2))
	
	println()
	println("Tokens:")
	p.token_l.toList.foreach(println)
	
	println()
	println("Messages:")
	p.getMessages.foreach(println)
}
