package roboliq.devices.pipette

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._


class L3P_Mix(robot: PipetteDevice) extends CommandCompilerL3 {
	type CmdType = L3C_Mix
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		val x = new L3P_Mix_Sub(robot, ctx, cmd)
		for { translation <- x.translation }
		yield translation
	}
}

private class L3P_Mix_Sub(val robot: PipetteDevice, val ctx: CompilerContextL3, val cmd: L3C_Mix) extends L3P_PipetteMixBase {
	type CmdType = L3C_Mix
	
	val args = cmd.args
	val dests = args.wells
	val mixSpec_? = Some(args.mixSpec)
	val tipOverrides = args.tipOverrides_? match { case Some(o) => o; case None => TipHandlingOverrides() }
	val tipModel_? = args.tipModel_?
	val bMixOnly = true
	
	
	override protected def mix(
		states0: RobotState,
		tws: Seq[TipWell]
	): Result[MixResult] = {
		mix_createItems(states0, tws) >>= mixItems(states0)
	}
	
	private def mix_createItems(states: RobotState, tws: Seq[TipWell]): Result[Seq[L2A_MixItem]] = {
		val items = tws.map(tw => {
			getMixPolicy(states, tw) match {
				case Error(sError) => return Error(sError)
				case Success(policy) =>
					new L2A_MixItem(tw.tip, tw.well, args.mixSpec.nVolume, args.mixSpec.nCount, policy)
			}
		})
		Success(items)
	}

	private def mixItems(
		states0: RobotState
	)(
		items: Seq[L2A_MixItem]
	): Result[MixResult] = {
		val builder = new StateBuilder(states0)		
		for {
			_ <- Result.forall(items, mix_handleItem(builder))
		} yield {
			new MixResult(builder.toImmutable, Seq(Mix(items)))
		}
	}
	
	/**
	 * Handle the mix item.
	 * Return an error if it violates any invariants.  Otherwise update tip state. 
	 */
	private def mix_handleItem(
		builder: StateBuilder
	)(
		item: L2A_MixItem
	): Result[Unit] = {
		val tip = item.tip
		val target = item.well
		val tipWriter = tip.obj.stateWriter(builder)
		val targetWriter = target.obj.stateWriter(builder)
		val liquid = targetWriter.state.liquid
		
		// Check liquid
		// If the tip hasn't been used for aspiration yet, associate the source liquid with it
		val liquidTip0 = tipWriter.state.liquid
		if (liquidTip0 eq Liquid.empty) {
			tipWriter.aspirate(liquid, 0)
		}

		// Check whether this dispense would require a cleaning
		val bFirst = false // NOTE: just set to false, because this will be recalculated later anyway
		val cleanSpec_? = getMixCleanSpec(builder, tipOverrides, bFirst, item.tip, item.well)
		
		for {
			// If we would need to aspirate a new liquid, abort
			_ <- Result.assert(liquid eq tipWriter.state.liquid, "INTERNAL: Error code dispense 1");
			// check for valid volume, i.e., not too much
			_ <- mix_checkVol(builder, tip, target);
			// Check whether this dispense would require a cleaning
			_ <- Result.assert(cleanSpec_?.isEmpty, "INTERNAL: Error code dispense 2")
		} yield {
			// Update tip state
			tipWriter.mix(liquid, item.nVolume)
		}
	}
	
	/** Check for permissible volumes */
	private def mix_checkVol(states: StateMap, tip: TipConfigL2, dest: WellConfigL2): Result[Unit] = {
		val tipState = tip.obj.state(states)
		val liquidSrc = tipState.liquid // since we've already aspirated the source liquid
		val nMin = robot.getTipAspirateVolumeMin(tipState, liquidSrc)
		val nMax = robot.getTipHoldVolumeMax(tipState, liquidSrc)
		val nTipVolume = 0
		val nVolume = args.mixSpec.nVolume
		
		// TODO: make sure that target well is not over-aspirated
		if (nVolume + nTipVolume < nMin)
			Error("Cannot mix "+nVolume+"ul with tip "+(tip.index+1)+": require >= "+nMin+"ul")
		else if (nVolume + nTipVolume > nMax)
			Error("Cannot mix "+nVolume+"ul with tip "+(tip.index+1)+": require <= "+nMax+"ul")
		else
			Success(())
	}
}
