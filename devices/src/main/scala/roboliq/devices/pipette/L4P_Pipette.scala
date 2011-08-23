package roboliq.devices.pipette

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._


class L4P_Pipette extends CommandCompilerL4 {
	type CmdType = L4C_Pipette
	val cmdType = classOf[CmdType]
	
	def addKnowledge(kb: KnowledgeBase, _cmd: Command) {
		val cmd = _cmd.asInstanceOf[CmdType]
		// Add sources to KB
		cmd.items.foreach(_.src match {
			case WPL_Well(o) => kb.addWell(o, true)
			case WPL_Plate(o) => kb.addPlate(o, true)
			case WPL_Liquid(o) => kb.addLiquid(o)
		})
		// Add destinations to KB
		cmd.items.foreach(_.dest match {
			case WP_Well(o) => kb.addWell(o, false)
			case WP_Plate(o) => kb.addPlate(o, false)
		})
	}
	
	def compileL4(ctx: CompilerContextL4, _cmd: Command): CompileResult = {
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
		val items = cmd.items
		val srcs = Set() ++ items.map(_.src)
		if (srcs.size == 0)
			return ("must have one or more sources") :: Nil

		val dests = Set() ++ items.map(_.dest)
		if (dests.size == 0)
			return ("must have one or more destinations") :: Nil
		
		// Check validity of source/dest pairs
		val destsAlready = new HashSet[Obj]
		for (item <- items) {
			val destObjs = item.dest match {
				case WP_Well(well) => Seq(well) 
				case WP_Plate(plate) => Seq(plate) ++ plate.state(states).conf.wells
			}
			if (destObjs.exists(destsAlready.contains))
				return ("each destination may only be used once") :: Nil
			destsAlready ++= destObjs
			// If the source is a plate, the destination must be too
			item.src match {
				case WPL_Plate(plate1) =>
					item.dest match {
						case WP_Plate(plate2) =>
							val plateConf1 = plate1.state(states).conf
							val plateConf2 = plate2.state(states).conf
							if (plateConf1.nWells != plateConf2.nWells)
								return ("source and destination plates must have the same number of wells") :: Nil
						case _ =>
							return ("when source is a plate, destination must also be a plate") :: Nil
					}
				case _ =>
			}
			
			if (item.nVolume <= 0)
				return ("volume must be > 0") :: Nil
		}
		
		Nil
	}
	
	def translate(states: RobotState, cmd: CmdType): Either[String, Command] = {
		val items2 = new ArrayBuffer[L3A_PipetteItem]
		val bAllOk = cmd.items.forall(item => {
			val srcWells1 = PipetteHelperL4.getWells1(states, item.src)
			val destWells1 = PipetteHelperL4.getWells1(states, item.dest)
			if (srcWells1.isEmpty || destWells1.isEmpty) {
				false
			}
			else {
				items2 ++= destWells1.map(dest1 => new L3A_PipetteItem(srcWells1, dest1, item.nVolume))
				true
			}
		})
		
		//println(items2)
		
		if (bAllOk) {
			val args = new L3A_PipetteArgs(
					items2,
					cmd.mixSpec_?,
					sAspirateClass_? = None,
					sDispenseClass_? = None,
					sMixClass_? = None,
					sTipKind_? = None,
					fnClean_? = None)
			Right(L3C_Pipette(args))
		}
		else {
			Left("missing well")
		}
	}
}
