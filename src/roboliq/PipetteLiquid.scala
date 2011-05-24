package roboliq

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

import concrete._



class Settings(val tips: Array[Tip], val tipGroups: Array[Array[Int]], val liquids: Array[Liquid])

class PipetteLiquid(settings: Settings) {
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
	case class DispenseSubunit(val tip: TipState, val dest: Well, val nVolume: Double)
	case class DispenseUnit(val subunits: Seq[DispenseSubunit])
/*sealed class Plate(val rows: Int, val cols: Int)

sealed class PipettingRule(val name: String)

sealed class Token
case class Aspirate(volumes: Array[Double], plate: Plate, loc: Int, wells: Set[Int], rule: PipettingRule) extends Token
case class Dispense(volumes: Array[Double], plate: Plate, loc: Int, wells: Set[Int], rule: PipettingRule) extends Token
*/
	class Params(
		//val settings: Settings,
		val srcs: Array[Well],
		val dests: Array[Well],
		val mapDestToVolume: Map[Well, Double],
		val aspirateStrategy: AspirateStrategy, val dispenseStrategy: DispenseStrategy	
	)
	
	class CycleState(val tips: Array[TipState]) {
		val aspirates = new ArrayBuffer[AspirateData]
		val dispenses = new ArrayBuffer[DispenseUnit]
	}
	
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
		
		def sortDests(well1: Well, well2: Well): Boolean = {
			(well1.holder.index < well2.holder.index || well1.index < well2.index)
		}
		val destsSorted = dests.sortWith(sortDests)
		
		val params = new Params(srcs, destsSorted, Map() ++ (dests zip volumes), aspirateStrategy, dispenseStrategy)
		
		if (bDispenseContaminates) {
			pipetteLiquid_DispenseContaminates(params)
		}
		else if (bAspirateContaminates) {
			pipetteLiquid_AspirateContaminates(params)
		}
		
		//aspirates.foreach(println)
		
		List()
	}
	
	private def pipetteLiquid_DispenseContaminates(params: Params) {
		import params._
		
		// For each destination well:
		//  - assert that there are tips which can completely hold the desired volume
		//  - try to select the first free tip which can completely hold the desired volume:
		//     - if available, indicate that the volume should be drawn into that tip from the next available source
		//  - otherwise append clean/aspirate/dispense tokens
		val tips: Array[TipState] = settings.tips.map(new TipState(_)).toArray
		val aspirates = new ArrayBuffer[AspirateData]
		val dispenses = new ArrayBuffer[AspirateData]
		
		def handle() {
			aspirates ++= handleAspirates(tips, srcs, aspirateStrategy)
			handleDispense(tips)
		}
		
		for (dest <- dests) {
			val nVolume = mapDestToVolume(dest)
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
	
	private def pipetteLiquid_AspirateContaminates(params: Params) {
		// Need to split into tip groups (e.g. large tips, small tips, all tips)
		// For each group, perform the pipetting and score the results
		// Pick the strategy with the best score
		for (tipGroup <- settings.tipGroups) {
			val tips = tipGroup.map(t => new TipState(settings.tips(t)))
			pipetteLiquid_AspirateContaminates_tipGroup(params, tips)
		}
	}

	private def pipetteLiquid_AspirateContaminates_tipGroup(params: Params, tips: Array[TipState]) {
		// For each dispense, pick the top-most destination wells available in the next column
		// Break off dispense batch if any tips cannot fully dispense volume
		val cycle = new CycleState(tips)
		val destsRemaining = new ArrayBuffer[Well]
		destsRemaining ++= params.dests
		var wellPrev_? : Option[Well] = None
		while (!destsRemaining.isEmpty) {
			val destsNext = getNextDestBatch(tips, destsRemaining, wellPrev_?)
			assert(!destsNext.isEmpty)

			val destsNextWithIndex = destsNext.zipWithIndex
			
			// Check whether the tips have enough free volume for their respective destinations
			val bOk = destsNextWithIndex.forall(pair => {
				val (dest, i) = pair
				val nVolume = params.mapDestToVolume(dest)
				val tip = tips(i)
				tipIsFreeAndHasVolume(nVolume)(tip)
			})
			
			if (bOk) {
				val subs = new ArrayBuffer[DispenseSubunit]
				for ((dest, i) <- destsNextWithIndex) {
					val nVolume = params.mapDestToVolume(dest)
					val tip = tips(i)
					tip.nVolume += nVolume
					tip.dests += (dest -> nVolume)
					subs += new DispenseSubunit(tip, dest, nVolume)
					destsRemaining -= dest
				}
				cycle.dispenses += new DispenseUnit(subs)
				wellPrev_? = Some(destsNext.head)
			}
		}
		
		cycle.dispenses.foreach(println)
		
		// Aspirate
		aspirate(params, cycle)
	}

	private def tipIsFreeAndHasVolume(nVolume: Double)(tip: TipState): Boolean =
		(tip.bFree && nVolume + tip.nVolume >= tip.nVolumeMin && nVolume + tip.nVolume <= tip.nVolumeMax)
	
	private def aspirate(params: Params, cycle: CycleState) {
		
	}
		
	private def handle(params: Params, state: CycleState) {
		state.aspirates ++= handleAspirates(state.tips, params.srcs, params.aspirateStrategy)
		handleDispense(state.tips)
	}
	
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
	
	def handleDispense(tips: Array[TipState]): ArrayBuffer[DispenseData] = {
		val dispenses = new ArrayBuffer[DispenseData]
		for (tip <- tips) {
			tip.bFree = true
			tip.nVolume = 0
			//tip.bContaminated = ...
		}
		dispenses
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
	
	private def getNextDestBatch(tips: Seq[TipState], wellsAvailable: Seq[Well], wellPrev_? : Option[Well]): Seq[Well] = {
		// If the previous well is defined but is on a different holder, then ignore it
		if (wellPrev_?.isDefined && !wellsAvailable.exists(_.holder == wellPrev_?.get.holder))
			getNextDestBatch(tips, wellsAvailable, None)
		else {
			val _well = wellPrev_? match {
				case Some(well) => well
				case None => wellsAvailable(0)
			}
			val holder = _well.holder
			val iCol = wellPrev_? match {
				case Some(well) => well.index / holder.nRows
				case None => 0
			}
			val wellsOnHolder = wellsAvailable.filter(_.holder == holder)
			getNextDestBatch(tips, holder, wellsOnHolder, iCol)
		}
	}
	
	// Pick the top-most destination wells available in the given column
	// If none found, loop through columns until wells are found
	private def getNextDestBatch(tips: Seq[TipState], holder: WellHolder, wellsOnHolder: Seq[Well], iCol0: Int): Seq[Well] = {
		val nRows = holder.nRows
		val nCols = holder.nCols
		var iCol = iCol0
		var wellsInCol: Seq[Well] = null
		do {
			wellsInCol = wellsOnHolder.filter(_.index / nRows == iCol).take(tips.size)
			if (wellsInCol.isEmpty) {
				iCol = (iCol + 1) % nCols
				assert(iCol != iCol0)
			}
		} while (wellsInCol.isEmpty)
		wellsInCol
	}
}
