package roboliq.evoware.parser

import ch.ethz.reactivesim.RsResult
import ch.ethz.reactivesim.RsError
import ch.ethz.reactivesim.RsSuccess
import java.io.File
import org.apache.commons.io.FileUtils
import scala.collection.immutable



/**
 * Represents the table setup for an Evoware script file.
 * @param carrierIdInternal_l List with optional CarrierID for each grid on the table
 */
class EvowareTableData(
	val configFile: EvowareCarrierData,
	val carrierIdInternal_l: List[Option[Int]],
	//lCarrier_? : List[Option[Carrier]],
	val hotelObject_l: List[HotelObject],
	val externalObject_l: List[ExternalObject],
	val carrierIdToGrids_m: Map[Int, List[Int]],
	//val mapCarrierToGrid: Map[Carrier, Int],
	val siteIdToLabel_m: Map[CarrierGridSiteIndex, String],
	val siteIdToLabwareModel_m: Map[CarrierGridSiteIndex, EvowareLabwareModel]
) {
	/*def print() {
		hotelObject_l.foreach(println)
		externalObject_l.foreach(println)
		siteIdToLabel_m.foreach(println)
		siteIdToLabwareModel_m.foreach(println)
		mapCarrierToGrid.toList.sortBy(_._2).foreach(println)
	}*/
	
	def toDebugString(): String = {
		List(
			hotelObject_l,
			externalObject_l,
			siteIdToLabel_m.toList,
			siteIdToLabwareModel_m.toList,
			carrierIdToGrids_m.toList.sortBy(_._2.min).map(pair => configFile.mapIdToCarrier.get(pair._1) -> pair._2)
		).flatten.mkString("\n")
	}
	
	def toStringWithLabware(
		siteIdToLabel_m2: Map[CarrierGridSiteIndex, String],
		siteIdToLabwareModel_m2: Map[CarrierGridSiteIndex, EvowareLabwareModel]
	): String = {
		val siteIdToLabel_m3 = siteIdToLabel_m ++ siteIdToLabel_m2
		val siteIdToLabwareModel_m3 = siteIdToLabwareModel_m ++ siteIdToLabwareModel_m2
		//println("siteIdToLabwareModel_m:")
		//siteIdToLabwareModel_m3.foreach(println)
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
			toString_tableLabware(siteIdToLabel_m3, siteIdToLabwareModel_m3) ++
			toString_hotels() ++
			toString_externals() ++
			toString_externalLabware(siteIdToLabwareModel_m3) ++
			toString_externalGrids() ++
			List("996;0;0;", "--{ RPG }--")
		l.mkString("\n")
	}
	
	def toStringWithLabware(
		siteToNameAndLabel_m: Map[CarrierNameGridSiteIndex, (String, String)]
	): String = {
		val siteIdToLabel_m2 = siteToNameAndLabel_m.map { case (cngsi, (label, _)) =>
			val carrierId = configFile.mapNameToCarrier(cngsi.carrierName).id
			val siteId = CarrierGridSiteIndex(carrierId, cngsi.gridIndex, cngsi.siteIndex)
			siteId -> label
		}
		val siteIdToLabwareModel_m2 = siteToNameAndLabel_m.map { case (cngsi, (_, labwareModelName)) =>
			val carrierId = configFile.mapNameToCarrier(cngsi.carrierName).id
			val siteId = CarrierGridSiteIndex(carrierId, cngsi.gridIndex, cngsi.siteIndex)
			val labwareModel = configFile.mapNameToLabwareModel(labwareModelName)
			siteId -> labwareModel
		}
		val siteIdToLabel_m3 = siteIdToLabel_m ++ siteIdToLabel_m2
		val siteIdToLabwareModel_m3 = siteIdToLabwareModel_m ++ siteIdToLabwareModel_m2
		//println("siteIdToLabwareModel_m:")
		//siteIdToLabwareModel_m3.foreach(println)
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
			toString_tableLabware(siteIdToLabel_m3, siteIdToLabwareModel_m3) ++
			toString_hotels() ++
			toString_externals() ++
			toString_externalLabware(siteIdToLabwareModel_m3) ++
			toString_externalGrids() ++
			List("996;0;0;", "--{ RPG }--")
		l.mkString("\n")
	}
	
	private def toString_carriers(): List[String] = {
		List("14;"+carrierIdInternal_l.map({ case None => "-1"; case Some(id) => id.toString }).mkString(";")+";")
	}
	
	private def toString_tableLabware(
		siteIdToLabel_m2: Map[CarrierGridSiteIndex, String],
		siteIdToLabwareModel_m2: Map[CarrierGridSiteIndex, EvowareLabwareModel]
	): List[String] = {
		carrierIdInternal_l.zipWithIndex.flatMap {
			case (None, _) => List("998;0;")
			case (Some(carrierId), gridIndex) =>
				val carrier = configFile.mapIdToCarrier(carrierId)
				//val sSiteCount = if (carrier.nSites > 0) carrier.nSites.toString else ""
				List(
					"998;"+carrier.nSites+";"+((0 until carrier.nSites).map(siteIndex => {
						val siteId = CarrierGridSiteIndex(carrierId, gridIndex, siteIndex)
						siteIdToLabwareModel_m2.get(siteId) match {
							case None => ""
							case Some(labwareModel) => labwareModel.sName
						}
					}).mkString(";"))+";",
					"998;"+((0 until carrier.nSites).map(siteIndex => {
						val siteId = CarrierGridSiteIndex(carrierId, gridIndex, siteIndex)
						siteIdToLabel_m2.get(siteId) match {
							case None => ""
							case Some(sLabel) => sLabel
						}
					}).mkString(";"))+";"
				)
		}
	}
	
	private def toString_hotels(): List[String] = {
		("998;"+hotelObject_l.length+";") :: hotelObject_l.map(o => "998;"+o.parent.id+";"+o.n+";")
	}
	
	private def toString_externals(): List[String] = {
		("998;"+externalObject_l.length+";") :: externalObject_l.map(o => "998;"+o.n1+";"+o.n2+";"+o.carrier.sName+";")
	}
	
	private def toString_externalLabware(
		siteIdToLabwareModel_m2: Map[CarrierGridSiteIndex, EvowareLabwareModel]
	): List[String] = {
		// List of external carriers
		val carrierId_l: Set[Int] = externalObject_l.map(_.carrier.id).toSet
		val siteIdToLabwareModel_m3 = siteIdToLabwareModel_m2.filterKeys(siteId => carrierId_l.contains(siteId.carrierId))
		// Map of external carrier to labware model
		val carrierIdToLabwareModel_m: Map[Int, EvowareLabwareModel]
			= siteIdToLabwareModel_m3.toList.groupBy(_._1.carrierId).mapValues(l => l.head._2)
		// List of external carriers with labware on them
		val lCarrierToLabware = carrierIdToLabwareModel_m.toList.map { case (carrierId, labwareModelE) =>
			s"998;${carrierId};${labwareModelE.sName};"
		}
		("998;"+carrierIdToLabwareModel_m.size+";") :: lCarrierToLabware
	}
	
	private def toString_externalGrids(): List[String] = {
		externalObject_l.map(o => "998;"+carrierIdToGrids_m(o.carrier.id).head+";")
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