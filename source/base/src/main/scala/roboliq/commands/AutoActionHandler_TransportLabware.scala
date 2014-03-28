package roboliq.commands

import scala.Option.option2Iterable
import aiplan.strips2.Strips
import aiplan.strips2.Unique
import roboliq.core._
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
import roboliq.plan.AutoActionHandler
import roboliq.entities.LabwareModel


class AutoActionHandler_TransportLabware extends AutoActionHandler {
	def getName = "transportLabware"
	
	def getDomainOperator: Strips.Operator = {
		Strips.Operator(
			name = "transportLabware",
			paramName_l = List("?labware", "?model", "?site1", "?site2", "?siteModel2"),
			paramTyp_l = List("labware", "model", "site", "site", "siteModel"),
			preconds = Strips.Literals(Unique(
				Strips.Literal(true, "location", "?labware", "?site1"),
				Strips.Literal(true, "model", "?labware", "?model"),
				Strips.Literal(true, "model", "?site2", "?siteModel2"),
				Strips.Literal(true, "stackable", "?siteModel2", "?model"),
				Strips.Literal(false, "site-blocked", "?site2")
			)),
			effects = Strips.Literals(Unique(
				Strips.Literal(false, "location", "?labware", "?site1"),
				Strips.Literal(false, "site-blocked", "?site1"),
				Strips.Literal(true, "location", "?labware", "?site2"),
				Strips.Literal(true, "site-blocked", "?site2")
			))
		)
	}
	
	def getInstruction(
		planned: Strips.Operator,
		eb: roboliq.entities.EntityBase
	): RqResult[List[roboliq.input.commands.Command]] = {
		val g = eb.transportGraph
		val List(labwareName, modelName, site1Name, site2Name) = planned.paramName_l
		for {
			labware <- eb.getEntityAs[Labware](labwareName)
			model <- eb.getEntityAs[LabwareModel](modelName)
			site1 <- eb.getEntityAs[Site](site1Name)
			site2 <- eb.getEntityAs[Site](site2Name)
			node1 <- g.find(site1).asRs(s"Site `$site1Name` is not in transport graph")
			node2 <- g.find(site2).asRs(s"Site `$site2Name` is not in transport graph")
			path <- node1.shortestPathTo(node2).asRs(s"No path in transport graph from `$site1Name` to `$site2Name`")
		} yield {
			println("path: "+path)
		}
		RqError("Not implemented yet")
	}
}
