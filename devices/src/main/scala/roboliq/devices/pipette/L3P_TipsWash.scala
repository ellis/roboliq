package roboliq.devices.pipette

import roboliq.common._
import roboliq.compiler._
import roboliq.commands.pipette._


class L3P_TipsWash(location: String) extends CommandCompilerL3 {
	type CmdType = L3C_TipsWash
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): CompileResult = {
		CompileTranslation(cmd, Seq(new L2C_TipsDrop(cmd.tips, location)))
		class L2A_WashArgs(
			val tips: Set[TipConfigL2],
			val iWashProgram: Int,
			val intensity: WashIntensity.Value,
			val nVolumeInside: Double
		)
	}
}
