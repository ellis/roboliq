package roboliq.evoware.translator

import roboliq.core._
import roboliq.entities._
import roboliq.commands._
import roboliq.input.Protocol
import grizzled.slf4j.Logger
import scala.collection.mutable.ArrayBuffer
import roboliq.tokens._
import scala.collection.mutable.HashMap
import roboliq.evoware.parser.CarrierSite
import roboliq.evoware.parser.EvowareLabwareModel
import roboliq.evoware.parser.Carrier

case class EvowareScript2(
	index: Int,
	siteToModel_m: Map[CarrierSite, EvowareLabwareModel],
	cmd_l: List[L0C_Command]
)

private case class TranslationItem(
	token: L0C_Command,
	siteToModel_l: List[(CarrierSite, EvowareLabwareModel)]
)

private case class TranslationResult(
	item_l: List[TranslationItem],
	state1: WorldState
)

private object TranslationResult {
	def empty(state1: WorldState) = TranslationResult(Nil, state1)
}

class EvowareClientScriptBuilder(agentName: String, config: EvowareConfig) extends ClientScriptBuilder(agentName) {
	private val logger = Logger[this.type]

	val script_l = new ArrayBuffer[EvowareScript2]
	private var scriptIndex: Int = 0
	//val builder = new EvowareScriptBuilder
	// REFACTOR: Rename to token_l
	val cmds = new ArrayBuffer[L0C_Command]
	//val mapCmdToLabwareInfo = new HashMap[Object, List[(CarrierSite, EvowareLabwareModel)]]
	val siteToModel_m = new HashMap[CarrierSite, EvowareLabwareModel]
	
	def addCommand(
		protocol: Protocol,
		state0: WorldState,
		agentIdent: String,
		command: Command
	): RsResult[WorldState] = {
		logger.debug(s"addCommand: $agentIdent, $command")
		val result_? = {
			if (agentIdent == "user")
				addCommandUser(protocol, state0, agentIdent, command)
			else
				addCommandRobot(protocol, state0, agentIdent, command)
		}
		for {
			result <- result_?
		} yield {
			handleTranslationResult(result)
		}
	}

	private def addCommandRobot(
		protocol: Protocol,
		state0: WorldState,
		agentIdent: String,
		command: Command
	): RsResult[TranslationResult] = {
		val identToAgentObject_m: Map[String, Object] = protocol.agentToIdentToInternalObject.get(agentIdent).map(_.toMap).getOrElse(Map())
		command match {
			case AgentActivate() => RsSuccess(TranslationResult.empty(state0))
			case AgentDeactivate() => RsSuccess(TranslationResult.empty(state0))
			
			case Log(text) =>
				val item = TranslationItem(L0C_Comment(text), Nil)
				RsSuccess(TranslationResult(List(item), state0))
			
			case EvowareSubroutine(path) =>
				val item = TranslationItem(L0C_Subroutine(path), Nil)
				RsSuccess(TranslationResult(List(item), state0))
				
			case cmd: PeelerRun =>
				for {
					carrierE <- identToAgentObject_m.get(cmd.deviceIdent).asRs(s"missing evoware carrier for device `${cmd.deviceIdent}`").flatMap(RsResult.asInstanceOf[Carrier])
					filepath <- identToAgentObject_m.get(cmd.specIdent).asRs(s"missing evoware data for spec `${cmd.specIdent}`").flatMap(RsResult.asInstanceOf[String])
					// List of site/labware mappings for those labware and sites which evoware has equivalences for
					siteToModel_l <- siteLabwareEntry(protocol, state0, identToAgentObject_m, cmd.siteIdent, cmd.labwareIdent).map(_.toList)
					labware <- protocol.eb.getEntityByIdent[Labware](cmd.labwareIdent)
				} yield {
					// Token
					val token = L0C_Facts(carrierE.sName, carrierE.sName+"_Peel", filepath)
					// Update state
					var state1 = state0.toMutable
					state1.labware_isSealed_l -= labware
					// Return
					val item = TranslationItem(token, siteToModel_l)
					TranslationResult(List(item), state1.toImmutable)
				}
			
			case Prompt(text) =>
				val item = TranslationItem(L0C_Prompt(text), Nil)
				RsSuccess(TranslationResult(List(item), state0))
			
			case cmd: ShakerRun =>
				shakerRun(protocol, state0, identToAgentObject_m, cmd)

			case TransporterRun(deviceIdent, labwareIdent, modelIdent, originIdent, destinationIdent, vectorIdent) =>
				// REFACTOR: lots of duplication with TransporterRun for user
				val labware = protocol.eb.getEntity(labwareIdent).get.asInstanceOf[Labware]
				val labwareModel_? = identToAgentObject_m.get(modelIdent).map(_.asInstanceOf[roboliq.evoware.parser.EvowareLabwareModel])
				val origin = protocol.eb.getEntity(originIdent).get
				val originE_? = identToAgentObject_m.get(originIdent).map(_.asInstanceOf[roboliq.evoware.parser.CarrierSite])
				val destination = protocol.eb.getEntity(destinationIdent).get
				val destinationE_? = identToAgentObject_m.get(destinationIdent).map(_.asInstanceOf[roboliq.evoware.parser.CarrierSite])
				//val state = state0.toMutable
				val cmd = {
					val roma_i: Int = identToAgentObject_m(deviceIdent).asInstanceOf[Integer]
					val model = identToAgentObject_m(modelIdent).asInstanceOf[roboliq.evoware.parser.EvowareLabwareModel]
					val originE = originE_?.get
					val destinationE = destinationE_?.get
					val vectorClass = identToAgentObject_m(vectorIdent).toString
					val carrierSrc = originE.carrier
					val iGridSrc = config.table.mapCarrierToGrid(carrierSrc)
					val lVectorSrc = config.table.configFile.mapCarrierToVectors(carrierSrc)
			
					val carrierDest = destinationE.carrier
					val iGridDest = config.table.mapCarrierToGrid(carrierDest)
					val lVectorDest = config.table.configFile.mapCarrierToVectors(carrierDest)
			
					L0C_Transfer_Rack(
						roma_i,
						vectorClass,
						//c.sPlateModel,
						//iGridSrc, siteSrc.iSite, siteSrc.carrier.sName,
						//iGridDest, siteDest.iSite, siteDest.carrier.sName,
						model,
						iGridSrc, originE,
						iGridDest, destinationE,
						LidHandling.NoLid, //c.lidHandling,
						iGridLid = 0,
						iSiteLid = 0,
						sCarrierLid = ""
					)
				}
				// Move labware to new location
				var state1 = state0.toMutable
				state1.labware_location_m(labware) = destination
				
				// List of site/labware mappings for those labware and sites which evoware has equivalences for
				val siteToModel_l = labwareModel_? match {
					case None => Nil
					case Some(labwareModel) => List(originE_?, destinationE_?).flatten.map(_ -> labwareModel)
				}
				
				// Finish up
				val item = TranslationItem(cmd, siteToModel_l)
				RsSuccess(TranslationResult(List(item), state1.toImmutable))
				
			case cmd: PipetterAspirate =>
				aspirate(protocol, state0, identToAgentObject_m, cmd.item_l)
				
			case cmd: PipetterDispense =>
				dispense(protocol, state0, identToAgentObject_m, cmd.item_l)
				
			case cmd: PipetterTipsRefresh =>
				pipetterTipsRefresh(protocol, state0, identToAgentObject_m, cmd)
				
			case cmd: SealerRun =>
				for {
					carrierE <- identToAgentObject_m.get(cmd.deviceIdent).asRs(s"missing evoware carrier for device `${cmd.deviceIdent}`").flatMap(RsResult.asInstanceOf[Carrier])
					filepath <- identToAgentObject_m.get(cmd.specIdent).asRs(s"missing evoware data for spec `${cmd.specIdent}`").flatMap(RsResult.asInstanceOf[String])
					// List of site/labware mappings for those labware and sites which evoware has equivalences for
					siteToModel_l <- siteLabwareEntry(protocol, state0, identToAgentObject_m, cmd.siteIdent, cmd.labwareIdent).map(_.toList)
					labware <- protocol.eb.getEntityByIdent[Labware](cmd.labwareIdent)
				} yield {
					// Token
					val token = L0C_Facts(carrierE.sName, carrierE.sName+"_Seal", filepath)
					// Update state
					var state1 = state0.toMutable
					state1.labware_isSealed_l += labware
					// Return
					val item = TranslationItem(token, siteToModel_l)
					TranslationResult(List(item), state1.toImmutable)
				}
			
			case cmd: ThermocyclerClose =>
				for {
					device <- protocol.eb.getEntityByIdent[Thermocycler](cmd.deviceIdent)
					carrierE <- identToAgentObject_m.get(cmd.deviceIdent).asRs(s"missing evoware carrier for device `${cmd.deviceIdent}`").flatMap(RsResult.asInstanceOf[Carrier])
				} yield {
					// Token
					val token = L0C_Facts(carrierE.sName, carrierE.sName+"_LidClose", "")
					// Update state
					var state1 = state0.toMutable
					state1.device_isOpen_l -= device
					// Return
					val item = TranslationItem(token, Nil)
					TranslationResult(List(item), state1.toImmutable)
				}
			
			case cmd: ThermocyclerOpen =>
				for {
					device <- protocol.eb.getEntityByIdent[Thermocycler](cmd.deviceIdent)
					carrierE <- identToAgentObject_m.get(cmd.deviceIdent).asRs(s"missing evoware carrier for device `${cmd.deviceIdent}`").flatMap(RsResult.asInstanceOf[Carrier])
				} yield {
					// Token
					val token = L0C_Facts(carrierE.sName, carrierE.sName+"_LidOpen", "")
					// Update state
					var state1 = state0.toMutable
					state1.device_isOpen_l += device
					// Return
					val item = TranslationItem(token, Nil)
					TranslationResult(List(item), state1.toImmutable)
				}
			
			case cmd: ThermocyclerRun =>
				for {
					device <- protocol.eb.getEntityByIdent[Thermocycler](cmd.deviceIdent)
					carrierE <- identToAgentObject_m.get(cmd.deviceIdent).asRs(s"missing evoware carrier for device `${cmd.deviceIdent}`").flatMap(RsResult.asInstanceOf[Carrier])
					value <- identToAgentObject_m.get(cmd.specIdent).asRs(s"missing evoware data for spec `${cmd.specIdent}`").flatMap(RsResult.asInstanceOf[String])
				} yield {
					// Token
					val token = L0C_Facts(carrierE.sName, carrierE.sName+"_RunProgram", value)
					// Return
					val item = TranslationItem(token, Nil)
					TranslationResult(List(item), state0)
				}
				
			case _ =>
				RsError(s"unknown command `$command`")
		}
	}

	private def addCommandUser(
		protocol: Protocol,
		state0: WorldState,
		agentIdent: String,
		command: Command
	): RsResult[TranslationResult] = {
		val identToAgentObject_m: Map[String, Object] = protocol.agentToIdentToInternalObject.get(agentIdent).map(_.toMap).getOrElse(Map())
		
		def promptUnknown(): RsResult[TranslationResult] = {
			val item = TranslationItem(L0C_Prompt(s"Please perform this command: $command"), Nil)
			RsSuccess(TranslationResult(List(item), state0))
		}
		
		def prompt(text: String, state1: WorldState): RsResult[TranslationResult] = {
			val item = TranslationItem(L0C_Prompt(text), Nil)
			RsSuccess(TranslationResult(List(item), state1))
		}
		
		command match {
			case AgentActivate() => RsSuccess(TranslationResult.empty(state0))
			case AgentDeactivate() => RsSuccess(TranslationResult.empty(state0))
			
			case Log(text) =>
				val item = TranslationItem(L0C_Comment(text), Nil)
				RsSuccess(TranslationResult(List(item), state0))
			
			case Prompt(text) =>
				val item = TranslationItem(L0C_Prompt(text), Nil)
				RsSuccess(TranslationResult(List(item), state0))

			case TransporterRun(deviceIdent, labwareIdent, modelIdent, originIdent, destinationIdent, vectorIdent) =>
				for {
					labware <- protocol.eb.getEntityAs[Labware](labwareIdent)
					model <- protocol.eb.getEntityAs[LabwareModel](modelIdent)
					origin <- protocol.eb.getEntityAs[Site](originIdent)
					destination <- protocol.eb.getEntityAs[Site](destinationIdent)
					//originE <- identToAgentObject_m.get(originIdent).map(_.asInstanceOf[roboliq.evoware.parser.CarrierSite]).asRs(s"missing evoware data for site `$originIdent`")
					//destinationE <- identToAgentObject_m.get(destinationIdent).map(_.asInstanceOf[roboliq.evoware.parser.CarrierSite]).asRs(s"missing evoware data for site `$destinationIdent`")
				} yield {
					val modelLabel = model.label.getOrElse(model.key)
					val originLabel = origin.label.getOrElse(origin.key)
					val destinationLabel = destination.label.getOrElse(destination.key)
					val text = s"Please move labware `${labwareIdent}` model `${modelLabel}` from `${originLabel}` to `${destinationLabel}`"

					// Move labware to new location
					var state1 = state0.toMutable
					state1.labware_location_m(labware) = destination
					
					// List of site/labware mappings for those labware and sites which evoware has equivalences for
					val labwareModel_? = identToAgentObject_m.get(modelIdent).map(_.asInstanceOf[roboliq.evoware.parser.EvowareLabwareModel])
					val originE_? = identToAgentObject_m.get(originIdent).map(_.asInstanceOf[roboliq.evoware.parser.CarrierSite])
					val destinationE_? = identToAgentObject_m.get(destinationIdent).map(_.asInstanceOf[roboliq.evoware.parser.CarrierSite])
					val siteToModel_l = labwareModel_? match {
						case None => Nil
						case Some(labwareModel) => List(originE_?, destinationE_?).flatten.map(_ -> labwareModel)
					}
					
					// Finish up
					val item = TranslationItem(L0C_Prompt(text), siteToModel_l)
					TranslationResult(List(item), state1.toImmutable)
				}
				
			case cmd: PipetterAspirate => promptUnknown()
			case cmd: PipetterDispense => promptUnknown()
			case cmd: SealerRun => promptUnknown()
				
			case _ => promptUnknown()
				//RsError(s"unknown command `$command`")
		}
	}

	def end(): RsResult[Unit] = {
		endScript()
		RsSuccess(())
	}
	
	private def handleTranslationResult(result: TranslationResult): WorldState = {
		for (item <- result.item_l) {
			setSiteModels(item.siteToModel_l)
			cmds += item.token
		}
		result.state1
	}
	
	private def getCarrierSite(
		protocol: Protocol,
		state: WorldState,
		identToAgentObject_m: Map[String, Object],
		labware: Labware
	): RsResult[CarrierSite] = {
		// TODO: Allow for labware to be on top of other labware (e.g. stacked plates), and figure out the proper site index
		// TODO: For tubes, the site needs to map to a well on an evoware-labware at a site
		// TODO: What we actually need to do here is get the chain of labware (labware may be on other labware) until we reach a 
		// labware which has an evoware equivalent -- for example, we consider tubes to be labware, but evoware only considers the
		// tube adapter to be labware.
		//def makeLocationChain(labware: Labware, acc: List[])
		for {
			location <- state.labware_location_m.get(labware).asRs("labware has not been placed anywhere yet")
			site <- if (location.isInstanceOf[Site]) RsSuccess(location.asInstanceOf[Site]) else RsError("expected labware to be on a site")
			siteIdent <- protocol.eb.getIdent(site)
			siteE <- identToAgentObject_m.get(siteIdent).map(_.asInstanceOf[roboliq.evoware.parser.CarrierSite]).asRs("no evoware site corresponds to site")
		} yield siteE
	}
	
	/*private def setModelSites(model: EvowareLabwareModel, sites: List[CarrierSite]) {
		// The assignment of labware to sites is compatible with the current
		// table setup iff none of the sites have already been assigned to a different labware model.
		val isCompatible = sites.forall(site => {
			siteToModel_m.get(site) match {
				case None => true
				case Some(model) => true
				case _ => false
			}
		})
		if (!isCompatible) {
			endScript()
		}
		sites.foreach(site => siteToModel_m(site) = model)
	}*/
	
	private def setSiteModels(siteModel_l: List[(CarrierSite, EvowareLabwareModel)]) {
		// The assignment of labware to sites is compatible with the current
		// table setup iff none of the sites have already been assigned to a different labware model.
		val isCompatible = siteModel_l.forall(pair => {
			val (site, model) = pair
			siteToModel_m.get(site) match {
				case None => true
				case Some(model) => true
				case _ => false
			}
		})
		if (!isCompatible) {
			endScript()
		}
		siteModel_l.foreach(pair => siteToModel_m(pair._1) = pair._2)
	}
	
	private def endScript() {
		if (!cmds.isEmpty) {
			scriptIndex += 1
			val script = EvowareScript2(
				scriptIndex,
				siteToModel_m.toMap,
				cmds.toList
			)
			script_l += script
		}
		siteToModel_m.clear
		cmds.clear
	}

	def saveScripts(basename: String): RsResult[Unit] = {
		for ((script, index) <- script_l.zipWithIndex) {
			val filename = basename + (if (scriptIndex <= 1) "" else f"_$index%02d") + ".esc"
			logger.debug("filename: "+filename)
			saveWithHeader(script, filename)
		}
		RsSuccess(())
	}
	
	private def saveWithHeader(script: EvowareScript2, sFilename: String) {
		val siteToLabel_m = script.siteToModel_m.map(pair => {
			val (site, _) = pair
			val id0 = f"C${site.carrier.id}%03dS${site.iSite+1}"
			val label = config.config.siteIds.getOrElse(id0, id0)
			site -> label
		})
		val sHeader = config.table.toStringWithLabware(siteToLabel_m, script.siteToModel_m)
		val sCmds = script.cmd_l.mkString("\n")
		val fos = new java.io.FileOutputStream(sFilename)
		writeLines(fos, sHeader)
		writeLines(fos, sCmds);
		fos.close();
	}
	
	private def writeLines(output: java.io.FileOutputStream, s: String) {
		val as = s.split("\r?\n")
		for (sLine <- as if !s.isEmpty) {
			val bytes = sLine.map(_.asInstanceOf[Byte]).toArray
			output.write(bytes)
			output.write("\r\n".getBytes())
		}
	}
	
	private def shakerRun(
		protocol: Protocol,
		state0: WorldState,
		identToAgentObject_m: Map[String, Object],
		cmd: ShakerRun
	): RsResult[TranslationResult] = {
		for {
			specIdent <- protocol.eb.getIdent(cmd.spec)
			internal_s <- identToAgentObject_m.get(specIdent.toLowerCase).asRs(s"missing internal value for specIdent `$specIdent`")
		} yield {
			val token_l = {
				if (cmd.device.label == Some("MP 2Pos H+P Shake")) {
					List(
						L0C_Facts("HPShaker", "HPShaker_HP__ShakeForTime", "*271|8*30*30*30*30|2|*30|1|*30|1,5*30*30|255*27")
					)
				}
				else {
					List(
						L0C_Facts("Shaker", "Shaker_Init", ""),
						L0C_Facts("Shaker", "Shaker_Start","1"),
						L0C_StartTimer(1),
						L0C_WaitTimer(1, Integer.parseInt(internal_s.toString)),
						L0C_Facts("Shaker", "Shaker_Stop","")
					)
				}
			}
			val item_l = token_l.map(token => TranslationItem(token, Nil))
			TranslationResult(item_l, state0)
		}
	}
	
	private def aspirate(
		protocol: Protocol,
		state0: WorldState,
		identToAgentObject_m: Map[String, Object],
		twvp_l: List[TipWellVolumePolicy]
	): RqResult[TranslationResult] = {
		spirate(protocol, state0, identToAgentObject_m, twvp_l, "Aspirate")
	}
	
	private def dispense(
		protocol: Protocol,
		state0: WorldState,
		identToAgentObject_m: Map[String, Object],
		twvp_l: List[TipWellVolumePolicy]
	): RqResult[TranslationResult] = {
		spirate(protocol, state0, identToAgentObject_m, twvp_l, "Dispense")
	}

	private def spirate(
		protocol: Protocol,
		state0: WorldState,
		identToAgentObject_m: Map[String, Object],
		twvp_l: List[TipWellVolumePolicy],
		func_s: String
	): RqResult[TranslationResult] = {
		if (twvp_l.isEmpty) return RsSuccess(TranslationResult.empty(state0))
		
		// TODO: Track volumes aspirated, like here:
		/*for (item <- cmd.item_l) {
			val state = item.well.vesselState
			val sLiquid = state.content.liquid.id
			val mapWellToAspirated = builder.state.mapLiquidToWellToAspirated.getOrElse(sLiquid, new HashMap())
			val vol0 = mapWellToAspirated.getOrElseUpdate(item.well.id, LiquidVolume.empty)
			mapWellToAspirated(item.well.id) = vol0 + item.volume
			builder.state.mapLiquidToWellToAspirated(sLiquid) = mapWellToAspirated
		}*/
		for {
			// Get WellPosition and CarrierSite for each item
			tuple_l <- RsResult.toResultOfList(twvp_l.map { item =>
				for {
					wellPosition <- state0.getWellPosition(item.well)
					siteE <- getCarrierSite(protocol, state0, identToAgentObject_m, wellPosition.parent)
				} yield {
					(item, wellPosition, siteE)
				}
			})
			// Make sure that the items are all on the same site
			siteToItem_m = tuple_l.groupBy(_._3)
			_ <- RsResult.assert(siteToItem_m.size == 1, "aspirate command expected all items to be on the same carrier and site")
			// Get site and plate model (they're the same for all items)
			siteE = tuple_l.head._3
			plateModel = tuple_l.head._2.parentModel
			plateModelIdent <- protocol.eb.getIdent(plateModel)
			plateModelE <- identToAgentObject_m.get(plateModelIdent).map(_.asInstanceOf[roboliq.evoware.parser.EvowareLabwareModel]).asRs(s"could not find equivalent evoware labware model for $plateModel")
			// List of items and their well indexes
			item_l = tuple_l.map(tuple => tuple._1 -> tuple._2.index)
			// Check item validity and get liquid class
			sLiquidClass <- checkTipWellPolicyItems(protocol, state0, tuple_l.map(tuple => tuple._1 -> tuple._2))
			// Translate items into evoware commands
			result <- spirateChecked(protocol, state0, identToAgentObject_m, siteE, plateModelE, item_l, func_s, sLiquidClass)
		} yield result
	}
	
	//private def dispense(builder: EvowareScriptBuilder, cmd: pipette.low.DispenseToken): RqResult[Seq[L0C_Command]] = {
	//	checkTipWellPolicyItems(builder, cmd.items).flatMap(sLiquidClass => spirateChecked(builder, cmd.items, "Dispense", sLiquidClass))
	//}

	/** Return name of liquid class */
	private def checkTipWellPolicyItems(
		protocol: Protocol,
		state0: WorldState,
		item_l: List[(HasTip with HasWell with HasPolicy, WellPosition)]
	): RqResult[String] = {
		item_l match {
			case Seq() => RqError("INTERNAL: items empty")
			case Seq(item0, rest @ _*) =>
				// Get the liquid class
				val policy = item0._1.policy
				// Assert that there is only one liquid class
				// FIXME: for debug only:
				//if (!rest.forall(twvp => twvp.policy.equals(policy))) {
				//	println("sLiquidClass: " + policy)
				//	rest.foreach(twvp => println(twvp.tip, twvp.policy))
				//}
				// ENDFIX
				if (!rest.forall(twvp => twvp._1.policy.equals(policy))) {
					return RqError("INTERNAL: policy should be the same for all spirate items: "+item_l)
				}
				
				// Assert that all tips are of the same kind
				// TODO: Re-add this error check somehow? -- ellis, 2011-08-25
				//val tipKind = config.getTipKind(twvp0.tip)
				//assert(items.forall(twvp => robot.getTipKind(twvp.tip) eq tipKind))
				
				// All tip/well pairs are equidistant or all tips are going to the same well
				val bEquidistant = TipWell.equidistant(item_l)
				val bSameWell = item_l.forall(item => item._1.well eq item0._1.well)
				if (!bEquidistant && !bSameWell)
					return RqError("INTERNAL: not equidistant, "+item_l.map(_._1.tip.index)+" -> "+item_l.map(_._2.index))
				
				RqSuccess(policy.id)
		}
	}

	private def spirateChecked(
		protocol: Protocol,
		state0: WorldState,
		identToAgentObject_m: Map[String, Object],
		siteE: CarrierSite,
		labwareModelE: EvowareLabwareModel,
		item_l: List[(TipWellVolumePolicy, Int)],
		sFunc: String,
		sLiquidClass: String
	): RqResult[TranslationResult] = {
		val tip_l = item_l.map(_._1.tip)
		val well_li = item_l.map(_._2)
		
		val mTips = encodeTips(tip_l)
		
		// Create a list of volumes for each used tip, leaving the remaining values at 0
		val asVolumes = Array.fill(12)("0")
		val fmt = new java.text.DecimalFormat("#.##")
		for (item <- item_l) {
			val iTip = item._1.tip.index
			assert(iTip >= 0 && iTip < 12)
			asVolumes(iTip) = ("\""+fmt.format(item._1.volume.ul.toDouble)+'"').replace(',', '.') // REFACTOR: rather than replacing ',' with '.', make sure that US locale is used for formatting
		}

		val iGrid = config.table.mapCarrierToGrid(siteE.carrier)
		val sPlateMask = encodeWells(labwareModelE.nRows, labwareModelE.nCols, well_li)
		//logger.debug("well_li: "+well_li)

		for {
			_ <- RsResult.zero
		} yield {
			val cmd = L0C_Spirate(
				sFunc, 
				mTips, sLiquidClass,
				asVolumes,
				iGrid, siteE.iSite,
				sPlateMask,
				siteE, labwareModelE
			)
			
			val state1 = sFunc match {
				case "Aspirate" =>
					item_l.foldLeft(state0) { (state, item) =>
						val amount = Distribution.fromVolume(item._1.volume)
						val aliquot0 = state.well_aliquot_m(item._1.well)
						val aliquot1 = aliquot0.remove(amount).getOrElse(aliquot0) // FIXME: handle the error instead of using getOrElse
						val state1 = state.copy(
							well_aliquot_m = state.well_aliquot_m + (item._1.well -> aliquot1)
						)
						state1
					}
				case "Dispense" => state0
			}
			
			TranslationResult(
				List(TranslationItem(cmd, List(siteE -> labwareModelE))),
				state1
			)
		}
	}

	private def pipetterTipsRefresh(
		protocol: Protocol,
		state0: WorldState,
		identToAgentObject_m: Map[String, Object],
		cmd: PipetterTipsRefresh
	): RqResult[TranslationResult] = {
		pipetterTipsRefresh_BSSE(protocol, state0, identToAgentObject_m, cmd)
	}

	// FIXME: This is a BSSE-specific HACK.  The function for handling this command should be loaded from a config file.
	private def pipetterTipsRefresh_BSSE(
		protocol: Protocol,
		state0: WorldState,
		identToAgentObject_m: Map[String, Object],
		cmd: PipetterTipsRefresh
	): RqResult[TranslationResult] = {
		val tipAll_l = protocol.eb.pipetterToTips_m.getOrElse(cmd.device, cmd.item_l.map(_._1))
		val tip1000_l = tipAll_l.filter(_.index < 4)
		val tip0050_l = tipAll_l.filter(_.index >= 4)
		val item1000_l = cmd.item_l.filter(_._1.index < 4)
		val item0050_l = cmd.item_l.filter(_._1.index >= 4)
		
		val state = state0.toMutable
		def doit(item_l: List[(Tip, CleanIntensity.Value, Option[TipModel])], tip_l: List[Tip], suffix: String): Option[TranslationItem] = {
			item_l match {
				case Nil => None
				case item0 :: rest =>
					val intensity: CleanIntensity.Value = rest.foldLeft(item0._2){(acc, item) => CleanIntensity.max(acc, item._2)}
					val intensity_s_? = intensity match {
						case CleanIntensity.None => None
						case CleanIntensity.Flush => Some("Light")
						case CleanIntensity.Light => Some("Light")
						case CleanIntensity.Thorough => Some("Thorough")
						case CleanIntensity.Decontaminate => Some("Decontaminate")
					}
					intensity_s_?.map { intensity_s =>
						// Set tip state to clean for the four tips that are being washed
						tip_l.foreach { tip =>
							val tipState0 = state.tip_state_m.getOrElse(tip, TipState.createEmpty(tip))
							val event = TipCleanEvent(tip, intensity)
							state.tip_state_m(tip) = new TipCleanEventHandler().handleEvent(tipState0, event).toOption.get
						}
						// Call the appropriate subroutine for cleaning
						val path = """C:\Program Files\TECAN\EVOware\database\scripts\Roboliq\Roboliq_Clean_"""+intensity_s+"_"+suffix+".esc"
						TranslationItem(L0C_Subroutine(path), Nil)
					}
			}
		}
		
		val translationItem_l = List(doit(item1000_l, tip1000_l, "1000"), doit(item0050_l, tip0050_l, "0050")).flatten
		RsSuccess(TranslationResult(translationItem_l, state.toImmutable))
	}

	// FIXME: This is a WIS-specific HACK.  The function for handling this command should be loaded from a config file.
	private def pipetterTipsRefresh_WIS(
		protocol: Protocol,
		state0: WorldState,
		identToAgentObject_m: Map[String, Object],
		cmd: PipetterTipsRefresh
	): RqResult[TranslationResult] = {
		val tipAll_l = protocol.eb.pipetterToTips_m.getOrElse(cmd.device, cmd.item_l.map(_._1)).toSet
		val tipAll_m = encodeTips(tipAll_l)
		
		val state = state0.toMutable
		def doit(item_l: List[(Tip, CleanIntensity.Value, Option[TipModel])]): List[TranslationItem] = {
			item_l match {
				case Nil => Nil
				case item0 :: rest =>
					val tip_l = item_l.map(_._1).toSet
					//tip_l.foreach(tip => println("state.tip_model_m(tip): "+state.tip_model_m.get(tip)))
					val intensity: CleanIntensity.Value = rest.foldLeft(item0._2){(acc, item) => CleanIntensity.max(acc, item._2)}
					val tipModel_? : Option[TipModel] = rest.foldLeft(item0._3){(acc, item) => acc.orElse(item._3)}
					val tipState_l = tip_l.map(tip => state.tip_state_m.getOrElse(tip, TipState.createEmpty(tip)))
					
					// If tips are currently attached and either the cleanIntensity >= Thorough or we're changing tip models, then drop old tips
					val tipDrop1_l = item_l.filter(tuple => {
						val (tip, _, tipModel_?) = tuple
						val tipState = state.tip_state_m.getOrElse(tip, TipState.createEmpty(tip))
						//println("stuff:", tip, tipState, (intensity >= CleanIntensity.Thorough), tipState.model_? != tipModel_?)
						(tipState.model_?.isDefined && (intensity >= CleanIntensity.Thorough || tipState.model_? != tipModel_?))
					}).map(_._1)
					// Also dropped any tips which weren't mentioned but are attached
					val tipDrop2_l = tipAll_l.filter(tip => {
						val tipState = state.tip_state_m.getOrElse(tip, TipState.createEmpty(tip))
						!tip_l.contains(tip) && tipState.model_?.isDefined
					})
					// If we need to drop any tips, drop all of them
					val tipDrop_l: Set[Tip] = {
						if (!tipDrop2_l.isEmpty || !tipDrop1_l.isEmpty)
							tipAll_l.filter(tip => {
								val tipState = state.tip_state_m.getOrElse(tip, TipState.createEmpty(tip))
								tipState.model_?.isDefined
							})
						else
							Set()
					}
					val tipDrop_m = encodeTips(tipDrop_l)
					
					// If we need new tips and either didn't have any before or are dropping our previous ones
					val tipGet_l = item_l.filter(tuple => {
						val (tip, _, tipModel_?) = tuple
						val tipState = state.tip_state_m.getOrElse(tip, TipState.createEmpty(tip))
						(tipModel_?.isDefined && (tipState.model_? == None || tipDrop_l.contains(tip)))
					}).map(_._1).toSet
					val tipGet_m = encodeTips(tipGet_l)
				
					// If tip state has no clean state, do a pre-wash
					val prewash_b = tipGet_m > 0 && tipState_l.exists(_.cleanDegreePrev == CleanIntensity.None)
					logger.debug(("prewash_b:", prewash_b, tipGet_m > 0, tipState_l.map(s => (s.conf.index, s.cleanDegreePrev))))
					
					val token_ll = List[List[L0C_Command]](
						if (tipDrop_m > 0) List(L0C_DropDITI(tipDrop_m, 1, 6)) else Nil,
						if (prewash_b) {
							List(
								L0C_Wash(tipAll_m, 1,1,1,0,50,500,1,500,20,70,30,true,true),
								L0C_Wash(tipAll_m,1,1,1,0,4,500,1,500,20,70,30,false,true)
							)
						} else Nil,
						if (tipGet_m > 0) {
							List(
								L0C_Wash(tipAll_m,1,1,1,0,2.0,500,1.0,500,20,70,30,true,true),
								L0C_GetDITI2(tipGet_m, tipModel_?.get.key)
							)
						} else Nil
					)
					
					// Update tip states
					val tipClean_l = if (prewash_b) tipAll_l else tipGet_l ++ tipDrop_l
					val tipToModel_l: List[(Tip, Option[TipModel])] = (tipDrop_l.toList.map(_ -> None) ++ tipGet_l.toList.map(_ -> tipModel_?))
					for (tip <- tipClean_l) {
						val tipState0 = state.tip_state_m.getOrElse(tip, TipState.createEmpty(tip))
						val event = TipCleanEvent(tip, CleanIntensity.Decontaminate)
						state.tip_state_m(tip) = new TipCleanEventHandler().handleEvent(tipState0, event).toOption.get
					}
					for ((tip, tipModel_?) <- tipToModel_l) {
						val tipState0 = state.tip_state_m.getOrElse(tip, TipState.createEmpty(tip))
						state.tip_state_m(tip) = tipState0.copy(model_? = tipModel_?)
					}

					val x = token_ll.flatten.map(token => TranslationItem(token, Nil))
					logger.debug("x: "+x)
					logger.debug()
					x
			}
		}
		
		val translationItem_l = doit(cmd.item_l)
		RsSuccess(TranslationResult(translationItem_l, state.toImmutable))
	}
	
	private def encode(n: Int): Char = ('0' + n).asInstanceOf[Char]
	private def hex(n: Int): Char = Integer.toString(n, 16).toUpperCase.apply(0)
	
	protected def encodeHasTips(list: Iterable[HasTip]): Int =
		list.foldLeft(0) { (sum, x) => sum | (1 << x.tip.index) }
	protected def encodeTips(list: Iterable[Tip]): Int =
		list.foldLeft(0) { (sum, tip) => sum | (1 << tip.index) }

	protected def encodeWells(rows: Int, cols: Int, well_li: Traversable[Int]): String = {
		//println("encodeWells:", holder.nRows, holder.nCols, aiWells)
		val nWellMaskChars = math.ceil(rows * cols / 7.0).asInstanceOf[Int]
		val amWells = new Array[Int](nWellMaskChars)
		for (well_i <- well_li) {
			val iChar = well_i / 7;
			val iWell1 = well_i % 7;
			// FIXME: for debug only
			if (iChar >= amWells.size)
				println("ERROR: encodeWells: "+(rows, cols, well_i, iChar, iWell1, well_li))
			// ENDFIX
			amWells(iChar) += 1 << iWell1
		}
		val sWellMask = amWells.map(encode).mkString
		val sPlateMask = f"$cols%02X$rows%02X" + sWellMask
		sPlateMask
	}
	
	private def siteLabwareEntry(
		protocol: Protocol,
		state0: WorldState,
		identToAgentObject_m: Map[String, Object],
		siteIdent: String,
		labwareIdent: String
	): RsResult[Option[(CarrierSite, EvowareLabwareModel)]] = {
		for {
			labware <- protocol.eb.getEntityByIdent[Labware](labwareIdent)
			model <- protocol.eb.labwareToModel_m.get(labware).asRs(s"missing model for labware `$labwareIdent`")
			modelIdent <- protocol.eb.getIdent(model)
		} yield {
			val modelE_? = identToAgentObject_m.get(modelIdent).map(_.asInstanceOf[EvowareLabwareModel])
			val siteE_? = identToAgentObject_m.get(siteIdent).map(_.asInstanceOf[CarrierSite])

			(siteE_?, modelE_?) match {
				case (Some(siteE), Some(modelE)) => Some(siteE -> modelE)
				case _ => None
			}
		}
	}
	
	private def siteLabwareEntry(
		protocol: Protocol,
		state0: WorldState,
		identToAgentObject_m: Map[String, Object],
		site: Site,
		labware: Labware
	): RsResult[Option[(CarrierSite, EvowareLabwareModel)]] = {
		for {
			siteIdent <- protocol.eb.getIdent(site)
			model <- protocol.eb.labwareToModel_m.get(labware).asRs(s"missing model for labware `$labware`")
			modelIdent <- protocol.eb.getIdent(model)
		} yield {
			val modelE_? = identToAgentObject_m.get(modelIdent).map(_.asInstanceOf[EvowareLabwareModel])
			val siteE_? = identToAgentObject_m.get(siteIdent).map(_.asInstanceOf[CarrierSite])

			(siteE_?, modelE_?) match {
				case (Some(siteE), Some(modelE)) => Some(siteE -> modelE)
				case _ => None
			}
		}
	}
}