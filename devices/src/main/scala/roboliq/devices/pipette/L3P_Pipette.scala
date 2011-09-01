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
	
	val dests = SortedSet[WellConfigL2](args.items.map(_.dest) : _*)
	val mapDestToItem: Map[WellConfigL2, L3A_PipetteItem] = args.items.map(t => t.dest -> t).toMap

	protected override def translateCommand(tips: SortedSet[TipConfigL2], mapTipToType: Map[TipConfigL2, String]): Either[Errors, Seq[CycleState]] = {
		val cycles = new ArrayBuffer[CycleState]
		
		// Pair up all tips and wells
		val twss0 = PipetteHelper.chooseTipWellPairsAll(ctx.states, tips, dests)
		
		def createCycles(twss: List[Seq[TipWell]], stateCycle0: RobotState): Either[Errors, Unit] = {
			if (twss.isEmpty)
				return Right()
			
			val cycle = new CycleState(tips, stateCycle0)
			// First tip/dest pairs for dispense
			val tws0 = twss.head
			
			val builder = new StateBuilder(stateCycle0)
			
			val mapTipToCleanSpec = new HashMap[TipConfigL2, CleanSpec2]
			val actionsD = new ArrayBuffer[Dispense]
			
			// Temporarily assume that the tips are perfectly clean
			// After choosing which dispenses to perform, then 
			// we'll go back again and see exactly how clean the tips really need to be.
			cleanTipStates(builder, mapTipToType)

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
			val actionsDM: Seq[Action] = args.mixSpec_? match {
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
			val mapTipToSrcs = actionsD.flatMap(_.items.map(item => item.tip -> mapDestToItem(item.well).srcs)).toMap
			val actionsA: Seq[Aspirate] = aspirate(stateCycle0, tips, mapTipToSrcs, mapTipToVolume) match {
				case Left(lsErrors) => return Left(lsErrors)
				case Right(acts) => acts
			}

			// Now we know all the dispenses and mixes we'll perform,
			// so we can go back and determine how to clean the tips based on the 
			// real starting state rather than the perfectly clean state we assumed.
			mapTipToCleanSpec.clear()
			val actions = new ArrayBuffer[Action]
			actions ++= actionsA
			actions ++= actionsDM
			var bFirst = cycles.isEmpty
			for (action <- actions) {
				val specs_? = action match {
					case Aspirate(items) => items.map(item => getAspirateCleanSpec(stateCycle0, mapTipToType, tipOverrides, bFirst, item))
					case Dispense(items) => items.map(item => getDispenseCleanSpec(stateCycle0, mapTipToType, tipOverrides, item.tip, item.well, item.policy.pos))
					case Mix(items) => items.map(item => getMixCleanSpec(stateCycle0, mapTipToType, tipOverrides, bFirst, item.tip, item.well))
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
			actions.insert(0, Clean(mapTipToCleanSpec.toMap))
			
			val cmds = createCommands(actions, cmd.args.mixSpec_?)
			
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
	
	private class DispenseResult(
		val states: RobotState,
		val mapTipToCleanSpec: Map[TipConfigL2, CleanSpec2],
		val actions: Seq[Dispense]
	)
	
	private def dispense(
		states0: RobotState,
		mapTipToCleanSpec0: Map[TipConfigL2, CleanSpec2],
		mapTipToType: Map[TipConfigL2, String],
		tws: Seq[TipWell]
	): Either[Seq[String], DispenseResult] = {
		dispense_createItems(states0, tws) match {
			case Left(sError) =>
				return Left(Seq(sError))
			case Right(items) =>
				val builder = new StateBuilder(states0)		
				val mapTipToCleanSpec = HashMap(mapTipToCleanSpec0.toSeq : _*)
				items.foreach(item => {
					val tip = item.tip
					val dest = item.well
					val tipWriter = tip.obj.stateWriter(builder)
					val destWriter = dest.obj.stateWriter(builder)
					val liquidTip0 = tipWriter.state.liquid
					val liquidSrc = mapDestToItem(dest).srcs.head.obj.state(builder).liquid
					val liquidDest = destWriter.state.liquid
					
					// Check liquid
					// If the tip hasn't been used for aspiration yet, associate the source liquid with it
					if (liquidTip0 eq Liquid.empty) {
						tipWriter.aspirate(liquidSrc, 0)
					}
					// If we would need to aspirate a new liquid, abort
					else if (liquidSrc ne liquidTip0) {
						return Left(Seq("INTERNAL: Error code dispense 1"))
					}
					
					// check volumes
					dispense_checkVol(builder, tip, dest) match {
						case Some(sError) => return Left(Seq(sError))
						case _ =>
					}

					// If we need to mix, then force wet contact when mixing
					val pos = args.mixSpec_? match {
						case None => item.policy.pos
						case _ => PipettePosition.WetContact
					}
					
					// Check whether this dispense would require a cleaning
					getDispenseCleanSpec(builder, mapTipToType, tipOverrides, item.tip, item.well, pos) match {
						case None =>
						case Some(spec) =>
							mapTipToCleanSpec.get(item.tip) match {
								case Some(_) =>
									return Left(Seq("INTERNAL: Error code dispense 2"))
								case None =>
									mapTipToCleanSpec(item.tip) = spec
							}
					}
					
					// Update tip and destination states
					tipWriter.dispense(item.nVolume, liquidDest, item.policy.pos)
					destWriter.add(liquidSrc, item.nVolume)
				})
				val actions = Seq(Dispense(items))
				Right(new DispenseResult(builder.toImmutable, mapTipToCleanSpec.toMap, actions))
		}
	}
	
	private def aspirate(
		states: StateMap,
		tips: SortedSet[TipConfigL2],
		mapTipToSrcs: Map[TipConfigL2, Set[WellConfigL2]],
		mapTipToVolume: Map[TipConfigL2, Double]
	): Either[Seq[String], Seq[Aspirate]] = {
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
		val actions = for (tws <- twss) yield {
			aspirate_createItems(states, mapTipToVolume, tws) match {
				case Left(sError) =>
					return Left(Seq(sError))
				case Right(items) =>
					Aspirate(items)
			}
		}
		Right(actions)
	}
	
	// Check for appropriate volumes
	private def dispense_checkVol(states: StateMap, tip: TipConfigL2, dest: WellConfigL2): Option[String] = {
		val item = mapDestToItem(dest)
		val tipState = tip.obj.state(states)
		val liquidSrc = tipState.liquid // since we've already aspirated the source liquid
		val nMin = robot.getTipAspirateVolumeMin(tipState, liquidSrc)
		val nMax = robot.getTipHoldVolumeMax(tipState, liquidSrc)
		val nTipVolume = -tipState.nVolume
		
		// TODO: make sure that source well is not over-aspirated
		// TODO: make sure that destination well is not over-filled
		if (item.nVolume + nTipVolume < nMin)
			Some("Cannot aspirate "+item.nVolume+"ul into tip "+(tip.index+1)+": require >= "+nMin+"ul")
		else if (item.nVolume + nTipVolume > nMax)
			Some("Cannot aspirate "+item.nVolume+"ul into tip "+(tip.index+1)+": require <= "+nMax+"ul")
		else
			None
	}

	private def dispense_createItems(states: RobotState, tws: Seq[TipWell]): Either[String, Seq[L2A_SpirateItem]] = {
		val items = tws.map(tw => {
			val item = mapDestToItem(tw.well)
			getDispensePolicy(states, tw, item.nVolume) match {
				case Left(sError) => return Left(sError)
				case Right(policy) =>
					new L2A_SpirateItem(tw.tip, tw.well, item.nVolume, policy)
			}
		})
		Right(items)
	}

	private def mix_createItems(states: RobotState, tws: Seq[TipWell], mixSpec: MixSpec): Either[String, Seq[L2A_MixItem]] = {
		val items = tws.map(tw => {
			getMixPolicy(states, tw) match {
				case Left(sError) => return Left(sError)
				case Right(policy) =>
					new L2A_MixItem(tw.tip, tw.well, mixSpec.nVolume, mixSpec.nCount, policy)
			}
		})
		Right(items)
	}

	private def aspirate_chooseTipWellPairs_liquid(states: StateMap, tips: SortedSet[TipConfigL2], srcs: Set[WellConfigL2]): Seq[Seq[TipWell]] = {
		val srcs2 = PipetteHelper.chooseAdjacentWellsByVolume(states, srcs, tips.size)
		PipetteHelper.chooseTipSrcPairs(states, tips, srcs2)
	}

	private def aspirate_chooseTipWellPairs_direct(states: StateMap, srcs: collection.Map[TipConfigL2, Set[WellConfigL2]]): Seq[Seq[TipWell]] = {
		//println("srcs: "+srcs)
		val tws: Seq[TipWell] = srcs.toSeq.sortBy(_._1).map(pair => new TipWell(pair._1, pair._2.head))
		val twss = PipetteHelper.splitTipWellPairs(tws)
		twss
	}
	
	private def aspirate_createItems(states: StateMap, mapTipToVolume: Map[TipConfigL2, Double], tws: Seq[TipWell]): Either[String, Seq[L2A_SpirateItem]] = {
		val items = tws.map(tw => {
			getAspiratePolicy(states, tw) match {
				case Left(sError) => return Left(sError)
				case Right(policy) =>
					new L2A_SpirateItem(tw.tip, tw.well, mapTipToVolume(tw.tip), policy)
			}
		})
		Right(items)
	}
	
	private def createCommands(actions: Seq[Action], mixSpec_? : Option[MixSpec]): Seq[Command] = {
		actions.flatMap(action => createCommand(action, mixSpec_?))
	}
	
	private def getDispensePolicy(states: StateMap, tw: TipWell, nVolume: Double): Either[String, PipettePolicy] = {
		getDispensePolicy(states, tw.tip, tw.well, nVolume, cmd.args.sDispenseClass_?)
	}
	
	private def getDispensePolicy(states: StateMap, tip: TipConfigL2, dest: WellConfigL2, nVolume: Double, sDispenseClass_? : Option[String]): Either[String, PipettePolicy] = {
		sDispenseClass_? match {
			case None =>
				val item = mapDestToItem(dest)
				val tipState = tip.state(states)
				val destState = dest.state(states)
				robot.getDispensePolicy(tipState, destState, item.nVolume) match {
					case None => Left("no dispense policy found for "+tip+" and "+dest)
					case Some(policy) => Right(policy)
				}
			case Some(sLiquidClass) =>
				robot.getPipetteSpec(sLiquidClass) match {
					case None => Left("no policy found for class "+sLiquidClass)
					case Some(spec) => Right(new PipettePolicy(spec.sName, spec.dispense))
				}
		}
	}
	
	private def getAspiratePolicy(states: StateMap, tw: TipWell): Either[String, PipettePolicy] = {
		getAspiratePolicy(states, tw.tip, tw.well, cmd.args.sAspirateClass_?)
	}
	
	private def getAspiratePolicy(states: StateMap, tip: TipConfigL2, src: WellConfigL2, sAspirateClass_? : Option[String]): Either[String, PipettePolicy] = {
		sAspirateClass_? match {
			case None =>
				val tipState = tip.state(states)
				val srcState = src.state(states)
				robot.getAspiratePolicy(tipState, srcState) match {
					case None => Left("no aspirate policy found for "+tip+" and "+src)
					case Some(policy) => Right(policy)
				}
			case Some(sLiquidClass) =>
				robot.getPipetteSpec(sLiquidClass) match {
					case None => Left("no policy found for class "+sLiquidClass)
					case Some(spec) => Right(new PipettePolicy(spec.sName, spec.aspirate))
				}
		}
	}
	
	private def getMixPolicy(states: StateMap, tw: TipWell): Either[String, PipettePolicy] = {
		val sMixClass_? = cmd.args.mixSpec_? match {
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
				val specsR = map.values.filter(_.isInstanceOf[ReplaceSpec2]).map(_.asInstanceOf[ReplaceSpec2])
				val itemsR = specsR.map(spec => new L3A_TipsReplaceItem(spec.tip, Some(spec.sType)))
				val cmdsR = if (itemsR.isEmpty) Seq() else Seq(L3C_TipsReplace(itemsR.toSeq))

				val specsW = map.values.filter(_.isInstanceOf[WashSpec2]).map(_.asInstanceOf[WashSpec2])
				val intensity = specsW.foldLeft(WashIntensity.None) { (acc, spec) => if (spec.spec.washIntensity > acc) spec.spec.washIntensity else acc }
				val itemsW = specsW.map(spec => new L3A_TipsWashItem(spec.tip, spec.spec.contamInside, spec.spec.contamOutside))
				val cmdsW = if (itemsW.isEmpty) Seq() else Seq(L3C_TipsWash(itemsW.toSeq, intensity))
				
				cmdsR ++ cmdsW
		}
	}
}
