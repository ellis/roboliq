package roboliq.devices.pipette

import roboliq.common._
import roboliq.compiler._
import roboliq.commands.pipette._


class L3P_CleanPending(robot: PipetteDevice) extends CommandCompilerL3 {
	type CmdType = L3C_CleanPending
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): CompileResult = {
		val cmd2_? = {
			val states = ctx.states
			val tips = cmd.tips
			
			if (robot.areTipsDisposable) {
				Some(L3C_TipsDrop(tips))
			}
			else {
				var intensity = WashIntensity.None 
				val items = tips.toSeq.map(tip => {
					val tipState = tip.state(states)
					if (tipState.cleanDegreePending > WashIntensity.None) {
						intensity = WashIntensity.max(tipState.cleanDegreePending, intensity)
						Some(new L3A_TipsWashItem(tip, tipState.contamInside, tipState.contamOutside))
					}
					else
						None
				}).flatten
				if (!items.isEmpty)
					Some(L3C_TipsWash(items, intensity))
				else
					None
			}
		}
		CompileTranslation(cmd, Seq(cmd2_?).flatten)
	}
}
