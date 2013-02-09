package roboliq.processor2

import scalaz._
import spray.json._
import roboliq.core._
//import RqPimper._


trait CommandHandler {
	val cmd_l: List[String]
	def getResult: RqResult[List[ComputationItem]]

	protected case class RequireItem[A: Manifest](id: String) {
		val clazz = implicitly[Manifest[A]].runtimeClass
	}
	
	protected def handlerRequire[A: Manifest](a: RequireItem[A])(fn: (A) => RqResult[List[ComputationItem]]): RqResult[List[ComputationItem]] = {
		RqSuccess(
			List(
				ComputationItem_Computation(List(IdClass(a.id, a.clazz)), (j_l) => j_l match {
					case List(oa: A) => fn(oa)
					case _ => RqError("[CommandHandler] invalid parameters: "+j_l+" "+j_l.head.getClass())
				})
			)
		)
	}
	
	protected def handlerReturn(a: Token): RqResult[List[ComputationItem]] = {
		RqSuccess(List(
				ComputationItem_Token(a)
		))
	}
	
	protected def as[A: Manifest](id: String): RequireItem[A] = RequireItem[A](id)
	protected def as[A: Manifest](symbol: Symbol): RequireItem[A] = as[A]("$"+symbol.name)
	/*
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
	}*/
}
