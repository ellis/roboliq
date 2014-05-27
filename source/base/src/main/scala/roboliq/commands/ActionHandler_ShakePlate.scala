package roboliq.commands

import scala.Option.option2Iterable
import aiplan.strips2.Strips
import aiplan.strips2.Unique
import roboliq.core.RqError
import roboliq.core.RqResult
import roboliq.core.RqSuccess
import roboliq.plan.ActionHandler
import roboliq.plan.ActionPlanInfo
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
import roboliq.plan.Instruction
import roboliq.entities.Agent
import roboliq.entities.WorldState


class ActionHandler_ShakePlate extends ActionHandler {
	
	def getActionName = "shakePlate"

	def getActionParamNames = List("agent", "device", "program", "object", "site")
	
	def getActionPlanInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)]
	): RqResult[ActionPlanInfo] = {
		val domainOperator = Strips.Operator(
			name = getActionName,
			paramName_l = List("?agent", "?device", "?program", "?labware", "?model", "?site"),
			paramTyp_l = List("agent", "shaker", "shakerProgram", "labware", "model", "site"),
			preconds = Strips.Literals(Unique(
				Strips.Literal(true, "agent-has-device", "?agent", "?device"),
				Strips.Literal(Strips.Atom("device-can-site", List("?device", "?site")), true),
				Strips.Literal(Strips.Atom("model", List("?labware", "?model")), true),
				Strips.Literal(Strips.Atom("location", List("?labware", "?site")), true)
			)),
			effects = aiplan.strips2.Strips.Literals.empty
		)

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
		
		println(s"getActionPlanInfo(${id}, ${paramToJsval_l}):")
		println(m0)
		println(programName, programObject_?)
		println(m)
		println(binding)
		
		RqSuccess(ActionPlanInfo(id, paramToJsval_l, domainOperator, problemObjectToTyp_l, Nil, planAction))
	}
	
	def getInstruction(
		planInfo: ActionPlanInfo,
		planned: Strips.Operator,
		eb: roboliq.entities.EntityBase,
		state0: WorldState
	): RqResult[List[Instruction]] = {
		val m0 = planInfo.paramToJsval_l.toMap

		for {
			agent <- eb.getEntityAs[Agent](planned.paramName_l(0))
			device <- eb.getEntityAs[Shaker](planned.paramName_l(1))
			program <- m0.get("program") match {
				case Some(x@JsObject(obj)) =>
					Converter.convAs[ShakerSpec](x, eb, None)
				case _ =>
					val programName = planned.paramName_l(2)
					eb.getEntityAs[ShakerSpec](programName)
			}
			labwareName = planned.paramName_l(3)
			labware <- eb.getEntityAs[Labware](labwareName)
			siteName = planned.paramName_l(5)
			site <- eb.getEntityAs[Site](siteName)
		} yield {
			List(Instruction(agent, ShakerRun(
				device,
				program,
				List((labware, site))
			)))
		}
	}
}
