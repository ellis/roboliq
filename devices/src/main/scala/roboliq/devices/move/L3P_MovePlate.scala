package roboliq.devices.move

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands.move._
import roboliq.compiler._


class L3P_MovePlate(device: MoveDevice) extends CommandCompilerL3 {
	type CmdType = L3C_MovePlate
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): CompileResult = {
		val iRoma = device.getRomaId(cmd.args) match {
			case Left(lsError) => return CompileError(cmd, lsError)
			case Right(iRoma) => iRoma
		}
		val (lidHandling, locationLid) = cmd.args.lidHandlingSpec_? match {
			case None => (LidHandling.NoLid, "")
			case Some(spec) => spec match {
				case LidRemoveSpec(location) => (LidHandling.RemoveAtSource, location)
				case LidCoverSpec(location) => (LidHandling.CoverAtSource, location)
			}
		}
		val args2 = L2A_MovePlateArgs(
			iRoma, // 0 for RoMa1, 1 for RoMa2
			cmd.args.plate,
			cmd.args.location,
			lidHandling,
			locationLid
		)
		CompileTranslation(cmd, Seq(L2C_MovePlate(args2)))
	}
}
