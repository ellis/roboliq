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
		mapTipToCleanSpec0: Map[TipConfigL2, CleanSpec2],
		mapTipToType: Map[TipConfigL2, String],
		tws: Seq[TipWell]
	): Either[Seq[String], MixResult] = {
		mix_createItems(states0, tws) match {
			case Left(sError) =>
				return Left(Seq(sError))
			case Right(items) =>
				val builder = new StateBuilder(states0)		
				val mapTipToCleanSpec = HashMap(mapTipToCleanSpec0.toSeq : _*)
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
					
					// TODO: check for valid volume, i.e., not too much
					/*dispense_checkVol(builder, tip, dest) match {
						case Some(sError) => return Left(Seq(sError))
						case _ =>
					}*/

					// Check whether this dispense would require a cleaning
					val bFirst = false // NOTE: just set to false, because this will be recalculated later anyway
					getMixCleanSpec(builder, mapTipToType, tipOverrides, bFirst, item.tip, item.well) match {
						case None =>
						case Some(spec) =>
							mapTipToCleanSpec.get(item.tip) match {
								case Some(_) =>
									return Left(Seq("INTERNAL: Error code dispense 2"))
								case None =>
									mapTipToCleanSpec(item.tip) = spec
							}
					}
					
					// Update tip state
					tipWriter.mix(liquid, item.nVolume)
				})
				val actions = Seq(Mix(items))
				Right(new MixResult(builder.toImmutable, mapTipToCleanSpec.toMap, actions))
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

	//val mapDestToItem: Map[WellConfigL2, L3A_MixItem] = args.items.map(t => t.well -> t).toMap

	/*
	private def mix(cycle: CycleState, tipStates: HashMap[Tip, TipStateL2], tws: Seq[TipWell]): Option[String] = {
		mix_checkVols(cycle, tipStates, tws) match {
			case None =>
			case e @ Some(sError) => return e
		}
		
		mix_createItems(cycle, tws, tipStates) match {
			case Left(sError) => Some(sError)
			case Right(twvpcs) =>
				// Create L2 dispense commands
				mix_addCommands(cycle, twvpcs)
				mix_updateTipStates(cycle, twvpcs, tipStates)
				None
		}
	}
	
	// Check for appropriate volumes
	private def mix_checkVols(cycle: CycleState, tipStates: HashMap[Tip, TipStateL2], tws: Seq[TipWell]): Option[String] = {
		assert(!tws.isEmpty)
		var sError_? : Option[String] = None
		
		def isVolOk(tw: TipWell): Boolean = {
			val tip = tw.tip
			val dest = tw.well
			val item = mapDestToItem(dest)
			val liquid = dest.obj.state(cycle.state0).liquid
			val tipState = tipStates(tip.obj)
			val nMin = robot.getTipAspirateVolumeMin(tipState, liquid)
			val nMax = robot.getTipHoldVolumeMax(tipState, liquid)
			val nTipVolume = -tipStates(tip.obj).nVolume
			sError_? = {
				val nVolume = item.nVolume
				if (nVolume < nMin)
					Some("Cannot aspirate "+nVolume+"ul into tip "+(tip.index+1)+": require >= "+nMin+"ul")
				else if (nVolume + nTipVolume > nMax)
					Some("Cannot aspirate "+nVolume+"ul into tip "+(tip.index+1)+": require <= "+nMax+"ul")
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

	private def mix_createItems(cycle: CycleState, tws: Seq[TipWell], tipStates: collection.Map[Tip, TipStateL2]): Either[String, Seq[L2A_MixItem]] = {
		// get pipetting policy for each dispense
		val policies_? = tws.map(tw => {
			cmd.args.sMixClass_? match {
				case None =>
					val tipState = tipStates(tw.tip.obj)
					val wellState = tw.well.obj.state(cycle.state0)
					val item = mapDestToItem(tw.well)
					robot.getDispensePolicy(tipState, wellState, item.nVolume)
				case Some(sLiquidClass) =>
					robot.getPipetteSpec(sLiquidClass) match {
						case None => None
						case Some(spec) => Some(new PipettePolicy(spec.sName, spec.mix))
					}
			}
		})
		if (policies_?.forall(_.isDefined)) {
			val twvps = (tws zip policies_?.map(_.get)).map(pair => mix_createItem(cycle, pair._1, pair._2))
			Right(twvps)
		}
		else {
			Left("INTERNAL: No appropriate mix policy available")
		}
	}
	
	private def mix_createItem(cycle: CycleState, tw: TipWell, policy: PipettePolicy): L2A_MixItem = {
		val item = mapDestToItem(tw.well)
		new L2A_MixItem(tw.tip, tw.well, item.nVolume, cmd.args.nCount, policy)
	}

	private def mix_addCommands(cycle: CycleState, twvpcs0: Seq[L2A_MixItem]) {
		val twvpcs = twvpcs0.sortBy(_.tip)
		val twvpcss = robot.batchesForMix(twvpcs)
		// Create dispense tokens
		cycle.mixes ++= twvpcss.map(twvpcs => L2C_Mix(twvpcs))
	}
	*/
}
