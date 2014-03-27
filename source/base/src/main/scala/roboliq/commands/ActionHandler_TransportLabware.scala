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


class ActionHandler_TransportLabware extends ActionHandler {
	def getSignature: Strips.Signature = Strips.Signature(
		"transportLabware", "object" -> "labware", "destination" -> "site"
	)
	
	def getActionPlanInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)]
	): RqResult[ActionPlanInfo] = {
		val domainOperator = Strips.Operator(
			name = "transportLabware",
			paramName_l = List("?labware", "?model", "?site1", "?site2"),
			paramTyp_l = List("labware", "model", "site", "site"),
			preconds = Strips.Literals(Unique(
				Strips.Literal(true, "location", "?labware", "?site1"),
				Strips.Literal(true, "model", "?labware", "?model"),
				Strips.Literal(true, "site-can-model", "?site2", "?model"),
				Strips.Literal(true, "transport-possible", "?model", "?site1", "?site2")
			)),
			effects = Strips.Literals(Unique(
				Strips.Literal(false, "location", "?labware", "?site1"),
				Strips.Literal(true, "location", "?labware", "?site2")
			))
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
		
		// TODO: require object and destination, otherwise the action doesn't make sense
		// TODO: possibly lookup labwareModel of labware
		val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
		val binding = Map(
			"?labware" -> m.getOrElse("object", "?labware"),
			"?site2" -> m.getOrElse("destination", "?site")
		)
		
		val planAction = domainOperator.bind(binding)
		
		println(s"getActionPlanInfo(${id}, ${paramToJsval_l}):")
		println(m0)
		println(programName, programObject_?)
		println(m)
		println(binding)
		
		RqSuccess(ActionPlanInfo(id, paramToJsval_l, domainOperator, problemObjectToTyp_l, Nil, planAction))
	}
	
	def getOperator(
		planInfo: ActionPlanInfo,
		planned: Strips.Operator,
		eb: EntityBase
	): RqResult[List[Command]] = {
		val m0 = planInfo.paramToJsval_l.toMap

		for {
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
			List(ShakerRun(
				device,
				program,
				List((labware, site))
			))
		}
	}
}
