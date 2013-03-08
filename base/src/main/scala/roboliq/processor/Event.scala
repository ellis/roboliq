package roboliq.processor

import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.TypeTag
import roboliq.core._, roboliq.entity._


abstract class Event[+A <: Entity : TypeTag] {
	/**
	 * If the event has the state, return Right(state), otherwise return Left(id-of-state-entity). 
	 */
	def getStateOrId: Either[String, A]
	/**
	 * Unique identifier for the kind of event
	 */
	//def kind: String
}


abstract class EventHandler[St <: Entity : TypeTag, Ev <: Event[St] : TypeTag](
	val id: String
) extends RqFunctionHandler {
	def handleEvent(state0: St, event: Ev): RqResult[St]

	def fnargs(event: Ev): RqReturn = {
		event.getStateOrId match {
			case Right(state0) => fnargs(state0, event)
			case Left(id) =>
				fnRequire (lookup[St](id)) { state0 => fnargs(state0, event) }
		}
	}
	
	private def fnargs(state0: St, event: Ev): RqReturn = {
		for {
			state_# <- handleEvent(state0, event)
			json <- ConversionsDirect.toJson[St](state_#)
			table <- ConversionsDirect.findTableForType(ru.typeTag[St].tpe)
		} yield {
			List(EventItem_State(TKP(table, state_#.id, Nil), json))
		}
	}
}
