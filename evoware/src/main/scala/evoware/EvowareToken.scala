package evoware

sealed abstract class T0_Token

case class T0_Error(val sMessage: String) extends T0_Token

case class T0_Spirate(
	val sFunc: String,
	val mTips: Int,
	val sLiquidClass: String,
	val asVolumes: Seq[String],
	val iGrid: Int,
	val iSite: Int,
	val sPlateMask: String
) extends T0_Token {
	override def toString = {
		Array(
			mTips,
			'"'+sLiquidClass+'"',
			asVolumes.mkString(","),
			iGrid, iSite,
			1,
			'"'+sPlateMask+'"',
			0, 0
		).mkString(sFunc+"(", ",", ")")
	}
}

case class T0_Wash(
	mTips: Int,
	iWasteGrid: Int, iWasteSite: Int,
	iCleanerGrid: Int, iCleanerSite: Int,
	sWasteVolume: String, // TODO: Change this to Double, but format it nicely below -- ellis, 2011-06-11
	nWasteDelay: Int,
	sCleanerVolume: String, // TODO: Change this to Double, but format it nicely below -- ellis, 2011-06-11
	nCleanerDelay: Int,
	nAirgapVolume: Int,
	nAirgapSpeed: Int,
	nRetractSpeed: Int,
	bFastWash: Boolean
) extends T0_Token {
	override def toString = {
		Array(
			mTips,
			iWasteGrid, iWasteSite,
			iCleanerGrid, iCleanerSite,
			'"'+sWasteVolume+'"',
			nWasteDelay,
			'"'+sCleanerVolume+'"',
			nCleanerDelay,
			nAirgapVolume,
			nAirgapSpeed,
			nRetractSpeed,
			(if (bFastWash) 1 else 0),
			0,1000,0
		).mkString("Wash(", ",", ")")
	}
}
