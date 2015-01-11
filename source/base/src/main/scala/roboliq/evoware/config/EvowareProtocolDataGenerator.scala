package roboliq.evoware.config

import spray.json._
import spray.json.DefaultJsonProtocol._
import grizzled.slf4j.Logger
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import roboliq.core._
import roboliq.entities._
import roboliq.input.commands._
import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.MultiMap
import roboliq.utils.FileUtils
import org.apache.commons.io.FilenameUtils
import java.io.File
import roboliq.evoware.translator.EvowareConfigData
import roboliq.evoware.translator.EvowareConfig
import roboliq.evoware.translator.EvowareClientScriptBuilder
import scalax.collection.Graph
import scalax.collection.edge.LHyperEdge
import scalax.collection.edge.LkUnDiEdge
import roboliq.plan.CallTree
import roboliq.plan.Call
import roboliq.ai.strips
import roboliq.ai.plan.Unique
import roboliq.plan.CommandSet
import roboliq.plan.OperatorInfo
import roboliq.ai.plan.PartialPlan
import roboliq.commands.OperatorHandler_TransportLabware
import roboliq.plan.ActionHandler
import roboliq.commands._
import roboliq.plan.OperatorHandler
import roboliq.commands.ShakePlateOperatorHandler
import roboliq.commands.ShakePlateActionHandler
import roboliq.evoware.handler.EvowareInfiniteM200InstructionHandler
import roboliq.evoware.translator.EvowareSealerProgram
import roboliq.evoware.commands.OperatorHandler_EvowareTransportLabware
import roboliq.evoware.commands.EvowareCentrifugeRunActionHandler
import roboliq.evoware.commands.EvowareCentrifugeRunOperatorHandler
import roboliq.evoware.commands.EvowareTimerWaitOperatorHandler
import roboliq.evoware.commands.EvowareTimerStartActionHandler
import roboliq.evoware.commands.EvowareTimerWaitActionHandler
import roboliq.evoware.commands.EvowareTimerSleepActionHandler
import roboliq.evoware.commands.EvowareTimerStartOperatorHandler
import roboliq.commands.PromptOperatorOperatorHandler
import roboliq.commands.PromptOperatorActionHandler
import roboliq.evoware.commands.EvowareBeginLoopOperatorHandler
import roboliq.evoware.commands.EvowareEndLoopOperatorHandler
import roboliq.evoware.commands.EvowareBeginLoopActionHandler
import roboliq.evoware.commands.EvowareEndLoopActionHandler
import com.google.gson.Gson
import roboliq.input.EvowareAgentBean
import roboliq.input.ProtocolDetails
import roboliq.input.ProtocolDetailsBuilder
import roboliq.evoware.parser.EvowareCarrierData
import roboliq.evoware.parser.EvowareLabwareModel
import roboliq.evoware.parser.EvowareTableData
import roboliq.input.RjsString
import roboliq.input.RjsNumber
import roboliq.input.RjsBasicMap
import roboliq.input.RjsAbstractMap

object EvowareProtocolDataGenerator {
	def createProtocolData(
		agentIdent: String,
		agentConfig: EvowareAgentConfig,
		table_l: List[String],
		searchPath_l: List[File]
	): ResultC[ProtocolDetails] = {
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
						ResultC.error(s"No table specified for agent `$agentIdent`")
				case s :: Nil => ResultC.unit(s)
				case l => ResultC.error(s"Agent `$agentIdent` can only be assigned one table, but multiple tables were specified: ${l.mkString(",")}")
			}
			tableSetupConfig = tableSetup_m(tableName)
			// Load table file
			tableFile <- ResultC.from(Option(tableSetupConfig.tableFile), s"tableFile property must be set on tableSetup `$tableName`")
			tableData <- ResultC.from(roboliq.evoware.parser.EvowareTableData.loadFile(carrierData, tableFile))
			// Merge protocolData of agent and table
			protocolDataA = agentConfig.protocolDetails_?.getOrElse(new ProtocolDetails())
			protocolDataB = tableSetupConfig.protocolDetails_?.getOrElse(new ProtocolDetails())
			protocolData0 <- protocolDataA merge protocolDataB
			//_ = println("protocolDataA:\n"+protocolDataA)
			//_ = println("protocolDataB:\n"+protocolDataB)
			//_ = println("protocolData0:\n"+protocolData0)
			// Merge evowareProtocolData of agent and table
			evowareProtocolDetails = agentConfig.evowareProtocolData_?.getOrElse(EvowareProtocolData.empty)
			evowareProtocolDataB = tableSetupConfig.evowareProtocolData_?.getOrElse(EvowareProtocolData.empty)
			evowareProtocolData0 = evowareProtocolDetails merge evowareProtocolDataB
			// Convert evowareProtocolData0 to ProtocolData
			protocolData1 <- convertEvowareProtocolData(agentIdent, protocolData0, evowareProtocolData0, carrierData, tableData)
			//_ = println("protocolData1:\n"+protocolData1)
			// Merge protocolDatas
			protocolData2 <- protocolData0 merge protocolData1
			//_ = println("protocolData2:\n"+protocolData2)
		} yield protocolData2
	}
	
	private def convertEvowareProtocolData(
		agentName: String,
		data0: ProtocolDetails,
		evowareProtocolData: EvowareProtocolData,
		carrierData: EvowareCarrierData,
		tableData: EvowareTableData
	): ResultC[ProtocolDetails] = {
		//sites: Map[String, EvowareSiteConfig],
		//devices: Map[String, EvowareDeviceConfig],
		//transporterBlacklist: List[EvowareTransporterBlacklistConfig]

		// Gather list of all available labware models referenced in data0
		//println("data0.objects.map.toList: "+data0.objects.map.toList)
		val modelNameToEvowareName_l: List[(String, String)] = data0.objects.map.toList.collect({ case (name, m: RjsAbstractMap) if m.getValue("type") == Some(RjsString("PlateModel")) && m.getValueMap.contains("evowareName") => name -> m.getValueMap("evowareName").toText })
		println("modelNameToEvowareName_l: "+modelNameToEvowareName_l)
		
		val builder = new ProtocolDetailsBuilder
		val siteIdToSiteName_m = new HashMap[(Int, Int), String]
		for {
			// Find the evoware labware models
			modelNameToEvowareModel_l <- ResultC.mapAll(modelNameToEvowareName_l) { case (modelName, evowareName) =>
				ResultC.context(s"protocolData.objects.$modelName") {
					for {
						mE <- ResultC.from(carrierData.mapNameToLabwareModel.get(evowareName), s"unknown Evoware labware model: $evowareName")
					} yield modelName -> mE
				}
			}
			
			// Create map from siteId to all labware model names that can be placed on that site
			siteIdToModelNames_m: Map[(Int, Int), Set[String]] = {
				val l = for {
					(modelName, mE) <- modelNameToEvowareModel_l
					siteId <- mE.sites
				} yield { siteId -> modelName }
				l.groupBy(_._1).mapValues(_.map(_._2).toSet)
			}

			// TODO: create System Liquid site, siteModel, and labware
			
			// Get map of sites we'll need plus their evoware siteId
			siteNameToSiteId_m <- getSiteNameToSiteIdMap(evowareProtocolData, carrierData, tableData)
			
			// Construct map of labware models that the sites should hold
			siteNameToModelNames_m =
				siteNameToSiteId_m.flatMap { case (siteName, siteId) =>
					siteIdToModelNames_m.get(siteId).map { modelName_l =>
						siteName -> modelName_l
					}
				}
			
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
					gridIndex: Int,
					siteIndex: Int
				): ResultC[Unit] = {
					// Create the site
					val rjsSite = RjsBasicMap(
						"evowareGrid" -> RjsNumber(gridIndex),
						"evowareSite" -> RjsNumber(siteIndex)
					)
					builder.addSite(siteName, rjsSite)
			
					// Indicate which labware models can go on the site
					val siteId = (gridIndex, siteIndex - 1)
					val modelName_l = siteIdToModelNames_m.getOrElse(siteId, Set())
					if (!modelName_l.isEmpty) {
						val siteModelName = modelNamesToSiteModelName_m(modelName_l)
						builder.setModel(siteName, siteModelName)
					}
					ResultC.unit(())
				}

				for ((siteName, siteId) <- siteNameToSiteId_m) {
					val siteConfig = evowareProtocolData.sites(siteName)
					buildSite(siteName, siteId._1, siteId._2 + 1)
				}
			}
			
			// Build the devices
			_ <- ResultC.context("devices") {
				val partToCarrier_m = carrierData.mapNameToCarrier.values.flatMap(c => c.partNo_?.map(_ -> c)).toMap
				ResultC.foreach(evowareProtocolData.devices) { case (deviceName, deviceConfig) =>
					ResultC.context(deviceName) {
						for {
							carrierE <- ResultC.from(
								carrierData.mapNameToCarrier.get(deviceConfig.evowareName)
									.orElse(partToCarrier_m.get(deviceConfig.evowareName)),
								s"unknown Evoware device: ${deviceConfig.evowareName}"
							)
							gridIndex <- ResultC.from(tableData.mapCarrierToGrid.get(carrierE), s"device is missing from the specified table: ${deviceConfig.evowareName}")
							// Find sites for this device
							siteName_l <- {
								deviceConfig.sitesOverride match {
									case Nil =>
										ResultC.unit(siteIdToSiteName_m.toList.filter(kv => kv._1._1 == gridIndex).map(_._2))
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

	private def getSiteNameToSiteIdMap(
		evowareProtocolData: EvowareProtocolData,
		carrierData: EvowareCarrierData,
		tableData: EvowareTableData
	): ResultC[Map[String, (Int, Int)]] = {
		ResultC.context("sites") {
			for {
				l <- ResultC.mapAll(evowareProtocolData.sites) { case (siteName, siteConfig) =>
					ResultC.context(siteName) {
						siteConfig match {
							case EvowareSiteConfig(Some(carrierName), None, siteIndex_?) =>
								for {
									carrierE <- ResultC.from(carrierData.mapNameToCarrier.get(carrierName), s"unknown carrier: $carrierName")
									gridIndex <- ResultC.from(tableData.mapCarrierToGrid.get(carrierE), s"carrier is missing from the given table: $carrierName")
									siteIndex = siteIndex_?.getOrElse(1)
								} yield siteName -> (gridIndex, siteIndex)
							case EvowareSiteConfig(None, Some(gridIndex), Some(siteIndex)) =>
								ResultC.unit(siteName -> (gridIndex, siteIndex))
							case _ =>
								ResultC.error("you must either supply `carrier` (and optionally `site`) OR both `grid` and `site`")
						}
					}
				}
			} yield l.toMap
		}
	}

}
