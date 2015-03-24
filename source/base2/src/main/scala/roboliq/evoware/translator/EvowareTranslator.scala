/*package roboliq.evoware.translator

import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import grizzled.slf4j.Logger
import roboliq.core._
import roboliq.entities._
import roboliq.evoware.parser._
//import roboliq.pipette.HasTip
//import roboliq.pipette.HasWell
//import roboliq.pipette.HasPolicy
//import roboliq.pipette.TipWellVolumePolicy
import roboliq.tokens._
import ch.ethz.reactivesim.RsResult
//import roboliq.pipette.Tip
//import roboliq.pipette.Plate
//import roboliq.pipette.LiquidVolume
//import roboliq.pipette.Printer


class EvowareTranslator(config: EvowareConfig) {
	private val logger = Logger[this.type]
	
	def translate(token_l: List[Token]): RqResult[EvowareScript] = {
		val t2 = new EvowareTranslator2(config, token_l)
		t2.translate().map(_.toImmutable)
	}

	def saveWithHeader(script: EvowareScript, sFilename: String) {
		val mapSiteToLabel = new HashMap[CarrierSite, String]
		// TODO: Need to split into multiple scripts is a single site needs to accomodate multiple labware models
		for (c <- script.cmds) {
			script.mapCmdToLabwareInfo.get(c) match {
				case None =>
				case Some(l) =>
					for ((site, model) <- l) {
						val id0 = f"C${site.carrier.id}%03dS${site.iSite+1}"
						val label = config.config.siteIds.getOrElse(id0, id0)
						mapSiteToLabel(site) = label
					}
			}
		}
		val mapSiteToLabwareModel: Map[CarrierSite, EvowareLabwareModel] =
			script.cmds.collect({case c: L0C_Command => c.getSiteToLabwareModelList}).flatten.toMap
		
		val sHeader = config.table.toStringWithLabware(mapSiteToLabel.toMap, mapSiteToLabwareModel)
		val sCmds = script.cmds.mkString("\n")
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
	
}

private class EvowareTranslator2(config: EvowareConfig, token_l: List[Token]) {
	private val logger = Logger[this.type]
	private var nodeCurrent: Token = null
	
	def translate(): RqResult[EvowareScriptBuilder] = {
		val builder = new EvowareScriptBuilder
		translate(token_l, builder).map(_ => builder)
	}
	
	private def translate(token_l: List[Token], builder: EvowareScriptBuilder): RqResult[Unit] = {
		RsResult.toResultOfList(token_l.map(token => {
			nodeCurrent = token
			translate(token, builder)
		})).map(_ => ())
	}

	private def translate(cmd1: Token, builder: EvowareScriptBuilder): RqResult[Unit] = {
		for { cmds0 <- cmd1 match {
			case c: control.CommentToken => comment(c)
			case c: control.PromptToken => prompt(c)
			case c: control.low.CallToken => call(c)
			case c: control.low.ExecToken => execute(c)
			case c: pipette.low.AspirateToken => aspirate(builder, c)
			case c: pipette.low.DispenseToken => dispense(builder, c)
			//case c: DetectLevelToken => detectLevel(builder, c)
			//case c: L1C_EvowareFacts => facts(builder, c)
			//case c: EvowareSubroutineToken => subroutine(builder, c)
			//case c: MixToken => mix(builder, c.items)
			case c: transport.EvowareTransporterRunToken => transportLabware(builder, c)
			//case c: transport.MovePlateToken => movePlate(builder, c)
			//case c: L1C_TipsGet => tipsGet(c)
			//case c: L1C_TipsDrop => tipsDrop(c)
			//case c: L1C_Timer => timer(c.args)
			//case c: L1C_SaveCurrentLocation => Success(Seq())
			//case c: TipsWashToken => clean(builder, c)
		}} yield {
			builder.cmds ++= cmds0
			()
		}
	}
	
	private def encode(n: Int): Char = ('0' + n).asInstanceOf[Char]
	private def hex(n: Int): Char = Integer.toString(n, 16).toUpperCase.apply(0)
	
	protected def encodeHasTips(list: Iterable[HasTip]): Int =
		list.foldLeft(0) { (sum, x) => sum | (1 << x.tip.index) }
	protected def encodeTips(list: Iterable[Tip]): Int =
		list.foldLeft(0) { (sum, tip) => sum | (1 << tip.index) }

	protected def encodeWells(model: PlateModel, aiWells: Traversable[Int]): String = {
		//println("encodeWells:", holder.nRows, holder.nCols, aiWells)
		val nWellMaskChars = math.ceil(model.rows * model.cols / 7.0).asInstanceOf[Int]
		val amWells = new Array[Int](nWellMaskChars)
		for (iWell <- aiWells) {
			val iChar = iWell / 7;
			val iWell1 = iWell % 7;
			// FIXME: for debug only
			if (iChar >= amWells.size)
				println("ERROR: encodeWells: "+(model, iWell, iChar, iWell1, aiWells))
			// ENDFIX
			amWells(iChar) += 1 << iWell1
		}
		val sWellMask = amWells.map(encode).mkString
		val sPlateMask = Array('0', hex(model.cols), '0', hex(model.rows)).mkString + sWellMask
		sPlateMask
	}

	/*
	def test() {
		val holder = new Plate(8, 12)
		def t(aiWells: Array[Int], sExpect: String) {
			println(aiWells.mkString(","))
			println(sExpect)
			val sActual = encodeWells(holder, aiWells)
			println(sActual)
			assert(sExpect == sActual)
			
		}
		t(Array( 0,  1,  2,  3), "0C08?0000000000000")
		t(Array( 4,  5,  6,  7), "0C08\2401000000000000")
		t(Array( 8,  9, 10, 11), "0C080N000000000000")
		t(Array(12, 13, 14, 15), "0C080\220300000000000")
	}
	*/
	

	private def aspirate(builder: EvowareScriptBuilder, cmd: pipette.low.AspirateToken): RqResult[List[L0C_Command]] = {
		for (item <- cmd.items) {
			val state = item.well.vesselState
			val sLiquid = state.content.liquid.id
			val mapWellToAspirated = builder.state.mapLiquidToWellToAspirated.getOrElse(sLiquid, new HashMap())
			val vol0 = mapWellToAspirated.getOrElseUpdate(item.well.id, LiquidVolume.empty)
			mapWellToAspirated(item.well.id) = vol0 + item.volume
			builder.state.mapLiquidToWellToAspirated(sLiquid) = mapWellToAspirated
		}
		checkTipWellPolicyItems(builder, cmd.items).flatMap(sLiquidClass => spirateChecked(builder, cmd.items, "Aspirate", sLiquidClass))
	}
	
	private def dispense(builder: EvowareScriptBuilder, cmd: pipette.low.DispenseToken): RqResult[Seq[L0C_Command]] = {
		checkTipWellPolicyItems(builder, cmd.items).flatMap(sLiquidClass => spirateChecked(builder, cmd.items, "Dispense", sLiquidClass))
	}

	/** Return name of liquid class */
	private def checkTipWellPolicyItems(builder: EvowareScriptBuilder, items: List[HasTip with HasWell with HasPolicy]): RqResult[String] = {
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

	private def spirateChecked(builder: EvowareScriptBuilder, items: List[TipWellVolumePolicy], sFunc: String, sLiquidClass: String): RqResult[List[L0C_Command]] = {
		val lWellInfo = items.map(_.well)
		val item0 = items.head
		val info0 = lWellInfo.head
		//val tipKind = robot.getTipKind(item0.tip)
		val idPlate = info0.idPlate
		val mTips = encodeTips(items.map(_.tip.conf))
		
		// Create a list of volumes for each used tip, leaving the remaining values at 0
		val asVolumes = Array.fill(12)("0")
		val fmt = new java.text.DecimalFormat("#.##")
		for (twv <- items) {
			val iTip = twv.tip.index
			assert(iTip >= 0 && iTip < 12)
			// HACK: robot is aborting when trying to aspirate <0.4ul from PCR well -- ellis, 2012-02-12
			//val nVolume = if (sFunc == "Aspirate" && twv.volume < 0.4) 0.4 else twv.volume
			asVolumes(iTip) = "\""+fmt.format(twv.volume.ul.toDouble)+'"'
		}
		//val sVolumes = asVolumes.mkString(",")
		
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
	
	/*private def detectLevel(builder: EvowareScriptBuilder, cmd: DetectLevelToken): RqResult[Seq[L0C_Command]] = {
		checkTipWellPolicyItems(builder, cmd.items).flatMap(sLiquidClass => detectLevelChecked(builder, cmd.items, sLiquidClass))
	}

	private def detectLevelChecked(builder: EvowareScriptBuilder, items: Seq[TipWellPolicy], sLiquidClass: String): RqResult[Seq[L0C_Command]] = {
		val lWellInfo = getWellInfo(items.map(_.well))
		val item0 = items.head
		val info0 = lWellInfo.head
		//val tipKind = robot.getTipKind(item0.tip)
		val idPlate = info0.idPlate
		val mTips = encodeTips(items.map(_.tip))
		
		for {
			plate <- builder.ob.findPlate(idPlate)
			location <- getLocation(idPlate)
			site <- getSite(location)
		} yield {
			val sPlateMask = encodeWells(plate, lWellInfo.map(_.index))
			val iGrid = config.table.mapCarrierToGrid(site.carrier)
			val labwareModel = config.table.configFile.mapNameToLabwareModel(plate.model.id)
			val cmd = L0C_DetectLevel(
				mTips, sLiquidClass,
				iGrid, site.iSite,
				sPlateMask,
				site, labwareModel
			)
			
			builder.mapCmdToLabwareInfo(cmd) = List((site, location, labwareModel))
			
			Seq(cmd)
		}
	}
	*/

	private def comment(cmd: control.CommentToken): RqResult[Seq[L0C_Command]] = {
		RqSuccess(Seq(L0C_Comment(cmd.text)))
	}
	
	private def execute(cmd: control.low.ExecToken): RqResult[Seq[L0C_Command]] = {
		val nWaitOpts = (cmd.waitTillDone, cmd.checkResult) match {
			case (true, true) => 6
			case _ => 2
		}
		val sResultVar = if (cmd.checkResult) "RESULT" else ""
		RqSuccess(Seq(L0C_Execute(cmd.text, nWaitOpts, sResultVar)))
	}
	
	private def prompt(cmd: control.PromptToken): RqResult[Seq[L0C_Command]] = {
		RqSuccess(Seq(L0C_Prompt(cmd.text)))
	}

	/*
	private def clean(builder: EvowareScriptBuilder, cmd: TipsWashToken): RqResult[Seq[L0C_Command]] = {
		val lPermanent = cmd.tips.filter(_.permanent_?.isDefined)
		val lModel = lPermanent.map(_.permanent_?.get.id).distinct
		val lName = lModel.map(s => {
			if (s.contains("1000")) "1000"
			else "0050"
		})
		val sDegree = cmd.washProgram match {
			case 0 => "Light"
			case 1 => "Thorough"
			case _ => "Decon"
		}
		Success(
			lName.map(s => L0C_Subroutine("Roboliq_Clean_"+sDegree+"_"+s))
		)
	}
	*/
	/*def wash(cmd: TipsWashToken): RqResult[Seq[Token]] = {
		config.mapWashProgramArgs.get(cmd.iWashProgram) match {
			case None =>
				Error(Seq("INTERNAL: Wash program "+cmd.iWashProgram+" not defined"))
			case Some(args) =>
				val nWasteVolume = cmd.items.foldLeft(0.0) { (acc, item) => math.max(acc, item.nVolumeInside) }
				Success(Seq(L0C_Wash(
					mTips = encodeTips(cmd.items.map(_.tip.obj)),
					iWasteGrid = args.iWasteGrid, iWasteSite = args.iWasteSite,
					iCleanerGrid = args.iCleanerGrid, iCleanerSite = args.iCleanerSite,
					nWasteVolume = args.nWasteVolume_?.getOrElse(nWasteVolume),
					nWasteDelay = args.nWasteDelay,
					nCleanerVolume = args.nCleanerVolume,
					nCleanerDelay = args.nCleanerDelay,
					nAirgapVolume = args.nAirgapVolume,
					nAirgapSpeed = args.nAirgapSpeed,
					nRetractSpeed = args.nRetractSpeed,
					bFastWash = args.bFastWash,
					bUNKNOWN1 = args.bUNKNOWN1
					)))
		}
	}*/
	/*
	private def mix(builder: EvowareScriptBuilder, items: Seq[MixTokenItem]): RqResult[Seq[L0C_Command]] = {
		// REFECTOR: duplicates a lot of checkTipWellVolumePolicy()
		val lWellInfo = getWellInfo(items.map(_.well))
		val lItemInfo = items zip lWellInfo
		lItemInfo match {
			case Seq() => Error(Seq("Empty Tip-Well-Volume list"))
			case Seq((item0, info0), rest @ _*) =>
				// Get the liquid class
				val policy = item0.policy
				// Assert that there is only one liquid class
				if (rest.exists(_._1.policy != policy)) {
					items.foreach(item => println(item.policy))
					return Error(Seq("INTERNAL: Liquid class must be the same for all mix items"))
				}
				if (rest.exists(_._1.count != item0.count))
					return Error(Seq("INTERNAL: Mix count must be the same for all mix items"))
				
				// Assert that all tips are of the same kind and that all wells are on the same holder
				// TODO: re-add this check
				//val tipKind = robot.getTipKind(tw0.tip)
				//val holder = tw0.well.holder
				//assert(cmd.tws.forall(twv => (robot.getTipKind(twv.tip) eq tipKind) && (twv.well.holder eq holder)))
				
				// Assert that tips are spaced at equal distances to each other as the wells are to each other
				def equidistant2(a: Tuple2[MixTokenItem, WellInfo], b: Tuple2[MixTokenItem, WellInfo]): Boolean =
					(b._1.tip.index - a._1.tip.index) == (b._2.index - a._2.index)
				// Test all adjacent items for equidistance
				def equidistant(tws: Seq[Tuple2[MixTokenItem, WellInfo]]): Boolean = tws match {
					case Seq() => true
					case Seq(_) => true
					case Seq(a, b, rest @ _*) =>
						equidistant2(a, b) match {
							case false => false
							case true => equidistant(Seq(b) ++ rest)
						}
				}
				// All tip/well pairs are equidistant or all tips are going to the same well
				assert(equidistant(lItemInfo) || lItemInfo.forall(_._1.well eq item0.well))
				
				mixChecked(builder, items, policy.id)
		}
	}

	// REFACTOR: duplicates a lot of spirateChecked and detectLevelChecked
	private def mixChecked(builder: EvowareScriptBuilder, items: Seq[MixTokenItem], sLiquidClass: String): RqResult[Seq[L0C_Command]] = {
		val lWellInfo = getWellInfo(items.map(_.well))
		val item0 = items.head
		val info0 = lWellInfo.head
		//val tipKind = robot.getTipKind(item0.tip)
		val idPlate = info0.idPlate
		val mTips = encodeTips(items.map(_.tip))
		
		// Create a list of volumes for each used tip, leaving the remaining values at 0
		val asVolumes = Array.fill(12)("0")
		val fmt = new java.text.DecimalFormat("#.##")
		for (item <- items) {
			val iTip = item.tip.index
			assert(iTip >= 0 && iTip < 12)
			asVolumes(iTip) = "\""+fmt.format(item.volume)+'"'
		}
		//val sVolumes = asVolumes.mkString(",")
		
		for {
			plate <- builder.ob.findPlate(idPlate)
			location <- getLocation(idPlate)
			site <- getSite(location)
		} yield {
			val sPlateMask = encodeWells(plate, lWellInfo.map(_.index))
			val iGrid = config.table.mapCarrierToGrid(site.carrier)
			val cmd = L0C_Mix(
				mTips, sLiquidClass,
				asVolumes,
				iGrid, site.iSite,
				sPlateMask,
				item0.count
			)
			
			val labwareModel = config.table.configFile.mapNameToLabwareModel(plate.model.id)
			builder.mapCmdToLabwareInfo(cmd) = List((site, location, labwareModel))
			
			Seq(cmd)
		}
	}
	*/
	/*
	private def tipsGet(c: L1C_TipsGet): RqResult[Seq[Token]] = {
		val mTips = encodeTips(c.tips.map(_.obj))
		Success(Seq(L0C_GetDITI2(mTips, c.model.id)))
	}
	
	private def tipsDrop(c: L1C_TipsDrop): RqResult[Seq[Token]] = {
		val mTips = encodeTips(c.tips.map(_.obj))
		for (site <- getSite(c.location)) yield {
			val iGrid = config.table.mapCarrierToGrid(site.carrier)
			Seq(L0C_DropDITI(mTips, iGrid, site.iSite))
		}
	}
	*/
	
	private def transportLabware(builder: EvowareScriptBuilder, c: transport.EvowareTransporterRunToken): RqResult[Seq[L0C_Command]] = {
		val carrierSrc = c.origin.carrier
		val iGridSrc = config.table.mapCarrierToGrid(carrierSrc)
		val lVectorSrc = config.table.configFile.mapCarrierToVectors(carrierSrc)

		val carrierDest = c.destination.carrier
		val iGridDest = config.table.mapCarrierToGrid(carrierDest)
		val lVectorDest = config.table.configFile.mapCarrierToVectors(carrierDest)

		val cmd = L0C_Transfer_Rack(
			c.roma_i,
			c.vectorClass,
			//c.sPlateModel,
			//iGridSrc, siteSrc.iSite, siteSrc.carrier.sName,
			//iGridDest, siteDest.iSite, siteDest.carrier.sName,
			c.model,
			iGridSrc, c.origin,
			iGridDest, c.destination,
			LidHandling.NoLid, //c.lidHandling,
			iGridLid = 0,
			iSiteLid = 0,
			sCarrierLid = ""
		)
		
		builder.mapCmdToLabwareInfo(cmd) = List(
			(c.origin, c.model),
			(c.destination, c.model)
		)
		
		RqSuccess(Seq(cmd))
	}
	
	private def movePlate(builder: EvowareScriptBuilder, c: transport.MovePlateToken): RqResult[Seq[L0C_Command]] = {
		for {
			siteSrc <- getSite(c.plateSrc.id)
			siteDest <- getSite(c.plateDest.id)
		} yield {
			val labwareModel = config.table.configFile.mapNameToLabwareModel(c.plate.model.id)
			
			val carrierSrc = siteSrc.carrier
			val iGridSrc = config.table.mapCarrierToGrid(carrierSrc)
			val lVectorSrc = config.table.configFile.mapCarrierToVectors(carrierSrc)

			val carrierDest = siteDest.carrier
			val iGridDest = config.table.mapCarrierToGrid(carrierDest)
			val lVectorDest = config.table.configFile.mapCarrierToVectors(carrierDest)

			val mapClassToValue = Map("Narrow" -> 0, "Wide" -> 1)
			val lVector1: Map[Tuple2[Int, String], List[Vector]] = (lVectorSrc ++ lVectorDest).groupBy(v => (v.iRoma, v.sClass)).filter(_._2.length == 2)
			val lVector2 = lVector1.toList.sortBy(pair => pair._1._1.toString + mapClassToValue.getOrElse(pair._1._2, pair._1._2))
			// TODO: figure out an intermediate path to follow instead (e.g. via a re-grip location)
			if (lVector2.isEmpty) 
				return RqError("no common RoMa: "+carrierSrc.sName+" and "+carrierDest.sName)
			val (iRoma, sVectorClass) = lVector2.head._1
			
			//println("movePlate:")
			//println("lVectorSrc: "+lVectorSrc)
			//println("lVectorDest: "+lVectorDest)
			//println("lVector1: "+lVector1)
			//println("lVector2: "+lVector2)
			
			val cmd = L0C_Transfer_Rack(
				iRoma,
				sVectorClass,
				//c.sPlateModel,
				//iGridSrc, siteSrc.iSite, siteSrc.carrier.sName,
				//iGridDest, siteDest.iSite, siteDest.carrier.sName,
				labwareModel,
				iGridSrc, siteSrc,
				iGridDest, siteDest,
				LidHandling.NoLid, //c.lidHandling,
				iGridLid = 0,
				iSiteLid = 0,
				sCarrierLid = ""
			)
			
			builder.mapCmdToLabwareInfo(cmd) = List(
				(siteSrc, labwareModel),
				(siteDest, labwareModel)
			)
			
			Seq(cmd)
		}
	}
	
	/*
	private def timer(args: L12A_TimerArgs): RqResult[Seq[Token]] = {
		Success(Seq(
			L0C_StartTimer(1),
			L0C_WaitTimer(1, args.nSeconds)
		))
	}
	
	private def facts(builder: EvowareScriptBuilder, cmd: L1C_EvowareFacts): RqResult[Seq[Token]] = {
		Success(Seq(L0C_Facts(
			sDevice = cmd.args.sDevice,
			sVariable = cmd.args.sVariable,
			sValue = cmd.args.sValue
		)))
	}
	*/
	
	private def call(cmd: control.low.CallToken): RqResult[Seq[L0C_Command]] = {
		RqSuccess(List(L0C_Subroutine(
			cmd.text
		)))
	}
	
	/*
	private def getLocation(idPlate: String): RqResult[String] = {
		tracker.getLocationForCommand(idPlate, nodeCurrent.index)
	}
	
	private def getWellInfo(well: Well): WellInfo = {
		WellInfo(well)
	}
	
	private def getWellInfo(lWell: Iterable[Well]): List[WellInfo] = {
		lWell.map(well => WellInfo(well)).toList
	}*/
	
	private def getSite(location: String): RqResult[CarrierSite] = {
		config.idToSite_m.get(location) match {
			case None =>
				logger.error("INTERNAL: missing evoware site for location \""+location+"\"")
				logger.error("config.mapLabelToSite: "+config.idToSite_m)
				RqError("INTERNAL: missing evoware site for location \""+location+"\"")
			case Some(site) => RqSuccess(site)
		}
	}
	
	/*
	private def addLabware(builder: EvowareScriptBuilder, labwareModel: LabwareModel, location: String): RqResult[Unit] = {
		val site = config.mapLabelToSite(location)
		val labwareModel = config.table.mapSiteToLabwareModel(site)
		
		//val labwareModel = config.table.configFile.mapNameToLabwareModel(sLabwareModel)
		builder.mapLocToLabware.get(site) match {
			case None =>
				builder.mapLocToLabware(site) = new LabwareObject(site, labwareModel, location)
			case Some(labware) if labware.sLabel == location && labware.labwareModel == labwareModel =>
				// Same as before, so we don't need to do anything
			case Some(_) =>
				// TODO: a new script needs to be started
		}
		Success(())
	}
	
	private def addLabware(builder: EvowareScriptBuilder, sLabwareModel: String, location: String): RqResult[Unit] = {
		val labwareModel = config.table.configFile.mapNameToLabwareModel(sLabwareModel)
		addLabware(builder, labwareModel, location)
	}
	
	private def addLabware(builder: EvowareScriptBuilder, plate: Plate, location: String): RqResult[Unit] = {
		if (plate.model_?.isEmpty)
			//return Error("plate model must be assigned to plate \""+plate+"\"")
			return Success(())
		addLabware(builder, plate.model_?.get.id, location)
	}
	*/
}
*/