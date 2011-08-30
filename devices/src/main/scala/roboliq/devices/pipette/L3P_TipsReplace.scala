package roboliq.devices.pipette

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.compiler._
import roboliq.commands.pipette._


class L3P_TipsReplace extends CommandCompilerL3 {
	type CmdType = L3C_TipsReplace
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): CompileResult = {
		val tips = cmd.items.map(_.tip)
		//if (ctx.nCompilerDepth == 0)
		//	println("L3P_TipsReplace: tips: "+cmd.items.toSeq.sortBy(_.tip.index).map(item => item.tip -> item.sType_?))
		
		val tipsWash = tips.filter(tip => tip.obj.state(ctx.states).cleanDegree == WashIntensity.None)
		val cmdsWash = {
			if (tipsWash.isEmpty) Seq()
			else Seq(L2C_Wash(tipsWash.map(tip => new L2A_WashItem(tip, 0)), 0, WashIntensity.Decontaminate))
		}
		
		val tipsDrop = tips.filter(tip => tip.obj.state(ctx.states).sType_?.isDefined).toSet
		val cmdsDrop = {
			if (tipsWash.isEmpty) Seq()
			else Seq(L3C_TipsDrop(tipsDrop))
		}

		val itemsGet = cmd.items.filter(_.sType_?.isDefined)
		val itemss = itemsGet.groupBy(_.sType_?.get)
		val cmdsGet2 = itemss.toSeq.flatMap(pair => {
			val (sType, items) = pair
			val tips = items.map(_.tip).toSet
			Seq(L2C_TipsGet(tips, sType))
		})
		
		val cmds2 = cmdsDrop ++ cmdsWash ++ cmdsGet2
		CompileTranslation(cmd, cmds2)
	}
}
