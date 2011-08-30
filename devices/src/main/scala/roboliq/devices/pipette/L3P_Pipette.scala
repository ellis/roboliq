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

	val args = cmd.args
	val tipOverrides = args.tipOverrides_? match { case Some(o) => o; case None => TipHandlingOverrides() }
	
	//case class SrcTipDestVolume(src: WellConfigL2, tip: TipStateL2, dest: WellConfigL2, nVolume: Double)
	
	class CycleState(val tips: SortedSet[TipConfigL2], val state0: RobotState) extends super.CycleState {
		val aspirates = new ArrayBuffer[L2C_Aspirate]
		val dispenses = new ArrayBuffer[L2C_Dispense]
		
		def toTokenSeq: Seq[Command] = gets ++ washs ++ aspirates ++ dispenses ++ mixes
	}
	
	val dests = SortedSet[WellConfigL2](args.items.map(_.dest) : _*)
	val mapDestToItem: Map[WellConfigL2, L3A_PipetteItem] = args.items.map(t => t.dest -> t).toMap

	protected override def translateCommand(tips: SortedSet[TipConfigL2], mapTipToType: Map[TipConfigL2, String]): Either[Errors, Seq[CycleState]] = {
		// For each dispense, pick the top-most destination wells available in the next column
		// Break off dispense batch if any tips cannot fully dispense volume
		val cycles = new ArrayBuffer[CycleState]
		//val state = new RobotStateBuilder(state0)
		
		// Pair up all tips and wells
		val twss0 = PipetteHelper.chooseTipWellPairsAll(ctx.states, tips, dests)
		
		val actions1 = twss0.map(tws => new Dispense(tws))
		
		var states = ctx.states
		var builder = new StateBuilder(states)
		var bFirst = true
		val actions2 = actions1.flatMap { case dispense @ Dispense(tws) => {
			val cleans: Seq[Action] = {
				if (robot.areTipsDisposable) {
					def needToReplace(tw: TipWell): Boolean = PipetteHelper.choosePreDispenseReplacement(tw.tip.obj.state(builder))
					val tipsToReplace = tws.filter(needToReplace).map(_.tip).toSet
					Seq(Clean(tipsToReplace))
				}
				else {
					val washIntensityDefault = if (bFirst) WashIntensity.Thorough else WashIntensity.Light
					val tipsToWash = tws.flatMap(tw => {
						val tipState = tw.tip.obj.state(builder)
						val destState = tw.well.obj.state(builder)
						val destLiquid = destState.liquid
						PipetteHelper.choosePreDispenseWashSpec(tipOverrides, washIntensityDefault, destLiquid, tipState) match {
							case None => Seq()
							case Some(wash) => Seq(tw.tip)
						}
					})
					Seq(Clean(tipsToWash.toSet))
				}
			}
			
			bFirst = false
			cleans ++ Seq(dispense)
		}}
		println()
		println("actions2:")
		actions2.foreach(println)
		println()

		def createCycles(twss: List[Seq[TipWell]], stateCycle0: RobotState): Either[Errors, Unit] = {
			if (twss.isEmpty)
				return Right()
			
			val cycle = new CycleState(tips, stateCycle0)
			// First tip/dest pairs for dispense
			val tws0 = twss.head
			
			val srcs0 = tws0.map(tw => tw.tip -> mapDestToItem(tw.well).srcs).toMap
			
			// Clean
			val twsA = srcs0.map(pair => new TipWell(pair._1, pair._2.head)).toSeq
			clean(cycle, mapTipToType, tipOverrides, cycles.isEmpty, twsA, tws0)

			// Create temporary tip state objects and associate them with the source liquid
			val mapTipToSrcLiquid = tws0.map(tw => tw.tip -> mapDestToItem(tw.well).srcs.head.obj.state(stateCycle0).liquid).toMap
			val tipStates: HashMap[Tip, TipStateL2] = createTemporaryTipStates(stateCycle0, mapTipToType, mapTipToSrcLiquid, tws0)

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
				tws.forall(tw => mapDestToItem(tw.well).srcs == srcs0.getOrElse(tw.tip, Set())) &&
				// dispense does not require cleaning
				checkNoCleanRequired(cycle, tipStates, tws) &&
				// dispense was possible without error
				dispense(cycle, tipStates, srcs0, tws).isEmpty
			})

			if (robot.areTipsDisposable) {
				// TODO: drop tips
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
	
	/*private def dispense_createCommand(builder: StateBuilder, tipStates: HashMap[Tip, TipStateL2], srcs: Map[TipConfigL2, Set[WellConfigL2]], tws: Seq[TipWell]): Either[Seq[String], Command] = {
		// get pipetting policy for each dispense
		val policies_? = tws.map(tw => {
			cmd.args.sDispenseClass_? match {
				case None =>
					val item = mapDestToItem(tw.well)
					val tipState = tipStates(tw.tip.obj)
					val wellState = tw.well.obj.state(builder)
					robot.getDispensePolicy(tipState, wellState, item.nVolume)
				case Some(sLiquidClass) =>
					robot.getPipetteSpec(sLiquidClass) match {
						case None => None
						case Some(spec) => Some(new PipettePolicy(spec.sName, spec.dispense))
					}
			}
		})
		if (policies_?.forall(_.isDefined)) {
			val items = (tws zip policies_?.map(_.get)).map(pair => {
				val (tw, policy) = pair
				val item = mapDestToItem(tw.well)
				tw.well.obj.stateWriter(builder).add()
				new L2A_SpirateItem(tw.tip, item.dest, item.nVolume, policy)
			})
			Right(L2C_Dispense(items))
		}
		else {
			Left(Seq("No appropriate dispense policy available"))
		}
		
	}*/
	
	private def dispense(cycle: CycleState, tipStates: HashMap[Tip, TipStateL2], srcs: Map[TipConfigL2, Set[WellConfigL2]], tws: Seq[TipWell]): Option[String] = {
		dispense_checkVols(cycle, tipStates, srcs, tws) match {
			case None =>
			case e @ Some(sError) => return e
		}
		
		dispense_createTwvps(cycle.state0, tws, tipStates) match {
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
			val tipState = tipStates(tip.obj)
			val nMin = robot.getTipAspirateVolumeMin(tipState, liquidSrc)
			val nMax = robot.getTipHoldVolumeMax(tipState, liquidSrc)
			val nTipVolume = -tipState.nVolume
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

	private def dispense_createTwvps(states: RobotState, tws: Seq[TipWell], tipStates: collection.Map[Tip, TipStateL2]): Either[String, Seq[L2A_SpirateItem]] = {
		// get pipetting policy for each dispense
		val policies_? = tws.map(tw => {
			cmd.args.sDispenseClass_? match {
				case None =>
					val item = mapDestToItem(tw.well)
					val tipState = tipStates(tw.tip.obj)
					val wellState = tw.well.obj.state(states)
					robot.getDispensePolicy(tipState, wellState, item.nVolume)
				case Some(sLiquidClass) =>
					robot.getPipetteSpec(sLiquidClass) match {
						case None => None
						case Some(spec) => Some(new PipettePolicy(spec.sName, spec.dispense))
					}
			}
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
		val tips = SortedSet(tipStates.values.map(_.conf).toSeq : _*)
		val srcs2 = PipetteHelper.chooseAdjacentWellsByVolume(cycle.state0, srcs, tips.size)
	
		val twss0 = PipetteHelper.chooseTipSrcPairs(cycle.state0, tips, srcs2)
		var sError_? : Option[String] = None
		twss0.forall(tws => {
			aspirate_createTwvps(cycle, tipStates, tws) match {
				case Left(sError) => sError_? = Some(sError); false
				case Right(twvps) =>
					val twvpss = robot.batchesForAspirate(twvps)
					cycle.aspirates ++= twvpss.map(twvs => new L2C_Aspirate(twvs))
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
				cycle.aspirates ++= twvpss.map(twvs => new L2C_Aspirate(twvs))
				None
		}
	}
	
	private def aspirate_createTwvps(cycle: CycleState, tipStates: collection.Map[Tip, TipStateL2], tws: Seq[TipWell]): Either[String, Seq[L2A_SpirateItem]] = {
		// get pipetting policy for each dispense
		val policies_? = tws.map(tw => {
			cmd.args.sAspirateClass_? match {
				case None =>
					val tipState = tipStates(tw.tip.obj)
					val srcState = tw.well.obj.state(cycle.state0)
					robot.getAspiratePolicy(tipState, srcState)
				case Some(sLiquidClass) =>
					robot.getPipetteSpec(sLiquidClass) match {
						case None => None
						case Some(spec) => Some(new PipettePolicy(spec.sName, spec.aspirate))
					}
			}
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
