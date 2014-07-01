package roboliq.commands

import roboliq.core._
import roboliq.entities._
import roboliq.input.commands.PipetteSpec


private case class XO(
	sv: TitrateItem_SourceVolume,
	amount_? : Option[PipetteAmount],
	tip_? : Option[Int]
)

private case class X(
	sv: TitrateItem_SourceVolume,
	volume: LiquidVolume,
	tip_? : Option[Int]
)

class TitrateMethod(
	eb: EntityBase,
	state0: WorldState,
	params: TitrateActionParams
) {
	def createPipetteActionParams(): RsResult[PipetteActionParams] = {
		//println("reagentToWells_m: "+eb.reagentToWells_m)
		for {
			// Turn the user-specified steps into simpler individual and/or/source items
			item_l <- RqResult.toResultOfList(params.allOf.map(_.getItem)).map(_.flatten)
			itemTop = TitrateItem_And(item_l)
			_ = println("itemTop:")
			_ = itemTop.printShortHierarchy(eb, "")
			// Number of wells required if we only use a single replicate
			mixtureAmount1_l = createWellMixtures(itemTop, Nil)
			//_ = mixture1_l.foreach(mixture => println(mixture.map(_._2)))
			wellCountMin = mixtureAmount1_l.length
			_ <- RqResult.assert(wellCountMin > 0, "A titration series must specify steps with sources and volumes")
			// Maximum number of wells available to us
			wellCountMax = params.destination.l.length
			_ <- RqResult.assert(wellCountMin <= wellCountMax, s"You must allocate more destination wells.  The titration series requires at least $wellCountMin wells, and you have only supplied $wellCountMax wells.")
			// Check replicate count
			replicateCountMax = wellCountMax / wellCountMin
			replicateCount = params.replicates_?.getOrElse(replicateCountMax)
			wellCount = wellCountMin * replicateCount
			_ <- RqResult.assert(wellCountMin <= wellCountMax, s"You must allocate more destination wells in order to accommodate $replicateCount replicates.  You have supplied $wellCountMax wells, which can accommodate $replicateCountMax replicates.  For $replicateCount replicates you will need to supply ${wellCount} wells.")
			//_ = println("params: "+params)
			tooManyFillers_l = mixtureAmount1_l.filter(mixture => mixture.filter(_.amount_?.isEmpty).size > 1)
			_ <- RqResult.assert(tooManyFillers_l.isEmpty, "Only one source may have an unspecified volume per well: "+tooManyFillers_l.map(_.map(_.amount_?)))
			//_ = println("wellsPerGroup: "+wellsPerGroup)
			//_ = println("groupCount: "+groupCount)
			//_ = println("wellCount: "+wellCount)
			/*l1 = params.steps.flatMap(step => {
				// If this is the filler step:
				step.volume_? match {
					case None => None
					case Some(volume) =>
						val wellsPerSource = wellCount / step.source.sources.length
						val wellsPerVolume = wellsPerSource / volume.length
						val source_l = step.source.sources.flatMap(x => List.fill(wellsPerSource)(x))
						val volume_l = List.fill(step.source.sources.length)(volume.flatMap(x => List.fill(wellsPerVolume)(x))).flatten
						//println("stuff:", wellsPerSource, wellsPerVolume, source_l.length, volume_l)
						Some(source_l zip volume_l)
				}
			})*/
			mixtureVolume1_l <- amountsToVolumes(mixtureAmount1_l, params.amount_?)
			l3 = mixtureVolume1_l.flatMap(mixture => List.fill(replicateCount)(mixture))
			//l3 = dox(params.steps, wellsPerGroup, Nil, Nil)
			/*stepToList_l: List[(TitrateStepParams, List[(LiquidSource, LiquidVolume)])] = params.steps.map(step => {
				// If this is the filler step:
				step.volume_? match {
					case None => val l = step -> fillVolume_l.map(step.source.sources.head -> _)
						l
					case Some(volume) =>
						val wellsPerSource = wellCount / step.source.sources.length
						val wellsPerVolume = wellsPerSource / volume.length
						val source_l = step.source.sources.flatMap(x => List.fill(wellsPerSource)(x))
						val volume_l = List.fill(step.source.sources.length)(volume.flatMap(x => List.fill(wellsPerVolume)(x))).flatten
						//println("s x v: "+source_l.length+", "+volume_l.length)
						assert(source_l.forall(s => !s.l.isEmpty))
						val l = step -> (source_l zip volume_l)
						l
				}
			})*/
			stepOrder_l = flattenSteps(itemTop)
			//stepToList_l = params.steps zip l3.transpose
		} yield {
			//printMixtureCsv(l3)
			//println("----------------")
			//println("l3")
			//println(l3)
			//printMixtureCsv(stepToList_l.map(_._2))
			val destinations = PipetteDestinations(params.destination.l.take(wellCount))
			//println("destinations: "+destinations)
			val destinationToMixture_l = destinations.l zip l3
			printDestinationMixtureCsv(destinationToMixture_l)
			//println("len: "+stepToList_l.map(_._2.length))
			val pipetteStep_l = stepOrder_l.flatMap(step => {
				// Get items corresponding to this step
				val l1: List[(WellInfo, List[X])]
					= destinationToMixture_l.map(pair => pair._1 -> pair._2.filter(x => (x.sv.step eq step) && (!x.volume.isEmpty)))
				// There should be at most one item per destination
				assert(l1.forall(_._2.size <= 1))
				// Keep the destinations with exactly one item
				val l2: List[(WellInfo, X)]
					= l1.filterNot(_._2.isEmpty).map(pair => pair._1 -> pair._2.head)
				val (destination_l, x_l) = l2.unzip
				val sv_l = x_l.map(_.sv)
				val volume_l = x_l.map(_.volume)
				val source_l = sv_l.map(_.source)
				val keep_l = volume_l.map(!_.isEmpty)
				assert(source_l.forall(s => !s.l.isEmpty))
				val sdv0_l = (sv_l, destination_l, volume_l).zipped.toList
				// Remove empty volumes
				val sdv_l = sdv0_l.filterNot({x => x._3.isEmpty})
				sdv_l.map { case (sv, destination, volume) =>
					PipetteStepParams(
						Some(sv.source),
						Some(PipetteDestination(destination)),
						Some(PipetteAmount_Volume(volume)),
						step.pipettePolicy_?,
						// I'm not sure how to set the cleaning params here, since the titrate step contains an entire group of pipetting steps.
						None, // clean_?
						step.cleanBefore_?, // cleanBefore_?
						step.cleanBetween_?, // cleanAfter_?
						step.tipModel_?,
						None
					)
				}
			}) //.filterNot(_.sources.sources.isEmpty)
			PipetteActionParams(
				destination_? = None,
				source_? = None,
				amount = Nil,
				clean_? = params.clean_?,
				cleanBegin_? = params.cleanBegin_?,
				cleanBetween_? = params.cleanBetween_?,
				cleanBetweenSameSource_? = params.cleanBetweenSameSource_?,
				cleanEnd_? = params.cleanEnd_?,
				pipettePolicy_? = params.pipettePolicy_?,
				tipModel_? = params.tipModel_?,
				tip_? = None,
				steps = pipetteStep_l
			)
		}
	}

	/*
	def run(): RsResult[List[PipetteSpec]] = {
		//println("reagentToWells_m: "+eb.reagentToWells_m)
		for {
			// Turn the user-specified steps into simpler individual and/or/source items
			item_l <- RqResult.toResultOfList(params.allOf.map(_.getItem)).map(_.flatten)
			itemTop = TitrateItem_And(item_l)
			_ = println("itemTop:")
			_ = itemTop.printShortHierarchy(eb, "")
			// Number of wells required if we only use a single replicate
			mixtureAmount1_l = createWellMixtures(itemTop, Nil)
			//_ = mixture1_l.foreach(mixture => println(mixture.map(_._2)))
			wellCountMin = mixtureAmount1_l.length
			_ <- RqResult.assert(wellCountMin > 0, "A titration series must specify steps with sources and volumes")
			// Maximum number of wells available to us
			wellCountMax = params.destination.l.length
			_ <- RqResult.assert(wellCountMin <= wellCountMax, s"You must allocate more destination wells.  The titration series requires at least $wellCountMin wells, and you have only supplied $wellCountMax wells.")
			// Check replicate count
			replicateCountMax = wellCountMax / wellCountMin
			replicateCount = params.replicates_?.getOrElse(replicateCountMax)
			wellCount = wellCountMin * replicateCount
			_ <- RqResult.assert(wellCountMin <= wellCountMax, s"You must allocate more destination wells in order to accommodate $replicateCount replicates.  You have supplied $wellCountMax wells, which can accommodate $replicateCountMax replicates.  For $replicateCount replicates you will need to supply ${wellCount} wells.")
			//_ = println("params: "+params)
			tooManyFillers_l = mixtureAmount1_l.filter(mixture => mixture.filter(_._2.isEmpty).size > 1)
			_ <- RqResult.assert(tooManyFillers_l.isEmpty, "Only one source may have an unspecified volume per well: "+tooManyFillers_l.map(_.map(_._2)))
			//_ = println("wellsPerGroup: "+wellsPerGroup)
			//_ = println("groupCount: "+groupCount)
			//_ = println("wellCount: "+wellCount)
			/*l1 = params.steps.flatMap(step => {
				// If this is the filler step:
				step.volume_? match {
					case None => None
					case Some(volume) =>
						val wellsPerSource = wellCount / step.source.sources.length
						val wellsPerVolume = wellsPerSource / volume.length
						val source_l = step.source.sources.flatMap(x => List.fill(wellsPerSource)(x))
						val volume_l = List.fill(step.source.sources.length)(volume.flatMap(x => List.fill(wellsPerVolume)(x))).flatten
						//println("stuff:", wellsPerSource, wellsPerVolume, source_l.length, volume_l)
						Some(source_l zip volume_l)
				}
			})*/
			mixtureVolume1_l <- amountsToVolumes(mixtureAmount1_l, params.amount_?)
			l3 = mixtureVolume1_l.flatMap(mixture => List.fill(replicateCount)(mixture))
			//l3 = dox(params.steps, wellsPerGroup, Nil, Nil)
			/*stepToList_l: List[(TitrateStepParams, List[(LiquidSource, LiquidVolume)])] = params.steps.map(step => {
				// If this is the filler step:
				step.volume_? match {
					case None => val l = step -> fillVolume_l.map(step.source.sources.head -> _)
						l
					case Some(volume) =>
						val wellsPerSource = wellCount / step.source.sources.length
						val wellsPerVolume = wellsPerSource / volume.length
						val source_l = step.source.sources.flatMap(x => List.fill(wellsPerSource)(x))
						val volume_l = List.fill(step.source.sources.length)(volume.flatMap(x => List.fill(wellsPerVolume)(x))).flatten
						//println("s x v: "+source_l.length+", "+volume_l.length)
						assert(source_l.forall(s => !s.l.isEmpty))
						val l = step -> (source_l zip volume_l)
						l
				}
			})*/
			stepOrder_l = flattenSteps(itemTop)
			//stepToList_l = params.steps zip l3.transpose
		} yield {
			//printMixtureCsv(l3)
			//println("----------------")
			//println("l3")
			//println(l3)
			//printMixtureCsv(stepToList_l.map(_._2))
			val destinations = PipetteDestinations(params.destination.l.take(wellCount))
			//println("destinations: "+destinations)
			val destinationToMixture_l = destinations.l zip l3
			printDestinationMixtureCsv(destinationToMixture_l)
			//println("len: "+stepToList_l.map(_._2.length))
			stepOrder_l.map(step => {
				// Get items corresponding to this step
				val l1: List[(WellInfo, List[X])]
					= destinationToMixture_l.map(pair => pair._1 -> pair._2.filter(pair => (pair._1.step eq step) && (!pair._2.isEmpty)))
				// There should be at most one item per destination
				assert(l1.forall(_._2.size <= 1))
				// Keep the destinations with exactly one item
				val l2: List[(WellInfo, X)]
					= l1.filterNot(_._2.isEmpty).map(pair => pair._1 -> pair._2.head)
				val (destination_l, l3) = l2.unzip
				val (sv_l, volume_l) = l3.unzip
				val source_l = sv_l.map(_.source)
				val keep_l = volume_l.map(!_.isEmpty)
				assert(source_l.forall(s => !s.l.isEmpty))
				// Remove items with empty volumes
				//val l1 = (destinations.l zip sourceToVolume_l).filterNot(_._2._2.isEmpty)
				//println("volume_l: "+volume_l)
				PipetteSpec(
					PipetteSources(source_l),
					PipetteDestinations(destination_l),
					volume_l,
					step.pipettePolicy_?.orElse(params.pipettePolicy_?),
					step.clean_?.orElse(params.clean_?),
					step.cleanBefore_?,
					step.cleanBetween_?.orElse(params.cleanBetween_?),
					step.cleanAfter_?,
					None // FIXME: handle tipModel_?
				)
			}).filterNot(_.sources.sources.isEmpty)
		}
	}
	*/
	
	// Combine two lists by crossing all items from list 1 with all items from list 2
	// Each list can be thought of as being in DNF (disjunctive normal form)
	// and we combine two with the AND operation and produce a new list in DNF.
	private def mixLists_And(
		mixture1_l: List[List[XO]],
		mixture2_l: List[List[XO]]
	): List[List[XO]] = {
		for {
			mixture1 <- mixture1_l
			mixture2 <- mixture2_l
		} yield mixture1 ++ mixture2
	}
	
	private def mixManyLists_And(
		mixture_ll: List[List[List[XO]]]
	): List[List[XO]] = {
		mixture_ll.filterNot(_.isEmpty) match {
			case Nil => Nil
			case first :: rest =>
				rest.foldLeft(first){ (acc, next) => mixLists_And(acc, next) }
		}
	}
	
	// ORing two lists in DNF just involves concatenating the two lists.
	private def mixManyLists_Or(
		mixture_ll: List[List[List[XO]]]
	): List[List[XO]] = {
		mixture_ll.flatten
	}
	
	// Return a list of source+volume for each well
	private def createWellMixtures(
		item: TitrateItem,
		mixture_l: List[List[XO]]
	): List[List[XO]] = {
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
					case Nil => List(List(XO(sv, sv.amount_?, None)))
					case tip_l => tip_l.map(tip_i => List(XO(sv, sv.amount_?, Some(tip_i))))
				}
		}
	}
	
	private def amountsToVolumes(
		mixtureAmount_l: List[List[XO]],
		volumeTotal_? : Option[LiquidVolume]
	): RqResult[List[List[X]]] = {
		RqResult.toResultOfList(mixtureAmount_l.map { mixture =>
			val (empty_l, nonempty_l) = mixture.partition(pair => pair.amount_?.isEmpty)
			var volumeWell = LiquidVolume.empty
			var numWell = BigDecimal(0)
			var denWell = BigDecimal(1)
			for (XO(_, amount_?, _) <- nonempty_l) {
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
						case None => X(xo.sv, volumeFiller, xo.tip_?)
						case Some(PipetteAmount_Volume(volume)) => X(xo.sv, volume, xo.tip_?)
						case Some(PipetteAmount_Dilution(c, d)) => X(xo.sv, volumeTotal * c / d, xo.tip_?)
					}
				}
			}
		})
	}

	/*
	// Replace any missing volumes with fill volumes
	private def addFillVolume(
		mixture_ll: List[List[XO]],
		fillVolume_l: List[LiquidVolume]
	): List[List[X]] = {
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
	
	/*def printMixtureCsv(ll: List[List[X]]): Unit = {
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
	
	private def printDestinationMixtureCsv(ll: List[(WellInfo, List[X])]): Unit = {
		if (ll.isEmpty) return
		var i = 1
		val header = (1 to ll.head._2.length).toList.map(n => "\"reagent"+n+"\",\"volume"+n+"\",\"tip"+n+"\"").mkString(""""plate","well","tip",""", ",", "")
		println(header)
		for ((wellInfo, l) <- ll) {
			val x = for (X(sv, volume, tip_?) <- l) yield {
				val well = sv.source.l.head.well
				val y = for {
					aliquot <- state0.well_aliquot_m.get(well).asRs("no liquid found in source")
				} yield {
					val flat = AliquotFlat(aliquot)
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
	
	private def flattenSteps(item: TitrateItem): List[TitrateStepParams] = {
		item match {
			case TitrateItem_And(l) => l.flatMap(flattenSteps).distinct
			case TitrateItem_Or(l) => l.flatMap(flattenSteps).distinct
			case TitrateItem_SourceVolume(step, _, _) => List(step)
		}
	}

}