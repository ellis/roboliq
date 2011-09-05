package weizmann

import roboliq.common._
import roboliq.compiler._
import roboliq.commands.pipette._
import roboliq.devices.pipette._


class L3P_TipsWash_Weizmann(robot: PipetteDevice, plateDeconAspirate: Plate, plateDeconDispense: Plate) extends CommandCompilerL3 {
	type CmdType = L3C_TipsWash
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): CompileResult = {
		cmd.intensity match {
			case WashIntensity.None =>
				CompileTranslation(cmd, Seq())
			case _ =>
				CompileTranslation(cmd, Seq(createWash2(ctx.states, cmd, 0)))
		}
	}
	
	private def createWash2(states: RobotState, cmd: CmdType, iWashProgram: Int): L2C_Wash = {
		val items2 = cmd.items.map(item => {
			val nVolumeInside = item.tip.obj.state(states).nContamInsideVolume // FIXME: add additional wash volume
			new L2A_WashItem(item.tip, nVolumeInside)
		})
		L2C_Wash(items2, iWashProgram, cmd.intensity)
	}
}
