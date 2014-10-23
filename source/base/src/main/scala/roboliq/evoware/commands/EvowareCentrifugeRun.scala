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
import aiplan.strips2.Unique


case class EvowareCentrifugeRunActionParams(
	agent_? : Option[String],
	device: Centrifuge,
	program: CentrifugeProgram
)

class EvowareCentrifugeRunActionHandler extends ActionHandler {
	
	def getActionName = "evoware.centrifuge.run"

	def getActionParamNames = List("agent", "device", "program")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[EvowareCentrifugeRunActionParams](paramToJsval_l, eb, state0)
			deviceName <- eb.getIdent(params.device)
		} yield {
			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val suffix = id.mkString("__", "_", "")
			val agentName = params.agent_?.getOrElse("$agent-"+suffix)
			val binding_m = Map(
				"?agent" -> agentName
			)

			OperatorInfo(id ++ List(1), Nil, Nil, "carousel.close-"+deviceName, binding_m, Map()) ::
			OperatorInfo(id ++ List(2), Nil, Nil, "evoware.centrifuge.run-"+deviceName, binding_m, paramToJsval_l.toMap) :: Nil
		}
	}
}

class EvowareCentrifugeRunOperatorHandler(
	deviceName: String,
	internalSiteIdent_l: List[String]
) extends OperatorHandler {
	def getDomainOperator: Strips.Operator = {
		Strips.Operator(
			name = "evoware.centrifuge.run-"+deviceName,
			paramName_l = List("?agent", "?device"),
			paramTyp_l = List("agent", "centrifuge"),
			preconds = Strips.Literals(Unique(
				Strips.Literal(true, "agent-has-device", "?agent", "?device")
			)),
			effects = Strips.Literals(Unique(internalSiteIdent_l.map(ident => 
				Strips.Literal(true, "site-closed", ident)
			) : _*))
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
			instruction = CentrifugeRun(device, params.program)
			_ <- Context.addInstruction(agent, instruction)
		} yield ()
	}
}
