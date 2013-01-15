package roboliq.utils0.applicative

import scalaz._
import Scalaz._

import spray.json.JsObject
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

sealed case class CompilerStep()

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
	def lookup(table: String, key: String, field: String): RqResult[JsValue] = {
		for {
			key_m <- table_m.get(table).asRq(s"table not found: `$table`")
			jsitem <- key_m.get(key).asRq(s"key not found: `$key` in table `$table`")
			jsfield <- jsitem.fields.get(field).asRq(s"field not found: `$field` of key `$key` in table `$table`")
		} yield jsfield
	}
}

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
			"plate" -> Map()
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
	
	final class LookupVariable[A](table: String, key: String, field: String, fn: JsValue => RqResult[A]) {
		def lookup(env: Environment): RqResult[A] = {
			for {
				jsval <- env.lookup(table, key, field)
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
	
	def getParam2(field: String): LookupVariable[String] = {
		new LookupVariable[String]("cmd", "_", field, (x: JsValue) => RqSuccess(x.toString))
	}

	// table, key, property => JsValue
	// fn: JsValue => A
	// action: A, B, C, D => Action | [Cmd]
	
	// RqResult
	// LookupVariable[
	def makeit2(): LookupList = {
		// 1. lookup in cmd: plate, liquid
		// 2. lookup in database: plate[P1], substance[water]
		// 3. run: print plate and liquid info
		
		// 1. (LookupCmdParameter[String]("plate") |@| LookupCmdParameter[String]("liquid"))
		// fn(String, String) => RqResult[ProcessStep]
		// 

		
		new LookupList2(
			getParam2("plate"),
			getParam2("liquid"),
			(plate: String, liquid: String) => { 
				println("level 1", plate, liquid)
				RqSuccess(CompilerStep())
			}
		)
	}
	
	//makeit()(env1)
	val x = makeit2().run(env1)
	println("x")
	println(x)
}