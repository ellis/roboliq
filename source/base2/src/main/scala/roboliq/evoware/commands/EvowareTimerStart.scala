package roboliq.evoware.commands

import scala.reflect.runtime.universe
import roboliq.ai.strips
import roboliq.ai.plan.Unique
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
import roboliq.evoware.translator.L0C_StartTimer


case class EvowareTimerStartActionParams(
	agent_? : Option[String],
	id: Int
)

class EvowareTimerStartActionHandler extends ActionHandler {
	
	def getActionName = "evoware.timer.start"

	def getActionParamNames = List("agent", "id")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[EvowareTimerStartActionParams](paramToJsval_l, eb, state0)
		} yield {
			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_l = List(
				"?agent" -> params.agent_?.getOrElse("?agent")
			)
			val binding = binding_l.toMap

			OperatorInfo(id, Nil, Nil, "evoware.timer.start", binding, paramToJsval_l.toMap) :: Nil
		}
	}
}

class EvowareTimerStartOperatorHandler extends OperatorHandler {
	def getDomainOperator: strips.Operator = {
		strips.Operator(
			name = "evoware.timer.start",
			paramName_l = List("?agent"),
			paramTyp_l = List("agent"),
			preconds = strips.Literals.empty,
			effects = roboliq.ai.strips.Literals.empty
		)
	}
	
	def getInstruction(
		operator: strips.Operator,
		instructionParam_m: Map[String, JsValue]
	): Context[Unit] = {
		val List(agentName) = operator.paramName_l
		
		for {
			params <- Converter.convInstructionParamsAs[EvowareTimerStartActionParams](instructionParam_m)
			agent <- Context.getEntityAs[Agent](agentName)
			instruction = EvowareInstruction(L0C_StartTimer(params.id))
			_ <- Context.addInstruction(agent, instruction)
		} yield ()
	}
}
