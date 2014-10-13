package roboliq.commands

import scala.Option.option2Iterable
import aiplan.strips2.Strips
import aiplan.strips2.Unique
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
import roboliq.entities.Reader


case class MeasureAbsorbanceActionParams(
	agent_? : Option[String],
	device_? : Option[String],
	programFile: String,
	`object`: Labware,
	site_? : Option[Site]
)

class MeasureAbsorbanceActionHandler extends ActionHandler {
	
	def getActionName = "measureAbsorbance"

	def getActionParamNames = List("agent", "device", "programFile", "object", "site")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[MeasureAbsorbanceActionParams](paramToJsval_l, eb, state0)
			labwareName <- eb.getIdent(params.`object`)
			siteName_? <- params.site_? match {
				case None => RqSuccess(None)
				case Some(site) => eb.getIdent(site).map(Some(_))
			}
		} yield {

			
			val suffix = id.mkString("__", "_", "")
			val agentName = params.agent_?.getOrElse("$agent"+suffix)
			val deviceName = params.device_?.getOrElse("$device"+suffix)
			val siteName = siteName_?.getOrElse("$site2"+suffix)
			val modelName = "$model"+suffix
			val site1Name = "$site1"+suffix
			val site3Name = site1Name // Could allow this to be different from site1...
			
			// Bindings for transfer to sealer
			val bindingOpenClose_m = Map[String, String](
				"?agent" -> agentName,
				"?device" -> deviceName,
				"?site" -> siteName
			)
			// Bindings for transfer to sealer
			val bindingTransportBefore_m = Map(
				"?labware" -> labwareName,
				"?model" -> modelName,
				"?site" -> site1Name
			)
			// Bindings for transfer to sealer
			val bindingTransportAfter_m = Map(
				"?labware" -> labwareName,
				"?model" -> modelName,
				"?site" -> site3Name
			)
			// Binding for the actual measurement
			val bindingMeasure_m = Map(
				"?agent" -> agentName,
				"?device" -> deviceName,
				"?labware" -> labwareName,
				"?model" -> modelName,
				"?site" -> siteName
			)
			
			List(
				OperatorInfo(id ++ List(1), Nil, Nil, "openDeviceSite", bindingOpenClose_m, Map()),
				OperatorInfo(id ++ List(2), Nil, Nil, "transportLabware", bindingTransportBefore_m, Map()),
				OperatorInfo(id ++ List(3), Nil, Nil, "closeDeviceSite", bindingOpenClose_m, Map()),
				OperatorInfo(id ++ List(4), Nil, Nil, "measureAbsorbance", bindingMeasure_m, paramToJsval_l.toMap),
				OperatorInfo(id ++ List(5), Nil, Nil, "openDeviceSite", bindingOpenClose_m, Map()),
				OperatorInfo(id ++ List(6), Nil, Nil, "transportLabware", bindingTransportAfter_m, Map()),
				OperatorInfo(id ++ List(7), Nil, Nil, "closeDeviceSite", bindingOpenClose_m, Map())
			)
		}
	}
}

class MeasureAbsorbanceOperatorHandler extends OperatorHandler {
	def getDomainOperator: Strips.Operator = {
		Strips.Operator(
			name = "measureAbsorbance",
			paramName_l = List("?agent", "?device", "?labware", "?model", "?site"),
			paramTyp_l = List("agent", "reader", "labware", "model", "site"),
			preconds = Strips.Literals(Unique(
				Strips.Literal(true, "agent-has-device", "?agent", "?device"),
				Strips.Literal(Strips.Atom("device-can-site", List("?device", "?site")), true),
				Strips.Literal(Strips.Atom("location", List("?labware", "?site")), true)
				// TODO: device site should be closed
			)),
			effects = aiplan.strips2.Strips.Literals.empty
		)
	}
	
	def getInstruction(
		operator: Strips.Operator,
		instructionParam_m: Map[String, JsValue]
	): Context[Unit] = {
		val List(agentName, deviceName, labwareName, _, siteName) = operator.paramName_l
		
		for {
			params <- Converter.convInstructionParamsAs[MeasureAbsorbanceActionParams](instructionParam_m)
			agent <- Context.getEntityAs[Agent](agentName)
			device <- Context.getEntityAs[Reader](deviceName)
			labware <- Context.getEntityAs[Labware](labwareName)
			site <- Context.getEntityAs[Site](siteName)
			instruction = ReaderRun(
				device,
				params.programFile,
				List((labware, site))
			)
			_ <- Context.addInstruction(agent, instruction)
		} yield ()
	}
}
