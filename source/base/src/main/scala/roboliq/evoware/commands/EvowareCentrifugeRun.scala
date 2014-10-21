package roboliq.evoware.commands

import aiplan.strips2.Strips
import roboliq.core.RqResult
import roboliq.core.RqSuccess
import roboliq.entities.Agent
import roboliq.entities.EntityBase
import roboliq.entities.WorldState
import roboliq.evoware.translator.L0C_StartTimer
import roboliq.input.Context
import roboliq.input.Converter
import roboliq.plan.ActionHandler
import roboliq.plan.OperatorHandler
import roboliq.plan.OperatorInfo
import spray.json.JsString
import spray.json.JsValue
import roboliq.entities.Centrifuge
import roboliq.commands.CentrifugeRun
import roboliq.commands.CentrifugeProgram
import roboliq.evoware.translator.L0C_Facts


case class EvowareCentrifugeRunActionParams(
	agent_? : Option[String],
	device_? : Option[String],
	program: CentrifugeProgram
)

class EvowareCentrifugeRunActionHandler extends ActionHandler {
	
	def getActionName = "evoware.centrifuge.run"

	def getActionParamNames = List("agent", "id", "duration")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[EvowareCentrifugeRunActionParams](paramToJsval_l, eb, state0)
		} yield {
			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_m = Map(
				"?agent" -> params.agent_?.getOrElse("?agent"),
				"?device" -> params.device_?.getOrElse("?device")
			)

			OperatorInfo(id, Nil, Nil, "evoware.centrifuge.run", binding_m, paramToJsval_l.toMap) :: Nil
		}
	}
}

class EvowareCentrifugeRunOperatorHandler extends OperatorHandler {
	def getDomainOperator: Strips.Operator = {
		Strips.Operator(
			name = "evoware.centrifuge.run",
			paramName_l = List("?agent", "?device"),
			paramTyp_l = List("agent", "centrifuge"),
			preconds = Strips.Literals.empty,
			effects = aiplan.strips2.Strips.Literals.empty
		)
	}
	
	def getInstruction(
		operator: Strips.Operator,
		instructionParam_m: Map[String, JsValue]
	): Context[Unit] = {
		val List(agentName, deviceName) = operator.paramName_l
		
		for {
			params <- Converter.convInstructionParamsAs[EvowareCentrifugeRunActionParams](instructionParam_m)
			agent <- Context.getEntityAs[Agent](agentName)
			device <- Context.getEntityAs[Centrifuge](deviceName)
			instruction = CentrifugeRun(device, params.program, Nil)
			_ <- Context.addInstruction(agent, instruction)
		} yield ()
	}
}
