package roboliq.devices.pipette

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._


class L3P_Pipette(robot: PipetteDevice) extends CommandCompilerL3 {
	type CmdType = L3C_Pipette
	val cmdType = classOf[CmdType]

	def compile(ctx: CompilerContextL3, cmd: CmdType): Result[Seq[Command]] = {
		val x = new L3P_Pipette_Sub(robot, ctx, cmd)
		for { translation <- x.translation }
		yield translation
	}
}

private class L3P_Pipette_Sub(val robot: PipetteDevice, val ctx: CompilerContextL3, val cmd: L3C_Pipette) extends L3P_PipetteMixBase {
	type CmdType = L3C_Pipette

	val args = cmd.args
	// Get all destination wells (but drop any empty volumes)
	val dests = SortedSet[WellConfigL2](args.items.collect({ case item if item.nVolume > 0.001 => item.dest }) : _*)
	val mixSpec_? : Option[MixSpec] = args.mixSpec_?
	val tipOverrides = args.tipOverrides_? match { case Some(o) => o; case None => TipHandlingOverrides() }
	val tipModel_? = args.tipModel_?
	val bMixOnly = false
	
	val mapDestToItem: Map[WellConfigL2, L3A_PipetteItem] = args.items.map(t => t.dest -> t).toMap

	override protected def dispense(
		states0: RobotState,
		tws: Seq[TipWell],
		remains0: Map[TipWell, Double],
		bFirstInCycle: Boolean
	): Result[DispenseResult] = {
		val builder = new StateBuilder(states0)
		
		// Make sure that we only need to take care of remains for the first dispense in a cycle
		assert(bFirstInCycle || remains0.isEmpty)
		
		dispense_createItems(builder.toImmutable, tws, remains0) match {
			case Error(lsError) =>
				return Error(lsError)
			case Success((items, remains)) =>
				//val mapTipToCleanSpec = HashMap(mapTipToCleanSpec0.toSeq : _*)
				items.foreach(item => {
					val tip = item.tip
					val dest = item.well
					val tipWriter = tip.obj.stateWriter(builder)
					val destWriter = dest.obj.stateWriter(builder)
					val liquidTip0 = tipWriter.state.liquid
					val liquidSrc = mapDestToItem(dest).srcs.head.obj.state(builder).liquid
					val liquidDest = destWriter.state.liquid
					
					// If the tip hasn't been used for aspiration yet, associate the source liquid with it
					for (tw <- tws) {
						val tip = tw.tip
						val tipWriter = tip.obj.stateWriter(builder)
						val liquidTip0 = tipWriter.state.liquid
						if (liquidTip0 eq Liquid.empty) {
							val dest = tw.well
							val liquidSrc = mapDestToItem(dest).srcs.head.obj.state(builder).liquid
							assert(liquidSrc ne Liquid.empty)
							tipWriter.aspirate(liquidSrc, 0)
						}
					}
					
					// check volumes
					dispense_checkVol(builder, tip, dest) match {
						case Error(lsError) => return Error(lsError)
						case _ =>
					}

					if (!bFirstInCycle) {
							// If we would need to aspirate a new liquid, end this cycle
						if (liquidSrc ne liquidTip0) {
							return Error("INTERNAL: Error code dispense 1; "+liquidSrc.getName()+"; "+liquidTip0.getName())
						}
						
						// If we need to mix, then force wet contact when checking for how to clean
						val pos = args.mixSpec_? match {
							case None => item.policy.pos
							case _ => PipettePosition.WetContact
						}
						
						// End this cycle if this dispense would require a cleaning
						getDispenseCleanSpec(builder, tipOverrides, item.tip, item.well, pos) match {
							case None =>
							case Some(spec) =>
								return Error(Seq("INTERNAL: Error code dispense 2"))
						}
					}
						
					// Update tip and destination states
					tipWriter.dispense(item.nVolume, liquidDest, item.policy.pos)
					destWriter.add(liquidSrc, item.nVolume)
				})
				val actions = Seq(Dispense(items))
				Success(new DispenseResult(builder.toImmutable, actions, remains))
		}
	}
	
	override protected def aspirate(
		states: StateMap,
		twsD: Seq[TipWell],
		mapTipToVolume: Map[TipConfigL2, Double]
	): Result[Seq[Aspirate]] = {
		val tips = SortedSet(twsD.map(_.tip) : _*)
		val mapTipToSrcs = twsD.map(tw => tw.tip -> mapDestToItem(tw.well).srcs).toMap
		val setSrcs = Set(mapTipToSrcs.values.toSeq : _*)
		val bAllSameSrcs = (setSrcs.size == 1)
		val twss = {
			if (bAllSameSrcs)
				aspirate_chooseTipWellPairs_liquid(states, tips, setSrcs.head)
			else {
				/*// FIXME: for debug only
				println("TIPS:", tips)
				cmd.args.items.foreach(item => println(item.dest, item.srcs.head))
				// ENDFIX*/
				aspirate_chooseTipWellPairs_direct(states, mapTipToSrcs)
			}
		}
		def toAction(tws: Seq[TipWell]): Result[Aspirate] = {
			aspirate_createItems(states, mapTipToVolume, tws).map(Aspirate(_))
		}
		Result.map(twss, toAction _)
	}
	
	// Check for appropriate volumes
	private def dispense_checkVol(states: StateMap, tip: TipConfigL2, dest: WellConfigL2): Result[Unit] = {
		val item = mapDestToItem(dest)
		val tipState = tip.obj.state(states)
		val liquidSrc = tipState.liquid // since we've already aspirated the source liquid
		val nMin = robot.getTipAspirateVolumeMin(tipState, liquidSrc)
		val nMax = robot.getTipHoldVolumeMax(tipState, liquidSrc)
		val nTipVolume = -tipState.nVolume
		
		// TODO: make sure that source well is not over-aspirated
		// TODO: make sure that destination well is not over-filled
		if (item.nVolume + nTipVolume < nMin)
			Error("Cannot aspirate "+item.nVolume+"ul into tip "+(tip.index+1)+": require >= "+nMin+"ul")
		else if (item.nVolume + nTipVolume > nMax)
			Error("Cannot aspirate "+item.nVolume+"ul into tip "+(tip.index+1)+": require <= "+nMax+"ul")
		else
			Success(())
	}
	
	private def dispense_getTipVolMinMax(states: StateMap, tip: TipConfigL2, dest: WellConfigL2): Tuple2[Double, Double] = {
		val item = mapDestToItem(dest)
		val tipState = tip.obj.state(states)
		val liquidSrc = tipState.liquid // since we've already aspirated the source liquid
		val nMin = robot.getTipAspirateVolumeMin(tipState, liquidSrc)
		val nMax = robot.getTipHoldVolumeMax(tipState, liquidSrc)
		(nMin, nMax)
	}

	/**
	 * on success, return tuple of spirate items and volumes remaining to be pipetted
	 */
	private def dispense_createItems(
		states: RobotState,
		tws: Seq[TipWell],
		remains0: Map[TipWell, Double]
	): Result[Tuple2[Seq[L2A_SpirateItem], Map[TipWell, Double]]] = {
		val tws2 = if (remains0.isEmpty) tws else tws.filter(remains0.contains)
		val items = new ArrayBuffer[L2A_SpirateItem]
		// For debug only:
		if (tws2.isEmpty) {
			println()
			println("createItems:")
			tws.sortBy(_.tip).foreach(println)
			remains0.toSeq.sortBy(_._1.tip).foreach(println)
		}
		val remains = new HashMap[TipWell, Double]
		for (tw <- tws2) {
			val (tip, dest) = (tw.tip, tw.well)
			val item = mapDestToItem(dest)
			val (nMin, nMax) = dispense_getTipVolMinMax(states, tip, dest)
			
			val tipState = tip.state(states)
			val nVolumeInTip = -tipState.nVolume
			val nVolume = {
				val nVolumeRequested = remains0.getOrElse(tw, item.nVolume)
				if (nVolumeRequested > nMax) {
					if (nVolumeInTip > 0)
						return Error("INTERNAL: tip must be emptied before transfering excess volumes")
					// Use maximum value possible, leaving at least nMin for the next round.
					val nVolume = math.min(nMax, nVolumeRequested - nMin)
					val nVolumeRemaining = nVolumeRequested - nVolume
					remains += (tw -> nVolumeRemaining)
					nVolume
				}
				else {
					nVolumeRequested
				}
			}
			getDispensePolicy(states, tw, nVolume) match {
				case Error(sError) => return Error(sError)
				case Success(policy) =>
					items += new L2A_SpirateItem(tip, dest, nVolume, policy)
			}
		}
		assert(!items.isEmpty)
		//println("remains:", remains)
		Success((items.toSeq, remains.toMap))
	}

	/*
	private def mix_createItems(states: RobotState, tws: Seq[TipWell], mixSpec: MixSpec): Result[Seq[L2A_MixItem]] = {
		val items = tws.map(tw => {
			getMixPolicy(states, tw) match {
				case Error(sError) => return Error(sError)
				case Success(policy) =>
					new L2A_MixItem(tw.tip, tw.well, mixSpec.nVolume, mixSpec.nCount, policy)
			}
		})
		Success(items)
	}*/

	private def aspirate_chooseTipWellPairs_liquid(states: StateMap, tips: SortedSet[TipConfigL2], srcs: Set[WellConfigL2]): Seq[Seq[TipWell]] = {
		val srcs2 = PipetteHelper.chooseAdjacentWellsByVolume(states, srcs, tips.size)
		PipetteHelper.chooseTipSrcPairs(states, tips, srcs2)
	}

	private def aspirate_chooseTipWellPairs_direct(states: StateMap, srcs: collection.Map[TipConfigL2, Set[WellConfigL2]]): Seq[Seq[TipWell]] = {
		//println("srcs: "+srcs)
		val tws: Seq[TipWell] = srcs.toSeq.sortBy(_._1).map(pair => new TipWell(pair._1, pair._2.head))
		val twss = PipetteHelper.splitTipWellPairs(tws)
		//println("tws:", tws)
		//println("twss:", twss)
		twss
	}
	
	private def aspirate_createItems(states: StateMap, mapTipToVolume: Map[TipConfigL2, Double], tws: Seq[TipWell]): Result[Seq[L2A_SpirateItem]] = {
		val items = tws.map(tw => {
			getAspiratePolicy(states, tw) match {
				case Error(sError) => return Error(sError)
				case Success(policy) =>
					new L2A_SpirateItem(tw.tip, tw.well, mapTipToVolume(tw.tip), policy)
			}
		})
		Success(items)
	}
	
	private def getDispensePolicy(states: StateMap, tw: TipWell, nVolume: Double): Result[PipettePolicy] = {
		getDispensePolicy(states, tw.tip, tw.well, nVolume, cmd.args.pipettePolicy_?)
	}
	
	private def getDispensePolicy(
		states: StateMap,
		tip: TipConfigL2,
		dest: WellConfigL2,
		nVolume: Double,
		pipettePolicy_? : Option[PipettePolicy]
	): Result[PipettePolicy] = {
		pipettePolicy_? match {
			case Some(policy) => Success(policy)
			case None =>
				val item = mapDestToItem(dest)
				val destState = dest.state(states)
				val liquidSrc = mapDestToItem(dest).srcs.head.obj.state(states).liquid
				robot.getDispensePolicy(liquidSrc, tip, item.nVolume, destState.nVolume) match {
					case None => Error("no dispense policy found for "+tip+" and "+dest)
					case Some(policy) => Success(policy)
				}
		}
	}
	
	private def getAspiratePolicy(states: StateMap, tw: TipWell): Result[PipettePolicy] = {
		getAspiratePolicy(states, tw.tip, tw.well, cmd.args.pipettePolicy_?)
	}
	
	private def getAspiratePolicy(
		states: StateMap,
		tip: TipConfigL2,
		src: WellConfigL2,
		pipettePolicy_? : Option[PipettePolicy]
	): Result[PipettePolicy] = {
		pipettePolicy_? match {
			case Some(policy) => Success(policy)
			case None =>
				val tipState = tip.state(states)
				val srcState = src.state(states)
				robot.getAspiratePolicy(tipState, srcState) match {
					case None => Error("no aspirate policy found for "+tip+" and "+src)
					case Some(policy) => Success(policy)
				}
		}
	}
}
