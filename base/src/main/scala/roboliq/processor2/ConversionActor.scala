package roboliq.processor2

import akka.actor._
import akka.routing.RoundRobinRouter
import roboliq.core.RqResult
import spray.json.JsValue

class ConversionActor(val idclass: IdClass, fn: List[Object] => ConversionResult) extends Actor {
	def receive = {
		case ActorMessage_Input(l) =>
			val res = fn(l)
			sender ! ActorMessage_ConversionOutput(idclass, res)
	}
}