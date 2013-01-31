package roboliq.processor2

import akka.actor._
import akka.pattern.ask
import akka.routing.RoundRobinRouter
import spray.json.JsObject
import spray.json.JsString
import roboliq.core._
import scala.concurrent.Future


class CommandActor(
	master: ActorRef,
	id: List[String],
	cmd: JsObject,
	index_? : Option[Integer]
) extends ComputingActor(
	master,
	index_?,
	Nil
) {
	var handler_? : Option[CommandHandler] = None
	var token_l: List[(Int, Token)] = Nil
	
	val cmd_? : Option[String] = cmd.fields.get("cmd").flatMap(_ match {
		case JsString(s) => Some(s)
		case _ => None
	})
	
	override def preStart() {
		master ! ActorMessage_Status(id, Status.Initialized)
		cmd_?.foreach(s => master ! ActorMessage_RequestCommandHandler(s))
	}
	
	def receive = {
		case ActorMessage_Handler(result) =>
			handler_? = None
			token_l = Nil
			removeChildren()
			result match {
				case RqSuccess(handler, _) =>
					val hresult = handler.getResult
					handler_? = Some(handler)
					result_? = Some(hresult)
					hresult.foreach(l => l.zipWithIndex.foreach { pair =>
						val (item, i) = pair
						val index = i + 1
						val idItem = id ++ List(index.toString)
						item match {
							case ComputationItem_Command(cmd) =>
								val actor = new CommandActor(master, idItem, cmd, Some(index))
								context.actorOf(Props(actor), name = idItem.mkString("."))
							case ComputationItem_Computation(entity_l, fn) =>
							case ComputationItem_Token(token) =>
						}
					})
				case _ =>
					handler_? = None
					result_? = Some(result)
			}
			
		case ActorMessage_Entities(map) =>
			handleEntities(map)
	}
}