package roboliq.commands

import roboliq.entities.PipetteDestination
import roboliq.entities.LiquidSource
import roboliq.entities.PipetteAmount
import roboliq.entities.CleanIntensity
import roboliq.entities.TipModel
import roboliq.entities.LiquidVolume
import roboliq.entities.PipetteDestinations
import roboliq.entities.PipetteSources
import roboliq.entities.WellInfo
import roboliq.core.RsResult
import roboliq.entities.PipetteAmount_Volume
import roboliq.entities.PipetteAmount_Dilution
import roboliq.core.RsError
import roboliq.core.RsSuccess
import scala.annotation.tailrec
import roboliq.entities.Well
import roboliq.entities.WorldState
import roboliq.entities.Pipetter
import roboliq.pipette.planners.PipetteDevice
import roboliq.entities.EntityBase

case class PipetteActionParams(
	destination_? : Option[PipetteDestinations],
	source_? : Option[PipetteSources],
	amount: List[PipetteAmount],
	pipettePolicy_? : Option[String],
	clean_? : Option[CleanIntensity.Value],
	cleanBefore_? : Option[CleanIntensity.Value],
	cleanAfter_? : Option[CleanIntensity.Value],
	tipModel_? : Option[TipModel],
	tip_? : Option[Int],
	steps: List[PipetteStepParams]
)

case class PipetteStepParams(
	d_? : Option[PipetteDestination],
	s_? : Option[LiquidSource],
	a_? : Option[PipetteAmount],
	pipettePolicy_? : Option[String],
	clean_? : Option[CleanIntensity.Value],
	cleanBefore_? : Option[CleanIntensity.Value],
	cleanAfter_? : Option[CleanIntensity.Value],
	tipModel_? : Option[TipModel],
	tip_? : Option[Int]
)

sealed abstract trait StepA
sealed abstract trait StepB

case class StepA_Pipette(
	d: WellInfo,
	s: LiquidSource,
	v: LiquidVolume,
	tipModel_? : Option[TipModel],
	pipettePolicy_? : Option[String],
	cleanBefore_? : Option[CleanIntensity.Value],
	cleanAfter_? : Option[CleanIntensity.Value],
	tip_? : Option[Int]
) extends StepA

case class StepA_Clean(
	clean: CleanIntensity.Value
) extends StepA with StepB

case class StepB_Pipette(
	d: WellInfo,
	s: List[WellInfo],
	v: LiquidVolume,
	tipModel: TipModel,
	pipettePolicy: String,
	cleanBefore: CleanIntensity.Value,
	cleanAfter: CleanIntensity.Value,
	tips: List[Int]
) extends StepB

case class PipetteStepC(
	d: WellInfo,
	s: WellInfo,
	v: LiquidVolume,
	tipModel: TipModel,
	pipettePolicy: String,
	cleanBefore: CleanIntensity.Value,
	cleanAfter: CleanIntensity.Value,
	tip: Int
)

class Pipette {
	def paramsToA(params: PipetteActionParams): List[StepA] = {
		val dn = params.destination_?.map(_.l.size).getOrElse(0)
		val sn = params.source_?.map(_.sources.size).getOrElse(0)
		val an = params.amount.size
		val stepn = params.steps.size
		val n_l = List(dn, sn, an, stepn)
		val n = n_l.max

		val d_l: List[Option[WellInfo]] = params.destination_? match {
			case None => List.make(n, None)
			case Some(x) => x.l.map(Some(_))
		}
		val s_l: List[Option[LiquidSource]] = params.source_? match {
			case None => List.make(n, None)
			case Some(x) => x.sources.map(Some(_))
		}
		val a_l: List[Option[PipetteAmount]] = params.amount match {
			case Nil => List.make(n, None)
			case x => x.map(Some(_))
		}
		val step_l: List[Option[PipetteStepParams]] = params.steps match {
			case Nil => List.make(n, None)
			case x => x.map(Some(_))
		}
		
		val all_l: List[((Option[WellInfo], Option[LiquidSource], Option[PipetteAmount]), Option[PipetteStepParams])] =
			(d_l, s_l, a_l).zipped.toList zip step_l
		
		for {
			// TODO: construct better error messages
			_ <- RsResult.mapFirst(n_l){x => RsResult.assert(x == 0 || x == 1 || x == n, "`destination`, `source`, `amount`, and `steps` lists must have compatible sizes")}
			stepA_l <- sub(params, d_l, s_l, a_l, step_l, 0, Nil)
		} yield ()
		Nil
	}
	
	@tailrec
	private def sub(
		params: PipetteActionParams,
		d_l: List[Option[WellInfo]],
		s_l: List[Option[LiquidSource]],
		a_l: List[Option[PipetteAmount]],
		step_l: List[Option[PipetteStepParams]],
		step_i: Int,
		stepA_r: List[StepA]
	): RsResult[List[StepA]] = {
		step_l match {
			case Nil => RsSuccess(stepA_r.reverse)
			case step_? :: step_rest =>
				val stepA_? = for {
					d <- RsResult.from(step_?.flatMap(_.d_?.map(_.wellInfo)).orElse(d_l.head), s"destination must be specified for step ${step_i+1}")
					s <- RsResult.from(step_?.flatMap(_.s_?).orElse(s_l.head), s"source must be specified for step ${step_i+1}")
					a <- RsResult.from(step_?.flatMap(_.a_?).orElse(a_l.head), s"amount must be specified for step ${step_i+1}")
					v <- a match {
						case PipetteAmount_Volume(volume) => RsSuccess(volume)
						case _ => RsError("INTERNAL: don't yet handle non-volume amounts")
					}
				} yield {
					StepA_Pipette(
						d,
						s,
						v,
						step_?.flatMap(_.tipModel_?).orElse(params.tipModel_?),
						step_?.flatMap(_.pipettePolicy_?).orElse(params.pipettePolicy_?),
						step_?.flatMap(_.cleanBefore_?).orElse(step_?.flatMap(_.clean_?)).orElse(params.cleanBefore_?).orElse(params.clean_?),
						step_?.flatMap(_.cleanAfter_?).orElse(step_?.flatMap(_.clean_?)).orElse(params.cleanAfter_?).orElse(params.clean_?),
						step_?.flatMap(_.tip_?).orElse(params.tip_?)
					)
				}
				stepA_? match {
					case RsError(e, w) => RsError(e, w)
					case RsSuccess(stepA, _) =>
						sub(params, d_l.tail, s_l.tail, a_l.tail, step_l.tail, step_i + 1, stepA :: stepA_r)
				}
		}
	}
	
	def aToB(stepA_l: List[StepA]): RsResult[List[StepB]] = {
		/*
		 * - split the data as necessary, then for each split
		 * - for all pipette steps for which no tipModel is assigned, get the possible tipModels for that step
		 * - see if any tip model can be used for
		 *   - all steps
		 *   - all steps without explicit tipModel
		 *   - split steps by source, and try to assign tip model to each source
		 *   - for any remaining sources which couldn't be assigned a single tipModel, assign as best as possible
		 * - choose pipettePolicy
		 * - assign cleanBefore, cleanAfter, and tip set
		 */
		val stepA_ll = splitA(stepA_l)
		RsResult.mapAll(stepA_ll)(aToB2).map(_.flatten)
	}

	/**
	 * Split this list into multiple lists, whenever a prior destination well is used as a source well. 
	 */
	private def splitA(stepA_l: List[StepA]): List[List[StepA]] = {
		val i = splitIndex(stepA_l)
		if (i <= 0) {
			List(stepA_l)
		}
		else {
			val (left, right) = stepA_l.splitAt(i)
			left :: splitA(right)
		}
	}
	
	private def splitIndex(stepA_l: List[StepA]): Int = {
		var destSeen_l = Set[Well]()
		for ((stepA, i) <- stepA_l.zipWithIndex) {
			stepA match {
				case x: StepA_Pipette =>
					if (!x.s.l.map(_.well).toSet.intersect(destSeen_l).isEmpty)
						return i
					destSeen_l += x.d.well
			}
		}
		-1
	}
	
	/**
	 * - for all pipette steps for which no tipModel is assigned, get the possible tipModels for that step
	 * - see if any tip model can be used for
	 *   - all steps
	 *   - all steps without explicit tipModel
	 *   - split steps by source, and try to assign tip model to each source
	 *   - for any remaining sources which couldn't be assigned a single tipModel, assign as best as possible
	 * - choose pipettePolicy
	 * - assign cleanBefore, cleanAfter, and tip set
	 */
	def aToB2(
		stepA_l: List[StepA],
		eb: EntityBase,
		state0: WorldState,
		pipetter: Pipetter
	): RsResult[List[StepB]] = {
		val device = new PipetteDevice
		val tipAll_l = eb.pipetterToTips_m(pipetter)
		val step1_l = stepA_l.collect { case x: StepA_Pipette => x }
		// Get
		val stepToTipModels = RsResult.mapAll(step1_l)(step => {
			val well = step.s.l.head.well
			for {
				mixture <- RsResult.from(state0.well_aliquot_m.get(well).map(_.mixture), s"no liquid specified in source well: ${well}")
				tip_l <- step.tip_? match {
					case None => RsSuccess(tipAll_l)
					case Some(tip_i) =>
						tipAll_l.find(_.index == tip_i) match {
							case None => RsError(s"invalid tip index ${tip_i}")
							case Some(tip) => RsSuccess(List(tip))
						}
				}
			} yield {
				val tipModelPossible_l = step.tipModel_? match {
					case None => tip_l.flatMap(eb.tipToTipModels_m).distinct
					case Some(tipModel) => List(tipModel)
				}
				// Get the valid tip models for this item's source mixture and volume
				val tipModel_l = device.getDispenseAllowableTipModels(tipModelPossible_l, mixture, step.v)
				step -> tipModel_l
			}
		}).map(_.toMap)
		
		CONTINUE HERE
		
		val source_l = step_l.map(_.s).toSet
		val sourceToMixture_m = RsResult.mapAll(source_l.toList)(s =>
			RsResult.from(state0.well_aliquot_m.get(s.l.head.well).map(s -> _.mixture), s"no liquid specified in source well: ${s.l.head.well}")
		).map(_.toMap)
		val stepToTipModels_m = stepA_l.map(step => step -> device.getDispenseAllowableTipModels(tipModel_l, mixture, item.volume))
	}

	/*
	def getWellEvents(): List[WorldStateEvent] = List(new WorldStateEvent {
		def update(state: WorldStateBuilder): RqResult[Unit] = {
			// Add liquid to destination
			val n = destinations.l.length
			val source_l = if (sources.sources.length == 1) List.fill(n)(sources.sources.head.l.head) else sources.sources.map(_.l.head)
			val volume2_l = if (volume_l.length == 1) List.fill(n)(volume_l.head) else volume_l
			for ((dst, src, volume) <- (destinations.l, source_l, volume2_l).zipped) {
				val srcAliquot = state.well_aliquot_m.getOrElse(src.well, Aliquot.empty)
				val dstAliquot0 = state.well_aliquot_m.getOrElse(dst.well, Aliquot.empty)
	            val amount = Distribution.fromVolume(volume)
				val aliquot = Aliquot(srcAliquot.mixture, amount)
				val x = for {
					dstAliquot1 <- dstAliquot0.add(aliquot)
				} yield {
					//println(s"update: ${dst.well.label} ${dstAliquot0} + ${aliquot} -> ${dstAliquot1}")
					state.well_aliquot_m(dst.well) = dstAliquot1
				}
				x match {
					case RqError(e, w) => return RqError(e, w)
					case _ =>
				}
			}
			RqSuccess(())
		}
	})*/
}