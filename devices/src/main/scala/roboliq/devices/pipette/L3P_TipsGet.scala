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
		val cmds2 = itemss.toSeq.flatMap(pair => {
			val (sType, items) = pair
			val cmds = new ArrayBuffer[Command]
			val tips = items.map(_.tip).toSet
			val get = Seq(
					new L2C_Wash(tips.toSeq.map(tip => new L2A_WashItem(tip, 0)), 0, WashIntensity.Decontaminate),
					new L2C_TipsGet(tips, sType)
					)
			val tipsToDrop = tips.filter(tip => tip.obj.state(ctx.states).sType_?.isDefined)
			if (!tipsToDrop.isEmpty)
				Seq(new L3C_TipsDrop(tipsToDrop)) ++ get
			else
				get
		})
		CompileTranslation(cmd, cmds2)
	}
}
