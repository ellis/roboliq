/*package roboliq.commands

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
import roboliq.entities.LiquidSource
import roboliq.entities.PipetteDestination
import roboliq.entities.PipetteAmount


case class QualityControl1ActionParams(
	agent_? : Option[String],
	device_? : Option[String],
	source: LiquidSource,
	destination: PipetteDestination,
	tip: List[Int],
	amount: List[PipetteAmount],
	replicates_? : Option[Int]
)

class QualityControl1ActionHandler extends ActionHandler {
	def getActionName = "qualityControl1"

	def getActionParamNames = List("agent", "device", "source", "destination", "tip", "amount", "replicates")
	
	def getOperatorInfo(
		id: List[Int],
		paramToJsval_l: List[(String, JsValue)],
		eb: roboliq.entities.EntityBase,
		state0: WorldState
	): RqResult[OperatorInfo] = {
		for {
			params <- Converter.convActionAs[QualityControl1ActionParams](paramToJsval_l, eb, state0)
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

			OperatorInfo(id, Nil, Nil, s"qualityControl1_$n", binding, paramToJsval_l.toMap)
		}
	}
}
*/