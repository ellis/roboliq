package meta.fixed

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import concrete._

class Well(val index: Int) {
	var liquid: Liquid = null
	var nVolume = 0.0
}
object Well {
	def apply(index: Int): Well = apply(index, Liquid.empty, 0)
	def apply(index: Int, liquid: Liquid, nVolume: Double): Well = {
		val well = new Well(index)
		well.liquid = liquid
		well.nVolume = nVolume
		well
	}
}
class AspirateStrategy(val sName: String)
class DispenseStrategy(val sName: String, val bEnter: Boolean)
class Liquid(val sName: String, val bContaminates: Boolean)
object Liquid {
	val empty = new Liquid(null, false)
}
class Tip(val index: Int, val nVolumeMin: Double, val nVolumeMax: Double)

class Settings(val tips: Array[Tip], val liquids: Array[Liquid])

class OneOverConcrete(settings: Settings) {
	class TipState(tip: Tip) {
		var bFree = true
		var bContaminated = true
		val dests = new ArrayBuffer[Tuple2[Well, Double]]
		var nVolume = 0.0
		
		//def index = tip.index
		def nVolumeMin = tip.nVolumeMin
		def nVolumeMax = tip.nVolumeMax
	}
	
	case class AspirateData(val tip: TipState, val src: Well, val nVolume: Double, val strategy: AspirateStrategy)
	case class DispenseData(val tip: TipState, val src: Well, val nVolume: Double, val strategy: DispenseStrategy)
/*sealed class Plate(val rows: Int, val cols: Int)

sealed class PipettingRule(val name: String)

sealed class Token
case class Aspirate(volumes: Array[Double], plate: Plate, loc: Int, wells: Set[Int], rule: PipettingRule) extends Token
case class Dispense(volumes: Array[Double], plate: Plate, loc: Int, wells: Set[Int], rule: PipettingRule) extends Token
*/
	
	def pipetteLiquid(srcs: Array[Well], dests: Array[Well], volumes: Array[Double], aspirateStrategy: AspirateStrategy, dispenseStrategy: DispenseStrategy): List[Token] = {
		assert(dests.size == volumes.size)
		
		val liquid = srcs(0).liquid
		assert(liquid != null)
		// Make sure that all the source wells contain the same liquid
		assert(srcs.forall(_.liquid == liquid))
		
		// Contamination scenarios:
		// dispense contaminates: wash after each dispense
		// aspirate contaminates, dispense does not: wash before each subsequent aspirate
		// aspirate does not contaminate, dispense doesn't contaminate: no washing required
		
		val bDestContaminates = dests.exists(_.liquid.bContaminates)
		
		val bAspirateContaminates = liquid.bContaminates
		val bDispenseContaminates = dispenseStrategy.bEnter && bDestContaminates
		
		val tips: Array[TipState] = settings.tips.map(new TipState(_)).toArray
		val aspirates = new ArrayBuffer[AspirateData]
		val dispenses = new ArrayBuffer[AspirateData]
		
		def handle() {
			aspirates ++= handleAspirates(tips, srcs, aspirateStrategy)
			handleDispense(tips)
		}
		
		if (bDispenseContaminates) {
			// For each destination well:
			//  - assert that there are tips which can completely hold the desired volume
			//  - try to select the first free tip which can completely hold the desired volume:
			//     - if available, indicate that the volume should be drawn into that tip from the next available source
			//  - otherwise append clean/aspirate/dispense tokens
			//val anAspirateVolumes = new Array[Double](settings.tips.size)
			//var iTip = 0
			var iSrc = 0
			def incSrcIndex() { iSrc += 1; if (iSrc >= srcs.size) iSrc = 0; }
			
			for (i <- 0 until dests.size) {
				val dest = dests(i)
				val nVolume = volumes(i)
				
				assert(tipsExistForVolume(nVolume))
				tips.find(tipIsFreeAndHasVolume(nVolume)) match {
					case Some(tip) =>
						tip.nVolume += nVolume
						tip.dests += Tuple2(dest, nVolume)
					case None =>
						handle()
				}
			}
			handle()
		}
		else if (bAspirateContaminates) {
			var iSrc = 0
			def incSrcIndex() { iSrc += 1; if (iSrc >= srcs.size) iSrc = 0; }
			
			for (i <- 0 until dests.size) {
				val dest = dests(i)
				val nVolume = volumes(i)
				
				assert(tipsExistForVolume(nVolume))
				tips.find(tipIsFreeAndHasVolume(nVolume)) match {
					case Some(tip) =>
						tip.nVolume += nVolume
						tip.dests += Tuple2(dest, nVolume)
					case None =>
						handle()
				}
			}
			handle()
		}
		
		aspirates.foreach(println)
		
		List()
	}

	def tipIsFreeAndHasVolume(nVolume: Double)(tip: TipState): Boolean =
		(tip.bFree && nVolume + tip.nVolume >= tip.nVolumeMin && nVolume + tip.nVolume <= tip.nVolumeMax)
	
	def handleAspirates(tips: Array[TipState], srcs: Array[Well], aspirateStrategy: AspirateStrategy): ArrayBuffer[AspirateData] = {
		val aspirates = new ArrayBuffer[AspirateData]
		// Cases:
		// Case 1: tips size == srcs size:
		// Case 2: tips size < srcs size:
		// Case 3: tips size > srcs size:
		// -----
		// The sources should be chosen according to this algorithm:
		// - sort the sources by volume descending (secondary sort key is index order)
		// - keep the top tips.size() entries
		// Repeat the sorting each time all sources have been used (e.g. when there are more tips than sources)
		var iTip = 0
		while (iTip < tips.size) {
			// sort the sources by volume descending (secondary sort key is index order)
			def order(a: Well, b: Well) = (a.nVolume > b.nVolume) || (a.nVolume == b.nVolume && a.index < b.index) 
			// keep the top tips.size() entries ordered by index
			val srcs2 = srcs.sortWith(order).take(tips.size).sortWith(_.index < _.index)
			assert(!srcs2.isEmpty)
			
			var iSrc = 0
			while (iTip < tips.size && iSrc < srcs2.size) {
				val tip = tips(iTip)
				val src = srcs2(iSrc)
				val nVolume = tip.nVolume
				
				aspirates += new AspirateData(tip, src, nVolume, aspirateStrategy)
				src.nVolume -= nVolume
				tip.bContaminated |= src.liquid.bContaminates

				iTip += 1
				iSrc += 1
				if (iSrc == srcs.size)
					iSrc = 0
			}
		}
		aspirates
	}
	
	def handleDispense(tips: Array[TipState]) {
		for (tip <- tips) {
			tip.bFree = true
			tip.nVolume = 0
			//tip.bContaminated = ...
		}
	}
	
	/*
	def freeTipIndexesForVolume(tips: Array[TipState], nVolume: Double): Array[Int] = {
		val ai = new mutable.ArrayBuffer[Int]
		var i = 0
		for (tip <- tips) {
			if (nVolume >= tip.nVolumeMin && nVolume <= tip.nVolumeMax)
				ai += i
			i += 1
		}
		ai.toArray
	}
	*/
	
	def tipsExistForVolume(nVolume: Double): Boolean = {
		settings.tips.exists(tip => nVolume >= tip.nVolumeMin && nVolume <= tip.nVolumeMax)
	}
}
