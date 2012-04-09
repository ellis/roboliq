package roboliq.core

import scala.collection
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.reflect.BeanProperty


class Tube(
	val id: String,
	val model: TubeModel
) extends Part with Ordered[Well] {
	def state(states: StateMap): WellState = states.findWellState(id) match {
		case Success(st) => st
		case _ => assert(false); null
	}
	def stateWriter(builder: StateBuilder): WellStateWriter = new WellStateWriter(id, builder)
	
	override def compare(that: Well) = id.compare(that.id)
	override def toString = id
}

object Tube {
	def fromBean(ob: ObjBase)(bean: PlateBean): Result[Tube] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
			idModel <- Result.mustBeSet(bean.model, "model")
			model <- ob.findTubeModel(idModel)
		} yield {
			new Tube(id, model)
		}
	}
}