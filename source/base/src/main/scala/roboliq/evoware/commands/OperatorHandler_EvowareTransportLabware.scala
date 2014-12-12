package roboliq.evoware.commands

import scala.Option.option2Iterable
import roboliq.ai.strips
import roboliq.ai.plan.Unique
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
import roboliq.input.Converter
import roboliq.commands.TransporterRun


private case class EvowareTransportLabwareExtraParams(
	agent_? : Option[String],
	roma_? : Option[Int],
	vector_? : Option[String]
)

class OperatorHandler_EvowareTransportLabware extends OperatorHandler {
	def getDomainOperator: strips.Operator = {
		strips.Operator(
			name = "evoware.transportLabware",
			paramName_l = List("?labware", "?model", "?site1", "?site2", "?siteModel2"),
			paramTyp_l = List("labware", "model", "site", "site", "siteModel"),
			preconds = strips.Literals(Unique(
				strips.Literal(true, "location", "?labware", "?site1"),
				strips.Literal(true, "model", "?labware", "?model"),
				strips.Literal(true, "model", "?site2", "?siteModel2"),
				strips.Literal(true, "stackable", "?siteModel2", "?model"),
				strips.Literal(false, "site-blocked", "?site2"),
				strips.Literal(false, "site-closed", "?site1"),
				strips.Literal(false, "site-closed", "?site2")
			)),
			effects = strips.Literals(Unique(
				strips.Literal(false, "location", "?labware", "?site1"),
				strips.Literal(false, "site-blocked", "?site1"),
				strips.Literal(true, "location", "?labware", "?site2"),
				strips.Literal(true, "site-blocked", "?site2")
			))
		)
	}
	
	def getInstruction(
		operator: strips.Operator,
		instructionParam_m: Map[String, JsValue]
	): Context[Unit] = {
		val List(labwareName, modelName, site1Name, site2Name, _) = operator.paramName_l
		
		if (site1Name == site2Name)
			return Context.unit(())
			
		for {
			data0 <- Context.get
			params <- Converter.convInstructionParamsAs[EvowareTransportLabwareExtraParams](instructionParam_m)
			g = {
				val g0 = data0.eb.transportGraph
				val g1 = g0.filter(g0.having(edge = _.label match {
					case (agentName: String, deviceName: String, programName: String) =>
						val agentOk = (params.agent_?.isEmpty || params.agent_? == Some(agentName))
						val romaOk = (params.roma_?.isEmpty || params.roma_?.map("mario__transporter"+_) == Some(deviceName))
						val vectorOk = (params.vector_?.isEmpty || params.vector_? == Some(agentName))
						agentOk && romaOk && vectorOk
				}))
				//val g1 = params.agent_?.map(agentName => g0.filter(g0.having(edge = (e) => { _.label._1 == agentName } ))).getOrElse(g0)
				g1
			}
			labware <- Context.getEntityAs[Labware](labwareName)
			model <- Context.getEntityAs[LabwareModel](modelName)
			site1 <- Context.getEntityAs[Site](site1Name)
			site2 <- Context.getEntityAs[Site](site2Name)
			constraintInfo = s"for agent `${params.agent_?.getOrElse("any")}`, roma `${params.roma_?.getOrElse("any")}`, vector `${params.vector_?.getOrElse("any")}`"
			node1 <- Context.from(g.find(site1), s"Origin site `$site1Name` is not in transport graph ${constraintInfo}")
			node2 <- Context.from(g.find(site2), s"Destination site `$site2Name` is not in transport graph ${constraintInfo}")
			path <- Context.from(node1.shortestPathTo(node2), s"No path in transport graph from `$site1Name` to `$site2Name` ${constraintInfo}")
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
