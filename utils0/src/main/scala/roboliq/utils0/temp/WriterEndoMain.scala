package roboliq.utils0.temp

import scala.language.implicitConversions
import scalaz._
import Scalaz._
//import Endo._
//import List._
//import WriterT._
//import scalaz.syntax.all._
//import scalaz.syntax.std.all._

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

/*case class Output(
	l: List[Task] = Nil
)*/

case class Fn(
	arg_l: List[String],
	fn: (List[String]) => Validation[String, List[Task]]
) extends Task

class OutputBuilder(val l: List[Validation[String, Task]]) {
	/*
	def |&|(a: Task, b: Task): Validation[String, Output] =
		Output(a :: b :: Nil).success
	
	def |&|(a: Validation[String, Output], b: Task): Validation[String, Output] =
		a.map(_ << b)
	
	def |&|(a: Validation[String, Output], b: Validation[String, Task]): Validation[String, Output] =
		a.flatMap(x => b.map(y => x << y))
	*/
}

object WriterEndoMain extends App {
	type V[A] = Validation[String, A]
	type Output = List[Task]
	
	implicit def OutputToValidation(output: Output): Validation[String, Output] = output.success
	/*implicit object OutputMonoid extends Monoid[Output] {
		def zero = Nil
		def append(a: Output, b: => Output) = Output(a.l ++ b.l)
	}*/
	implicit def TaskToValidationOutput(a: Task): Validation[String, Output] =
		List(a).success
	implicit def TaskToOutputBuilder(a: Task): OutputBuilder =
		new OutputBuilder(List(a.success))
	
	/*class OutputBuilder
	def |&|(a: Task, b: Task): Validation[String, Output] =
		Output(a :: b :: Nil).success
	
	def |&|(a: Validation[String, Output], b: Task): Validation[String, Output] =
		a.map(_ << b)
	
	def |&|(a: Validation[String, Output], b: Validation[String, Task]): Validation[String, Output] =
		a.flatMap(x => b.map(y => x << y))
	*/
		
	//val output = Output()
	//val output = Monoid[Output].zero
	
	def output(l: OutputBuilder*): V[Output] = {
		val l1: List[V[Task]] = l.toList.flatMap(_.l)
		val l2: V[List[Task]] = l1.sequence
		//val l3: V[Output] = l2.map(Output(_))
		l2
	}
	
	def input
		(a: String)
		(fn: (String) => Validation[String, Output])
		: Validation[String, Output]
	= {
		def fn_#(l: List[String]) = fn(l.head)
		output(Fn(List(a), fn_# _))
	}
	
	def handleCmd0(cmd: String): Validation[String, Output] = {
		output(
			Event("E1"),
			Event("E2")
		)
	}
	
	def handleCmd1(cmd: String): Validation[String, Output] = {
		input ("a") { (a) =>
			output(
				Event("E1:"+a),
				Event("E2")
			)
		}
	}
	
	def handleCmd2(cmd: String): Validation[String, Output] = {
		input("a") { (a) =>
			input("b") { (b) =>
				output(
					Event("E1:"+a),
					Event("E2:"+b)
				)
			}
		}
	}
	// C1/S1/S1/E[12]
	
	println(handleCmd0("wash"))
	println()
	
	val fn1_? = handleCmd1("wash")
	println(fn1_?)
	//println(fn1_?.map(_.sub_r.map(_.fn(List("me")))))
	println()
	
	val fn2_? = handleCmd2("wash")
	println(fn2_?)
	//println(fn2_?.map(_.sub_r.map(_.fn(List("me")))))
	println()
}

object WriterEndoMain2 extends App {
	//import Endo._
	import WriterT._

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