package roboliq.robot

import scala.collection.Map
import scala.collection.immutable
import scala.collection.mutable

import roboliq.parts._


class RobotState(val prev_? : Option[RobotState], val sites: immutable.Map[Part, Site]) {
	def getSite(part: Part) = RobotState.getSite(part, sites, prev_?)
}

object RobotState {
	def getSite(part: Part, sites: Map[Part, Site], prev_? : Option[RobotState]): Option[Site] = {
		sites.get(part) match {
			case Some(site) => Some(site)
			case None =>
				prev_? match {
					case None => None
					case Some(prev) => getSite(part, prev.sites, prev.prev_?)
				}
		}
	}
}

class RobotStateBuilder(val prev_? : Option[RobotState]) {
	val sites = new mutable.HashMap[Part, Site]
	
	def getSite(part: Part) = RobotState.getSite(part, sites, prev_?)
	
	def toState = new RobotState(prev_?, sites.toMap)
}
