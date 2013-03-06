package roboliq.utils0.temp

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
	event_l: List[Event],
	token_l: List[Token]
)

object WriterEndoMain extends App {
	def withEvent(event: Event, func: Func): Func = func.copy(event_l = event :: func.event_l)
	def withToken(o: Token, func: Func): Func = func.copy(token_l = o :: func.token_l)
	
	def event(s: String): Writer[Endo[Func], Event] = {
		val event = Event(s)
		for {
			_ <- tell(((func: Func) => withEvent(event, func)).endo)
		} yield event
	}
	
	def token(s: String): Writer[Endo[Func], Token] = {
		val o = Token(s)
		for {
			_ <- tell(((func: Func) => withToken(o, func)).endo)
		} yield o
	}
	
	def func(e: Writer[Endo[Func], Unit]): Func = {
		val fn = Func(Nil, Nil)
		e.run._1(fn)
	}
	
	val f1 = func {
		for {
			_ <- event("hi")
			_ <- event("bye")
			_ <- token("token")
		} yield ()
	}
	
	println(f1)
}