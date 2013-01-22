package roboliq.utils0.applicative

import scala.language.implicitConversions
import scala.language.postfixOps
import scala.collection.mutable.ArrayBuffer
import scalaz._
import spray.json.JsObject
import spray.json.JsString
import spray.json.JsValue
import spray.json.JsNull
import spray.json.JsonParser
import scala.collection.mutable.HashMap


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

//private class Message(id: List[Int], level: Int, message: String)

class ProcessorData {
	val cnToJs_m = new HashMap[List[Int], JsObject]
	val cn_l = new ArrayBuffer[Computation]
	val cn_m = new HashMap[List[Int], Computation]
	val cnQueue = new scala.collection.mutable.LinkedHashSet[Computation]
	val cnWaiting = new scala.collection.mutable.LinkedHashSet[Computation]()
	val entity_m = new HashMap[String, JsValue]
	val dependency_m = new HashMap[String, List[Computation]]
	val token_l = new ArrayBuffer[Token]
	val message_m = new HashMap[List[Int], List[String]]

	def addCommand(cmd: JsObject, handler: CommandHandler) {
		val cn = addComputation(Nil, (l: List[JsValue]) => handler.getResult, Nil)
		cnToJs_m(cn.id) = cmd
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
	
	def addComputation(
		entity_l: List[String],
		fn: (List[JsValue]) => RqResult[List[ComputationResult]],
		idParent: List[Int]
	): Computation = {
		val id_r = getNextChildId_r(idParent)
		val cn = Computation(id_r, entity_l, fn)
		cn_l += cn
		cn_m(cn.id) = cn
		// Add dependencies
		for (id <- cn.entity_l) {
			val l: List[Computation] = dependency_m.get(id).getOrElse(Nil)
			dependency_m(id) = l ++ List(cn)
		}
		addToQueueOrWaiting(cn)
		cn
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
	
	def addComputationResult(result: RqResult[List[ComputationResult]], id: List[Int]) {
		result match {
			case RqSuccess(l, warning_l) =>
				addMessages(id, warning_l)
				l.foreach(_ match {
					case ComputationResult_Token(t) => addToken(t)
					case ComputationResult_Computation(l, fn) => addComputation(l, fn, id)
					case ComputationResult_EntityRequest(idEntity) => addEntity(idEntity, JsString("my "+idEntity))
					case _ =>
				})
			case RqError(error_l, warning_l) =>
				addMessages(id, error_l ++ warning_l)
				None
		}
	}
	
	def addMessages(id: List[Int], message_l: List[String]) {
		if (message_l.isEmpty)
			message_m -= id
		else
			message_m(id) = message_l
	}
	
	def handleNextComputation(): Boolean = {
		if (cnQueue.isEmpty)
			return false
		else {
			val cn = cnQueue.head
			cnQueue -= cn
			val entity_l: List[JsValue] = cn.entity_l.map(entity_m)
			addComputationResult(cn.fn(entity_l), cn.id)
			true
		}
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
