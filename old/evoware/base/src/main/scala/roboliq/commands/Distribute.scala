package roboliq.commands

import scala.reflect.runtime.universe
import roboliq.ai.strips
import roboliq.ai.strips._
import roboliq.ai.plan.Unique
import grizzled.slf4j.Logger
import roboliq.core.RqResult
import roboliq.entities.Agent
import roboliq.entities.CleanIntensity
import roboliq.entities.LiquidSource
import roboliq.entities.PipetteAmount
import roboliq.entities.PipetteDestinations
import roboliq.entities.Pipetter
import roboliq.entities.TipModel
import roboliq.entities.WorldState
import roboliq.input.AgentInstruction
import roboliq.input.Converter
import roboliq.plan.ActionHandler
import roboliq.plan.OperatorHandler
import roboliq.plan.OperatorInfo
import spray.json.JsString
import spray.json.JsValue
import roboliq.entities.PipetteSources
import roboliq.input.Context


case class DistributeActionParams(
	agent_? : Option[String],
	device_? : Option[String],
	source: LiquidSource,
	destination: PipetteDestinations,
	amount: List[PipetteAmount],
	clean_? : Option[CleanIntensity.Value],
	cleanBegin_? : Option[CleanIntensity.Value],
	cleanBetween_? : Option[CleanIntensity.Value],
	cleanBetweenSameSource_? : Option[CleanIntensity.Value],
	cleanEnd_? : Option[CleanIntensity.Value],
	pipettePolicy_? : Option[String],
	tipModel_? : Option[TipModel],
	tip_? : Option[Int]
)

class DistributeActionHandler extends ActionHandler {

	def getActionName = "distribute"

	def getActionParamNames = List("agent", "device", "source", "destination", "amount", "clean", "cleanBegin", "cleanBetween", "cleanBetweenSameSource", "cleanEnd", "pipettePolicy", "tipModel", "tip")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: roboliq.entities.EntityBase,
		state0: WorldState
	): RqResult[List[OperatorInfo]] = {
		for {
			params <- Converter.convActionAs[DistributeActionParams](paramToJsval_l, eb, state0)
		} yield {
			val sourceLabware_l = params.source.l.map(_.labwareName)
			val destinationLabware_l = params.destination.l.map(_.labwareName)
			val labwareIdent_l = (sourceLabware_l ++ destinationLabware_l).distinct
			val n = labwareIdent_l.size

			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_l = {
				"?agent" -> m.getOrElse("agent", "?agent") ::
				"?device" -> m.getOrElse("device", "?device") ::
				labwareIdent_l.zipWithIndex.map(pair => s"?labware${pair._2 + 1}" -> s"${pair._1}")
			}
			val binding = binding_l.toMap

			OperatorInfo(id, Nil, Nil, s"distribute$n", binding, paramToJsval_l.toMap) :: Nil
		}
	}
}


class DistributeOperatorHandler(n: Int) extends OperatorHandler {
	private val logger = Logger[this.type]
	
	def getDomainOperator: strips.Operator = {
		val name = s"distribute$n"
		val paramName_l = "?agent" :: "?device" :: (1 to n).flatMap(i => List(s"?labware$i", s"?model$i", s"?site$i", s"?siteModel$i")).toList
		val paramTyp_l = "agent" :: "pipetter" :: List.fill(n)(List("labware", "model", "site", "siteModel")).flatten
		val preconds =
			strips.Literal(true, "agent-has-device", "?agent", "?device") ::
			strips.Literal(strips.Atom("ne", (1 to n).map(i => s"?site$i")), true) ::
			(1 to n).flatMap(i => List(
				strips.Literal(true, "device-can-site", "?device", s"?site$i"),
				strips.Literal(true, "model", s"?labware$i", s"?model$i"),
				strips.Literal(true, "location", s"?labware$i", s"?site$i"),
				strips.Literal(true, "model", s"?site$i", s"?siteModel$i"),
				strips.Literal(true, "stackable", s"?siteModel$i", s"?model$i")
			)).toList

		strips.Operator(
			name = name,
			paramName_l = paramName_l,
			paramTyp_l = paramTyp_l,
			preconds = strips.Literals(Unique(preconds : _*)),
			effects = roboliq.ai.strips.Literals.empty
		)
	}
	
	def getInstruction(
		operator: strips.Operator,
		instructionParam_m: Map[String, JsValue]
	): Context[Unit] = {
		for {
			agent <- Context.getEntityAs[Agent](operator.paramName_l(0))
			pipetter <- Context.getEntityAs[Pipetter](operator.paramName_l(1))
			params <- Converter.convInstructionParamsAs[DistributeActionParams](instructionParam_m)
			pipetteActionParams = PipetteActionParams(
				source_? = Some(PipetteSources(List(params.source))),
				destination_? = Some(params.destination),
				amount = params.amount,
				clean_? = params.clean_?,
				cleanBegin_? = params.cleanBegin_?,
				cleanBetween_? = params.cleanBetween_?,
				cleanBetweenSameSource_? = params.cleanBetweenSameSource_?,
				cleanEnd_? = params.cleanEnd_?,
				pipettePolicy_? = params.pipettePolicy_?,
				tipModel_? = params.tipModel_?,
				tip_? = params.tip_?,
				steps = Nil
			)
			_ <- new PipetteMethod().run(agent, pipetter, pipetteActionParams)
		} yield ()
	}
}
