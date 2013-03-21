package roboliq.processor

import scala.reflect.ClassTag
import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.typeTag
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

abstract class EventHandler(
	val id: String
) extends RqFunctionHandler {
	def eventClass: Class[_]

	def fnargs(event: Event[_ <: Entity]): RqReturn
}

abstract class EventHandlerAB[St <: Entity : TypeTag, Ev <: Event[St] : TypeTag : ClassTag](
	id: String
) extends EventHandler(id) {
	def eventClass: Class[_] = scala.reflect.classTag[Ev].runtimeClass
	
	def handleEvent(state0: St, event: Ev): RqResult[St]

	def fnargs(event: Event[_ <: Entity]): RqReturn = {
		event match {
			case event_# : Ev =>
				event_#.getStateOrId match {
					case Right(state0) => fnargs(state0, event_#)
					case Left(id) =>
						fnRequire (lookup[St](id)) { state0 => fnargs(state0, event_#) }
				}
			case _ =>
				RqError(s"expected event class `$eventClass`, but received `${event.getClass}`.")
		}
	}
	
	private def fnargs(state0: St, event: Ev): RqReturn = {
		for {
			state_# <- handleEvent(state0, event)
		} yield {
			List(EventItem_State(typeTag[St].tpe, state_#))
		}
	}
}

/*
abstract class EventHandler[A: TypeTag] extends RqFunctionHandler {
	protected def eventAs[A <: Object : TypeTag](fn: A => RqReturn): RqFunctionArgs = {
		val arg_l = List[KeyClassOpt](RequireItem[JsValue](TKP("cmd", "$", Nil)).toKeyClass)
		val fn0: RqFunction = (l: List[Object]) => l match {
			case List(jsval: JsValue) =>
				val typ = ru.typeTag[A].tpe
				ConversionsDirect.convRequirements(jsval, typ).map(_ match {
					case Left(pathToKey_m) =>
						val pathToKey_l = pathToKey_m.toList
						val arg_l = pathToKey_l.map(_._2)
						List(RqItem_Function(RqFunctionArgs(
							arg_l = arg_l,
							fn = (input_l) => {
								val lookup_m = (pathToKey_l.map(_._1) zip input_l).toMap
								ConversionsDirect.conv(jsval, typ, lookup_m).flatMap(o => fn(o.asInstanceOf[A]))
							}
						)))
					case Right(o) =>
						List(RqItem_Function(RqFunctionArgs(
							arg_l = Nil,
							fn = (_) => {
								fn(o.asInstanceOf[A])
							}
						)))
				})
			case _ =>
				RqError("Expected JsValue")
		}
		RqFunctionArgs(fn0, arg_l)
	}
	
	def returnEvent(key: TKP, jsval: JsValue) = RqFunctionHandler.returnEvent(key, jsval)
}
*/
