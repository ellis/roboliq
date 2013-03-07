/*package roboliq.core

import scala.reflect.runtime.universe.TypeTag


trait Event

abstract class EventHandler[A <: Event : TypeTag](
	val id: String
) extends RqFunctionHandler {
	def handleEvent(event: A): Object
}
*/
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
