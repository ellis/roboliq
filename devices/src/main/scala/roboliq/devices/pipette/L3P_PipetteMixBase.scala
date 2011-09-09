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

	sealed abstract class Action
	case class Aspirate(items: Seq[L2A_SpirateItem]) extends Action
	case class Dispense(items: Seq[L2A_SpirateItem]) extends Action
	case class Mix(items: Seq[L2A_MixItem]) extends Action
	case class Clean(map: Map[TipConfigL2, CleanSpec2]) extends Action
	
	sealed abstract class CleanSpec2 { val tip: TipConfigL2 }
	case class ReplaceSpec2(tip: TipConfigL2, sType: String) extends CleanSpec2
	case class WashSpec2(tip: TipConfigL2, spec: WashSpec) extends CleanSpec2

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
				case Right(cycles) =>
					val cmds1 = cycles.flatMap(_.cmds) ++ dropSeq(tips)
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
	
	private def translateCommand(tips: SortedSet[TipConfigL2], mapTipToType: Map[TipConfigL2, String]): Either[Errors, Seq[CycleState]] = {
		val cycles = new ArrayBuffer[CycleState]
		
		// Pair up all tips and wells
		val twss0 = PipetteHelper.chooseTipWellPairsAll(ctx.states, tips, dests)
		
		def createCycles(twss: List[Seq[TipWell]], stateCycle0: RobotState): Either[Errors, Unit] = {
			if (twss.isEmpty)
				return Right()
			
			val cycle = new CycleState(tips, stateCycle0)
			// First tip/dest pairs for dispense
			val tws0 = twss.head
			
			val (actionsADM, twssRest) = {
				if (!bMixOnly) {
					dispenseMixAspirate(cycle, mapTipToType, twss) match {
						case Left(lsErrors) => return Left(lsErrors)
						case Right(pair) => pair
					}
				}
				else {
					mixOnly(cycle, mapTipToType, twss) match {
						case Left(lsErrors) => return Left(lsErrors)
						case Right(pair) => pair
					}
				}
			}
			/*val builder = new StateBuilder(stateCycle0)
			
			val mapTipToCleanSpec = new HashMap[TipConfigL2, CleanSpec2]
			val actionsD = new ArrayBuffer[Dispense]
			
			// Temporarily assume that the tips are perfectly clean
			// After choosing which dispenses to perform, then 
			// we'll go back again and see exactly how clean the tips really need to be.
			cleanTipStates(builder, mapTipToType)

			//
			// DISPENSE
			//
			def doDispense(tws: Seq[TipWell]): Either[Seq[String], Unit] = {
				dispense(builder.toImmutable, mapTipToCleanSpec.toMap, mapTipToType, tws) match {
					case Left(lsErrors) =>
						Left(lsErrors)
					case Right(res) =>
						builder.map ++= res.states.map
						mapTipToCleanSpec ++= res.mapTipToCleanSpec
						actionsD ++= res.actions
						Right(())
				}
			}
			
			// First dispense
			doDispense(tws0) match {
				case Left(lsErrors) => return Left(lsErrors)
				case Right(res) =>
			}
			// Add as many tip/dest groups to this cycle as possible, and return list of remaining groups
			val twssRest = twss.tail.dropWhile(tws => doDispense(tws).isRight)
			
			// Mix
			val actionsDM: Seq[Action] = mixSpec_? match {
				case None => actionsD
				case Some(mixSpec) =>
					actionsD.flatMap(actionD => {
						createMixAction(stateCycle0, actionD, mixSpec) match {
							case Left(lsErrors) => return Left(lsErrors)
							case Right(actionM) => Seq(actionD, actionM)
						}
					})
					
			}
			
			// aspirate
			val mapTipToVolume = tips.toSeq.map(tip => tip -> -tip.state(builder).nVolume).toMap
			//val mapTipToSrcs = actionsD.flatMap(_.items.map(item => item.tip -> mapDestToItem(item.well).srcs)).toMap
			val twsD = actionsD.flatMap(_.items.map(item => new TipWell(item.tip, item.well)))
			val actionsA: Seq[Aspirate] = aspirate(stateCycle0, twsD, mapTipToVolume) match {
				case Left(lsErrors) => return Left(lsErrors)
				case Right(acts) => acts
			}
			*/

			// Now we know all the dispenses and mixes we'll perform,
			// so we can go back and determine how to clean the tips based on the 
			// real starting state rather than the perfectly clean state we assumed.
			//mapTipToCleanSpec.clear()
			//if (bMixOnly)
			//	println("clean")
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
					}
				}
				bFirst = false
			}
			// Prepend the clean action
			val actions = Seq(Clean(mapTipToCleanSpec.toMap)) ++ actionsADM
			
			val cmds = createCommands(actions, mixSpec_?)
			
			getUpdatedState(cycle, cmds) match {
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
				Right(cycles)
		}
	}
	
	private def dispenseMixAspirate(
		cycle: CycleState,
		mapTipToType: Map[TipConfigL2, String],
		twss: List[Seq[TipWell]]
	): Either[Seq[String], Tuple2[Seq[Action], List[Seq[TipWell]]]] = {
		val builder = new StateBuilder(cycle.state0)
		
		val actionsD = new ArrayBuffer[Dispense]
		
		// Temporarily assume that the tips are perfectly clean
		// After choosing which dispenses to perform, then 
		// we'll go back again and see exactly how clean the tips really need to be.
		cleanTipStates(builder, mapTipToType)

		//
		// DISPENSE
		//
		def doDispense(tws: Seq[TipWell]): Either[Seq[String], Unit] = {
			dispense(builder.toImmutable, mapTipToType, tws) match {
				case Left(lsErrors) =>
					Left(lsErrors)
				case Right(res) =>
					builder.map ++= res.states.map
					actionsD ++= res.actions
					Right(())
			}
		}
		
		// First dispense
		val tws0 = twss.head
		doDispense(tws0) match {
			case Left(lsErrors) => return Left(lsErrors)
			case Right(res) =>
		}
		// Add as many tip/dest groups to this cycle as possible, and return list of remaining groups
		val twssRest = twss.tail.dropWhile(tws => doDispense(tws).isRight)
		
		// Mix
		val actionsDM: Seq[Action] = mixSpec_? match {
			case None => actionsD
			case Some(mixSpec) =>
				actionsD.flatMap(actionD => {
					createMixAction(cycle.state0, actionD, mixSpec) match {
						case Left(lsErrors) => return Left(lsErrors)
						case Right(actionM) => Seq(actionD, actionM)
					}
				})
				
		}
		
		// aspirate
		val mapTipToVolume = cycle.tips.toSeq.map(tip => tip -> -tip.state(builder).nVolume).toMap
		//val mapTipToSrcs = actionsD.flatMap(_.items.map(item => item.tip -> mapDestToItem(item.well).srcs)).toMap
		val twsD = actionsD.flatMap(_.items.map(item => new TipWell(item.tip, item.well)))
		val actionsA: Seq[Aspirate] = aspirate(cycle.state0, twsD, mapTipToVolume) match {
			case Left(lsErrors) => return Left(lsErrors)
			case Right(acts) => acts
		}
		
		val actions = actionsA ++ actionsDM
		Right((actions, twssRest))
	}
	
	protected class DispenseResult(
		val states: RobotState,
		//val mapTipToCleanSpec: Map[TipConfigL2, CleanSpec2],
		val actions: Seq[Dispense]
	)
	
	protected def dispense(
		states0: RobotState,
		//mapTipToCleanSpec0: Map[TipConfigL2, CleanSpec2],
		mapTipToType: Map[TipConfigL2, String],
		tws: Seq[TipWell]
	): Either[Seq[String], DispenseResult] = {
		Right(new DispenseResult(states0, Seq()))
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
		// After choosing which dispenses to perform, then 
		// we'll go back again and see exactly how clean the tips really need to be.
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
	
	private def dropSeq(tips: SortedSet[TipConfigL2]): Seq[L3C_TipsDrop] = {
		if (robot.areTipsDisposable)
			Seq(L3C_TipsDrop(tips))
		else
			Seq()
	}
	
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
	
	private def createCommands(actions: Seq[Action], mixSpec_? : Option[MixSpec]): Seq[Command] = {
		actions.flatMap(action => createCommand(action, mixSpec_?))
	}
	
	private def createCommand(action: Action, mixSpec_? : Option[MixSpec]): Seq[Command] = {
		action match {
			case Dispense(items) =>
				/*mixSpec_? match {
					case Some(mixSpec) =>
						mixSpec.sMixClass_? match {
							case None =>
							case Some(sMixClass) =>
								robot.getPipetteSpec(sMixClass) match {
									case Some(spec) => spec.mix
									case None =>
								}
						}
						val itemsMix = items.map(item => new L2A_MixItem(item.tip, item.well, mixSpec.nVolume, mixSpec.nCount, mixSpec.sMixClass_?))
					case None =>
						robot.batchesForDispense(items).map(items => L2C_Dispense(items))
				}*/
				robot.batchesForDispense(items).map(items => L2C_Dispense(items))
			
			case Mix(items) =>
				Seq(L2C_Mix(items))
				
			case Aspirate(items) =>
				robot.batchesForAspirate(items).map(items => L2C_Aspirate(items))
				
			case Clean(map) =>
				// Replace
				val specsR = map.values.collect({ case v: ReplaceSpec2 => v }).toSeq.sortBy(_.tip)
				val itemsR = specsR.map(spec => new L3A_TipsReplaceItem(spec.tip, Some(spec.sType)))
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
