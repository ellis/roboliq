package roboliq.devices.pipette

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._


private trait L3P_PipetteMixBase {
	type CmdType <: Command
	type L3A_ItemType
	
	type Errors = Seq[String]

	val robot: PipetteDevice
	val ctx: CompilerContextL3
	val cmd: CmdType

	trait CycleState {
		val tips: SortedSet[TipConfigL2]
		val state0: RobotState
		
		val gets = new ArrayBuffer[L3C_TipsReplace]
		val washs = new ArrayBuffer[L3C_TipsWash]
		val mixes = new ArrayBuffer[L2C_Mix]
		
		var ress: Seq[CompileFinal] = Nil

		def toTokenSeq: Seq[Command]
	}
	
	val dests: SortedSet[WellConfigL2]

	def translation: Either[CompileError, Seq[Command]] = {
		// Need to split into tip groups (e.g. large tips, small tips, all tips)
		// For each group, perform the pipetting and score the results
		// Pick the strategy with the best score
		var winner = Seq[Command]()
		var nWinnerScore = Int.MaxValue
		var lsErrors = new ArrayBuffer[String]
		val mapIndexToTip = robot.config.tips.map(tip => tip.index -> tip).toMap
		for (tipGroup <- robot.config.tipGroups) {
			val mapTipToType = tipGroup.map(pair => mapIndexToTip(pair._1).state(ctx.states).conf -> pair._2.sName).toMap
			val tips = SortedSet(mapTipToType.keys.toSeq : _*)
			//val tipAndSpecs = robot.config.tips.filter(tip => tipGroup.contains(tip.index)).map(tip => tip.state(ctx.states).conf)
	
			translateCommand(tips, mapTipToType) match {
				case Left(lsErrors2) =>
					lsErrors ++= lsErrors2
				case Right(Seq()) =>
				case Right(cycles) =>
					val cmds1 = cycles.flatMap(_.toTokenSeq) ++ dropSeq(tips)
					ctx.subCompiler.compile(ctx.states, cmds1) match {
						case Right(nodes) =>
							ctx.subCompiler.scoreNodes(ctx.states, nodes) match {
								case Right(nScore) =>
									if (nScore < nWinnerScore) {
										winner = cmds1
										nWinnerScore = nScore
									}
								case Left(err) => lsErrors ++= err.errors
							}
						case _ =>
					}
				}
		}
		if (nWinnerScore < Int.MaxValue)
			Right(winner)
		else
			Left(CompileError(cmd, lsErrors))
	}
	
	protected def translateCommand(tips: SortedSet[TipConfigL2], mapTipToType: Map[TipConfigL2, String]): Either[Errors, Seq[CycleState]]

	/** Create temporary tip state objects and associate them with the source liquid */
	protected def createTemporaryTipStates(stateCycle0: RobotState, mapTipToType: Map[TipConfigL2, String], mapTipToSrcLiquid: Map[TipConfigL2, Liquid], tws0: Seq[TipWell]): HashMap[Tip, TipStateL2] = {
		// Create temporary tip state objects and associate them with the source liquid
		val tipStates = new HashMap[Tip, TipStateL2]
		tws0.foreach(tw => {
			// Create clean tip state from stateCycle0
			val tipState0 = tw.tip.obj.state(stateCycle0)
			val tipState = tw.tip.createState0(tipState0.sType_?)
			tipStates(tw.tip.obj) = tipState
			
			val tipWriter = tw.tip.obj.stateWriter(tipStates)
			// Get proper tip
			if (robot.areTipsDisposable)
				tipWriter.get(mapTipToType(tw.tip))
			// Associate liquid by "aspirating" 0 ul of it
			mapTipToSrcLiquid.get(tw.tip) match {
				case Some(liquid) => tipWriter.aspirate(liquid, 0)
				case _ =>
			}
		})
		tipStates
	}
	
	
	protected def dispense_updateTipStates(cycle: CycleState, twvps: Seq[L2A_SpirateItem], tipStates: HashMap[Tip, TipStateL2]) {
		// Add volumes to amount required in tips
		for (twvp <- twvps) {
			val wellState = twvp.well.obj.state(cycle.state0)
			val tipWriter = twvp.tip.obj.stateWriter(tipStates)
			tipWriter.dispense(twvp.nVolume, wellState.liquid, twvp.policy.pos)
		}
	}
	
	protected def mix_updateTipStates(cycle: CycleState, twvps: Seq[L2A_MixItem], tipStates: HashMap[Tip, TipStateL2]) {
		// Add volumes to amount required in tips
		for (twvp <- twvps) {
			val wellState = twvp.well.obj.state(cycle.state0)
			val tipWriter = twvp.tip.obj.stateWriter(tipStates)
			tipWriter.mix(wellState.liquid, twvp.nVolume)
		}
	}
	
	/** Would a cleaning be required before a subsequent dispense from the same tip? */
	protected def checkNoCleanRequired(cycle: CycleState, tipStates: collection.Map[Tip, TipStateL2], tws: Seq[TipWell]): Boolean = {
		def step(tipWell: TipWell): Boolean = {
			val tipState = tipStates(tipWell.tip.obj)
			PipetteHelper.getCleanDegreeDispense(tipState) == WashIntensity.None
		}
		tws.forall(step)
	}
	
	/*protected def clean(cycle: CycleState, tcs: Seq[Tuple2[TipConfigL2, WashIntensity.Value]]) {
		// Add tokens
		val tcss = robot.batchesForClean(tcs)
		for (tcs <- tcss) {
			val tips = tcs.map(_._1).toSet
			cycle.cleans += L3C_Clean(tips, tcs.head._2)
		}
	}*/
	
	protected def clean(cycle: CycleState, mapTipToType: Map[TipConfigL2, String], overrides: TipHandlingOverrides, bFirst: Boolean, twsA: Seq[TipWell], twsD: Seq[TipWell]) {
		if (robot.areTipsDisposable) {
			val mapTipToReplacement = new HashMap[TipConfigL2, TipReplacementAction.Value]
			// Replacement requires by aspirate
			for (tw <- twsA) {
				val tipState = tw.tip.obj.state(cycle.state0)
				val srcState = tw.well.obj.state(cycle.state0)
				val srcLiquid = srcState.liquid
				val replacement = PipetteHelper.choosePreAsperateReplacement(overrides.replacement_?, srcLiquid, tipState)
				mapTipToReplacement(tw.tip) = replacement
			}
			// Replacement that would be required by dispense, were we to dispense immediately without 
			for (tw <- twsD) {
				val tipState = tw.tip.obj.state(cycle.state0)
				val destState = tw.well.obj.state(cycle.state0)
				val destLiquid = destState.liquid
				val replacement = PipetteHelper.choosePreDispenseReplacement(overrides.replacement_?, destLiquid, tipState)
				if (replacement > mapTipToReplacement(tw.tip))
					mapTipToReplacement(tw.tip) = replacement
			}
			val getItems = cycle.tips.map(tip => {
				val replacement = mapTipToReplacement.getOrElse(tip, TipReplacementAction.None)
				val sType_? = if (replacement == TipReplacementAction.Replace) mapTipToType.get(tip) else None
				new L3A_TipsReplaceItem(tip, sType_?)
			})
			cycle.gets += new L3C_TipsReplace(getItems.toSeq) 
		}
		else {
			val mapTipToWash = new HashMap[TipConfigL2, WashSpec]
			val washIntensityDefault = if (bFirst) WashIntensity.Thorough else WashIntensity.Light
			for (tw <- twsA) {
				val tipState = tw.tip.obj.state(cycle.state0)
				val srcState = tw.well.obj.state(cycle.state0)
				val srcLiquid = srcState.liquid
				val wash = PipetteHelper.choosePreAsperateWashSpec(overrides, washIntensityDefault, srcLiquid, tipState)
				mapTipToWash(tw.tip) = wash
			}
			for (tw <- twsD) {
				val tipState = tw.tip.obj.state(cycle.state0)
				val destState = tw.well.obj.state(cycle.state0)
				val destLiquid = destState.liquid
				val wash1 = PipetteHelper.choosePreAsperateWashSpec(overrides, washIntensityDefault, destLiquid, tipState)
				val wash0 = mapTipToWash(tw.tip)
				val washIntensity = if (wash0.washIntensity >= wash1.washIntensity) wash0.washIntensity else wash1.washIntensity
				mapTipToWash(tw.tip) = new WashSpec(
					washIntensity,
					wash0.contamInside ++ wash1.contamInside,
					wash0.contamOutside ++ wash1.contamOutside
				)
			}
			
			val washItems = mapTipToWash
					.filter(_._2 != WashIntensity.None)
					.map(pair => new L3A_TipsWashItem(pair._1, pair._2.contamInside, pair._2.contamOutside))
			val intensity = mapTipToWash.values.foldLeft(WashIntensity.None) { (acc, wash) => if (wash.washIntensity > acc) wash.washIntensity else acc }
			if (!washItems.isEmpty)
				cycle.washs += new L3C_TipsWash(washItems.toSeq, intensity)
		}
	}
	
	private def dropSeq(tips: SortedSet[TipConfigL2]): Seq[L3C_TipsDrop] = {
		if (robot.areTipsDisposable)
			Seq(L3C_TipsDrop(tips))
		else
			Seq()
	}
	
	protected def getUpdatedState(cycle: CycleState): Either[Seq[String], RobotState] = {
		val cmds1 = cycle.toTokenSeq
		//println("cmds1: "+cmds1)
		ctx.subCompiler.compile(cycle.state0, cmds1) match {
			case Left(e) =>
				Left(e.errors)
			case Right(nodes) =>
				cycle.ress = nodes.flatMap(_.collectFinal())
				//println("cycle.ress: "+cycle.ress)
				Right(cycle.ress.last.state1)
		}
	}
}
