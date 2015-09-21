package roboliq.commands

import roboliq.core._
import roboliq.entities._
import roboliq.input.commands.PipetteSpec
import roboliq.input.Context


private case class SourceAmountTip(
	sv: TitrateItem_SourceVolume,
	amount_? : Option[PipetteAmount],
	tip_? : Option[Int]
)

private case class SourceVolumeTip(
	sv: TitrateItem_SourceVolume,
	volume: LiquidVolume,
	tip_? : Option[Int]
) {
	override def toString = s"SVT($volume, ${tip_?})"
}

class TitrateMethod(
	params: TitrateActionParams
) {
	def createPipetteActionParams(): Context[List[PipetteActionParams]] = {
		//println("reagentToWells_m: "+eb.reagentToWells_m)
		for {
			// Turn the user-specified steps into simpler individual and/or/source items
			item_l <- Context.from(RqResult.mapFirst(params.allOf)(_.getItem).map(_.flatten))
			itemTop = TitrateItem_And(item_l)
			//_ = println("itemTop:")
			//_ = itemTop.printShortHierarchy(eb, "")
			// A list of wells, each holding a list of what goes in that well
			sat1_ll = createWellMixtures(itemTop, Nil)
			tooManyFillers_l = sat1_ll.filter(sat1_l => sat1_l.filter(_.amount_?.isEmpty).size > 1)
			_ <- Context.assert(tooManyFillers_l.isEmpty, "Only one source may have an unspecified volume per well: "+tooManyFillers_l.map(_.map(_.amount_?)))
			svt1_ll <- Context.from(amountsToVolumes(sat1_ll, params.amount_?))
			svt2_ll = combineWithTips(svt1_ll, params.tip, 1)
			//_ = mixture1_l.foreach(mixture => println(mixture.map(_._2)))
			// Number of wells required if we only use a single replicate
			wellCountReplicate1 = svt2_ll.length
			wellCountMin = wellCountReplicate1 * params.replicates_?.getOrElse(1)
			_ <- Context.assert(wellCountMin > 0, "A titration series must specify steps with sources and volumes")
			// Maximum number of wells available to us
			wellCountMax = params.destination.l.length
			_ <- Context.assert(wellCountReplicate1 <= wellCountMax, s"You must allocate more destination wells.  The titration series requires at least $wellCountMin wells, and you have only supplied $wellCountMax wells.")
			// Check replicate count
			replicateCountMax = wellCountMax / wellCountReplicate1
			replicateCount = params.replicates_?.getOrElse(replicateCountMax)
			wellCount = wellCountReplicate1 * replicateCount
			//_ = println("counts: ", wellCountMin, wellCountMax, replicateCountMax, replicateCount, wellCount)
			//_ = 1 / 0
			_ <- Context.assert(wellCount <= wellCountMax, s"You must allocate more destination wells in order to accommodate $replicateCount replicates.  You have supplied $wellCountMax wells, which can accommodate $replicateCountMax replicates.  For $replicateCount replicates you will need to supply ${wellCount} wells.")
			svt3_ll = combineWithTips(svt1_ll, params.tip, replicateCount)
			//_ = println("svt1_ll:\n"+svt1_ll)
			//_ = println("svt2_ll:\n"+svt2_ll)
			//_ = println("svt3_ll:\n"+svt3_ll)
			//_ = println(replicateCount, wellCount, svt2_ll.size, svt2_ll.length, wellCountMin, wellCountMax, replicateCountMax)
			//_ = 1/0
			stepOrder_l = flattenSteps(itemTop)
			//printMixtureCsv(l3)
			//println("----------------")
			//println("l3")
			//println(l3)
			//printMixtureCsv(stepToList_l.map(_._2))
			destinations = PipetteDestinations(params.destination.l.take(wellCount))
			_ = println()
			_ = println("INFO: ", wellCountReplicate1, wellCountMin, wellCountMax, replicateCountMax, replicateCount, wellCount)
			_ = println("destinations: "+destinations)
			_ = println("svt1_ll:\n"+svt1_ll)
			_ = println("svt3_ll:\n"+svt3_ll)
			_ = println()
			destinationToSvts_l = destinations.l zip svt3_ll
			_ <- printDestinationMixtureCsv(destinationToSvts_l)
		} yield {
			//println("len: "+stepToList_l.map(_._2.length))
			stepOrder_l.map(step => {
				// Get items corresponding to this step
				val wellToSvt0_l: List[(WellInfo, SourceVolumeTip)] = destinationToSvts_l.flatMap { case (well, svt_l) =>
					svt_l.find(svt => svt.sv.step == step && !svt.volume.isEmpty).map(well -> _)
				}
				
				// FIXME: the sort order needs to be specified by the user
				// If none is specified, then just use the order of wellToSvt0_l (ordered by destination)
				val wellToSvt_l = {
					// If each step should be sorted by source:
					//if (true) {
						val sourceUniq_l = wellToSvt0_l.map(_._2.sv.source).distinct
						val wellToSvtToIndex_l = wellToSvt0_l.map(pair => (pair, sourceUniq_l.indexOf(pair._2.sv.source)))
						wellToSvtToIndex_l.sortBy(_._2).map(_._1)
					//}
				}
				
				val pipetteStep_l = wellToSvt_l.map { case (destination, svt) =>
					PipetteStepParams(
						Some(svt.sv.source),
						Some(PipetteDestination(destination)),
						Some(PipetteAmount_Volume(svt.volume)),
						step.pipettePolicy_?,
						// FIXME: I'm not sure how to set the cleaning params here, since the titrate step contains an entire group of pipetting steps.
						None, // clean_?
						None, // cleanBefore_?
						None, // cleanAfter_?
						step.tipModel_?,
						svt.tip_?
					)
				}
				PipetteActionParams(
					source_? = None,
					destination_? = None,
					amount = Nil,
					clean_? = step.clean_? orElse params.clean_?,
					cleanBegin_? = step.clean_? orElse params.cleanBegin_?,
					cleanBetween_? = step.cleanBetween_? orElse params.cleanBetween_?,
					cleanBetweenSameSource_? = step.cleanBetweenSameSource_? orElse params.cleanBetweenSameSource_?,
					cleanEnd_? = step.cleanEnd_? orElse params.cleanEnd_?,
					pipettePolicy_? = params.pipettePolicy_?,
					tipModel_? = params.tipModel_?,
					tip_? = None,
					steps = pipetteStep_l
				)
			}) //.filterNot(_.sources.sources.isEmpty)
		}
	}
	
	// Combine two lists by crossing all items from list 1 with all items from list 2
	// Each list can be thought of as being in DNF (disjunctive normal form)
	// and we combine two with the AND operation and produce a new list in DNF.
	private def mixLists_And(
		mixture1_l: List[List[SourceAmountTip]],
		mixture2_l: List[List[SourceAmountTip]]
	): List[List[SourceAmountTip]] = {
		for {
			mixture1 <- mixture1_l
			mixture2 <- mixture2_l
		} yield mixture1 ++ mixture2
	}
	
	private def mixManyLists_And(
		mixture_ll: List[List[List[SourceAmountTip]]]
	): List[List[SourceAmountTip]] = {
		mixture_ll.filterNot(_.isEmpty) match {
			case Nil => Nil
			case first :: rest =>
				rest.foldLeft(first){ (acc, next) => mixLists_And(acc, next) }
		}
	}
	
	// ORing two lists in DNF just involves concatenating the two lists.
	private def mixManyLists_Or(
		mixture_ll: List[List[List[SourceAmountTip]]]
	): List[List[SourceAmountTip]] = {
		mixture_ll.flatten
	}
	
	// Return a list of source+volume for each well
	private def createWellMixtures(
		item: TitrateItem,
		mixture_l: List[List[SourceAmountTip]]
	): List[List[SourceAmountTip]] = {
		//println("item: ")
		//item.printShortHierarchy(eb, "  ")
		//println("mixture_l:")
		//mixture_l.foreach(mixture => println(mixture.map(_._2).mkString("+")))
		item match {
			case TitrateItem_And(l) =>
				val l2 = l.map(item => createWellMixtures(item, Nil))
				//println("l2: "+l2.map(_.map(_.map(_._2).mkString("+")).mkString(",")))
				val l3 = mixManyLists_And(mixture_l :: l2)
				//println("l3: "+l3.map(_.map(_._2).mkString("+")).mkString(","))
				l3
			case TitrateItem_Or(l) =>
				val l4 = l.map(item => createWellMixtures(item, Nil))
				//println("l4: "+l4.map(_.map(_.map(_._2).mkString("+")).mkString(",")))
				val l5 = mixManyLists_Or(mixture_l :: l4)
				//println("l5: "+l5.map(_.map(_._2).mkString("+")).mkString(","))
				l5
			case sv: TitrateItem_SourceVolume =>
				sv.step.tip match {
					case Nil => List(List(SourceAmountTip(sv, sv.amount_?, None)))
					case tip_l => tip_l.map(tip_i => List(SourceAmountTip(sv, sv.amount_?, Some(tip_i))))
				}
		}
	}
	
	private def amountsToVolumes(
		mixtureAmount_l: List[List[SourceAmountTip]],
		volumeTotal_? : Option[LiquidVolume]
	): RqResult[List[List[SourceVolumeTip]]] = {
		RqResult.toResultOfList(mixtureAmount_l.map { mixture =>
			val (empty_l, nonempty_l) = mixture.partition(pair => pair.amount_?.isEmpty)
			var volumeWell = LiquidVolume.empty
			var numWell = BigDecimal(0)
			var denWell = BigDecimal(1)
			for (SourceAmountTip(_, amount_?, _) <- nonempty_l) {
				amount_? match {
					case None =>
					case Some(PipetteAmount_Volume(volume)) => volumeWell += volume
					case Some(PipetteAmount_Dilution(c, d)) =>
						// a/b + c/d = (a*d + c*b)/(b*d)
						val a = numWell
						val b = denWell
						numWell = a*d + c*b
						denWell = b * d
				}
			}
			// if volumeTotal is defined:
			//  volume-of-dilutions/volumeTotal = a / b
			//  => volume-of-dilutions = a * volumeTotal / b
			// otherwise:
			//  volume-of-dilutions / (volumeWell + volume-of-dilutions) = a / b
			//  b*volume-of-dilutions  = a*volumeWell + a*volume-of-dilutions
			//  (b - a)*volume-of-dilutions = a*volumeWell
			//  volume-of-dilutions = a*volumeWell / (b - a)
			val volumeDilutions: LiquidVolume = volumeTotal_? match {
				case Some(volumeTotal) =>
					volumeTotal * numWell / denWell
				case None =>
					volumeWell * numWell / (denWell - numWell)
			}
			val volumeNonfiller = volumeWell + volumeDilutions
			val volumeTotal: LiquidVolume = volumeTotal_? match {
				case Some(x) => x
				case None => volumeNonfiller
			}
			for {
				volumeFiller <- empty_l match {
					case Nil => RqSuccess(LiquidVolume.empty)
					case empty :: Nil => RqSuccess(volumeTotal - volumeNonfiller)
					case _ => RqError("Mixtures may have at most one filler component (one without a specified amount)")
				}
				_ <- RqResult.assert(numWell <= denWell, "Invalid dilutions, exceed 1:1")
				_ <- RqResult.assert(volumeWell <= volumeTotal, "Sum of component volumes exceeds total volume")
			} yield {
				//println("titrate:", volumeTotal, volumeNonfiller, volumeFiller, volumeDilutions, volumeWell)
				//println()
				mixture.map { xo =>
					xo.amount_? match {
						case None => SourceVolumeTip(xo.sv, volumeFiller, xo.tip_?)
						case Some(PipetteAmount_Volume(volume)) => SourceVolumeTip(xo.sv, volume, xo.tip_?)
						case Some(PipetteAmount_Dilution(c, d)) => SourceVolumeTip(xo.sv, volumeTotal * c / d, xo.tip_?)
					}
				}
			}
		})
	}
	
	/**
	 * For each well, make copies of that well for each tip in tip_l.
	 * If we should make replicates, iterate through tips first, then replicate that list.
	 */
	private def combineWithTips(
		svt0_ll: List[List[SourceVolumeTip]],
		tip_l: List[Int],
		replicateCount: Int
	): List[List[SourceVolumeTip]] = {
		if (tip_l.isEmpty) {
			return svt0_ll.flatMap(svt => List.fill(replicateCount)(svt))
		}
		else {
			svt0_ll.flatMap { svt0_l =>
				// If there are any unset tips in this well:
				if (svt0_l.exists(_.tip_?.isEmpty)) {
					List.fill(replicateCount)(tip_l.map(tip_i => svt0_l.map(_.copy(tip_? = Some(tip_i))))).flatten
				}
				// Otherwise, all tips have already been set by the steps, so don't make any new combinations
				else {
					svt0_l.map(svt => List.fill(replicateCount)(svt))
				}
			}
		}
	}

	/*
	// Replace any missing volumes with fill volumes
	private def addFillVolume(
		mixture_ll: List[List[SourceAmount]],
		fillVolume_l: List[LiquidVolume]
	): List[List[SourceVolumeTip]] = {
		assert(mixture_ll.length == fillVolume_l.length)
		for {
			(mixture_l, fillVolume) <- (mixture_ll zip fillVolume_l)
		} yield {
			mixture_l.map { mixture =>
				mixture._2 match {
					case None => (mixture._1, fillVolume)
					case Some(amount) => (mixture._1, volume)
				}
			}
		}
	}*/
	
	/*def printMixtureCsv(ll: List[List[SourceVolumeTip]]): Unit = {
		var i = 1
		for (l <- ll) {
			val x = for ((sv, volume) <- l) yield {
				val well = sv.source.l.head.well
				val y = for {
					aliquote <- state0.well_aliquot_m.get(well).asRs("no liquid found in source")
				} yield {
					List("\""+aliquote.mixture.toShortString+"\"", volume.ul.toString)
				}
				y match {
					case RqSuccess(l, _) => l
					case RqError(_, _) => List("\"ERROR\"", "0")
				}
			}
			println(x.flatten.mkString(", "))
		}
	}*/
	
	private def printDestinationMixtureCsv(ll: List[(WellInfo, List[SourceVolumeTip])]): Context[Unit] = {
		if (ll.isEmpty) return Context.unit(())
		var i = 1
		val header = (1 to ll.head._2.length).toList.map(n => "\"mixture"+n+"\",\"volume"+n+"\",\"tip"+n+"\"").mkString(""""plate","well","tip",""", ",", "")
		println(header)
		for {
			state0 <- Context.gets(_.state)
		} yield {
			for ((wellInfo, l) <- ll) {
				val x = for (SourceVolumeTip(sv, volume, tip_?) <- l) yield {
					val well = sv.source.l.head.well
					val y = for {
						aliquot <- RsResult.from(state0.well_aliquot_m.get(well), "no liquid found in source")
						_ <- RsResult.assert(!aliquot.isEmpty, "no mixture found in source")
					} yield {
						val aliquot2 = if (aliquot.hasVolume) aliquot else Aliquot(aliquot.mixture, Distribution.fromVolume(LiquidVolume.l(1)))
						val flat = AliquotFlat(aliquot2)
						val tip_s = tip_?.map(_.toString).getOrElse("")
						List("\""+flat.toMixtureString+"\"", volume.ul.toString, tip_s)
					}
					y match {
						case RqSuccess(l, _) => l
						case RqError(_, _) => List("\"ERROR\"", "0")
					}
				}
				val wellName = "\"" + wellInfo.rowcol.toString + "\""
				println((wellInfo.labwareName :: wellName :: x.flatten).mkString(","))
			}
		}
	}
	
	private def flattenSteps(item: TitrateItem): List[TitrateStepParams] = {
		item match {
			case TitrateItem_And(l) => l.flatMap(flattenSteps).distinct
			case TitrateItem_Or(l) => l.flatMap(flattenSteps).distinct
			case TitrateItem_SourceVolume(step, _, _) => List(step)
		}
	}

}