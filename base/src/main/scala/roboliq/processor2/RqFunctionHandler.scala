package roboliq.processor2

import language.implicitConversions
import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.TypeTag
import scalaz._
import Scalaz._
import spray.json._
import roboliq.core._
import scala.reflect.ClassTag
//import RqPimper._

case class RqFunctionArgs(fn: RqFunction, arg_l: List[KeyClassOpt])

object InputListToTuple {
	def check1[A: ClassTag](l: List[Object]): RqResult[(A)] = {
		l match {
			case List(a: A) => RqSuccess(a)
			case _ => RqError("[InputList] invalid parameter types: "+l)
		}
	}

	def check2[A: ClassTag, B: ClassTag](l: List[Object]): RqResult[(A, B)] = {
		l match {
			case List(a: A, b: B) => RqSuccess((a, b))
			case _ => RqError("[InputList] invalid parameter types: "+l+", "+l.map(_.getClass())+", "+implicitly[ClassTag[A]]+", "+implicitly[ClassTag[B]])
		}
	}

	/*
	def checkN[FN, RET](l: List[Object], fn: FN): RqReturn = {
		// fn.getClass.getMethods
		// val jm = fn.getClass.getMethod("apply", classOf[String], classOf[String])
		// jm.invoke(fn, "b", "cd")
		// jm.getParameterTypes
		
		/*import scala.reflect.runtime.{universe => ru}
		def getTypeTag[T: TypeTag](obj: T) = TypeTag[T]
		val thetype = getTypeTag(f)
		val t = thetype.tpe
		t.asInstanceOf[ru.TypeRefApi].args*/
		
		def getInputTypes[FN: TypeTag](fn: FN) = TypeTag[FN].tpe.asInstanceOf[ru.TypeRefApi].args.init
		
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

case class RequireItem[A: TypeTag](
	tkp: TKP,
	conversion_? : Option[RqFunctionArgs] = None
) {
	private val clazz0 = ru.typeTag[A].tpe
	//private val opt = (clazz0.typeSymbol.name.decoded == "Option")
	private val clazz = clazz0//if (opt) clazz0.asInstanceOf[ru.TypeRefApi].args.head else clazz0
	val toKeyClass = KeyClassOpt(KeyClass(tkp, clazz), false, conversion_?)
}

// REFACTOR: Rename Handler => Builder ?
abstract class RqFunctionHandler {
	private implicit def toIdClass(ri: RequireItem[_]): KeyClassOpt = ri.toKeyClass
	
	protected implicit def itemToRqReturn(item: RqItem): RqReturn =
		RqSuccess(List(item))
	
	protected implicit def fnargsToItem(fnargs: RqFunctionArgs) =
		RqItem_Function(fnargs)
	
	protected implicit def fnargsToRqReturn(fnargs: RqFunctionArgs) =
		RqSuccess(List(RqItem_Function(fnargs)))
	
	protected def handlerRequire[
		A: Manifest
	](
		a: RequireItem[A]
	)(
		fn: (A) => RqReturn
	): RqReturn = {
		val arg_l = List[KeyClassOpt](a)
		handlerRequireRun(fn, arg_l)
	}
	
	/*def getInputTypes[FN: TypeTag](fn: FN) = TypeTag[FN].tpe.asInstanceOf[ru.TypeRefApi].args.init
	val type_l = getInputTypes(fn)
	
	println()
	println("handlerRequire2: "+List(a, b))
	println("type_l: "+type_l)
	println()*/
	
	protected def handlerRequire[A: TypeTag, B: TypeTag](
		a: RequireItem[A],
		b: RequireItem[B]
	)(
		fn: (A, B) => RqReturn
	): RqReturn = {
		val arg_l = List[KeyClassOpt](a, b)
		handlerRequireRun(fn, arg_l)
	}
	
	protected def handlerRequire[A: TypeTag, B: TypeTag, C: TypeTag](
		a: RequireItem[A],
		b: RequireItem[B],
		c: RequireItem[C]
	)(
		fn: (A, B, C) => RqReturn
	): RqReturn = {
		val arg_l = List[KeyClassOpt](a, b, c)
		handlerRequireRun(fn, arg_l)
	}
	
	protected def handlerRequire[
		A: TypeTag,
		B: TypeTag,
		C: TypeTag,
		D: TypeTag
	](
		a: RequireItem[A],
		b: RequireItem[B],
		c: RequireItem[C],
		d: RequireItem[D]
	)(
		fn: (A, B, C, D) => RqReturn
	): RqReturn = {
		val arg_l = List[KeyClassOpt](a, b, c, d)
		handlerRequireRun(fn, arg_l)
	}
	
	private def handlerRequireRun(fn: Object, arg_l: RqArgs): RqReturn = {
		val fn2: RqFunction = (arg_l) => {
			val method = fn.getClass.getMethods.toList.find(_.getName == "apply").get
			val ret = method.invoke(fn, arg_l : _*)
			ret.asInstanceOf[RqReturn]
		}
		RqSuccess(
			List(
				RqItem_Function(RqFunctionArgs(fn2, arg_l))
			)
		)
	}
	
	protected def fnRequire[A: TypeTag](
		a: RequireItem[A]
	)(
		fn: (A) => RqReturn
	): RqFunctionArgs = {
		val arg_l = List[KeyClassOpt](a)
		fnRequireRun(fn, arg_l)
	}
	
	protected def fnRequire[A: TypeTag, B: TypeTag](
		a: RequireItem[A],
		b: RequireItem[B]
	)(
		fn: (A, B) => RqReturn
	): RqFunctionArgs = {
		val arg_l = List[KeyClassOpt](a, b)
		fnRequireRun(fn, arg_l)
	}
	
	protected def fnRequire[A: TypeTag, B: TypeTag, C: TypeTag](
		a: RequireItem[A],
		b: RequireItem[B],
		c: RequireItem[C]
	)(
		fn: (A, B, C) => RqReturn
	): RqFunctionArgs = {
		val arg_l = List[KeyClassOpt](a, b, c)
		fnRequireRun(fn, arg_l)
	}

	protected def fnRequire[A: TypeTag, B: TypeTag, C: TypeTag, D: TypeTag](
		a: RequireItem[A],
		b: RequireItem[B],
		c: RequireItem[C],
		d: RequireItem[D]
	)(
		fn: (A, B, C, D) => RqReturn
	): RqFunctionArgs = {
		val arg_l = List[KeyClassOpt](a, b, c, d)
		fnRequireRun(fn, arg_l)
	}
	
	protected def fnRequireList[A](
		l: List[RequireItem[A]]
	)(
		fn: List[A] => RqReturn
	): RqFunctionArgs = {
		val fn2: RqFunction = (arg_l) => {
			val method = fn.getClass.getMethods.toList.find(_.getName == "apply").get
			val ret = method.invoke(fn, arg_l)
			ret.asInstanceOf[RqReturn]
		}
		val arg_l: List[KeyClassOpt] = l.map(_.toKeyClass)
		RqFunctionArgs(fn2, arg_l)
	}

	private def fnRequireRun(fn: Object, arg_l: RqArgs): RqFunctionArgs = {
		val fn2: RqFunction = (arg_l) => {
			val method = fn.getClass.getMethods.toList.find(_.getName == "apply").get
			val ret = method.invoke(fn, arg_l : _*)
			ret.asInstanceOf[RqReturn]
		}
		RqFunctionArgs(fn2, arg_l)
	}
	
	protected def handlerRequireN[A](
		l: List[RequireItem[A]]
	)(
		fn: List[A] => RqReturn
	): RqReturn = {
		val fn2: RqFunction = (arg_l) => {
			val method = fn.getClass.getMethods.toList.find(_.getName == "apply").get
			val ret = method.invoke(fn, arg_l)
			ret.asInstanceOf[RqReturn]
		}
		val arg_l: List[KeyClassOpt] = l.map(_.toKeyClass)
		RqSuccess(
			List(
				RqItem_Function(RqFunctionArgs(fn2, arg_l))
			)
		)
	}
	
	protected def handlerReturn(a: CmdToken): RqReturn = {
		RqSuccess(List(
				ComputationItem_Token(a)
		))
	}
	
	protected def fnReturn(a: CmdToken): RqReturn = {
		ComputationItem_Token(a)
	}
	
	protected def returnObject(obj: Object) =
		ConversionItem_Object(obj)
	
	//protected def get
	protected def as[A: TypeTag](tkp: TKP): RequireItem[A] = RequireItem[A](tkp)
	protected def as[A: TypeTag](symbol: Symbol): RequireItem[A] = as[A](TKP("cmd", "$", List(symbol.name)))

	//protected def asOpt[A: TypeTag](tkp: TKP): RequireItem[Option[A]] = RequireItem[Option[A]](tkp)
	//protected def asOpt[A: TypeTag](symbol: Symbol): RequireItem[Option[A]] = asOpt[A](TKP("cmd", "$", List(symbol.name)))

	//protected def lookup[A: TypeTag](table: String, symbol: Symbol): RequireItem[A] =
	//	RequireItem[A](TKP(table, symbol.name, Nil))
	
	protected def lookup[A <: Object : TypeTag : ClassTag](
		symbol: Symbol,
		lookupById: String => RequireItem[A]
	): RequireItem[A] = {
		val fn = (l: List[Object]) => {
			InputListToTuple.check1[String](l).map(id => {
				List(RqItem_Function(RqFunctionArgs(
					arg_l = List(lookupById(id)),
					fn = (l: List[Object]) => InputListToTuple.check1[A](l).map { a =>
						List(ConversionItem_Object(a))
					}
				)))
			})
		}
		val args = List[KeyClassOpt](as[String](symbol))
		RequireItem[A](TKP("param", "#", Nil), Some(RqFunctionArgs(fn, args)))
	}
	
	protected def lookupPlateModel(id: String): RequireItem[PlateModel] = RequireItem[PlateModel](TKP("plateModel", id, Nil))
	protected def lookupPlateModel(symbol: Symbol): RequireItem[PlateModel] = 
		lookup(symbol, lookupPlateModel _)
		
	protected def lookupPlateLocation(id: String): RequireItem[PlateLocation] = RequireItem[PlateLocation](TKP("plateLocation", id, Nil))
	protected def lookupPlateLocation(symbol: Symbol): RequireItem[PlateLocation] = 
		lookup(symbol, lookupPlateLocation _)
		
	protected def lookupPlate(id: String): RequireItem[Plate] = RequireItem[Plate](TKP("plate", id, Nil))
	protected def lookupPlate(symbol: Symbol): RequireItem[Plate] =
		lookup(symbol, lookupPlate _)

	protected def lookupPlateState(id: String): RequireItem[PlateState] = RequireItem[PlateState](TKP("plateState", id, Nil))
	protected def lookupPlateState(symbol: Symbol): RequireItem[PlateState] =
		lookup(symbol, lookupPlateState _)
	
	protected def cmdAs[A <: Object : TypeTag](fn: A => RqReturn): RqFunctionArgs = {
		val arg_l = List[KeyClassOpt](RequireItem[JsValue](TKP("cmd", "$", Nil)))
		val fn0: RqFunction = (l: List[Object]) => l match {
			case List(jsval: JsValue) =>
				val typ = ru.typeTag[A].tpe
				ConversionsDirect.conv(jsval, typ).flatMap(o => fn(o.asInstanceOf[A]))
			case _ =>
				RqError("Expected JsValue")
		}
		RqFunctionArgs(fn0, arg_l)
	}
		
	implicit class SymbolWrapper(symbol: Symbol) {
		def as[A: TypeTag]: RequireItem[A] = RequireItem[A](TKP("cmd", "$", List(symbol.name)))

		def lookup[A <: Object : TypeTag]: RequireItem[A] = {
			val fnargs = fnRequire (as[String]) { (id) =>
				val t = ru.typeTag[A].tpe
				val s0 = t.typeSymbol.name.decoded
				val s = s0.take(1).toLowerCase + s0.tail
				fnRequire (RequireItem[A](TKP(s, id, Nil))) { o =>
					returnObject(o)
				}
			}
			RequireItem[A](TKP("param", "#", Nil), Some(fnargs))
		}

		def lookup_?[A: TypeTag]: RequireItem[Option[A]] = {
			val fnargs = fnRequire (as[Option[String]]) { (id_?) =>
				id_? match {
					case Some(id) =>
						val t = ru.typeTag[A].tpe
						val s0 = t.typeSymbol.name.decoded
						val s = s0.take(1).toLowerCase + s0.tail
						fnRequire (RequireItem[A](TKP(s, id, Nil))) { o =>
							returnObject(Some(o))
						}
					case None =>
						returnObject(None)
				}
			}
			RequireItem[Option[A]](TKP("param", "#", Nil), Some(fnargs))
		}
		
		def lookupList[A <: Object : TypeTag]: RequireItem[List[A]] = {
			val fnargs = fnRequire (as[List[String]]) { (id_l) =>
				val t = ru.typeTag[A].tpe
				val s0 = t.typeSymbol.name.decoded
				val s = s0.take(1).toLowerCase + s0.tail
				fnRequireList (id_l.map(id => RequireItem[A](TKP(s, id, Nil)))) { o =>
					returnObject(o)
				}
			}
			RequireItem[List[A]](TKP("param", "#", Nil), Some(fnargs))
		}
	}
}

abstract class RqFunctionHandler0 extends RqFunctionHandler {
	val fn: RqFunction
	val fnargs = RqFunctionArgs(fn, Nil)
}

abstract class CommandHandler(
	val cmd_l: String*
) extends RqFunctionHandler {
	val fnargs: RqFunctionArgs
}

abstract class ConversionHandler1 extends RqFunctionHandler {
	val fn: RqFunction = (l: List[Object]) => {
		l match {
			case List(jsval: JsValue) => getResult(jsval)
			case _ => RqError("expected JsValue")
		}
	}
	val getResult: JsValue => RqReturn
	
	def createFunctionArgs(kc: KeyClass): RqFunctionArgs =
		RqFunctionArgs(fn, List(KeyClassOpt(kc, false)))
}

object ConversionHandler1 {
	def apply(getResult0: JsValue => RqReturn): ConversionHandler1 = new ConversionHandler1 {
		val getResult = getResult0
	}
}

abstract class ConversionHandlerN extends RqFunctionHandler {
	val fnargs: RqFunctionArgs
}
