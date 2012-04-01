/*package roboliq.core

import scala.collection
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet


trait ObjConfig
trait ObjState

trait ObjConfigImpl[T <: Obj] extends ObjConfig {
	val obj: T
}

trait ObjStateImpl[T <: ObjConfig] extends ObjState {
	val conf: T
}

abstract class Obj {
	type Config <: ObjConfig
	type State <: ObjState
	
	def getLabel(kb: KnowledgeBase): String
	def createConfigAndState0(): Result[Tuple2[Config, State]]
	
	def getConfig(map31: ObjMapper): Option[Config] = map31.config(this) match { case Some(o) => Some(o.asInstanceOf[Config]); case None => None }
	def getState0(map31: ObjMapper): Option[State] = map31.state0(this) match { case Some(o) => Some(o.asInstanceOf[State]); case None => None }
	
	def state(states: StateMap): State = states(this).asInstanceOf[State]
	def stateOpt(states: StateMap): Option[State] = states.map.get(this).map(_.asInstanceOf[State])
	def stateRes(states: StateMap): Result[State] = Result.get(stateOpt(states), "missing state for object \""+toString+"\"")
}

sealed class Setting[T] {
	var default_? : Option[T] = None
	var user_? : Option[T] = None
	var possible: List[T] = Nil
	
	def get = user_? match {
		case None =>
			default_?.get
		case Some(o) =>
			o
	}
	
	def get_? : Option[T] = user_? match {
		case None =>
			default_?
		case Some(o) =>
			user_?
	} 
	
	def isDefined: Boolean = { user_?.isDefined || default_?.isDefined }
	def isEmpty: Boolean = !isDefined
}
*/