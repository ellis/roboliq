package roboliq.core

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.ArrayBuffer
import scala.language.higherKinds
import scala.language.implicitConversions
import roboliq.entities.WorldState
import roboliq.entities.EntityBase
import roboliq.entities.Aliquot
import roboliq.entities.Well
import roboliq.core.RsResult


/**
 * ProtocolState should carry the eb, current world state, current command, current instruction, warnings and errors, and various HDF5 arrays
 */
case class ProtocolContext(
	eb: EntityBase,
	state: WorldState,
	command: List[Int],
	instruction: List[Int],
	warning_r: List[String],
	error_r: List[String],
	well_aliquot_r: List[(List[Int], Well, Aliquot)]
) {
	def setState(state: WorldState): ProtocolContext =
		copy(state = state)
	
	def setCommand(idx: List[Int]): ProtocolContext =
		copy(command = idx, instruction = Nil)

	def setInstruction(idx: List[Int]): ProtocolContext =
		copy(instruction = idx)
}

case class ProtocolResult[+A](context: ProtocolContext, res: RsResult[A])
