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
				val labware = protocol.eb.getEntity(labwareIdent).get.asInstanceOf[Labware]
				val destination = protocol.eb.getEntity(destinationIdent).get
				//val state = state0.toMutable
				val cmd = {
					if (agentIdent == "user") {
						val model = protocol.eb.getEntity(modelIdent).get
						val modelLabel = model.label.getOrElse(model.key)
						val origin = protocol.eb.getEntity(originIdent).get
						val originLabel = origin.label.getOrElse(origin.key)
						val destinationLabel = destination.label.getOrElse(destination.key)
						val text = s"Please move labware `${labwareIdent}` model `${modelLabel}` from `${originLabel}` to `${destinationLabel}`"
						// TODO: if destination or source site have evoware equivalents, then call setModelSites() for them
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
				var state = state0.toMutable
				state.labware_location_m(labware) = destination
				cmds += cmd
				// TODO: change state of labware so it's now in the new given location
				RqSuccess(state0)
			case cmd: PipetterAspirate =>
				for {
					result <- aspirate(
						protocol,
						state0,
						identToAgentObject_m,
						cmd
					)
				} yield {
					for (item <- result.item_l) {
						item.siteToModel_l
					}
					cmds ++= result.item_l.map(_.token)
					result.state1
				}
			case _ =>
				RsError(s"unknown command `$command`")
		}
	}
	
	def end(): RsResult[Unit] = {
		endScript()
		RsSuccess(())
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
			siteIdent <- protocol.eb.names.get(site).asRs("site has not been assigned an identifier")
			siteE <- identToAgentObject_m.get(siteIdent).map(_.asInstanceOf[roboliq.evoware.parser.CarrierSite]).asRs("no evoware site corresponds to site")
		} yield siteE
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
		identToAgentObject_m: Map[String, Object],
		command: PipetterAspirate
	): RqResult[TranslationResult] = {
		if (command.item_l.isEmpty) return RsSuccess(TranslationResult.empty(state0))
		
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
			tuple_l <- RsResult.toResultOfList(command.item_l.map { item =>
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
			sLiquidClass <- checkTipWellPolicyItems(protocol, state0, command, tuple_l.map(tuple => tuple._1 -> tuple._2))
			// Translate items into evoware commands
			result <- spirateChecked(protocol, state0, identToAgentObject_m, siteE, plateModelE, item_l, "Aspirate", sLiquidClass)
		} yield result
	}
	
	//private def dispense(builder: EvowareScriptBuilder, cmd: pipette.low.DispenseToken): RqResult[Seq[L0C_Command]] = {
	//	checkTipWellPolicyItems(builder, cmd.items).flatMap(sLiquidClass => spirateChecked(builder, cmd.items, "Dispense", sLiquidClass))
	//}

	/** Return name of liquid class */
	private def checkTipWellPolicyItems(
		protocol: Protocol,
		state0: WorldState,
		command: PipetterAspirate,
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
			// HACK: robot is aborting when trying to aspirate <0.4ul from PCR well -- ellis, 2012-02-12
			//val nVolume = if (sFunc == "Aspirate" && twv.volume < 0.4) 0.4 else twv.volume
			asVolumes(iTip) = "\""+fmt.format(item._1.volume.ul.toDouble)+'"'
		}

		val iGrid = config.table.mapCarrierToGrid(siteE.carrier)
		val sPlateMask = encodeWells(labwareModelE.nRows, labwareModelE.nCols, well_li)

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
			
			TranslationResult(
				List(TranslationItem(cmd, List(siteE -> labwareModelE))),
				state0
			)
		}
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
		val sPlateMask = Array('0', hex(cols), '0', hex(rows)).mkString + sWellMask
		sPlateMask
	}
}