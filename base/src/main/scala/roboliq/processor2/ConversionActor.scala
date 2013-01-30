/*package roboliq.processor2

import akka.actor._
import akka.routing.RoundRobinRouter
import roboliq.core.RqResult
import spray.json.JsValue

class ConversionActor(idclass: IdClass, fn: JsValue => ComputationResult) extends Actor {
	def receive = {
		case ActorMessage_ConversionInput(jsval) =>
			sender ! ActorMessage_ConversionOutput(idclass, fn(jsval))
	}
}
*/