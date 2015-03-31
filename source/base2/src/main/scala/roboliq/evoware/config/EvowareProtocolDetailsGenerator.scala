package roboliq.evoware.config

import spray.json._
import spray.json.DefaultJsonProtocol._
import grizzled.slf4j.Logger
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import roboliq.core._
import roboliq.entities._
import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.MultiMap
import roboliq.utils.FileUtils
import org.apache.commons.io.FilenameUtils
import java.io.File
import roboliq.evoware.translator.EvowareConfigData
import roboliq.evoware.translator.EvowareConfig
import scalax.collection.Graph
import scalax.collection.edge.LHyperEdge
import scalax.collection.edge.LkUnDiEdge
import roboliq.ai.strips
import roboliq.ai.plan.Unique
import roboliq.ai.plan.PartialPlan
import com.google.gson.Gson
import roboliq.input.EvowareAgentBean
import roboliq.input.ProtocolData
import roboliq.input.ProtocolDataBuilder
import roboliq.evoware.parser.EvowareCarrierData
import roboliq.evoware.parser.EvowareLabwareModel
import roboliq.evoware.parser.EvowareTableData
import roboliq.input.RjsString
import roboliq.input.RjsNumber
import roboliq.input.RjsBasicMap
import roboliq.input.RjsAbstractMap
import roboliq.evoware.parser.CarrierGridSiteIndex
import roboliq.evoware.parser.CarrierSiteIndex

/**
 * @param gridIndex 1-based index of grid
 * @param siteIndex 0-based index of site
 */
case class SiteId(gridIndex: Int, siteIndex: Int)

object EvowareProtocolDetailsGenerator {
	def loadCarrierData(
		agentConfig: EvowareAgentConfig
	): ResultC[EvowareCarrierData] = {
		ResultC.from(EvowareCarrierData.loadFile(new File(agentConfig.evowareDir, "Carrier.cfg").getPath))
	}
	
	def loadTableSetupConfig(
		agentConfig: EvowareAgentConfig,
		table_l: List[String]
	): ResultC[EvowareTableSetupConfig] = {
		val agentIdent = agentConfig.name
		val tableNameDefault = s"${agentIdent}.default"
		val tableSetup_m = agentConfig.tableSetups.toMap.map(pair => s"${agentIdent}.${pair._1}" -> pair._2)
		for {
			// Load carrier file
			carrierData <- ResultC.from(EvowareCarrierData.loadFile(new File(agentConfig.evowareDir, "Carrier.cfg").getPath))
			// FIXME: for debug only
			//_ = carrierData.printCarriersById
			// ENDIF
			// Choose a table
			tableName <- table_l.filter(tableSetup_m.contains) match {
				case Nil =>
					if (tableSetup_m.contains(tableNameDefault))
						ResultC.unit(tableNameDefault)
					else
						ResultC.error(s"No table specified for agent `$agentIdent`, and agent doesn't have a default table")
				case s :: Nil => ResultC.unit(s)
				case l => ResultC.error(s"Agent `$agentIdent` can only be assigned one table, but multiple tables were specified: ${l.mkString(",")}")
			}
			tableSetupConfig = tableSetup_m(tableName)
		} yield tableSetupConfig
	}
	
	def loadTableData(
		carrierData: EvowareCarrierData,
		tableSetupConfig: EvowareTableSetupConfig,
		searchPath_l: List[File]
	): ResultC[EvowareTableData] = {
		for {
			filename <- ResultC.from(FileUtils.findFile(tableSetupConfig.tableFile, searchPath_l))
			tableData <- ResultC.from(roboliq.evoware.parser.EvowareTableData.loadFile(carrierData, tableSetupConfig.tableFile))
		} yield tableData
	}
	
	def createEvowareProtocolData(
		agentConfig: EvowareAgentConfig,
		carrierData: EvowareCarrierData,
		tableSetupConfig: EvowareTableSetupConfig
	): EvowareProtocolData = {
		// Merge evowareProtocolData of agent and table
		val evowareProtocolDataA = agentConfig.evowareProtocolData_?.getOrElse(EvowareProtocolData.empty)
		val evowareProtocolDataB = tableSetupConfig.evowareProtocolData_?.getOrElse(EvowareProtocolData.empty)
		evowareProtocolDataA merge evowareProtocolDataB
	}
	
	def createProtocolDetails(
		agentConfig: EvowareAgentConfig,
		table_l: List[String],
		searchPath_l: List[File]
	): ResultC[ProtocolData] = {
		val agentIdent = agentConfig.name
		for {
			// Load carrier file
			carrierData <- loadCarrierData(agentConfig)
			tableSetupConfig <- loadTableSetupConfig(agentConfig, table_l)
			tableData <- loadTableData(carrierData, tableSetupConfig, searchPath_l)
			evowareProtocolData = createEvowareProtocolData(agentConfig, carrierData, tableSetupConfig)
			// Merge protocolData of agent and table
			detailsA = agentConfig.protocolDetails_?.getOrElse(new ProtocolData())
			detailsB = tableSetupConfig.protocolDetails_?.getOrElse(new ProtocolData())
			details0 <- detailsA merge detailsB
			// Convert evowareProtocolData0 to ProtocolData
			details1 <- convertEvowareProtocolData(agentIdent, details0, evowareProtocolData, carrierData, tableData)
			//_ = println("protocolData1:\n"+protocolData1)
			// Merge protocolDatas
			details2 <- details0 merge details1
			//_ = println("protocolData2:\n"+protocolData2)
		} yield details2
	}
	
	private def convertEvowareProtocolData(
		agentName: String,
		data0: ProtocolData,
		evowareProtocolData: EvowareProtocolData,
		carrierData: EvowareCarrierData,
		tableData: EvowareTableData
	): ResultC[ProtocolData] = {
		//sites: Map[String, EvowareSiteConfig],
		//devices: Map[String, EvowareDeviceConfig],
		//transporterBlacklist: List[EvowareTransporterBlacklistConfig]

		//carrierData.printCarriersById()
		//println("carrierData.mapNameToLabwareModel:")
		//carrierData.mapNameToLabwareModel.foreach(println)
		//println()
		
		//println("tableData:")
		//println(tableData.toDebugString)
		//println()

		// Gather list of all available labware models referenced in data0
		//println("data0.objects.map.toList: "+data0.objects.map.toList)
		val modelNameToEvowareName_l: List[(String, String)] = data0.objects.map.toList.collect({ case (name, m: RjsAbstractMap) if m.getValue("type") == Some(RjsString("PlateModel")) && m.getValueMap.contains("evowareName") => name -> m.getValueMap("evowareName").toText })
		//println("modelNameToEvowareName_l: "+modelNameToEvowareName_l)
		
		val builder = new ProtocolDataBuilder
		//val siteIdToSiteName_m = new HashMap[(Int, Int), String]
		for {
			// Find the evoware labware models
			modelNameToEvowareModel_l <- ResultC.mapAll(modelNameToEvowareName_l) { case (modelName, evowareName) =>
				ResultC.context(s"protocolData.objects.$modelName") {
					for {
						mE <- ResultC.from(carrierData.mapNameToLabwareModel.get(evowareName), s"unknown Evoware labware model: $evowareName")
					} yield modelName -> mE
				}
			}
			//_ = println("modelNameToEvowareModel_l: "+modelNameToEvowareModel_l)
			//_ = println()
			//_ = println("tableData.carrierIdToGrids_m: "+tableData.carrierIdToGrids_m)
			//_ = println()
			
			// Create map from siteId to all labware model names that can be placed on that site
			siteIdToModelNames_m: Map[CarrierGridSiteIndex, Set[String]] = {
				val l = for {
					(modelName, mE) <- modelNameToEvowareModel_l
					siteId <- mE.sites
					grid_i <- tableData.carrierIdToGrids_m.get(siteId.carrierId).getOrElse(Nil)
				} yield { CarrierGridSiteIndex(siteId.carrierId, grid_i, siteId.siteIndex) -> modelName }
				l.groupBy(_._1).mapValues(_.map(_._2).toSet)
			}
			//_ = println("siteIdToModelNames_m:")
			//_ = siteIdToModelNames_m.toList.sortBy(_._1.carrierId).foreach(println)
			//_ = println()

			/*// TODO: create System Liquid site, siteModel, and labware
			val siteSystem_? = for {
				o <- tableData.lExternalObject.find(_.carrier.sName == "System")
				site <- createSite(o.carrier, -1, s"${agentIdent} System Liquid")
			} yield site*/
			
			// Get map of sites we'll need plus their evoware siteId
			siteNameToSiteId_m <- getSiteNameToSiteIdMap(evowareProtocolData, carrierData, tableData)
			//_ = println("siteNameToSiteId_m:")
			//_ = siteNameToSiteId_m.toList.sortBy(_._1).foreach(println)
			//_ = println()
			
			// Construct map of labware models that the sites should hold
			siteNameToModelNames_m <- ResultC.mapAll(siteNameToSiteId_m)({ case (siteName, siteId) =>
				for {
					modelName_l <- ResultC.from(siteIdToModelNames_m.get(siteId), s"site `$siteName`: no compatible labware models found for this site")
				} yield siteName -> modelName_l
			}).map(_.toMap)
			//_ = println("siteNameToModelNames_m:")
			//_ = siteNameToModelNames_m.toList.sortBy(_._1).foreach(println)
			//_ = println()
			
			// Create the site models and get a map from set of labware models to corresponding site model name
			modelNamesToSiteModelName_m = {
				val modelName_ll = siteNameToModelNames_m.values.toSet
				val modelsToSiteModelName_l = for ((modelName_l, i) <- modelName_ll.zipWithIndex) yield {
					val siteModelName = s"${agentName}.sm${i}"
					builder.addSiteModel(siteModelName)
					builder.appendStackables(siteModelName, modelName_l)
					modelName_l -> siteModelName
				}
				modelsToSiteModelName_l.toMap
			}
			
			// Build the sites
			_ = {
				def buildSite(
					siteName: String,
					siteId: CarrierGridSiteIndex
				): ResultC[Unit] = {
					// Create the site
					val rjsSite = RjsBasicMap(
						"evowareCarrier" -> RjsNumber(siteId.carrierId),
						"evowareGrid" -> RjsNumber(siteId.gridIndex),
						"evowareSite" -> RjsNumber(siteId.siteIndex + 1)
					)
					builder.addSite(siteName, rjsSite)
			
					// Indicate which labware models can go on the site
					val modelName_l = siteIdToModelNames_m.getOrElse(siteId, Set())
					if (!modelName_l.isEmpty) {
						val siteModelName = modelNamesToSiteModelName_m(modelName_l)
						builder.setModel(siteName, siteModelName)
					}
					ResultC.unit(())
				}

				for ((siteName, siteId) <- siteNameToSiteId_m) {
					val siteConfig = evowareProtocolData.sites(siteName)
					buildSite(siteName, siteId)
				}
			}
			
			// Build the devices
			_ <- ResultC.context("devices") {
				val carrierId_l = tableData.carrierIdToGrids_m.keys.toList
				val carrier_l = carrierId_l.flatMap(tableData.configFile.mapIdToCarrier.get)
				val nameToCarrier_m = carrier_l.map(c => c.sName -> c).toMap
				val partToCarrier_m = carrier_l.flatMap(c => c.partNo_?.map(_ -> c)).toMap
				ResultC.foreach(evowareProtocolData.devices) { case (deviceName, deviceConfig) =>
					ResultC.context(deviceName) {
						for {
							carrierE <- ResultC.from(
								nameToCarrier_m.get(deviceConfig.evowareName)
									.orElse(partToCarrier_m.get(deviceConfig.evowareName)),
								s"unknown Evoware device: ${deviceConfig.evowareName}"
							)
							gridIndex_l <- ResultC.from(tableData.carrierIdToGrids_m.get(carrierE.id), s"device is missing from the specified table: ${carrierE}\n" + tableData.carrierIdToGrids_m.keys.toList.sorted.mkString("\n"))
							// Find sites for this device
							siteName_l <- {
								deviceConfig.sitesOverride match {
									case Nil =>
										ResultC.unit(siteNameToSiteId_m.toList.filter(kv => gridIndex_l.contains(kv._2.gridIndex)).map(_._1))
									case l =>
										ResultC.map(l) { siteName =>
											for {
												_ <- ResultC.assert(siteNameToSiteId_m.contains(siteName), s"unknown site: $siteName")
											} yield siteName
										}
								}
							}
						} yield {
							val rjsDevice = RjsBasicMap(
								"type" -> RjsString(deviceConfig.`type`),
								"evowareName" -> RjsString(carrierE.sName)
							)
							builder.addObject(deviceName, rjsDevice)
							builder.addPlanningDomainObject(deviceName, deviceConfig.`type`)
							builder.appendAgentDevice(agentName, deviceName)
							
							for (siteName <- siteName_l) {
								builder.appendDeviceSite(deviceName, siteName)
							}
							
							val modelName_l = siteName_l.flatMap(siteName => siteNameToModelNames_m.getOrElse(siteName, Nil)) 
							for (modelName <- modelName_l) {
								builder.appendDeviceModel(deviceName, modelName)
							}
						}
					}
				}
			}
		} yield builder.get
	}

	def getSiteNameToSiteIdMap(
		evowareProtocolData: EvowareProtocolData,
		carrierData: EvowareCarrierData,
		tableData: EvowareTableData
	): ResultC[Map[String, CarrierGridSiteIndex]] = {
		ResultC.context("sites") {
			for {
				l <- ResultC.mapAll(evowareProtocolData.sites) { case (siteName, siteConfig) =>
					println("(siteName, siteConfig): "+(siteName, siteConfig))
					ResultC.context(siteName) {
						siteConfig match {
							case EvowareSiteConfig(Some(carrierName), None, siteIndex_?) =>
								for {
									pair <- {
										if (carrierName == "System") {
											ResultC.unit((-1, -1))
										}
										else {
											for {
												carrierE <- ResultC.from(carrierData.mapNameToCarrier.get(carrierName), s"unknown carrier: $carrierName")
												gridIndex_l <- ResultC.from(tableData.carrierIdToGrids_m.get(carrierE.id), s"carrier is missing from the given table: $carrierName")
												_ <- ResultC.assert(!gridIndex_l.isEmpty, s"unable to configure device `$carrierName`, because has not been placed on the selected table")
												_ <- ResultC.assert(gridIndex_l.size == 1, s"unable to configure device `$carrierName`, because it's present on the selected table more than once: grids ${gridIndex_l}")
											} yield (carrierE.id, gridIndex_l.head)
										}
									}
								} yield {
									val (carrierId, gridIndex) = pair
									val siteIndex = siteIndex_?.getOrElse(1) - 1
									siteName -> CarrierGridSiteIndex(carrierId, gridIndex, siteIndex)
								}
							case EvowareSiteConfig(None, Some(gridIndex), Some(siteIndex1)) =>
								val siteIndex = siteIndex1 - 1
								for {
									carrierId <- ResultC.from(tableData.carrierIdToGrids_m.find(_._2.contains(gridIndex)).map(_._1), s"site `$siteName` requires a carrier at grid $gridIndex, but no carrier has been placed at that grid on the selected table")
									carrierE <- ResultC.from(carrierData.mapIdToCarrier.get(carrierId), "carrier with ID $carrierID is on selected table at grid $gridIndex, but it is not defined in the Carrier file")
									_ <- ResultC.assert(siteIndex <= carrierE.nSites, "site `$siteName` has an invalid site index $siteIndex")
								} yield {
									siteName -> CarrierGridSiteIndex(carrierId, gridIndex, siteIndex)
								}
							case _ =>
								ResultC.error("you must either supply `carrier` (and optionally `site`) OR both `grid` and `site`")
						}
					}
				}
			} yield l.toMap
		}
	}

}
