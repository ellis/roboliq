package roboliq.devices.pipette

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.compiler._
import roboliq.commands.pipette._


class L3P_TipsGet extends CommandCompilerL3 {
	type CmdType = L3C_TipsGet
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): CompileResult = {
		val itemss = cmd.items.groupBy(_.sType)
		val cmds2 = itemss.flatMap(pair => {
			val (sType, items) = pair
			val cmds = new ArrayBuffer[Command]
			val tips = items.map(_.tip)
			val tipsToDrop = tips.filter(tip => tip.obj.state(ctx.states).sType_?.isDefined)
			new L2C_Wash()
			new L2C_TipsGet(items.map(_.tip).toSet, sType)
		})
		CompileTranslation(cmd, cmds2.toSeq)
	}
}
