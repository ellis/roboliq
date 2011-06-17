/*package evoware

import scala.collection.Map
import scala.collection.immutable
import scala.collection.mutable

import roboliq.parts._


class EvowareRobotState(val parent_? : Option[EvowareRobotState], val sites: immutable.Map[Part, Site]) {
	def getSite(part: Part) = EvowareRobotState.getSite(part, sites, parent_?)
}

object EvowareRobotState {
	def getSite(part: Part, sites: Map[Part, Site], parent_? : Option[EvowareRobotState]): Option[Site] = {
		sites.get(part) match {
			case Some(site) => Some(site)
			case None =>
				parent_? match {
					case None => None
					case Some(parent) => getSite(part, parent.sites, parent.parent_?)
				}
		}
	}
}

class EvowareRobotStateBuilder(val parent_? : Option[EvowareRobotState]) {
	val sites = new mutable.HashMap[Part, Site]
	
	def getSite(part: Part) = EvowareRobotState.getSite(part, sites, parent_?)
	
	def toState = new EvowareRobotState(parent_?, sites.toMap)
}
*/