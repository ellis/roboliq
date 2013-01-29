package roboliq.processor2

import scalaz._
import spray.json._
import roboliq.core._
//import RqPimper._


trait CommandHandler {
	val cmd_l: List[String]
	def getResult: RqResult[List[ComputationResult]]

	protected case class RequireItem[A](id: String, fn: JsValue => RqResult[List[ComputationResult]])//map: JsValue => RqResult[A])
	
	protected def handlerRequire[A](a: RequireItem[A])(fn: (A) => RqResult[List[ComputationResult]]): RqResult[List[ComputationResult]] = {
		RqSuccess(
			List(
				ComputationResult_Computation(List(a.id), (j_l) => j_l match {
					case List(oa: A) =>
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
	
	protected def getJsValue(id: String): RequireItem[JsValue] = {
		
	}
	
	protected def getJsObject(id: String): RequireItem[JsObject]
	
	protected def getString(a: Symbol): RequireItem[String] =
		RequireItem[String]("$"+a.name, (jsval: JsValue) => jsval match {
			case JsString(text) => RqSuccess(text)
			case _ => RqSuccess(jsval.toString)
		})
	
	protected def getPlateModel(id: String): RequireItem[PlateModel] = {
		handlerRequire(
			getJsObject(s"plateModel[$id]")
		) {
			(jsobj) =>
			handlerReturn((
				getString('id, jsobj) |@|
				getInteger('rows, jsobj) |@|
				getInteger('cols, jsobj) |@|
				getLiquidVolume('wellVolume, jsObj)
			) {
				new PlateModel
			})
		}
	}
		
	protected def getPlate(id: String): RequireItem[Plate] = {
		RequireItem[Plate](s"plate[$id]", (jsval: JsValue) => jsval match {
			case JsString(text) => RqSuccess(text)
			case _ => RqSuccess(jsval.toString)
		})
	}
	
	protected def getPlate(id: String): RequireItem[Plate] = {
		RequireItem[Plate](id, (jsval: JsValue) => jsval match {
			case JsString(text) => RqSuccess(text)
			case _ => RqSuccess(jsval.toString)
		})
	}
	
	protected def getPlate(param: Symbol): RequireItem[Plate] = {
		RequireItem[Plate]("$"+a.name, (jsval: JsValue) => jsval match {
			case JsString(text) => RqSuccess(text)
			case _ => RqSuccess(jsval.toString)
		})
	}
}
