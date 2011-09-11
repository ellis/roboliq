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
	val bMixOnly = true
	
	
	override protected def mix(
		states0: RobotState,
		//mapTipToCleanSpec0: Map[TipConfigL2, CleanSpec2],
		mapTipToModel: Map[TipConfigL2, TipModel],
		tws: Seq[TipWell]
	): Either[Seq[String], MixResult] = {
		mix_createItems(states0, tws) match {
			case Left(sError) =>
				return Left(Seq(sError))
			case Right(items0) =>
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
							return Left(Seq("INTERNAL: Error code dispense 1"))
						}
						
						// check for valid volume, i.e., not too much
						mix_checkVol(builder, tip, target) match {
							case Some(sError) => return Left(Seq(sError))
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
								return Left(Seq("INTERNAL: Error code dispense 2"))
						}
						
						// Update tip state
						tipWriter.mix(liquid, item.nVolume)
					})
					actions = actions ++ Seq(Mix(items))
				}
				Right(new MixResult(builder.toImmutable, actions))
		}
	}
	
	private def mix_createItems(states: RobotState, tws: Seq[TipWell]): Either[String, Seq[L2A_MixItem]] = {
		val items = tws.map(tw => {
			getMixPolicy(states, tw) match {
				case Left(sError) => return Left(sError)
				case Right(policy) =>
					new L2A_MixItem(tw.tip, tw.well, args.mixSpec.nVolume, args.mixSpec.nCount, policy)
			}
		})
		Right(items)
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

	// Check for appropriate volumes
	private def mix_checkVols(cycle: CycleState, tipStates: HashMap[Tip, TipStateL2], tws: Seq[TipWell]): Option[String] = {
		assert(!tws.isEmpty)
		var sError_? : Option[String] = None
		
		def isVolOk(tw: TipWell): Boolean = {
			val tip = tw.tip
			val dest = tw.well
			val liquid = dest.obj.state(cycle.state0).liquid
			val tipState = tipStates(tip.obj)
			val nMin = robot.getTipAspirateVolumeMin(tipState, liquid)
			val nMax = robot.getTipHoldVolumeMax(tipState, liquid)
			val nTipVolume = -tipStates(tip.obj).nVolume
			sError_? = {
				val nVolume = args.mixSpec.nVolume
				if (nVolume < nMin)
					Some("Cannot mix "+nVolume+"ul with tip "+(tip.index+1)+": require >= "+nMin+"ul")
				else if (nVolume + nTipVolume > nMax)
					Some("Cannot mix "+nVolume+"ul with tip "+(tip.index+1)+": require <= "+nMax+"ul")
				else
					None
			}
			// TODO: make sure that source well is not over-aspirated
			// TODO: make sure that destination well is not over-filled
			sError_?.isEmpty
		}

		tws.forall(isVolOk)
		sError_?
	}
}
