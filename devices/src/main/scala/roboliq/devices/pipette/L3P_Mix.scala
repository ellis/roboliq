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
	
	val compiler = ctx.compiler
	val args = cmd.args
	val tipOverrides = args.tipOverrides_? match { case Some(o) => o; case None => TipHandlingOverrides() }
	
	case class SrcTipDestVolume(src: WellConfigL2, tip: TipConfigL2, dest: WellConfigL2, nVolume: Double)
	
	class CycleState(val tips: SortedSet[TipConfigL2], val state0: RobotState) extends super.CycleState {
		override def toTokenSeq: Seq[Command] = gets ++ washs ++ mixes ++ drops
	}
	
	val dests = SortedSet[WellConfigL2](args.items.map(_.well).toSeq : _*)
	val mapDestToItem: Map[WellConfigL2, L3A_MixItem] = args.items.map(t => t.well -> t).toMap

	protected override def translateCommand(tips: SortedSet[TipConfigL2], mapTipToType: Map[TipConfigL2, String]): Either[Errors, Seq[CycleState]] = {
		// For each dispense, pick the top-most destination wells available in the next column
		// Break off dispense batch if any tips cannot fully dispense volume
		val cycles = new ArrayBuffer[CycleState]
		//val state = new RobotStateBuilder(state0)
		
		// Pair up all tips and wells
		val twss0 = PipetteHelper.chooseTipWellPairsAll(ctx.states, tips, dests)

		def createCycles(twss: List[Seq[TipWell]], stateCycle0: RobotState): Either[Errors, Unit] = {
			if (twss.isEmpty)
				return Right()
			
			val cycle = new CycleState(tips, stateCycle0)
			// First tip/dest pairs for dispense
			val tws0 = twss.head
			
			clean(cycle, mapTipToType, tipOverrides, cycles.isEmpty, tws0, Seq())

			// Create temporary tip state objects and associate them with the source liquid
			val tipStates: HashMap[Tip, TipStateL2] = createTemporaryTipStates(stateCycle0, mapTipToType, Map(), tws0)
			
			//
			// First mix
			//
			val sError_? = mix(cycle, tipStates, tws0)
			if (sError_?.isDefined) {
				return Left(Seq(sError_?.get))
			}
			
			// Add as many tip/dest groups to this cycle as possible, and return list of remaining groups
			val twssRest = twss.tail.dropWhile(tws => {
				// dispense does not require cleaning
				checkNoCleanRequired(cycle, tipStates, tws) &&
				mix(cycle, tipStates, tws).isEmpty
			})
			
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
				case Some(sMixClass) =>
					robot.getPipetteSpec(sMixClass) match {
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
}
