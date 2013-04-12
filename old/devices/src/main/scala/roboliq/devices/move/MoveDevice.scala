package roboliq.devices.move

import roboliq.common._
import roboliq.commands.move._


trait MoveDevice extends Device {
	def addKnowledge(kb: KnowledgeBase) {
		
	}
	
	def getRomaId(args: L3A_MovePlateArgs): Result[Int] = {
		Success(0)
	}
}