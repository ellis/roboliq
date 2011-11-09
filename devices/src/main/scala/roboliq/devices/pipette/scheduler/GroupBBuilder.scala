package roboliq.devices.pipette.scheduler

import scala.collection.immutable.SortedSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

import roboliq.common._
import roboliq.commands._
import roboliq.commands.pipette._
import roboliq.commands.pipette.{L3A_PipetteItem => Item}
import roboliq.compiler._
import roboliq.devices.pipette._


class GroupBBuilder(
	val device: PipetteDevice,
	val ctx: CompilerContextL3
) {
	val lTipAll: SortedSet[TipConfigL2] = device.config.tips.map(_.state(ctx.states).conf)
	
	/**
	 * @param lTipCleanable0 tip which can potentially be washed at an earlier stage
	 */
	def tr_groupB(
		groupA: GroupA,
		lTipCleanable0: SortedSet[TipConfigL2]
	): Result[GroupB] = {
		val lAspirate = groupSpirateItems(groupA, groupA.lAspirate).map(items => L2C_Aspirate(items))
		val lDispense = groupSpirateItems(groupA, groupA.lDispense).map(items => L2C_Dispense(items))
		
		// Tip which require cleaning before aspirate
		val cleans0 = getCleanSpec2(groupA)
		// Tips which cannot be cleaned in a prior group
		val cleans1 = cleans0.keySet -- lTipCleanable0
		// Tips which are flagged to cleaning in a prior group
		val precleans = if (cleans1.isEmpty) cleans0 else Map[TipConfigL2, CleanSpec2]()
		// Tips which will be cleaned in this group
		val cleans = if (cleans1.isEmpty) Map[TipConfigL2, CleanSpec2]() else cleans0
		
		// Indicate which tips could be cleaned earlier for the NEXT group
		val lTipCleanable: SortedSet[TipConfigL2] = {
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
			scoreCleans(cleans)
		}
			
		val groupB = GroupB(
			groupA.lItem,
			groupA.bClean,
			precleans = precleans,
			cleans = cleans,
			lTipCleanable = lTipCleanable,
			Nil,
			lAspirate = lAspirate,
			lDispense = lDispense,
			Nil,
			nScore
		)
		Success(groupB)
	}
	
	def groupSpirateItems(
		groupA: GroupA,
		lTwvp: Seq[TipWellVolumePolicy]
	): Seq[Seq[L2A_SpirateItem]] = {
		/*
		// Group by: tipModel, pipettePolicy
		val ma: Map[Tuple2[TipModel, PipettePolicy], Seq[TipWellVolumePolicy]]
			= lTwvp.groupBy(twvp => (groupA.mTipToLM(twvp.tip).tipModel, twvp.policy))
		// Order of (tipModel, pipettePolicy)
		val lMP: Seq[Tuple2[TipModel, PipettePolicy]]
			= lTwvp.map(twvp => (groupA.mTipToLM(twvp.tip).tipModel, twvp.policy)).distinct
		// TODO: Make sure that the order of dispenses to a given well always remains the same
		// Create list of list of spirate items
		lMP.map(mp => {
			ma(mp).map(twvp => new L2A_SpirateItem(twvp.tip, twvp.well, twvp.nVolume, twvp.policy))
		})
		*/
		
		val x = lTwvp.foldLeft(List[List[TipWellVolumePolicy]]())(groupSpirateItems_add(groupA))
		val y = x.reverse.map(_.reverse)
		y.map(_.map(twvp => new L2A_SpirateItem(twvp.tip, twvp.well, twvp.nVolume, twvp.policy)))
		//val x1 = new ArrayBuffer[Seq[L2A_SpirateItem]]
		//var x2 = new ArrayBuffer[L2A_SpirateItem]
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
					device.canBatchSpirateItems(xs2)
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
	): Map[TipConfigL2, CleanSpec2] = {
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
		mapTipToModel: Map[TipConfigL2, TipModel],
		tip: TipConfigL2,
		cleanSpec: WashSpec
	): Option[CleanSpec2] = {
		if (cleanSpec.washIntensity == WashIntensity.None) {
			None
		}
		else if (device.areTipsDisposable) {
			val tipState = tip.obj.state(states)
			val bReplace = tipState.model_?.isEmpty || (overrides.replacement_? match {
				case Some(TipReplacementPolicy.ReplaceAlways) => true
				case Some(TipReplacementPolicy.KeepBetween) => false
				case Some(TipReplacementPolicy.KeepAlways) => false
				case None => false
			})
			if (bReplace)
				Some(ReplaceSpec2(tip, mapTipToModel.getOrElse(tip, tipState.model_?.get)))
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
			acc + (if (cmd.items.forall(_.policy.pos == PipettePosition.Free)) 1.0 else 2.0)
		})
	}
	
	def scoreDispenses(lDispense: Seq[L2C_Dispense]): Double = {
		val nCostStart = if (lDispense.isEmpty) 0.0 else 5.0
		lDispense.foldLeft(nCostStart)((acc, cmd) => {
			acc + (if (cmd.items.forall(_.policy.pos == PipettePosition.Free)) 1.0 else 2.0)
		})
	}
	
	def scoreCleans(cleans: Map[TipConfigL2, CleanSpec2]): Double = {
		// TODO: if we need to wash tip groups separately, then the costs will increase!
		val nCostStart = if (cleans.isEmpty) 0.0 else 40.0
		nCostStart
	}
}
