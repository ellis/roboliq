package roboliq.commands

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
import roboliq.plan.OperatorInfo
import roboliq.plan.ActionHandler
import roboliq.input.Converter
import spray.json.JsNull
import roboliq.input.Instruction
import scalax.collection.Graph
import scalax.collection.edge.LkUnDiEdge


case class TransportLabwareActionParams(
	agent_? : Option[String],
	device_? : Option[String],
	program_? : Option[String],
	`object`: Labware,
	site: Site
)

class TransportLabwareActionHandler extends ActionHandler {
	
	def getActionName = "transportLabware"

	def getActionParamNames = List("agent", "device", "program", "object", "site")
	
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

			OperatorInfo(id, Nil, Nil, "transportLabware", binding_m, paramToJsval_l.toMap) :: Nil
		}
	}
}

private case class TransportLabwareOperatorParams(
	agent_? : Option[String],
	device_? : Option[String],
	program_? : Option[String]
)

class OperatorHandler_TransportLabware extends OperatorHandler {
	def getDomainOperator: strips.Operator = {
		strips.Operator(
			name = "transportLabware",
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
			_ <- Context.or(
				getInstructionSub(operator, instructionParam_m, labwareName, modelName, site1Name, site2Name, data0.eb.transportGraph),
				getInstructionSub(operator, instructionParam_m, labwareName, modelName, site1Name, site2Name, data0.eb.transportUserGraph)
			)
		} yield ()
	}
	
	private def getInstructionSub(
		operator: strips.Operator,
		instructionParam_m: Map[String, JsValue],
		labwareName: String,
		modelName: String,
		site1Name: String,
		site2Name: String,
		g0: Graph[Site, LkUnDiEdge]
	): Context[Unit] = {
		for {
			params <- Converter.convInstructionParamsAs[TransportLabwareOperatorParams](instructionParam_m)
			data0 <- Context.get
			g = g0.filter(g0.having(edge = _.label match {
				case (agentName: String, deviceName: String, programName: String) =>
					val agentOk = (params.agent_?.isEmpty || params.agent_? == Some(agentName))
					val deviceOk = (params.device_?.isEmpty || params.device_? == Some(deviceName))
					val programOk = (params.program_?.isEmpty || params.program_? == Some(programName))
					agentOk && deviceOk && programOk
			}))
			labware <- Context.getEntityAs[Labware](labwareName)
			model <- Context.getEntityAs[LabwareModel](modelName)
			site1 <- Context.getEntityAs[Site](site1Name)
			site2 <- Context.getEntityAs[Site](site2Name)
			constraintInfo = s"for agent `${params.agent_?.getOrElse("any")}`, device `${params.device_?.getOrElse("any")}`, program `${params.program_?.getOrElse("any")}`"
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
