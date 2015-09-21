package roboliq.devices.pipette

import roboliq.common._
import roboliq.compiler._
import roboliq.commands.pipette._


class L3P_TipsDrop(location: String) extends CommandCompilerL3 {
	type CmdType = L3C_TipsDrop
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		val tips = cmd.tips.filter(_.obj.state(ctx.states).model_?.isDefined)
		if (tips.isEmpty)
			Success(Seq())
		else
			Success(Seq(new L2C_TipsDrop(tips, location)))
	}
}
