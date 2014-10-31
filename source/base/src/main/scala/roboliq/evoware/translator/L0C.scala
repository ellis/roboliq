package roboliq.evoware.translator

import roboliq.core._
import roboliq.evoware.parser._
import java.io.File


abstract class L0C_Command {
	/**
	 * List of sites which need to be assigned to particular labware in order
	 * for this command to work in Evoware.
	 */
	def getSiteToLabwareModelList: List[Tuple2[CarrierSite, EvowareLabwareModel]] = Nil
	
	//def toEvowareString(file: File): String = toString
}

case class L0C_BeginLoop(
	count: Int,
	variable: String
) extends L0C_Command {
	override def toString = {
		List(
			'"'+count+'"',
			'"'+variable+'"'
		).mkString("BeginLoop(", ",", ");")
	}
}

case class L0C_Comment(
	s: String
) extends L0C_Command {
	override def toString = {
		List(
			'"'+s+'"'
		).mkString("Comment(", ",", ");")
	}
}

case class L0C_DetectLevel(
	val mTips: Int,
	val sLiquidClass: String,
	val iGrid: Int,
	val iSite: Int,
	val sPlateMask: String,
	val site: CarrierSite,
	val labwareModel: EvowareLabwareModel
) extends L0C_Command {
	override def getSiteToLabwareModelList: List[Tuple2[CarrierSite, EvowareLabwareModel]] =
		List(site -> labwareModel)

	override def toString = {
		val l = List(
			mTips,
			'"'+sLiquidClass+'"',
			iGrid, iSite,
			1,
			'"'+sPlateMask+'"',
			0,
			0
		)
		l.mkString("Detect_Liquid(", ",", ");")
	}
}

case class L0C_DropDITI(
	val mTips: Int,
	val iGrid: Int,
	val iSite: Int
) extends L0C_Command {
	override def toString = {
		Array(
			mTips,
			iGrid,
			iSite,
			10, 70
		).mkString("DropDITI(", ",", ");")
	}
}

case class L0C_EndLoop(
) extends L0C_Command {
	override def toString = {
		List(
		).mkString("EndLoop(", ",", ");")
	}
}

case class L0C_Execute(
	val cmd: String,
	val nWaitOpt: Int,
	val sResultVar: String
) extends L0C_Command {
	override def toString = {
		Array(
			'"'+cmd+'"',
			nWaitOpt,
			'"'+sResultVar+'"',
			2
		).mkString("Execute(", ",", ");")
	}
}

case class L0C_GetDITI2(
	val mTips: Int,
	val sType: String
) extends L0C_Command {
	override def toString = {
		Array(
			mTips,
			'"'+sType+'"',
			1, 0, 0, 0
		).mkString("GetDITI2(", ",", ");")
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
) extends L0C_Command {
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

/**
 * @param tipSpacing 1 if the tips go to adjacent wells, 2 if they are separated by 1 well, and so on -- 0 is probably not valid.
 */
case class L0C_Spirate(
	val sFunc: String,
	val mTips: Int,
	val sLiquidClass: String,
	val asVolumes: Seq[String],
	val iGrid: Int,
	val iSite: Int,
	val tipSpacing: Int,
	val sPlateMask: String,
	val site: CarrierSite,
	val labwareModel: EvowareLabwareModel
) extends L0C_Command {
	override def getSiteToLabwareModelList: List[Tuple2[CarrierSite, EvowareLabwareModel]] =
		List(site -> labwareModel)

	override def toString = {
		val l = List(
			mTips,
			'"'+sLiquidClass+'"',
			asVolumes.mkString(","),
			iGrid, iSite,
			tipSpacing,
			'"'+sPlateMask+'"',
			0,
			0
		)
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
) extends L0C_Command {
	override def toString = {
		val fmtWaste = new java.text.DecimalFormat("#.##")
		val fmtCleaner = fmtWaste
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
			1000,
			0
		)
		l.mkString("Wash(", ",", ");")
	}
}

case class L0C_Transfer_Rack(
	iRoma: Int, // 0 for RoMa1, 1 for RoMa2
	sVectorClass: String, // Narrow, Wide, User Defined...
	//sPlateModel: String,
	//iGridSrc: Int, iSiteSrc: Int, sCarrierModelSrc: String,
	//iGridDest: Int, iSiteDest: Int, sCarrierModelDest: String,
	labwareModel: EvowareLabwareModel,
	iGridSrc: Int, siteSrc: CarrierSite,
	iGridDest: Int, siteDest: CarrierSite,
	lidHandling: LidHandling.Value,
	iGridLid: Int, iSiteLid: Int, sCarrierLid: String
) extends L0C_Command {
	override def getSiteToLabwareModelList: List[Tuple2[CarrierSite, EvowareLabwareModel]] =
		List(siteSrc -> labwareModel, siteDest -> labwareModel)
	
	override def toString = {
		import LidHandling._
		
		val sCarrierSrc = siteSrc.carrier.sName
		val sCarrierDest = siteDest.carrier.sName
		val iSiteSrc = siteSrc.iSite
		val iSiteDest = siteDest.iSite
		
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
			//'"'+sPlateModel+'"',
			'"'+labwareModel.sName+'"',
			'"'+sVectorClass+'"',
			"\"\"",
			"\"\"",
			'"'+sCarrierSrc+'"',
			'"'+sCarrierLid+'"',
			'"'+sCarrierDest+'"',
			'"'+(iSiteSrc+1).toString+'"',
			'"'+(if (lidHandling == NoLid) "(Not defined)" else (iSiteLid+1).toString)+'"',
			'"'+(iSiteDest+1).toString+'"'
		).mkString("Transfer_Rack(", ",", ");")
	}
}

case class L0C_StartTimer(
	id: Int
) extends L0C_Command {
	override def toString = {
		List(
			'"'+id.toString+'"'
		).mkString("StartTimer(", ",", ");")
	}
}

case class L0C_WaitTimer(
	id: Int,
	nSeconds: Int
) extends L0C_Command {
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
) extends L0C_Command {
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
) extends L0C_Command {
	override def toString = {
		List(
			'"'+sFilename+'"',
			0
		).mkString("Subroutine(", ",", ");")
	}
}

/*
case class L0C_SubroutineIndexed(
	sFilename: String
) extends L0C_Command {
	override def toEvowareString(file: File): String = {
		List(
			'"'+(new File(file.getParentFile, sFilename).getPath)+'"',
			0
		).mkString("Subroutine(", ",", ");")
	}
	
	override def toString = {
		List(
			'"'+sFilename+'"',
			0
		).mkString("Subroutine(", ",", ");")
	}
}
*/

case class L0C_Prompt(
	s: String
) extends L0C_Command {
	override def toString = {
		List(
			'"'+s+'"',
			0,
			-1
		).mkString("UserPrompt(", ",", ");")
	}
}
