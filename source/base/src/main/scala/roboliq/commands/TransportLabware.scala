package roboliq.commands

import scala.Option.option2Iterable
import aiplan.strips2.Strips
import aiplan.strips2.Unique
import roboliq.core.RqResult
import roboliq.core.RqSuccess
import roboliq.entities.Agent
import roboliq.entities.EntityBase
import roboliq.entities.Labware
import roboliq.entities.LabwareModel
import roboliq.entities.Shaker
import roboliq.entities.ShakerSpec
import roboliq.entities.Site
import roboliq.entities.WorldState
import roboliq.input.AgentInstruction
import roboliq.input.Context
import roboliq.plan.OperatorHandler
import spray.json.JsValue
import roboliq.plan.OperatorInfo
import roboliq.plan.ActionHandler
import roboliq.input.Converter


case class TransportLabwareActionParams(
	agent_? : Option[String],
	device_? : Option[String],
	`object`: Labware,
	site: Site
)

class TransportLabwareActionHandler extends ActionHandler {
	
	def getActionName = "transportLabware"

	def getActionParamNames = List("agent", "device", "object", "site")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[TransportLabwareActionParams](paramToJsval_l, eb, state0)
			labwareName <- eb.getIdent(params.`object`)
			site2Name <- eb.getIdent(params.site)
		} yield {
			val suffix = id.mkString("__", "_", "")
			
			// Bindings for transfer to sealer
			val binding_m = Map[String, String](
				"?labware" -> labwareName,
				"?site2" -> site2Name
			)

			OperatorInfo(id, Nil, Nil, "transportLabware", binding_m, Map()) :: Nil
		}
	}
}

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
				Strips.Literal(false, "site-blocked", "?site2"),
				Strips.Literal(false, "site-closed", "?site1"),
				Strips.Literal(false, "site-closed", "?site2")
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
		instructionParam_m: Map[String, JsValue]
	): Context[Unit] = {
		val List(labwareName, modelName, site1Name, site2Name, _) = operator.paramName_l
		
		for {
			data0 <- Context.get
			g = data0.eb.transportGraph
			labware <- Context.getEntityAs[Labware](labwareName)
			model <- Context.getEntityAs[LabwareModel](modelName)
			site1 <- Context.getEntityAs[Site](site1Name)
			site2 <- Context.getEntityAs[Site](site2Name)
			node1 <- Context.from(g.find(site1), s"Site `$site1Name` is not in transport graph")
			node2 <- Context.from(g.find(site2), s"Site `$site2Name` is not in transport graph")
			path <- Context.from(node1.shortestPathTo(node2), s"No path in transport graph from `$site1Name` to `$site2Name`")
			//_ = println("path: "+path.edges)
			_ <- Context.foreachFirst(path.nodes.toList zip path.edges.toList) { pair =>
				val (node1, edge) = pair
				val site1 = node1.value
				val site2 = if (site1 == edge._1.value) edge._2.value else edge._1.value
				//println(s"Move from $site1 to $site2")
				edge.label match {
					case (agentName: String, deviceName: String, programName: String) =>
						for {
							agent <- Context.getEntityAs[Agent](agentName)
							_ <- Context.addInstruction(agent, TransporterRun(deviceName, labware, model, site1, site2, programName))
						} yield ()
					case x =>
						Context.error("unrecognized transport edge label: "+edge.label)
				}
			}
		} yield ()
	}
}
