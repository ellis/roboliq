package roboliq.devices.pipette

import roboliq.common._
import roboliq.compiler._
import roboliq.commands.pipette._


class L3P_Clean(robot: PipetteDevice, plateDeconAspirate: Plate, plateDeconDispense: Plate) extends CommandCompilerL3 {
	type CmdType = L3C_Clean
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): CompileResult = {
		cmd.degree match {
			case CleanDegree.None =>
				CompileTranslation(cmd, Seq())
			case CleanDegree.Light =>
				val l1c = L2C_Wash(new L2A_WashArgs(cmd.tips, cmd.degree, 0))
				CompileTranslation(cmd, Seq(l1c))
			case CleanDegree.Thorough =>
				val l1c = L2C_Wash(new L2A_WashArgs(cmd.tips, cmd.degree, 1))
				CompileTranslation(cmd, Seq(l1c))
			case CleanDegree.Decontaminate =>
				(ctx.states.map.get(plateDeconAspirate), ctx.states.map.get(plateDeconDispense)) match {
					case (Some(pcA: PlateStateL2), Some(pcD: PlateStateL2)) =>
						val nVolume = 600
						val twvpsAD = cmd.tips.toSeq.map(tip => {
							val tipState = tip.obj.state(ctx.states)
							val wellA = pcA.conf.wells(tip.index % pcA.conf.nWells)
							val wellD = pcD.conf.wells(tip.index % pcD.conf.nWells)
							val wellStateA = wellA.state(ctx.states)
							val wellStateD = wellD.state(ctx.states)
							val policyA_? = robot.getAspiratePolicy(tipState, wellStateA)
							val policyD_? = robot.getDispensePolicy(tipState, wellStateD, nVolume)
							val well1A = wellStateA.conf
							val well1D = wellStateD.conf
							(policyA_?, policyD_?) match {
								case (Some(policyA), Some(policyD)) =>
									val twvpA = new L2A_SpirateItem(tip, well1A, nVolume, policyA)
									val twvpD = new L2A_SpirateItem(tip, well1D, nVolume, policyD)
									(twvpA, twvpD)
								case _ =>
									return CompileError(cmd, Seq("unable to find pipetting policy for decon wells"))
							}
						})
						val twvpsA = twvpsAD.map(_._1)
						val twvpsD = twvpsAD.map(_._2)
						CompileTranslation(cmd, Seq(
								L2C_Wash(new L2A_WashArgs(cmd.tips, cmd.degree, 3)),
								L2C_Aspirate(twvpsA),
								L2C_Dispense(twvpsD),
								L2C_Wash(new L2A_WashArgs(cmd.tips, cmd.degree, 4)),
								L2C_Wash(new L2A_WashArgs(cmd.tips, cmd.degree, 5))
								))
					case _ =>
						CompileError(cmd, Seq("level 1 config for decon plates not defined"))
				}
		}
	}
}
