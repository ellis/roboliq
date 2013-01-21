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
	id: List[Int],
	entity_l: List[String],
	fn: (List[JsValue]) => RqResult[List[ComputationResult]]
)

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
class CommandHandler
trait Token

case class Token_Comment(s: String) extends Token

class ProcessorData {
	//val cnRoot = ComputationNode(Computation(List(), Nil, (_) => RqSuccess(Nil)), 0, Nil)
	val cn_l = new ArrayBuffer[Computation]
	val cn_m = new HashMap[List[Int], Computation]
	val cnQueue = new scala.collection.mutable.Queue[Computation]
	val entity_m = new HashMap[String, JsValue]
	val dependency_m = new HashMap[String, List[Computation]]
	val token_l = new ArrayBuffer[Token]

	def getComputationChildren(id: List[Int]): List[Computation] = {
		val n = id.length + 1
		getComputationDecendents(id).filter(_.id.length == n)
	}
	
	def getComputationDecendents(id: List[Int]): List[Computation] = {
		cn_l.filter(_.id.startsWith(id)).toList
	}
	
	def getNextChildId(idParent: List[Int]): List[Int] = {
		val l = getComputationChildren(idParent)
		val i = l.foldLeft(1){(acc, cn) => math.max(acc, cn.id.last)}
		idParent ++ List(i)
	}
	
	def addComputation(
		entity_l: List[String],
		fn: (List[JsValue]) => RqResult[List[ComputationResult]],
		idParent: List[Int]
	) {
		val id = getNextChildId(idParent)
		val cn = Computation(id, entity_l, fn)
		cn_l += cn
		cn_m(id) = cn
		// Add dependencies
		for (id <- cn.entity_l) {
			val l: List[Computation] = dependency_m.get(id).getOrElse(Nil)
			dependency_m(id) = l ++ List(cn)
		}
		addToQueue(cn)
	}
	
	private def addToQueue(cn: Computation) {
		if (cn.entity_l.forall(entity_m.contains)) {
			cnQueue += cn
		}
	}
	
	def addEntity(id: String, jsval: JsValue) {
		entity_m(id) = jsval
		// Queue computations which are now complete
		dependency_m.get(id).map(_.foreach(addToQueue))
	}
	
	def addToken(token: Token) {
		token_l += token
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
				ComputationResult_Token(Token_Comment(l.mkString))
			)))
	
	val p = new ProcessorData
	
	p.addEntity("a", JsString("1"))
	p.addEntity("b", JsString("2"))
	p.addEntity("c", JsString("3"))
	println(p.cnQueue)
	p.addComputation(cn1.entity_l, cn1.fn, Nil)
	println(p.cnQueue)
	p.addComputation(cn2.entity_l, cn2.fn, Nil)
	println(p.cnQueue)
	
	while (!p.cnQueue.isEmpty) {
		val cn = p.cnQueue.dequeue
		val entity_l: List[JsValue] = cn.entity_l.map(p.entity_m)
		cn.fn(entity_l) match {
			case RqSuccess(l, warning_l) =>
				warning_l.foreach(println)
				l.foreach(_ match {
					case ComputationResult_Token(t) => p.addToken(t)
					case ComputationResult_Computation(l, fn) => p.addComputation(l, fn, cn.id)
					case ComputationResult_EntityRequest(id) => p.addEntity(id, JsString("my "+id))
					case _ =>
				})
			case RqError(error_l, warning_l) =>
				warning_l.foreach(println)
				error_l.foreach(println)
				None
		}
		println(p.cnQueue)
	}

	def compComputation(cn1: Computation, cn2: Computation): Boolean =
		compId(cn1.id, cn2.id)
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

	println("Computations:")
	println()
	p.cn_l.toList.sortWith(compComputation).map(cn => cn.id.mkString(".")+" "+cn.entity_l).foreach(println)

	println()
	println("Entities:")
	p.entity_m.toList.sortBy(_._1).foreach(pair => println(pair._1+" "+pair._2))
	println()
	println("Tokens:")
	p.token_l.toList.foreach(println)
}
