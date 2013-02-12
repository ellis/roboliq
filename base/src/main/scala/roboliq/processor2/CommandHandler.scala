package roboliq.processor2

import language.implicitConversions

import scala.reflect.runtime.{universe => ru}

import scalaz._
import Scalaz._
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
			case _ => RqError("[InputList] invalid parameter types: "+l+", "+l.map(_.getClass())+", "+implicitly[Manifest[A]].runtimeClass+", "+implicitly[Manifest[B]].runtimeClass)
		}
	}

	/*
	def checkN[FN, RET](l: List[Object], fn: FN): ComputationResult = {
		// fn.getClass.getMethods
		// val jm = fn.getClass.getMethod("apply", classOf[String], classOf[String])
		// jm.invoke(fn, "b", "cd")
		// jm.getParameterTypes
		
		/*import scala.reflect.runtime.{universe => ru}
		def getTypeTag[T: ru.TypeTag](obj: T) = ru.typeTag[T]
		val thetype = getTypeTag(f)
		val t = thetype.tpe
		t.asInstanceOf[ru.TypeRefApi].args*/
		
		def getInputTypes[FN: ru.TypeTag](fn: FN) = ru.typeTag[FN].tpe.asInstanceOf[ru.TypeRefApi].args.init
		
		val type_l = getInputTypes(fn)
		
		
		
		val l1 = pair_l.zipWithIndex.map(pair => {
			val ((clazz, o), i) = pair
			if (clazz.isInstance(o)) RqSuccess((clazz, o))
			else RqError(s"wrong class for parameter ${i+1}.  Expected `${clazz}`.  Found `${o.getClass}`.")
		})
		RqResult.toResultOfList(l1) match {
			case RqError(e, w) => RqError(e, w)
			case RqSuccess(l) =>
		}
		
		l match {
			case List(a: A, b: B) => RqSuccess((a, b))
			case _ => RqError("[InputList] invalid parameter types: "+l+", "+l.map(_.getClass())+", "+implicitly[Manifest[A]].runtimeClass+", "+implicitly[Manifest[B]].runtimeClass)
		}
	}
	*/
	
	def check3[A: Manifest, B: Manifest, C: Manifest](l: List[Object]): RqResult[(A, B, C)] = {
		l match {
			case List(a: A, b: B, c: C) => RqSuccess((a, b, c))
			case _ => RqError("[InputList] invalid parameter types: "+l)
		}
	}
}

case class RequireItem[A: ru.TypeTag](tkp: TKP) {
	private val clazz0 = ru.typeTag[A].tpe
	private val opt = (clazz0.typeSymbol.name.decoded == "Option")
	private val clazz = if (opt) clazz0.asInstanceOf[ru.TypeRefApi].args.head else clazz0
	val toKeyClass = KeyClassOpt(KeyClass(tkp, clazz), opt)
}

/*
case class RequireParam[A: Manifest](tkp: TKP) extends RequireItem[A](tkp)
case class RequireParamOpt[A: Manifest](tkp: TKP) extends RequireItem[A](tkp)
case class RequireState[A: Manifest](tkp: TKP) extends RequireItem[A](tkp)
case class RequireEntity[A: Manifest](tkp: TKP) extends RequireItem[A](tkp)
case class RequireRef[A: Manifest](tkp: TKP) extends RequireItem[A](tkp)
*/

trait CommandHandler {
	import InputListToTuple._
	
	val cmd_l: List[String]
	def getResult: ComputationResult

	private implicit def toIdClass(ri: RequireItem[_]): KeyClassOpt = ri.toKeyClass
	
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
		/*def getInputTypes[FN: ru.TypeTag](fn: FN) = ru.typeTag[FN].tpe.asInstanceOf[ru.TypeRefApi].args.init
		val type_l = getInputTypes(fn)
		
		println()
		println("handlerRequire2: "+List(a, b))
		println("type_l: "+type_l)
		println()*/
		
		val input_l = List[KeyClassOpt](a, b)
		RqSuccess(
			List(
				ComputationItem_Computation(input_l, (arg_l) => {
					println("arg_l: "+arg_l.map(_.getClass))
					val method = fn.getClass.getMethods.toList.find(_.getName == "apply").get
					println("!!!!!!!!!!!")
					val ret = method.invoke(fn, arg_l : _*)
					println("BBBBBBBBBBB")
					ret.asInstanceOf[ComputationResult]
				})
			)
		)
	}
	
	protected def handlerReturn(a: Token): ComputationResult = {
		RqSuccess(List(
				ComputationItem_Token(a)
		))
	}
	
	protected def as[A: ru.TypeTag](tkp: TKP): RequireItem[A] = RequireItem[A](tkp)
	protected def as[A: ru.TypeTag](symbol: Symbol): RequireItem[A] = as[A](TKP("cmd", "$", List(symbol.name)))

	//protected def asOpt[A: ru.TypeTag](tkp: TKP): RequireItem[Option[A]] = RequireItem[Option[A]](tkp)
	//protected def asOpt[A: ru.TypeTag](symbol: Symbol): RequireItem[Option[A]] = asOpt[A](TKP("cmd", "$", List(symbol.name)))
	
	protected def lookupPlateModel[PlateModel: ru.TypeTag](id: String): RequireItem[PlateModel] = RequireItem[PlateModel](TKP("plateModel", id, Nil))
	protected def lookupPlate[Plate: ru.TypeTag](id: String): RequireItem[Plate] = RequireItem[Plate](TKP("plate", id, Nil))
}
