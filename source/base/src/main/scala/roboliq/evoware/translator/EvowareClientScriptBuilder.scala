package roboliq.evoware.translator

import scala.Array.canBuildFrom
import scala.Option.option2Iterable
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import grizzled.slf4j.Logger
import roboliq.commands.AgentActivate
import roboliq.commands.AgentDeactivate
import roboliq.commands.Log
import roboliq.commands.PeelerRun
import roboliq.commands.PipetterAspirate
import roboliq.commands.PipetterDispense
import roboliq.commands.PipetterTipsRefresh
import roboliq.commands.Prompt
import roboliq.commands.SealerRun
import roboliq.commands.ShakerRun
import roboliq.commands.ThermocyclerClose
import roboliq.commands.ThermocyclerOpen
import roboliq.commands.ThermocyclerRun
import roboliq.commands.TransporterRun
import roboliq.core.RqResult
import roboliq.core.RsResult
import roboliq.core.RsSuccess
import roboliq.entities.Aliquot
import roboliq.entities.CleanIntensity
import roboliq.entities.ClientScriptBuilder
import roboliq.entities.Distribution
import roboliq.entities.HasPolicy
import roboliq.entities.HasTip
import roboliq.entities.HasWell
import roboliq.entities.Labware
import roboliq.entities.PipettePosition
import roboliq.entities.Site
import roboliq.entities.Thermocycler
import roboliq.entities.Tip
import roboliq.entities.TipAspirateEvent
import roboliq.entities.TipAspirateEventHandler
import roboliq.entities.TipCleanEvent
import roboliq.entities.TipCleanEventHandler
import roboliq.entities.TipDispenseEvent
import roboliq.entities.TipDispenseEventHandler
import roboliq.entities.TipModel
import roboliq.entities.TipState
import roboliq.entities.TipWell
import roboliq.entities.TipWellVolumePolicy
import roboliq.entities.WellPosition
import roboliq.entities.WorldState
import roboliq.evoware.parser.Carrier
import roboliq.evoware.parser.CarrierSite
import roboliq.evoware.parser.EvowareLabwareModel
import roboliq.input.Context
import roboliq.input.Instruction
import roboliq.commands.DeviceSiteOpen
import roboliq.entities.Device
import roboliq.commands.DeviceSiteClose
import roboliq.commands.ReaderRun
import org.apache.commons.io.FileUtils
import java.io.File
import roboliq.evoware.commands.EvowareInstruction
import roboliq.evoware.commands.EvowareSubroutine
import roboliq.input.DeviceInstruction
import roboliq.evoware.handler.EvowareDeviceInstructionHandler

case class EvowareScript2(
	index: Int,
	siteToModel_m: Map[CarrierSite, EvowareLabwareModel],
	cmd_l: List[L0C_Command]
)

case class TranslationItem(
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
	import EvowareDeviceInstructionHandler._
	
	private val logger = Logger[this.type]

	val script_l = new ArrayBuffer[EvowareScript2]
	private var scriptIndex: Int = 0
	//val builder = new EvowareScriptBuilder
	// REFACTOR: Rename to token_l
	val cmds = new ArrayBuffer[L0C_Command]
	//val mapCmdToLabwareInfo = new HashMap[Object, List[(CarrierSite, EvowareLabwareModel)]]
	val siteToModel_m = new HashMap[CarrierSite, EvowareLabwareModel]
	
	def addCommand(
		agentIdent: String,
		command: Instruction
	): Context[Unit] = {
		logger.debug(s"addCommand: $agentIdent, $command")
		for {
			item_l <- {
				if (agentIdent == "user")
					addCommandUser(agentIdent, command)
				else
					addCommandRobot(agentIdent, command)
			}
		} yield {
			handleTranslationResult(item_l)
		}
	}

	private def addCommandRobot(
		agentIdent: String,
		command: Instruction
	): Context[List[TranslationItem]] = {
		for {
			data0 <- Context.get
			identToAgentObject_m: Map[String, Object] = data0.protocol.agentToIdentToInternalObject.get(agentIdent).map(_.toMap).getOrElse(Map())
			ret <- addCommandRobot(agentIdent, command, identToAgentObject_m)
		} yield ret
	}
	
	private def addCommandRobot(
		agentIdent: String,
		command: Instruction,
		identToAgentObject_m: Map[String, Object]
	): Context[List[TranslationItem]] = {
		def getAgentObject[A](ident: String, error: => String): Context[A] = {
			Context.from(identToAgentObject_m.get(ident).map(_.asInstanceOf[A]), error)
		}
		for {
			x <- command match {
				case AgentActivate() => Context.unit(Nil)
				case AgentDeactivate() => Context.unit(Nil)

				case deviceInstruction: DeviceInstruction =>
					val device = deviceInstruction.device
					for {
						deviceIdent <- Context.getEntityIdent(device)
						handler <- getAgentObject[EvowareDeviceInstructionHandler](deviceIdent, s"missing instruction handler for device `${device.getName}`")
						item_l <- handler.handleInstruction(command, identToAgentObject_m)
					} yield item_l
					
				case Log(text) =>
					val item = TranslationItem(L0C_Comment(text), Nil)
					Context.unit(List(item))
				
				case EvowareInstruction(l0c) =>
					val item = TranslationItem(l0c, Nil)
					Context.unit(List(item))
					
				case EvowareSubroutine(path) =>
					val item = TranslationItem(L0C_Subroutine(path), Nil)
					Context.unit(List(item))
					
				/*case cmd: PeelerRun =>
					for {
						deviceIdent <- Context.getEntityIdent(cmd.device)
						carrierE <- getAgentObject[Carrier](deviceIdent, s"missing evoware carrier for device `${deviceIdent}`")
						filepath <- getAgentObject[String](cmd.specIdent, s"missing evoware data for spec `${cmd.specIdent}`")
						// List of site/labware mappings for those labware and sites which evoware has equivalences for
						siteToModel_l <- siteLabwareEntry(identToAgentObject_m, cmd.siteIdent, cmd.labwareIdent).map(_.toList)
					} yield {
						// Token
						val token = L0C_Facts(carrierE.sName, carrierE.sName+"_Peel", filepath)
						// Return
						val item = TranslationItem(token, siteToModel_l)
						List(item)
					}*/
				
				case Prompt(text) =>
					val item = TranslationItem(L0C_Prompt(text), Nil)
					Context.unit(List(item))
					
				case cmd: PipetterAspirate =>
					aspirate(identToAgentObject_m, cmd.item_l)
					
				case cmd: PipetterDispense =>
					dispense(identToAgentObject_m, cmd.item_l)
					
				case cmd: PipetterTipsRefresh =>
					pipetterTipsRefresh(identToAgentObject_m, cmd)
					
				case cmd: ReaderRun =>
					for {
						deviceIdent <- Context.getEntityIdent(cmd.device)
						handler <- getAgentObject[EvowareDeviceInstructionHandler](deviceIdent, s"missing instruction handler for device `${cmd.device.getName}`")
						item_l <- handler.handleInstruction(command, identToAgentObject_m)
					} yield item_l
					
				case cmd: SealerRun =>
					sealerRun(identToAgentObject_m, cmd)
				
				case cmd: ShakerRun =>
					shakerRun(identToAgentObject_m, cmd)
				
				case cmd: ThermocyclerClose =>
					for {
						device <- Context.getEntityAs[Thermocycler](cmd.deviceIdent)
						carrierE <- getAgentObject[Carrier](cmd.deviceIdent, s"missing evoware carrier for device `${cmd.deviceIdent}`")
						// Update state
						_ <- Context.modifyStateBuilder(_.device_isOpen_l -= device)
					} yield {
						val token = L0C_Facts(carrierE.sName, carrierE.sName+"_LidClose", "")
						val item = TranslationItem(token, Nil)
						List(item)
					}
				
				case cmd: ThermocyclerOpen =>
					for {
						device <- Context.getEntityAs[Thermocycler](cmd.deviceIdent)
						carrierE <- getAgentObject[Carrier](cmd.deviceIdent, s"missing evoware carrier for device `${cmd.deviceIdent}`")
						// Update state
						_ <- Context.modifyStateBuilder(_.device_isOpen_l += device)
					} yield {
						val token = L0C_Facts(carrierE.sName, carrierE.sName+"_LidOpen", "")
						val item = TranslationItem(token, Nil)
						List(item)
					}
				
				case cmd: ThermocyclerRun =>
					for {
						device <- Context.getEntityAs[Thermocycler](cmd.deviceIdent)
						carrierE <- getAgentObject[Carrier](cmd.deviceIdent, s"missing evoware carrier for device `${cmd.deviceIdent}`")
						value <- getAgentObject[String](cmd.specIdent, s"missing evoware data for spec `${cmd.specIdent}`")
					} yield {
						val token = L0C_Facts(carrierE.sName, carrierE.sName+"_RunProgram", value)
						val item = TranslationItem(token, Nil)
						List(item)
					}
	
				case TransporterRun(deviceIdent, labware, model, origin, destination, vectorIdent) =>
					// REFACTOR: lots of duplication with TransporterRun for user
					for {
						modelIdent <- Context.getEntityIdent(model)
						labwareModel_? = identToAgentObject_m.get(modelIdent).map(_.asInstanceOf[roboliq.evoware.parser.EvowareLabwareModel])
						originIdent <- Context.getEntityIdent(origin)
						originE <- Context.from(identToAgentObject_m.get(originIdent).map(_.asInstanceOf[roboliq.evoware.parser.CarrierSite]), s"missing agent data for `$originIdent`")
						destinationIdent <- Context.getEntityIdent(destination)
						destinationE <- Context.from(identToAgentObject_m.get(destinationIdent).map(_.asInstanceOf[roboliq.evoware.parser.CarrierSite]), s"missing agent data for `$originIdent`")
						// Move labware to new location
						_ <- Context.modifyStateBuilder(_.labware_location_m(labware) = destination)
					} yield {
						val cmd = {
							val roma_i: Int = identToAgentObject_m(deviceIdent).asInstanceOf[Integer]
							val model = identToAgentObject_m(modelIdent).asInstanceOf[roboliq.evoware.parser.EvowareLabwareModel]
							val vectorClass = identToAgentObject_m.getOrElse(vectorIdent, vectorIdent).toString
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
					
						// List of site/labware mappings for those labware and sites which evoware has equivalences for
						val siteToModel_l = labwareModel_? match {
							case None => Nil
							case Some(labwareModel) => List(originE, destinationE).map(_ -> labwareModel)
						}
						
						// Finish up
						val item = TranslationItem(cmd, siteToModel_l)
						List(item)
					}
					
				case _ =>
					for {
						_ <- Context.error(s"unknown command `$command`")
					} yield Nil
			}
		} yield x
	}

	private def addCommandUser(
		agentIdent: String,
		command: Instruction
	): Context[List[TranslationItem]] = {
		for {
			data0 <- Context.get
			identToAgentObject_m: Map[String, Object] = data0.protocol.agentToIdentToInternalObject.get(agentIdent).map(_.toMap).getOrElse(Map())
			ret <- addCommandUser(agentIdent, command, identToAgentObject_m)
		} yield ret
	}
	
	private def addCommandUser(
		agentIdent: String,
		command: Instruction,
		identToAgentObject_m: Map[String, Object]
	): Context[List[TranslationItem]] = {
		def promptUnknown(): Context[List[TranslationItem]] = {
			Context.unit(List(TranslationItem(L0C_Prompt(s"Please perform this command: $command"), Nil)))
		}
		
		def prompt(text: String, state1: WorldState): Context[List[TranslationItem]] = {
			val item = TranslationItem(L0C_Prompt(text), Nil)
			for {
				_ <- Context.modify(_.setState(state1))
			} yield List(item)
		}
		
		command match {
			case AgentActivate() => Context.unit(Nil)
			case AgentDeactivate() => Context.unit(Nil)
			
			case Log(text) =>
				Context.unit(List(TranslationItem(L0C_Comment(text), Nil)))
			
			case Prompt(text) =>
				Context.unit(List(TranslationItem(L0C_Prompt(text), Nil)))

			case TransporterRun(deviceIdent, labware, model, origin, destination, vectorIdent) =>
				for {
					modelIdent <- Context.getEntityIdent(model)
					labwareModel_? = identToAgentObject_m.get(modelIdent).map(_.asInstanceOf[roboliq.evoware.parser.EvowareLabwareModel])
					originIdent <- Context.getEntityIdent(origin)
					originE <- Context.from(identToAgentObject_m.get(originIdent).map(_.asInstanceOf[roboliq.evoware.parser.CarrierSite]), s"missing agent data for `$originIdent`")
					destinationIdent <- Context.getEntityIdent(destination)
					destinationE <- Context.from(identToAgentObject_m.get(destinationIdent).map(_.asInstanceOf[roboliq.evoware.parser.CarrierSite]), s"missing agent data for `$originIdent`")
					// Move labware to new location
					_ <- Context.modifyStateBuilder(_.labware_location_m(labware) = destination)
				} yield ()
				for {
					labwareIdent <- Context.getEntityIdent(labware)
					modelIdent <- Context.getEntityIdent(model)
					originIdent <- Context.getEntityIdent(origin)
					destinationIdent <- Context.getEntityIdent(destination)
					// Move labware to new location
					_ <- Context.modifyStateBuilder(_.labware_location_m(labware) = destination)
				} yield {
					val modelLabel = model.label.getOrElse(model.key)
					val originLabel = origin.label.getOrElse(origin.key)
					val destinationLabel = destination.label.getOrElse(destination.key)
					val text = s"Please move labware `${labwareIdent}` model `${modelLabel}` from `${originLabel}` to `${destinationLabel}`"

					// List of site/labware mappings for those labware and sites which evoware has equivalences for
					val labwareModel_? = identToAgentObject_m.get(modelIdent).map(_.asInstanceOf[roboliq.evoware.parser.EvowareLabwareModel])
					val originE_? = identToAgentObject_m.get(originIdent).map(_.asInstanceOf[roboliq.evoware.parser.CarrierSite])
					val destinationE_? = identToAgentObject_m.get(destinationIdent).map(_.asInstanceOf[roboliq.evoware.parser.CarrierSite])
					val siteToModel_l = labwareModel_? match {
						case None => Nil
						case Some(labwareModel) => List(originE_?, destinationE_?).flatten.map(_ -> labwareModel)
					}
					
					// Finish up
					List(TranslationItem(L0C_Prompt(text), siteToModel_l))
				}
				
			case cmd: PipetterAspirate => promptUnknown()
			case cmd: PipetterDispense => promptUnknown()
			case cmd: SealerRun => promptUnknown()
				
			case _ => promptUnknown()
		}
	}

	def end(): RsResult[Unit] = {
		endScript()
		RsSuccess(())
	}
	
	private def handleTranslationResult(item_l: List[TranslationItem]) {
		for (item <- item_l) {
			setSiteModels(item.siteToModel_l)
			cmds += item.token
		}
	}
	
	private def getCarrierSite(
		identToAgentObject_m: Map[String, Object],
		labware: Labware
	): Context[CarrierSite] = {
		// TODO: Allow for labware to be on top of other labware (e.g. stacked plates), and figure out the proper site index
		// TODO: For tubes, the site needs to map to a well on an evoware-labware at a site
		// TODO: What we actually need to do here is get the chain of labware (labware may be on other labware) until we reach a 
		// labware which has an evoware equivalent -- for example, we consider tubes to be labware, but evoware only considers the
		// tube adapter to be labware.
		//def makeLocationChain(labware: Labware, acc: List[])
		for {
			location <- Context.getsOption(_.state.labware_location_m.get(labware), "labware has not been placed anywhere yet")
			_ <- Context.assert(location.isInstanceOf[Site], "expected labware to be on a site")
			site = location.asInstanceOf[Site]
			siteIdent <- Context.getEntityIdent(site)
			siteE <- getAgentObject[CarrierSite](identToAgentObject_m, siteIdent, s"no evoware site corresponds to site: $site")
		} yield siteE
	}
	
	def getAgentObject[A](
		identToAgentObject_m: Map[String, Object],
		ident: String,
		error: => String
	): Context[A] = {
		Context.from(identToAgentObject_m.get(ident).map(_.asInstanceOf[A]), error)
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

	def generateScripts(basename: String): RsResult[List[(String, Array[Byte])]] = {
		val l = script_l.toList.zipWithIndex.map { case (script, index) =>
			val filename = basename + (if (scriptIndex <= 1) "" else f"_$index%02d") + ".esc"
			logger.debug("generateScripts: filename: "+filename)
			filename -> generateWithHeader(script)
		}
		RsSuccess(l)
	}
	
	private def generateWithHeader(script: EvowareScript2): Array[Byte] = {
		val siteToLabel_m = script.siteToModel_m.map(pair => {
			val (site, _) = pair
			val id0 = f"C${site.carrier.id}%03dS${site.iSite+1}"
			val label = config.config.siteIds.getOrElse(id0, id0)
			site -> label
		})
		val sHeader = config.table.toStringWithLabware(siteToLabel_m, script.siteToModel_m)
		val sCmds = script.cmd_l.mkString("\n")
		val os = new java.io.ByteArrayOutputStream()
		writeLines(os, sHeader)
		writeLines(os, sCmds);
		os.toByteArray()
	}
	
	private def writeLines(output: java.io.OutputStream, s: String) {
		val as = s.split("\r?\n")
		for (sLine <- as if !s.isEmpty) {
			val bytes = sLine.map(_.asInstanceOf[Byte]).toArray
			output.write(bytes)
			output.write("\r\n".getBytes())
		}
	}
	
	private def sealerRun(
		identToAgentObject_m: Map[String, Object],
		cmd: SealerRun
	): Context[List[TranslationItem]] = {
		def getAgentObject[A](ident: String, error: => String): Context[A] = {
			Context.from(identToAgentObject_m.get(ident).map(_.asInstanceOf[A]), error)
		}
		for {
			deviceIdent <- Context.getEntityIdent(cmd.device)
			carrierE <- getAgentObject[Carrier](deviceIdent, s"missing evoware carrier for device `${deviceIdent}`")
			model <- cmd.labwareToSite_l match {
				case (labware, site) :: Nil => Context.getLabwareModel(labware)
				case _ => Context.error("must supply exactly one labware to seal")
			}
			filepath <- cmd.program_? match {
				case Some(program) =>
					program.filename match {
						case Some(filename) => Context.unit(filename)
						case None => Context.error("a filename must be specified for the sealer program")
					}
				case None =>
					val filename_? = config.sealerProgram_l.find(program => program.model eq model).map(_.filename)
					Context.from(filename_?, s"unable to find a sealer program for labware model `${model.getName}`")
			}
			// List of site/labware mappings for those labware and sites which evoware has equivalences for
			siteToModel_l <- Context.mapFirst(cmd.labwareToSite_l) { case (labware, site) =>
				for {
					labwareIdent <- Context.getEntityIdent(labware)
					siteIdent <- Context.getEntityIdent(site)
					siteToModel <- siteLabwareEntry(identToAgentObject_m, siteIdent, labwareIdent)
				} yield siteToModel
			}
		} yield {
			// Token
			val token = L0C_Facts(carrierE.sName, carrierE.sName+"_Seal", filepath)
			// Return
			val item = TranslationItem(token, siteToModel_l.flatten)
			List(item)
		}
	}
	
	private def shakerRun(
		identToAgentObject_m: Map[String, Object],
		cmd: ShakerRun
	): Context[List[TranslationItem]] = {
		if (cmd.device.label == Some("MP 2Pos H+P Shake")) {
			for {
				duration <- Context.from(cmd.spec.duration, s"Shaker program must specify `duration`")
				rpm <- Context.from(cmd.spec.rpm, s"Shaker program must specify `rpm`")
			} yield {
				// FIXME: need to find shaker index, in case there are multiple shakers
				// FIXME: if there are multiple shakers, but they don't have unique indexes specified in a config file, then issue an error here
				val shakerNo = 1
				// FIXME: Let the user specify mode1, steps1, mode2, steps2, power
				val mode1 = 2
				val steps1 = 0
				val mode2 = 1
				val steps2 = 0
				val cycles: Int = rpm * duration / 60
				val power = 50
				val s0 = s"*27${shakerNo}|${60000000/rpm}|${mode1}|${steps1}|${mode2}|${steps2}|${cycles}|${255*power/100}*27"
				// Replace all occurences of '0' with "*30"
				val s1 = s0.replace("0", "*30")
				// Finally, split string into 32 char parts, then rebind them, separated by commas
				val s2 = s1.grouped(32).mkString(",")

				val token_l = List(
					L0C_Facts("HPShaker", "HPShaker_HP__ShakeForTime", s2)
				)
				token_l.map(token => TranslationItem(token, Nil))
			}	
		}
		else {
			for {
				duration <- Context.from(cmd.spec.duration, s"Shaker program must specify `duration`")
			} yield {
				val token_l = List(
					L0C_Facts("Shaker", "Shaker_Init", ""),
					// FIXME: need to find shaker index, in case there are multiple shakers
					// FIXME: if there are multiple shakers, but they don't have unique indexes specified in a config file, then issue an error here
					L0C_Facts("Shaker", "Shaker_Start", "1"),
					// FIXME: need to keep track of timers in use so that we don't take a timer that's being used
					L0C_StartTimer(1),
					L0C_WaitTimer(1, duration),
					L0C_Facts("Shaker", "Shaker_Stop","")
				)
				token_l.map(token => TranslationItem(token, Nil))
			}
		}
	}
	
	private def aspirate(
		identToAgentObject_m: Map[String, Object],
		twvp_l: List[TipWellVolumePolicy]
	): Context[List[TranslationItem]] = {
		spirate(identToAgentObject_m, twvp_l, "Aspirate")
	}
	
	private def dispense(
		identToAgentObject_m: Map[String, Object],
		twvp_l: List[TipWellVolumePolicy]
	): Context[List[TranslationItem]] = {
		spirate(identToAgentObject_m, twvp_l, "Dispense")
	}

	private def spirate(
		identToAgentObject_m: Map[String, Object],
		twvp_l: List[TipWellVolumePolicy],
		func_s: String
	): Context[List[TranslationItem]] = {
		if (twvp_l.isEmpty) return Context.unit(Nil)
		
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
			tuple_l <- Context.mapFirst(twvp_l) { item =>
				for {
					wellPosition <- Context.getsResult(_.state.getWellPosition(item.well))
					siteE <- getCarrierSite(identToAgentObject_m, wellPosition.parent)
				} yield {
					(item, wellPosition, siteE)
				}
			}
			// Make sure that the items are all on the same site
			siteToItem_m = tuple_l.groupBy(_._3)
			_ <- Context.assert(siteToItem_m.size == 1, "aspirate command expected all items to be on the same carrier and site")
			// Get site and plate model (they're the same for all items)
			siteE = tuple_l.head._3
			plateModel = tuple_l.head._2.parentModel
			plateModelIdent <- Context.getEntityIdent(plateModel)
			plateModelE <- getAgentObject[roboliq.evoware.parser.EvowareLabwareModel](identToAgentObject_m, plateModelIdent, s"could not find equivalent evoware labware model for $plateModel")
			// List of items and their well indexes
			item_l = tuple_l.map(tuple => tuple._1 -> tuple._2.index)
			// Check item validity and get liquid class
			pair <- checkTipWellPolicyItems(tuple_l.map(tuple => tuple._1 -> tuple._2))
			(sLiquidClass, tipSpacing) = pair
			// Translate items into evoware commands
			result <- spirateChecked(identToAgentObject_m, siteE, plateModelE, item_l, func_s, sLiquidClass, tipSpacing)
		} yield result
	}
	
	//private def dispense(builder: EvowareScriptBuilder, cmd: pipette.low.DispenseToken): RqResult[Seq[L0C_Command]] = {
	//	checkTipWellPolicyItems(builder, cmd.items).flatMap(sLiquidClass => spirateChecked(builder, cmd.items, "Dispense", sLiquidClass))
	//}

	/** Return name of liquid class */
	private def checkTipWellPolicyItems(
		item_l: List[(HasTip with HasWell with HasPolicy, WellPosition)]
	): Context[(String, Int)] = {
		item_l match {
			case Seq() => Context.error("INTERNAL: items empty")
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
					return Context.error("INTERNAL: policy should be the same for all spirate items: "+item_l)
				}
				
				// Assert that all tips are of the same kind
				// TODO: Re-add this error check somehow? -- ellis, 2011-08-25
				//val tipKind = config.getTipKind(twvp0.tip)
				//assert(items.forall(twvp => robot.getTipKind(twvp.tip) eq tipKind))
				
				// All tip/well pairs are equidistant or all tips are going to the same well
				val tipDistance_? = TipWell.equidistance(item_l)
				val bEquidistant = tipDistance_?.isDefined
				val bSameWell = item_l.forall(item => item._1.well eq item0._1.well)
				if (!bEquidistant && !bSameWell)
					return Context.error("INTERNAL: not equidistant, "+item_l.map(_._1.tip.index)+" -> "+item_l.map(_._2.index))
				
				Context.unit((policy.id, tipDistance_?.getOrElse(1)))
		}
	}

	private def spirateChecked(
		identToAgentObject_m: Map[String, Object],
		siteE: CarrierSite,
		labwareModelE: EvowareLabwareModel,
		item_l: List[(TipWellVolumePolicy, Int)],
		sFunc: String,
		sLiquidClass: String,
		tipSpacing: Int
	): Context[List[TranslationItem]] = {
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
		
		/*
		def getState(state: WorldState, item: TipWellVolumePolicy): RqResult[WorldState] = {
			val wellAliquot0 = state.well_aliquot_m.getOrElse(item.well, Aliquot.empty)
			val tipState0 = state.tip_state_m.getOrElse(item.tip, TipState.createEmpty(item.tip))
			val amount = Distribution.fromVolume(item.volume)
			sFunc match {
				case "Aspirate" =>
					for {
						wellAliquot1 <- wellAliquot0.remove(amount)
						tipEvent = TipAspirateEvent(item.tip, item.well, wellAliquot0.mixture, item.volume)
						tipState1 <- new TipAspirateEventHandler().handleEvent(tipState0, tipEvent)
					} yield {
						//println("tipState1: "+tipState1.content)
						state.copy(
							well_aliquot_m = state.well_aliquot_m + (item.well -> wellAliquot1),
							tip_state_m = state.tip_state_m + (item.tip -> tipState1)
						)
					}
				case "Dispense" =>
					val pos = PipettePosition.getPositionFromPolicyNameHack(sLiquidClass)
					val tipEvent = TipDispenseEvent(item.tip, wellAliquot0.mixture, item.volume, pos)
					for {
						tipState1 <- new TipDispenseEventHandler().handleEvent(tipState0, tipEvent)
						aliquot = Aliquot(tipState0.content.mixture, amount)
						wellAliquot1 <- wellAliquot0.add(aliquot)
					} yield {
						//println("aliquot: "+aliquot)
						//println("wellAliquot: "+wellAliquot1)
						state.copy(
							well_aliquot_m = state.well_aliquot_m + (item.well -> wellAliquot1),
							tip_state_m = state.tip_state_m + (item.tip -> tipState1)
						)
					}
			}
		}
		
		for {
			_ <- Context.foreachFirst(item_l) { case (item, _) =>
				for {
					state0 <- Context.gets(_.state)
					state1 <- Context.from(getState(state0, item))
					_ <- Context.modify(_.setState(state1))
				} yield ()
			}
		} yield {*/
			val cmd = L0C_Spirate(
				sFunc, 
				mTips, sLiquidClass,
				asVolumes,
				iGrid, siteE.iSite,
				tipSpacing,
				sPlateMask,
				siteE, labwareModelE
			)
			Context.unit(List(TranslationItem(cmd, List(siteE -> labwareModelE))))
		//}
	}

	private def pipetterTipsRefresh(
		identToAgentObject_m: Map[String, Object],
		cmd: PipetterTipsRefresh
	): Context[List[TranslationItem]] = {
		pipetterTipsRefresh_BSSE(identToAgentObject_m, cmd)
	}

	// FIXME: This is a BSSE-specific HACK.  The function for handling this command should be loaded from a config file.
	private def pipetterTipsRefresh_BSSE(
		identToAgentObject_m: Map[String, Object],
		cmd: PipetterTipsRefresh
	): Context[List[TranslationItem]] = {
		def doit(
			item_l: List[(Tip, CleanIntensity.Value, Option[TipModel])],
			tip_l: List[Tip],
			suffix: String
		): Context[Option[TranslationItem]] = {
			item_l match {
				case Nil => Context.unit(None)
				case item0 :: rest =>
					val intensity: CleanIntensity.Value = rest.foldLeft(item0._2){(acc, item) => CleanIntensity.max(acc, item._2)}
					val intensity_s_? = intensity match {
						case CleanIntensity.None => None
						case CleanIntensity.Flush => Some("Light")
						case CleanIntensity.Light => Some("Light")
						case CleanIntensity.Thorough => Some("Thorough")
						case CleanIntensity.Decontaminate => Some("Decontaminate")
					}
					intensity_s_? match {
						case None => Context.unit(None)
						case Some(intensity_s) =>
							// Set tip state to clean for the four tips that are being washed
							for {
								_ <- Context.foreachFirst(tip_l) { tip =>
									for {
										_ <- Context.modifyStateBuilder { state =>
											val tipState0 = state.tip_state_m.getOrElse(tip, TipState.createEmpty(tip))
											val event = TipCleanEvent(tip, intensity)
											state.tip_state_m(tip) = new TipCleanEventHandler().handleEvent(tipState0, event).toOption.get
										}
									} yield ()
								}
							} yield {
								val tip_m = encodeTips(tip_l)
								if (tip_l.isEmpty) {
									None
								}
								else if (intensity == CleanIntensity.Flush) {
									val volume = if (tip_l.head.index >= 4) 0.05 else 1.0
									val wash = L0C_Wash(
										mTips = tip_m,
										iWasteGrid = 1, iWasteSite = 1,
										iCleanerGrid = 1, iCleanerSite = 0,
										nWasteVolume = volume,
										nWasteDelay = 500,
										nCleanerVolume = volume,
										nCleanerDelay = 500,
										nAirgapVolume = 10,
										nAirgapSpeed = 70,
										nRetractSpeed = 30,
										bFastWash = false,
										bUNKNOWN1 = false
									)
									Some(TranslationItem(wash, Nil))
								}
								else if (intensity == CleanIntensity.Light) {
									val wash = L0C_Wash(
										mTips = tip_m,
										iWasteGrid = 1, iWasteSite = 1,
										iCleanerGrid = 1, iCleanerSite = 0,
										nWasteVolume = 4,
										nWasteDelay = 500,
										nCleanerVolume = 2,
										nCleanerDelay = 500,
										nAirgapVolume = 10,
										nAirgapSpeed = 70,
										nRetractSpeed = 30,
										bFastWash = false,
										bUNKNOWN1 = false
									)
									Some(TranslationItem(wash, Nil))
								}
								else {
									// Call the appropriate subroutine for cleaning
									val path = """C:\ProgramData\TECAN\EVOware\database\scripts\Roboliq\Roboliq_Clean_"""+intensity_s+"_"+suffix+".esc"
									Some(TranslationItem(L0C_Subroutine(path), Nil))
								}
							}
					}
			}
		}
			
		for {
			tipAll_l <- Context.gets(_.eb.pipetterToTips_m.getOrElse(cmd.device, cmd.item_l.map(_._1)))
			tip1000_l = tipAll_l.filter(_.index < 4)
			tip0050_l = tipAll_l.filter(_.index >= 4)
			item1000_l = cmd.item_l.filter(_._1.index < 4)
			item0050_l = cmd.item_l.filter(_._1.index >= 4)
			token1000_? <- doit(item1000_l, tip1000_l, "1000")
			token0050_? <- doit(item0050_l, tip0050_l, "0050")
		} yield {
			List(token1000_?, token0050_?).flatten
		}
	}

	// FIXME: This is a WIS-specific HACK.  The function for handling this command should be loaded from a config file.
	private def pipetterTipsRefresh_WIS(
		identToAgentObject_m: Map[String, Object],
		cmd: PipetterTipsRefresh
	): Context[List[TranslationItem]] = {
		//val tipAll_l = protocol.eb.pipetterToTips_m.getOrElse(cmd.device, cmd.item_l.map(_._1)).toSet
		
		def doit(item_l: List[(Tip, CleanIntensity.Value, Option[TipModel])]): Context[List[TranslationItem]] = {
			item_l match {
				case Nil => Context.unit(Nil)
				case item0 :: rest =>
					val tip_l = item_l.map(_._1).toSet
					//tip_l.foreach(tip => println("state.tip_model_m(tip): "+state.tip_model_m.get(tip)))
					val intensity: CleanIntensity.Value = rest.foldLeft(item0._2){(acc, item) => CleanIntensity.max(acc, item._2)}
					val tipModel_? : Option[TipModel] = rest.foldLeft(item0._3){(acc, item) => acc.orElse(item._3)}
					for {
						tipAll_l <- Context.gets(_.eb.pipetterToTips_m.getOrElse(cmd.device, cmd.item_l.map(_._1)).toSet)
						tipAll_m = encodeTips(tipAll_l)
						state <- Context.gets(_.state)
						tipState_l = tip_l.map(tip => state.tip_state_m.getOrElse(tip, TipState.createEmpty(tip)))
						
						// If tips are currently attached and either the cleanIntensity >= Thorough or we're changing tip models, then drop old tips
						tipDrop1_l = item_l.filter(tuple => {
							val (tip, _, tipModel_?) = tuple
							val tipState = state.tip_state_m.getOrElse(tip, TipState.createEmpty(tip))
							//println("stuff:", tip, tipState, (intensity >= CleanIntensity.Thorough), tipState.model_? != tipModel_?)
							(tipState.model_?.isDefined && (intensity >= CleanIntensity.Thorough || tipState.model_? != tipModel_?))
						}).map(_._1)
						// Also dropped any tips which weren't mentioned but are attached
						tipDrop2_l = tipAll_l.filter(tip => {
							val tipState = state.tip_state_m.getOrElse(tip, TipState.createEmpty(tip))
							!tip_l.contains(tip) && tipState.model_?.isDefined
						})
						// If we need to drop any tips, drop all of them
						tipDrop_l: Set[Tip] = {
							if (!tipDrop2_l.isEmpty || !tipDrop1_l.isEmpty)
								tipAll_l.filter(tip => {
									val tipState = state.tip_state_m.getOrElse(tip, TipState.createEmpty(tip))
									tipState.model_?.isDefined
								})
							else
								Set()
						}
						tipDrop_m = encodeTips(tipDrop_l)
						
						// If we need new tips and either didn't have any before or are dropping our previous ones
						tipGet_l = item_l.filter(tuple => {
							val (tip, _, tipModel_?) = tuple
							val tipState = state.tip_state_m.getOrElse(tip, TipState.createEmpty(tip))
							(tipModel_?.isDefined && (tipState.model_? == None || tipDrop_l.contains(tip)))
						}).map(_._1).toSet
						tipGet_m = encodeTips(tipGet_l)
					
						// If tip state has no clean state, do a pre-wash
						prewash_b = tipGet_m > 0 && tipState_l.exists(_.cleanDegreePrev == CleanIntensity.None)
						//_ <- logger.debug(("prewash_b:", prewash_b, tipGet_m > 0, tipState_l.map(s => (s.conf.index, s.cleanDegreePrev))))
						
						token_ll = List[List[L0C_Command]](
							if (tipDrop_m > 0) List(L0C_DropDITI(tipDrop_m, 1, 6)) else Nil,
							if (prewash_b) {
								List(
									L0C_Wash(tipAll_m,1,1,1,0,50,500,1,500,20,70,30,true,true),
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
						_ <- Context.modifyStateBuilder { state =>
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
						}
					} yield {
						val x = token_ll.flatten.map(token => TranslationItem(token, Nil))
						logger.debug("x: "+x)
						logger.debug("")
						x
					}
			}
		}
		
		doit(cmd.item_l)
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
}