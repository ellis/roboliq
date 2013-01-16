package roboliq.processor

import roboliq.core._


abstract class Command {
	def x: String
}

abstract class CommandHandler {
	def find[A, B, C, D](
		a: LookupVariable[A],
		b: LookupVariable[B],
		c: LookupVariable[C],
		d: LookupVariable[D]
	)(fn: (A, B, C, D) => Step): Step = {
		null
	}
	
	def findPlate(idRef: Symbol): LookupVariable[Plate] = {
		import MyJsonProtocol._
		def fn(jsobj: JsObject): RqResult[Plate] = {
			try RqSuccess(jsobj.convertTo[Plate])
			catch {
				case ex : Throwable => RqError(ex.getMessage())
			}
		}
		new LookupObj[Plate]("plate", id, fn _)
	}
}