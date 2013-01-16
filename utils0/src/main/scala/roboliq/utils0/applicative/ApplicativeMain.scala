package roboliq.utils0.applicative

import scalaz._
import Scalaz._

import spray.json.JsObject
import spray.json.JsString
import spray.json.JsValue
import spray.json.JsonParser

sealed trait RqResult[A] {
	val warning_r: List[String]
	def map[B](f: A => B): RqResult[B]
	def flatMap[B](f: A => RqResult[B]): RqResult[B]
}

sealed case class RqSuccess[A](res: A, warning_r: List[String] = Nil) extends RqResult[A] {
	def map[B](f: A => B): RqResult[B] = RqSuccess(f(res), warning_r)
	def flatMap[B](f: A => RqResult[B]): RqResult[B] = {
		f(res) match {
			case RqSuccess(res2, warning_r2) => RqSuccess(res2, warning_r2 ++ warning_r)
			case RqError(error_l2, warning_r2) => RqError(error_l2, warning_r2 ++ warning_r)
		}
	} 
}

sealed case class RqError[A](error_l: List[String], warning_r: List[String] = Nil) extends RqResult[A] {
	def map[B](f: A => B): RqResult[B] = RqError[B](error_l, warning_r)
	def flatMap[B](f: A => RqResult[B]): RqResult[B] = RqError[B](error_l, warning_r)
}

object RqError {
	def apply[A](error: String): RqError[A] = RqError(List(error))
}

sealed trait CompilerStep
case class CompilerStep_Lookup(l: LookupList) extends CompilerStep
case class CompilerStep_Done() extends CompilerStep

class RqOptionW[A](opt: Option[A]) {
	def asRq(error: String): RqResult[A] = opt match {
		case Some(x) => RqSuccess(x)
		case None => RqError(error)
	}
}

object RqPimper {
	implicit def pimpedOption[A](opt: Option[A]) = new RqOptionW(opt)
	
	implicit def resultMonad: Monad[RqResult] = new Monad[RqResult] {
		def bind[A, B](fa: RqResult[A])(f: (A) => RqResult[B]) = fa.flatMap(f)
		def point[A](a: => A) = new RqSuccess[A](a)
	}
}

import RqPimper._

class Environment(
	val obj_m: Map[String, String],
	val cmd_m: Map[String, String],
	val table_m: Map[String, Map[String, JsObject]]
) {
	def lookupObject(table: String, key: String): RqResult[JsObject] = {
		for {
			key_m <- table_m.get(table).asRq(s"table not found: `$table`")
			jsobj <- key_m.get(key).asRq(s"key not found: `$key` in table `$table`")
		} yield jsobj
	}
	
	def lookupField(table: String, key: String, field: String): RqResult[JsValue] = {
		for {
			jsobj <- lookupObject(table, key)
			jsfield <- jsobj.fields.get(field).asRq(s"field not found: `$field` of key `$key` in table `$table`")
		} yield jsfield
	}
}

sealed abstract class LookupVariable[A] {
	def lookup(env: Environment): RqResult[A]
	
	def apply(fn: (A) => RqResult[CompilerStep]): LookupList = {
		new LookupList1(this, fn)
	}
}

class LookupCmdField[A](
	field: String, fn: JsValue => RqResult[A]
) extends LookupVariable[A] {
	def lookup(env: Environment): RqResult[A] = {
		for {
			jsval <- env.lookupField("cmd", "_", field)
			res <- fn(jsval)
		} yield res
	}
} 

class LookupObj[A](
	table: String, key: String, fn: JsObject => RqResult[A]
) extends LookupVariable[A] {
	def lookup(env: Environment): RqResult[A] = {
		for {
			jsval <- env.lookupObject(table, key)
			res <- fn(jsval)
		} yield res
	}
} 

class LookupObjField[A](
	table: String, key: String, field: String, fn: JsValue => RqResult[A]
) extends LookupVariable[A] {
	def lookup(env: Environment): RqResult[A] = {
		for {
			jsval <- env.lookupField(table, key, field)
			res <- fn(jsval)
		} yield res
	}
} 

trait LookupList {
	def run(env: Environment): RqResult[CompilerStep]
}

final class LookupList1[A](
	a: LookupVariable[A],
	fn: (A) => RqResult[CompilerStep]
) extends LookupList {
	def run(env: Environment): RqResult[CompilerStep] = {
		for {
			a1 <- a.lookup(env)
			res <- fn(a1)
		} yield res
	}
}

final class LookupList2[A, B](
	a: LookupVariable[A],
	b: LookupVariable[B],
	fn: (A, B) => RqResult[CompilerStep]
) extends LookupList {
	def run(env: Environment): RqResult[CompilerStep] = {
		for {
			res1 <- (a.lookup(env) |@| b.lookup(env)) { fn }
			res <- res1
		} yield res
	}
}

final class LookupList3[A, B, C](
	a: LookupVariable[A],
	b: LookupVariable[B],
	c: LookupVariable[C],
	fn: (A, B, C) => RqResult[CompilerStep]
) extends LookupList {
	def run(env: Environment): RqResult[CompilerStep] = {
		for {
			res1 <- (a.lookup(env) |@| b.lookup(env) |@| c.lookup(env)) { fn }
			res <- res1
		} yield res
	}
}

final class LookupListBuilder2[A, B](
	a: LookupVariable[A],
	b: LookupVariable[B]
) {
	def apply(fn: (A, B) => RqResult[CompilerStep]): LookupList = {
		new LookupList2(a, b, fn)
	}
	
	def |@|[C](c: LookupVariable[C]): LookupListBuilder3[C] = {
		new LookupListBuilder3(c)
	}
	
	final class LookupListBuilder3[C](c: LookupVariable[C]) {
		def apply(fn: (A, B, C) => RqResult[CompilerStep]): LookupList = {
			new LookupList3(a, b, c, fn)
		}
	}
}

case class Plate(val id: String, val plateModel: String)

object ApplicativeMain extends App {
	
	def getParam(id: String)(implicit env: Environment): Option[String] = env.cmd_m.get(id)
	def getPlate(id: String)(implicit env: Environment): Option[String] = env.obj_m.get(id)
	def getLiquid(id: String)(implicit env: Environment): Option[String] = env.obj_m.get(id)
	
	val env1 = new Environment(
		Map("P1" -> "Plate1", "water" -> "Water"),
		Map("plate" -> "P1", "liquid" -> "water"),
		table_m = Map(
			"cmd" -> Map(
				"_" -> JsonParser("""{ "plate": "P1", "liquid": "water" }""").asJsObject
			),
			"plate" -> Map(
				"P1" -> JsonParser("""{ "id": "P1", "plateModel": "Nunc" }""").asJsObject
			)
		)
	)
	
	def makeit(): (Environment => Unit) = {
		def x(env0: Environment): Unit = {
			implicit val env = env0
			(getParam("plate") |@| getParam("liquid")) { (plateId, liquidId) =>
				(getPlate(plateId) |@| getLiquid(liquidId)) { (plate, liquid) =>
					println(plate, liquid)
				}
			}
		}
		x
	}
	
	def getParam2(field: String): LookupVariable[String] = {
		new LookupCmdField[String](field, (x: JsValue) => x match {
			case JsString(s) => RqSuccess(s)
			case _ => RqSuccess(x.toString)
		})
	}
	
	object MyJsonProtocol extends spray.json.DefaultJsonProtocol {
		implicit val plateFormat = jsonFormat2(Plate)
	}
	
	def getPlate2(id: String): LookupVariable[Plate] = {
		import MyJsonProtocol._
		def fn(jsobj: JsObject): RqResult[Plate] = {
			try {
				val plate = jsobj.convertTo[Plate]
				RqSuccess(plate)
			}
			catch {
				case ex : Throwable => RqError(ex.getMessage())
			}
		}
		new LookupObj[Plate]("plate", id, fn _)
	}

	// table, key, property => JsValue
	// fn: JsValue => A
	// action: A, B, C, D => Action | [Cmd]
	
	// RqResult
	// LookupVariable[
	def makeit2(): CompilerStep = {
		// 1. lookup in cmd: plate, liquid
		// 2. lookup in database: plate[P1], substance[water]
		// 3. run: print plate and liquid info
		
		// 1. (LookupCmdParameter[String]("plate") |@| LookupCmdParameter[String]("liquid"))
		// fn(String, String) => RqResult[ProcessStep]
		// 

		
		CompilerStep_Lookup(new LookupList2(
			getParam2("plate"),
			getParam2("liquid"),
			(plateId: String, liquidId: String) => { 
				println("level 1", plateId, liquidId)
				RqSuccess(CompilerStep_Lookup(getPlate2(plateId) {
					(plate: Plate) =>
					println("plate", plate)
					RqSuccess(CompilerStep_Done())
				}))
			}
		))
	}
	
	//makeit()(env1)
	def doit(step: CompilerStep) {
		step match {
			case CompilerStep_Done() =>
			case CompilerStep_Lookup(l) =>
				l.run(env1) match {
					case RqSuccess(step2, _) => doit(step2)
					case RqError(ls, _) => ls.foreach(println)
				}
		}
	}
	val step0 = makeit2()
	doit(step0)
}