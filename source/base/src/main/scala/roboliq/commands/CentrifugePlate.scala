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
import roboliq.entities.Centrifuge
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


case class CentrifugeProgram(
	rpm_? : Option[Int],
	duration_? : Option[Int],
	temperature_? : Option[Int],
	spinUpTime_? : Option[Int],
	spinDownTime_? : Option[Int]
)

case class CentrifugePlateActionParams(
	agent_? : Option[String],
	device_? : Option[String],
	program: CentrifugeProgram,
	`object`: Labware,
	site_? : Option[Site]
)

class CentrifugePlateActionHandler extends ActionHandler {
	
	def getActionName = "centrifugePlate"

	def getActionParamNames = List("agent", "device", "program", "object", "site")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[CentrifugePlateActionParams](paramToJsval_l, eb, state0)
			labwareName <- eb.getIdent(params.`object`)
			siteName_? <- params.site_? match {
				case None => RqSuccess(None)
				case Some(site) => eb.getIdent(site).map(Some(_))
			}
		} yield {
			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_l = List(
				"?agent" -> params.agent_?.getOrElse("?agent"),
				"?device" -> params.device_?.getOrElse("?device"),
				"?labware" -> labwareName,
				"?site" -> siteName_?.getOrElse("?site")
			)
			val binding = binding_l.toMap

			// FIXME: A lot more needs to be done here...
			OperatorInfo(id, Nil, Nil, "centrifugePlate", binding, paramToJsval_l.toMap) :: Nil
		}
	}
}

class CentrifugePlateOperatorHandler extends OperatorHandler {
	def getDomainOperator: Strips.Operator = {
		Strips.Operator(
			name = "centrifugePlate",
			paramName_l = List("?agent", "?device", "?labware", "?model", "?site"),
			paramTyp_l = List("agent", "centrifuge", "labware", "model", "site"),
			preconds = Strips.Literals(Unique(
				Strips.Literal(true, "agent-has-device", "?agent", "?device"),
				Strips.Literal(Strips.Atom("device-can-site", List("?device", "?site")), true),
				Strips.Literal(Strips.Atom("model", List("?labware", "?model")), true),
				Strips.Literal(Strips.Atom("location", List("?labware", "?site")), true)
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
			agent <- Context.getEntityAs[Agent](agentName)
			device <- Context.getEntityAs[Centrifuge](deviceName)
			program <- instructionParam_m.get("program") match {
				case Some(x@JsObject(obj)) =>
					Context.getEntityAs[CentrifugeProgram](x)
				//case Some(JsString(s)) =>
				//	val programName = operator.paramName_l(2)
				//	Context.getEntityAs[CentrifugeSpec](programName)
				case _ => Context.error("Expected identifier or centrifuge program")
			}
			labware <- Context.getEntityAs[Labware](labwareName)
			site <- Context.getEntityAs[Site](siteName)
			instruction = CentrifugeRun(
				device,
				program/*,
				List((labware, site))*/
			)
			_ <- Context.addInstruction(agent, instruction)
		} yield ()
	}
}
