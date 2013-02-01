package roboliq.processor2

import akka.actor._
import akka.routing.RoundRobinRouter

import roboliq.core._


class ComputationActor(
	master: ActorRef,
	index_? : Option[Integer],
	id: List[String],
	fn: List[Object] => ComputationResult,
	cmdKey: String
) extends ComputingActor(
	master,
	index_?,
	id,
	Nil
) {
	var handler_? : Option[CommandHandler] = None
	var token_l: List[(Int, Token)] = Nil
	
	def receive = {
		case ActorMessage_Input(l) =>
			val res = fn(l)
			sender ! ActorMessage_ComputationOutput(node, res)
	}

	def setComputationResult(result: ComputationResult) {
		result_? = Some(result)
		result match {
			case RqSuccess(l, _) =>
				val child_l: List[Node_Computes] = l.zipWithIndex.flatMap { pair => 
					val (r, index0) = pair
					val index = index0 + 1
					r match {
						case ComputationItem_Command(cmd, fn) =>
							val actor = new CommandActor(cmd, fn)
							val actorRef = system.actor
							/*val idCmd = node.id ++ List(index)
							val idCmd_s = getIdString(idCmd)
							val idEntity = s"cmd[${idCmd_s}]"
							cmdToJs_m(idCmd) = cmd
							setEntity(idEntity, cmd)
	
							val result = ComputationItem_Computation(Nil, (_: List[Object]) => fn)*/
							Some(Node_Command(node, index, cmd))
						case result: ComputationItem_Computation =>
							val entity2_l = result.entity_l.map(idclass => {
								// Substitute in full path for command parameters starting with '$'
								val idclass2 = {
									if (idclass.id.startsWith("$")) {
										val idCmd_s = getIdString(node.idCmd)
										idclass.copy(id = s"cmd[${idCmd_s}].${idclass.id.tail}")
									}
									else idclass
								}
								idclass2
							})
							//val result2 = result.copy(entity_l = entity2_l)
							Some(Node_Computation(node, index, entity2_l, result.fn, node.idCmd))
						case ComputationItem_Token(token) =>
							println("token: "+Node_Token(node, index + 1, token))
							None
						case ComputationItem_Entity(id, jsval) =>
							setEntity(id, jsval)
							//new Node_Result(node, index, r)
							None
						case _ =>
							//new Node_Result(node, index, r)
							None
					}
				}
				setChildren(node, child_l)
				status_m(node) = 2
			case _ =>
				status_m(node) = -1
		}
	}
}