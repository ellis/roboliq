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

class ComputingEnvironment(
	val handler_m: Map[String, CommandHandler],
	val entity_m: Map[IdClass, Object]
)

abstract class ComputingActor(
	val master: ActorRef,
	val index_? : Option[Integer],
	val id: List[String],
	val input_l: List[IdClass]
) extends ProcessorActor {
	protected var status = 0
	protected var result_? : Option[RqResult[_]] = None
	private var inputObj_m = Map[IdClass, Object]()
	private var inputObj_l = List[Object]()
	
	override def preStart() {
		master ! ActorMessage_RequestInputs(input_l)
	}
	
	def compute(env: ComputingEnvironment) {
		inputObj_l = Nil
		val input2_l = input_l.map(env.entity_m.get)
		val bHaveInput = input2_l.forall(_.isDefined)
		val inputObj2_l = if (bHaveInput) input2_l.flatten else Nil
		val bCompute = (bHaveInput && inputObj_l != inputObj2_l)
		if (!bHaveInput || bCompute)
			removeChildren()
		if (bCompute) {
			process(env, inputObj_l)
			for (child <- context.children)
				child ! ActorMessage_Compute(env)
		}
	}

	def handleComputationItems(l: List[ComputationItem]) {
		l.zipWithIndex.foreach { pair =>
			val (item, i) = pair
			val index = i + 1
			val idItem = id ++ List(index.toString)
			val idItem_s = idItem.mkString(".")
			item match {
				case ComputationItem_Command(cmd) =>
					val actor = new CommandActor(master, Some(index), idItem, cmd)
					context.actorOf(Props(actor), name = idItem_s)
				case ComputationItem_Computation(entity_l, fn) =>
					val actor = new ComputationActor(master, Some(index), idItem, fn)
					context.actorOf(Props(actor), name = idItem_s)
				case ComputationItem_Token(token) =>
			}
		}
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
	
	def run()
	def process(env: ComputingEnvironment, inputObj_l: List[Object]): RqResult[_]

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
