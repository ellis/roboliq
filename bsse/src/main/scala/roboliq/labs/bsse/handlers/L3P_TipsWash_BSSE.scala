package roboliq.labs.bsse.handlers

import scala.collection.immutable.SortedSet

import roboliq.common._
import roboliq.compiler._
import roboliq.commands.pipette._
import roboliq.devices.pipette._
import roboliq.robots.evoware.commands._
import roboliq.labs.bsse.devices._


class L3P_TipsWash_BSSE(device: BssePipetteDevice, plateDecon: Plate) extends CommandCompilerL3 {
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
				val b1000 = cmd.items.exists(_.tip.index < 4)
				val b50 = cmd.items.exists(_.tip.index >= 4)
				val l1000 =
					if (b1000) Seq(L2C_EvowareSubroutine("""C:\Program Files\TECAN\EVOware\database\Scripts\Decontamination_WashBigTips.esc""", updateStates_wash1000(cmd.intensity)))
					else Seq()
				val l50 =
					if (b50) Seq(L2C_EvowareSubroutine("""C:\Program Files\TECAN\EVOware\database\Scripts\Decontamination_WashSmallTips.esc""", updateStates_wash50(cmd.intensity)))
					else Seq()
				Success(l1000 ++ l50)
				/*
				for {
					plateDeconState <- plateDecon.stateRes(ctx.states)
					itemsADW <- Result.mapOver(cmd.items) { item => decon(ctx, plateDeconState.conf, item) }
				} yield {
					val twvpsA = itemsADW.map(_._1)
					val twvpsD = itemsADW.map(_._2)
					val bPrewash = itemsADW.exists(_._3 == true) 

					val cmdsPrewash = {
						if (bPrewash) {
							// Light rinse
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
					
					cmdsPrewash ++ cmdsDecon
				}
				*/
		}
	}
	
	private def createWash2(states: RobotState, cmd: CmdType, iWashProgram: Int): L2C_Wash = {
		val items2 = cmd.items.map(item => {
			val nVolumeInside = item.tip.obj.state(states).nContamInsideVolume // FIXME: add additional wash volume
			new L2A_WashItem(item.tip, nVolumeInside)
		})
		L2C_Wash(items2, iWashProgram, cmd.intensity)
	}
	
	private def decon(ctx: CompilerContextL3, plateDecon: PlateConfigL2, item: L3A_TipsWashItem): Result[Tuple3[L2A_SpirateItem, L2A_SpirateItem, Boolean]] = {
		val tip = item.tip
		val tipState = tip.obj.state(ctx.states)
		val tipModel = tipState.model_?.get
		val well = plateDecon.wells(tip.index % plateDecon.nWells)
		val wellState = well.state(ctx.states)
		val nVolumeTip = tipModel.nVolume
		val nVolume = math.min(nVolumeTip, tipState.nContamInsideVolume + nVolumeTip / 10)
		val policyA_? = device.getAspiratePolicy(tipState, wellState)
		val policyD_? = device.getDispensePolicy(wellState.liquid, tip, nVolume, wellState.nVolume)
		val well1 = wellState.conf
		(policyA_?, policyD_?) match {
			case (Some(policyA), Some(policyD)) =>
				val twvpA = new L2A_SpirateItem(tip, well1, nVolume, policyA)
				val twvpD = new L2A_SpirateItem(tip, well1, nVolume, policyD)
				val bPrewash = (tipState.contamInside ++ tipState.contamOutside).contains(Contaminant.Cell)
				return Success((twvpA, twvpD, bPrewash))
			case _ =>
				return Error("unable to find pipetting policy for decon wells")
		}
	}
	
	private def updateStates_wash1000(intensity: WashIntensity.Value)(builder: StateBuilder) {
		for (t <- device.config.tips if t.index < 4) {
			t.stateWriter(builder).clean(intensity)
		}
	}
	
	private def updateStates_wash50(intensity: WashIntensity.Value)(builder: StateBuilder) {
		for (t <- device.config.tips if t.index >= 4) {
			t.stateWriter(builder).clean(intensity)
		}
	}
}
