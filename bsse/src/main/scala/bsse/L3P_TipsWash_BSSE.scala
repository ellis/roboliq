package bsse

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.compiler._
import roboliq.commands.pipette._
import roboliq.devices.pipette._


class L3P_TipsWash_BSSE(robot: BssePipetteDevice, plateDeconAspirate: Plate, plateDeconDispense: Plate) extends CommandCompilerL3 {
	type CmdType = L3C_TipsWash
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): CompileResult = {
		cmd.intensity match {
				case WashIntensity.None =>
				CompileTranslation(cmd, Seq())
			case WashIntensity.Light =>
				CompileTranslation(cmd, Seq(
					createWash2(ctx.states, cmd, 1),
					createWash2(ctx.states, cmd, 2)))
			case WashIntensity.Thorough =>
				CompileTranslation(cmd, Seq(
					createWash2(ctx.states, cmd, 1),
					createWash2(ctx.states, cmd, 2)))
			case WashIntensity.Decontaminate =>
				(ctx.states.map.get(plateDeconAspirate), ctx.states.map.get(plateDeconDispense)) match {
					case (Some(pcA: PlateStateL2), Some(pcD: PlateStateL2)) =>
						// Create aspirate and dispense lists for the DECON wash
						val itemsAD = cmd.items.map(item => {
							val tip = item.tip
							val tipState = tip.obj.state(ctx.states)
							val wellA = pcA.conf.wells(tip.index % pcA.conf.nWells)
							val wellD = pcD.conf.wells(tip.index % pcD.conf.nWells)
							val wellStateA = wellA.state(ctx.states)
							val wellStateD = wellD.state(ctx.states)
							val nVolumeTip = tipState.model_?.get.nVolume
							val nVolume = math.max(nVolumeTip, tipState.nContamInsideVolume + nVolumeTip / 10)
							val policyA_? = robot.getAspiratePolicy(tipState, wellStateA)
							val policyD_? = robot.getDispensePolicy(wellStateA.liquid, tip, nVolume, wellStateD.nVolume)
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
						val twvpsA = itemsAD.map(_._1)
						val twvpsD = itemsAD.map(_._2)
						
						CompileTranslation(cmd, Seq(
								createWash2(ctx.states, cmd, 5),
								L2C_Aspirate(twvpsA),
								L2C_Dispense(twvpsD),
								createWash2(ctx.states, cmd, 6),
								createWash2(ctx.states, cmd, 7)
								))
					case _ =>
						CompileError(cmd, Seq("level 1 config for decon plates not defined"))
				}
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
