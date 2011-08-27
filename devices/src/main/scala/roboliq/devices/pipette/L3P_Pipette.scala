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

	def compile(ctx: CompilerContextL3, cmd: CmdType): CompileResult = {
		val x = new L3P_Pipette_Sub(robot, ctx, cmd)
		x.translation match {
			case Right(translation) =>
				CompileTranslation(cmd, translation)
			case Left(e) =>
				println("e: "+e)
				e
		}
	}
}

private class L3P_Pipette_Sub(val robot: PipetteDevice, val ctx: CompilerContextL3, val cmd: L3C_Pipette) extends L3P_PipetteMixBase {
	type CmdType = L3C_Pipette

	val compiler = ctx.compiler
	val args = cmd.args
	val tipOverrides = args.tipOverrides_? match { case Some(o) => o; case None => TipHandlingOverrides() }
	
	//case class SrcTipDestVolume(src: WellConfigL2, tip: TipStateL2, dest: WellConfigL2, nVolume: Double)
	
	class CycleState(val tips: SortedSet[TipConfigL2], val state0: RobotState) extends super.CycleState {
		val cleans = new ArrayBuffer[L3C_Clean]
		val aspirates = new ArrayBuffer[L2C_Aspirate]
		val dispenses = new ArrayBuffer[L2C_Dispense]
		val mixes = new ArrayBuffer[L2C_Mix]
		
		def toTokenSeq: Seq[Command] = cleans ++ aspirates ++ dispenses ++ mixes
	}
	
	val dests = SortedSet[WellConfigL2](args.items.map(_.dest) : _*)
	val mapDestToItem: Map[WellConfigL2, L3A_PipetteItem] = args.items.map(t => t.dest -> t).toMap

	protected override def translateCommand(tips: SortedSet[TipConfigL2]): Either[Errors, Seq[CycleState]] = {
		// For each dispense, pick the top-most destination wells available in the next column
		// Break off dispense batch if any tips cannot fully dispense volume
		val cycles = new ArrayBuffer[CycleState]
		//val state = new RobotStateBuilder(state0)
		
		// Pair up all tips and wells
		val twss0 = helper.chooseTipWellPairsAll(ctx.states, tips, dests)

		def createCycles(twss: List[Seq[TipWell]], stateCycle0: RobotState): Either[Errors, Unit] = {
			if (twss.isEmpty)
				return Right()
			
			val cycle = new CycleState(tips, stateCycle0)
			// First tip/dest pairs for dispense
			val tws0 = twss.head
			
			val srcs0 = tws0.map(tw => tw.tip -> mapDestToItem(tw.well).srcs).toMap
			
			// Create temporary tip state objects and associate them with the source liquid
			val tipStates = new HashMap[Tip, TipStateL2]
			tws0.foreach(tw => {
				// Create clean tip state from stateCycle0
				val tipState0 = tw.tip.obj.state(stateCycle0)
				val tipState = tw.tip.createState0(tipState0.sType_?)
				tipStates(tw.tip.obj) = tipState
				/*// Associate liquid by "aspirating" 0 ul of it
				val item = mapDestToItem(tw.well)
				val srcState = item.srcs.head.obj.state(stateCycle0)
				tw.tip.obj.stateWriter(tipStates).aspirate(srcState.liquid, 0)*/
			})

			val mapTipToReplacement = new HashMap[TipConfigL2, TipReplacementAction.Value]
			val mapTipToWash = new HashMap[TipConfigL2, WashSpec]
			if (robot.areTipsDisposable) {
				for ((tip, srcs) <- srcs0) {
					val tipState = tip.obj.state(stateCycle0)
					val srcState = srcs.head.obj.state(stateCycle0)
					val srcLiquid = srcState.liquid
					val replacement = PipetteHelper.choosePreAsperateReplacement(tipOverrides.replacement_?, srcLiquid, tipState)
					mapTipToReplacement(tip) = replacement
				}
				for (tw <- tws0) {
					val tipState = tw.tip.obj.state(stateCycle0)
					val destState = tw.well.obj.state(stateCycle0)
					val destLiquid = destState.liquid
					val replacement = PipetteHelper.choosePreDispenseReplacement(tipOverrides.replacement_?, srcLiquid, tipState)
					if (replacement > mapTipToReplacement(tw.tip))
						mapTipToReplacement(tw.tip) = replacement
				}
			}
			else {
				val washIntensityDefault = if (cycles.isEmpty) WashIntensity.Thorough else WashIntensity.Light
				for ((tip, srcs) <- srcs0) {
					val tipState = tip.obj.state(stateCycle0)
					val srcState = srcs.head.obj.state(stateCycle0)
					val srcLiquid = srcState.liquid
					val wash = PipetteHelper.choosePreAsperateWash(tipOverrides, washIntensityDefault, srcLiquid, tipState)
					mapTipToWash(tip) = wash
				}
				for (tw <- tws0) {
					val tipState = tw.tip.obj.state(stateCycle0)
					val destState = tw.well.obj.state(stateCycle0)
					val destLiquid = destState.liquid
					val wash1 = PipetteHelper.choosePreAsperateWash(tipOverrides, washIntensityDefault, srcLiquid, tipState)
					val wash0 = mapTipToWash(tw.tip)
					val washIntensity = if (wash0.washIntensity >= wash1.washIntensity) wash0.washIntensity else wash1.washIntensity
					mapTipToWash(tw.tip) = new WashSpec(
						washIntensity,
						wash0.contamInside ++ wash1.contamInside,
						wash0.contamOutside ++ wash1.contamOutside
					)
				}
			}
			
			//
			// First dispense
			//
			val sError_? = dispense(cycle, tipStates, srcs0, tws0)
			if (sError_?.isDefined) {
				return Left(Seq(sError_?.get))
			}
			
			// Add as many tip/dest groups to this cycle as possible, and return list of remaining groups
			val twssRest = twss.tail.dropWhile(tws => {
				// each tip still has the same set of source wells
				tws.forall(tw => mapDestToItem(tw.well).srcs == srcs0(tw.tip)) &&
				// dispense does not require cleaning
				checkNoCleanRequired(cycle, tipStates, tws) &&
				// dispense was possible without error
				dispense(cycle, tipStates, srcs0, tws).isEmpty
			})

			if (robot.areTipsDisposable) {
				
			}

			val src00 = srcs0.head._2
			val bAllSameSrcs = srcs0.values.forall(_ == src00)
			val sError2_? = {
				if (bAllSameSrcs)
					aspirateLiquid(cycle, tipStates, src00)
				else
					aspirateDirect(cycle, tipStates, srcs0)
			}
			if (sError2_?.isDefined) {
				return Left(Seq(sError_?.get))
			}

			getUpdatedState(cycle) match {
				case Right(stateNext) => 
					cycles += cycle
					createCycles(twssRest, stateNext)
				case Left(lsErrors) =>
					Left(lsErrors)
			}			
		}

		createCycles(twss0.toList, ctx.states) match {
			case Left(e) =>
				Left(e)
			case Right(()) =>
				// TODO: optimize aspiration
				Right(cycles)
		}
	}
	
	private def dispense(cycle: CycleState, tipStates: HashMap[Tip, TipStateL2], srcs: Map[TipConfigL2, Set[WellConfigL2]], tws: Seq[TipWell]): Option[String] = {
		dispense_checkVols(cycle, tipStates, srcs, tws) match {
			case None =>
			case e @ Some(sError) => return e
		}
		
		dispense_createTwvps(cycle, tws, tipStates) match {
			case Left(sError) => Some(sError)
			case Right(twvps) =>
				// Create L2 dispense commands
				dispense_addCommands(cycle, twvps)
				dispense_updateTipStates(cycle, twvps, tipStates)
				None
		}
	}
	
	// Check for appropriate volumes
	private def dispense_checkVols(cycle: CycleState, tipStates: HashMap[Tip, TipStateL2], srcs: Map[TipConfigL2, Set[WellConfigL2]], tws: Seq[TipWell]): Option[String] = {
		assert(!tws.isEmpty)
		var sError_? : Option[String] = None
		
		def isVolOk(tw: TipWell): Boolean = {
			val tip = tw.tip
			val dest = tw.well
			val item = mapDestToItem(dest)
			val src = srcs(tip).head
			val liquidSrc = src.obj.state(cycle.state0).liquid
			val tipState = tip.obj.state(cycle.state0)
			val nMin = robot.getTipAspirateVolumeMin(tipState, liquidSrc)
			val nMax = robot.getTipHoldVolumeMax(tipState, liquidSrc)
			val nTipVolume = -tipStates(tip.obj).nVolume
			sError_? = {
				if (item.nVolume < nMin)
					Some("Cannot aspirate "+item.nVolume+"ul into tip "+(tip.index+1)+": require >= "+nMin+"ul")
				else if (item.nVolume + nTipVolume > nMax)
					Some("Cannot aspirate "+item.nVolume+"ul into tip "+(tip.index+1)+": require <= "+nMax+"ul")
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

	private def dispense_createTwvps(cycle: CycleState, tws: Seq[TipWell], tipStates: collection.Map[Tip, TipStateL2]): Either[String, Seq[L2A_SpirateItem]] = {
		// get pipetting policy for each dispense
		val policies_? = tws.map(tw => {
			val item = mapDestToItem(tw.well)
			val tipState = tipStates(tw.tip.obj)
			val wellState = tw.well.obj.state(cycle.state0)
			robot.getDispensePolicy(tipState, wellState, item.nVolume)
		})
		if (policies_?.forall(_.isDefined)) {
			val twvps = (tws zip policies_?.map(_.get)).map(pair => {
				val (tw, policy) = pair
				val item = mapDestToItem(tw.well)
				new L2A_SpirateItem(tw.tip, item.dest, item.nVolume, policy)
			})
			Right(twvps)
		}
		else {
			Left("No appropriate dispense policy available")
		}
	}

	private def dispense_addCommands(cycle: CycleState, twvps0: Seq[L2A_SpirateItem]) {
		val twvps = twvps0.sortBy(_.tip)
		val twvpss = robot.batchesForDispense(twvps)
		// Create dispense tokens
		cycle.dispenses ++= twvpss.map(twvps => new L2C_Dispense(twvps))
	}
	
	private def aspirateLiquid(cycle: CycleState, tipStates: collection.Map[Tip, TipStateL2], srcs: Set[WellConfigL2]): Option[String] = {
		// Get list of tips which require aspiration	
		var tips = SortedSet[TipConfigL2]() ++ tipStates.values.map(_.conf)

		// sort the sources by volume descending (secondary sort key is index order)
		def order(well1: WellConfigL2, well2: WellConfigL2): Boolean = {
			val a = well1.obj.state(cycle.state0)
			val b = well2.obj.state(cycle.state0)
			(a.nVolume > b.nVolume) || (a.nVolume == b.nVolume && well1.index < well2.index)
		}
		// keep the top tips.size() entries ordered by index
		val srcs2 = SortedSet[WellConfigL2](srcs.toSeq.sortWith(order).take(tips.size) : _*)
		val pairs = srcs2.toSeq zip tips.toSeq
	
		val twss0 = helper.chooseTipSrcPairs(cycle.state0, tips, srcs2)
		var sError_? : Option[String] = None
		twss0.forall(tws => {
			aspirate_createTwvps(cycle, tipStates, tws) match {
				case Left(sError) => sError_? = Some(sError); false
				case Right(twvps) =>
					val twvpss = robot.batchesForAspirate(twvps)
					cycle.aspirates ++= twvpss.map(twvs => new L2C_Aspirate(twvps))
					true
			}
		})
		
		sError_?
	}

	private def aspirateDirect(cycle: CycleState, tipStates: collection.Map[Tip, TipStateL2], srcs: collection.Map[TipConfigL2, Set[WellConfigL2]]): Option[String] = {
		val tips = tipStates.values.map(_.conf).toSeq.sortBy(tip => tip)
		val tws = tips.map(tip => new TipWell(tip, srcs(tip).head))
		aspirate_createTwvps(cycle, tipStates, tws) match {
			case Left(sError) => Some(sError)
			case Right(twvps) =>
				val twvpss = robot.batchesForAspirate(twvps)
				cycle.aspirates ++= twvpss.map(twvs => new L2C_Aspirate(twvps))
				None
		}
	}
	
	private def aspirate_createTwvps(cycle: CycleState, tipStates: collection.Map[Tip, TipStateL2], tws: Seq[TipWell]): Either[String, Seq[L2A_SpirateItem]] = {
		// get pipetting policy for each dispense
		val policies_? = tws.map(tw => {
			val tipState = tipStates(tw.tip.obj)
			val srcState = tw.well.obj.state(cycle.state0)
			robot.getAspiratePolicy(tipState, srcState)
		})
		if (policies_?.forall(_.isDefined)) {
			val twvps = (tws zip policies_?.map(_.get)).map(pair => {
				val (tw, policy) = pair
				val tipState = tipStates(tw.tip.obj)
				new L2A_SpirateItem(tw.tip, tw.well, -tipState.nVolume, policy)
			})
			Right(twvps)
		}
		else {
			Left("No appropriate aspirate policy available")
		}
	}
}
