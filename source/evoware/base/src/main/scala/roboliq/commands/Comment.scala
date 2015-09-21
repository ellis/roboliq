package roboliq.commands

import scala.reflect.runtime.universe

import roboliq.ai.strips
import roboliq.core.RqResult
import roboliq.entities.Agent
import roboliq.entities.EntityBase
import roboliq.entities.WorldState
import roboliq.input.Context
import roboliq.input.Converter
import roboliq.plan.ActionHandler
import roboliq.plan.OperatorHandler
import roboliq.plan.OperatorInfo
import spray.json.JsString
import spray.json.JsValue


case class CommentActionParams(
	agent_? : Option[String],
	text: String
)

class CommentActionHandler extends ActionHandler {
	
	def getActionName = "comment"

	def getActionParamNames = List("agent", "text")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[CommentActionParams](paramToJsval_l, eb, state0)
		} yield {
			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_l = List(
				"?agent" -> params.agent_?.getOrElse("?agent")
			)
			val binding = binding_l.toMap

			OperatorInfo(id, Nil, Nil, "comment", binding, paramToJsval_l.toMap) :: Nil
		}
	}
}

class CommentOperatorHandler extends OperatorHandler {
	def getDomainOperator: strips.Operator = {
		strips.Operator(
			name = "comment",
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
			agent <- Context.getEntityAs[Agent](agentName)
			params <- Converter.convInstructionParamsAs[CommentActionParams](instructionParam_m)
			instruction = Log(params.text)
			_ <- Context.addInstruction(agent, instruction)
		} yield ()
	}
}
