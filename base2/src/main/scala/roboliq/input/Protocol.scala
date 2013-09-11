package roboliq.input

import spray.json._
import spray.json.DefaultJsonProtocol._
import roboliq.entities.Entity
import scala.collection.mutable.ArrayBuffer
import roboliq.core._
import roboliq.entities._
import scala.collection.mutable.HashSet
import scala.collection.mutable.HashMap
import scala.collection.mutable.MultiMap
import roboliq.utils.FileUtils

class Protocol {
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
	
	/** Tuple: (specIdent, filepath) */
	val sealerSpec_l = new ArrayBuffer[(String, String)]
	/** Tuple: (deviceIdent, evoware plate model ID, specIdent) */
	val sealerSpecRel_l = new ArrayBuffer[(String, String, String)]
	
	/** Tuple: (specIdent, dir.prog) */
	val thermocyclerSpec_l = new ArrayBuffer[(String, String)]
	/** Tuple: (deviceIdent, specIdent) */
	val thermocyclerSpecRel_l = new ArrayBuffer[(String, String)]
	
	val nameToSubstance_m = new HashMap[String, Substance]
	/**
	 * Map of task variable identifier to an internal object -- for example, the name of a text variable to the actual text.
	 */
	val idToObject = new HashMap[String, Object]
	/**
	 * Individual agents may need to map identifiers to internal objects
	 */
	val agentToIdentToInternalObject = new HashMap[String, HashMap[String, Object]]

	/**
	 * This should eventually load a YAML file.
	 * For now it's just hard-coded for my testing purposes.
	 */
	def loadConfig() {
		import roboliq.entities._
		
		val user = Agent(gid, Some("user"))
		val offsite = Site(gid, Some("offsite"))
		val shakerSpec1 = ShakerSpec(gid)
		val thermocyclerSpec1 = ThermocyclerSpec(gid)
		
		eb.addAlias("Thermocycler Plate", "D-BSSE 96 Well PCR Plate")
		eb.addAgent(user, "user")
		eb.addModel(offsiteModel, "offsiteModel")
		eb.addSite(offsite, "offsite")
		eb.addDevice(user, userArm, "userArm")
		eb.addDeviceSpec(userArm, userArmSpec, "userArmSpec")
		
		// userArm can transport from offsite
		eb.addRel(Rel("transporter-can", List("userArm", "offsite", "userArmSpec")))
		// A few other user-specified sites where the user can put plates on the robot
		eb.addRel(Rel("transporter-can", List("userArm", "r1_hotel_245x1", "userArmSpec")))
		
		//eb.addRel(Rel("sealer-can", List("r1_sealer", ")))
		sealerSpec_l += (("sealerSpec1", """C:\Programme\HJBioanalytikGmbH\RoboSeal3\RoboSeal_PlateParameters\4titude_PCR_blau.bcf"""))
		sealerSpecRel_l += (("r1_sealer", "D-BSSE 96 Well PCR Plate", "sealerSpec1"))
		
		thermocyclerSpec_l += (("thermocyclerSpec1", "0.3"))
		thermocyclerSpecRel_l += (("r1_thermocycler1", "thermocyclerSpec1"))
	}

	def loadJson(jsobj: JsObject) {
		jsobj.fields.get("substances") match {
			case Some(js) =>
				val inputs: List[Map[String, String]] = js.convertTo[List[Map[String, String]]]
				for (m <- inputs) {
					val key = m.getOrElse("id", gid)
					val name = m.getOrElse("name", key)
					val kind = m("kind") match {
						case "Liquid" => SubstanceKind.Liquid
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
				}
			case _ =>
		}
		
		jsobj.fields.get("plates") match {
			case Some(js) =>
				val plateInputs: List[Map[String, String]] = js.convertTo[List[Map[String, String]]]
				for (m <- plateInputs) {
					val id = m.getOrElse("id", gid)
					val name = m.getOrElse("name", id)
					val modelKey = m("model")
					println("modelKey: "+modelKey)
					//println("eb.nameToEntity: "+eb.nameToEntity)
					//println("eb.idToEntity: "+eb.idToEntity)
					//println("eb.idToEntity.get(\"Thermocycler Plate\"): "+eb.idToEntity.get("Thermocycler Plate"))
					println("eb.aliases: "+eb.aliases)
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
			case _ =>
		}
		
		jsobj.fields.get("tubes") match {
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
			case _ =>
		}
		
		jsobj.fields.get("wellContents") match {
			case Some(js) =>
				val inputs: List[Map[String, String]] = js.convertTo[List[Map[String, String]]]
				for (m <- inputs) {
					val wellIdent = m("name")
					val contents_s = m("contents")
					for {
						aliquot <- AliquotParser.parseAliquot(contents_s, nameToSubstance_m.toMap)
						l <- eb.lookupLiquidSource(wellIdent)
						well_l <- RsResult.toResultOfList(l.map(state0.getWell))
					} {
						for (well <- well_l) {
							state0.well_aliquot_m(well) = aliquot
						}
					}
				}
			case _ =>
		}
		
		jsobj.fields.get("protocol") match {
			case Some(JsArray(l)) =>
				for (js <- l) {
					js match {
						case JsObject(fields) =>
							if (fields.contains("command")) {
								fields.get("command") match {
									case Some(JsString("log")) =>
										fields.get("text") match {
											case Some(JsString(text)) =>
												val agent = f"?a$nvar%04d"
												val textId = f"text$nvar%04d"
												idToObject(textId) = text
												//println("idToObject:" + idToObject)
												tasks += Rel("!log", List(agent, textId))
											case _ =>
										}
									case Some(JsString("prompt")) =>
										fields.get("text") match {
											case Some(JsString(text)) =>
												val agent = f"?a$nvar%04d"
												val textId = f"text$nvar%04d"
												idToObject(textId) = text
												//println("idToObject:" + idToObject)
												tasks += Rel("!prompt", List(agent, textId))
											case _ =>
										}
									case Some(JsString("move")) =>
										//val agent = x(fields, "agent")
										//val device = x(fields, "device")
										val labware = fields("labware").asInstanceOf[JsString].value
										val destination = fields("destination").asInstanceOf[JsString].value
										tasks += Rel("move-labware", List(labware, destination))
									case Some(JsString("seal")) =>
										fields.get("object") match {
											case Some(JsString(key)) =>
												val agent = f"?a$nvar%04d"
												val device = f"?d$nvar%04d"
												val plate = eb.getEntity(key).get.asInstanceOf[Labware]
												val plateName = eb.names(plate)
												tasks += Rel("sealer-run", List(agent, device, plateName, f"?s$nvar%04d"))
											case _ =>
										}
									case Some(JsString("thermocycle")) =>
										val plateIdent_? = fields.get("object") match {
											case Some(JsString(plateIdent)) => RsSuccess(plateIdent)
											case _ => RsError("must supply an `object` which references a plate by name")
										}
										val specIdent_? = fields.get("spec") match {
											case Some(JsString(specIdent)) => RsSuccess(specIdent)
											case _ => RsError("must supply a `spec`")
										}
										for {
											plateIdent <- plateIdent_?
											specIdent <- specIdent_?
										} {
											val agentIdent = f"?a$nvar%04d"
											val deviceIdent = f"?d$nvar%04d"
											val site2Ident = f"?s$nvar%04d"
											tasks += Rel("thermocycle-plate", List(agentIdent, deviceIdent, specIdent, plateIdent, site2Ident))
										}
									case Some(JsString("distribute")) =>
										val source_? = fields.get("source") match {
											case Some(JsString(sourceIdent)) =>
												eb.lookupLiquidSource(sourceIdent)
											case _ => RsError("must supply a `source` string")
										}
										val destination_? = fields.get("destination") match {
											case Some(JsString(destinationIdent)) =>
												eb.lookupLiquidSource(destinationIdent)
											case _ => RsError("must supply a `destination` string")
										}
										val volume_? = fields.get("volume") match {
											case Some(JsString(volume_s)) =>
												LiquidVolumeParser.parse(volume_s)
											case _ => RsError("must supply a `volume` string")
										}
										println(s"source: ${source_?}, dest: ${destination_?}, vol: ${volume_?}")
										// produces a Relation such as: distribute2 [agent] [device] [spec] [labware1] [labware2]
										// The script builder later lookups up the spec in the protocol.
										// That should return an object that accepts the two labware objects.
										for {
											source <- source_?
											destination <- destination_?
											volume <- volume_?
										} {
											val agentIdent = f"?a$nvar%04d"
											val deviceIdent = f"?d$nvar%04d"
											val labware_l: List[Labware] = (source ++ destination).map(_._1).distinct
											val labwareIdent_l: List[String] = labware_l.map(eb.names)
											val n = labware_l.size
											val spec = PipetteSpec(source, destination, volume)
											val specIdent = f"spec$nvar%04d"
											idToObject(specIdent) = spec
											tasks += Rel(s"distribute$n", agentIdent :: deviceIdent :: specIdent :: labwareIdent_l)
										}
									case Some(JsString("shake")) =>
									case _ =>
								}
							}
						case _ =>
					}
				}
			case _ =>
		}
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
		tableData: roboliq.evoware.parser.EvowareTableData
	) {
		import roboliq.entities._
		
		val agent = Agent(gid)
		eb.addAgent(agent, agentIdent)
		
		val identToAgentObject = new HashMap[String, Object]
		agentToIdentToInternalObject(agentIdent) = identToAgentObject

		// FIXME: This doesn't belong here at all!
		val labwareNamesOfInterest_l = new HashSet[String]
		labwareNamesOfInterest_l += "D-BSSE 96 Well PCR Plate"
		labwareNamesOfInterest_l += "D-BSSE 96 Well DWP"
		val tipModel1000 = TipModel("1000ul", None, None, LiquidVolume.ul(950), LiquidVolume.ul(3), Map())
		val tipModel50 = TipModel("50ul", None, None, LiquidVolume.ul(45), LiquidVolume.ul(0.1), Map())
		
		val tip1 = Tip("tip1", None, None, 0, 0, 0, Some(tipModel1000))
		val tip2 = Tip("tip2", None, None, 1, 1, 0, Some(tipModel1000))
		val tip3 = Tip("tip3", None, None, 2, 2, 0, Some(tipModel1000))
		val tip4 = Tip("tip4", None, None, 3, 3, 0, Some(tipModel1000))
		
		// Permanent tips at BSSE
		state0.tip_model_m(tip1) = tipModel1000
		state0.tip_model_m(tip2) = tipModel1000
		state0.tip_model_m(tip3) = tipModel1000
		state0.tip_model_m(tip4) = tipModel1000

		val pipetterIdent = agentIdent+"_pipetter1"
		val pipetter = new Pipetter(gid, Some(agentIdent+" LiHa"))
		eb.addDevice(agent, pipetter, pipetterIdent)
		eb.pipetterToTips_m(pipetter) = List(tip1, tip2, tip3, tip4)
		eb.tipToTipModels_m(tip1) = List(tipModel1000)
		eb.tipToTipModels_m(tip2) = List(tipModel1000)
		eb.tipToTipModels_m(tip3) = List(tipModel1000)
		eb.tipToTipModels_m(tip4) = List(tipModel1000)
		// ENDFIX
		
		// Add labware on the table definition to the list of labware we're interested in
		labwareNamesOfInterest_l ++= tableData.mapSiteToLabwareModel.values.map(_.sName)

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
		
		// Find which sites the transporters can access
		for ((carrierE, vector_l) <- carrierData.mapCarrierToVectors) {
			for (site_i <- 0 until carrierE.nSites) {
				val siteId = (carrierE.id, site_i)
				siteIdToSite_m.get(siteId).foreach { site =>
					for (vector <- vector_l) {
						val transporter = roma_m(vector.iRoma)
						val spec = transporterSpec_m(vector.sClass)
						eb.addRel(Rel("transporter-can", List(eb.names(transporter), eb.names(site), eb.names(spec))))
					}
				}
			}
		}
		
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
				eb.addDeviceSite(device, site)
				siteIdToModels_m(siteId).foreach(m => eb.addDeviceModel(device, m))
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
		
		def addSealer(
			deviceName: String,
			carrierE: roboliq.evoware.parser.Carrier
		): Device = {
			val device = new Sealer(gid, Some(carrierE.sName))
			addDevice0(device, deviceName, carrierE)
		}
		
		for ((carrierE, iGrid) <- tableData.mapCarrierToGrid) {
			carrierE.sName match {
				case "RoboSeal" =>
					val deviceIdent = agentIdent+"_sealer"
					val device = addSealer(deviceIdent, carrierE)
					// Add user-defined specs for this device
					for ((deviceIdent2, plateModelId, specIdent) <- sealerSpecRel_l if deviceIdent2 == deviceIdent) {
						// Get or create the sealer spec for specIdent
						val spec: SealerSpec = eb.getEntity(specIdent) match {
							case Some(spec) => spec.asInstanceOf[SealerSpec]
							case None =>
								// Store the evoware string for this spec
								val internal = sealerSpec_l.find(_._1 == specIdent).get._2
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
				case "RoboPeel" =>
					addDevice("peeler", agentIdent+"_peeler", carrierE)
				case "TRobot1" =>
					val deviceIdent = agentIdent+"_thermocycler1"
					val device = addDevice0(new Thermocycler(gid, Some(carrierE.sName)), deviceIdent, carrierE)
					// Add user-defined specs for this device
					for ((deviceIdent2, specIdent) <- thermocyclerSpecRel_l if deviceIdent2 == deviceIdent) {
						// Get or create the spec for specIdent
						val spec: ThermocyclerSpec = eb.getEntity(specIdent) match {
							case Some(spec) => spec.asInstanceOf[ThermocyclerSpec]
							case None =>
								// Store the evoware string for this spec
								val internal = thermocyclerSpec_l.find(_._1 == specIdent).get._2
								identToAgentObject(specIdent.toLowerCase) = internal
								ThermocyclerSpec(gid, None, Some(internal))
						}
						// Register the spec
						eb.addDeviceSpec(device, spec, specIdent)
					}
				case _ =>
			}
		}
	}
	
	def saveProblem(name: String, userInitialConditions: String = "") {
		FileUtils.printToFile(new java.io.File(s"tasks/autogen/$name.lisp")) { p =>
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