package roboliq.processor

import language.implicitConversions
import scala.reflect.runtime.{universe => ru}
import scala.reflect.runtime.universe.Type
import scala.reflect.runtime.universe.TypeTag
import scala.reflect.runtime.universe.typeTag
import scalaz._
import Scalaz._
import spray.json._
import roboliq.core._, roboliq.entity._
import scala.reflect.ClassTag
//import RqPimper._

sealed trait Lookup {
	val tpe: Type
	val keyName: String
}
case class Lookup_Command(tpe: Type) extends Lookup {
	val keyName = s"cmd<$tpe>"
}
/*case class Lookup_Key(key: Key) extends Lookup {
	val keyName = s"$tpe[$id]"
}*/
case class Lookup_Entity(tpe: Type, id: String) extends Lookup {
	val keyName = s"$tpe[$id]"
}
case class Lookup_EntityOption(tpe: Type, id: String) extends Lookup {
	val keyName = s"$tpe[$id]?"
}
case class Lookup_EntityList(tpe: Type, id_l: List[String]) extends Lookup {
	val keyName = s"$tpe[${id_l.mkString(",")}]"
}
case class Lookup_EntityAll(tpe: Type) extends Lookup {
	val keyName = s"$tpe[*]"
}

sealed trait LookupWrapper {
	val tpe: Type
}

sealed abstract class LookupWrapperA[A <: Object : TypeTag] extends LookupWrapper {
	val tpe = typeTag[A].tpe
	def unwrap: Lookup
}
case class LookupWrapper_Command[A <: Cmd : TypeTag]() extends LookupWrapperA[A] {
	def unwrap = Lookup_Command(tpe)
}
case class LookupWrapper_Entity[A <: Entity : TypeTag](id: String) extends LookupWrapperA[A] {
	def unwrap = Lookup_Entity(tpe, id)
}
case class LookupWrapper_EntityOption[A <: Option[_ <: Entity] : TypeTag](id: String) extends LookupWrapperA[A] {
	def unwrap = Lookup_EntityOption(typeTag[A].tpe.asInstanceOf[ru.TypeRefApi].args(0), id)
}
case class LookupWrapper_EntityList[A <: List[_ <: Entity] : TypeTag](id_l: List[String]) extends LookupWrapperA[A] {
	def unwrap = Lookup_EntityList(typeTag[A].tpe.asInstanceOf[ru.TypeRefApi].args(0), id_l)
}
case class LookupWrapper_EntityAll[A <: List[_ <: Entity] : TypeTag]() extends LookupWrapperA[A] {
	def unwrap = Lookup_EntityAll(typeTag[A].tpe.asInstanceOf[ru.TypeRefApi].args(0))
}

case class RqFunctionLookups(fn: RqFunction, lookup_l: List[Lookup])
case class RqFunctionArgs(fn: RqFunction, arg_l: List[LookupKey])

class RqReturnBuilder(val l: List[RqResult[RqItem]])

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

trait RqFunctionHandlerMethods {
	implicit def itemToRqReturn(item: RqItem): RqReturn =
		RqSuccess(List(item))
	implicit def fnargsToItem(fnargs: RqFunctionArgs) =
		RqItem_Function(fnargs)
	implicit def fnargsToRqReturn(fnargs: RqFunctionArgs) =
		RqSuccess(List(RqItem_Function(fnargs)))
	implicit def tokenToItem(token: CmdToken) =
		ComputationItem_Token(token)
	implicit def tokenToRqReturn(token: CmdToken) =
		RqSuccess(List(ComputationItem_Token(token)))
	implicit def lookupWrapperToLookup(wrapper: LookupWrapper[_]): Lookup =
		wrapper.unwrap

	def as[A <: Entity : TypeTag](id: String): LookupWrapper[A] = LookupWrapper_Entity[A](id)
	def asOption[A <: Entity : TypeTag](id: String): LookupWrapper[Option[A]] = LookupWrapper_EntityOption[Option[A]](id)
	def asList[A <: Entity : TypeTag](id_l: List[String]): LookupWrapper[List[A]] = LookupWrapper_EntityList[List[A]](id_l)
	def all[A <: Entity : TypeTag](): LookupWrapper[List[A]] = LookupWrapper_EntityAll[List[A]]()

	def lookup()(fn: => RqReturn): RqFunctionArgs = {
		RqFunctionArgs(
			arg_l = Nil,
			fn = (_) => fn
		)
	}
	
	def lookup[A: TypeTag](
		a: LookupWrapper[A]
	)(
		fn: (A) => RqReturn
	): RqFunctionArgs = {
		val arg_l = List[Lookup](a)
		lookupRun(fn, arg_l)
	}
	
	def lookup[A: TypeTag, B: TypeTag](
		a: LookupWrapper[A],
		b: LookupWrapper[B]
	)(
		fn: (A, B) => RqReturn
	): RqFunctionArgs = {
		lookupRun(fn, List(a, b))
	}
	
	def lookup[A: TypeTag, B: TypeTag, C: TypeTag](
		a: LookupWrapper[A],
		b: LookupWrapper[B],
		c: LookupWrapper[C]
	)(
		fn: (A, B, C) => RqReturn
	): RqFunctionArgs = {
		lookupRun(fn, List(a, b, c))
	}

	def lookup[A: TypeTag, B: TypeTag, C: TypeTag, D: TypeTag](
		a: LookupWrapper[A],
		b: LookupWrapper[B],
		c: LookupWrapper[C],
		d: LookupWrapper[D]
	)(
		fn: (A, B, C, D) => RqReturn
	): RqFunctionArgs = {
		lookupRun(fn, List(a, b, c, d))
	}
	
	private def lookupRun(fn: Object, arg_l: RqArgs): RqFunctionArgs = {
		val fn2: RqFunction = (arg_l) => {
			val method = fn.getClass.getMethods.toList.find(_.getName == "apply").get
			val ret = method.invoke(fn, arg_l : _*)
			ret.asInstanceOf[RqReturn]
		}
		RqFunctionArgs(fn2, arg_l)
	}
}

// REFACTOR: Rename Handler => Builder ?
abstract class RqFunctionHandler extends RqFunctionHandlerMethods {
	protected def cmdAs[A <: Cmd : TypeTag](fn: A => RqReturn): RqFunctionArgs = {
		lookup(LookupWrapper_Command[A]()) { cmd =>
			fn(cmd)
		}
	}
}

object RqFunctionHandler extends RqFunctionHandlerMethods {
	/*
	def returnObject(obj: Object) =
		ConversionItem_Object(obj)
	*/
	def returnEvent[A <: Event[Entity] : TypeTag](event: A) =
		ComputationItem_Events(List(event))
}

abstract class RqFunctionHandler0 extends RqFunctionHandler {
	val fn: RqFunction
	val fnargs = RqFunctionArgs(fn, Nil)
}

abstract class CommandHandler[A <: Cmd : TypeTag](
	val id: String
) extends RqFunctionHandler {
	
	//implicit def RqItemToValidationOutput(a: RqItem): Validation[String, Output] =
	//	List(a).success
	implicit def RqItemToRqReturnBuilder(a: RqItem): RqReturnBuilder =
		new RqReturnBuilder(List(RqSuccess(a)))
	implicit def TokenToReturnBuilder(a: CmdToken): RqReturnBuilder =
		new RqReturnBuilder(List(RqSuccess(ComputationItem_Token(a))))
	implicit def EventToReturnBuilder(a: Event[Entity]): RqReturnBuilder =
		new RqReturnBuilder(List(RqSuccess(ComputationItem_Events(List(a)))))
	implicit def ListEventToReturnBuilder(l: Iterable[Event[Entity]]): RqReturnBuilder =
		new RqReturnBuilder(List(RqSuccess(ComputationItem_Events(l.toList))))
	implicit def CmdToReturnBuilder[A <: Cmd](a: A): RqReturnBuilder =
		new RqReturnBuilder(List(RqSuccess(ComputationItem_Command(a))))
	implicit def ListCmdToReturnBuilder[A <: Cmd](l: Iterable[A]): RqReturnBuilder = {
		new RqReturnBuilder(
			l.toList.map(cmd => RqSuccess(ComputationItem_Command(cmd)))
		)
	}
	def entityToReturnBuilder[A <: Entity : TypeTag](a: A): RqReturnBuilder = {
		new RqReturnBuilder(List(RqSuccess(ComputationItem_Entity(typeTag[A].tpe, a))))
	}
	def entitiesToReturnBuilder[A <: Entity : TypeTag](l: List[A]): RqReturnBuilder = {
		val item_? = l.map(a => RqSuccess(ComputationItem_Entity(typeTag[A].tpe, a)))
		new RqReturnBuilder(item_?)
	}
	
	val fnargs: RqFunctionArgs = {
		cmdAs[A] { cmd =>
			handleCmd(cmd)
		}
	}
	
	def handleCmd(cmd: A): RqReturn

	def output(l: RqReturnBuilder*): RqReturn = {
		val l1: List[RqResult[RqItem]] = l.toList.flatMap(_.l)
		RqResult.toResultOfList(l1)
	}
}
/*
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
*/