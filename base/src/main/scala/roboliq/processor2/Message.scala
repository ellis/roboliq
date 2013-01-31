package roboliq.processor2

import spray.json.JsValue
import roboliq.core.RqResult
import spray.json.JsObject

sealed trait ActorMessage
case class ActorMessage_Start() extends ActorMessage
case class ActorMessage_CommandLookup(node: Node_Command) extends ActorMessage
case class ActorMessage_EntityLookup(id: String) extends ActorMessage
case class ActorMessage_ComputationInput(node: Node_Computation, l: List[Object]) extends ActorMessage
case class ActorMessage_ComputationOutput(node: Node_Computation, result: ComputationResult) extends ActorMessage
case class ActorMessage_ConversionInput(node: Node_Conversion, l: List[Object]) extends ActorMessage
case class ActorMessage_ConversionOutput(node: Node_Conversion, result: ConversionResult) extends ActorMessage
case class ActorMessage_Input(l: List[Object]) extends ActorMessage

case class ActorMessage_Status(id: List[String], status: Status.Value) extends ActorMessage
case class ActorMessage_AddCommand(cmd: JsObject) extends ActorMessage
case class ActorMessage_RequestCommandHandler(cmd: String) extends ActorMessage
case class ActorMessage_Handler(result: RqResult[CommandHandler]) extends ActorMessage
case class ActorMessage_Entities(map: scala.collection.Map[IdClass, Object]) extends ActorMessage