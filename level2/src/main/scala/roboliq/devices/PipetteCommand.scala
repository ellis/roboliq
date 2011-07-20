package roboliq.devices

import roboliq.builder.parts._

class TipSpec()
class MixSpec(val nVolume: Double, val nCount: Int)
sealed class PipetteItem(
		val src: Object,
		val dest: Object,
		val nVolume: Double,
		val tipSpec_? : Option[TipSpec] = None,
		val mixSpec_? : Option[MixSpec] = None)
//case class PipetteItemLP(src: Liquid, dest: Plate)

case class CommandError(val message: String, val obj_? : Option[Object] = None)

class PipetteArgs(
	val srcs: Seq[Object],
	val dests: Seq[Object],
	val volumes: Map[Object, Double],
	val mixSpecs: Map[Object, MixSpec]
)

class PipetteCommand {
	// Return a new KnowledgeBase, a list of errors, and a set of T2 tokens
	def pipette(kb: KnowledgeBase, items: Seq[PipetteItem]): Seq[CommandError] = {
		val srcs = Set() ++ items.map(_.src)
		if (srcs.size == 0)
			return CommandError("must have one or more sources") :: Nil
		else {
			// Add sources to KB
			for (src <- srcs) {
				if (src.isInstanceOf[Liquid]) {
					val liq = src.asInstanceOf[Liquid]
					kb.liqs += liq
				}
				else if (src.isInstanceOf[Well])
					kb.addWell(src.asInstanceOf[Well], true)
				else if (src.isInstanceOf[Plate])
					kb.addPlate(src.asInstanceOf[Plate], true)
				else
					return CommandError("unknown source object") :: Nil
			}
		}

		val dests = Set() ++ items.map(_.dest)
		if (dests.size == 0)
			return CommandError("must have one or more sources") :: Nil
		else {
			//val src0 = srcs.head
			// Add destinations to KB
			for (dest <- dests) {
				if (dest.isInstanceOf[Well])
					kb.addWell(dest.asInstanceOf[Well], false)
				else if (dest.isInstanceOf[Plate])
					kb.addPlate(dest.asInstanceOf[Plate], false)
				else
					return CommandError("unknown destination object") :: Nil
			}
		}
		
		// Check validity of source/dest pairs
		for (item <- items) {
			// If the source is a plate, the destination must be too
			if (item.src.isInstanceOf[Plate]) {
				if (!item.dest.isInstanceOf[Plate]) {
					return // TODO: add error handling
				}
			}
		}
		
		// check whether we can have all the information we need
		def checkPart(part: Part): Boolean = {
			// Do we know its location?
			kb.mapPartToLoc.contains(part) || (part.parent_?.isDefined && checkPart(part.parent_?.get))
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
			val src = item.src
			val dest = item.dest
			
			if (src.isInstanceOf[Liquid])
				checkLiquid(src.asInstanceOf[Liquid])
			else if (src.isInstanceOf[Well])
				checkWell(src.asInstanceOf[Well])
			else if (src.isInstanceOf[Plate])
				checkPlate(src.asInstanceOf[Plate])
			else
				false
		})
	}
}
