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

class EntityBase

case class Entity(
	id: String,
	input_l: List[String],
	fn: (List[JsValue]) => RqResult[JsValue]
	//castObject: (Object) => RqResult[Object],
	//getField: (String) => Entity
)

// States:
// 0: not evaluated yet
// 1: stable, nothing needs to be done
// 2: dirty

sealed trait ComputationResult
case class ComputationResult_Event(event: Event) extends ComputationResult
case class ComputationResult_EntityRequest(id: String) extends ComputationResult
case class ComputationResult_Computation(computation: Computation) extends ComputationResult
case class ComputationResult_Command(command: Command) extends ComputationResult
case class ComputationResult_Token(token: Token) extends ComputationResult

case class Computation(
	id: List[Int],
	entity_l: List[String],
	fn: (List[JsValue]) => RqResult[List[ComputationResult]]
)

case class ComputationNode(
	computation: Computation,
	var state: Int,
	var child_l: List[ComputationNode]
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
	val cn_l = new ArrayBuffer[ComputationNode]
	val cn_m = new HashMap[List[Int], ComputationNode]
	val cnQueue = new scala.collection.mutable.Queue[ComputationNode]
	val entity_m = new HashMap[String, JsValue]
	val dependency_m = new HashMap[String, List[ComputationNode]]
	val token_l = new ArrayBuffer[Token]

	def addComputation(computation: Computation /*, parent: ComputationNode*/) {
		val cn = ComputationNode(computation, 0, Nil)
		cn_l += cn
		cn_m(computation.id) = cn
		// Add dependencies
		for (id <- computation.entity_l) {
			val l: List[ComputationNode] = dependency_m.get(id).getOrElse(Nil)
			dependency_m(id) = l ++ List(cn)
		}
		addToQueue(cn)
	}
	
	private def addToQueue(cn: ComputationNode) {
		if (cn.computation.entity_l.forall(entity_m.contains)) {
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
	val entity_l = List[Entity](
		Entity("a", Nil, (_) => RqSuccess(JsString("1"))),
		Entity("b", Nil, (_) => RqSuccess(JsString("2"))),
		Entity("c", Nil, (_) => RqSuccess(JsString("3")))
	)
	val entity_m = entity_l.map(entity => entity.id -> entity).toMap
	val cn1 = Computation(List(1), List("a", "b"), (l) => RqSuccess(
			List(
				ComputationResult_Token(Token_Comment(l.mkString)),
				ComputationResult_EntityRequest("d")
			)))
	val cn2 = Computation(List(2), List("c", "d"), (l) => RqSuccess(
			List(
				ComputationResult_Token(Token_Comment(l.mkString))
			)))
	
	val p = new ProcessorData
	
	p.addEntity("a", JsString("1"))
	p.addEntity("b", JsString("2"))
	p.addEntity("c", JsString("3"))
	println(p.cnQueue)
	p.addComputation(cn1)
	println(p.cnQueue)
	p.addComputation(cn2)
	println(p.cnQueue)
	
	while (!p.cnQueue.isEmpty) {
		val cn = p.cnQueue.dequeue
		val entity_l: List[JsValue] = cn.computation.entity_l.map(p.entity_m)
		cn.computation.fn(entity_l) match {
			case RqSuccess(l, warning_l) =>
				warning_l.foreach(println)
				l.foreach(_ match {
					case ComputationResult_Token(t) => p.addToken(t)
					case ComputationResult_Computation(c) => p.addComputation(c)
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

	println("Entities:")
	p.entity_m.foreach(println)
	println("Tokens:")
	println(p.token_l)
	
	
	/*
	// Put all computation nodes into a linear list
	val computationNode_l = new ArrayBuffer[ComputationNode]
	def desc(root: ComputationNode) {
		computationNode_l += root
		root.child_l.foreach(desc)
	}
	desc(computationTree)
	
	def processComputation(computation: Computation, entity_m: Map[String, JsValue]) {
		// If entity_m contains all inputs for this entity:
		if (computation.entity_l.forall(entity_m.contains)) {
			val entity_l: List[JsValue] = computation.entity_l.map(entity_m)
			computation.fn(entity_l) match {
				case RqSuccess(l, warning_l) =>
					warning_l.foreach(println)
					Some(computation.id -> jsval)
				case RqError(error_l, warning_l) =>
					warning_l.foreach(println)
					error_l.foreach(println)
					None
			}
		}
	}
	
	def process(node_l: List[ComputationNode], entity_m: Map[String, JsValue]) {
		for (node <- node_l if node.state == 0) {
			processComputation(node.computation) match {
				case None =>
				case Some(RqSuccess(l, warning_l)) =>
				case Some(RqError(error_l, warning_l)) =>
			}
		}
	}
	
	def updateMap(map0: Map[String, JsValue]): Map[String, JsValue] = {
		entityNode_l.map( node => {
			if (map0.contains(node.entity.id))
				None
			// If map0 contains all inputs for this entity:
			else if (node.entity.input_l.forall(map0.contains)) {
				val input_l: List[JsValue] = node.entity.input_l.map(map0)
				node.entity.fn(input_l) match {
					case RqSuccess(jsval, warning_l) =>
						warning_l.foreach(println)
						Some(node.entity.id -> jsval)
					case RqError(error_l, warning_l) =>
						warning_l.foreach(println)
						error_l.foreach(println)
						None
				}
			}
			else
				None
		}).flatten.toMap
	}
	
	val map1 = updateMap(Map())
	println(map1)
	val map2 = updateMap(map1)
	println(map2)
	val map3 = updateMap(map1 ++ map2)
	println(map3)
	
	//println(entityTree)
	*/
}
