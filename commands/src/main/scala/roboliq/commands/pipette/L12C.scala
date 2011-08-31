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
	
	override def toDebugString = {
		val (tip0, tip1) = (items.head.tip, items.last.tip)
		val policies = items.groupBy(_.policy.sName)
		if (policies.size == 1) {
			val wells = items.map(_.well)
			val sTips = TipSet.toDebugString(items.map(_.tip))
			val sVolumes = super.getSeqDebugString(items.map(_.nVolume))
			getClass().getSimpleName() + "("+sTips+", "+sVolumes+", "+policies.keys.head+", "+getWellsDebugString(wells)+")" 
		}
		else {
			toString
		}
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
	
	override def toDebugString = {
		val (tip0, tip1) = (items.head.tip, items.last.tip)
		val bTipsContiguous = ((tip1.index - tip0.index + 1) == items.size)
		val volumes = items.groupBy(_.nVolume)
		val policies = items.groupBy(_.policy.sName)
		if (bTipsContiguous && volumes.size == 1 && policies.size == 1) {
			val wells = items.map(_.well)
			val sTips = TipSet.toDebugString(items.map(_.tip))
			getClass().getSimpleName() + "("+sTips+", "+volumes.keys.head+", "+policies.keys.head+", "+getWellsDebugString(wells)+")" 
		}
		else {
			toString
		}
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

case class L2C_Wash(items: Seq[L2A_WashItem], iWashProgram: Int, intensity: WashIntensity.Value) extends CommandL2 {
	type L1Type = L1C_Wash
	
	def updateState(builder: StateBuilder) {
		for (item <- items) {
			item.tip.obj.stateWriter(builder).clean(intensity)
		}
	}
	
	def toL1(states: RobotState): Either[Seq[String], L1Type] = {
		val items1 = items.map(item => new L1A_WashItem(item.tip, item.nVolumeInside))
		Right(L1C_Wash(items1, iWashProgram))
	}
}

case class L1C_Wash(items: Seq[L1A_WashItem], iWashProgram: Int) extends CommandL1

class L2A_WashItem(
	val tip: TipConfigL2,
	val nVolumeInside: Double
)

sealed class L1A_WashItem(
	val tip: TipConfigL2,
	val nVolumeInside: Double
)


/*sealed class L1A_WashArgs(
	val tips: Set[TipConfigL2],
	val degree: CleanDegree.Value,
	val iWashProgram: Int)*/

//-------------------------------
// TipsGet
//-------------------------------

case class L2C_TipsGet(tips: Set[TipConfigL2], sType: String) extends CommandL2 {
	type L1Type = L1C_TipsGet
	
	def updateState(builder: StateBuilder) {
		for (tip <- tips) {
			tip.obj.stateWriter(builder).get(sType)
		}
		/*val tip0 = tips.head
		println("after1: "+builder.map(tip0.obj))
		println("after2: "+builder.map.toMap.apply(tip0.obj))
		println("after3: "+builder.toImmutable.map(tip0.obj))
		println("after4: "+new RobotState(builder.map.toMap).apply(tip0.obj))*/
	}
	
	def toL1(states: RobotState): Either[Seq[String], L1Type] = {
		for (tip <- tips) {
			val tipState = tip.obj.state(states)
			tipState.sType_? match {
				case Some(sType) => return Left(Seq("tip "+tip.index+" must be dropped before getting a new one"))
				case _ =>
			}
		}
		Right(L1C_TipsGet(tips, sType))
	}

	override def toDebugString = {
		val sTips = TipSet.toDebugString(tips)
		getClass().getSimpleName() + List(sTips, sType).mkString("(", ", ", ")") 
	}
}

case class L1C_TipsGet(tips: Set[TipConfigL2], sType: String) extends CommandL1

//-------------------------------
// TipsDrop
//-------------------------------

case class L2C_TipsDrop(tips: Set[TipConfigL2], location: String) extends CommandL2 {
	type L1Type = L1C_TipsDrop
	
	def updateState(builder: StateBuilder) {
		for (tip <- tips) {
			tip.obj.stateWriter(builder).drop()
		}
	}
	
	def toL1(states: RobotState): Either[Seq[String], L1Type] = {
		Right(L1C_TipsDrop(tips, location))
	}

	override def toDebugString = {
		val sTips = TipSet.toDebugString(tips)
		getClass().getSimpleName() + List(sTips, location).mkString("(", ", ", ")") 
	}
}

case class L1C_TipsDrop(tips: Set[TipConfigL2], location: String) extends CommandL1

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

case class L2C_SetTipStateClean(tips: Seq[TipConfigL2], degree: WashIntensity.Value) extends Command
