package roboliq.devices.pipette

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._


class L3P_Mix extends CommandCompilerL3 {
	type CmdType = L3C_Mix
	val cmdType = classOf[CmdType]
	
	def addKnowledge(kb: KnowledgeBase, _cmd: Command) {
		val cmd = _cmd.asInstanceOf[CmdType]
		// Add destinations to KB
		cmd.dests.foreach(_ match {
			case WP_Well(o) => kb.addWell(o, false)
			case WP_Plate(o) => kb.addPlate(o, false)
		})
	}
	
	def compileL3(ctx: CompilerContextL3, _cmd: Command): CompileResult = {
		val cmd = _cmd.asInstanceOf[CmdType]
		val errors = checkParams(ctx.states, cmd)
		if (!errors.isEmpty)
			return CompileError(cmd, errors)
		
		translate(ctx.states, cmd) match {
			case Right(translation) => CompileTranslation(cmd, Seq(translation))
			case Left(errors) => CompileError(cmd, Seq(errors))
		}
	}
	
	private def checkParams(states: RobotState, cmd: CmdType): Seq[String] = {
		val dests = Set(cmd.dests : _*)
		if (dests.size == 0)
			return ("must have one or more destinations") :: Nil
		
		// Check validity of source/dest pairs
		val destsAlready = new HashSet[Obj]
		for (dest <- cmd.dests) {
			val destObjs = dest match {
				case WP_Well(well) => Seq(well) 
				case WP_Plate(plate) => Seq(plate) ++ plate.state(states).conf.wells
			}
			if (destObjs.exists(destsAlready.contains))
				return ("each destination may only be used once") :: Nil
			destsAlready ++= destObjs
		}
		
		if (cmd.mixSpec.nVolume <= 0)
			return ("volume must be > 0") :: Nil
		if (cmd.mixSpec.nCount <= 0)
			return ("count must be > 0") :: Nil
		
		Nil
	}
	
	def translate(states: RobotState, cmd: CmdType): Either[String, Command] = {
		val destConfs = new ArrayBuffer[WellConfigL1]
		val bAllOk = cmd.dests.forall(dest => {
			val confs = PipetteHelperL3.getWells1(states, dest)
			if (confs.isEmpty) {
				false
			}
			else {
				destConfs ++= confs
				true
			}
		})
		
		//println(items2)
		
		if (bAllOk) {
			Right(L2C_Mix(new L2A_MixArgs(destConfs.toSet, cmd.mixSpec)))
		}
		else {
			Left("missing well")
		}
	}
}
