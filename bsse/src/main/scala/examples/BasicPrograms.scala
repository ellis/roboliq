package examples

import scala.collection.mutable.ArrayBuffer

import roboliq.common._
import roboliq.commands.pipette._
//import roboliq.compiler._
import roboliq.devices.pipette._
//import bsse.Protocol

trait Program extends PipetteCommands {
	val cmds = new ArrayBuffer[Command]
	
	def getCommands(): Seq[Command] = {
		cmds.clear()
		run()
		cmds.toSeq
	}
	
	protected def run()
}

case class ProgramStageSuccess(cmds: Seq[Command], vars: Object, log: Log = Log.empty) extends CompileStageResult {
	def print() {
		println(this.toString)
	}
}

object Program02 {
	class Input(
		val ddw: Reagent
	)
	class Output {
		val plate = new Plate
	}
	class Params(
		var nVolume: Double = 30,
		var nMixCount: Int = 5
	)
	case class Vars(
		val pipette: L4A_PipetteArgs
	)
	
	/*class Runner(kb: KnowledgeBase, in: Input, params: Params) extends Program {
		val out = new Output 

		import in._, out._, params._
		
		pipette(ddw, plate, nVolume)
		mix(plate, nVolume, nMixCount)
	}*/
	
	//def addKnowledge(kb: KnowledgeBase, in: Input, out: Output, params: Params): Option[CompileStageError] = {
	//}
	
	def getCommands(in: Input, out: Output, params: Params): Either[CompileStageError, ProgramStageSuccess] = {
		val (cmds1, vars1) = pipette(WPL_Liquid(in.ddw), WP_Plate(out.plate), params.nVolume)
		
		val vars = new Vars(vars1)
		val cmds = cmds1
		Success(ProgramStageSuccess(cmds, vars))
	}
	
	//def pipette() = Seq[Command]()
	//def mix() = Seq[Command]()
	
	def pipette(source: WellOrPlateOrLiquid, dest: WellOrPlate, volume: Double): Tuple2[Seq[Command], L4A_PipetteArgs] = {
		val item = new L4A_PipetteItem(source, dest, volume)
		val args = new L4A_PipetteArgs(Seq(item))
		val cmd = L4C_Pipette(args)
		(Seq(cmd), args)
	}
}
/*
class Program01(ddw: Liquid) extends Program {
	val plate1 = new Plate(PlateFamily.Standard)
	
	def run(): Seq[Command] = {
		pipette(ddw, plate1, 30)
		mix(plate1, 30, 5)
	}
}
*/