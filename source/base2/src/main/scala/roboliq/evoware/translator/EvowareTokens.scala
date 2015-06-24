package roboliq.evoware.translator

import roboliq.evoware.parser.EvowareLabwareModel
import roboliq.evoware.parser.CarrierNameGridSiteIndex

case class Token(
	line: String,
	siteToNameAndModel_m: Map[CarrierNameGridSiteIndex, (String, String)] = Map()
) {
	
}
/*
case class Token_Transfer_Rack(
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
) extends Token {
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
*/