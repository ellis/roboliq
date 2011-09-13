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

	def compile(ctx: CompilerContextL3, cmd: CmdType): CompileResult = {
		val x = new L3P_Mix_Sub(robot, ctx, cmd)
		x.translation match {
			case Right(translation) =>
				CompileTranslation(cmd, translation)
			case Left(e) =>
				println("e: "+e)
				e
		}
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
		mapTipToModel: Map[TipConfigL2, TipModel],
		tws: Seq[TipWell]
	): Result[MixResult] = {
		mix_createItems(states0, tws) >>= mixItems(states0, mapTipToModel, tws)
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
		states0: RobotState,
		mapTipToModel: Map[TipConfigL2, TipModel],
		tws: Seq[TipWell]
	)(
		items0: Seq[L2A_MixItem]
	): Result[MixResult] = {
		val builder = new StateBuilder(states0)		
		//val mapTipToCleanSpec = HashMap(mapTipToCleanSpec0.toSeq : _*)
		val itemss = robot.batchesForMix(items0)
		var actions = Seq[Mix]()
		for (items <- itemss) {
			items.foreach(item => {
				val tip = item.tip
				val target = item.well
				val tipWriter = tip.obj.stateWriter(builder)
				val targetWriter = target.obj.stateWriter(builder)
				val liquidTip0 = tipWriter.state.liquid
				val liquid = targetWriter.state.liquid
				
				// Check liquid
				// If the tip hasn't been used for aspiration yet, associate the source liquid with it
				if (liquidTip0 eq Liquid.empty) {
					tipWriter.aspirate(liquid, 0)
				}
				// If we would need to aspirate a new liquid, abort
				else if (liquid ne liquidTip0) {
					return Error(Seq("INTERNAL: Error code dispense 1"))
				}
				
				// check for valid volume, i.e., not too much
				mix_checkVol(builder, tip, target) match {
					case Some(sError) => return Error(Seq(sError))
					case _ =>
				}

				// Check whether this dispense would require a cleaning
				val bFirst = false // NOTE: just set to false, because this will be recalculated later anyway
				//if (tip.index == 0)
				//	println("mix")
				getMixCleanSpec(builder, mapTipToModel, tipOverrides, bFirst, item.tip, item.well) match {
					case None =>
					case Some(spec) =>
						//println("spec:", spec)
						return Error(Seq("INTERNAL: Error code dispense 2"))
				}
				
				// Update tip state
				tipWriter.mix(liquid, item.nVolume)
			})
			actions = actions ++ Seq(Mix(items))
		}
		Success(new MixResult(builder.toImmutable, actions))
	}
	
	// Check for appropriate volumes
	private def mix_checkVol(states: StateMap, tip: TipConfigL2, dest: WellConfigL2): Option[String] = {
		val tipState = tip.obj.state(states)
		val liquidSrc = tipState.liquid // since we've already aspirated the source liquid
		val nMin = robot.getTipAspirateVolumeMin(tipState, liquidSrc)
		val nMax = robot.getTipHoldVolumeMax(tipState, liquidSrc)
		val nTipVolume = 0
		val nVolume = args.mixSpec.nVolume
		
		// TODO: make sure that target well is not over-aspirated
		if (nVolume + nTipVolume < nMin)
			Some("Cannot mix "+nVolume+"ul with tip "+(tip.index+1)+": require >= "+nMin+"ul")
		else if (nVolume + nTipVolume > nMax)
			Some("Cannot mix "+nVolume+"ul with tip "+(tip.index+1)+": require <= "+nMax+"ul")
		else
			None
	}
}
