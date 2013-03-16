package roboliq.robots.evoware

import scala.collection.mutable
import grizzled.slf4j.Logger
import spray.json._
import roboliq.core._
import roboliq.entity._
import roboliq.processor.ConversionsDirect


case class EvowareEntityData private (
	val plateModel_l: List[PlateModel],
	val plateLocation_l: List[PlateLocation],
	val plate_l: List[Plate],
	val plateState_l: List[PlateState],
	val pipettePolicy_l: List[PipettePolicy]
) {
	def toJson: RqResult[JsObject] = {
		for {
			pipettePolicyJson <- RqResult.toResultOfList(pipettePolicy_l.map(ConversionsDirect.toJson[PipettePolicy]))
			plateModelJson <- RqResult.toResultOfList(plateModel_l.map(ConversionsDirect.toJson[PlateModel]))
			plateLocationJson <- RqResult.toResultOfList(plateLocation_l.map(ConversionsDirect.toJson[PlateLocation]))
			plateJson <- RqResult.toResultOfList(plate_l.map(ConversionsDirect.toJson[Plate]))
			plateStateJson <- RqResult.toResultOfList(plateState_l.map(ConversionsDirect.toJson[PlateState]))
		} yield {
			JsObject(
				"pipettePolicy" -> JsArray(pipettePolicyJson),
				"plateModel" -> JsArray(plateModelJson),
				"plateLocation" -> JsArray(plateLocationJson),
				"plate" -> JsArray(plateJson),
				"plateState" -> JsArray(plateStateJson)
			)
		}
	}

	def toJsonString: RqResult[String] = {
		for {
			json <- toJson
		} yield {
			json.prettyPrint
		}
	}
}

object EvowareEntityData {
	private val logger = Logger("roboliq.robots.evoware.EvowareEntityData")
	
	def createEntities(
		carrier: EvowareCarrierData,
		table: EvowareTableData,
		config: EvowareConfigData,
		pipettePolicy_l: List[PipettePolicy]
	): RqResult[EvowareEntityData] = {
		val warning_l = new mutable.ArrayBuffer[String]
		
		val gridToCarrier_m = table.mapCarrierToGrid.map(pair => pair._2 -> pair._1)
		
		// Get or create an ID for each site on the table
		val gridSiteToSiteId_m: Map[(Int, Int), (CarrierSite, String)] =
			table.mapCarrierToGrid.toList.flatMap(pair => {
				val (carrier, grid_i) = pair
				(0 until carrier.nSites).map(site_i => {
					val gridSite = (grid_i, site_i)
					val id0 = f"G${grid_i}%03dS${site_i+1}"
					val id = config.siteIds.getOrElse(id0, id0)
					gridSite -> (CarrierSite(carrier, site_i), id)
				})
			}).toMap

		val gridSite_l = gridSiteToSiteId_m.keys.toList
		
		//val siteToId_m = table.mapLabelToSite.toList.map(pair => pair._2 -> pair._1).toMap
		
		val plateModel_m = carrier.mapNameToLabwareModel.map(pair => {
			val (id, labware) = pair
			id -> PlateModel(id, labware.nRows, labware.nCols, LiquidVolume.ul(labware.ul))
		})
		
		val plateModelAll_l = new mutable.HashSet[PlateModel]
		val plateLocation_m = gridSiteToSiteId_m.map(pair => {
			val (_, (site, id)) = pair
			val sitepair = (site.carrier.id, site.iSite)
			val labware_l = carrier.mapNameToLabwareModel.values.filter(labware => labware.sites.contains(sitepair)).toList
			val plateModel_l = labware_l.map(labware => plateModel_m(labware.sName))
			plateModelAll_l ++= plateModel_l
			id -> PlateLocation(id, plateModel_l, false)
		})
		
		val plateAndState_l = gridSiteToSiteId_m.toList.flatMap(pair => {
			val (_, (site, id)) = pair
			for {
				labware <- table.mapSiteToLabwareModel.get(site)
				plateModel <- plateModel_m.get(labware.sName)
				plateLocation <- plateLocation_m.get(id)
			} yield {
				val plate = Plate(id, plateModel, Some(id))
				val plateState = PlateState(plate, Some(plateLocation))
				(plate, plateState)
			}
		})
		
		val entities = EvowareEntityData(
			plateModelAll_l.toList.sortBy(_.id),
			plateLocation_m.values.toList.sortBy(_.id),
			plateAndState_l.map(_._1).sortBy(_.id),
			plateAndState_l.map(_._2).sortBy(_.id),
			pipettePolicy_l
		)
		
		RqSuccess(entities, warning_l.toList)
	}

	// Can call from sbt console with e.g.:
	// EvowareEntityData.createEntities("testdata/bsse-robot1/config/carrier.cfg", "testdata/bsse-robot1/config/table-01.esc", "testdata/bsse-robot1/config/table-01.yaml", "testdata/bsse-robot1/config/DefaultLCs.XML", "testdata/bsse-robot1/config/CustomLCs.XML")
	def createEntities(
		carrierFilename: String,
		tableFilename: String,
		configFilename: String,
		defaultLcsFilename: String,
		customLcsFilename: String
	): RqResult[EvowareEntityData] = {
		for {
			carrier <- EvowareCarrierData.loadFile(carrierFilename)
			table <- EvowareTableData.loadFile(carrier, tableFilename)
			config <- EvowareConfigData.loadFile(configFilename)
			defaultLcs <- EvowareLiquidClassParser.parseFile(defaultLcsFilename)
			customLcs <- EvowareLiquidClassParser.parseFile(customLcsFilename)
			ret <- createEntities(carrier, table, config, defaultLcs ++ customLcs)
		} yield ret
	}
}
