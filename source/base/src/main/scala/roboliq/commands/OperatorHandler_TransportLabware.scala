package roboliq.commands

import scala.Option.option2Iterable
import aiplan.strips2.Strips
import aiplan.strips2.Unique
import roboliq.core._
import roboliq.core.RqResult
import roboliq.core.RqSuccess
import roboliq.plan.OperatorHandler
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
import roboliq.entities.LabwareModel
import roboliq.input.commands.TransporterRun
import roboliq.plan.AgentInstruction
import roboliq.entities.Agent
import roboliq.entities.WorldState


class OperatorHandler_TransportLabware extends OperatorHandler {
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
		operator: Strips.Operator,
		instructionParam_m: Map[String, JsValue],
		eb: roboliq.entities.EntityBase,
		state0: WorldState
	): RqResult[List[AgentInstruction]] = {
		val g = eb.transportGraph
		val List(labwareName, modelName, site1Name, site2Name, _) = operator.paramName_l
		
		for {
			labware <- eb.getEntityAs[Labware](labwareName)
			model <- eb.getEntityAs[LabwareModel](modelName)
			site1 <- eb.getEntityAs[Site](site1Name)
			site2 <- eb.getEntityAs[Site](site2Name)
			node1 <- g.find(site1).asRs(s"Site `$site1Name` is not in transport graph")
			node2 <- g.find(site2).asRs(s"Site `$site2Name` is not in transport graph")
			path <- node1.shortestPathTo(node2).asRs(s"No path in transport graph from `$site1Name` to `$site2Name`")
			_ = println("path: "+path.edges)
			op_l <- RqResult.mapAll(path.nodes.toList zip path.edges.toList) { pair =>
				val (node1, edge) = pair
				val site1 = node1.value
				val site2 = if (site1 == edge._1.value) edge._2.value else edge._1.value
				println(s"Move from $site1 to $site2")
				edge.label match {
					case (agentName: String, deviceName: String, programName: String) =>
						for {
							agent <- eb.getEntityAs[Agent](agentName)
						} yield {
							AgentInstruction(agent, TransporterRun(deviceName, labware, model, site1, site2, programName))
						}
					case x =>
						RqError("unrecognized transport edge label: "+edge.label)
				}
			}
		} yield op_l
	}
}
