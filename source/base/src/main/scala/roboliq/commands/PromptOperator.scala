package roboliq.commands

import scala.Option.option2Iterable

import roboliq.ai.plan.Strips
import roboliq.ai.plan.Unique
import roboliq.core.RqError
import roboliq.core.RqResult
import roboliq.core.RqSuccess
import roboliq.entities.Agent
import roboliq.entities.EntityBase
import roboliq.entities.Labware
import roboliq.entities.Shaker
import roboliq.entities.ShakerSpec
import roboliq.entities.Site
import roboliq.entities.WorldState
import roboliq.input.AgentInstruction
import roboliq.input.Context
import roboliq.input.Converter
import roboliq.plan.ActionHandler
import roboliq.plan.OperatorHandler
import roboliq.plan.OperatorInfo
import spray.json.JsObject
import spray.json.JsString
import spray.json.JsValue


case class PromptOperatorActionParams(
	agent_? : Option[String],
	text: String
)

class PromptOperatorActionHandler extends ActionHandler {
	
	def getActionName = "promptOperator"

	def getActionParamNames = List("agent", "text")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[PromptOperatorActionParams](paramToJsval_l, eb, state0)
		} yield {
			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_l = List(
				"?agent" -> params.agent_?.getOrElse("?agent")
			)
			val binding = binding_l.toMap

			OperatorInfo(id, Nil, Nil, "promptOperator", binding, paramToJsval_l.toMap) :: Nil
		}
	}
}

class PromptOperatorOperatorHandler extends OperatorHandler {
	def getDomainOperator: Strips.Operator = {
		Strips.Operator(
			name = "promptOperator",
			paramName_l = List("?agent"),
			paramTyp_l = List("agent"),
			preconds = Strips.Literals.empty,
			effects = roboliq.ai.plan.Strips.Literals.empty
		)
	}
	
	def getInstruction(
		operator: Strips.Operator,
		instructionParam_m: Map[String, JsValue]
	): Context[Unit] = {
		val List(agentName) = operator.paramName_l
		
		for {
			agent <- Context.getEntityAs[Agent](agentName)
			params <- Converter.convInstructionParamsAs[PromptOperatorActionParams](instructionParam_m)
			instruction = Prompt(params.text)
			_ <- Context.addInstruction(agent, instruction)
		} yield ()
	}
}
