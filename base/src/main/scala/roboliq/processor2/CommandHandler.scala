package roboliq.processor2

import scalaz._
import spray.json._
import roboliq.core._
//import RqPimper._


trait CommandHandler {
	val cmd_l: List[String]
	def getResult: RqResult[List[ComputationResult]]

	protected case class RequireItem[A](id: String, map: JsValue => RqResult[A])
	
	protected def paramString(a: Symbol): RequireItem[String] =
		RequireItem[String]("$"+a.name, (jsval: JsValue) => jsval match {
			case JsString(text) => RqSuccess(text)
			case _ => RqSuccess(jsval.toString)
		})
	
	protected def handlerRequire[A](a: RequireItem[A])(fn: (A) => RqResult[List[ComputationResult]]): RqResult[List[ComputationResult]] = {
		RqSuccess(
			List(
				ComputationResult_Computation(List(a.id), (j_l) => j_l match {
					case List(ja) =>
						val oa_? = a.map(ja)
						for {
							oa <- oa_?
							res <- fn(oa)
						} yield res
					case _ => RqError("invalid parameters")
				})
			)
		)
	}
	
	protected def handlerReturn(a: Token): RqResult[List[ComputationResult]] = {
		RqSuccess(List(
				ComputationResult_Token(a)
		))
	}
}
