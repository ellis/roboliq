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
import spray.json.JsNull
import roboliq.input.Instruction
import scalax.collection.Graph
import scalax.collection.edge.LkUnDiEdge


case class GetLabwareLocationActionParams(
	`object`: Labware,
	site: Site
)

class GetLabwareLocationActionHandler extends ActionHandler {
	
	def getActionName = "getLabwareLocation"

	def getActionParamNames = List("object", "site")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[GetLabwareLocationActionParams](paramToJsval_l, eb, state0)
			labwareName <- eb.getIdent(params.`object`)
			siteName <- eb.getIdent(params.site)
		} yield {
			val suffix = id.mkString("__", "_", "")
			
			// Bindings for transfer to sealer
			val binding_m = Map[String, String](
				"?labware" -> labwareName,
				"?site" -> siteName
			)

			OperatorInfo(id, Nil, Nil, "getLabwareLocation", binding_m, paramToJsval_l.toMap) :: Nil
		}
	}
}

class GetLabwareLocationOperatorHandler extends OperatorHandler {
	def getDomainOperator: Strips.Operator = {
		Strips.Operator(
			name = "getLabwareLocation",
			paramName_l = List("?labware", "?model", "?site", "?siteModel"),
			paramTyp_l = List("labware", "model", "site", "siteModel"),
			preconds = Strips.Literals(Unique(
				Strips.Literal(true, "location", "?labware", "?site"),
				Strips.Literal(true, "model", "?labware", "?model"),
				Strips.Literal(true, "model", "?site", "?siteModel")
			)),
			effects = Strips.Literals.empty
		)
	}
	
	def getInstruction(
		operator: Strips.Operator,
		instructionParam_m: Map[String, JsValue]
	): Context[Unit] = {
		Context.unit(())
	}
}
