package roboliq.commands.pipette.scheduler

import scala.collection.immutable.SortedSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import roboliq.core._
import roboliq.commands.pipette._
//import roboliq.compiler._
import roboliq.devices.pipette._


class GroupBBuilder(
	val device: PipetteDevice,
	val ctx: CompilerContextL3
) {
	val lTipAll: SortedSet[Tip] = device.config.tips.map(_.state(ctx.states).conf)
	
	/**
	 * @param lTipCleanable0 tip which can potentially be washed at an earlier stage
	 */
	def tr_groupB(
		groupA: GroupA,
		lTipCleanable0: SortedSet[Tip]
	): Result[GroupB] = {
		val lAspirate = groupSpirateItems(groupA, groupA.lAspirate).map(items => L2C_Aspirate(items))
		val lDispense = groupSpirateItems(groupA, groupA.lDispense).map(items => L2C_Dispense(items))
		val lPremix = groupMixItems(groupA, groupA.lPremix).map(items => L2C_Mix(items))
		val lPostmix = groupMixItems(groupA, groupA.lPostmix).map(items => L2C_Mix(items))
		
		//println("groupA:"+groupA)
		
		// Tip which require cleaning before aspirate
		val cleans0 = getCleanSpec2(groupA)
		// Tips which cannot be cleaned in a prior group
		val cleans1 = cleans0.keySet -- lTipCleanable0
		// Tips which are flagged to cleaning in a prior group
		val precleans = if (cleans1.isEmpty) cleans0 else Map[Tip, CleanSpec2]()
		// Tips which will be cleaned in this group
		val cleans = if (cleans1.isEmpty) Map[Tip, CleanSpec2]() else cleans0
		
		// Indicate which tips could be cleaned earlier for the NEXT group
		val lTipCleanable: SortedSet[Tip] = {
			if (precleans.size + cleans.size == 0)
				lTipCleanable0
			else if (cleans.isEmpty)
				lTipCleanable0 -- precleans.keySet
			else {
				val lTipCleaning = SortedSet(cleans.keys.toSeq : _*)
				device.getOtherTipsWhichCanBeCleanedSimultaneously(lTipAll, lTipCleaning) -- cleans.keySet
			}
		}
		
		// Score
		// TODO: if cleaning is moved back to preceding group, that cleaning procedure might become more expensive
		//  take that into consideration when calculating path cost
		val nScore = {
			scoreAspirates(lAspirate) +
			scoreDispenses(lDispense) +
			scoreCleans(cleans) +
			scorePostmixes(lDispense.size, lPostmix)
		}
			
		val groupB = GroupB(
			groupA.lItem,
			groupA.bClean,
			precleans = precleans,
			cleans = cleans,
			lTipCleanable = lTipCleanable,
			Nil,
			lPremix = lPremix,
			lAspirate = lAspirate,
			lDispense = lDispense,
			lPostmix = lPostmix,
			nScore
		)
		Success(groupB)
	}
	
	def groupSpirateItems(
		groupA: GroupA,
		lTwvp: Seq[TipWellVolumePolicy]
	): Seq[Seq[L2A_SpirateItem]] = {
		val x = lTwvp.foldLeft(List[List[TipWellVolumePolicy]]())(groupSpirateItems_add(groupA))
		val y = x.reverse.map(_.reverse)
		y.map(_.map(twvp => new L2A_SpirateItem(twvp.tip, twvp.well, twvp.nVolume, twvp.policy)))
	}
	
	def groupSpirateItems_add(groupA: GroupA)(
		acc: List[List[TipWellVolumePolicy]],
		twvp: TipWellVolumePolicy
	): List[List[TipWellVolumePolicy]] = {
		acc match {
			case (xs @ List(x0, _*)) :: rest  =>
				val tipModel = groupA.mTipToLM(twvp.tip).tipModel
				val tipModel0 = groupA.mTipToLM(x0.tip).tipModel
				val xs2 = twvp :: xs
				if (
					tipModel.eq(tipModel0) && 
					twvp.policy == x0.policy && 
					// tip not already used
					!xs.exists(twvp.tip eq _.tip) &&
					// well not already visited
					!xs.exists(twvp.well eq _.well) && 
					device.canBatchSpirateItems(groupA.states0, xs2)
				)
					xs2 :: rest
				else
					List(twvp) :: xs :: rest
			case _ =>
				List(List(twvp))
		}
	}
	
	def groupMixItems(
		groupA: GroupA,
		lTwvp: Seq[TipWellMix]
	): Seq[Seq[L2A_MixItem]] = {
		val x = lTwvp.foldLeft(List[List[TipWellMix]]())(groupMixItems_add(groupA))
		val y = x.reverse.map(_.reverse)
		y.map(_.map(twvp => new L2A_MixItem(twvp.tip, twvp.well, twvp.mixSpec.nVolume, twvp.mixSpec.nCount, twvp.mixSpec.mixPolicy)))
	}
	
	def groupMixItems_add(groupA: GroupA)(
		acc: List[List[TipWellMix]],
		twvp: TipWellMix
	): List[List[TipWellMix]] = {
		acc match {
			case (xs @ List(x0, _*)) :: rest  =>
				val tipModel = groupA.mTipToLM(twvp.tip).tipModel
				val tipModel0 = groupA.mTipToLM(x0.tip).tipModel
				val xs2 = twvp :: xs
				if (
					tipModel.eq(tipModel0) && 
					twvp.mixSpec.mixPolicy == x0.mixSpec.mixPolicy && 
					device.canBatchMixItems(groupA.states0, xs2)
				)
					xs2 :: rest
				else
					List(twvp) :: xs :: rest
			case _ =>
				List(List(twvp))
		}
	}
	
	private def getCleanSpec2(
		groupA: GroupA
	): Map[Tip, CleanSpec2] = {
		val mTipToModel = groupA.mTipToLM.mapValues(_.tipModel)
		// Tip which require cleaning before aspirate
		groupA.mTipToCleanSpec.map(pair => {
			val (tip, cleanSpec) = pair
			tip -> getCleanSpec2(groupA.states0, TipHandlingOverrides(), mTipToModel, tip, cleanSpec)
		}).collect({ case (tip, Some(cleanSpec2)) => tip -> cleanSpec2 })
	}

	private def getCleanSpec2(
		states: StateMap,
		overrides: TipHandlingOverrides,
		mTipToModel: Map[Tip, TipModel],
		tip: Tip,
		cleanSpec: WashSpec
	): Option[CleanSpec2] = {
		if (cleanSpec.washIntensity == WashIntensity.None) {
			None
		}
		else if (device.areTipsDisposable) {
			val tipState = tip.obj.state(states)
			val bGetTip = tipState.model_?.isEmpty && mTipToModel.contains(tip)
			val bDropTip = !tipState.model_?.isEmpty && !mTipToModel.contains(tip)
			val bReplace = bGetTip || (overrides.replacement_? match {
				case Some(TipReplacementPolicy.ReplaceAlways) => true
				case Some(TipReplacementPolicy.KeepBetween) => false
				case Some(TipReplacementPolicy.KeepAlways) => false
				case None => false
			})
			if (bDropTip)
				Some(DropSpec2(tip))
			else if (bReplace)
				Some(ReplaceSpec2(tip, mTipToModel.getOrElse(tip, tipState.model_?.get)))
			else
				None
		}
		else {
			val tipState = tip.obj.state(states)
			if (cleanSpec.washIntensity > WashIntensity.None) Some(WashSpec2(tip, cleanSpec))
			else None
		}
	}
	
	def scoreAspirates(lAspirate: Seq[L2C_Aspirate]): Double = {
		val nCostStart = if (lAspirate.isEmpty) 0.0 else 5.0
		lAspirate.foldLeft(nCostStart)((acc, cmd) => {
			acc + 2.0
		})
	}
	
	def scoreDispenses(lDispense: Seq[L2C_Dispense]): Double = {
		val nCostStart = if (lDispense.isEmpty) 0.0 else 5.0
		lDispense.foldLeft(nCostStart)((acc, cmd) => {
			acc + (if (cmd.items.forall(_.policy.pos == PipettePosition.Free)) 1.0 else 2.0)
		})
	}
	
	def scorePostmixes(nDispense: Int, lMix: Seq[L2C_Mix]): Double = {
		if (lMix.isEmpty) return 0.0
		
		val nCostStart = if (nDispense == 0) 5.0 else if (nDispense == 1) 0.0 else 1.0
		lMix.foldLeft(nCostStart)((acc, cmd) => {
			acc + 5.0
		})
	}
	
	def scoreCleans(cleans: Map[Tip, CleanSpec2]): Double = {
		// TODO: if we need to wash tip groups separately, then the costs will increase!
		val nCostStart = if (cleans.isEmpty) 0.0 else 40.0
		nCostStart
	}
}
