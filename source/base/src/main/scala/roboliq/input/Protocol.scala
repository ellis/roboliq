package roboliq.input

import spray.json._
import spray.json.DefaultJsonProtocol._
import grizzled.slf4j.Logger
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer
import roboliq.core._
import roboliq.entities._
import roboliq.input.commands._
import roboliq.method
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

case class ReagentBean(
	id: String,
	wells: PipetteDestinations,
	contaminants : Set[String],
	viscosity_? : Option[String],
	sterilize_? : Option[String],
	pipettePolicy_? : Option[String],
	key_? : Option[String]
)

private case class Task(
	rel: Rel,
	effects: List[WorldStateEvent]
) extends Action {
}

class Protocol {
	
	private val logger = Logger[this.type]

	val eb = new EntityBase
	val state0 = new WorldStateBuilder
	private var tasks = new ArrayBuffer[Rel]
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

	def loadConfigBean(configBean: ConfigBean): RsResult[Unit] = {
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
			RsResult.toResultOfList(configBean.evowareAgents.toList.map(pair => {
				val (name, agent) = pair
				for {
					// Load carrier file
					evowarePath <- RsResult(agent.evowareDir, "evowareDir must be set")
					carrierData <- roboliq.evoware.parser.EvowareCarrierData.loadFile(new File(evowarePath, "carrier.cfg").getPath)
					// FIXME: for debug only
					//_ = carrierData.printCarriersById
					// ENDIF
					// Load table file
					tableFile <- RsResult(agent.tableFile, "tableFile must be set")
					tableData <- roboliq.evoware.parser.EvowareTableData.loadFile(carrierData, tableFile)
					
					_ <- loadEvoware(name, carrierData, tableData, agent)
				} yield ()
			})).map(_ => ())
		}
	}

	def loadJson(jsobj: JsObject): RsResult[Unit] = {
		for {
			_ <- jsobj.fields.get("labware") match {
				case Some(JsObject(map)) =>
					RqResult.toResultOfList(map.toList.map(pair => {
						val (name,jsobj) = pair
						def make(modelRef: String, locationRef: String): RqResult[Unit] = {
							// REFACTOR: duplicates lots of code from the `plates` section below
							val key = gid
							//logger.debug("modelKey: "+modelKey)
							//println("eb.nameToEntity: "+eb.nameToEntity)
							//println("eb.idToEntity: "+eb.idToEntity)
							//println("eb.idToEntity.get(\"Thermocycler Plate\"): "+eb.idToEntity.get("Thermocycler Plate"))
							//logger.debug("eb.aliases: "+eb.aliases)
							val model = eb.getEntityAs[PlateModel](modelRef).toOption.get
							val plate = new Plate(key)
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
							RqSuccess(())
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
					}))
				case _ => RqSuccess(())
			}
			
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
						val tipCleanPolicy = m.getOrElse("tipCleanPolicy", "Thorough") match {
							case "None" => TipCleanPolicy.NN
							case "ThoroughNone" => TipCleanPolicy.TN
							case "ThoroughLight" => TipCleanPolicy.TL
							case "Thorough" => TipCleanPolicy.TT
							case "Decontaminate" => TipCleanPolicy.DD
						}
						val substance = Substance(key, Some(name), None, kind, tipCleanPolicy, Set(), None, None, None, None, Nil, None)
						nameToSubstance_m(name) = substance
						RqSuccess(())
					}))
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
				case Some(jsval) => loadJsonProtocol_Protocol(jsval)
				case _ => RsSuccess(())
			}
		} yield ()
	}
	
	private def loadJsonProtocol_Protocol(jsval: JsValue): RsResult[Unit] = {
		logger.debug("parse `protocol`")
		val path0 = new PlanPath(Nil, state0.toImmutable)
		def step(jscmd_l: List[JsValue], path: PlanPath): RqResult[PlanPath] = {
			jscmd_l match {
				case Nil => RqSuccess(path)
				case jscmd :: rest =>
					for {
						pair <- loadJsonProtocol_Protocol_getCommand(jscmd)
						//_ = println("pair: "+pair)
						path1 <- pair match {
							case None => RsSuccess(path)
							case Some((cmd, nameToVal_l)) =>
								loadJsonProtocol_ProtocolCommand(cmd, nameToVal_l, path)
						}
						path2 <- step(rest, path1)
					} yield path2
			}
		}
		jsval match {
			case JsArray(jscmd_l) =>
				for {
					path1 <- step(jscmd_l, path0)
				} yield {
					val action_l = path1.action_r.reverse
					val task_l = action_l.map(_.asInstanceOf[Task])
					tasks ++= task_l.map(_.rel)
				}
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
			nameToVal3_l <- RsResult.toResultOfList(nameToVals_l.map(pair => {
				val (name, jsval_l) = pair
				jsval_l match {
					case jsval :: Nil => RsSuccess((name, jsval))
					case _ => RsError(s"too many values supplied for argument `$name`")
				}
			}))
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
			nameToVal3_l <- RsResult.toResultOfList(nameToVals_l.map(pair => {
				val (name, jsval_l) = pair
				jsval_l match {
					case jsval :: Nil => RsSuccess((name, jsval))
					case _ => RsError(s"too many values supplied for argument `$name`")
				}
			}))
			nameToVal_m = nameToVal3_l.toMap
			l <- doit(name_l, jsval_l, nameToVal_m, Nil)
		} yield l
	}
	
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
		agentBean: EvowareAgentBean
	): RsResult[Unit] = {
		import roboliq.entities._
		
		val agent = Agent(gid)
		eb.addAgent(agent, agentIdent)
		
		val identToAgentObject = new HashMap[String, Object]
		agentToIdentToInternalObject(agentIdent) = identToAgentObject

		val pipetterIdent = agentIdent+"_pipetter1"
		val pipetter = new Pipetter(gid, Some(agentIdent+" LiHa"))
		eb.addDevice(agent, pipetter, pipetterIdent)

		val labwareNamesOfInterest_l = new HashSet[String]

		def loadAgentBean(): RsResult[Unit] = {
			// Labware to be used
			if (agentBean.labware != null) {
				labwareNamesOfInterest_l ++= agentBean.labware
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
		//logger.debug("labwareNamesOfInterest_l: "+labwareNamesOfInterest_l)

		// Create PlateModels
		val labwareModelEs = carrierData.models.collect({case m: roboliq.evoware.parser.EvowareLabwareModel if labwareNamesOfInterest_l.contains(m.sName) => m})
		val idToModel_m = new HashMap[String, LabwareModel]
		for (mE <- labwareModelEs) {
			if (mE.sName.contains("Plate") || mE.sName.contains("96")) {
				val m = PlateModel(mE.sName, Some(mE.sName), None, mE.nRows, mE.nCols, LiquidVolume.ul(mE.ul))
				val ident = f"m${idToModel_m.size + 1}%03d"
				idToModel_m(mE.sName) = m
				eb.addModel(m, ident)
				// All models can be offsite
				eb.addStackable(offsiteModel, m)
				// The user arm can handle all models
				eb.addDeviceModel(userArm, m)
				//eb.addRel(Rel("transporter-can", List(eb.names(userArm), eb.names(m), "nil")))
				identToAgentObject(ident.toLowerCase) = mE
			}
		}
		
		//
		// Create Sites
		//
		
		val siteEsToSiteModel_m = new HashMap[List[(Int, Int)], SiteModel]
		val siteIdToSite_m = new HashMap[(Int, Int), Site]
		val carriersSeen_l = new HashSet[Int]
		
		// Create Hotel Sites
		for (o <- tableData.lHotelObject) {
			val carrierE = o.parent
			carriersSeen_l += carrierE.id
			//println("carrier: "+carrierE)
			for (site_i <- 0 until carrierE.nSites) {
				val siteE = roboliq.evoware.parser.CarrierSite(carrierE, site_i)
				val siteId = (carrierE.id, site_i)
				val site = Site(gid, Some(s"${agentIdent} hotel ${carrierE.sName} site ${site_i+1}"))
				val siteIdent = s"${agentIdent}_hotel_${carrierE.id}x${site_i+1}"
				agentToIdentToInternalObject(agentIdent)
				siteIdToSite_m(siteId) = site
				identToAgentObject(siteIdent.toLowerCase) = siteE
				eb.addSite(site, siteIdent)
			}
		}
		
		// Create Device Sites
		for (o <- tableData.lExternalObject if !carriersSeen_l.contains(o.carrier.id)) {
			val carrierE = o.carrier
			carriersSeen_l += carrierE.id
			for (site_i <- 0 until carrierE.nSites) {
				val siteE = roboliq.evoware.parser.CarrierSite(carrierE, site_i)
				val siteId = (carrierE.id, site_i)
				val site = Site(gid, Some(s"${agentIdent} device ${carrierE.sName} site ${site_i+1}"))
				val siteIdent = s"${agentIdent}_device_${carrierE.id}x${site_i+1}"
				siteIdToSite_m(siteId) = site
				identToAgentObject(siteIdent.toLowerCase) = siteE
				eb.addSite(site, siteIdent)
			}
		}
		
		// Create on-bench Sites for Plates
		for ((carrierE, grid_i) <- tableData.mapCarrierToGrid if !carriersSeen_l.contains(carrierE.id)) {
			for (site_i <- 0 until carrierE.nSites) {
				val siteE = roboliq.evoware.parser.CarrierSite(carrierE, site_i)
				val siteId = (carrierE.id, site_i)
				val site = Site(gid, Some(s"${agentIdent} bench ${carrierE.sName} site ${site_i+1}"))
				val siteIdent = f"${agentIdent}_bench_${grid_i}%03dx${site_i+1}"
				siteIdToSite_m(siteId) = site
				identToAgentObject(siteIdent.toLowerCase) = siteE
				eb.addSite(site, siteIdent)
			}
		}
		
		// TODO: Create on-bench Sites and SiteModels for Tubes
		// TODO: Let userArm handle tube models
		// TODO: Let userArm access all sites that the robot arms can't
		
		// Create SiteModels for for sites which hold Plates
		val siteIdToModels_m = new HashMap[(Int, Int), collection.mutable.Set[LabwareModel]] with MultiMap[(Int, Int), LabwareModel]
		
		{
			// First gather map of all relevant labware models that can be placed on each site 
			for (mE <- labwareModelEs if idToModel_m.contains(mE.sName)) {
				val m = idToModel_m(mE.sName)
				for (siteId <- mE.sites if siteIdToSite_m.contains(siteId)) {
					val site = siteIdToSite_m(siteId)
					siteIdToModels_m.addBinding(siteId, m)
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
				val ident = s"r1_transporter${roma_i + 1}"
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
				val ident = s"${agentIdent}_transporterSpec${vector_i}"
				identToAgentObject(ident.toLowerCase) = vectorClass
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
			import scalax.collection.Graph // or scalax.collection.mutable.Graph
			import scalax.collection.GraphPredef._, scalax.collection.GraphEdge._
			import scalax.collection.edge.LHyperEdge
			val edge_l = test_m.toList.flatMap(pair => {
				val (key, site_l) = pair
				site_l.combinations(2).map(l => LkUnDiEdge(l(0), l(1))(key))
			})
			Graph[Site, LkUnDiEdge](edge_l : _*)
		}
		println("graph: "+graph.size)
		graph.take(5).foreach(println)
		
		def addDevice0(
			device: Device,
			deviceIdent: String,
			carrierE: roboliq.evoware.parser.Carrier
		): Device = {
			// Add device
			eb.addDevice(agent, device, deviceIdent)
			identToAgentObject(deviceIdent) = carrierE
	
			// Add device sites
			for (site_i <- 0 until carrierE.nSites) {
				val siteId = (carrierE.id, site_i)
				val site: Site = siteIdToSite_m(siteId)
				siteIdToModels_m.get(siteId).map { model_l =>
					eb.addDeviceSite(device, site)
					model_l.foreach(m => eb.addDeviceModel(device, m))
				}
			}
			
			device
		}
		
		def addDevice(
			typeName: String,
			deviceName: String,
			carrierE: roboliq.evoware.parser.Carrier
		): Device = {
			// Add device
			val device = new Device { val key = gid; val label = Some(carrierE.sName); val description = None; val typeNames = List(typeName) }
			addDevice0(device, deviceName, carrierE)
		}
		
		def addPeeler(
			deviceName: String,
			carrierE: roboliq.evoware.parser.Carrier
		): Device = {
			val device = new Peeler(gid, Some(carrierE.sName))
			addDevice0(device, deviceName, carrierE)
		}
		
		def addSealer(
			deviceName: String,
			carrierE: roboliq.evoware.parser.Carrier
		): Device = {
			val device = new Sealer(gid, Some(carrierE.sName))
			addDevice0(device, deviceName, carrierE)
		}
		
		def addShaker(
			deviceName: String,
			carrierE: roboliq.evoware.parser.Carrier
		): Device = {
			val device = new Shaker(gid, Some(carrierE.sName))
			addDevice0(device, deviceName, carrierE)
		}
		
		for ((carrierE, iGrid) <- tableData.mapCarrierToGrid) {
			carrierE.sName match {
				case "MP 2Pos H+P Shake" =>
					val deviceIdent = agentIdent+"_shaker"
					
					// REFACTOR: duplicates addShaker() because only one of the sites can be used for shaking
					val device = new Shaker(gid, Some(carrierE.sName))
					// Add device
					eb.addDevice(agent, device, deviceIdent)
					identToAgentObject(deviceIdent) = carrierE
					// Add device sites
					// HACK: only use last site for shaking, this is truely a bad hack!  Things like this should be performed via configuration overrides.
					for (site_i <- List(carrierE.nSites - 1)) {
						val siteId = (carrierE.id, site_i)
						val site: Site = siteIdToSite_m(siteId)
						siteIdToModels_m.get(siteId).map { model_l =>
							eb.addDeviceSite(device, site)
							model_l.foreach(m => eb.addDeviceModel(device, m))
						}
					}

				case "RoboPeel" =>
					val deviceIdent = agentIdent+"_peeler"
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
					val deviceIdent = agentIdent+"_sealer"
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
				case "Te-Shake 2Pos" =>
					val deviceIdent = agentIdent+"_shaker"
					val device = addDevice0(new Shaker(gid, Some(carrierE.sName)), deviceIdent, carrierE)
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
					val deviceIdent = agentIdent+"_thermocycler1"
					val device = addDevice0(new Thermocycler(gid, Some(carrierE.sName)), deviceIdent, carrierE)
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
	
	def saveProblem(path: String, userInitialConditions: String = "") {
		val file = new java.io.File(path)
		val name = FilenameUtils.getBaseName(file.getName())
		FileUtils.printToFile(file) { p =>
			p.println(s"(defproblem $name domain")
			p.println(" ; initial conditions")
			p.println(" (")
			p.println(eb.makeInitialConditions)
			if (userInitialConditions != null && !userInitialConditions.isEmpty()) {
				p.println(" ; user initial conditions")
				p.println(userInitialConditions)
			}
			p.println(" )")
			p.println(" ; tasks")
			p.println(" (")
			tasks.foreach(r => p.println("  "+r))
			p.println(" )")
			p.println(")")
		}
	}

}