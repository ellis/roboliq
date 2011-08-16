package roboliq.devices.pipette

import roboliq.devices._

import scala.collection.mutable.HashSet
import roboliq.compiler._
import roboliq.level3._


class Compiler_PipetteCommand(kb: KnowledgeBase, cmd: PipetteCommand) {
	val items = cmd.items
	
	// Return a new KnowledgeBase, a list of errors, and a set of T2 tokens
	def addKnowledge() {
		// Add sources to KB
		items.foreach(_.src match {
			case WPL_Well(o) => kb.addWell(o, true)
			case WPL_Plate(o) => kb.addPlate(o, true)
			case WPL_Liquid(o) => kb.liqs += o
		})
		// Add destinations to KB
		items.foreach(_.dest match {
			case WP_Well(o) => kb.addWell(o, false)
			case WP_Plate(o) => kb.addPlate(o, false)
		})
	}
	
	def checkParams(): Seq[String] = {
		val srcs = Set() ++ items.map(_.src)
		if (srcs.size == 0)
			return ("must have one or more sources") :: Nil

		val dests = Set() ++ items.map(_.dest)
		if (dests.size == 0)
			return ("must have one or more destinations") :: Nil
		
		// Check validity of source/dest pairs
		val dests2 = new HashSet[Object]
		for (item <- items) {
			val destWells = getWells(item.dest)
			val o = item.dest match {
				case WP_Well(o) => o 
				case WP_Plate(o) => o
			}
			if (dests2.contains(o) || destWells.exists(dests2.contains))
				return ("a destination is not allowed to occur multiple times") :: Nil
			dests2 += o
			dests2 ++= destWells
			// If the source is a plate, the destination must be too
			item.src match {
				case WPL_Plate(plate1) =>
					item.dest match {
						case WP_Plate(plate2) =>
							val pd1 = kb.getPlateData(plate1)
							val pd2 = kb.getPlateData(plate1)
							val (wells1, wells2) = (pd1.wells, pd2.wells)
							if (wells1.isDefined && wells2.isDefined) {
								if (wells1.get.size != wells2.get.size)
									return ("source and destination plates must have the same number of wells") :: Nil
							}
						case _ =>
							return ("when source is a plate, destination must also be a plate") :: Nil
					}
				case _ =>
			}
			
			if (item.nVolume <= 0)
				return ("volume must be > 0") :: Nil
		}
		
		Nil
	}
	
	def compile(): Seq[Command] = {
		/*
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
		*/
		
		var bAllOk = true
		val items2: Seq[L2_PipetteItem] = items.flatMap(item => {
			val srcWells1 = getWells1(getWells(item.src))
			val destWells1 = getWells1(getWells(item.dest))
			if (srcWells1.isEmpty || destWells1.isEmpty) {
				bAllOk = false
				Nil
			}
			else {
				destWells1.map(dest1 => new L2_PipetteItem(srcWells1, dest1, item.nVolume))
			}
		})
		
		println(items2)
		
		if (bAllOk) {
			val args = new L2_PipetteArgs(
					items2,
					cmd.mixSpec_?,
					sAspirateClass_? = None,
					sDispenseClass_? = None,
					sMixClass_? = None,
					sTipKind_? = None,
					fnClean_? = None)
			L2_PipetteCommand(args) :: Nil
		}
		else {
			Nil
		}
	}
	
	private def getWells(well: Well): Set[Well] = Set(well)
	private def getWells(plate: Plate): Set[Well] = kb.getPlateData(plate).wells.get_? match {
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
	
	private def getWells1(wells3: Set[Well]): Set[roboliq.parts.Well] = {
		if (wells3.forall(kb.map31.contains)) {
			wells3.map(well3 => kb.map31(well3).asInstanceOf[roboliq.parts.Well])
		}
		else {
			Set()
		}
	}
}
