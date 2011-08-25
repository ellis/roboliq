package roboliq.commands.pipette

import roboliq.common._

//-------------------------------
// Aspirate
//-------------------------------

case class L2C_Aspirate(items: Seq[L2A_SpirateItem]) extends CommandL2 {
	type L1Type = L1C_Aspirate
	
	def updateState(builder: StateBuilder) {
		for (item <- items) {
			val wellState = item.well.obj.state(builder)
			item.tip.obj.stateWriter(builder).aspirate(wellState.liquid, item.nVolume)
			item.well.obj.stateWriter(builder).remove(item.nVolume)
		}
	}
	
	def toL1(states: RobotState): Either[Seq[String], L1Type] = {
		Right(L1C_Aspirate(items.map(_.toL1(states))))
	}
}

case class L1C_Aspirate(items: Seq[L1A_SpirateItem]) extends CommandL1

sealed class L2A_SpirateItem(
	val tip: TipConfigL2,
	val well: WellConfigL2,
	val nVolume: Double,
	val policy: PipettePolicy
) extends HasTipWellVolumePolicy {
	def toL1(states: RobotState) = {
		new L1A_SpirateItem(this, well.holder.obj.state(states).location)
	}
}

sealed class L1A_SpirateItem(
	val itemL2: L2A_SpirateItem,
	val location: String
) {
	def tip = itemL2.tip
	def well = itemL2.well
	def nVolume = itemL2.nVolume
	def policy = itemL2.policy
}

//-------------------------------
// Dispense
//-------------------------------

case class L2C_Dispense(items: Seq[L2A_SpirateItem]) extends CommandL2 {
	type L1Type = L1C_Dispense
	
	def updateState(builder: StateBuilder) {
		for (item <- items) {
			val tipState = item.tip.obj.state(builder)
			val wellState = item.well.obj.state(builder)
			item.tip.obj.stateWriter(builder).dispense(item.nVolume, wellState.liquid, item.policy.pos)
			item.well.obj.stateWriter(builder).add(tipState.liquid, item.nVolume)
		}
	}
	
	def toL1(states: RobotState): Either[Seq[String], L1Type] = {
		Right(L1C_Dispense(items.map(_.toL1(states))))
	}
}

case class L1C_Dispense(items: Seq[L1A_SpirateItem]) extends CommandL1

//-------------------------------
// Mix
//-------------------------------

case class L2C_Mix(items: Seq[L2A_MixItem]) extends CommandL2 {
	type L1Type = L1C_Mix
	
	def updateState(builder: StateBuilder) {
		for (item <- items) {
			val wellState = item.well.obj.state(builder)
			item.tip.obj.stateWriter(builder).mix(wellState.liquid, item.nVolume)
		}
	}
	
	def toL1(states: RobotState): Either[Seq[String], L1Type] = {
		Right(L1C_Mix(items.map(_.toL1(states))))
	}
}

case class L1C_Mix(items: Seq[L1A_MixItem]) extends CommandL1

sealed class L2A_MixItem(
	val tip: TipConfigL2,
	val well: WellConfigL2,
	val nVolume: Double,
	val nCount: Int,
	val policy: PipettePolicy
) extends HasTipWellVolumePolicy {
	def toL1(states: RobotState) = {
		new L1A_MixItem(this, well.holder.obj.state(states).location)
	}
}

sealed class L1A_MixItem(
	val itemL2: L2A_MixItem,
	val location: String
) {
	def tip = itemL2.tip
	def well = itemL2.well
	def nVolume = itemL2.nVolume
	def nCount = itemL2.nCount
	def policy = itemL2.policy
}

//-------------------------------
// Wash
//-------------------------------

case class L2C_Wash(args: L2A_WashArgs) extends CommandL2 {
	type L1Type = L1C_Wash
	
	def updateState(builder: StateBuilder) {
		for (tip <- args.tips) {
			tip.obj.stateWriter(builder).clean(args.degree)
		}
	}
	
	def toL1(states: RobotState): Either[Seq[String], L1Type] = {
		Right(L1C_Wash(args))
	}
}

case class L1C_Wash(args: L2A_WashArgs) extends CommandL1

sealed class L2A_WashArgs(
	val tips: Set[TipConfigL2],
	val degree: CleanDegree.Value,
	val iWashProgram: Int)

/*sealed class L1A_WashArgs(
	val tips: Set[TipConfigL2],
	val degree: CleanDegree.Value,
	val iWashProgram: Int)*/

//-------------------------------
// 
//-------------------------------

case class L2C_TipsDrop(tips: Set[Tip])
case class L2C_TipsGet(tips: Set[Tip]) // FIXME: add tip kind
/*case class T0_Wash(
	mTips: Int,
	iWasteGrid: Int, iWasteSite: Int,
	iCleanerGrid: Int, iCleanerSite: Int,
	nWasteVolume: Double,
	nWasteDelay: Int,
	nCleanerVolume: Double,
	nCleanerDelay: Int,
	nAirgapVolume: Int,
	nAirgapSpeed: Int,
	nRetractSpeed: Int,
	bFastWash: Boolean
) extends T0_Token("wash") {
	override def toString = {
		val fmt = new java.text.DecimalFormat("#.##")
		Array(
			mTips,
			iWasteGrid, iWasteSite,
			iCleanerGrid, iCleanerSite,
			'"'+fmt.format(nWasteVolume)+'"',
			nWasteDelay,
			'"'+fmt.format(nCleanerVolume)+'"',
			nCleanerDelay,
			nAirgapVolume,
			nAirgapSpeed,
			nRetractSpeed,
			(if (bFastWash) 1 else 0),
			0,1000,0
		).mkString("Wash(", ",", ")")
	}
}
*/

case class L2C_SetTipStateClean(tips: Seq[TipConfigL2], degree: CleanDegree.Value) extends Command
