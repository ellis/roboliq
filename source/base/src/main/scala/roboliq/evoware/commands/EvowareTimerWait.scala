package roboliq.evoware.commands

import scala.reflect.runtime.universe
import aiplan.strips2.Strips
import aiplan.strips2.Unique
import roboliq.core.RqResult
import roboliq.core.RqSuccess
import roboliq.entities.Agent
import roboliq.entities.EntityBase
import roboliq.entities.Reader
import roboliq.entities.Site
import roboliq.entities.WorldState
import roboliq.input.Context
import roboliq.input.Converter
import roboliq.plan.ActionHandler
import roboliq.plan.OperatorHandler
import roboliq.plan.OperatorInfo
import spray.json.JsString
import spray.json.JsValue
import roboliq.commands.DeviceSiteOpen
import roboliq.evoware.translator.L0C_WaitTimer
import roboliq.evoware.translator.L0C_StartTimer


case class EvowareTimerWaitActionParams(
	agent_? : Option[String],
	id: Int,
	duration: Int // time in seconds
)

class EvowareTimerWaitActionHandler extends ActionHandler {
	
	def getActionName = "evoware.timer.wait"

	def getActionParamNames = List("agent", "id", "till")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[EvowareTimerWaitActionParams](paramToJsval_l, eb, state0)
		} yield {
			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_l = List(
				"?agent" -> params.agent_?.getOrElse("?agent")
			)
			val binding = binding_l.toMap

			OperatorInfo(id, Nil, Nil, "evoware.timer.wait", binding, paramToJsval_l.toMap) :: Nil
		}
	}
}

class EvowareTimerWaitOperatorHandler extends OperatorHandler {
	def getDomainOperator: Strips.Operator = {
		Strips.Operator(
			name = "evoware.timer.wait",
			paramName_l = List("?agent"),
			paramTyp_l = List("agent"),
			preconds = Strips.Literals.empty,
			effects = aiplan.strips2.Strips.Literals.empty
		)
	}
	
	def getInstruction(
		operator: Strips.Operator,
		instructionParam_m: Map[String, JsValue]
	): Context[Unit] = {
		val List(agentName) = operator.paramName_l
		
		for {
			params <- Converter.convInstructionParamsAs[EvowareTimerWaitActionParams](instructionParam_m)
			agent <- Context.getEntityAs[Agent](agentName)
			instruction = EvowareInstruction(L0C_WaitTimer(params.id, params.duration))
			_ <- Context.addInstruction(agent, instruction)
		} yield ()
	}
}
