package roboliq.devices.pipette

import roboliq.common._
import roboliq.compiler._
import roboliq.commands.pipette._


class L3P_TipsDrop(location: String) extends CommandCompilerL3 {
	type CmdType = L3C_TipsDrop
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): CompileResult = {
		val tips = cmd.tips.filter(_.obj.state(ctx.states).sType_?.isDefined)
		if (tips.isEmpty)
			CompileTranslation(cmd, Seq())
		else
			CompileTranslation(cmd, Seq(new L2C_TipsDrop(cmd.tips, location)))
	}
}
