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

case class EvowareScript2(
	filename: String,
	siteToModel_m: Map[CarrierSite, EvowareLabwareModel],
	cmd_l: List[Object]
)

class EvowareClientScriptBuilder(config: EvowareConfig, basename: String) extends ClientScriptBuilder {
	private val logger = Logger[this.type]

	val script_l = new ArrayBuffer[EvowareScript2]
	//val builder = new EvowareScriptBuilder
	val cmds = new ArrayBuffer[Object]
	//val mapCmdToLabwareInfo = new HashMap[Object, List[(CarrierSite, EvowareLabwareModel)]]
	val siteToModel_m = new HashMap[CarrierSite, EvowareLabwareModel]
	
	def addCommand(
		protocol: Protocol,
		state0: WorldState,
		agentIdent: String,
		command: Command
	): RsResult[WorldState] = {
		println(s"addCommand: $agentIdent, $command")
		val identToAgentObject_m: Map[String, Object] = protocol.agentToIdentToInternalObject.get(agentIdent).map(_.toMap).getOrElse(Map())
		command match {
			case AgentActivate() => RsSuccess(state0)
			case AgentDeactivate() => RsSuccess(state0)
			case Log(text) =>
				cmds += L0C_Comment(text)
				RsSuccess(state0)
			case Prompt(text) =>
				cmds += L0C_Prompt(text)
				RsSuccess(state0)
			case TransporterRun(deviceIdent, labwareIdent, modelIdent, originIdent, destinationIdent, vectorIdent) =>
				//val state = state0.toMutable
				val cmd = {
					if (agentIdent == "user") {
						val model = protocol.eb.getEntity(modelIdent).get
						val modelLabel = model.label.getOrElse(model.key)
						val origin = protocol.eb.getEntity(originIdent).get
						val originLabel = origin.label.getOrElse(origin.key)
						val destination = protocol.eb.getEntity(destinationIdent).get
						val destinationLabel = destination.label.getOrElse(destination.key)
						val text = s"Please move labware `${labwareIdent}` model `${modelLabel}` from `${originLabel}` to `${destinationLabel}`"
						L0C_Prompt(text)
					}
					else {
						val roma_i: Int = identToAgentObject_m(deviceIdent).asInstanceOf[Integer]
						val model = identToAgentObject_m(modelIdent).asInstanceOf[roboliq.evoware.parser.EvowareLabwareModel]
						val origin = identToAgentObject_m(originIdent).asInstanceOf[roboliq.evoware.parser.CarrierSite]
						val destination = identToAgentObject_m(destinationIdent).asInstanceOf[roboliq.evoware.parser.CarrierSite]
						val vectorClass = identToAgentObject_m(vectorIdent).toString
						setModelSites(model, List(origin, destination))
						val carrierSrc = origin.carrier
						val iGridSrc = config.table.mapCarrierToGrid(carrierSrc)
						val lVectorSrc = config.table.configFile.mapCarrierToVectors(carrierSrc)
				
						val carrierDest = destination.carrier
						val iGridDest = config.table.mapCarrierToGrid(carrierDest)
						val lVectorDest = config.table.configFile.mapCarrierToVectors(carrierDest)
				
						L0C_Transfer_Rack(
							roma_i,
							vectorClass,
							//c.sPlateModel,
							//iGridSrc, siteSrc.iSite, siteSrc.carrier.sName,
							//iGridDest, siteDest.iSite, siteDest.carrier.sName,
							model,
							iGridSrc, origin,
							iGridDest, destination,
							LidHandling.NoLid, //c.lidHandling,
							iGridLid = 0,
							iSiteLid = 0,
							sCarrierLid = ""
						)
					}
				}
				cmds += cmd
				// TODO: change state of labware so it's now in the new given location
				RqSuccess(state0)
			case PipetterAspirate(item_l) =>
				L0C_Spirate(
					"Aspirate"
				)
			case _ =>
				RsError(s"unknown operation `$operation`")
		}
	}
	
	def end(): RsResult[Unit] = {
		endScript()
		RsSuccess(())
	}
	
	private def setModelSites(model: EvowareLabwareModel, sites: List[CarrierSite]) {
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
	}
	
	private def endScript() {
		if (!cmds.isEmpty) {
			val script = EvowareScript2(
				basename + ".esc",
				siteToModel_m.toMap,
				cmds.toList
			)
			script_l += script
		}
		siteToModel_m.clear
		cmds.clear
	}

	def saveWithHeader(script: EvowareScript2, sFilename: String) {
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
	private def aspirate(
		protocol: Protocol,
		state0: WorldState,
		command: PipetterAspirate
	): RqResult[List[L0C_Command]] = {
		/*for (item <- cmd.item_l) {
			val state = item.well.vesselState
			val sLiquid = state.content.liquid.id
			val mapWellToAspirated = builder.state.mapLiquidToWellToAspirated.getOrElse(sLiquid, new HashMap())
			val vol0 = mapWellToAspirated.getOrElseUpdate(item.well.id, LiquidVolume.empty)
			mapWellToAspirated(item.well.id) = vol0 + item.volume
			builder.state.mapLiquidToWellToAspirated(sLiquid) = mapWellToAspirated
		}*/
		checkTipWellPolicyItems(protocol, state0, command, command.item_l).flatMap(sLiquidClass => spirateChecked(builder, cmd.items, "Aspirate", sLiquidClass))
	}
	
	//private def dispense(builder: EvowareScriptBuilder, cmd: pipette.low.DispenseToken): RqResult[Seq[L0C_Command]] = {
	//	checkTipWellPolicyItems(builder, cmd.items).flatMap(sLiquidClass => spirateChecked(builder, cmd.items, "Dispense", sLiquidClass))
	//}

	/** Return name of liquid class */
	private def checkTipWellPolicyItems(
		protocol: Protocol,
		state0: WorldState,
		command: PipetterAspirate,
		items: List[HasTip with HasWell with HasPolicy]
	): RqResult[String] = {
		items match {
			case Seq() => RqError("INTERNAL: items empty")
			case Seq(twvp0, rest @ _*) =>
				// Get the liquid class
				val policy = twvp0.policy
				// Assert that there is only one liquid class
				// FIXME: for debug only:
				if (!rest.forall(twvp => twvp.policy.equals(policy))) {
					println("sLiquidClass: " + policy)
					rest.foreach(twvp => println(twvp.tip, twvp.policy))
				}
				// ENDFIX
				assert(rest.forall(twvp => twvp.policy.equals(policy)))
				
				val lWellInfo = items.map(_.well)
				val idPlate = lWellInfo.head.idPlate
				
				// Assert that all tips are of the same kind
				// TODO: Re-add this error check somehow? -- ellis, 2011-08-25
				//val tipKind = config.getTipKind(twvp0.tip)
				//assert(items.forall(twvp => robot.getTipKind(twvp.tip) eq tipKind))
				
				if (!lWellInfo.forall(_.idPlate == idPlate))
					return RqError(("INTERNAL: all wells must be on the same plate `"+idPlate+"`") :: lWellInfo.map(w => w.id+" on "+w.idPlate))
				
				/*
				// All tip/well pairs are equidistant or all tips are going to the same well
				// Assert that tips are spaced at equal distances to each other as the wells are to each other
				def equidistant2(a: Tuple2[HasTip, WellInfo], b: Tuple2[HasTip, WellInfo]): Boolean =
					(b._1.tip.index - a._1.tip.index) == (b._2.index - a._2.index)
				// Test all adjacent items for equidistance
				def equidistant(tws: Seq[Tuple2[HasTip, WellInfo]]): Boolean = tws match {
					case Seq() => true
					case Seq(_) => true
					case Seq(a, b, rest @ _*) =>
						equidistant2(a, b) match {
							case false => false
							case true => equidistant(Seq(b) ++ rest)
						}
				}
				*/
				val lItemInfo = items zip lWellInfo
				// All tip/well pairs are equidistant or all tips are going to the same well
				val bEquidistant = Utils.equidistant2(lItemInfo)
				val bSameWell = items.forall(_.well eq twvp0.well)
				if (!bEquidistant && !bSameWell)
					return RqError("INTERNAL: not equidistant, "+items.map(_.tip.conf.id)+" -> "+Printer.getWellsDebugString(items.map(_.well)))
				
				RqSuccess(policy.id)
		}
	}

	private def spirateChecked(
		protocol: Protocol,
		state0: WorldState,
		identToAgentObject_m: Map[String, Object],
		item_l: List[TipWellVolumePolicy],
		sFunc: String,
		sLiquidClass: String
	): RqResult[List[L0C_Command]] = {
		val item0 = item_l.head
		val well0 = item0.well
		val labware = state0.well_labware_m(well0)/* match {
			case plate: Plate => plate
			case tube: Tube => 
		}*/
		val model = state0.labware_model_m(labware)
		val site = state0.
		//val idPlate = info0.idPlate
		val mTips = encodeTips(item_l.map(_.tip))
		
		// Create a list of volumes for each used tip, leaving the remaining values at 0
		val asVolumes = Array.fill(12)("0")
		val fmt = new java.text.DecimalFormat("#.##")
		for (twv <- item_l) {
			val iTip = twv.tip.index
			assert(iTip >= 0 && iTip < 12)
			// HACK: robot is aborting when trying to aspirate <0.4ul from PCR well -- ellis, 2012-02-12
			//val nVolume = if (sFunc == "Aspirate" && twv.volume < 0.4) 0.4 else twv.volume
			asVolumes(iTip) = "\""+fmt.format(twv.volume.ul.toDouble)+'"'
		}
		//val sVolumes = asVolumes.mkString(",")
		
		val modelIdent = protocol.eb.names(model)
		val modelE = identToAgentObject_m(modelIdent).asInstanceOf[roboliq.evoware.parser.EvowareLabwareModel]
		val site = identToAgentObject_m(destinationIdent).asInstanceOf[roboliq.evoware.parser.CarrierSite]
		setModelSites(model, List(site))
		val carrier = site.carrier
		val iGrid = config.table.mapCarrierToGrid(carrier)

		val plate = info0.position.plate
		for {
			location <- info0.position.plate.location_?.asRs(s"plate location must be set for plate `$plate.id`")
			site <- getSite(location.id)
		} yield {
			val sPlateMask = encodeWells(plate.plate, lWellInfo.map(_.index))
			val iGrid = config.table.mapCarrierToGrid(site.carrier)
			val labwareModel = config.table.configFile.mapNameToLabwareModel(plate.plate.model.id)
			val cmd = L0C_Spirate(
				sFunc, 
				mTips, sLiquidClass,
				asVolumes,
				iGrid, site.iSite,
				sPlateMask,
				site, labwareModel
			)
			
			builder.mapCmdToLabwareInfo(cmd) = List((site, labwareModel))
			
			List(cmd)
		}
	}

	private def encode(n: Int): Char = ('0' + n).asInstanceOf[Char]
	private def hex(n: Int): Char = Integer.toString(n, 16).toUpperCase.apply(0)
	
	protected def encodeHasTips(list: Iterable[HasTip]): Int =
		list.foldLeft(0) { (sum, x) => sum | (1 << x.tip.index) }
	protected def encodeTips(list: Iterable[Tip]): Int =
		list.foldLeft(0) { (sum, tip) => sum | (1 << tip.index) }
}