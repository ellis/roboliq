package roboliq.commands

import scala.reflect.runtime.universe

import aiplan.strips2.Strips
import aiplan.strips2.Strips._
import aiplan.strips2.Unique
import grizzled.slf4j.Logger
import roboliq.core.RqResult
import roboliq.entities.Agent
import roboliq.entities.CleanIntensity
import roboliq.entities.LiquidVolume
import roboliq.entities.PipetteDestinations
import roboliq.entities.PipettePosition
import roboliq.entities.PipetteSources
import roboliq.entities.Pipetter
import roboliq.entities.TipModel
import roboliq.entities.WorldState
import roboliq.input.Converter
import roboliq.input.commands.PipetteSpec
import roboliq.method.PipetteMethod
import roboliq.plan.ActionHandler
import roboliq.plan.AgentInstruction
import roboliq.plan.OperatorHandler
import roboliq.plan.OperatorInfo
import spray.json.JsString
import spray.json.JsValue


case class DistributeActionParams(
	agent_? : Option[String],
	device_? : Option[String],
	source: String,
	destination: String
)

class DistributeActionHandler extends ActionHandler {

	def getActionName = "distribute"

	def getActionParamNames = List("agent", "device", "source", "destination", "volume", "clean", "cleanBefore", "cleanBetween", "cleanAfter", "tipModel", "pipettePolicy")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: roboliq.entities.EntityBase,
		state0: WorldState
	): RqResult[OperatorInfo] = {
		for {
			params <- Converter.convActionAs[DistributeActionParams](paramToJsval_l, eb, state0)
			sources <- eb.lookupLiquidSources(params.source, state0)
			destinations <- eb.lookupLiquidDestinations(params.destination, state0)
		} yield {
			val sourceLabware_l = sources.sources.flatMap(_.l.map(_.labwareName))
			val destinationLabware_l = destinations.l.map(_.labwareName)
			val labwareIdent_l = (sourceLabware_l ++ destinationLabware_l).distinct
			val n = labwareIdent_l.size

			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_l = {
				"?agent" -> m.getOrElse("agent", "?agent") ::
				"?device" -> m.getOrElse("device", "?device") ::
				labwareIdent_l.zipWithIndex.map(pair => s"?labware${pair._2 + 1}" -> s"${pair._1}")
			}
			val binding = binding_l.toMap

			OperatorInfo(id, Nil, Nil, s"distribute$n", binding, paramToJsval_l.toMap)
		}
	}
}


case class DistributeInstructionParams(
	agent_? : Option[String],
	device_? : Option[String],
	source: PipetteSources,
	destination: PipetteDestinations,
	volume: List[LiquidVolume],
	contact_? : Option[PipettePosition.Value],
	clean_? : Option[CleanIntensity.Value],
	cleanBefore_? : Option[CleanIntensity.Value],
	cleanBetween_? : Option[CleanIntensity.Value],
	cleanAfter_? : Option[CleanIntensity.Value],
	tipModel_? : Option[TipModel],
	pipettePolicy_? : Option[String]
)

class DistributeOperatorHandler(n: Int) extends OperatorHandler {
	private val logger = Logger[this.type]
	
	def getDomainOperator: Strips.Operator = {
		val name = s"distribute$n"
		val paramName_l = "?agent" :: "?device" :: (1 to n).flatMap(i => List(s"?labware$i", s"?model$i", s"?site$i", s"?siteModel$i")).toList
		val paramTyp_l = "agent" :: "pipetter" :: List.fill(n)(List("labware", "model", "site", "siteModel")).flatten
		val preconds =
			Strips.Literal(true, "agent-has-device", "?agent", "?device") ::
			Strips.Literal(Strips.Atom("ne", (1 to n).map(i => s"?site$i")), true) ::
			(1 to n).flatMap(i => List(
				Strips.Literal(true, "device-can-site", "?device", s"?site$i"),
				Strips.Literal(true, "model", s"?labware$i", s"?model$i"),
				Strips.Literal(true, "location", s"?labware$i", s"?site$i"),
				Strips.Literal(true, "model", s"?site$i", s"?siteModel$i"),
				Strips.Literal(true, "stackable", s"?siteModel$i", s"?model$i")
			)).toList

		Strips.Operator(
			name = name,
			paramName_l = paramName_l,
			paramTyp_l = paramTyp_l,
			preconds = Strips.Literals(Unique(preconds : _*)),
			effects = aiplan.strips2.Strips.Literals.empty
		)
	}
	
	def getInstruction(
		operator: Strips.Operator,
		instructionParam_m: Map[String, JsValue],
		eb: roboliq.entities.EntityBase,
		state0: WorldState
	): RqResult[List[AgentInstruction]] = {
		for {
			agent <- eb.getEntityAs[Agent](operator.paramName_l(0))
			device <- eb.getEntityAs[Pipetter](operator.paramName_l(1))
			params <- Converter.convInstructionAs[DistributeInstructionParams](instructionParam_m, eb, state0)
			spec = PipetteSpec(
				params.source,
				params.destination,
				params.volume,
				params.pipettePolicy_?,
				params.clean_?,
				params.cleanBefore_?,
				params.cleanBetween_?,
				params.cleanAfter_?,
				params.tipModel_?
			)
			action_l <- new PipetteMethod().run(eb, state0, spec, device)
		} yield {
			action_l.map(x => AgentInstruction(agent, x))
		}
	}
}
