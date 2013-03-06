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
	val withEvent = (event: Event, func: Func) => func.copy(event_l = event :: func.event_l)
	
	def event(s: String): Writer[Endo[Func], Event] = {
		val event = Event(s)
		for {
			_ <- tell(((func: Func) => withEvent(event, func)).endo)
		} yield event
	}
	
	def func(e: Writer[Endo[Func], Unit]): Func = {
		val fn = Func(Nil, Nil)
		e.run._1(fn)
	}
	
	val f1 = func {
		for {
			_ <- event("hi")
			_ <- event("bye")
		} yield ()
	}
	
	println(f1)
}