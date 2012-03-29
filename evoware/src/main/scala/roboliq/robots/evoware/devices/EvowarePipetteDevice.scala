package roboliq.robots.evoware.devices

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._
import roboliq.devices.pipette._
import roboliq.robots.evoware._


abstract class EvowarePipetteDevice extends PipetteDevice {
	val config: PipetteDeviceConfig
			
	type Config = EvowarePipetteConfig
	type State = EvowarePipetteState
	
	def getLabel(kb: KnowledgeBase): String = "pipetter"

	def createConfigAndState0(): Result[Tuple2[Config, State]] = {
		val conf = new Config
		val state = new State
		Success(conf, state)
	}

	def addKnowledge(kb: KnowledgeBase) = {
		config.tips.foreach(tip => {
			kb.addObject(tip)
		})
	}

	def areTipsDisposable: Boolean
	
	def getTipAspirateVolumeMin(tip: TipStateL2, liquid: Liquid): Double = {
		tip.model_?.map(_.nVolumeAspirateMin).getOrElse(0.0)
	}
	
	def getTipHoldVolumeMax(tip: TipStateL2, liquid: Liquid): Double = {
		tip.model_?.map(tipModel => {
			val nExtra = WashIntensity.max(tip.cleanDegreePending, liquid.group.cleanPolicy.exit) match {
				case WashIntensity.Decontaminate => tipModel.nVolumeDeconExtra
				case _ => tipModel.nVolumeWashExtra
			}
			tipModel.nVolume - nExtra
		}).getOrElse(0.0)
	}
	
	def chooseTipWellPairs(tips: SortedSet[Tip], wells: SortedSet[Well], wellPrev_? : Option[Well]): Seq[Tuple2[Tip, Well]] = {
		val tips2 = tips.toIndexedSeq
		val wells2 = wells.toSeq.zipWithIndex
		for ((well, i) <- wells2) yield {
			val i2 = i % tips2.size
			tips2(i2) -> well
		}
	}
		
	/**
	 * Ensure that all items use the same policy and same tip sizes
	 */
	def canBatchSpirateItems(states: StateMap, lTwvp: List[TipWellVolumePolicy]): Boolean = {
		// Ensure that all items use the same policy and same tip sizes
		val b1 = lTwvp match {
			case Nil => true
			case x :: xs =>
				val tipState = x.tip.state(states)
				xs.forall(twvp => twvp.policy == x.policy && twvp.tip.state(states).model_? == tipState.model_?)
		}
		// Ensure that all intra-tip distances are equal to the intra-well distances
		val b2 = roboliq.robots.evoware.Utils.equidistant(lTwvp)
		// Ensure that all wells are in the same column
		val b3 = WellGroup(lTwvp.map(_.well)).splitByCol().size == 1
		
		// FIXME: for debug only
		//if (lTwvp.size == 2 && lTwvp.head.well.index == 0) {
			//println("!!!!!!!!!!!")
			//println((lTwvp, b1, b2, b3))
		//}
		
		b1 && b2 && b3
	}
	
	def canBatchMixItems(states: StateMap, lTwvp: List[TipWellMix]): Boolean = {
		// Ensure that all items use the same policy, tip sizes, and mix repetitions
		val b1 = lTwvp match {
			case Nil => true
			case x :: xs =>
				val tipState = x.tip.state(states)
				xs.forall(twvp => twvp.mixSpec.mixPolicy == x.mixSpec.mixPolicy && twvp.tip.state(states).model_? == tipState.model_? && twvp.mixSpec.nCount == x.mixSpec.nCount)
		}
		// Ensure that all intra-tip distances are equal to the intra-well distances
		val b2 = roboliq.robots.evoware.Utils.equidistant(lTwvp)
		// Ensure that all wells are in the same column
		val b3 = WellGroup(lTwvp.map(_.well)).splitByCol().size == 1
		
		b1 && b2 && b3
	}


	/*
	private case class KeySpirate(plate: Plate, iTipType: Int) {
		def this(item: L2A_SpirateItem) = this(item.well.holder, item.tip.index / 4)
	}
	def batchesForAspirate(items: Seq[L2A_SpirateItem]): Seq[Seq[L2A_SpirateItem]] = {
		items.groupBy(item => new KeySpirate(item)).values.toSeq
	}
	def batchesForDispense(items: Seq[L2A_SpirateItem]): Seq[Seq[L2A_SpirateItem]] = {
		items.groupBy(item => new KeySpirate(item)).values.toSeq
	}
	def batchesForClean(tcs: Seq[Tuple2[TipConfigL2, WashIntensity.Value]]): Seq[Seq[Tuple2[TipConfigL2, WashIntensity.Value]]] = Seq(tcs)
	def batchesForMix(items: Seq[L2A_MixItem]): Seq[Seq[L2A_MixItem]] = {
		items.groupBy(item => KeySpirate(item.well.holder, item.tip.index / 4)).values.toSeq
	}
	*/
}

class EvowarePipetteConfig extends ObjConfig
class EvowarePipetteState extends ObjState
