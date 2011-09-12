package roboliq.robots.evoware.devices

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands.move._
import roboliq.commands.shake._
import roboliq.compiler._
import roboliq.robots.evoware.commands._


class L3P_Shake_HPShaker(location: String) extends CommandCompilerL3 {
	type CmdType = L3C_Shake
	val cmdType = classOf[CmdType]
	
	def compile(ctx: CompilerContextL3, cmd: CmdType): CompileResult = {
		val cmds = new ArrayBuffer[Command]
		val plate = cmd.args.plate
		val plateState = cmd.args.plate.state(ctx.states)
		if (plateState.location != location) {
			cmds += L3C_MovePlate(new L3A_MovePlateArgs(plate, ValueArg(location), None))
		}
		val args2 = new L12A_EvowareFactsArgs("HPShaker", "HPShaker_HP__ShakeForTime", "*271|5*30*30*30*30|2|*30|1|*30|1,2*30*30|255*27")
		cmds += L2C_EvowareFacts(args2)
		cmd.args.setup.locationFinal_?.map(location => {
			cmds += L3C_MovePlate(new L3A_MovePlateArgs(plate, ValueArg(location), None))
		})
		CompileTranslation(cmd, cmds.toSeq)
	}
}
