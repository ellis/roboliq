package roboliq.utils0.applicative

import scala.language.implicitConversions
import scala.language.postfixOps

import scala.collection.mutable.ArrayBuffer

import scalaz._
//import Scalaz._

import spray.json.JsObject
import spray.json.JsString
import spray.json.JsValue
import spray.json.JsNull
import spray.json.JsonParser


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

case class EntityNode(
	entity: Entity,
	var state: Int,
	var child_l: List[EntityNode]
)

// States:
// 0: stable, nothing needs to be done
// 1: unresolved
// 2: dirty

case class Data(
	table: String,
	key: String,
	field: String
)
class Obj
class Computation
class Command
class CommandHandler
class Token


object ApplicativeMain2 extends App {
	val entityTree = EntityNode(
		Entity("root", Nil, (_) => RqSuccess(JsNull)),
		0,
		List(
			EntityNode(Entity("a", Nil, (_) => RqSuccess(JsString("1"))), 0, Nil),
			EntityNode(Entity("b", Nil, (_) => RqSuccess(JsString("2"))), 0, Nil),
			EntityNode(Entity("c", Nil, (_) => RqSuccess(JsString("3"))), 0, Nil),
			EntityNode(Entity("d", List("a", "b"), (l) => RqSuccess(JsString(l.mkString))), 0, Nil),
			EntityNode(Entity("e", List("c", "d"), (l) => RqSuccess(JsString(l.mkString))), 0, Nil)
		)
	)
	
	// Put all entitie nodes into a linear list
	val entityNode_l = new ArrayBuffer[EntityNode]
	def desc(root: EntityNode) {
		entityNode_l += root
		root.child_l.foreach(desc)
	}
	desc(entityTree)
	
	def updateMap(map0: Map[String, JsValue]): Map[String, JsValue] = {
		entityNode_l.map( node => {
			// If map0 contains all inputs for this entity:
			if (map0.contains(node.entity.id))
				None
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
}
