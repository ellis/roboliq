package evoware

import roboliq.common._


case class L0C_Spirate(
	val sFunc: String,
	val mTips: Int,
	val sLiquidClass: String,
	val asVolumes: Seq[String],
	val iGrid: Int,
	val iSite: Int,
	val sPlateMask: String
) extends Command {
	override def toString = {
		Array(
			mTips,
			'"'+sLiquidClass+'"',
			asVolumes.mkString(","),
			iGrid, iSite,
			1,
			'"'+sPlateMask+'"',
			0, 0
		).mkString(sFunc+"(", ",", ");")
	}
}

case class L0C_Wash(
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
	bFastWash: Boolean,
	bUNKNOWN1: Boolean
) extends Command {
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
			(if (bUNKNOWN1) 1 else 0),
			1000,0
		).mkString("Wash(", ",", ");")
	}
}

case class L0C_Mix(
	val mTips: Int,
	val sLiquidClass: String,
	val asVolumes: Seq[String],
	val iGrid: Int,
	val iSite: Int,
	val sPlateMask: String,
	val nCount: Int
) extends Command {
	override def toString = {
		Array(
			mTips,
			'"'+sLiquidClass+'"',
			asVolumes.mkString(","),
			iGrid, iSite,
			1, // TODO: Spacing
			'"'+sPlateMask+'"',
			nCount, 0
		).mkString("Mix(", ",", ");")
	}
}

case class L0C_GetDITI2(
	val mTips: Int,
	val sType: String
) extends Command {
	override def toString = {
		Array(
			mTips,
			'"'+sType+'"',
			1, 0, 0, 0
		).mkString("GetDITI2(", ",", ");")
	}
	
}

case class L0C_DropDITI(
	val mTips: Int,
	val iGrid: Int,
	val iSite: Int
) extends Command {
	override def toString = {
		Array(
			mTips,
			iGrid,
			iSite,
			10, 70
		).mkString("DropDITI(", ",", ");")
	}	
}
