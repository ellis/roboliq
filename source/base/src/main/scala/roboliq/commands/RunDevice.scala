package roboliq.commands

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
import roboliq.entities.Device


case class RunDeviceActionParams(
	agent_? : Option[String],
	device_? : Option[String],
	program_? : Option[JsValue],
	programFile_? : Option[String],
	programData_? : Option[String]
)

// REFACTOR: This entire file is practically a duplicate of OpenDeviceSite.scala
class RunDeviceActionHandler extends ActionHandler {
	
	def getActionName = "runDevice"

	def getActionParamNames = List("agent", "device", "program", "programFile")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[RunDeviceActionParams](paramToJsval_l, eb, state0)
		} yield {
			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_l = List(
				"?agent" -> params.agent_?.getOrElse("?agent"),
				"?device" -> params.device_?.getOrElse("?device")
			)
			val binding = binding_l.toMap

			OperatorInfo(id, Nil, Nil, "runDevice", binding, paramToJsval_l.toMap) :: Nil
		}
	}
}

class RunDeviceOperatorHandler extends OperatorHandler {
	def getDomainOperator: Strips.Operator = {
		Strips.Operator(
			name = "runDevice",
			paramName_l = List("?agent", "?device"),
			paramTyp_l = List("agent", "device"),
			preconds = Strips.Literals(Unique(
				Strips.Literal(true, "agent-has-device", "?agent", "?device")
			)),
			effects = Strips.Literals.empty
		)
	}
	
	def getInstruction(
		operator: Strips.Operator,
		instructionParam_m: Map[String, JsValue]
	): Context[Unit] = {
		val List(agentName, deviceName) = operator.paramName_l
		
		for {
			params <- Converter.convInstructionParamsAs[RunDeviceActionParams](instructionParam_m)
			agent <- Context.getEntityAs[Agent](agentName)
			device <- Context.getEntityAs[Device](deviceName)
			instruction = DeviceRun(
				device,
				params.program_?,
				params.programFile_?,
				params.programData_?
			)
			_ <- Context.addInstruction(agent, instruction)
		} yield ()
	}
}
