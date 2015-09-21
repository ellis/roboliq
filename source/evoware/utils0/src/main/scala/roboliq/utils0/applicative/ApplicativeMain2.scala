package roboliq.utils0.applicative

import scala.language.existentials
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

import RqPimper._


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
case class ComputationResult_Command(cmd: JsObject, fn: RqResult[List[ComputationResult]]) extends ComputationResult
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

	protected case class RequireItem[A](id: String, map: JsValue => RqResult[A])
	
	protected def paramString(a: Symbol): RequireItem[String] =
		RequireItem[String]("$"+a.name, (jsval: JsValue) => jsval match {
			case JsString(text) => RqSuccess(text)
			case _ => RqSuccess(jsval.toString)
		})
	
	protected def handlerRequire[A](a: RequireItem[A])(fn: (A) => RqResult[List[ComputationResult]]): RqResult[List[ComputationResult]] = {
		RqSuccess(
			List(
				ComputationResult_Computation(List(a.id), (j_l) => j_l match {
					case List(ja) =>
						val oa_? = a.map(ja)
						for {
							oa <- oa_?
							res <- fn(oa)
						} yield res
					case _ => RqError("invalid parameters")
				})
			)
		)
	}
	
	protected def handlerReturn(a: Token): RqResult[List[ComputationResult]] = {
		RqSuccess(List(
				ComputationResult_Token(a)
		))
	}
}
trait Token

case class Token_Comment(s: String) extends Token

class Node(val parent: Node, val index: Int) {
	lazy val id_r = getId_r
	lazy val id = id_r.reverse
	
	private def getId: List[Int] = {
		getId_r.reverse
	}
	
	private def getId_r: List[Int] = {
		if (parent == null)
			Nil
		else
			index :: parent.getId
	}
}

class Node_Result(
	parent: Node,
	index: Int,
	val result: ComputationResult
) extends Node(parent, index)

class Node_Computation(
	parent: Node,
	index: Int,
	val result: ComputationResult_Computation,
	val idCmd: List[Int]
) extends Node(parent, index)

class Node_Token(
	parent: Node,
	index: Int,
	val token: Token
) extends Node(parent, index)

/*
class Node[A <: ComputationResult](parent: Node[_], index: Int, result: A) {
	def getId: List[Int] = {
		getId_r.reverse
	}
	
	def getId_r: List[Int] = {
		index :: (if (parent != null) parent.getId else Nil)
	}
}
*/

//private class Message(id: List[Int], level: Int, message: String)

class ProcessorData {
	// key is a Computation.id_r
	val root = new Node_Computation(null, 0, ComputationResult_Computation(Nil, (_: List[JsValue]) => RqError("hmm")), Nil)
	val message_m = new HashMap[Node, List[String]]
	val children_m = new HashMap[Node, List[Node]]
	val status_m = new HashMap[Node_Computation, Int]
	val dep_m: MultiMap[String, Node_Computation] = new HashMap[String, mutable.Set[Node_Computation]] with MultiMap[String, Node_Computation]
	//val node_l = new mutable.TreeSet[Node]()(NodeOrdering)
	val entity_m = new HashMap[String, JsValue]
	val entityFind_l = mutable.Set[String]()
	val entityMissing_l = mutable.Set[String]()
	val cmdToJs_m = new HashMap[List[Int], JsObject]
	
	/*
	val cmdToJs_m = new HashMap[List[Int], JsObject]
	val cn_l = new ArrayBuffer[Computation]
	val cn_m = new HashMap[List[Int], Computation]
	val cnQueue = new scala.collection.mutable.LinkedHashSet[Computation]
	val cnWaiting = new scala.collection.mutable.LinkedHashSet[Computation]()
	val dependency_m = new HashMap[String, List[Computation]]
	val token_l = new ArrayBuffer[Token]
	// Number of commands 
	val cnToCommands_m = new HashMap[List[Int], Integer]
	*/

	def setComputationResult(node: Node_Computation, result: RqResult[List[ComputationResult]]) {
		val child_l: List[Node] = result match {
			case RqSuccess(l, warning_l) =>
				setMessages(node, warning_l)
				l.zipWithIndex.map { pair => 
					val (r, index0) = pair
					val index = index0 + 1
					r match {
						case ComputationResult_Command(cmd, fn) =>
							val idCmd = node.id ++ List(index)
							val idCmd_s = getIdString(idCmd)
							val idEntity = s"cmd[${idCmd_s}]"
							cmdToJs_m(idCmd) = cmd
							setEntity(idEntity, cmd)

							val result = ComputationResult_Computation(Nil, (_: List[JsValue]) => fn)
							new Node_Computation(node, index, result, idCmd)
						case result: ComputationResult_Computation =>
							val entity2_l = result.entity_l.map(s => {
								if (s.startsWith("$")) {
									val idCmd_s = getIdString(node.idCmd)
									s"cmd[${idCmd_s}].${s.tail}"
								}
								else s
							})
							val result2 = result.copy(entity_l = entity2_l)
							new Node_Computation(node, index, result2, node.idCmd)
						case ComputationResult_Token(token) =>
							new Node_Token(node, index + 1, token)
						case _ =>
							new Node_Result(node, index, r)
					}
				}
			case RqError(error_l, warning_l) =>
				setMessages(node, error_l ++ warning_l)
				Nil
		}
		setChildren(node, child_l)
		status_m(node) = 2
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
		
		// Handle addition of the child nodes to the Processor
		for (child <- child_l) {
			child match {
				case n: Node_Computation =>
					addDependencies(n)
					updateComputationStatus(n)
				//case ComputationResult_EntityRequest(idEntity) =>
				//	setEntity(idEntity, JsString("my "+idEntity))
				case _ =>
			}
		}
		//node_l ++= child_l
	}
	
	def setEntity(id: String, jsval: JsValue) {
		entity_m(id) = jsval
		entityFind_l -= id
		entityMissing_l -= id
		// Queue the computations for which all inputs are available 
		dep_m.get(id).map(_.foreach(updateComputationStatus))
	}

	// Add node to dependency set for each of its inputs
	private def addDependencies(node: Node_Computation) {
		node.result.entity_l.foreach(s => {
			dep_m.addBinding(s, node)
			if (!entity_m.contains(s) && !entityMissing_l.contains(s))
				entityFind_l += s
		})
	}
	
	private def updateComputationStatus(node: Node_Computation) {
		status_m(node) = if (node.result.entity_l.forall(entity_m.contains)) 1 else 0
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
	private val Rx1 = """^([a-zA-Z]+)\[([0-9.]+)\]$""".r
	private val Rx2 = """^([a-zA-Z]+)\[([0-9.]+)\]\.(.+)$""".r
	
	// REFACTOR: turn entity lookups into computations, somehow
	def run() {
		def step() {
			val id_l = entityFind_l.toList
			for (id <- id_l) {
				entityFind_l -= id
				(id match {
					case Rx2(t, k, f) => findEntity(t, k, f.split('.').toList)
					case Rx1(t, k) => findEntity(t, k, Nil)
					case _ => RqError(s"don't know how to find entity: `$id`")
				}) match {
					case e: RqError[_] =>
						entityMissing_l += id
						System.err.println(e)
					case RqSuccess(jsval, w) =>
						w.foreach(System.err.println)
						setEntity(id, jsval)
				}
			}
			val l = makePendingComputationList
			if (!l.isEmpty) {
				run(l)
				step()
			}
		}
		step()
		makeMessagesForMissingInputs()
	}
	
	private def findEntity(table: String, key: String, field_l: List[String]): RqResult[JsValue] = {
		val id = (s"$table[$key]" :: field_l).mkString(".")
		entity_m.get(id) match {
			case Some(jsval) => RqSuccess(jsval)
			case None =>
				if (field_l.isEmpty)
					RqError(s"entity not found: `$id`")
				else {
					for {
						jsval <- findEntity(table, key, field_l.init)
						jsobj <- if (jsval.isInstanceOf[JsObject]) RqSuccess(jsval.asJsObject) else RqError(s"bad field name: `$id`")
						fieldVal <- jsobj.fields.get(field_l.last).asRq(s"field not found: `$id`")
					} yield fieldVal
				}
		}
	}
	
	private def run(node_l: List[Node_Computation]) {
		println("run")
		if (!node_l.isEmpty) {
			node_l.foreach(handleComputation)
			run(makePendingComputationList)
		}
	}
	
	private def makePendingComputationList: List[Node_Computation] = {
		status_m.filter(_._2 == 1).keys.toList.sorted(NodeOrdering)
	}
	
	private def handleComputation(node: Node_Computation) {
		node.result match {
			case ComputationResult_Computation(entity_l, fn) =>
				val input_l: List[JsValue] = entity_l.map(entity_m)
				setComputationResult(node, fn(input_l))
			case _ =>
		}
	}
	
	private def makeMessagesForMissingInputs() {
		for ((node, status) <- status_m if status == 0) {
			node.result match {
				case ComputationResult_Computation(entity_l, _) =>
					val message_l = entity_l.filterNot(entity_m.contains).map(entity => s"ERROR: missing command parameter `$entity`")
					setMessages(node, message_l) 
				case _ =>
			}
		}
	}
	
	def getNodeList(): List[Node] = {
		def step(node: Node): List[Node] = {
			node :: children_m.getOrElse(node, Nil).flatMap(step)
		}
		step(root).tail
	}
	
	def getComputationList(): List[Node_Computation] = {
		getNodeList.collect { case n: Node_Computation => n }
	}
	
	def getTokenList(): List[Node_Token] = {
		getNodeList.collect { case n: Node_Token => n }
	}
	
	def getMessages(): List[String] = {
		message_m.toList.sortBy(_._1).flatMap { pair =>
			val id = getIdString(pair._1)
			pair._2.map(s => s"$id: $s")
		}
	}
	
	def getIdString(node: Node): String = getIdString(node.id)
	def getIdString(id: List[Int]): String = id.mkString(".")

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
		def compare(a: List[Int], b: List[Int]): Int = {
			(a, b) match {
				case (Nil, Nil) => 0
				case (Nil, _) => -1
				case (_, Nil) => 1
				case (a1 :: arest, b1 :: brest) =>
					if (a1 != b1) a1 - b1
					else compare(arest, brest)
			}
		}
	}
	
	implicit val NodeOrdering = new Ordering[Node] {
		def compare(a: Node, b: Node): Int = {
			ListIntOrdering.compare(a.id, b.id)
		}
	}
}

class PrintCommandHandler extends CommandHandler {
	val cmd_l = List[String]("print") 
	
	def getResult =
		handlerRequire (paramString('text)) { (text) =>
			handlerReturn(Token_Comment(text))
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
	val cmd1 = JsonParser("""{ "cmd": "print", "text": "Hello, World!" }""").asJsObject
	
	val p = new ProcessorData
	
	p.setEntity("a", JsString("1"))
	p.setEntity("b", JsString("2"))
	p.setEntity("c", JsString("3"))
	//p.setEntity("cmd[1].text", JsString("Hello, World"))
	//p.addComputation(cn1.entity_l, cn1.fn, Nil)
	//p.addComputation(cn2.entity_l, cn2.fn, Nil)
	val h1 = new PrintCommandHandler
	//p.addCommand(cmd1, h1)
	p.setComputationResult(p.root, RqSuccess(List(ComputationResult_Command(cmd1, h1.getResult))))
	
	p.run()

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
	p.getComputationList.map({ node =>
		node.result match {
			case ComputationResult_Computation(entity_l, _) =>
				p.getIdString(node)+": "+entity_l.mkString(",")
			case _ =>
		}
	}).foreach(println)

	println()
	println("Entities:")
	p.entity_m.toList.sortBy(_._1).foreach(pair => println(pair._1+" "+pair._2))
	
	println()
	println("Tokens:")
	p.getTokenList.map(_.token).foreach(println)
	
	println()
	println("Messages:")
	p.getMessages.foreach(println)
}
