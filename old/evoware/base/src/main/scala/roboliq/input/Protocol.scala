package roboliq.input

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

case class WellGroupBean(
	name: String,
	description_? : Option[String],
	well: PipetteDestinations
)

case class SubstanceBean(
	name: String,
	description_? : Option[String],
	type_? : Option[SubstanceKind.Value],
	tipCleanPolicy_? : Option[TipCleanPolicy],
	contaminants: Set[String]
)

private case class SourceSubstanceBean(
	name: String,
	amount_? : Option[AmountSpec],
	description_? : Option[String],
	type_? : Option[SubstanceKind.Value],
	tipCleanPolicy_? : Option[TipCleanPolicy],
	contaminants: Set[String]
)

private case class SourceBean(
	name: List[String],
	well: PipetteDestinations,
	substance: List[SourceSubstanceBean],
	amount_? : Option[LiquidVolume]
)

private case class ReagentBean(
	id: String,
	wells: PipetteDestinations,
	contaminants : Set[String],
	viscosity_? : Option[String],
	sterilize_? : Option[String],
	pipettePolicy_? : Option[String],
	key_? : Option[String]
)

class Protocol {
	
	private val logger = Logger[this.type]

	val eb = new EntityBase
	val state0 = new WorldStateBuilder
	private var cs: CommandSet = null
	private var tree: CallTree = null
	//private var tasks = new ArrayBuffer[Rel]
	private var var_i = 0
	private var commandDef_l: List[CommandDef] = Nil

	// HACK: defined here so that loadConfig() and loadEvoware() both have access
	private val offsiteModel = SiteModel(gid)
	private val userArm = Transporter(gid)
	private val userArmSpec = TransporterSpec("userArmSpec")

	private def gid: String = java.util.UUID.randomUUID().toString()
	private def nvar: Int = { var_i += 1; var_i }
	
	/** Specs which consists of a single string.  This is a list of tuples (specIdent, string) */
	val specToString_l = new ArrayBuffer[(String, String)]
	/** Valid device+spec combinations.  List of tuples (deviceIdent, specIdent) */
	val deviceToSpec_l = new ArrayBuffer[(String, String)]
	/** Valid device+model+spec combinations.  List of tuples (deviceIdent, evoware plate model ID, specIdent) */
	val deviceToModelToSpec_l = new ArrayBuffer[(String, String, String)]
	
	val nameToSubstance_m = new HashMap[String, Substance]
	/**
	 * Map of task variable identifier to an internal object -- for example, the name of a text variable to the actual text.
	 */
	val idToObject = new HashMap[String, Object]
	/**
	 * Individual agents may need to map identifiers to internal objects
	 */
	val agentToIdentToInternalObject = new HashMap[String, HashMap[String, Object]]
	val agentToBuilder_m = new HashMap[String, ClientScriptBuilder]

	def loadCommandSet(): RsResult[CommandSet] = {
		val actionHandler_l = List[ActionHandler](
			new CarouselCloseActionHandler,
			new CarouselOpenSiteActionHandler,
			new CloseDeviceSiteActionHandler,
			new CommentActionHandler,
			new DistributeActionHandler,
			new EnsureLabwareLocationActionHandler,
			new EvowareBeginLoopActionHandler,
			new EvowareCentrifugeRunActionHandler,
			new EvowareEndLoopActionHandler,
			new EvowareTimerSleepActionHandler,
			new EvowareTimerStartActionHandler,
			new EvowareTimerWaitActionHandler,
			new GetLabwareLocationActionHandler,
			new Hack01ActionHandler,
			new MeasureAbsorbanceActionHandler,
			new OpenDeviceSiteActionHandler,
			new PipetteActionHandler,
			new PromptOperatorActionHandler,
			new RunDeviceActionHandler,
			new SealPlateActionHandler,
			new ShakePlateActionHandler,
			new TitrateActionHandler,
			new TransportLabwareActionHandler
		)
		val operatorHandler_l = List[OperatorHandler](
			new CarouselCloseOperatorHandler("mario__Centrifuge", List("CENTRIFUGE_1", "CENTRIFUGE_2", "CENTRIFUGE_3", "CENTRIFUGE_4")),
			// FIXME: HACK: need to add the operators from config somehow, not from here
			new CarouselOpenSiteOperatorHandler("mario", "mario__Centrifuge", "CENTRIFUGE_1", List("CENTRIFUGE_1", "CENTRIFUGE_2", "CENTRIFUGE_3", "CENTRIFUGE_4")),
			new CarouselOpenSiteOperatorHandler("mario", "mario__Centrifuge", "CENTRIFUGE_2", List("CENTRIFUGE_1", "CENTRIFUGE_2", "CENTRIFUGE_3", "CENTRIFUGE_4")),
			new CarouselOpenSiteOperatorHandler("mario", "mario__Centrifuge", "CENTRIFUGE_3", List("CENTRIFUGE_1", "CENTRIFUGE_2", "CENTRIFUGE_3", "CENTRIFUGE_4")),
			new CarouselOpenSiteOperatorHandler("mario", "mario__Centrifuge", "CENTRIFUGE_4", List("CENTRIFUGE_1", "CENTRIFUGE_2", "CENTRIFUGE_3", "CENTRIFUGE_4")),
			new CloseDeviceSiteOperatorHandler,
			new CommentOperatorHandler,
			new DistributeOperatorHandler(1),
			new DistributeOperatorHandler(2),
			new DistributeOperatorHandler(3),
			new DistributeOperatorHandler(4),
			new EnsureLabwareLocationOperatorHandler,
			new EvowareBeginLoopOperatorHandler,
			new EvowareEndLoopOperatorHandler,
			new EvowareCentrifugeRunOperatorHandler("mario__Centrifuge", List("CENTRIFUGE_1", "CENTRIFUGE_2", "CENTRIFUGE_3", "CENTRIFUGE_4")),
			new EvowareTimerStartOperatorHandler,
			new EvowareTimerWaitOperatorHandler,
			new GetLabwareLocationOperatorHandler,
			new MeasureAbsorbanceOperatorHandler,
			new OpenDeviceSiteOperatorHandler,
			new PipetteOperatorHandler(1),
			new PipetteOperatorHandler(2),
			new PipetteOperatorHandler(3),
			new PipetteOperatorHandler(4),
			new PromptOperatorOperatorHandler,
			new RunDeviceOperatorHandler,
			new SealPlateOperatorHandler,
			new ShakePlateOperatorHandler,
			new TitrateOperatorHandler(1),
			new TitrateOperatorHandler(2),
			new TitrateOperatorHandler(3),
			new TitrateOperatorHandler(4),
			new OperatorHandler_EvowareTransportLabware,
			new OperatorHandler_TransportLabware
		)
		// TODO: HACK: the idea here was to only put the required operators in the domain,
		// but since some actions depend on various operators, we'll need to pull in
		// all of those dependencies for this to work.
		// Set that up.  For now, I'm just including ALL operators.
		val autoHandler_l = operatorHandler_l.map(_.getDomainOperator.name)
		cs = new CommandSet(
			nameToActionHandler_m = actionHandler_l.map(h => h.getActionName -> h).toMap,
			nameToOperatorHandler_m = operatorHandler_l.map(h => h.getDomainOperator.name -> h).toMap,
			nameToAutoOperator_l = autoHandler_l,
			nameToMethods_m = Map(
				/*"shakePlate" -> List(
					shakePlate_to_tecan_shakePlate,
					(call: Call) => RqSuccess(call.copy(name = "tecan_shakePlate")
				)*/
			)
		)
		RqSuccess(cs)
	}
	
	// TODO: This should probably be moved out of the Protocol class
	def loadConfigBean(
		configBean: ConfigBean,
		table_l: List[String],
		searchPath_l: List[File]
	): RsResult[Unit] = {
		import roboliq.entities._
		
		val user = Agent(gid, Some("user"))
		val offsite = Site(gid, Some("offsite"))
		
		// TODO: put these into a for-comprehension in order to return warnings and errors
		eb.addAgent(user, "user")
		eb.addModel(offsiteModel, "offsiteModel")
		eb.addSite(offsite, "offsite")
		eb.addDevice(user, userArm, "userArm")
		eb.addDeviceSpec(userArm, userArmSpec, "userArmSpec")
		// userArm can transport from offsite
		eb.addRel(Rel("transporter-can", List("userArm", "offsite", "userArmSpec")))
		
		for {
			// Aliases
			_ <- {
				if (configBean.aliases != null) {
					for ((key, value) <- configBean.aliases.toMap) {
						eb.addAlias(key, value)
					}
				}
				RsSuccess(())
			}
			
			_ <- {
				// Logic
				if (configBean.logic != null) {
					for (s <- configBean.logic.toList) {
						val l = s.split(" ").toList
						eb.addRel(Rel(l.head, l.tail))
					}
				}
				RsSuccess(())
			}
			
			_ <- {
				// Specs
				if (configBean.specs != null) {
					for ((key, value) <- configBean.specs.toMap) {
						specToString_l += ((key, value))
					}
				}
				
				// device+spec combinations
				if (configBean.deviceToSpec != null) {
					for (l <- configBean.deviceToSpec.toList) {
						deviceToSpec_l += ((l(0), l(1)))
					}
				}
				
				// device+model+spec combinations
				if (configBean.deviceToModelToSpec != null) {
					for (l <- configBean.deviceToModelToSpec.toList) {
						deviceToModelToSpec_l += ((l(0), l(1), l(2)))
					}
				}
				RsSuccess(())
			}
			
			_ <- {
				if (configBean.evowareAgents == null) {
					RsSuccess(())
				}
				else {
					RsResult.mapAll(configBean.evowareAgents.toList)(pair => {
						val (agentIdent, agentBean) = pair
						val tableNameDefault = s"${agentIdent}_default"
						val tableSetup_m = agentBean.tableSetups.toMap.map(pair => s"${agentIdent}_${pair._1}" -> pair._2)
						for {
							// Load carrier file
							evowarePath <- RsResult(agentBean.evowareDir, "evowareDir must be set")
							carrierData <- roboliq.evoware.parser.EvowareCarrierData.loadFile(new File(evowarePath, "Carrier.cfg").getPath)
							// FIXME: for debug only
							//_ = carrierData.printCarriersById
							// ENDIF
							// Choose a table
							tableName <- table_l.filter(tableSetup_m.contains) match {
								case Nil =>
									if (tableSetup_m.contains(tableNameDefault))
										RsSuccess(tableNameDefault)
									else
										RsError(s"No table specified for agent `$agentIdent`")
								case s :: Nil => RsSuccess(s)
								case l => RsError(s"Agent `$agentIdent` can only be assigned one table, but multiple tables were specified: $l")
							}
							tableSetupBean = tableSetup_m(tableName)
							// Load table file
							tableFile <- RsResult(tableSetupBean.tableFile, s"tableFile property must be set on tableSetup `$tableName`")
							tableData <- roboliq.evoware.parser.EvowareTableData.loadFile(carrierData, tableFile)
							configEvoware = new ConfigEvoware(eb, agentIdent, carrierData, tableData,
									agentBean, tableSetupBean, offsiteModel, userArm,
									specToString_l.toList, deviceToSpec_l.toList, deviceToModelToSpec_l.toList)
							scriptBuilder <- configEvoware.loadEvoware()
						} yield {
							agentToIdentToInternalObject(agentIdent) = configEvoware.identToAgentObject
							agentToBuilder_m += agentIdent -> scriptBuilder
							if (!agentToBuilder_m.contains("user"))
								agentToBuilder_m += "user" -> scriptBuilder
						}
					}).map(_ => ())
				}
			}
			
			_ <- {
				if (configBean.commandFiles == null) {
					RsSuccess(())
				}
				else {
					for {
						commandDef_ll <- RsResult.mapAll(configBean.commandFiles.toList)(filename => {
							for {
								file <- roboliq.utils.FileUtils.findFile(filename, searchPath_l)
								jsval <- loadJsonValue(file)
								commandDef_l <- Converter.convAs[List[CommandDef]](jsval, eb, None)
							} yield commandDef_l
						})
					} yield {
						this.commandDef_l = commandDef_ll.flatten
						println("commandDef_l:")
						commandDef_l.foreach(println)
						
						val operator_l = commandDef_l.map(_.createOperator)
						operator_l.foreach(println)
						
						1/0
					}
				}
			}
		} yield ()
	}

	// REFACTOR: duplicated code from Runner
	private def yamlToJson(s: String): RsResult[String] = {
		import org.yaml.snakeyaml._
		val yaml = new Yaml()
		//val o = yaml.load(s).asInstanceOf[java.util.Map[String, Object]]
		val o = yaml.load(s)
		val gson = new Gson
		val s_~ = gson.toJson(o)
		//println("gson: " + s_~)
		RsSuccess(s_~)
	}

	private def loadJsonValue(file: File): RsResult[JsValue] = {
		for {
			_ <- RsResult.assert(file.exists, s"File not found: ${file.getPath}")
			bYaml <- FilenameUtils.getExtension(file.getPath).toLowerCase match {
				case "json" => RsSuccess(false)
				case "yaml" => RsSuccess(true)
				case ext => RsError(s"unrecognized command file extension `$ext`.  Expected either json or yaml.")
			}
			input0 = org.apache.commons.io.FileUtils.readFileToString(file)
			input <- if (bYaml) yamlToJson(input0) else RsSuccess(input0) 
		} yield input.parseJson
	}

	def loadJson(jsobj: JsObject): RsResult[Unit] = {
		for {
			_ <- jsobj.fields.get("labware") match {
				case Some(JsObject(map)) =>
					RqResult.mapAll(map.toList)(pair => {
						val (name,jsobj) = pair
						def make(modelRef: String, locationRef: String): RqResult[Unit] = {
							// REFACTOR: duplicates lots of code from the `plates` section below
							val key = gid
							//logger.debug("modelKey: "+modelKey)
							//println("eb.nameToEntity: "+eb.nameToEntity)
							//println("eb.idToEntity: "+eb.idToEntity)
							//println("eb.idToEntity.get(\"Thermocycler Plate\"): "+eb.idToEntity.get("Thermocycler Plate"))
							//logger.debug("eb.aliases: "+eb.aliases)
							for {
								model <- eb.getEntityAs[PlateModel](modelRef)
								site <- RsResult.from(eb.getEntity(locationRef), s"Unknown location `$locationRef`")
							} yield {
								val plate = new Plate(key, Some(name))
								eb.addLabware(plate, name)
								eb.setModel(plate, model)
								state0.labware_model_m(plate) = model
								// Create plate wells
								for (row <- 0 until model.rows; col <- 0 until model.cols) {
									val index = row + col * model.rows
									val ident = WellIdentParser.wellId(plate, model, row, col)
									val well = new Well(gid, Some(ident))
									state0.addWell(well, plate, RowCol(row, col), index)
								}
								eb.setLocation(plate, site)
								state0.labware_location_m(plate) = site
								()
							}
						}
						jsobj match {
							case JsString(modelRef) => make(modelRef, "offsite")
							case JsObject(map) =>
								(map.get("model"), map.get("location")) match {
									case (Some(JsString(modelRef)), Some(JsString(locationRef))) => make(modelRef, locationRef)
									case (Some(JsString(modelRef)), None) => make(modelRef, "offsite")
									case _ => RqError("Expected values for `model` and `location`")
								}
							case _ => RqError("Expected a string for model reference")
						}
					})
				case _ => RqSuccess(())
			}
			
			_ <- jsobj.fields.get("wellGroup") match {
				case Some(jsval) =>
					for {
						wellGroupBean_l <- Converter.convAs[List[WellGroupBean]](jsval, eb, Some(state0.toImmutable))
						_ <- RqResult.mapAll(wellGroupBean_l) { bean =>
							// TODO: need to check for naming conflict, also with entities -- in general, all names should be tracked in a central location so that naming conflicts can be detected and/or resolved.
							eb.wellGroupToWells_m (bean.name) = bean.well.l
							RqSuccess(())
						}
					} yield ()
				case _ => RqSuccess(())
			}
			
			// TODO: FIXME: Remove this
			_ <- jsobj.fields.get("substances") match {
				case Some(js) =>
					val inputs: List[Map[String, String]] = js.convertTo[List[Map[String, String]]]
					RqResult.toResultOfList(inputs.map(m => {
						val key = m.getOrElse("id", gid)
						val name = m.getOrElse("name", key)
						val kind = m("kind") match {
							case "Liquid" => SubstanceKind.Liquid
							case "Dna" => SubstanceKind.Dna
						}
						val tipCleanPolicy = m.getOrElse("tipCleanPolicy", "Thorough").toLowerCase match {
							case "none" => TipCleanPolicy.NN
							case "thoroughnone" => TipCleanPolicy.TN
							case "thoroughlight" => TipCleanPolicy.TL
							case "thorough" => TipCleanPolicy.TT
							case "decontaminate" => TipCleanPolicy.DD
						}
						val substance = Substance(key, Some(name), None, kind, tipCleanPolicy, Set(), None, None, None, None, Nil, None)
						nameToSubstance_m(name) = substance
						RqSuccess(())
					}))
				case _ => RqSuccess(())
			}
			
			_ <- jsobj.fields.get("substance") match {
				case Some(jsval) =>
					for {
						substanceBean_l <- Converter.convAs[List[SubstanceBean]](jsval, eb, Some(state0.toImmutable))
						_ <- RqResult.mapAll(substanceBean_l) { bean =>
							val substance = Substance(
								key = gid,
								label = Some(bean.name),
								description = bean.description_?,
								kind = bean.type_?.getOrElse(SubstanceKind.Liquid),
								tipCleanPolicy = bean.tipCleanPolicy_?.getOrElse(TipCleanPolicy.TT),
								contaminants = bean.contaminants,
								costPerUnit_? = None,
								valuePerUnit_? = None,
								molarity_? = None,
								gramPerMole_? = None,
								celciusAndConcToViscosity = Nil,
								sequence_? = None
							)
							nameToSubstance_m(bean.name) = substance
							RqSuccess(())
						}
					} yield ()
				case _ => RqSuccess(())
			}
			
			//println(jsobj.fields.get("source"))
			_ <- jsobj.fields.get("source") match {
				case Some(jsval) =>
					//println("jsval: "+jsval)
					//println(Converter.convAs[List[SourceBean]](jsval, eb, None))
					for {
						sourceBean_l <- Converter.convAs[List[SourceBean]](jsval, eb, Some(state0.toImmutable))
						_ <- RsResult.mapFirst(sourceBean_l.zipWithIndex) { case (sourceBean, i) =>
							//val well_l = sourceBean.well.l.map(_.well)
							for {
								// TODO: check that array sizes for `name` and `well` are compatible
								name_l <- (sourceBean.name.size, sourceBean.well.l.size) match {
									case (0, _) => RsError(s"source ${i+1}: `name` must be supplied")
									case (_, 0) => RsError(s"source ${i+1}: `well` must be supplied")
									case (1, n) => RsSuccess(List.fill(n)(sourceBean.name.head))
									case (a, b) if a != b => RsError(s"`name` and `well` lists must have the same size for source ${i+1}")
									case _ => RsSuccess(sourceBean.name)
								}
								// Replace any occurrences of "{{WELL}}" in the name with the well position
								nameToWell_l = name_l.zip(sourceBean.well.l).map { case (s, wellInfo) => s.replace("{{WELL}}", wellInfo.rowcol.toString) -> wellInfo }
								nameToWells_m: Map[String, List[WellInfo]] = nameToWell_l.groupBy(_._1).mapValues(l => l.map(_._2))
								_ <- RsResult.mapAll(nameToWells_m.toList) { case (name, well_l) =>
									val substance_l = sourceBean.substance match {
										case Nil => List(SourceSubstanceBean(
												name = name,
												amount_? = None,
												description_? = None,
												type_? = None,
												tipCleanPolicy_? = None,
												contaminants = Set()
											))
										case _ => sourceBean.substance
									}
									// List of substances and their optional amounts
									val mixtureToAmount_l = substance_l.map(substanceBean => {
										val substance = nameToSubstance_m.get(substanceBean.name) match {
											case Some(substance) => substance
											case None =>
												val substance = Substance(
													key = gid,
													label = Some(substanceBean.name),
													description = substanceBean.description_?,
													kind = SubstanceKind.Liquid,
													tipCleanPolicy = substanceBean.tipCleanPolicy_?.getOrElse(TipCleanPolicy.TT),
													contaminants = substanceBean.contaminants,
													costPerUnit_? = None,
													valuePerUnit_? = None,
													molarity_? = None,
													gramPerMole_? = None,
													celciusAndConcToViscosity = Nil,
													sequence_? = None
												)
												nameToSubstance_m(substanceBean.name) = substance
												substance
										}
										(Mixture(Left(substance)), substanceBean.amount_?)
									})
									// Get the mixture for the substance+amount list
									for {
										mixture <- Mixture.fromMixtureAmountList(mixtureToAmount_l)
									} yield {
										// Map the source name to the mixture and list of wells
										eb.sourceToMixture_m(name) = mixture
										eb.reagentToWells_m(name) = well_l.map(_.well)
										// Add the mixture to the wells
										val aliquot = Aliquot(mixture, Distribution.fromVolume(LiquidVolume.empty))
										for (well <- well_l) {
											state0.well_aliquot_m(well.well) = aliquot
										}
									}
								}
							} yield ()
						}
					} yield ()
				case _ => RqSuccess(())
			}
			
			// TODO: FIXME: Remove this
			//println(jsobj.fields.get("reagents"))
			_ <- jsobj.fields.get("reagents") match {
				case Some(jsval) =>
					//println("jsval: "+jsval)
					//println(Converter.convAs[Set[ReagentBean]](jsval, eb, None))
					for {
						reagentBean_l <- Converter.convAs[Set[ReagentBean]](jsval, eb, Some(state0.toImmutable))
						//_ = println("reagentBean_l: "+reagentBean_l)
						substance_l <- RsResult.toResultOfList(reagentBean_l.toList.map(bean => {
							val key = bean.key_?.getOrElse(gid)
							val name = bean.id
							val kind = SubstanceKind.Liquid
							for {
								tipCleanPolicy <- bean.sterilize_?.getOrElse("rinse").toLowerCase match {
									case "keep" => RsSuccess(TipCleanPolicy.NN)
									case "rinse/none" => RsSuccess(TipCleanPolicy.TN)
									case "rinse/light" => RsSuccess(TipCleanPolicy.TL)
									case "rinse" => RsSuccess(TipCleanPolicy.TT)
									case "replace" => RsSuccess(TipCleanPolicy.DD)
									case s => RsError(s"`tipPolicy`: unrecognized value for `$s`")
								}
							} yield {
								val substance = Substance(
									key = key,
									label = Some(bean.id),
									description = None,
									kind = SubstanceKind.Liquid,
									tipCleanPolicy = tipCleanPolicy,
									contaminants = bean.contaminants,
									costPerUnit_? = None,
									valuePerUnit_? = None,
									molarity_? = None,
									gramPerMole_? = None,
									celciusAndConcToViscosity = Nil,
									sequence_? = None
								)
								//println("substance: "+substance)
								//println("well_l: "+well_l)
								nameToSubstance_m(name) = substance
								val well_l = bean.wells.l.map(_.well)
								eb.reagentToWells_m(name) = well_l
								val mixture = Mixture(Left(substance))
								val aliquot = Aliquot(mixture, Distribution.fromVolume(LiquidVolume.empty))
								// Add aliquot to all referenced wells
								for (well <- well_l) {
									state0.well_aliquot_m(well) = aliquot
								}
								//println(eb.lookupLiquidSource(bean.id, state0.toImmutable))
							}
						}))
					} yield ()
				case _ => RqSuccess(())
			}
			
			_ <- jsobj.fields.get("plates") match {
				case Some(js) =>
					val plateInputs: List[Map[String, String]] = js.convertTo[List[Map[String, String]]]
					for (m <- plateInputs) {
						val id = m.getOrElse("id", gid)
						val name = m.getOrElse("name", id)
						val modelKey = m("model")
						logger.debug("modelKey: "+modelKey)
						//println("eb.nameToEntity: "+eb.nameToEntity)
						//println("eb.idToEntity: "+eb.idToEntity)
						//println("eb.idToEntity.get(\"Thermocycler Plate\"): "+eb.idToEntity.get("Thermocycler Plate"))
						logger.debug("eb.aliases: "+eb.aliases)
						val model = eb.getEntityAs[PlateModel](modelKey).toOption.get
						val plate = new Plate(id)
						eb.addLabware(plate, name)
						eb.setModel(plate, model)
						state0.labware_model_m(plate) = model
						// Create plate wells
						for (row <- 0 until model.rows; col <- 0 until model.cols) {
							val index = row + col * model.rows
							val ident = WellIdentParser.wellId(plate, model, row, col)
							val well = new Well(gid, Some(ident))
							state0.addWell(well, plate, RowCol(row, col), index)
						}
						m.get("location") match {
							case Some(key) =>
								val entity = eb.getEntity(key).get
								eb.setLocation(plate, entity)
							case _ =>
						}
					}
					RqSuccess(())
				case _ => RqSuccess(())
			}
			
			_ <- jsobj.fields.get("tubes") match {
				case Some(js) =>
					val tubeInputs = js.convertTo[List[Map[String, String]]]
					for (m <- tubeInputs) {
						val id = m.getOrElse("id", gid)
						val name = m.getOrElse("name", id)
						val modelKey = m("model")
						val model = eb.getEntity(modelKey).get.asInstanceOf[LabwareModel]
						val tube = new Plate(id)
						eb.addLabware(tube, name)
						eb.setModel(tube, model)
						state0.labware_model_m(tube) = model
						// Create tube well
						val well = new Well(gid, Some(s"$name()"))
						state0.addWell(well, tube, RowCol(0, 0), 0)
					}
					RqSuccess(())
				case _ => RqSuccess(())
			}
			
			_ <- jsobj.fields.get("wellContents") match {
				case Some(js) =>
					val inputs: List[Map[String, String]] = js.convertTo[List[Map[String, String]]]
					RqResult.toResultOfList(inputs.map(m => {
						val wellIdent = m("name")
						val contents_s = m("contents")
						for {
							aliquot <- AliquotParser.parseAliquot(contents_s, nameToSubstance_m.toMap)
							dst_l <- eb.lookupLiquidDestinations(wellIdent, state0.toImmutable)
						} yield {
							for (wellInfo <- dst_l.l) {
								state0.well_aliquot_m(wellInfo.well) = aliquot
							}
						}
					}))
				case _ => RsSuccess(())
			}

			_ <- jsobj.fields.get("shakerPrograms") match {
				case Some(jsval) =>
					for {
						m <- Converter.convAs[Map[String, ShakerSpec]](jsval, eb, None)
					} yield {
						for ((name, program) <- m) {
							eb.addUserShakerProgram(program, name)
						}
					}
				case _ => RqSuccess(())
			}
		
			_ <- jsobj.fields.get("protocol") match {
				case Some(jsval) => loadJsonProtocol_Protocol(jsval).map(tree => { this.tree = tree; () })
				case _ => RsSuccess(())
			}
		} yield ()
	}
	
	private def loadJsonProtocol_Protocol(jsval: JsValue): RsResult[CallTree] = {
		logger.debug("parse `protocol`")
		val path0 = new PlanPath(Nil, state0.toImmutable)
		def step(jscmd_l: List[JsValue], top_r: List[Call]): RqResult[CallTree] = {
			jscmd_l match {
				case Nil => RqSuccess(CallTree(top_r.reverse))
				case jscmd :: rest =>
					loadJsonProtocol_Protocol_getCommand(jscmd).flatMap(pair_? => {
						val top_r2 = pair_? match {
							case None => top_r
							case Some((cmd, nameToVal_l)) =>
								new Call(cmd, nameToVal_l) :: top_r
						}
						step(rest, top_r2)
					})
			}
		}
		jsval match {
			case JsArray(jscmd_l) =>
				step(jscmd_l, Nil)
			case _ =>
				RsError("unrecognized format for `protocol` section")
		}
	}

	private def loadJsonProtocol_Protocol_getCommand(
		jscmd: JsValue
	): RsResult[Option[(String, List[(Option[String], JsValue)])]] = {
		jscmd match {
			case JsString(line) =>
				// TODO: Create a parser
				val space_i = line.indexOf(" ")
				if (space_i <= 0) {
					RsSuccess(None)
				} else {
					val cmd = line.substring(0, space_i)
					val args = line.substring(space_i + 1)
					for {
						nameToVal_l <- loadJsonProtocol_Protocol_getNameVals(JsString(args))
					} yield Some((cmd, nameToVal_l))
				}
			case JsObject(cmd_m) =>
				if (cmd_m.size == 0)
					RsSuccess(None)
				else if (cmd_m.size > 1)
					RsError("expected single field with command name: "+cmd_m)
				else {
					val (cmd, jsval) = cmd_m.head
					for {
						nameToVal_l <- loadJsonProtocol_Protocol_getNameVals(jsval)
					} yield Some((cmd, nameToVal_l))
				}
			case _ =>
				RsError("unrecognized command format")
		}
	}
	
	private def loadJsonProtocol_Protocol_getNameVals(
		args: JsValue
	): RsResult[List[(Option[String], JsValue)]] = {
		args match {
			case JsString(s) =>
				val arg_l = s.split(" ").toList
				val l = arg_l.map { s =>
					val i = s.indexOf("=")
					if (i > 0) (Some(s.substring(0, i)), JsString(s.substring(i + 1)))
					else (None, JsString(s))
				}
				RsSuccess(l)
			case JsNull =>
				RsSuccess(Nil)
			case n: JsNumber =>
				RsSuccess(List((None, n)))
			case JsArray(arg_l) =>
				val l = arg_l.map(jsval => (None, jsval))
				RsSuccess(l)
			case JsObject(arg_m) =>
				val l = arg_m.toList.map(pair => (Some(pair._1), pair._2))
				RsSuccess(l)
			case _ =>
				RsError("invalid argument list")
		}
	}
	
	private def jsvalToString(argname: String, jsval: JsValue): RsResult[String] = {
		jsval match {
			case JsString(s) => RsSuccess(s)
			case _ => RsSuccess(jsval.toString)
		}
	}
	
	private def jsvalToEntity[A <: Entity : Manifest](argname: String, jsval: JsValue): RsResult[A] = {
		jsval match {
			case JsString(ref) => eb.getEntityAs[A](ref)
			case _ => RsError(s"`$argname`: expected reference to entity")
		}
	}
	
	private def parseArgList(
		argSpec_l: List[(String, Boolean, (String, JsValue) => RsResult[Object])],
		nameVal_l: List[(Option[String], JsValue)]
	): RsResult[List[Object]] = {
		
		def doit(
			spec_l: List[(String, Boolean, (String, JsValue) => RsResult[Object])],
			jsval_l: List[JsValue],
			nameToVal_m: Map[String, JsValue],
			acc_r: List[Object]
		): RsResult[List[Object]] = {
			spec_l match {
				case Nil => RsSuccess(acc_r.reverse)
				case spec :: spec_l_~ =>
					val (name, required, fn) = spec
					// Check whether named parameter is provided
					nameToVal_m.get(name) match {
						case Some(jsval) =>
							val nameToVal_m_~ = nameToVal_m - name
							fn(name, jsval).flatMap(o => doit(spec_l_~, jsval_l, nameToVal_m_~, (if (required) o else Some(o)) :: acc_r))
						case None =>
							jsval_l match {
								// Use unnamed parameter
								case jsval :: jsval_l_~ =>
									fn(name, jsval).flatMap(o => doit(spec_l_~, jsval_l_~, nameToVal_m, (if (required) o else Some(o)) :: acc_r))
								// Else parameter value is blank
								case Nil =>
									if (required) RsError(s"missing argument for `$name`")
									else doit(spec_l_~, jsval_l, nameToVal_m, None :: acc_r)
							}
					}
			}
		}

		val jsval_l = nameVal_l.collect({case (None, jsval) => jsval})
		val nameToVal2_l: List[(String, JsValue)] = nameVal_l.collect({case (Some(name), jsval) => (name, jsval)})
		val nameToVals_m: Map[String, List[(String, JsValue)]] = nameToVal2_l.groupBy(_._1)
		val nameToVals_l: List[(String, List[JsValue])] = nameToVals_m.toList.map(pair => pair._1 -> pair._2.map(_._2))
		
		for {
			nameToVal3_l <- RsResult.mapAll(nameToVals_l){ pair =>
				val (name, jsval_l) = pair
				jsval_l match {
					case Nil => RsError(s"missing value for argument `$name`") 
					case jsval :: Nil => RsSuccess((name, jsval))
					case _ => RsError(s"too many values supplied for argument `$name`: ${jsval_l}")
				}
			}
			nameToVal_m = nameToVal3_l.toMap
			l <- doit(argSpec_l, jsval_l, nameToVal_m, Nil)
		} yield l
	}
	
	private def loadJsonProtocol_ProtocolCommand_getArgList(
		name_l: List[String],
		nameVal_l: List[(Option[String], JsValue)]
	): RsResult[List[Option[JsValue]]] = {
		
		def doit(
			name_l: List[String],
			jsval_l: List[JsValue],
			nameToVal_m: Map[String, JsValue],
			acc_r: List[Option[JsValue]]
		): RsResult[List[Option[JsValue]]] = {
			name_l match {
				case Nil => RsSuccess(acc_r.reverse)
				case name :: name_l_~ =>
					// Check whether named parameter is provided
					nameToVal_m.get(name) match {
						case Some(jsval) =>
							val nameToVal_m_~ = nameToVal_m - name
							doit(name_l_~, jsval_l, nameToVal_m_~, Some(jsval) :: acc_r)
						case None =>
							jsval_l match {
								// Use unnamed parameter
								case jsval :: jsval_l_~ =>
									doit(name_l_~, jsval_l_~, nameToVal_m, Some(jsval) :: acc_r)
								// Else parameter value is blank
								case Nil =>
									doit(name_l_~, jsval_l, nameToVal_m, None :: acc_r)
							}
					}
			}
		}

		val nameToIndex_m = name_l.zipWithIndex.toMap
		// TODO: check for duplicate names when arguments are passed by name
		val jsval_l = nameVal_l.collect({case (None, jsval) => jsval})
		val nameToVal2_l: List[(String, JsValue)] = nameVal_l.collect({case (Some(name), jsval) => (name, jsval)})
		val nameToVals_m: Map[String, List[(String, JsValue)]] = nameToVal2_l.groupBy(_._1)
		val nameToVals_l: List[(String, List[JsValue])] = nameToVals_m.toList.map(pair => pair._1 -> pair._2.map(_._2))
		
		for {
			nameToVal3_l <- RsResult.mapAll(nameToVals_l){ pair =>
				val (name, jsval_l) = pair
				jsval_l match {
					case Nil => RsError(s"missing value for argument `$name`") 
					case jsval :: Nil => RsSuccess((name, jsval))
					case _ => RsError(s"too many values supplied for argument `$name`: ${jsval_l}")
				}
			}
			nameToVal_m = nameToVal3_l.toMap
			l <- doit(name_l, jsval_l, nameToVal_m, Nil)
		} yield l
	}
	
	def createPlan(): RqResult[(List[OperatorInfo], PartialPlan)] = {
		for {
			operatorInfo_l <- CallTree.getOperatorInfo(cs, tree, eb, state0.toImmutable)
			_ = println("planInfo_l:")
			_ = println(operatorInfo_l)
			_ = println("domain:")
			domain <- createDomain(cs, operatorInfo_l)
			_ = println(domain.toStripsText)
			// Save to file system
			_ = {
				val path = new File("log-domain.lisp").getPath
				roboliq.utils.FileUtils.writeToFile(path, domain.toStripsText)
			}
			
			problem <- createProblem(operatorInfo_l, domain)
			_ = println(problem.toStripsText)
			// Save to file system
			_ = {
				val path = new File("log-problem.lisp").getPath
				roboliq.utils.FileUtils.writeToFile(path, problem.toStripsText)
			}

			plan0 = PartialPlan.fromProblem(problem, Some(getPotentialNewProviders(operatorInfo_l.size)))
			// Save to file system
			_ = {
				val path = new File("log-plan0.dot").getPath
				roboliq.utils.FileUtils.writeToFile(path, plan0.toDot(showInitialState=true))
			}

			operator_l <- RsResult.mapAll(operatorInfo_l)(operatorInfo => {
				println("operatorInfo: "+operatorInfo)
				for {
					handler <- RsResult.from(cs.nameToOperatorHandler_m.get(operatorInfo.operatorName), s"createPlan: Unknown operator `${operatorInfo.operatorName}`")
				} yield {
					val domainOperator = handler.getDomainOperator
					domainOperator.bind(operatorInfo.operatorBinding_m)
				}
			})
			plan1 <- plan0.addActionSequence(operator_l).asRs
			// Save to file system
			_ = {
				val path = new File("log-plan1.dot").getPath
				roboliq.utils.FileUtils.writeToFile(path, plan1.toDot(showInitialState=true))
			}
		} yield {
			(operatorInfo_l, plan1)
		}
	}
	
	// 'userActionCount' was used to let user actions have different providers, but it's not used currently
	private def getPotentialNewProviders(userActionCount: Int)(plan: PartialPlan, consumer_i: Int): List[strips.Operator] = {
		val op0_l = plan.problem.domain.operator_l
		val allow_l = Set(
			"carousel.close-mario__Centrifuge",
			"carousel.openSite-CENTRIFUGE_1",
			"carousel.openSite-CENTRIFUGE_2",
			"carousel.openSite-CENTRIFUGE_3",
			"carousel.openSite-CENTRIFUGE_4",
			"closeDeviceSite",
			"openDeviceSite"
		)
		val consumer = plan.action_l(consumer_i)
		val op_l = op0_l.filter { op =>
			// These operators are always allowed
			val always = allow_l.contains(op.name)
			// 'transportLabware' is allowed if the consumer is not a 'transportLabware' action
			val transportOk = (op.name == "transportLabware") && (consumer.name != "transportLabware" && consumer.name != "evoware.transportLabware" && consumer.name != "getLabwareLocation")
			always || transportOk
		}
		/*if (consumer_i > 37 && consumer.name == "transportLabware") {
			println(s"getPotentialNewProviders($userActionCount)(${consumer_i}): "+op_l.map(_.name))
			1/0
		}*/
		op_l
	}
	
	def createDomain(cs: CommandSet, operatorInfo_l: List[OperatorInfo]): RqResult[strips.Domain] = {
		RsResult.prependError("createDomain:") {
			val name_l = (cs.nameToAutoOperator_l ++ operatorInfo_l.map(_.operatorName)).distinct.sorted
			for {
				operatorHandler_l <- RsResult.mapAll(name_l)(cs.getOperatorHandler)
			} yield {
				val operator_l = operatorHandler_l.map(_.getDomainOperator)

				// FIXME: HACK: need to programmatically figure out the parent classes of type -- this is here as a hack 
				val type0_m = Map(
					"centrifuge" -> "device",
					"peeler" -> "device",
					"pipetter" -> "device",
					"reader" -> "device",
					"sealer" -> "device",
					"shaker" -> "device",
					"thermocycler" -> "device",
					"transporter" -> "device"
				)
				// Get types used, and have them all inherit from 'any'
				val type1_m = operator_l.flatMap(_.paramTyp_l.map(_ -> "any")).toMap
				// The type0 types take precedence
				val type_m = type1_m ++ type0_m
				
				strips.Domain(
					type_m = type_m,
					constantToType_l = Nil,
					predicate_l = List[strips.Signature](
						strips.Signature("agent-has-device", "?agent" -> "agent", "?device" -> "device"),
						strips.Signature("device-can-site", "?device" -> "device", "?site" -> "site"),
						strips.Signature("location", "?labware" -> "labware", "?site" -> "site"),
						strips.Signature("model", "?labware" -> "labware", "?model" -> "model"),
						strips.Signature("stackable", "?sm" -> "siteModel", "?m" -> "model")
					),
					operator_l = operator_l
				)
			}
		}
	}


	def createProblem(planInfo_l: List[OperatorInfo], domain: strips.Domain): RqResult[strips.Problem] = {
		val typToObject_l: List[(String, String)] = eb.createProblemObjects.map(_.swap) ++ planInfo_l.flatMap(_.problemObjectToTyp_l).map(_.swap)
		
		val state0 = strips.State(Set[strips.Atom]() ++ planInfo_l.flatMap(_.problemState_l) ++ eb.createProblemState.map(rel => strips.Atom(rel.name, rel.args)))
		
		RqSuccess(strips.Problem(
			domain = domain,
			typToObject_l = typToObject_l,
			state0 = state0,
			goals = strips.Literals.empty
		))
	}

}