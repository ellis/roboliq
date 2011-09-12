package roboliq.commands.shake

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.commands._


case class L4C_Shake(args: L4A_ShakeArgs) extends CommandL4 {
	type L3Type = L3C_Shake

	def addKnowledge(kb: KnowledgeBase) {
		// TODO: note that plate will occupy the target location
		// TODO: request plate compatibility with this device
	}
	
	def toL3(states: RobotState): Either[Seq[String], L3Type] = {
		args.toL3(states) match {
			case Left(lsErrors) => Left(lsErrors)
			case Right(args3) => Right(new L3C_Shake(args3))
		}
	}
}

case class L3C_Shake(args: L3A_ShakeArgs) extends CommandL3

class L4A_ShakeSetup {
	val plate = new L4A_PlateSetup
}

class L4A_ShakeArgs(
	val plate: Plate,
	val nDuration: Int,
	val idDevice_? : Option[String]
) {
	val setup = new L4A_ShakeSetup
	
	def toL3(states: RobotState): Either[Seq[String], L3A_ShakeArgs] = {
		val setupPlate = setup.plate.toL3(states) match {
			case Left(lsError) => return Left(lsError)
			case Right(o) => o
		}
		Right(new L3A_ShakeArgs(
			plate = plate.state(states).conf,
			nDuration = nDuration,
			idDevice_? = idDevice_?,
			setup = setupPlate
		))
	}
}

class L3A_ShakeArgs(
	val plate: PlateConfigL2,
	val nDuration: Int,
	val idDevice_? : Option[String],
	val setup: L3A_PlateSetup
)
