package roboliq.utils0.temp

import scala.language.implicitConversions
import scalaz._
//import Scalaz._
import Endo._
import WriterT._
import scalaz.syntax.all._
import scalaz.syntax.std.all._

trait Task
case class Event(s: String) extends Task
case class Token(s: String) extends Task

case class Func(
	input_l: List[Event],
	body: FuncBody
) extends Task

case class FuncBody(
	event_l: List[Event],
	token_l: List[Token],
	sub_l: List[Func]
)

case class Output(
	event_r: List[Event] = Nil,
	token_r: List[Token] = Nil,
	sub_r: List[Fn] = Nil
) {
	def event(o: String): Output = copy(event_r = Event(o) :: event_r)
	def token(o: String): Output = copy(token_r = Token(o) :: token_r)
	def sub(o: Fn): Output = copy(sub_r = o :: sub_r)
}

case class Fn(
	arg_l: List[String],
	fn: (List[String]) => Validation[String, Output]
)

object WriterEndoMain extends App {
	val output = Output()
	
	def input
		(a: String)
		(fn: (String) => Validation[String, Output])
		: Validation[String, Output]
	= {
		def fn_#(l: List[String]) = fn(l.head)
		output.sub(Fn(List(a), fn_# _))
	}
	
	implicit def OutputToValidation(output: Output): Validation[String, Output] = output.success
	
	def handleCmd0(cmd: String): Validation[String, Output] = {
		output
			.event("E1")
			.event("E2")
			.success
	}
	
	def handleCmd1(cmd: String): Validation[String, Output] = {
		input ("a") { (a) =>
			output
				.event("E1:"+a)
				.event("E2")
		}
	}
	
	def handleCmd2(cmd: String): Validation[String, Output] = {
		input("a") { (a) =>
			input("b") { (b) =>
				output
					.event("E1:"+a)
					.event("E2:"+b)
			}
		}
	}
	// C1/S1/S1/E[12]
	
	println(handleCmd0("wash"))
	println()
	
	val fn1_? = handleCmd1("wash")
	println(fn1_?)
	println(fn1_?.map(_.sub_r.map(_.fn(List("me")))))
	println()
	
	val fn2_? = handleCmd2("wash")
	println(fn2_?)
	println(fn2_?.map(_.sub_r.map(_.fn(List("me")))))
	println()
}

object WriterEndoMain2 extends App {
	def withEvent(event: Event, body: FuncBody): FuncBody = body.copy(event_l = event :: body.event_l)
	def withToken(o: Token, body: FuncBody): FuncBody = body.copy(token_l = o :: body.token_l)
	def withSub(o: Func, body: FuncBody): FuncBody = body.copy(sub_l = o :: body.sub_l)
	
	def event(s: String): Writer[Endo[FuncBody], Event] = {
		val event = Event(s)
		for {
			_ <- tell(((body: FuncBody) => withEvent(event, body)).endo)
		} yield event
	}
	
	def token(s: String): Writer[Endo[FuncBody], Token] = {
		val o = Token(s)
		for {
			_ <- tell(((body: FuncBody) => withToken(o, body)).endo)
		} yield o
	}
	
	def sub(func: Func): Writer[Endo[FuncBody], Func] = {
		for {
			_ <- tell(((body: FuncBody) => withSub(func, body)).endo)
		} yield func
	}
	
	def sub(e: Writer[Endo[FuncBody], Unit]): Writer[Endo[FuncBody], Func] = {
		val body0 = FuncBody(Nil, Nil, Nil)
		val body = e.run._1(body0)
		val func = Func(Nil, body)
		for {
			_ <- tell(((body: FuncBody) => withSub(func, body)).endo)
		} yield func
	}
	
	def func(e: Writer[Endo[FuncBody], Unit]): Validation[String, Func] = {
		val body0 = FuncBody(Nil, Nil, Nil)
		val body = e.run._1(body0)
		val fn = Func(Nil, body)
		fn.success
	}
	
	val f1 = func {
		for {
			_ <- event("hi")
			_ <- event("bye")
			_ <- token("token")
			_ <- sub {
				for {
					_ <- event("subevent")
				} yield ()
			}
		} yield ()
	}
	
	println(f1)
}