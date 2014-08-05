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
import aiplan.strips2.Strips
import aiplan.strips2.Unique
import roboliq.plan.CommandSet
import roboliq.plan.OperatorInfo
import aiplan.strips2.PartialPlan
import roboliq.commands.OperatorHandler_TransportLabware
import roboliq.plan.ActionHandler
import roboliq.commands._
import roboliq.plan.OperatorHandler
import roboliq.commands.ShakePlateOperatorHandler
import roboliq.commands.ShakePlateActionHandler
import roboliq.evoware.translator.EvowareInfiniteM200InstructionHandler

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
			new CloseDeviceSiteActionHandler,
			new DistributeActionHandler,
			new OpenDeviceSiteActionHandler,
			new PipetteActionHandler,
			new ShakePlateActionHandler,
			new TitrateActionHandler
		)
		val operatorHandler_l = List[OperatorHandler](
			new CloseDeviceSiteOperatorHandler,
			new DistributeOperatorHandler(1),
			new DistributeOperatorHandler(2),
			new DistributeOperatorHandler(3),
			new DistributeOperatorHandler(4),
			new OpenDeviceSiteOperatorHandler,
			new PipetteOperatorHandler(1),
			new PipetteOperatorHandler(2),
			new PipetteOperatorHandler(3),
			new PipetteOperatorHandler(4),
			new ShakePlateOperatorHandler,
			new TitrateOperatorHandler(1),
			new TitrateOperatorHandler(2),
			new TitrateOperatorHandler(3),
			new TitrateOperatorHandler(4),
			new OperatorHandler_TransportLabware
		)
		val autoHandler_l = List("transportLabware")
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
	
	def loadConfigBean(configBean: ConfigBean, table_l: List[String]): RsResult[Unit] = {
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
		
		// Aliases
		if (configBean.aliases != null) {
			for ((key, value) <- configBean.aliases.toMap) {
				eb.addAlias(key, value)
			}
		}
		
		// Logic
		if (configBean.logic != null) {
			for (s <- configBean.logic.toList) {
				val l = s.split(" ").toList
				eb.addRel(Rel(l.head, l.tail))
			}
		}

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
		
		if (configBean.evowareAgents == null) {
			RsSuccess(())
		}
		else {
			RsResult.mapAll(configBean.evowareAgents.toList)(pair => {
				val (name, agent) = pair
				val tableNameDefault = s"${name}_default"
				val tableSetup_m = agent.tableSetups.toMap.map(pair => s"${name}_${pair._1}" -> pair._2)
				for {
					// Load carrier file
					evowarePath <- RsResult(agent.evowareDir, "evowareDir must be set")
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
								RsError(s"No table specified for agent `$name`")
						case s :: Nil => RsSuccess(s)
						case l => RsError(s"Agent `$name` can only be assigned one table, but multiple tables were specified: $l")
					}
					tableSetup = tableSetup_m(tableName)
					// Load table file
					tableFile <- RsResult(tableSetup.tableFile, s"tableFile property must be set on tableSetup `$tableName`")
					tableData <- roboliq.evoware.parser.EvowareTableData.loadFile(carrierData, tableFile)
					_ <- loadEvoware(name, carrierData, tableData, agent, tableSetup)
				} yield ()
			}).map(_ => ())
		}
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
								val site = eb.getEntity(locationRef).get
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

	/*
	private def loadJsonProtocol_ProtocolCommand(
		cmd: String, nameVal_l: List[(Option[String], JsValue)],
		path0: PlanPath
	): RsResult[PlanPath] = {
		cmd match {
			case "log" =>
				for {
					arg_l <- loadJsonProtocol_ProtocolCommand_getArgList(List("text"), nameVal_l)
					task <- arg_l match {
						case List(Some(JsString(text))) =>
							val agentIdent = f"?a$nvar%04d"
							val textIdent = f"text$nvar%04d"
							idToObject(textIdent) = text
							val task = Task(Rel("!log", List(agentIdent, textIdent)), Nil)
							RsSuccess(task)
						case _ => RsError(s"bad arguments to `$cmd`")
					}
					path1 <- path0.add(task)
				} yield path1
			case "move" =>
				for {
					arg_l <- loadJsonProtocol_ProtocolCommand_getArgList(List("object", "destination"), nameVal_l)
					path1 <- arg_l match {
						case List(Some(JsString(objectRef)), Some(JsString(dstRef))) =>
							for {
								labware <- eb.getEntityAs[Labware](objectRef)
								dst <- eb.getEntityAs[Labware](dstRef)
								labwareIdent <- eb.getIdent(labware)
								dstIdent <- eb.getIdent(dst)
								task = Task(Rel("move-labware", List(labwareIdent, dstIdent)), Nil)
								path1 <- path0.add(task)
							} yield path1
						case _ => RsError(s"bad arguments to `$cmd`")
					}
				} yield path1
			case "peel" =>
				for {
					arg_l <- loadJsonProtocol_ProtocolCommand_getArgList(List("object"), nameVal_l)
					task <- arg_l match {
						case List(Some(JsString(objectRef))) =>
							for {
								labware <- eb.getEntityAs[Labware](objectRef)
								labwareIdent <- eb.getIdent(labware)
							} yield {
								val agentIdent = f"?a$nvar%04d"
								val deviceIdent = f"?d$nvar%04d"
								val specIdent = f"?spec$nvar%04d"
								val siteIdent = f"?s$nvar%04d"
								Task(Rel("peeler-run", List(agentIdent, deviceIdent, specIdent, labwareIdent, siteIdent)), Nil)
							}
						case _ => RsError(s"bad arguments to `$cmd`")
					}
					path1 <- path0.add(task)
				} yield path1
			case "prompt" =>
				for {
					arg_l <- loadJsonProtocol_ProtocolCommand_getArgList(List("text"), nameVal_l)
					task <- arg_l match {
						case List(Some(JsString(text))) =>
							val agentIdent = f"?a$nvar%04d"
							val textIdent = f"text$nvar%04d"
							idToObject(textIdent) = text
							val task = Task(Rel("!prompt", List(agentIdent, textIdent)), Nil)
							RsSuccess(task)
						case _ => RsError(s"bad arguments to `$cmd`")
					}
					path1 <- path0.add(task)
				} yield path1
			case "seal" =>
				for {
					arg_l <- loadJsonProtocol_ProtocolCommand_getArgList(List("object"), nameVal_l)
					task <- arg_l match {
						case List(Some(JsString(objectRef))) =>
							for {
								labware <- eb.getEntityAs[Labware](objectRef)
								labwareIdent <- eb.getIdent(labware)
							} yield {
								val agentIdent = f"?a$nvar%04d"
								val deviceIdent = f"?d$nvar%04d"
								val specIdent = f"?spec$nvar%04d"
								val siteIdent = f"?s$nvar%04d"
								Task(Rel("sealer-run", List(agentIdent, deviceIdent, specIdent, labwareIdent, siteIdent)), Nil)
							}
						case _ => RsError(s"bad arguments to `$cmd`")
					}
					path1 <- path0.add(task)
				} yield path1
			case "shakePlate" =>
				println("shakePlate:")
				nameVal_l.foreach(println)
				for {
					cmd <- Converter.convCommandAs[ShakePlate](nameVal_l, eb, path0.state)
					specIdent <- eb.getIdent(cmd.program)
					labwareIdent <- eb.getIdent(cmd.plate)
					agentIdent = f"?a$nvar%04d"
					deviceIdent = f"?d$nvar%04d"
					siteIdent = f"?s$nvar%04d"
					task = Task(Rel("shaker-run", List(agentIdent, deviceIdent, specIdent, labwareIdent, siteIdent)), Nil)
					path1 <- path0.add(task)
				} yield path1
/*				for {
					arg_l <- loadJsonProtocol_ProtocolCommand_getArgList(List("object", "spec"), nameVal_l)
					task <- arg_l match {
						case List(Some(JsString(objectRef)), Some(JsString(specIdent))) =>
							for {
								labware <- eb.getEntityAs[Labware](objectRef)
								labwareIdent <- eb.getIdent(labware)
							} yield {
								val agentIdent = f"?a$nvar%04d"
								val deviceIdent = f"?d$nvar%04d"
								val siteIdent = f"?s$nvar%04d"
								Task(Rel("shaker-run", List(agentIdent, deviceIdent, specIdent, labwareIdent, siteIdent)), Nil)
							}
						case _ => RsError(s"bad arguments to `$cmd`")
					}
					path1 <- path0.add(task)
				} yield path1
*/
			case "thermocycle" =>
				val argSpec_l = List(
					("object", true, jsvalToEntity[Labware] _),
					("spec", true, jsvalToString _)
				)
				for {
					arg_l <- parseArgList(argSpec_l, nameVal_l)
					task <- arg_l match {
						case List(Some(labware: Labware), Some(specIdent: String)) =>
							for {
								labwareIdent <- eb.getIdent(labware)
							} yield {
								val agentIdent = f"?a$nvar%04d"
								val deviceIdent = f"?d$nvar%04d"
								val siteIdent = f"?s$nvar%04d"
								Task(Rel("thermocycle-plate", List(agentIdent, deviceIdent, specIdent, labwareIdent, siteIdent)), Nil)
							}
						case _ => RsError(s"bad arguments to `$cmd`")
					}
					path1 <- path0.add(task)
				} yield path1
			case "pipette" =>
				for {
					arg_l <- loadJsonProtocol_ProtocolCommand_getArgList(List("steps"), nameVal_l)
					path1 <- arg_l match {
						case List(Some(JsArray(step_l))) =>
							def handleSubCmds(
								pair_l: List[(String, List[(Option[String], JsValue)])],
								spec0_l: List[PipetteSpec],
								path0: PlanPath
							): RqResult[List[PipetteSpec]] = {
								pair_l match {
									case Nil => RqSuccess(spec0_l)
									case (cmd, nameToVal_l) :: rest =>
										for {
											spec_l <- cmd match {
												case "distribute" =>
													loadJsonProtocol_DistributeSub(nameToVal_l, path0.state)
												case "titrate" =>
													loadJsonProtocol_TitrateSub(nameToVal_l, path0.state)
												case "transfer" =>
													loadJsonProtocol_TransferSub(nameToVal_l, path0.state)
												case _ =>
													RsError("unrecognized pipette sub-command: $command")
											}
											task = pipetteSpecsToTask(spec_l)
											path1 <- path0.add(task)
											spec1_l <- handleSubCmds(rest, spec0_l ++ spec_l, path1)
										} yield spec1_l
								}
							}
							for {
								l0 <- RsResult.toResultOfList(step_l.map(loadJsonProtocol_Protocol_getCommand))
								pair_l = l0.flatten
								spec_l <- handleSubCmds(pair_l, Nil, path0)
								task = pipetteSpecsToTask(spec_l)
								path1 <- path0.add(task)
							} yield path1
						case _ => RsError(s"bad arguments to `$cmd`")
					}
				} yield path1
			case "distribute" =>
				loadJsonProtocol_Distribute(nameVal_l, path0)
			case "setReagents" =>
				loadJsonProtocol_SetReagents(nameVal_l, path0)
			case "titrate" =>
				loadJsonProtocol_Titrate(nameVal_l, path0)
			case "transfer" =>
				loadJsonProtocol_Transfer(nameVal_l, path0)
			case _ =>
				RsSuccess(path0)
		}
	}
	*/
	
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
	
	/*
	private def loadJsonProtocol_Distribute(
		nameToVal_l: List[(Option[String], JsValue)],
		path0: PlanPath
	): RsResult[PlanPath] = {
		for {
			spec_l <- loadJsonProtocol_DistributeSub(nameToVal_l, path0.state)
			task = pipetteSpecsToTask(spec_l)
			path1 <- path0.add(task)
		} yield path1
	}
	
	private def loadJsonProtocol_DistributeSub(
		nameToVal_l: List[(Option[String], JsValue)],
		state0: WorldState
	): RsResult[List[PipetteSpec]] = {
		for {
			cmd <- Converter.convCommandAs[commands.Distribute](nameToVal_l, eb, state0)
			tipModel_? <- cmd.tipModel_? match {
				case Some(key) => eb.getEntityAs[TipModel](key).map(Some(_))
				case _ => RsSuccess(None)
			}
		} yield {
			List(PipetteSpec(
				cmd.source,
				cmd.destination,
				List(cmd.volume),
				cmd.pipettePolicy_?,
				cmd.sterilize_?,
				cmd.sterilizeBefore_?,
				cmd.sterilizeBetween_?,
				cmd.sterilizeAfter_?,
				tipModel_?
			))
		}
	}
	private def loadJsonProtocol_Transfer(
		nameToVal_l: List[(Option[String], JsValue)],
		path0: PlanPath
	): RsResult[PlanPath] = {
		for {
			spec_l <- loadJsonProtocol_TransferSub(nameToVal_l, path0.state)
			task = pipetteSpecsToTask(spec_l)
			path1 <- path0.add(task)
		} yield path1
	}
	
	private def loadJsonProtocol_TransferSub(
		nameToVal_l: List[(Option[String], JsValue)],
		state0: WorldState
	): RsResult[List[PipetteSpec]] = {
		for {
			cmd <- Converter.convCommandAs[commands.Transfer](nameToVal_l, eb, state0)
			tipModel_? <- cmd.tipModel_? match {
				case Some(key) => eb.getEntityAs[TipModel](key).map(Some(_))
				case _ => RsSuccess(None)
			}
			_ <- RqResult.assert(cmd.source.l.size == cmd.destination.l.size, "Must specify an equal number of source and destination wells")
			_ <- RqResult.assert(cmd.volume.size == 1 || cmd.volume.size == cmd.destination.l.size, "Must specify a single volume or an equal number of volumes and destination wells")
		} yield {
			val l = cmd.source.l zip cmd.destination.l
			val src = PipetteSources(cmd.source.l.map(well => LiquidSource(List(well))))
			List(PipetteSpec(
				src,
				cmd.destination,
				cmd.volume,
				cmd.pipettePolicy_?,
				cmd.sterilize_?,
				cmd.sterilizeBefore_?,
				cmd.sterilizeBetween_?,
				cmd.sterilizeAfter_?,
				tipModel_?
			))
		}
	}
	
	private def loadJsonProtocol_SetReagents(
		nameToVal_l: List[(Option[String], JsValue)],
		path0: PlanPath
	): RsResult[PlanPath] = {
		val task_? = for {
			cmd <- Converter.convCommandAs[commands.SetReagents](nameToVal_l, eb, path0.state)
		} yield {
			val specIdent = f"spec$nvar%04d"
			idToObject(specIdent) = cmd
			Task(Rel(s"!nop", specIdent :: Nil), Nil)
		}
		task_?.flatMap(path0.add)
	}
	
	private def loadJsonProtocol_Titrate(
		nameToVal_l: List[(Option[String], JsValue)],
		path0: PlanPath
	): RsResult[PlanPath] = {
		for {
			cmd <- Converter.convCommandAs[commands.Titrate](nameToVal_l, eb, path0.state)
			spec_l <- new method.TitrateMethod(eb, path0.state, cmd).run()
			task = pipetteSpecsToTask(spec_l)
			path1 <- path0.add(task)
		} yield path1
	}
	
	private def loadJsonProtocol_TitrateSub(
		nameToVal_l: List[(Option[String], JsValue)],
		state0: WorldState
	): RsResult[List[PipetteSpec]] = {
		for {
			cmd <- Converter.convCommandAs[commands.Titrate](nameToVal_l, eb, state0)
			spec_l <- new method.TitrateMethod(eb, state0, cmd).run()
		} yield spec_l
	}
	
	private def pipetteSpecsToTask(spec_l: List[PipetteSpec]): Task = {
		val labwareIdent_l = spec_l.flatMap(spec => (spec.sources.sources.flatMap(_.l) ++ spec.destinations.l).map(_.labwareName)).distinct
		val agentIdent = f"?a$nvar%04d"
		val deviceIdent = f"?d$nvar%04d"
		val n = labwareIdent_l.size
		val specIdent = f"spec$nvar%04d"
		idToObject(specIdent) = PipetteSpecList(spec_l)
		val event_l = spec_l.flatMap(_.getWellEvents)
		Task(Rel(s"distribute$n", agentIdent :: deviceIdent :: specIdent :: labwareIdent_l), event_l)
	}
	
	private def x(fields: Map[String, JsValue], id: String): String =
		x(fields, id, f"?x$nvar%04d")
	
	private def x(fields: Map[String, JsValue], id: String, default: => String): String = {
		fields.get(id) match {
			case Some(JsString(value)) => value
			case _ => default
		}
	}
	*/
	
	/**
	 * Challenges when reading in Evoware configuration files:
	 * 
	 * There are lots of labware models we don't want to use, so we need to filter them out somehow.
	 * A carrier may have mutually exclusive sites, so in such cases, we need to filter out the ones that can't be used on the current table.
	 * When a table has tube labware on which cannot be moved by the RoMa, that labware should be treated as a site for tubes.
	 * What to do with other labware on the table definition?  One thing we should probably do is add it to this list of labware models we're interested in.
	 * We want to use site models, but these are not declared in Evoware, so we'll need to extract them indirectly. 
	 */
	def loadEvoware(
		agentIdent: String,
		carrierData: roboliq.evoware.parser.EvowareCarrierData,
		tableData: roboliq.evoware.parser.EvowareTableData,
		agentBean: EvowareAgentBean,
		tableSetupBean: TableSetupBean
	): RsResult[Unit] = {
		import roboliq.entities._
		
		val agent = Agent(gid, Some(agentIdent))
		eb.addAgent(agent, agentIdent)
		
		val identToAgentObject = new HashMap[String, Object]
		agentToIdentToInternalObject(agentIdent) = identToAgentObject

		// FIXME: this should not be hard-coded -- some robots have no pipetters, some have more than one...
		val pipetterIdent = agentIdent+"__pipetter1"
		val pipetter = new Pipetter(gid, Some(agentIdent+" LiHa"))
		eb.addDevice(agent, pipetter, pipetterIdent)

		val labwareNamesOfInterest_l = new HashSet[String]
		
		val labwareModel_l = agentBean.labwareModels.toList
		val labwareModel_m = labwareModel_l.map(x => x.name -> x).toMap

		def loadAgentBean(): RsResult[Unit] = {
			// Labware to be used
			if (agentBean.labwareModels != null) {
				labwareNamesOfInterest_l ++= labwareModel_l.map(_.evowareName)
			}
			
			// Tip models
			val tipModel_l = new ArrayBuffer[TipModel]
			if (agentBean.tipModels != null) {
				for ((id, tipModelBean) <- agentBean.tipModels.toMap) {
					val tipModel = TipModel(id, None, None, LiquidVolume.ul(BigDecimal(tipModelBean.max)), LiquidVolume.ul(BigDecimal(tipModelBean.min)), Map())
					tipModel_l += tipModel
					eb.addEntityWithoutIdent(tipModel)
				}
			}
			
			// Tips
			val tip_l = new ArrayBuffer[Tip]
			val tipBean_l = if (agentBean.tips != null) agentBean.tips.toList else Nil
			val x = for {
				_ <- RsResult.toResultOfList(tipBean_l.zipWithIndex.map { pair =>
					val (tipBean, index_) = pair
					val row: Int = if (tipBean.row == 0) index_ else tipBean.row
					// HACK: use the row as index instead, need to figure out a more general solution,
					//  such as specifying that a tip cannot be used -- ellis, 2014-02-06
					val index = row - 1
					val col = 0
					for {
						permanentTipModel_? <- if (tipBean.permanentModel == null) RsSuccess(None) else eb.getEntityAs[TipModel](tipBean.permanentModel).map(Option(_))
						tipModel2_l <- (permanentTipModel_?, tipBean.models) match {
							case (Some(tipModel), _) => RsSuccess(List(tipModel))
							case (_, null) => RsSuccess(tipModel_l.toList)
							case _ => RsResult.toResultOfList(tipBean.models.toList.map(eb.getEntityAs[TipModel](_)))
						}
					} yield {
						val tip = Tip("tip"+(index + 1), None, None, index, row, col, permanentTipModel_?)
						tip_l += tip
						eb.tipToTipModels_m(tip) = tipModel2_l.toList
					}
				})
			} yield ()
			
			eb.pipetterToTips_m(pipetter) = tip_l.toList
			x
		}
		val x = loadAgentBean()
		if (x.isError)
			return x
		
		// Add labware on the table definition to the list of labware we're interested in
		labwareNamesOfInterest_l ++= tableData.mapSiteToLabwareModel.values.map(_.sName)
		logger.debug("labwareNamesOfInterest_l: "+labwareNamesOfInterest_l)

		// Create PlateModels
		// Start with gathering list of all available labware models whose names are either in the config or table template, as well as the "portrait" variants
		val labwareModelEs = carrierData.models.collect({case m: roboliq.evoware.parser.EvowareLabwareModel if labwareNamesOfInterest_l.contains(m.sName) || labwareNamesOfInterest_l.contains(m.sName.replace(" portrait", "")) => m})
		val idToModel_m = new HashMap[String, LabwareModel]
		val evowareNameToLabwareModel_m = labwareModel_l.map(x => x.evowareName -> x).toMap
		//println("labwareModelEvowareNameToName_m: "+labwareModelEvowareNameToName_m)
		for (mE <- labwareModelEs) {
			//println("mE.sName: "+mE.sName)
			//if (mE.sName.contains("Plate") || mE.sName.contains("96") || mE.sName.contains("Trough")) {
			evowareNameToLabwareModel_m.get(mE.sName) match {
				case Some(model) =>
					val m = PlateModel(model.name, Option(model.label), Some(mE.sName), mE.nRows, mE.nCols, LiquidVolume.ul(mE.ul))
					idToModel_m(mE.sName) = m
					eb.addModel(m, model.name)
					// All models can be offsite
					eb.addStackable(offsiteModel, m)
					// The user arm can handle all models
					eb.addDeviceModel(userArm, m)
					//eb.addRel(Rel("transporter-can", List(eb.names(userArm), eb.names(m), "nil")))
					identToAgentObject(model.name.toLowerCase) = mE
				case _ =>
			}
		}
		
		//
		// Create Sites
		//
		
		val siteEsToSiteModel_m = new HashMap[List[(Int, Int)], SiteModel]
		val siteIdToSite_m = new HashMap[(Int, Int), Site]
		val carriersSeen_l = new HashSet[Int]
		
		def addSite(carrierE: roboliq.evoware.parser.Carrier, site_i: Int, description: String) {
			val grid_i = tableData.mapCarrierToGrid(carrierE)
			// TODO: should adapt CarrierSite to require grid_i as a parameter 
			val siteE = roboliq.evoware.parser.CarrierSite(carrierE, site_i)
			val siteId = (carrierE.id, site_i)
			findSiteIdent(tableSetupBean, carrierE.sName, grid_i, site_i + 1) match {
				case Some(siteIdent) =>
					val site = Site(gid, Some(siteIdent), Some(description))
					siteIdToSite_m(siteId) = site
					identToAgentObject(siteIdent.toLowerCase) = siteE
					eb.addSite(site, siteIdent)
					// Make site pipetter-accessible if it's listed in the table setup's `pipetterSites`
					if (tableSetupBean.pipetterSites.contains(siteIdent)) {
						eb.addRel(Rel("device-can-site", List(pipetterIdent, siteIdent)))
					}
					// Make site user-accessible if it's listed in the table setup's `userSites`
					if (tableSetupBean.userSites.contains(siteIdent)) {
						eb.addRel(Rel("transporter-can", List("userArm", siteIdent, "userArmSpec")))
					}
				case None =>
			}
		}
		
		// Create Hotel Sites
		for (o <- tableData.lHotelObject) {
			val carrierE = o.parent
			carriersSeen_l += carrierE.id
			//println("carrier: "+carrierE)
			for (site_i <- 0 until carrierE.nSites) {
				addSite(carrierE, site_i, s"${agentIdent} hotel ${carrierE.sName} site ${site_i+1}")
			}
		}
		
		// Create Device Sites
		for (o <- tableData.lExternalObject if !carriersSeen_l.contains(o.carrier.id)) {
			val carrierE = o.carrier
			carriersSeen_l += carrierE.id
			for (site_i <- 0 until carrierE.nSites) {
				addSite(carrierE, site_i, s"${agentIdent} device ${carrierE.sName} site ${site_i+1}")
			}
		}
		
		// Create on-bench Sites for Plates
		for ((carrierE, grid_i) <- tableData.mapCarrierToGrid if !carriersSeen_l.contains(carrierE.id)) {
			for (site_i <- 0 until carrierE.nSites) {
				addSite(carrierE, site_i, s"${agentIdent} bench ${carrierE.sName} site ${site_i+1}")
			}
		}
		
		// TODO: For tubes, create their on-bench Sites and SiteModels
		// TODO: Let userArm handle tube models
		// TODO: Let userArm access all sites that the robot arms can't
		
		// Create SiteModels for for sites which hold Plates
		val siteIdToModels_m = new HashMap[(Int, Int), collection.mutable.Set[LabwareModel]] with MultiMap[(Int, Int), LabwareModel]
		
		{
			// First gather map of all relevant labware models that can be placed on each site 
			for (mE <- labwareModelEs) {
				//println("mE: "+mE+" "+idToModel_m.get(mE.sName.replace(" portrait", "")))
				idToModel_m.get(mE.sName.replace(" portrait", "")) match {
					case Some(m) =>
						for (siteId <- mE.sites if siteIdToSite_m.contains(siteId)) {
							val site = siteIdToSite_m(siteId)
							siteIdToModels_m.addBinding(siteId, m)
						}
					case None =>
				}
			}
			// Find all unique sets of labware models
			val unique = siteIdToModels_m.values.toSet
			val modelsToSiteModel_m = new HashMap[collection.mutable.Set[LabwareModel], SiteModel]
			var i = 1
			for (l <- unique) {
				val sm = SiteModel(l.toString)
				modelsToSiteModel_m(l) = sm
				eb.addModel(sm, f"sm${i}")
				eb.addStackables(sm, l.toList)
				i += 1
			}

			// Assign SiteModels to Sites
			for ((siteId, l) <- siteIdToModels_m) {
				val site = siteIdToSite_m(siteId)
				val sm = modelsToSiteModel_m(l)
				eb.setModel(site, sm)
			}
		}
		
		// Create transporters
		val roma_m = new HashMap[Int, Transporter]()
		
		{
			// List of RoMa indexes
			val roma_li = carrierData.mapCarrierToVectors.values.flatMap(_.map(_.iRoma)).toSet
			// Add transporter device for each RoMa
			for (roma_i <- roma_li) {
				val ident = s"${agentIdent}__transporter${roma_i + 1}"
				val roma = Transporter(gid)
				identToAgentObject(ident.toLowerCase) = roma_i.asInstanceOf[Integer]
				roma_m(roma_i) = roma
				eb.addDevice(agent, roma, ident)
			}
		}
		
		// Create transporter specs
		// Map vector class to transporter spec
		val transporterSpec_m = new HashMap[String, TransporterSpec]()
		
		{
			val vectorClass_l: List[String] = carrierData.mapCarrierToVectors.toList.flatMap(_._2).map(_.sClass).toSet.toList.sorted
			var vector_i = 0
			for (vectorClass <- vectorClass_l) {
				val spec = TransporterSpec(gid, Some(s"${agentIdent} ${vectorClass}"))
				val ident = s"${agentIdent}__transporterSpec${vector_i}"
				identToAgentObject(ident.toLowerCase) = vectorClass
				//println(ident.toLowerCase)
				transporterSpec_m(vectorClass) = spec
				for (roma <- roma_m.values) {
					vector_i += 1
					eb.addDeviceSpec(roma, spec, ident)
				}
			}
		}
		
		val test_m = new HashMap[(String, String, String), List[Site]]
		// Find which sites the transporters can access
		for ((carrierE, vector_l) <- carrierData.mapCarrierToVectors) {
			for (site_i <- 0 until carrierE.nSites) {
				val siteId = (carrierE.id, site_i)
				siteIdToSite_m.get(siteId).foreach { site =>
					for (vector <- vector_l) {
						val transporter = roma_m(vector.iRoma)
						val deviceIdent = eb.entityToIdent_m(transporter)
						val spec = transporterSpec_m(vector.sClass)
						val key = (agentIdent, deviceIdent, vector.sClass)
						test_m(key) = site :: test_m.getOrElse(key, Nil)
						eb.addRel(Rel("transporter-can", List(deviceIdent, eb.entityToIdent_m(site), eb.entityToIdent_m(spec))))
					}
				}
			}
		}

		val graph = {
			// Add user-accessible sites into the graph
			val offsite = eb.getEntityAs[Site]("offsite").getOrElse(null)
			test_m(("user", "", "")) = offsite :: RqResult.flatten(tableSetupBean.userSites.toList.map(eb.getEntityAs[Site]))
			// Populate graph from entries in test_m
			import scalax.collection.Graph // or scalax.collection.mutable.Graph
			import scalax.collection.GraphPredef._, scalax.collection.GraphEdge._
			import scalax.collection.edge.LHyperEdge
			val edge_l = test_m.toList.flatMap(pair => {
				val (key, site_l) = pair
				site_l.combinations(2).map(l => LkUnDiEdge(l(0), l(1))(key))
			})
			Graph[Site, LkUnDiEdge](edge_l : _*)
		}
		// FIXME: should append to transportGraph (not replace it) so that we can have multiple evoware agents
		eb.transportGraph = graph
		//println("graph: "+graph.size)
		//graph.take(5).foreach(println)
		//graph.foreach(println)
		
		def getDeviceSitesAndModels(
			carrierE: roboliq.evoware.parser.Carrier
		): List[(Site, Set[LabwareModel])] = {
			//val l = new ArrayBuffer[(Site, List[LabwareModel])]
			for {
				site_i <- (0 until carrierE.nSites).toList
				siteId = (carrierE.id, site_i)
				site <- siteIdToSite_m.get(siteId) match {
					case Some(site: Site) => List(site)
					case None => Nil
				}
			} yield (site, siteIdToModels_m.get(siteId).map(_.toSet).getOrElse(Set()))
		}
		
		/*def addDeviceSitesAndModels(
			device: Device,
			carrierE: roboliq.evoware.parser.Carrier
		) {
			// Add device sites
			getDeviceSitesAndModels(carrierE).foreach { case (site, model_l) =>
				eb.addDeviceSite(device, site)
				model_l.foreach(m => eb.addDeviceModel(device, m))
			}
		}*/
		
		def addDeviceOnly(
			device: Device,
			deviceIdent: String,
			carrierE: roboliq.evoware.parser.Carrier
		) {
			// Add device
			eb.addDevice(agent, device, deviceIdent)
			identToAgentObject(deviceIdent) = carrierE
		}
		
		def addDeviceAndSitesAndModels(
			device: Device,
			deviceIdent: String,
			carrierE: roboliq.evoware.parser.Carrier
		) {
			addDeviceOnly(device, deviceIdent, carrierE)
	
			// Add device sites
			getDeviceSitesAndModels(carrierE).foreach { case (site, model_l) =>
				eb.addDeviceSite(device, site)
				model_l.foreach(m => eb.addDeviceModel(device, m))
			}
		}
		
		def addDevice(
			typeName: String,
			deviceName: String,
			carrierE: roboliq.evoware.parser.Carrier
		): Device = {
			// Add device
			val device = new Device { val key = gid; val label = Some(carrierE.sName); val description = None; val typeNames = List(typeName) }
			addDeviceAndSitesAndModels(device, deviceName, carrierE)
			device
		}
		
		def addPeeler(
			deviceName: String,
			carrierE: roboliq.evoware.parser.Carrier
		): Device = {
			val device = new Peeler(gid, Some(carrierE.sName))
			addDeviceAndSitesAndModels(device, deviceName, carrierE)
			device
		}
		
		def addSealer(
			deviceName: String,
			carrierE: roboliq.evoware.parser.Carrier
		): Device = {
			val device = new Sealer(gid, Some(carrierE.sName))
			addDeviceAndSitesAndModels(device, deviceName, carrierE)
			device
		}
		
		def addShaker(
			deviceName: String,
			carrierE: roboliq.evoware.parser.Carrier
		): Device = {
			val device = new Shaker(gid, Some(carrierE.sName))
			addDeviceAndSitesAndModels(device, deviceName, carrierE)
			device
		}
		
		def createDeviceIdent(carrierE: roboliq.evoware.parser.Carrier): String = {
			agentIdent + "__" ++ carrierE.sName.map(c => if (c.isLetterOrDigit) c else '_')
		}
		
		//println("Carriers: " + tableData.mapCarrierToGrid.keys.mkString("\n"))
		for ((carrierE, iGrid) <- tableData.mapCarrierToGrid) {
			carrierE.partNo_?.getOrElse(carrierE.sName) match {
				// Infinite M200
				case "Tecan part no. 30016056 or 30029757" =>
					val deviceIdent = createDeviceIdent(carrierE)
					val handler = new EvowareInfiniteM200InstructionHandler(carrierE)
					val device = new Reader(gid, Some(carrierE.sName))
					
					eb.addDevice(agent, device, deviceIdent)
					// Bind deviceIdent to the handler instead of to the carrier
					identToAgentObject(deviceIdent) = handler
					// Add device sites
					getDeviceSitesAndModels(carrierE).foreach { case (site, model_l) =>
						val siteIdent = eb.getIdent(site).toOption.get
						eb.addDeviceSite(device, site)
						model_l.foreach(m => eb.addDeviceModel(device, m))
						eb.addRel(Rel("device-can-open-site", List(deviceIdent, siteIdent)))
					}

				case "MP 2Pos H+P Shake" =>
					val deviceIdent = createDeviceIdent(carrierE)
					// REFACTOR: duplicates addShaker(), because for this device, only the second site can actually be used for shaking
					val device = new Shaker(gid, Some(carrierE.sName))
					// Add device
					eb.addDevice(agent, device, deviceIdent)
					identToAgentObject(deviceIdent) = carrierE
					// Add device sites
					// HACK: only use last site for shaking, this is truly a bad hack!  Things like this should be performed via configuration overrides.
					for (site_i <- List(carrierE.nSites - 1)) {
						val siteId = (carrierE.id, site_i)
						siteIdToSite_m.get(siteId) match {
							case Some(site) =>
								siteIdToModels_m.get(siteId).map { model_l =>
									eb.addDeviceSite(device, site)
									model_l.foreach(m => eb.addDeviceModel(device, m))
								}
							case None =>
						}
					}

				case "RoboPeel" =>
					val deviceIdent = createDeviceIdent(carrierE)
					val device = addPeeler(deviceIdent, carrierE)
					// Add user-defined specs for this device
					for ((deviceIdent2, plateModelId, specIdent) <- deviceToModelToSpec_l if deviceIdent2 == deviceIdent) {
						// Get or create the sealer spec for specIdent
						val spec: PeelerSpec = eb.getEntity(specIdent) match {
							case Some(spec) => spec.asInstanceOf[PeelerSpec]
							case None =>
								// Store the evoware string for this spec
								val internal = specToString_l.find(_._1 == specIdent).get._2
								identToAgentObject(specIdent.toLowerCase) = internal
								PeelerSpec(gid, None, Some(internal))
						}
						// Register the spec
						eb.addDeviceSpec(device, spec, specIdent)
						// Let entity base know that that the spec can be used for the plate model
						val plateModel = idToModel_m(plateModelId)
						val plateModelIdent = eb.getIdent(plateModel).toOption.get
						eb.addRel(Rel("device-spec-can-model", List(deviceIdent, specIdent, plateModelIdent)))
					}
				case "RoboSeal" =>
					val deviceIdent = createDeviceIdent(carrierE)
					val device = addSealer(deviceIdent, carrierE)
					// Add user-defined specs for this device
					for ((deviceIdent2, plateModelId, specIdent) <- deviceToModelToSpec_l if deviceIdent2 == deviceIdent) {
						// Get or create the sealer spec for specIdent
						val spec: SealerSpec = eb.getEntity(specIdent) match {
							case Some(spec) => spec.asInstanceOf[SealerSpec]
							case None =>
								// Store the evoware string for this spec
								val internal = specToString_l.find(_._1 == specIdent).get._2
								identToAgentObject(specIdent.toLowerCase) = internal
								SealerSpec(gid, None, Some(internal))
						}
						// Register the spec
						eb.addDeviceSpec(device, spec, specIdent)
						// Let entity base know that that the spec can be used for the plate model
						val plateModel = idToModel_m(plateModelId)
						val plateModelIdent = eb.getIdent(plateModel).toOption.get
						eb.addRel(Rel("device-spec-can-model", List(deviceIdent, specIdent, plateModelIdent)))
					}
					
				// Te-Shake 2Pos
				case "Tecan part no. 10760722 with 10760725" =>
					val deviceIdent = createDeviceIdent(carrierE)
					val device = new Shaker(gid, Some(carrierE.sName))
					addDeviceAndSitesAndModels(device, deviceIdent, carrierE)
					// Add user-defined specs for this device
					for ((deviceIdent2, specIdent) <- deviceToSpec_l if deviceIdent2 == deviceIdent) {
						// Get or create the spec for specIdent
						val spec: ShakerSpec = eb.getEntity(specIdent) match {
							case Some(spec) => spec.asInstanceOf[ShakerSpec]
							case None =>
								// Store the evoware string for this spec
								val internal = specToString_l.find(_._1 == specIdent).get._2
								identToAgentObject(specIdent.toLowerCase) = internal
								ShakerSpec(gid, None, Some(internal))
						}
						// Register the spec
						eb.addDeviceSpec(device, spec, specIdent)
					}
					
				case "TRobot1" =>
					val deviceIdent = createDeviceIdent(carrierE)
					val device = new Thermocycler(gid, Some(carrierE.sName))
					addDeviceAndSitesAndModels(device, deviceIdent, carrierE)
					// Add user-defined specs for this device
					for ((deviceIdent2, specIdent) <- deviceToSpec_l if deviceIdent2 == deviceIdent) {
						// Get or create the spec for specIdent
						val spec: ThermocyclerSpec = eb.getEntity(specIdent) match {
							case Some(spec) => spec.asInstanceOf[ThermocyclerSpec]
							case None =>
								// Store the evoware string for this spec
								val internal = specToString_l.find(_._1 == specIdent).get._2
								identToAgentObject(specIdent.toLowerCase) = internal
								ThermocyclerSpec(gid, None, Some(internal))
						}
						// Register the spec
						eb.addDeviceSpec(device, spec, specIdent)
					}
				case _ =>
			}
		}
		
		val configData = EvowareConfigData(Map("G009S1" -> "pipette2hi"))
		val config = new EvowareConfig(carrierData, tableData, configData)
		val scriptBuilder = new EvowareClientScriptBuilder(agentIdent, config)
		agentToBuilder_m += agentIdent -> scriptBuilder
		if (!agentToBuilder_m.contains("user"))
			agentToBuilder_m += "user" -> scriptBuilder

		// TODO: Return real warnings and errors
		RsSuccess(())
	}

	def findSiteIdent(tableSetupBean: TableSetupBean, carrierName: String, grid: Int, site: Int): Option[String] = {
		def ok[A](a: A, b: A): Boolean = (a == null || a == b)
		def okInt(a: Integer, b: Int): Boolean = (a == null || a == b)
		def matches(siteBean: SiteBean): Boolean =
			ok(siteBean.carrier, carrierName) &&
			okInt(siteBean.grid, grid) &&
			okInt(siteBean.site, site)
		for ((ident, bean) <- tableSetupBean.sites.toMap) {
			if (matches(bean))
				return Some(ident)
		}
		None
	}

	def createPlan(): RqResult[(List[OperatorInfo], PartialPlan)] = {
		/*val eb = {
			import roboliq.entities._
			val r1 = Agent("r1", Some("r1"))
			val sm001 = SiteModel("sm001", Some("sm001"), None)
			val siteA = Site("siteA", Some("siteA"))
			val siteB = Site("siteB", Some("siteB"))
			val shaker = Shaker("r1_shaker", Some("r1_shaker"))
			val m001 = PlateModel("m001", Some("m001"), None, 8, 12, LiquidVolume.ul(300))
			val plateA = Plate("plateA", Some("plateA"))
			val eb = new EntityBase
			eb.addAgent(r1, r1.key)
			eb.addModel(sm001, sm001.key)
			eb.addSite(siteA, siteA.key)
			eb.addSite(siteB, siteB.key)
			eb.addDevice(r1, shaker, shaker.key)
			eb.addDeviceSite(shaker, siteB)
			eb.addModel(m001, m001.key)
			eb.addLabware(plateA, plateA.key)
			eb.transportGraph = Graph[Site, LkUnDiEdge](LkUnDiEdge(siteA, siteB)(("user", "", "")))
			eb
		}*/
		
		for {
			operatorInfo_l <- CallTree.getOperatorInfo(cs, tree, eb, state0.toImmutable)
			_ = println("planInfo_l:")
			_ = println(operatorInfo_l)
			_ = println("domain:")
			domain <- createDomain(cs, operatorInfo_l)
			_ = println(domain.toStripsText)
			problem <- createProblem(operatorInfo_l, domain)
			_ = println(problem.toStripsText)
			plan0 = PartialPlan.fromProblem(problem)
			operator_l <- RsResult.mapAll(operatorInfo_l)(operatorInfo => {
				for {
					handler <- RsResult.from(cs.nameToOperatorHandler_m.get(operatorInfo.operatorName), s"createPlan: Unknown operator `${operatorInfo.operatorName}`")
				} yield {
					val domainOperator = handler.getDomainOperator
					domainOperator.bind(operatorInfo.operatorBinding_m)
				}
			})
			plan1 <- plan0.addActionSequence(operator_l).asRs
		} yield {
			(operatorInfo_l, plan1)
		}
	}
	
	def createDomain(cs: CommandSet, operatorInfo_l: List[OperatorInfo]): RqResult[Strips.Domain] = {
		RsResult.prependError("createDomain:") {
			val name_l = (cs.nameToAutoOperator_l ++ operatorInfo_l.map(_.operatorName)).distinct.sorted
			for {
				operatorHandler_l <- RsResult.mapAll(name_l)(cs.getOperatorHandler)
			} yield {
				val operator_l = operatorHandler_l.map(_.getDomainOperator)
				Strips.Domain(
					type_l = List(
						"labware",
						"model",
						"site",
						"siteModel",
						
						"agent",
						
						"pipetter",
						"pipetterProgram",
						
						"shaker",
						"shakerProgram"
					),
					constantToType_l = Nil,
					predicate_l = List[Strips.Signature](
						Strips.Signature("agent-has-device", "?agent" -> "agent", "?device" -> "device"),
						Strips.Signature("device-can-site", "?device" -> "device", "?site" -> "site"),
						Strips.Signature("location", "?labware" -> "labware", "?site" -> "site"),
						Strips.Signature("model", "?labware" -> "labware", "?model" -> "model"),
						Strips.Signature("stackable", "?sm" -> "siteModel", "?m" -> "model")
					),
					operator_l = operator_l
				)
			}
		}
	}


	def createProblem(planInfo_l: List[OperatorInfo], domain: Strips.Domain): RqResult[Strips.Problem] = {
		val typToObject_l: List[(String, String)] = /*List(
			"agent" -> "r1",
			"pipetter" -> "r1_pipetter",
			"shaker" -> "r1_shaker",
			"model" -> "m001",
			"siteModel" -> "sm001",
			"site" -> "siteA",
			"site" -> "siteB",
			"labware" -> "plateA",
			"labware" -> "plateB"
		)*/ eb.createProblemObjects.map(_.swap) ++ planInfo_l.flatMap(_.problemObjectToTyp_l).map(_.swap)
		
		val state0 = Strips.State(Set[Strips.Atom](
			/*
			Strips.Atom("location", "plateA", "siteA"),
			Strips.Atom("site-blocked", "siteA"),
			Strips.Atom("agent-has-device", "r1", "r1_pipetter"),
			Strips.Atom("agent-has-device", "r1", "r1_shaker"),
			Strips.Atom("model", "plateA", "m001"),
			Strips.Atom("device-can-site", "r1_pipetter", "siteB"),
			Strips.Atom("device-can-site", "r1_shaker", "siteB"),
			Strips.Atom("model", "siteA", "sm001"),
			Strips.Atom("model", "siteB", "sm001"),
			Strips.Atom("stackable", "sm001", "m001")*/
		) ++ planInfo_l.flatMap(_.problemState_l) ++ eb.createProblemState.map(rel => Strips.Atom(rel.name, rel.args)))
		
		RqSuccess(Strips.Problem(
			domain = domain,
			typToObject_l = typToObject_l,
			state0 = state0,
			goals = Strips.Literals.empty
		))
	}

}