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
import roboliq.entities.Sealer
import roboliq.entities.SealerSpec
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


case class SealPlateActionParams(
	agent_? : Option[String],
	device_? : Option[String],
	program_? : Option[SealerSpec],
	`object`: Labware,
	site_? : Option[Site]
)

class SealPlateActionHandler extends ActionHandler {
	
	def getActionName = "sealPlate"

	def getActionParamNames = List("agent", "device", "program", "object", "site")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[SealPlateActionParams](paramToJsval_l, eb, state0)
			labwareName <- eb.getIdent(params.`object`)
			siteName_? <- params.site_? match {
				case None => RqSuccess(None)
				case Some(site) => eb.getIdent(site).map(Some(_))
			}
		} yield {
			val suffix = id.mkString("__", "_", "")
			val modelName = "$model"+suffix
			val site2Name = siteName_?.getOrElse("$site2"+suffix)
			
			// Bindings for transfer to sealer
			val binding1_l = List[(String, String)](
				"?labware" -> labwareName,
				"?model" -> modelName,
				"?site1" -> ("$site1"+suffix),
				"?site2" -> site2Name
			)
			// Bindings for transfer to sealer
			val binding2_l = List(
				"?agent" -> params.agent_?.getOrElse("?agent"),
				"?device" -> params.device_?.getOrElse("?device"),
				"?labware" -> labwareName,
				"?model" -> modelName,
				"?site" -> site2Name
			)
			// Bindings for transfer from sealer
			val binding3_l = List(
				"?labware" -> labwareName,
				"?model" -> modelName,
				"?site1" -> site2Name,
				"?site2" -> ("$site1"+suffix)
			)

			OperatorInfo(id ++ List(1), Nil, Nil, "transportLabware", binding1_l.toMap, Map()) ::
			OperatorInfo(id ++ List(2), Nil, Nil, "sealPlate", binding2_l.toMap, paramToJsval_l.toMap) ::
			OperatorInfo(id ++ List(3), Nil, Nil, "transportLabware", binding3_l.toMap, Map()) :: Nil
		}
	}
}

class SealPlateOperatorHandler extends OperatorHandler {
	def getDomainOperator: Strips.Operator = {
		Strips.Operator(
			name = "sealPlate",
			paramName_l = List("?agent", "?device", "?labware", "?model", "?site"),
			paramTyp_l = List("agent", "sealer", "labware", "model", "site"),
			preconds = Strips.Literals(Unique(
				Strips.Literal(true, "agent-has-device", "?agent", "?device"),
				Strips.Literal(Strips.Atom("device-can-site", List("?device", "?site")), true),
				Strips.Literal(Strips.Atom("model", List("?labware", "?model")), true),
				Strips.Literal(Strips.Atom("location", List("?labware", "?site")), true)
			)),
			effects = roboliq.ai.plan.Strips.Literals.empty
		)
	}
	
	def getInstruction(
		operator: Strips.Operator,
		instructionParam_m: Map[String, JsValue]
	): Context[Unit] = {
		val List(agentName, deviceName, labwareName, _, siteName) = operator.paramName_l
		
		for {
			agent <- Context.getEntityAs[Agent](agentName)
			device <- Context.getEntityAs[Sealer](deviceName)
			program_? <- instructionParam_m.get("program") match {
				case Some(x@JsObject(obj)) =>
					Context.getEntityAs[SealerSpec](x).map(Some(_))
				case Some(JsString(s)) =>
					val programName = operator.paramName_l(2)
					Context.getEntityAs[SealerSpec](programName).map(Some(_))
				case _ => Context.unit(None)
			}
			labware <- Context.getEntityAs[Labware](labwareName)
			site <- Context.getEntityAs[Site](siteName)
			instruction = SealerRun(
				device,
				program_?,
				List((labware, site))
			)
			_ <- Context.addInstruction(agent, instruction)
		} yield ()
	}
}
