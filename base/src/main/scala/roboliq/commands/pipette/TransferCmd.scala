package roboliq.commands.pipette

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scalaz._
import Scalaz._
import roboliq.core._,roboliq.entity._,roboliq.processor._,roboliq.events._
import roboliq.devices.pipette.PipetteDevice
import roboliq.commands.pipette.planner._


case class TransferCmd(
	description_? : Option[String],
	source: List[String],
	destination: List[String],
	amount: List[LiquidVolume],
	tipModel_? : Option[TipModel] = None,
	pipettePolicy_? : Option[String] = None,
	sterility_? : Option[CleanIntensity.Value] = None,
	sterilityBefore_? : Option[CleanIntensity.Value] = None,
	sterilityAfter_? : Option[CleanIntensity.Value] = None,
	aspirateMixSpec_? : Option[MixSpecOpt] = None,
	dispenseMixSpec_? : Option[MixSpecOpt] = None
)

class TransferHandler extends CommandHandler[TransferCmd]("pipette.transfer") {
	def handleCmd(cmd: TransferCmd): RqReturn = {
		// List of identifying codes which we'll try to get Sources for
		val code_l = cmd.source ++ cmd.destination
		for {
			codeToSpecs_l <- RqResult.toResultOfList(code_l.map(s => SourceParser.parse(s).map(s -> _)))
			codeToSpecs_m = codeToSpecs_l.toMap
			// Codes which belong to substances
			substance_l = new mutable.HashSet[String]
			// IDs for `source[*]` entities
			codeToSourceIds_m = codeToSpecs_m.map(pair => {
				pair._1 -> (pair._2.map(_ match {
					case SourceSpec_Plate(id) => id
					case SourceSpec_PlateWell(id) => id
					case SourceSpec_Tube(id) => id
					case SourceSpec_Substance(id) => substance_l += id; id //return RqError("substances not allowed as sources for pipette.transfer commands")
					case x => return RqError("the given form of well specification not supported yet here: "+x)
				}))
			})
			sourceId_l = codeToSourceIds_m.toList.flatMap(_._2)
			ret <- fnRequireList(sourceId_l.map(id => lookup[Source](id))) { source_l =>
				val wellId_l = source_l.flatMap(_.vessels.map(_.id))
				fnRequireList(wellId_l.map(id => lookup[VesselSituatedState](id))) { vss_l =>
					fnRequire(
						lookup[PipetteDevice]("default"),
						lookupAll[TipModel],
						lookupAll[TipState]
					) { (device, tipModel_l, tip_l) =>
						val sourceIdToSource_m = (sourceId_l zip source_l).toMap
						val vss_m = vss_l.map(vss => vss.id -> vss).toMap
						val codeToWells_m = codeToSourceIds_m.map(pair => {
							val (code, sourceId_l) = pair
							val source_l = sourceId_l.map(sourceIdToSource_m)
							val wellId_l = source_l.flatMap(_.vessels.map(_.id))
							val well_l = wellId_l.map(vss_m)
							code -> well_l
						})
						val srcAll_ll: List[List[VesselSituatedState]] =
							cmd.source.flatMap(code => {
								if (substance_l.contains(code))
									List(codeToWells_m(code))
								else
									codeToWells_m(code).map(List(_))
							})
						val dstAll_l: List[VesselSituatedState] =
							cmd.destination.flatMap(codeToWells_m)
						
						val n = math.max(srcAll_ll.size, math.max(dstAll_l.size, cmd.amount.size))
						if (srcAll_ll.size != 1 && srcAll_ll.size != n)
							return RqError("`source` must have same length as `destination` and `amount`, or be a singul entity")
						if (dstAll_l.size != 1 && dstAll_l.size != n)
							return RqError("`destination` must have same length as `source` and `amount`, or be a singul entity")
						if (cmd.amount.size != 1 && cmd.amount.size != n)
							return RqError("`amount` must have same length as `source` and `destination`, or be a singul entity")
							
						val l1 = srcAll_ll.zip(dstAll_l).zip(cmd.amount)
						val l = l1.map(tuple => (tuple._1._1, tuple._1._2, tuple._2))
						val item_l = l.map(tuple => {
							val (src_l, dst, volume) = tuple
							TransferPlanner2.Item(src_l, vss_m(dst.id), volume)
						})
		
						val tipModel_? = cmd.tipModel_? match {
							case Some(tipModel) => RqSuccess(tipModel)
							case None =>
								val itemToLiquid_m = item_l.map(item => item -> item.dst.liquid).toMap
								val itemToModels_m = item_l.map(item => item -> device.getDispenseAllowableTipModels(tipModel_l, item.src_l.head.liquid, item.volume)).toMap
								val tipModelSearcher = new scheduler.TipModelSearcher1[TransferPlanner2.Item, Liquid, TipModel]
								val itemToTipModel_m_? = tipModelSearcher.searchGraph(item_l, itemToLiquid_m, itemToModels_m)
								itemToTipModel_m_?.map(_.values.toSet.head)
						}
						
						// FIXME: need to make PipettePolicy and entity to be loaded from database
						val pipettePolicy = cmd.pipettePolicy_?.map(s => PipettePolicy(s, PipettePosition.Free)).getOrElse(PipettePolicy("POLICY", PipettePosition.Free))
						for {
							tipModel <- tipModel_?
							batch_l <- planner.TransferPlanner2.searchGraph(
								device,
								SortedSet(tip_l.map(_.conf).toSeq : _*),
								tipModel,
								pipettePolicy,
								item_l
							)
							cmd_l = makeBatches(device, cmd, batch_l, tip_l, pipettePolicy)
							ret <- output(
								cmd_l
							)
						} yield ret
					}
				}
			}
		} yield ret
	}
	
	/**
	 * Simplifications for planning:
	 * - only deal with one tip model
	 */
	
	/**
	 * Think about creating a PDDL representation for this problem or its subproblems.
	 * types { }
	 */
	
	private def makeBatches(
		device: PipetteDevice,
		cmd: TransferCmd,
		batch_l: List[TransferPlanner2.Batch],
		tip0_l: List[TipState],
		policy: PipettePolicy
	): List[Cmd] = {
		//var rest = batch_l
		val tipSterilized_l = new mutable.HashSet[Tip]
		var tip_l = tip0_l
		//var tipToSterility: Map[TipState, CleanIntensity.Value] = tip_l.map(tip => tip -> tip.cleanDegreePending).toMap
		val batchesCmd_l = batch_l.flatMap(batch => {
			// Create TipWellVolumePolicy lists from item and tip lists
			val twvpA_l = batch.item_l.map(item => {
				val tipState = TipState.createEmpty(item.tip)
				TipWellVolumePolicy(tipState, item.src, item.volume, policy)
			})
			// Dispenses TWVP
			val twvpD_l = batch.item_l.map(item => {
				val tipState = TipState.createEmpty(item.tip)
				TipWellVolumePolicy(tipState, item.dst, item.volume, policy)
			})
			// Group the TWVPs into groups that can be performed simultaneously
			val twvpA_ll = device.groupSpirateItems(twvpA_l)
			val twvpD_ll = device.groupSpirateItems(twvpD_l)
			
			val tipToTipCleanPolicy_m: Map[TipState, TipCleanPolicy] =
				getTipCleanPolicies(None, Nil, twvpA_l, twvpD_l)
			val tipCmd_? = makeTipsCmds(tipToTipCleanPolicy_m)
			// Note tips which have been sterilized
			tipSterilized_l ++= tipCmd_?.map(_.items.map(_.tip.conf)).getOrElse(Nil)

			// Create mixing commands
			val premix_l = makeMixCmds(cmd.aspirateMixSpec_?, twvpA_ll)
			val postmix_l = makeMixCmds(cmd.dispenseMixSpec_?, twvpD_ll)
			// Create aspriate and dispense commands
			val asp_l = twvpA_ll.map(twvp_l => low.AspirateCmd(None, twvp_l))
			val disp_l = twvpD_ll.map(twvp_l => low.DispenseCmd(None, twvp_l)) 

			tipCmd_? ++
			premix_l ++
			asp_l ++
			disp_l ++
			postmix_l
		})
		
		val tipsAfter_? = {
			// List of tips which haven't been 
			val tip_l = tip0_l.filter(tipState => !tipSterilized_l.contains(tipState.conf) && tipState.cleanDegreePending > CleanIntensity.None)
			
			val tipToTipCleanPolicy_m: Map[TipState, TipCleanPolicy] =
				getTipCleanPolicies(cmd.sterilityAfter_?, tip_l, Nil, Nil)
			makeTipsCmds(tipToTipCleanPolicy_m)
		}
		
		batchesCmd_l ++ tipsAfter_?
	}

	private def getTipCleanPolicies(
		sterilize_? : Option[CleanIntensity.Value],
		tipState_l: List[TipState],
		twvpA_l: List[TipWellVolumePolicy],
		twvpD_l: List[TipWellVolumePolicy]
	): Map[TipState, TipCleanPolicy] = {
		sterilize_? match {
			case Some(cleanIntensity) =>
				twvpA_l.map(twvp => {
					twvp.tip -> TipCleanPolicy(cleanIntensity, cleanIntensity)
				}).toMap
			case None =>
				// TipCleanPolicies pending from prior pipetting
				val cleanT_m = tipState_l.map(tip => tip -> TipCleanPolicy(tip.cleanDegreePending, CleanIntensity.None)).toMap

				// Aspirate TipCleanPolicies
				val cleanA_m = twvpA_l.map(twvp => twvp.tip -> twvp.well.liquid.tipCleanPolicy).toMap
				// Dispense TipCleanPolicies
				val cleanD_m: Map[TipState, TipCleanPolicy] = {
					twvpD_l.map(twvp => twvp.tip -> (
						if (twvp.policy.pos == PipettePosition.WetContact)
							twvp.well.liquid.tipCleanPolicy
						else
							TipCleanPolicy.NN
					)).toMap
				}
				
				cleanT_m |+| cleanA_m |+| cleanD_m
		}
	}
	
	private def makeTipsCmds(
		tipToTipCleanPolicy_m: Map[TipState, TipCleanPolicy]
	): Option[TipsCmd] = {
		if (tipToTipCleanPolicy_m.isEmpty)
			return None
		
		val items = tipToTipCleanPolicy_m.toList.map(pair => {
			val (tip, cleanPolicy) = pair
			// FIXME: set Some(tipModel)
			TipsItem(tip, None, Some(cleanPolicy.enter))
		})
		Some(TipsCmd(None, Nil, None, None, items))
	}
	
	private def makeMixCmds(mixSpecOpt_? : Option[MixSpecOpt], twvp_ll: List[List[TipWellVolumePolicy]]): List[low.MixCmd] = {
		mixSpecOpt_? match {
			case None => Nil
			case _ =>
				twvp_ll.map(twvp_l => {
					val items = twvp_l.map(twvp => low.MixItem(twvp.tip, twvp.well, None))
					low.MixCmd(None, items, mixSpecOpt_?)
				})
		}
	}
}
