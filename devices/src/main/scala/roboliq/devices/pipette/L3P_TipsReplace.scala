package roboliq.devices.pipette

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.compiler._
import roboliq.commands.pipette._


class L3P_TipsReplace extends CommandCompilerL3 {
	type CmdType = L3C_TipsReplace
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): CompileResult = {
		val tipsWash = cmd.items.map(_.tip).toSet
		val cmdWash = new L2C_Wash(tipsWash.toSeq.map(tip => new L2A_WashItem(tip, 0)), 0, WashIntensity.Decontaminate)
		
		val tipsDrop = tipsWash.filter(tip => tip.obj.state(ctx.states).sType_?.isDefined)
		val cmdDrop = L3C_TipsDrop(tipsDrop)

		val itemss = cmd.items.groupBy(_.sType_?)
		val cmdsGet2 = itemss.toSeq.flatMap(pair => {
			val (sType_?, items) = pair
			sType_? match {
				case None => Seq()
				case Some(sType) =>
					val tips= items.map(_.tip).toSet
					Seq(L2C_TipsGet(tips, sType))
			}
		})
		
		val cmds2 = {
			if (!tipsDrop.isEmpty)
				Seq[Command](cmdDrop, cmdWash) ++ cmdsGet2
			else
				Seq[Command](cmdWash) ++ cmdsGet2
		}
		CompileTranslation(cmd, cmds2)
	}
}
