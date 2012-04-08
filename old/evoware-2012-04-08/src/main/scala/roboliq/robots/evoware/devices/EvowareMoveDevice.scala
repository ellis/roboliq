package roboliq.robots.evoware.devices

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands.move._
import roboliq.compiler._
import roboliq.devices.move._
import roboliq.robots.evoware._


class EvowareMoveDevice extends MoveDevice {
	type Config = EvowareMoveConfig
	type State = EvowareMoveState
	
	def getLabel(kb: KnowledgeBase): String = "pipetter"
	def createConfigAndState0(): Result[Tuple2[Config, State]] = {
		val conf = new Config
		val state = new State
		Success(conf, state)
	}
}

class EvowareMoveConfig extends ObjConfig
class EvowareMoveState extends ObjState
