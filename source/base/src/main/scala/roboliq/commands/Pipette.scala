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
import roboliq.pipette.planners.TipModelSearcher0
import roboliq.entities.Mixture
import roboliq.entities.Tip

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
	s: LiquidSource,
	d: WellInfo,
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
	s: List[WellInfo],
	d: WellInfo,
	v: LiquidVolume,
	tipModel: TipModel,
	pipettePolicy: String,
	cleanBefore: CleanIntensity.Value,
	cleanAfter: CleanIntensity.Value,
	tips: List[Int]
) extends StepB

case class PipetteStepC(
	s: WellInfo,
	d: WellInfo,
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
						s,
						d,
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
	 * @param index0 this represents the index of stepA_l.head within the complete list of steps specified in the action
	 */
	def aToB2(
		index0: Int,
		stepA_l: List[StepA],
		eb: EntityBase,
		state0: WorldState,
		pipetter: Pipetter
	): RsResult[List[StepB]] = {
		val device = new PipetteDevice
		val tipAll_l = eb.pipetterToTips_m(pipetter)
		val tipModelAll_l = tipAll_l.flatMap(eb.tipToTipModels_m).distinct
		val step1_l = stepA_l.collect { case x: StepA_Pipette => x }
		
		// Get the available tipModels for the given step
		def getTipModels(step: StepA_Pipette, i0: Int): RsResult[(StepA_Pipette, (Mixture, List[Tip], List[TipModel]))] = {
			val index = index0 + i0 + 1
			val well = step.s.l.head.well
			for {
				mixture <- RsResult.from(state0.well_aliquot_m.get(well).map(_.mixture), s"step $index: no liquid specified in source well: ${well}")
				// If a tip is explicitly assigned, use it, otherwise consider all available tips 
				tip_l <- step.tip_? match {
					case None => RsSuccess(tipAll_l)
					case Some(tip_i) =>
						tipAll_l.find(_.index == tip_i) match {
							case None => RsError(s"invalid tip index ${tip_i}")
							case Some(tip) => RsSuccess(List(tip))
						}
				}
				_ <- RsResult.assert(!tip_l.isEmpty, s"step $index: no tips available")
				// Get tip models for the selected tips
				tipModelPossible_l = step.tipModel_? match {
					case None => tip_l.flatMap(eb.tipToTipModels_m).distinct
					case Some(tipModel) => List(tipModel)
				}
				// Get the subset valid tip models for this item's source mixture and volume
				tipModel_l = device.getDispenseAllowableTipModels(tipModelPossible_l, mixture, step.v)
				_ <- RsResult.assert(!tipModel_l.isEmpty, s"step $index: no tip models available")
			} yield {
				step -> ((mixture, tip_l, tipModel_l))
			}
		}
		
		for {
			// Get possible tip models for each step
			stepToMixtureTipsModels_m <- RsResult.mapAll(step1_l.zipWithIndex)(pair => getTipModels(pair._1, pair._2)).map(_.toMap)
			stepToTipModels_m = stepToMixtureTipsModels_m.mapValues(_._3)
			// Choose tip model for each step
			stepToTipModel_m <- chooseTipModel(tipModelAll_l, stepToTipModels_m)
			// Assign pipette policy for each step
			// TODO: Need to choose pipette policy intelligently
			stepToPolicy_m = stepToTipModel_m.map(pair => pair._1 -> pair._1.pipettePolicy_?.getOrElse("POLICY"))

			// cleanBefore, cleanAfter
			
			// COPIED FROM PipetteMethod:
			
			// Run transfer planner to get pippetting batches
			batch_l <- TransferSimplestPlanner.searchGraph(
				device,
				path0.state,
				SortedSet(tipCandidate_l : _*),
				tipModel, //itemToTipModel_m.head._2,
				pipettePolicy,
				item_l
			)
			
			// Refresh command before pipetting starts
			refreshBefore_l = {
				spec.cleanBefore_?.orElse(spec.clean_?) match {
					case Some(intensity) if intensity != CleanIntensity.None =>
						val tip_l = batch_l.flatMap(_.item_l.map(_.tip))
						PipetterTipsRefresh(pipetter, tip_l.map(tip => {
							(tip, intensity, Some(tipModel))
						})) :: Nil
					case _ => Nil
				}
			}
			path1 <- path0.add(refreshBefore_l)
	
			path2 <- getAspDis(path1, spec, pipetter, device, tipModel, pipettePolicy, batch_l)

			refreshAfter_l = {
				val tipOverrides = TipHandlingOverrides(None, spec.cleanAfter_?.orElse(spec.clean_?), None, None, None)
				PipetterTipsRefresh(pipetter, tip_l.map(tip => {
					val tipState = path2.state.getTipState(tip)
					val washSpec = PipetteHelper.choosePreAspirateWashSpec(tipOverrides, Mixture.empty, tipState)
					(tip, washSpec.washIntensity, None)
				})) :: Nil
			}
			path3 <- path2.add(refreshAfter_l)
		} yield {
			
		}
	}

	/*
	 * - see if any tip model can be used for
	 *   - all steps
	 *   - all steps without explicit tipModel
	 *   - split steps by source, and try to assign tip model to each source
	 *   - for any remaining sources which couldn't be assigned a single tipModel, assign as best as possible
	 */
	private def chooseTipModel(
		tipModelAll_l: List[TipModel],
		stepToTipModels_m: Map[StepA_Pipette, List[TipModel]]
	): RsResult[Map[StepA_Pipette, TipModel]] = {
		// See whether any tipModels can be used for all steps
		val tipModelSearcher = new TipModelSearcher0[StepA_Pipette, TipModel]
		tipModelSearcher.searchGraph(tipModelAll_l, stepToTipModels_m) match {
			case RsSuccess(tipModel, _) => RsSuccess(stepToTipModels_m.mapValues(_ => tipModel))
			case _ =>
				val m = stepToTipModels_m.mapValues(_.head)
				RsSuccess(m, List("INTERNAL: not yet implemented to have more than one tip model per pipetting set, so setting the tip model individually for each step"))
		}
	}
	
	private def choosePolicyAndClean(
		stepToMixtureTipsModels_m: Map[StepA_Pipette, (Mixture, List[Tip], List[TipModel])],
		stepToTipModel_m: Map[StepA_Pipette, TipModel]
	): Unit = {
		
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