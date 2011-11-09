package roboliq.robots.evoware

import roboliq.common._
import roboliq.commands.move.LidHandling


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
		val l = List(
			mTips,
			'"'+sLiquidClass+'"',
			asVolumes.mkString(","),
			iGrid, iSite,
			1,
			'"'+sPlateMask+'"',
			0
		) ++ (if (RoboeaseHack.bEmulateEvolab) Seq() else Seq(0))
		l.mkString(sFunc+"(", ",", ");")
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
		val fmtWaste = new java.text.DecimalFormat("#.##")
		val fmtCleaner = if (RoboeaseHack.bEmulateEvolab) new java.text.DecimalFormat("#.0") else fmtWaste
		val l = Seq(
			mTips,
			iWasteGrid, iWasteSite,
			iCleanerGrid, iCleanerSite,
			'"'+fmtWaste.format(nWasteVolume)+'"',
			nWasteDelay,
			'"'+fmtCleaner.format(nCleanerVolume)+'"',
			nCleanerDelay,
			nAirgapVolume,
			nAirgapSpeed,
			nRetractSpeed,
			(if (bFastWash) 1 else 0),
			(if (bUNKNOWN1) 1 else 0),
			1000
		) ++ (if (RoboeaseHack.bEmulateEvolab) Seq() else Seq(0))
		l.mkString("Wash(", ",", ");")
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

case class L0C_Transfer_Rack(
	iRoma: Int, // 0 for RoMa1, 1 for RoMa2
	sPlateModel: String,
	iGridSrc: Int, iSiteSrc: Int, sCarrierModelSrc: String,
	iGridDest: Int, iSiteDest: Int, sCarrierModelDest: String,
	lidHandling: LidHandling.Value,
	iGridLid: Int, iSiteLid: Int, sCarrierModelLid: String
) extends Command {
	override def toString = {
		import LidHandling._
		
		val bMoveBackToHome = true // 1 = move back to home position
		List(
			'"'+iGridSrc.toString+'"',
			'"'+iGridDest.toString+'"',
			if (bMoveBackToHome) 1 else 0,
			if (lidHandling == NoLid) 0 else 1,
			0, // speed: 0 = maximum, 1 = taught in vector dialog
			iRoma,
			if (lidHandling == RemoveAtSource) 1 else 0,
			'"'+(if (lidHandling == NoLid) "" else iGridLid.toString)+'"',
			'"'+sPlateModel+'"',
			"\"Narrow\"", // Vector: Narrow, Wide, User Defined...
			"\"\"",
			"\"\"",
			'"'+sCarrierModelSrc+'"',
			'"'+sCarrierModelLid+'"',
			'"'+sCarrierModelDest+'"',
			'"'+(iSiteSrc+1).toString+'"',
			'"'+(if (lidHandling == NoLid) "(Not defined)" else (iSiteLid+1).toString)+'"',
			'"'+(iSiteDest+1).toString+'"'
		).mkString("Transfer_Rack(", ",", ");")
	}
}

case class L0C_StartTimer(
	id: Int
) extends Command {
	override def toString = {
		List(
			'"'+id.toString+'"'
		).mkString("StartTimer(", ",", ");")
	}
}

case class L0C_WaitTimer(
	id: Int,
	nSeconds: Int
) extends Command {
	override def toString = {
		List(
			'"'+id.toString+'"',
			'"'+nSeconds.toString+'"'
		).mkString("WaitTimer(", ",", ");")
	}
}

case class L0C_Facts(
	sDevice: String,
	sVariable: String,
	sValue: String
) extends Command {
	override def toString = {
		List(
			'"'+sDevice+'"',
			'"'+sVariable+'"',
			'"'+sValue+'"',
			"\"0\"",
			"\"\""
		).mkString("FACTS(", ",", ");")
	}
}

case class L0C_Subroutine(
	sFilename: String
) extends Command {
	override def toString = {
		List(
			'"'+sFilename+'"',
			0
		).mkString("Subroutine(", ",", ");")
	}
}
