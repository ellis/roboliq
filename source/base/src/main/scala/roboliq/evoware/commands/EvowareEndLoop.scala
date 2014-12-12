package roboliq.evoware.commands

import scala.reflect.runtime.universe

import roboliq.ai.plan.Strips
import roboliq.core.RqResult
import roboliq.entities.Agent
import roboliq.entities.EntityBase
import roboliq.entities.WorldState
import roboliq.evoware.translator.L0C_EndLoop
import roboliq.input.Context
import roboliq.input.Converter
import roboliq.plan.ActionHandler
import roboliq.plan.OperatorHandler
import roboliq.plan.OperatorInfo
import spray.json.JsString
import spray.json.JsValue


case class EvowareEndLoopActionParams(
	agent_? : Option[String]
)

class EvowareEndLoopActionHandler extends ActionHandler {
	
	def getActionName = "evoware.endLoop"

	def getActionParamNames = List("agent")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[EvowareEndLoopActionParams](paramToJsval_l, eb, state0)
		} yield {
			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_l = List(
				"?agent" -> params.agent_?.getOrElse("?agent")
			)
			val binding = binding_l.toMap

			OperatorInfo(id, Nil, Nil, "evoware.endLoop", binding, paramToJsval_l.toMap) :: Nil
		}
	}
}

class EvowareEndLoopOperatorHandler extends OperatorHandler {
	def getDomainOperator: Strips.Operator = {
		Strips.Operator(
			name = "evoware.endLoop",
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
			instruction = EvowareInstruction(L0C_EndLoop())
			_ <- Context.addInstruction(agent, instruction)
		} yield ()
	}
}
