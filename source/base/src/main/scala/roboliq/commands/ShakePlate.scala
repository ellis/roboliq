package roboliq.commands

import scala.Option.option2Iterable
import aiplan.strips2.Strips
import aiplan.strips2.Unique
import roboliq.core.RqError
import roboliq.core.RqResult
import roboliq.core.RqSuccess
import roboliq.plan.ActionHandler
import roboliq.plan.OperatorInfo
import spray.json.JsNull
import spray.json.JsObject
import spray.json.JsString
import spray.json.JsValue
import roboliq.input.commands.ShakerRun
import roboliq.entities.EntityBase
import roboliq.entities.Shaker
import roboliq.input.Converter
import roboliq.entities.ShakerSpec
import roboliq.entities.Labware
import roboliq.entities.Site
import roboliq.input.commands.Command
import roboliq.plan.AgentInstruction
import roboliq.entities.Agent
import roboliq.entities.WorldState
import roboliq.plan.OperatorHandler


case class ShakePlateActionParams(
	agent_? : Option[String],
	device_? : Option[String],
	program: ShakerSpec,
	`object`: Labware,
	site_? : Option[Site]
)

class ShakePlateActionHandler extends ActionHandler {
	
	def getActionName = "shakePlate"

	def getActionParamNames = List("agent", "device", "program", "object", "site")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[OperatorInfo] = {
		/*
		val m0 = paramToJsval_l.toMap
		val (programName, programObject_?) = m0.get("program") match {
			case None => ("?program", None)
			case Some(JsNull) => ("?program", None)
			case Some(JsString(s)) => (s, None)
			case Some(JsObject(obj)) =>
				val programName = id.mkString("_")+"_program"
				(programName, Some(programName -> "shakerProgram"))
			case x =>
				// TODO, should return an error here
				return RqError(s"Unexpected data for `program`: ${x}")
		}

		// Create planner objects if program was defined inside this command
		val problemObjectToTyp_l = List[Option[(String, String)]](
			programObject_?
		).flatten
		
		// TODO: require labware, otherwise the action doesn't make sense
		// TODO: possibly lookup labwareModel of labware
		val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
		val binding = Map(
			"?agent" -> m.getOrElse("agent", "?agent"),
			"?device" -> m.getOrElse("device", "?device"),
			"?program" -> programName,
			"?labware" -> m.getOrElse("object", "?labware"),
			//"?model" -> m.getOrElse("model", "?model"),
			"?site" -> m.getOrElse("site", "?site")
		)
		
		val planAction = domainOperator.bind(binding)
		
		println(s"getOperatorInfo(${id}, ${paramToJsval_l}):")
		println(m0)
		println(programName, programObject_?)
		println(m)
		println(binding)
		
		RqSuccess(OperatorInfo(id, paramToJsval_l, domainOperator, problemObjectToTyp_l, Nil, planAction))
		*/
		for {
			params <- Converter.convActionAs[ShakePlateActionParams](paramToJsval_l, eb)
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

			OperatorInfo(id, Nil, Nil, "shakePlate", binding, paramToJsval_l.toMap)
		}
	}
}

/*case class ShakePlateInstructionParams(
	agent: Agent,
	device: Shaker,
	program: ShakerSpec,
	labware: Labware,
	site: Site
)*/

class ShakePlateOperatorHandler extends OperatorHandler {
	def getDomainOperator: Strips.Operator = {
		Strips.Operator(
			name = "shakePlate",
			paramName_l = List("?agent", "?device", "?labware", "?model", "?site"),
			paramTyp_l = List("agent", "shaker", "labware", "model", "site"),
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
		instructionParam_m: Map[String, JsValue],
		eb: roboliq.entities.EntityBase,
		state0: WorldState
	): RqResult[List[AgentInstruction]] = {
		val List(agentName, deviceName, labwareName, _, siteName) = operator.paramName_l
		
		(for {
			agent <- eb.getEntityAs[Agent](agentName)
			device <- eb.getEntityAs[Shaker](deviceName)
			program <- instructionParam_m.get("program") match {
				case Some(x@JsObject(obj)) =>
					Converter.convAs[ShakerSpec](x, eb, None)
				case Some(JsString(s)) =>
					val programName = operator.paramName_l(2)
					eb.getEntityAs[ShakerSpec](programName)
				case _ => RqError("Expected identifier or shaker program")
			}
			labware <- eb.getEntityAs[Labware](labwareName)
			site <- eb.getEntityAs[Site](siteName)
		} yield {
			List(AgentInstruction(agent, ShakerRun(
				device,
				program,
				List((labware, site))
			)))
		}).prependError("ShakePlate.getInstruction:")
	}
}
