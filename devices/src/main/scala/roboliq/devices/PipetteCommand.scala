package roboliq.devices

import scala.collection.mutable.HashSet
import roboliq.level3._
import roboliq.devices.pipette.L2_PipetteItem

case class PipetteCommand(items: Seq[PipetteItem], mixSpec_? : Option[MixSpec] = None) extends Command

/*
 * TipSpec:
 * - type
 * - what to do with tips before aspirate (e.g., discard, clean, nothing...)
 */
//class TipSpec(kind: String = null)
case class MixSpec(val nVolume: Double, val nCount: Int)
sealed class PipetteItem(
		val src: WellOrPlateOrLiquid,
		val dest: WellOrPlate,
		val nVolume: Double)
//case class PipetteItemLP(src: Liquid, dest: Plate)

case class CommandError(val message: String, val obj_? : Option[Object] = None)

class PipetteCommandHandler(kb: KnowledgeBase, cmd: PipetteCommand) {
	val items = cmd.items
	
	// Return a new KnowledgeBase, a list of errors, and a set of T2 tokens
	def exec(): Seq[CommandError] = {
		val srcs = Set() ++ items.map(_.src)
		if (srcs.size == 0)
			return CommandError("must have one or more sources") :: Nil
		// Add sources to KB
		srcs.foreach(_ match {
			case WPL_Well(o) => kb.addWell(o, true)
			case WPL_Plate(o) => kb.addPlate(o, true)
			case WPL_Liquid(o) => kb.liqs += o
		})

		val dests = Set() ++ items.map(_.dest)
		if (dests.size == 0)
			return CommandError("must have one or more destinations") :: Nil
		// Add destinations to KB
		dests.foreach(_ match {
			case WP_Well(o) => kb.addWell(o, false)
			case WP_Plate(o) => kb.addPlate(o, false)
		})
		
		// Check validity of source/dest pairs
		val dests2 = new HashSet[Object]
		for (item <- items) {
			val destWells = getWells(item.dest)
			val o = item.dest match {
				case WP_Well(o) => o 
				case WP_Plate(o) => o
			}
			if (dests2.contains(o) || destWells.exists(dests2.contains))
				return CommandError("a destination is not allowed to occur multiple times") :: Nil
			dests2 += o
			dests2 ++= destWells
			// If the source is a plate, the destination must be too
			item.src match {
				case WPL_Plate(plate1) =>
					item.dest match {
						case WP_Plate(plate2) =>
							val (wells1, wells2) = (plate1.wells_?, plate2.wells_?)
							if (wells1.isDefined && wells2.isDefined) {
								if (wells1.get.size != wells2.get.size)
									return CommandError("source and destination plates must have the same number of wells") :: Nil
							}
						case _ =>
							return CommandError("when source is a plate, destination must also be a plate") :: Nil
					}
				case _ =>
			}
			
			if (item.nVolume <= 0)
				return CommandError("volume must be > 0") :: Nil
		}
		
		// check whether we have all the information we need
		def checkPart(part: Part): Boolean = {
			// Do we know its location?
			kb.mapPartToLoc.contains(part) || 
				(
					part.parent_?.isDefined && 
					part.index_?.isDefined &&
					checkPart(part.parent_?.get)
				)
		}
		def checkWell(well: Well): Boolean = {
			if (!checkPart(well))
				return false
			val wk = kb.wellKnowledge(well) 
			if (wk.bRequiresIntialLiq_?.isEmpty)
				return false
			if (wk.bRequiresIntialLiq_?.get == true) {
				if (wk.bRequiresIntialLiq_?.isEmpty)
					return false
			}
			return true
		}
		def checkPlate(plate: Plate): Boolean = {
			if (!checkPart(plate))
				return false
			if (!plate.nRows_?.isDefined || !plate.nCols_?.isDefined || !plate.wells_?.isDefined)
				return false
			if (!plate.wells_?.get.forall(checkWell))
				return false
			return true
		}
		def checkLiquid(liquid: Liquid): Boolean = {
			val wells = kb.getLiqWells(liquid)
			if (wells.isEmpty)
				false
			else
				wells.forall(checkWell)
		}
		
		val b = items.forall(item => {
			val dest = item.dest
			
			item.src match {
				case WPL_Well(o) => checkWell(o)
				case WPL_Plate(o) => checkPlate(o)
				case WPL_Liquid(o) => checkLiquid(o)
			}
			
			item.dest match {
				case WP_Well(o) => checkWell(o)
				case WP_Plate(o) => checkPlate(o)
			}
		})
		
		if (!b)
			return Nil
		
		val items2: Seq[L2_PipetteItem] = items.flatMap(item => {
			val srcWells = getWells(item.src)
			val destWells = getWells(item.dest)
			for (dest <- destWells) yield {
				L2_PipetteItem(srcWells, dest, item.nVolume, mixSpec_? = cmd.mixSpec_?)
			}
		})
		
		println(items2)
		
		Nil
	}
	
	private def getWells(well: Well): Set[Well] = Set(well)
	private def getWells(plate: Plate): Set[Well] = plate.wells_? match {
		case None => Set()
		case Some(wells) => wells.toSet
	}
	private def getWells(liquid: Liquid): Set[Well] = kb.getLiqWells(liquid)
	private def getWells(wpl: WellOrPlateOrLiquid): Set[Well] = wpl match {
		case WPL_Well(o) => getWells(o)
		case WPL_Plate(o) => getWells(o)
		case WPL_Liquid(o) => getWells(o)
	}			
	private def getWells(wp: WellOrPlate): Set[Well] = wp match {
		case WP_Well(o) => getWells(o)
		case WP_Plate(o) => getWells(o)
	}			
}
