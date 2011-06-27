package roboliq.robot

import scala.collection
import scala.collection.immutable
import scala.collection.mutable
import scala.collection.mutable.HashMap

import roboliq.parts._
import roboliq.tokens._


class SiteNode(val part: Part, site: Site, children: Map[Int, SiteNode])

trait IRobotState {
	val prev_? : Option[IRobotState]
	val mapPartToSite: collection.Map[Part, Option[Site]]
	//val mapSiteToPart: collection.Map[Site, Part]
	val mapPartToChildren: collection.Map[Part, immutable.Map[Int, Part]]
	val tipStates: collection.Map[Tip, TipState]
	val wellStates: collection.Map[Well, WellState]
	
	def toImmutable: RobotState
	
	def getSite(part: Part): Option[Site] = RobotState.getSite(this, part)
	def getSiteList(part: Part): List[Site] = RobotState.getSiteList(this, part)
	def getTipState(tip: Tip): TipState = RobotState.getTipState(this, tip)
	def getWellState(well: Well): WellState = RobotState.getWellState(this, well)
	
	def getLiquid(well: Well): Liquid = getWellState(well).liquid
}

class RobotState(
		val prev_? : Option[RobotState],
		val mapPartToSite: immutable.Map[Part, Option[Site]],
		val tipStates: immutable.Map[Tip, TipState],
		val wellStates: immutable.Map[Well, WellState]
) extends IRobotState {
	//val mapSiteToPart: immutable.Map[Site, Part] = mapPartToSite.map(pair => pair._2 -> pair._1)
	val mapPartToChildren: immutable.Map[Part, immutable.Map[Int, Part]] = {
		val map = new HashMap[Part, immutable.Map[Int, Part]]
		for ((part, site_?) <- mapPartToSite) {
			site_? match {
				case None =>
				case Some(site) =>
					val parent = site.parent
					val mapChildren = map.getOrElse(parent, Map())
					map(parent) = mapChildren + (site.index -> part)
			}
		}
		map.toMap
	}
	def toImmutable: RobotState = this
}

// REFACTOR: Why aren't these in IRobotState? -- ellis, 2011-06-27
object RobotState {
	val empty = new RobotState(None, immutable.Map(), immutable.Map(), immutable.Map())
	
	def getSite(state: IRobotState, part: Part): Option[Site] = {
		state.mapPartToSite.get(part) match {
			case Some(None) => None
			case Some(opt @ Some(site)) => opt
			case None =>
				state.prev_? match {
					case None => None
					case Some(prev) => getSite(prev, part)
				}
		}
	}
	
	def getSiteList(state: IRobotState, part: Part): List[Site] = {
		getSite(state, part) match {
			case None => Nil
			case Some(site) =>
				site :: (if (site.parent == null) Nil else getSiteList(state, site.parent))
		}
	}
	
	def getTipState(state: IRobotState, tip: Tip): TipState = {
		state.tipStates.get(tip) match {
			case Some(tipState) => tipState
			case None =>
				state.prev_? match {
					case None => TipState(tip)
					case Some(prev) => getTipState(prev, tip)
				}
		}
	}

	def getWellState(state: IRobotState, well: Well): WellState = {
		state.wellStates.get(well) match {
			case Some(wellState) => wellState
			case None =>
				state.prev_? match {
					case None => new WellState(well, Liquid.empty, 0)
					case Some(prev) => getWellState(prev, well)
				}
		}
	}
}

class RobotStateBuilder(val prev : RobotState) extends IRobotState {
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
	def dispense(twvd: TipWellVolumeDispense) {
		import twvd._
		val tipState = getTipState(tip)
		val wellState = getWellState(well)
		tipStates(tip) = dispenseKind match {
			case DispenseKind.WetContact => tipState.dispenseIn(wellState.liquid, nVolume)
			case _ => tipState.dispenseFree(nVolume)
		}
		addLiquid0(well, tipState.liquid, nVolume)
	}


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
