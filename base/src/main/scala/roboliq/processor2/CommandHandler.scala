package roboliq.processor2

import language.implicitConversions

import scalaz._
import spray.json._
import roboliq.core._
//import RqPimper._

object InputListToTuple {
	def check1[A: Manifest](l: List[Object]): RqResult[(A)] = {
		l match {
			case List(a: A) => RqSuccess(a)
			case _ => RqError("[InputList] invalid parameter types: "+l)
		}
	}

	def check2[A: Manifest, B: Manifest](l: List[Object]): RqResult[(A, B)] = {
		l match {
			case List(a: A, b: B) => RqSuccess((a, b))
			case _ => RqError("[InputList] invalid parameter types: "+l)
		}
	}

	def check3[A: Manifest, B: Manifest, C: Manifest](l: List[Object]): RqResult[(A, B, C)] = {
		l match {
			case List(a: A, b: B, c: C) => RqSuccess((a, b, c))
			case _ => RqError("[InputList] invalid parameter types: "+l)
		}
	}
}

trait CommandHandler {
	import InputListToTuple._
	
	val cmd_l: List[String]
	def getResult: ComputationResult

	protected case class RequireItem[A: Manifest](id: String) {
		val clazz = implicitly[Manifest[A]].runtimeClass
	}

	private implicit def toIdClass(ri: RequireItem[_]): IdClass = IdClass(ri.id, ri.clazz)
	
	protected def handlerRequire[A: Manifest](a: RequireItem[A])(fn: (A) => RqResult[List[ComputationItem]]): ComputationResult = {
		RqSuccess(
			List(
				ComputationItem_Computation(List(a),
					(j_l) => check1(j_l).flatMap { a => fn(a) }
				)
			)
		)
	}
	
	protected def handlerRequire[A: Manifest, B: Manifest](
		a: RequireItem[A],
		b: RequireItem[B]
	)(
		fn: (A, B) => RqResult[List[ComputationItem]]
	): ComputationResult = {
		RqSuccess(
			List(
				ComputationItem_Computation(List(a, b),
					(j_l) => check2(j_l).flatMap { case (a, b) => fn(a, b) }
				)
			)
		)
	}
	
	protected def handlerReturn(a: Token): ComputationResult = {
		RqSuccess(List(
				ComputationItem_Token(a)
		))
	}
	
	protected def as[A: Manifest](id: String): RequireItem[A] = RequireItem[A](id)
	protected def as[A: Manifest](symbol: Symbol): RequireItem[A] = as[A]("$"+symbol.name)
	
	protected def lookupPlateModel(id: String): RequireItem[PlateModel] = RequireItem[PlateModel](s"plateModel[$id]")
	protected def lookupPlate(id: String): RequireItem[Plate] = RequireItem[Plate](s"plate[$id]")
}
