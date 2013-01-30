package roboliq.processor2

import akka.actor._
import akka.routing.RoundRobinRouter

class ComputationActor(node: Node_Computation) extends Actor {
	def receive = {
		case ActorMessage_ComputationInput(l) =>
			sender ! ActorMessage_ComputationOutput(node, node.result.fn(l))
	}
}