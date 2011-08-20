package roboliq.devices.pipette

import roboliq.common._
import roboliq.compiler._
import roboliq.commands.pipette._


class L2P_Clean(robot: PipetteDevice, plateDeconAspirate: Plate, plateDeconDispense: Plate) extends CommandCompilerL2 {
	type CmdType = L2C_Clean
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL2, cmd: CmdType): CompileResult = {
		cmd.degree match {
			case CleanDegree.None =>
				CompileTranslation(cmd, Seq())
			case CleanDegree.Light =>
				val l1c = L1C_Wash(cmd.tips, cmd.degree, 0)
				CompileTranslation(cmd, Seq(l1c))
			case CleanDegree.Thorough =>
				val l1c = L1C_Wash(cmd.tips, cmd.degree, 1)
				CompileTranslation(cmd, Seq(l1c))
			case CleanDegree.Decontaminate =>
				(ctx.map31.configL1(plateDeconAspirate), ctx.map31.configL1(plateDeconDispense)) match {
					case (Some(pcA: PlateConfigL1), Some(pcD: PlateConfigL1)) =>
						val nVolume = 600
						val twvpsAD = cmd.tips.toSeq.map(tip => {
							val tipState = tip.obj.state(ctx.state0)
							val wellA = pcA.wells(tip.index % pcA.nWells)
							val wellD = pcD.wells(tip.index % pcA.nWells)
							val wellConfA = wellA.getConfigL1(ctx.map31).get
							val wellConfD = wellD.getConfigL1(ctx.map31).get
							val wellStateA = wellA.state(ctx.state0)
							val wellStateD = wellD.state(ctx.state0)
							val policyA_? = robot.getAspiratePolicy(tipState, wellStateA)
							val policyD_? = robot.getDispensePolicy(tipState, wellStateD, nVolume)
							(policyA_?, policyD_?) match {
								case (Some(policyA), Some(policyD)) =>
									val twvpA = new TipWellVolumePolicy(tip, wellConfA, nVolume, policyA)
									val twvpD = new TipWellVolumePolicy(tip, wellConfD, nVolume, policyD)
									(twvpA, twvpD)
								case _ =>
									return CompileError(cmd, Seq("unable to find pipetting policy for decon wells"))
							}
						})
						val twvpsA = twvpsAD.map(_._1)
						val twvpsD = twvpsAD.map(_._2)
						CompileTranslation(cmd, Seq(
								L1C_Wash(cmd.tips, cmd.degree, 3),
								L1C_Aspirate(twvpsA),
								L1C_Dispense(twvpsD),
								L1C_Wash(cmd.tips, cmd.degree, 4),
								L1C_Wash(cmd.tips, cmd.degree, 5)
								))
					case _ =>
						CompileError(cmd, Seq("level 1 config for decon plates not defined"))
				}
		}
	}
}
