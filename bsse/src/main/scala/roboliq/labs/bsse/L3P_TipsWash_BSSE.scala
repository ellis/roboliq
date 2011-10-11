package roboliq.labs.bsse

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.compiler._
import roboliq.commands.pipette._
import roboliq.devices.pipette._


class L3P_TipsWash_BSSE(robot: BssePipetteDevice, plateDecon: Plate) extends CommandCompilerL3 {
	type CmdType = L3C_TipsWash
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		cmd.intensity match {
				case WashIntensity.None =>
				Success(Seq())
			case WashIntensity.Light =>
				Success(Seq(
					createWash2(ctx.states, cmd, 1),
					createWash2(ctx.states, cmd, 2)))
			case WashIntensity.Thorough =>
				Success(Seq(
					createWash2(ctx.states, cmd, 1),
					createWash2(ctx.states, cmd, 2)))
			case WashIntensity.Decontaminate =>
				for {
					pc <- Result.get(ctx.states.map.get(plateDecon), "level 1 config for decon plates not defined")
					itemsADW <- Result.mapOver(cmd.items) { item => decon(ctx, item) }
				} yield {
					val twvpsA = itemsADW.map(_._1)
					val twvpsD = itemsADW.map(_._2)
					val bPrewash = itemsADW.exists(_._3 == true) 

					val cmdsPrewash = {
						if (bPrewash) {
							Seq(
								createWash2(ctx.states, cmd, 1),
								createWash2(ctx.states, cmd, 2))
						}
						else
							Seq()
					}
					
					val cmdsDecon = Seq(
						createWash2(ctx.states, cmd, 5),
						L2C_Aspirate(twvpsA),
						L2C_Dispense(twvpsD),
						createWash2(ctx.states, cmd, 6),
						createWash2(ctx.states, cmd, 7)
					)
					
					Success(cmdsDecon)
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
	
	private def decon(ctx: CompilerContextL3, item: L3A_TipsWashItem): Result[Tuple3[L2A_SpirateItem, L2A_SpirateItem, Boolean]] = {
		val tip = item.tip
		val tipState = tip.obj.state(ctx.states)
		val tipModel = tipState.model_?.get
		val well = pc.conf.wells(tip.index % pc.conf.nWells)
		val wellState = well.state(ctx.states)
		val nVolumeTip = tipModel.nVolume
		val nVolume = math.min(nVolumeTip, tipState.nContamInsideVolume + nVolumeTip / 10)
		val policyA_? = robot.getAspiratePolicy(tipState, wellState)
		val policyD_? = robot.getDispensePolicy(wellState.liquid, tip, nVolume, wellState.nVolume)
		val well1 = wellState.conf
		(policyA_?, policyD_?) match {
			case (Some(policyA), Some(policyD)) =>
				val twvpA = new L2A_SpirateItem(tip, well1, nVolume, policyA)
				val twvpD = new L2A_SpirateItem(tip, well1, nVolume, policyD)
				val bPrewash = (tipState.contamInside ++ tipState.contamOutside).contains(Contaminant.DNA)
				return Success((twvpA, twvpD, bPrewash))
			case _ =>
				return Error("unable to find pipetting policy for decon wells")
		}
	}
}
