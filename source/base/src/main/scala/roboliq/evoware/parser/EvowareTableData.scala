package roboliq.evoware.parser

import ch.ethz.reactivesim.RsResult
import ch.ethz.reactivesim.RsError
import ch.ethz.reactivesim.RsSuccess
import java.io.File
import org.apache.commons.io.FileUtils



/**
 * Represents the table setup for an Evoware script file
 */
class EvowareTableData(
	val configFile: EvowareCarrierData,
	lCarrier_? : List[Option[Carrier]],
	val lHotelObject: List[HotelObject],
	val lExternalObject: List[ExternalObject],
	val mapCarrierToGrid: Map[Carrier, Int],
	val mapSiteToLabel: Map[CarrierSite, String],
	val mapSiteToLabwareModel: Map[CarrierSite, EvowareLabwareModel]
) {
	/*def print() {
		lHotelObject.foreach(println)
		lExternalObject.foreach(println)
		mapSiteToLabel.foreach(println)
		mapSiteToLabwareModel.foreach(println)
		mapCarrierToGrid.toList.sortBy(_._2).foreach(println)
	}*/
	
	def toDebugString(): String = {
		List(
			lHotelObject,
			lExternalObject,
			mapSiteToLabel.toList,
			mapSiteToLabwareModel.toList,
			mapCarrierToGrid.toList.sortBy(_._2)
		).flatten.mkString("\n")
	}
	
	def toStringWithLabware(
		mapSiteToLabel2: Map[CarrierSite, String],
		mapSiteToLabwareModel2: Map[CarrierSite, EvowareLabwareModel]
	): String = {
		val mapSiteToLabel3 = mapSiteToLabel ++ mapSiteToLabel2
		val mapSiteToLabwareModel3 = mapSiteToLabwareModel ++ mapSiteToLabwareModel2
		//println("mapSiteToLabwareModel:")
		//mapSiteToLabwareModel3.foreach(println)
		// TODO: output current date and time
		// TODO: See whether we need to save the RES section when loading in the table
		// TODO: do we need to save values for the 999 line when loading the table?
		val l = List(
				"00000000",
				"20111117_122139 No log in       ",
				"                                                                                                                                ",
				"No user logged in                                                                                                               ",
				"--{ RES }--",
				"V;200",
				"--{ CFG }--",
				"999;219;32;"
			) ++
			toString_carriers() ++
			toString_tableLabware(mapSiteToLabel3, mapSiteToLabwareModel3) ++
			toString_hotels() ++
			toString_externals() ++
			toString_externalLabware(mapSiteToLabwareModel3) ++
			toString_externalGrids() ++
			List("996;0;0;", "--{ RPG }--")
		l.mkString("\n")
	}
	
	private def toString_carriers(): List[String] = {
		List("14;"+lCarrier_?.map(_ match { case None => "-1"; case Some(o) => o.id.toString }).mkString(";")+";")
	}
	
	private def toString_tableLabware(
		mapSiteToLabel2: Map[CarrierSite, String],
		mapSiteToLabwareModel2: Map[CarrierSite, EvowareLabwareModel]
	): List[String] = {
		def step(lCarrier_? : List[Option[Carrier]], acc: List[String]): List[String] = {
			lCarrier_? match {
				case Nil => acc
				case carrier_? :: rest =>
					val acc2 = carrier_? match {
						case None => List("998;0;")
						case Some(carrier) =>
							//val sSiteCount = if (carrier.nSites > 0) carrier.nSites.toString else ""
							List(
								"998;"+carrier.nSites+";"+((0 until carrier.nSites).map(iSite => {
									mapSiteToLabwareModel2.get(CarrierSite(carrier, iSite)) match {
										case None => ""
										case Some(labwareModel) => labwareModel.sName
									}
								}).mkString(";"))+";",
								"998;"+((0 until carrier.nSites).map(iSite => {
									mapSiteToLabel2.get(CarrierSite(carrier, iSite)) match {
										case None => ""
										case Some(sLabel) => sLabel
									}
								}).mkString(";"))+";"
							)
					}
					step(rest, acc ++ acc2)
			}
		}
		step(lCarrier_?, Nil)
	}
	
	private def toString_hotels(): List[String] = {
		("998;"+lHotelObject.length+";") :: lHotelObject.map(o => "998;"+o.parent.id+";"+o.n+";")
	}
	
	private def toString_externals(): List[String] = {
		("998;"+lExternalObject.length+";") :: lExternalObject.map(o => "998;"+o.n1+";"+o.n2+";"+o.carrier.sName+";")
	}
	
	private def toString_externalLabware(
		mapSiteToLabwareModel2: Map[CarrierSite, EvowareLabwareModel]
	): List[String] = {
		// List of external carriers
		val lCarrier0 = lExternalObject.map(_.carrier)
		// Map of external carrier to labware model
		val map = mapSiteToLabwareModel2.filter(pair => lCarrier0.contains(pair._1.carrier)).map(pair => pair._1.carrier -> pair._2)
		// List of external carriers with labware on them
		val lCarrierToLabware = lCarrier0.flatMap(carrier => map.get(carrier).map(carrier -> _))
		("998;"+lCarrierToLabware.length+";") :: lCarrierToLabware.map(pair => "998;"+pair._1.id+";"+pair._2.sName+";")
	}
	
	private def toString_externalGrids(): List[String] = {
		lExternalObject.map(o => "998;"+mapCarrierToGrid(o.carrier)+";")
	}
}

object EvowareTableData {
	def loadFile(carrierData: EvowareCarrierData, filename: String): RsResult[EvowareTableData] = {
		//try {
			val tableDataFile = new File(filename)
			if (tableDataFile.exists()) {
				val tableData = EvowareTableParser.parseFile(carrierData, filename)
				RsSuccess(tableData)
			}
			else {
				RsError(s"Evoware table file not found: $filename")
			}
		/*}
		catch {
			case ex: Throwable => RsError(ex.getMessage)
		}*/
	}
}