package roboliq.processor2

import spray.json.JsValue
import roboliq.core.RqResult

sealed trait ActorMessage
case class ActorMessage_ComputationInput(l: List[Object]) extends ActorMessage
case class ActorMessage_ComputationOutput(node: Node_Computation, result: HandlerResult) extends ActorMessage
//case class ActorMessage_ConversionInput(jsval: JsValue) extends ActorMessage
//case class ActorMessage_ConversionOutput(idclass: IdClass, result: HandlerResult) extends ActorMessage
