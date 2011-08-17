package roboliq.devices.pipette

import scala.collection
import scala.collection.immutable
import scala.collection.mutable
import scala.collection.mutable.HashMap

import roboliq.common._
//import roboliq.parts._
//import roboliq.tokens._


class SiteNode(val part: Part, site: Site, children: Map[Int, SiteNode])


case class PipetteStateL1(
		val mapPartToSite: Map[Part, Site],
		val tipStates: Map[Tip, TipState],
		val wellStates: Map[Well, WellState]
) {
	def getSite(part: Part): Option[Site] = mapPartToSite.get(part)
	
	def getSiteList(part: Part): List[Site] = {
		getSite(part) match {
			case None => Nil
			case Some(site) =>
				site :: (if (site.parent == null) Nil else getSiteList(site.parent))
		}
	}
	
	val mapPartToChildren: immutable.Map[Part, immutable.Map[Int, Part]] = {
		val map = new HashMap[Part, immutable.Map[Int, Part]]
		for ((part, site) <- mapPartToSite) {
			val parent = site.parent
			val mapChildren = map.getOrElse(parent, Map())
			map(parent) = mapChildren + (site.index -> part)
		}
		map.toMap
	}
}

// REFACTOR: Why aren't these in IRobotState? -- ellis, 2011-06-27
object PipetteState {
	val empty = new PipetteState(Map(), Map(), Map())
	
}

class PipetteStateBuilder(val prev : RobotState) extends IRobotState {
	val prev_? = Some(prev)
	val mapPartToSite = new mutable.HashMap[Part, Option[Site]]
	val mapPartToChildren = new mutable.HashMap[Part, immutable.Map[Int, Part]]
	val tipStates = new mutable.HashMap[Tip, TipState]
	val wellStates = new mutable.HashMap[Well, WellState]
	
	// Remove part from its current site
	def removePartFromParent0(part: Part) {
		getSite(part) match {
			case None =>
			case Some(site) =>
				// Indicate that the part has no site
				mapPartToSite(part) = None
				// Remove the part from it's parent's list of children
				mapPartToChildren(site.parent) = mapPartToChildren(site.parent).filter(_._2 ne part) 
		}
	}
	
	def movePartTo(part: Part, dest: Part, index: Int) {
		removePartFromParent0(part)
		// Make sure that no other part is already at the given location
		assert(!mapPartToChildren.contains(dest) || !mapPartToChildren(dest).contains(index))
		mapPartToSite(part) = Some(Site(dest, index))
		mapPartToChildren(dest) = mapPartToChildren.getOrElse(dest, Map()) + (index -> part)  
	}

	/*def placePlateAt(plate: Plate, iGrid: Int, iSite: Int) {
		val carrier = 
		sites(plate) = new Site(carrier, iSite)
	}*/

	// Aspirate liquid into the tip -- prefer aspirate()
	def addLiquid0(tip: Tip, liquid: Liquid, nVolume: Double) {
		tipStates(tip) = getTipState(tip).aspirate(liquid, nVolume)
	}

	// Remove liquid from well and put it in tip
	def aspirate(twv: TipWellVolume) {
		import twv._
		val liquid = getWellState(well).liquid
		addLiquid0(tip, liquid, nVolume)
		removeLiquid0(well, nVolume)
	}

	// Remove liquid from tip -- prefer dispense()
	def removeLiquid0(tip: Tip, nVolume: Double) {
		tipStates(tip) = getTipState(tip).dispenseFree(nVolume)
	}

	// Remove liquid from tip and put it in well
	def dispense(twvp: TipWellVolumePolicy) {
		import twvp._
		val tipState = getTipState(tip)
		val wellState = getWellState(well)
		tipStates(tip) = policy.pos match {
			case PipettePosition.WetContact => tipState.dispenseIn(nVolume, wellState.liquid)
			case _ => tipState.dispenseFree(nVolume)
		}
		addLiquid0(well, tipState.liquid, nVolume)
	}
	
	/*def mix(twv: TipWellVolume) {
		aspirate(twv)
		val twvd = new TipWellVolumePolicy(twv.tip, twv.well, twv.nVolume, PipettePolicy(PipettePosition.WetContact))
		dispense(twvd)
	}*/

	def addLiquid0(well: Well, liquid: Liquid, nVolume: Double) {
		wellStates(well) = getWellState(well).add(liquid, nVolume)
	}
	def removeLiquid0(well: Well, nVolume: Double) {
		wellStates(well) = getWellState(well).remove(nVolume)
	}

	def fillWells(wells: Traversable[Well], liquid: Liquid, nVolume: Double) {
		for (well <- wells)
			addLiquid0(well, liquid, nVolume)
	}
	
	def cleanTip(tip: Tip, cleanDegree: CleanDegree.Value) {
		tipStates(tip) = getTipState(tip).clean(cleanDegree)
	}

	def toImmutable = new RobotState(prev_?, mapPartToSite.toMap, tipStates.toMap, wellStates.toMap)
}
