/*package roboliq.processor2

import akka.actor._
import akka.pattern.ask
import akka.routing.RoundRobinRouter
import spray.json.JsObject
import spray.json.JsString
import roboliq.core._
import scala.concurrent.Future


class CommandActor(
	master: ActorRef,
	index_? : Option[Integer],
	id: List[String],
	cmd: JsObject
) extends ComputingActor(
	master,
	index_?,
	Nil
) {
	//var handler_? : Option[CommandHandler] = None
	var token_l: List[(Int, Token)] = Nil
	
	val cmd_? : Option[String] = cmd.fields.get("cmd").flatMap(_ match {
		case JsString(s) => Some(s)
		case _ => None
	})
	
	def receive = {
		case ActorMessage_Compute(env) =>
			compute(env)
	}
	
	def process(env: ComputingEnvironment, inputObj_l: List[Object]): RqResult[_] = {
		cmd_? match {
			case None => RqError("missing field `cmd`")
			case Some(name) =>
				env.handler_m.get(name) match {
					case None => RqError(s"no handler found for `cmd = ${name}`")
					case Some(handler) =>
						val hresult = handler.getResult
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
						hresult
				}
		}
	}
}
*/