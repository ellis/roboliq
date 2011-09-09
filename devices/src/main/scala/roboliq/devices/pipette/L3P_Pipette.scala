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
	val dests = SortedSet[WellConfigL2](args.items.map(_.dest) : _*)
	val mixSpec_? : Option[MixSpec] = args.mixSpec_?
	val tipOverrides = args.tipOverrides_? match { case Some(o) => o; case None => TipHandlingOverrides() }
	val bMixOnly = false
	
	val mapDestToItem: Map[WellConfigL2, L3A_PipetteItem] = args.items.map(t => t.dest -> t).toMap

	override protected def dispense(
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

					// If we need to mix, then force wet contact when checking for how to clean
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
	
	override protected def aspirate(
		states: StateMap,
		twsD: Seq[TipWell],
		mapTipToVolume: Map[TipConfigL2, Double]
	): Either[Seq[String], Seq[Aspirate]] = {
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

	/*
	private def mix_createItems(states: RobotState, tws: Seq[TipWell], mixSpec: MixSpec): Either[String, Seq[L2A_MixItem]] = {
		val items = tws.map(tw => {
			getMixPolicy(states, tw) match {
				case Left(sError) => return Left(sError)
				case Right(policy) =>
					new L2A_MixItem(tw.tip, tw.well, mixSpec.nVolume, mixSpec.nCount, policy)
			}
		})
		Right(items)
	}*/

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
	
	private def getDispensePolicy(states: StateMap, tw: TipWell, nVolume: Double): Either[String, PipettePolicy] = {
		getDispensePolicy(states, tw.tip, tw.well, nVolume, cmd.args.sDispenseClass_?)
	}
	
	private def getDispensePolicy(states: StateMap, tip: TipConfigL2, dest: WellConfigL2, nVolume: Double, sDispenseClass_? : Option[String]): Either[String, PipettePolicy] = {
		sDispenseClass_? match {
			case None =>
				val item = mapDestToItem(dest)
				val destState = dest.state(states)
				val liquidSrc = mapDestToItem(dest).srcs.head.obj.state(states).liquid
				robot.getDispensePolicy(liquidSrc, tip, item.nVolume, destState.nVolume) match {
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
	
	/*private def getMixPolicy(states: StateMap, tw: TipWell): Either[String, PipettePolicy] = {
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
	}*/
}
