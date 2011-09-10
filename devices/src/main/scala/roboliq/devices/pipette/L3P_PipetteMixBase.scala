package roboliq.devices.pipette

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import roboliq.common._
import roboliq.commands.pipette._
import roboliq.compiler._


private trait L3P_PipetteMixBase {
	type CmdType <: Command
	type L3A_ItemType
	
	type Errors = Seq[String]

	sealed abstract class Action
	case class Aspirate(items: Seq[L2A_SpirateItem]) extends Action
	case class Dispense(items: Seq[L2A_SpirateItem]) extends Action
	case class Mix(items: Seq[L2A_MixItem]) extends Action
	case class Clean(map: Map[TipConfigL2, CleanSpec2]) extends Action
	
	sealed abstract class CleanSpec2 { val tip: TipConfigL2 }
	case class ReplaceSpec2(tip: TipConfigL2, sType: String) extends CleanSpec2
	case class WashSpec2(tip: TipConfigL2, spec: WashSpec) extends CleanSpec2
	case class DropSpec2(tip: TipConfigL2) extends CleanSpec2

	class CycleState(
		val tips: SortedSet[TipConfigL2],
		val state0: RobotState
	) {
		var cmds: Seq[Command] = Nil
		var ress: Seq[CompileFinal] = Nil
	}
	

	val robot: PipetteDevice
	val ctx: CompilerContextL3
	val cmd: CmdType
	val dests: SortedSet[WellConfigL2]
	val mixSpec_? : Option[MixSpec]
	val tipOverrides: TipHandlingOverrides
	val bMixOnly: Boolean


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
				case Right(cmds) =>
					ctx.subCompiler.compile(ctx.states, cmds) match {
						case Right(nodes) =>
							ctx.subCompiler.scoreNodes(ctx.states, nodes) match {
								case Right(nScore) =>
									if (nScore < nWinnerScore) {
										winner = cmds
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
	
	private def translateCommand(tips: SortedSet[TipConfigL2], mapTipToType: Map[TipConfigL2, String]): Either[Errors, Seq[Command]] = {
		val actionsAll = new ArrayBuffer[Action]
		val cycles = new ArrayBuffer[CycleState]
		
		// Pair up all tips and wells
		val twss0 = PipetteHelper.chooseTipWellPairsAll(ctx.states, tips, dests)
		
		def createCycles(stateCycle0: RobotState, twss: List[Seq[TipWell]], remains0: Map[TipWell, Double]): Either[Errors, RobotState] = {
			// All done.  Perform final clean.
			if (twss.isEmpty) {
				if (robot.areTipsDisposable && tipOverrides.replacement_? == Some(TipReplacementPolicy.KeepAlways))
					return Right(stateCycle0)
				val cycle = new CycleState(tips, stateCycle0)
				val cmds = Seq(L3C_CleanPending(tips))
				val res = getUpdatedState(cycle, cmds) match {
					case Right(stateNext) => 
						cycles += cycle
						Right(stateNext)
					case Left(lsErrors) =>
						Left(lsErrors)
				}
				return res
			}
			
			val cycle = new CycleState(tips, stateCycle0)
			// First tip/dest pairs for dispense
			val tws0 = twss.head
			
			val (actionsADM, twssRest, remains) = {
				if (!bMixOnly) {
					dispenseMixAspirate(cycle, mapTipToType, twss, remains0) match {
						case Left(lsErrors) => return Left(lsErrors)
						case Right(tuple) => tuple
					}
				}
				else {
					mixOnly(cycle, mapTipToType, twss) match {
						case Left(lsErrors) => return Left(lsErrors)
						case Right(pair) => (pair._1, pair._2, Map[TipWell, Double]())
					}
				}
			}
			
			// Check a couple invariants in order to make sure that we are 
			// reducing the amount of data we need to work with on each step
			if (remains0.isEmpty) {
				if (remains.isEmpty)
					assert(twssRest.size < twss.size)
			}
			else {
				if (!remains.isEmpty)
					assert(remains.forall(pair => pair._2 < remains0.getOrElse(pair._1, 0.0)))
			}
			
			// Now we know all the dispenses and mixes we'll perform,
			// so we can go back and determine how to clean the tips based on the 
			// real starting state rather than the perfectly clean state we assumed in
			// dispenseMixAspirate() and mixOnly().
			val mapTipToCleanSpec = new HashMap[TipConfigL2, CleanSpec2]
			var bFirst = cycles.isEmpty
			for (action <- actionsADM) {
				val specs_? = action match {
					case Aspirate(items) => items.map(item => getAspirateCleanSpec(stateCycle0, mapTipToType, tipOverrides, bFirst, item))
					case Dispense(items) => items.map(item => getDispenseCleanSpec(stateCycle0, mapTipToType, tipOverrides, item.tip, item.well, item.policy.pos))
					case Mix(items) => items.map(item => getMixCleanSpec(stateCycle0, mapTipToType, tipOverrides, bFirst, item.tip, item.well))
					case _ => return Left(Seq("INTERNAL: error code actions 4"))
				}
				val specs = specs_?.flatMap(_ match {
					case None => Seq()
					case Some(spec) => Seq(spec)
				})
				for (cleanSpec <- specs) {
					val tip = cleanSpec.tip
					cleanSpec match {
						case spec: ReplaceSpec2 =>
							mapTipToCleanSpec(tip) = spec
						case spec: WashSpec2 =>
							mapTipToCleanSpec.get(tip) match {
								case None =>
									mapTipToCleanSpec(tip) = spec
								case Some(specPrev: WashSpec2) =>
									val wash = spec.spec
									val washPrev = specPrev.spec
									mapTipToCleanSpec(tip) = new WashSpec2(
										tip,
										new WashSpec(
											if (wash.washIntensity >= washPrev.washIntensity) wash.washIntensity else washPrev.washIntensity,
											washPrev.contamInside ++ wash.contamInside,
											washPrev.contamOutside ++ wash.contamOutside
										)
									)
								case Some(_) =>
									return Left(Seq("INTERNAL: Error code dispense 3"))
							}
						case spec: DropSpec2 =>
							return Left(Seq("INTERNAL: Error code dispense 4"))
					}
				}
				bFirst = false
			}
			// Prepend the clean action
			val actions = Seq(Clean(mapTipToCleanSpec.toMap)) ++ actionsADM
			actionsAll ++= actions
			
			val cmds = createCommands(actions)
			
			getUpdatedState(cycle, cmds) match {
				case Right(stateNext) => 
					cycles += cycle
					createCycles(stateNext, twssRest, remains)
				case Left(lsErrors) =>
					Left(lsErrors)
			}			
		}
		
		val statesFinal = createCycles(ctx.states, twss0.toList, Map()) match {
			case Left(e) => return Left(e)
			case Right(states) => states
		}
		
		actionsAll ++= finalClean(statesFinal, tips)
		
		val actionsOptimized = optimizeTipCleaning(actionsAll, tips)
		val cmds = createCommands(actionsOptimized)
		Right(cmds)
	}
	
	private def dispenseMixAspirate(
		cycle: CycleState,
		mapTipToType: Map[TipConfigL2, String],
		twss: List[Seq[TipWell]],
		remains0: Map[TipWell, Double]
	): Either[Seq[String], Tuple3[Seq[Action], List[Seq[TipWell]], Map[TipWell, Double]]] = {
		val builder = new StateBuilder(cycle.state0)
		val actionsD = new ArrayBuffer[Dispense]

		// Temporarily assume that the tips are perfectly clean
		cleanTipStates(builder, mapTipToType)

		def dispenseFirst(tws: Seq[TipWell]): Either[Seq[String], Map[TipWell, Double]] = {
			dispense(builder.toImmutable, mapTipToType, tws, remains0, true) match {
				case Left(lsErrors) =>
					Left(lsErrors)
				case Right(res) =>
					builder.map ++= res.states.map
					actionsD ++= res.actions
					Right(res.remains)
			}
		}
		
		def dispenseNext(tws: Seq[TipWell]): Boolean = {
			dispense(builder.toImmutable, mapTipToType, tws, Map(), false) match {
				case Left(lsErrors) => false
				case Right(res) =>
					builder.map ++= res.states.map
					actionsD ++= res.actions
					true
			}
		}
		
		// First dispense
		val tws0 = twss.head
		val remains = dispenseFirst(tws0) match {
			case Left(lsErrors) => return Left(lsErrors)
			case Right(res) => res
		}
		// Add as many tip/dest groups to this cycle as possible, and return list of remaining groups
		val twssRest = {
			if (remains.isEmpty) {
				if (mixSpec_?.isDefined)
					twss.tail
				else
					twss.tail.dropWhile(dispenseNext)
			}
			else
				twss
		}
		
		// Mix
		val actionsDM: Seq[Action] = {
			if (remains.isEmpty) {
				mixSpec_? match {
					case None => actionsD
					case Some(mixSpec) =>
						actionsD.flatMap(actionD => {
							createMixAction(cycle.state0, actionD, mixSpec) match {
								case Left(lsErrors) => return Left(lsErrors)
								case Right(actionM) => Seq(actionD, actionM)
							}
						})
				}
			}
			else {
				actionsD
			}
		}
		
		// aspirate
		val mapTipToVolume = cycle.tips.toSeq.map(tip => tip -> -tip.state(builder).nVolume).toMap
		//println("mapTipToVolume:", mapTipToVolume)
		//val mapTipToSrcs = actionsD.flatMap(_.items.map(item => item.tip -> mapDestToItem(item.well).srcs)).toMap
		val twsD = actionsD.flatMap(_.items.map(item => new TipWell(item.tip, item.well)))
		//println("twsD:", twsD)
		val actionsA: Seq[Aspirate] = aspirate(cycle.state0, twsD, mapTipToVolume) match {
			case Left(lsErrors) => return Left(lsErrors)
			case Right(acts) => acts
		}
		
		//println("stuff:", twss.size, twssRest.size, remains)
		
		val actions = actionsA ++ actionsDM
		Right((actions, twssRest, remains))
	}
	
	protected class DispenseResult(
		val states: RobotState,
		val actions: Seq[Dispense],
		/** Remaining volume to pipette for the given destination if the tip is too small to hold entire volume */
		val remains: Map[TipWell, Double]
	)
	
	protected def dispense(
		states0: RobotState,
		mapTipToType: Map[TipConfigL2, String],
		tws: Seq[TipWell],
		remains: Map[TipWell, Double],
		bFirstInCycle: Boolean
	): Either[Seq[String], DispenseResult] = {
		Right(new DispenseResult(states0, Seq(), Map()))
	}

	private def mixOnly(
		cycle: CycleState,
		mapTipToType: Map[TipConfigL2, String],
		twss: List[Seq[TipWell]]
	): Either[Seq[String], Tuple2[Seq[Action], List[Seq[TipWell]]]] = {
		val builder = new StateBuilder(cycle.state0)
		
		//val mapTipToCleanSpec = new HashMap[TipConfigL2, CleanSpec2]
		val actionsM = new ArrayBuffer[Mix]
		
		// Temporarily assume that the tips are perfectly clean
		cleanTipStates(builder, mapTipToType)

		// Mix
		def doMix(tws: Seq[TipWell]): Either[Seq[String], Unit] = {
			mix(builder.toImmutable, mapTipToType, tws) match {
				case Left(lsErrors) =>
					//println("lsErrors:", lsErrors)
					Left(lsErrors)
				case Right(res) =>
					//println("res:", res)
					builder.map ++= res.states.map
					//mapTipToCleanSpec ++= res.mapTipToCleanSpec
					actionsM ++= res.actions
					Right(())
			}
		}
		
		// First mix
		//println("first")
		doMix(twss.head) match {
			case Left(lsErrors) => return Left(lsErrors)
			case Right(res) =>
		}
		// Add as many tip/dest groups to this cycle as possible, and return list of remaining groups
		//println("rest")
		val twssRest = twss.tail.dropWhile(tws => doMix(tws).isRight)
		
		Right((actionsM, twssRest))
	}
	
	protected class MixResult(
		val states: RobotState,
		//val mapTipToCleanSpec: Map[TipConfigL2, CleanSpec2],
		val actions: Seq[Mix]
	)
	
	protected def mix(
		states0: RobotState,
		//mapTipToCleanSpec0: Map[TipConfigL2, CleanSpec2],
		mapTipToType: Map[TipConfigL2, String],
		tws: Seq[TipWell]
	): Either[Seq[String], MixResult] = {
		Right(new MixResult(states0, Seq()))
	}

	protected def aspirate(
		states: StateMap,
		twsD: Seq[TipWell],
		mapTipToVolume: Map[TipConfigL2, Double]
	): Either[Seq[String], Seq[Aspirate]] = {
		Right(Seq())
	}
	
	/** Create temporary tip state objects and associate them with the source liquid */
	private def cleanTipStates(builder: StateBuilder, mapTipToType: Map[TipConfigL2, String]) {
		for ((tip, sType) <- mapTipToType) {
			val tipWriter = tip.obj.stateWriter(builder)
			// Get proper tip
			if (robot.areTipsDisposable) {
				tipWriter.drop()
				tipWriter.get(sType)
			}
			tipWriter.clean(WashIntensity.Decontaminate)
		}
	}

	private def createMixAction(
		states: RobotState,
		actionD: Dispense,
		mixSpec: MixSpec
	): Either[Seq[String], Mix] = {
		val items = actionD.items.map(itemD => {
			getMixPolicy(states, itemD.tip, itemD.well, mixSpec.sMixClass_?) match {
				case Left(sError) => return Left(Seq(sError))
				case Right(policy) =>
					new L2A_MixItem(itemD.tip, itemD.well, mixSpec.nVolume, mixSpec.nCount, policy)
			}
		})
		Right(Mix(items))
	}
	
	protected def getMixPolicy(states: StateMap, tw: TipWell): Either[String, PipettePolicy] = {
		val sMixClass_? = mixSpec_? match {
			case None => None
			case Some(spec) => spec.sMixClass_?
		}
		getMixPolicy(states, tw.tip, tw.well, sMixClass_?)
	}
	
	private def getMixPolicy(states: StateMap, tip: TipConfigL2, well: WellConfigL2, sMixClass_? : Option[String]): Either[String, PipettePolicy] = {
		sMixClass_? match {
			case None =>
				val tipState = tip.state(states)
				val wellState = well.state(states)
				robot.getAspiratePolicy(tipState, wellState) match {
					case None => Left("no mix policy found for "+tip+" and "+well)
					case Some(policy) => Right(policy)
				}
			case Some(sLiquidClass) =>
				robot.getPipetteSpec(sLiquidClass) match {
					case None => Left("no policy found for class "+sLiquidClass)
					case Some(spec) => Right(new PipettePolicy(spec.sName, spec.aspirate))
				}
		}
	}
	
	/*
	protected def dispense_updateTipStates(cycle: CycleState, twvps: Seq[L2A_SpirateItem], builder: StateBuilder) {
		// Add volumes to amount required in tips
		for (twvp <- twvps) {
			val wellState = twvp.well.obj.state(cycle.state0)
			val tipWriter = twvp.tip.obj.stateWriter(builder)
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
	}*/
	
	/** Would a cleaning be required before a subsequent dispense from the same tip? */
	// REFACTOR: Remove this method -- ellis, 2011-08-30
	/*protected def checkNoCleanRequired(cycle: CycleState, tipStates: collection.Map[Tip, TipStateL2], tws: Seq[TipWell]): Boolean = {
		def step(tipWell: TipWell): Boolean = {
			val tipState = tipStates(tipWell.tip.obj)
			PipetteHelper.getCleanDegreeDispense(tipState) == WashIntensity.None
		}
		tws.forall(step)
	}*/
	
	/*protected def clean(cycle: CycleState, tcs: Seq[Tuple2[TipConfigL2, WashIntensity.Value]]) {
		// Add tokens
		val tcss = robot.batchesForClean(tcs)
		for (tcs <- tcss) {
			val tips = tcs.map(_._1).toSet
			cycle.cleans += L3C_Clean(tips, tcs.head._2)
		}
	}*/
	
	protected def getAspirateCleanSpec(
		builder: StateMap,
		mapTipToType: Map[TipConfigL2, String],
		overrides: TipHandlingOverrides,
		bFirst: Boolean,
		item: L2A_SpirateItem
	): Option[CleanSpec2] = {
		if (robot.areTipsDisposable) {
			val tipsToReplace = collection.mutable.Set[TipConfigL2]()
			// Replacement requires by aspirate
			val tipState = item.tip.obj.state(builder)
			val srcState = item.well.obj.state(builder)
			val srcLiquid = srcState.liquid
			val bReplace = tipState.sType_?.isEmpty || (overrides.replacement_? match {
				case None =>
					PipetteHelper.choosePreAspirateWashSpec(overrides, srcLiquid, tipState).washIntensity > WashIntensity.None
				case Some(TipReplacementPolicy.ReplaceAlways) => true
				case Some(TipReplacementPolicy.KeepBetween) => bFirst
				case Some(TipReplacementPolicy.KeepAlways) => false
			})
			if (bReplace)
				Some(ReplaceSpec2(item.tip, mapTipToType(item.tip)))
			else
				None
		}
		else {
			val mapTipToWash = new HashMap[TipConfigL2, WashSpec]
			val washIntensityDefault = if (bFirst) WashIntensity.Thorough else WashIntensity.Light
			val tipState = item.tip.obj.state(builder)
			val srcState = item.well.obj.state(builder)
			val srcLiquid = srcState.liquid
			val spec = PipetteHelper.choosePreAspirateWashSpec(overrides, srcLiquid, tipState)
			if (spec.washIntensity > WashIntensity.None) Some(WashSpec2(item.tip, spec))
			else None
		}
	}
	
	protected def getDispenseCleanSpec(
		states: StateMap,
		mapTipToType: Map[TipConfigL2, String],
		overrides: TipHandlingOverrides,
		tip: TipConfigL2,
		dest: WellConfigL2,
		pos: PipettePosition.Value
	): Option[CleanSpec2] = {
		if (pos == PipettePosition.Free || pos == PipettePosition.DryContact) {
			None
		}
		else if (robot.areTipsDisposable) {
			val tipState = tip.obj.state(states)
			val bReplace = tipState.sType_?.isEmpty || (overrides.replacement_? match {
				case Some(TipReplacementPolicy.ReplaceAlways) => true
				case Some(TipReplacementPolicy.KeepBetween) => false
				case Some(TipReplacementPolicy.KeepAlways) => false
				case None =>
					PipetteHelper.choosePreDispenseWashSpec(overrides, tipState.liquid, dest.state(states).liquid, tipState).washIntensity > WashIntensity.None
			})
			if (bReplace)
				Some(ReplaceSpec2(tip, mapTipToType(tip)))
			else
				None
		}
		else {
			val washIntensityDefault = WashIntensity.Light
			val tipState = tip.obj.state(states)
			val destState = dest.obj.state(states)
			val destLiquid = destState.liquid
			val spec = PipetteHelper.choosePreDispenseWashSpec(overrides, tipState.liquid, destLiquid, tipState)
			if (spec.washIntensity > WashIntensity.None) Some(WashSpec2(tip, spec))
			else None
		}
	}
	
	protected def getMixCleanSpec(
		states: StateMap,
		mapTipToType: Map[TipConfigL2, String],
		overrides: TipHandlingOverrides,
		bFirst: Boolean,
		tip: TipConfigL2,
		well: WellConfigL2
	): Option[CleanSpec2] = {
		val tipState = tip.state(states)
		val liquidTarget = well.state(states).liquid
		if (robot.areTipsDisposable) {
			val bReplace = tipState.sType_?.isEmpty || (overrides.replacement_? match {
				case Some(TipReplacementPolicy.ReplaceAlways) => true
				case Some(TipReplacementPolicy.KeepBetween) => bFirst
				case Some(TipReplacementPolicy.KeepAlways) => false
				case None =>
					PipetteHelper.choosePreAspirateWashSpec(overrides, liquidTarget, tipState).washIntensity > WashIntensity.None
			})
			if (bReplace)
				Some(ReplaceSpec2(tip, mapTipToType(tip)))
			else
				None
		}
		else {
			val spec = PipetteHelper.choosePreAspirateWashSpec(overrides, liquidTarget, tipState)
			//if (tip.index == 0)
			//	println("res:", overrides, liquidTarget, tipState, spec)
			if (spec.washIntensity > WashIntensity.None) Some(WashSpec2(tip, spec))
			else None
		}
	}
	
	private def optimizeTipCleaning(actions0: Seq[Action], tips: SortedSet[TipConfigL2]): Seq[Action] = {
		//return actions0
		val actions0R = actions0.reverse
		val mapTipToSpecPrev = new HashMap[TipConfigL2, CleanSpec2]
		val actions1R = for (action <- actions0R) yield {
			action match {
				case Aspirate(items) => mapTipToSpecPrev --= items.map(_.tip); action
				case Dispense(items) => mapTipToSpecPrev --= items.map(_.tip); action
				case Mix(items) => mapTipToSpecPrev --= items.map(_.tip); action
				case Clean(mapTipToSpec) =>
					mapTipToSpecPrev ++= mapTipToSpec
					Clean(mapTipToSpecPrev.toMap)
			}
		}
		val actions1 = actions1R.reverse
		return actions1
		
		val tipsToClean = new HashSet[TipConfigL2]
		val actions2 = for (action <- actions0R) yield {
			action match {
				case Aspirate(items) => tipsToClean ++= items.map(_.tip); action
				case Dispense(items) => tipsToClean ++= items.map(_.tip); action
				case Mix(items) => tipsToClean --= items.map(_.tip); action
				case Clean(mapTipToSpec) =>
					val mapNew = mapTipToSpec.filterKeys(tipsToClean.contains)
					tipsToClean.clear()
					Clean(mapNew)
			}
		}
		actions2
	}
	
	private def finalClean(states: StateMap, tips: SortedSet[TipConfigL2]): Seq[Action] = {
		if (robot.areTipsDisposable) {
			tipOverrides.replacement_? match {
				case Some(TipReplacementPolicy.KeepAlways) =>
					Seq()
				case _ =>
					val items = tips.toSeq.map(tip => tip -> DropSpec2(tip)).toMap
					Seq(Clean(items))
			}
		}
		else {
			var intensity = WashIntensity.None 
			val items = tips.toSeq.map(tip => {
				val tipState = tip.state(states)
				if (tipState.cleanDegreePending > WashIntensity.None) {
					intensity = WashIntensity.max(tipState.cleanDegreePending, intensity)
					Some(tip -> WashSpec2(tip, new WashSpec(intensity, tipState.contamInside, tipState.contamOutside)))
				}
				else
					None
			}).flatten
			Seq(Clean(items.toMap))
		}
	}

	/*
	private def finalCleanSeq(states: StateMap, tips: SortedSet[TipConfigL2]): Seq[Command] = {
		if (robot.areTipsDisposable) {
			tipOverrides.replacement_? match {
				case Some(TipReplacementPolicy.KeepAlways) => Seq()
				case _ => Seq(L3C_TipsDrop(tips))
			}
		}
		else {
			var intensity = WashIntensity.None 
			val items = tips.toSeq.map(tip => {
				val tipState = tip.state(states)
				if (tipState.cleanDegreePending > WashIntensity.None) {
					intensity = WashIntensity.max(tipState.cleanDegreePending, intensity)
					Some(new L3A_TipsWashItem(tip, tipState.contamInside, tipState.contamOutside))
				}
				else
					None
			}).flatten
			Seq(L3C_TipsWash(items, intensity))
		}
	}*/
	
	protected def getUpdatedState(cycle: CycleState, cmds: Seq[Command]): Either[Seq[String], RobotState] = {
		cycle.cmds = cmds
		//println("cmds1: "+cmds1)
		ctx.subCompiler.compile(cycle.state0, cmds) match {
			case Left(e) =>
				Left(e.errors)
			case Right(nodes) =>
				cycle.ress = nodes.flatMap(_.collectFinal())
				//println("cycle.ress: "+cycle.ress)
				cycle.ress match {
					case Seq() => Right(cycle.state0)
					case _ => Right(cycle.ress.last.state1)
				}
		}
	}
	
	private def createCommands(actions: Seq[Action]): Seq[Command] = {
		actions.flatMap(action => createCommand(action))
	}
	
	private def createCommand(action: Action): Seq[Command] = {
		action match {
			case Dispense(items) =>
				robot.batchesForDispense(items).map(items => L2C_Dispense(items))
			
			case Mix(items) =>
				Seq(L2C_Mix(items))
				
			case Aspirate(items) =>
				robot.batchesForAspirate(items).map(items => L2C_Aspirate(items))
				
			case Clean(map) =>
				// Replace & Drop
				val specsR = map.values.collect({ case v: ReplaceSpec2 => v }).toSeq.sortBy(_.tip)
				val specsD = map.values.collect({ case v: DropSpec2 => v }).toSeq.sortBy(_.tip)
				val itemsR =
					specsR.map(spec => new L3A_TipsReplaceItem(spec.tip, Some(spec.sType))) ++
					specsD.map(spec => new L3A_TipsReplaceItem(spec.tip, None))
				val cmdsR = if (itemsR.isEmpty) Seq() else Seq(L3C_TipsReplace(itemsR.toSeq))
				// Wash
				val specsW = map.values.collect({ case v: WashSpec2 => v }).toSeq.sortBy(_.tip)
				val intensity = specsW.foldLeft(WashIntensity.None) { (acc, spec) => if (spec.spec.washIntensity > acc) spec.spec.washIntensity else acc }
				val itemsW = specsW.map(spec => new L3A_TipsWashItem(spec.tip, spec.spec.contamInside, spec.spec.contamOutside))
				val cmdsW = if (itemsW.isEmpty) Seq() else Seq(L3C_TipsWash(itemsW.toSeq, intensity))
				
				cmdsR ++ cmdsW
		}
	}
}
