package roboliq.commands

import scala.collection.immutable.SortedSet
import scala.reflect.runtime.universe
import scala.runtime.ZippedTraversable3.zippedTraversable3ToTraversable
import aiplan.strips2.Strips
import aiplan.strips2.Strips._
import aiplan.strips2.Unique
import grizzled.slf4j.Logger
import roboliq.core.RqError
import roboliq.core.RqResult
import roboliq.core.RqSuccess
import roboliq.entities.Agent
import roboliq.entities.CleanIntensity
import roboliq.entities.EntityBase
import roboliq.entities.LiquidVolume
import roboliq.entities.Mixture
import roboliq.entities.PipetteDestinations
import roboliq.entities.PipettePolicy
import roboliq.entities.PipettePosition
import roboliq.entities.PipetteSources
import roboliq.entities.Pipetter
import roboliq.entities.TipHandlingOverrides
import roboliq.entities.TipModel
import roboliq.entities.TipWellVolumePolicy
import roboliq.entities.WellIdentParser
import roboliq.entities.WorldState
import roboliq.input.Converter
import roboliq.input.commands.PipetteSpec
import roboliq.input.commands.PipetterAspirate
import roboliq.input.commands.PipetterDispense
import roboliq.input.commands.PipetterTipsRefresh
import roboliq.input.commands.PlanPath
import roboliq.pipette.planners.PipetteDevice
import roboliq.pipette.planners.PipetteHelper
import roboliq.pipette.planners.TipModelSearcher0
import roboliq.pipette.planners.TransferPlanner.Item
import roboliq.pipette.planners.TransferSimplestPlanner
import roboliq.plan.ActionHandler
import roboliq.plan.AgentInstruction
import roboliq.plan.OperatorHandler
import roboliq.plan.OperatorInfo
import spray.json.JsString
import spray.json.JsValue
import roboliq.entities.WellInfo
import roboliq.entities.PipetteDestination
import roboliq.entities.LiquidSource
import roboliq.method.PipetteMethod


case class TransferActionParams(
	agent_? : Option[String],
	device_? : Option[String],
	source_? : Option[PipetteSources],
	destination_? : Option[PipetteDestinations],
	steps: List[TransferStepParams]
)

class TransferActionHandler extends ActionHandler {

	def getActionName = "transfer"

	def getActionParamNames = List("agent", "device", "source", "destination", "volume", "steps", "clean", "cleanBefore", "cleanBetween", "cleanAfter", "tipModel", "pipettePolicy")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: roboliq.entities.EntityBase,
		state0: WorldState
	): RqResult[OperatorInfo] = {
		for {
			params <- Converter.convActionAs[TransferActionParams](paramToJsval_l, eb, state0)
		} yield {
			val sourceLabware_l = params.source_?.getOrElse(PipetteSources(Nil)).sources.flatMap(_.l.map(_.labwareName)) ++ params.steps.flatMap(_.s_?.map(_.l.map(_.labwareName)).getOrElse(Nil))
			val destinationLabware_l = params.destination_?.getOrElse(PipetteDestinations(Nil)).l.map(_.labwareName) ++ params.steps.map(_.d_?.map(_.wellInfo.labwareName).getOrElse(Nil))
			val labwareIdent_l = (sourceLabware_l ++ destinationLabware_l).distinct
			val n = labwareIdent_l.size

			val m = paramToJsval_l.collect({case (name, JsString(s)) => (name, s)}).toMap
			val binding_l = {
				"?agent" -> m.getOrElse("agent", "?agent") ::
				"?device" -> m.getOrElse("device", "?device") ::
				labwareIdent_l.zipWithIndex.map(pair => s"?labware${pair._2 + 1}" -> s"${pair._1}")
			}
			val binding = binding_l.toMap

			OperatorInfo(id, Nil, Nil, s"transfer$n", binding, paramToJsval_l.toMap)
		}
	}
}
/*
 * steps:
 * - {s: plate(A01), d: plate(A02), v: 20ul
 */

case class TransferInstructionParams(
	agent_? : Option[String],
	device_? : Option[String],
	source_? : Option[PipetteSources],
	destination_? : Option[PipetteDestinations],
	volume : List[LiquidVolume],
	steps: List[TransferStepParams],
	contact_? : Option[PipettePosition.Value],
	clean_? : Option[CleanIntensity.Value],
	cleanBefore_? : Option[CleanIntensity.Value],
	cleanBetween_? : Option[CleanIntensity.Value],
	cleanAfter_? : Option[CleanIntensity.Value],
	tipModel_? : Option[TipModel],
	pipettePolicy_? : Option[String]
)

case class TransferStepParams(
	s_? : Option[LiquidSource],
	d_? : Option[PipetteDestination],
	v_? : Option[LiquidVolume]/*,
	pipettePolicy_? : Option[String],
	cleanBefore_? : Option[CleanIntensity.Value],
	cleanBetween_? : Option[CleanIntensity.Value],
	cleanAfter_? : Option[CleanIntensity.Value],
	tipModel_? : Option[TipModel]*/
)

class TransferOperatorHandler(n: Int) extends OperatorHandler {
	private val logger = Logger[this.type]
	
	def getDomainOperator: Strips.Operator = {
		val name = s"transfer$n"
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
			params <- Converter.convInstructionAs[TransferInstructionParams](instructionParam_m, eb, state0)
			spec <- getPipetteSpec(params)
			action_l <- new PipetteMethod().run(eb, state0, spec, device)
		} yield {
			action_l.map(x => AgentInstruction(agent, x))
		}
	}
	
	private def getPipetteSpec(params: TransferInstructionParams): RqResult[PipetteSpec] = {
		for {
			destination_l <- getDestinations(params)
			source_l <- getSources(params)
			volume_l <- getVolumes(params)
			// TODO: Add warning if no destinations and sources are not provided
		} yield {
			PipetteSpec(
				PipetteSources(source_l),
				PipetteDestinations(destination_l),
				volume_l,
				params.pipettePolicy_?,
				params.clean_?,
				params.cleanBefore_?,
				params.cleanBetween_?,
				params.cleanAfter_?,
				params.tipModel_?
			)
		}
	}

	/**
	 * Get list of WellInfo destinations
	 */
	private def getDestinations(params: TransferInstructionParams): RqResult[List[WellInfo]] = {
		(params.destination_?, params.steps) match {
			case (None, Nil) => RqSuccess(Nil)
			case (None, step_l) =>
				RqResult.mapFirst(step_l.zipWithIndex) { case (step, i) =>
					RqResult.from(step.d_?.map(_.wellInfo), s"missing destination in step ${i+1}")
				}
			case (Some(destinations), Nil) => RqSuccess(destinations.l)
			case (Some(destinations), step_l) =>
				destinations.l match {
					case Nil =>
						RqResult.mapFirst(step_l.zipWithIndex) { case (step, i) =>
							RqResult.from(step.d_?.map(_.wellInfo), s"missing destination in step ${i+1}")
						}
					case one :: Nil =>
						RqSuccess(step_l.map(_.d_?.map(_.wellInfo).getOrElse(one)))
					case l =>
						for {
							_ <- RqResult.assert(l.size == step_l.size, "`destination` must either contain a single destination or a list the same length as `steps`")
						} yield (l zip step_l).map(pair => pair._2.d_?.map(_.wellInfo).getOrElse(pair._1))
				}
		}
	}
	
	/**
	 * Get list of LiquidSource
	 */
	private def getSources(params: TransferInstructionParams): RqResult[List[LiquidSource]] = {
		(params.source_?, params.steps) match {
			case (None, Nil) => RqSuccess(Nil)
			case (None, step_l) =>
				RqResult.mapFirst(step_l.zipWithIndex) { case (step, i) =>
					RqResult.from(step.s_?, s"missing source in step ${i+1}")
				}
			case (Some(sources), Nil) => RqSuccess(sources.sources)
			case (Some(sources), step_l) =>
				sources.sources match {
					case Nil =>
						RqResult.mapFirst(step_l.zipWithIndex) { case (step, i) =>
							RqResult.from(step.s_?, s"missing source in step ${i+1}")
						}
					case one :: Nil =>
						RqSuccess(step_l.map(_.s_?.getOrElse(one)))
					case l =>
						for {
							_ <- RqResult.assert(l.size == step_l.size, "`source` must either contain a single source or a list the same length as `steps`")
						} yield (l zip step_l).map(pair => pair._2.s_?.getOrElse(pair._1))
				}
		}
	}
	
	/**
	 * Get list of LiquidVolume
	 */
	private def getVolumes(params: TransferInstructionParams): RqResult[List[LiquidVolume]] = {
		(params.volume, params.steps) match {
			case (Nil, Nil) => RqSuccess(Nil)
			case (Nil, step_l) =>
				RqResult.mapFirst(step_l.zipWithIndex) { case (step, i) =>
					RqResult.from(step.v_?, s"missing volume in step ${i+1}")
				}
			case (volume_l, Nil) => RqSuccess(volume_l)
			case (volume_l, step_l) =>
				volume_l match {
					case Nil =>
						RqResult.mapFirst(step_l.zipWithIndex) { case (step, i) =>
							RqResult.from(step.v_?, s"missing volume in step ${i+1}")
						}
					case one :: Nil =>
						RqSuccess(step_l.map(_.v_?.getOrElse(one)))
					case l =>
						for {
							_ <- RqResult.assert(l.size == step_l.size, "`volume` must either contain a single source or a list the same length as `steps`")
						} yield (l zip step_l).map(pair => pair._2.v_?.getOrElse(pair._1))
				}
		}
	}
	
}
