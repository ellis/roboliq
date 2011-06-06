package roboliq.robot

import scala.collection
import scala.collection.immutable
import scala.collection.mutable

import roboliq.parts._


object RobotState {
	val empty = new RobotState(None, immutable.Map(), immutable.Map())
	
	def getSite(state: IRobotState, part: Part): Option[Site] = {
		state.sites.get(part) match {
			case opt @ Some(site) => opt
			case None =>
				state.prev_? match {
					case None => None
					case Some(prev) => getSite(prev, part)
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

trait IRobotState {
	val prev_? : Option[IRobotState]
	val sites: collection.Map[Part, Site]
	val wellStates: collection.Map[Well, WellState]
	
	def toImmutable: RobotState
	
	def getSite(part: Part): Option[Site] = RobotState.getSite(this, part)
	def getWellState(well: Well): WellState = RobotState.getWellState(this, well)
	
	def getLiquid(well: Well): Liquid = getWellState(well).liquid
}

class RobotState(
		val prev_? : Option[RobotState],
		val sites: immutable.Map[Part, Site],
		val wellStates: immutable.Map[Well, WellState]
) extends IRobotState {
	def toImmutable: RobotState = this
}

class RobotStateBuilder(val prev : RobotState) extends IRobotState {
	val prev_? = Some(prev)
	val sites = new mutable.HashMap[Part, Site]
	val wellStates = new mutable.HashMap[Well, WellState]
	
	def addLiquid(well: Well, liquid: Liquid, nVolume: Double) {
		wellStates(well) = getWellState(well).add(liquid, nVolume)
	}
	def removeLiquid(well: Well, nVolume: Double) {
		wellStates(well) = getWellState(well).remove(nVolume)
	}
	def fillWells(wells: Traversable[Well], liquid: Liquid, nVolume: Double) {
		for (well <- wells)
			addLiquid(well, liquid, nVolume)
	}
	
	def toImmutable = new RobotState(prev_?, sites.toMap, wellStates.toMap)
}
