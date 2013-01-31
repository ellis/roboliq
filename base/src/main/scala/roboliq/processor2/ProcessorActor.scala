package roboliq.processor2

import akka.actor._
import akka.routing.RoundRobinRouter
import roboliq.core._
import scala.collection._


abstract class MasterActor extends Actor

abstract class ProcessorActor extends Actor {
	def removeChildren() {
		context.children.foreach(context.stop)
	}
}

abstract class ComputingActor(
	val master: ActorRef,
	//val parent_? : Option[ComputingActor],
	val index_? : Option[Integer],
	//val id_? : Option[]
	val input_l: List[IdClass]
) extends ProcessorActor {
	protected var status = 0
	protected var result_? : Option[RqResult[_]] = None
	private var inputObj_m = Map[IdClass, Object]()
	private var inputObj_l = List[Object]()
	
	def run()
	def process(l: List[Object])
	
	def handleEntities(map: scala.collection.Map[IdClass, Object]) {
		val input2_l = input_l.map(map.get)
		val b = input2_l.forall(_.isDefined)
		val inputObj2_l = {
			if (b)
				input2_l.flatten
			else
				Nil
		}
		if (inputObj_l != inputObj2_l) {
			removeChildren()
			inputObj_l = inputObj2_l
			if (b) {
				status = 1
				run()
			}
			else {
				status = 0
			}
		}
		else {
			for (child <- context.children)
				child ! ActorMessage_Entities(map)
		}
	}

	def handleComputationResult(result: ComputationResult) {
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
	}
}
