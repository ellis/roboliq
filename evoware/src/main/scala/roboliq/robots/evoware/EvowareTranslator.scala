package roboliq.robots.evoware

import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter
import scala.collection.JavaConversions._
import scala.collection.mutable.HashMap
import roboliq.core._
import roboliq.commands.move._
import roboliq.commands.pipette._
import roboliq.devices.pipette._
import commands.EvowareSubroutineToken
//import roboliq.robots.evoware.commands._


class EvowareTranslator(config: EvowareConfig) {
	def translate(processorResult: ProcessorResult): Result[EvowareScriptBuilder] = {
		val t2 = new EvowareTranslator2(config, processorResult)
		t2.translate()
	}
}

private class EvowareTranslator2(config: EvowareConfig, processorResult: ProcessorResult) {
	private val ob = processorResult.ob
	private val lNode = processorResult.lNode
	private val tracker = processorResult.locationTracker
	
	private var nodeCurrent: CmdNodeBean = null
	
	def translate(): Result[EvowareScriptBuilder] = {
		val builder = new EvowareScriptBuilder(ob)
		translate(lNode, builder).map(_ => builder)
	}
	
	private def translate(lNode: List[CmdNodeBean], builder: EvowareScriptBuilder): Result[Unit] = {
		Result.mapOver(lNode)(node => {
			nodeCurrent = node
			if (node.tokens != null) {
				Result.mapOver(node.tokens.toList) { token => translate(token, builder) }
			}
			else if (node.children != null) {
				translate(node.children.toList, builder)
			}
			else {
				Success()
			}
		}).map(_ => ())
	}
	
	private def translate(cmd1: CmdToken, builder: EvowareScriptBuilder): Result[Unit] = {
		for { cmds0 <- cmd1 match {
			case c: AspirateToken => aspirate(builder, c)
			//case c: L1C_Comment => comment(c)
			case c: DispenseToken => dispense(builder, c)
			//case c: L1C_EvowareFacts => facts(builder, c)
			case c: EvowareSubroutineToken => subroutine(builder, c)
			case c: MixToken => mix(builder, c.items)
			//case c: L1C_MovePlate => movePlate(builder, c.args)
			//case c: L1C_Prompt => prompt(c)
			//case c: L1C_TipsGet => tipsGet(c)
			//case c: L1C_TipsDrop => tipsDrop(c)
			//case c: L1C_Timer => timer(c.args)
			//case c: L1C_SaveCurrentLocation => Success(Seq())
			case c: TipsWashToken => clean(builder, c)
		}} yield {
			builder.cmds ++= cmds0
			()
		}
	}

	def saveWithHeader(
		cmds: Seq[CmdToken],
		tableFile: EvowareTableFile,
		//mapSiteToLabel: Map[CarrierSite, String],
		mapCmdToLabwareInfo: Map[CmdToken, List[Tuple3[CarrierSite, String, LabwareModel]]],
		sFilename: String
	): String = {
		val mapSiteToLabel = new HashMap[CarrierSite, String]
		for (c <- cmds) {
			mapCmdToLabwareInfo.get(c) match {
				case None =>
				case Some(l) =>
					for (info <- l)
						mapSiteToLabel(info._1) = info._2
			}
		}
		val mapSiteToLabwareModel: Map[CarrierSite, LabwareModel] =
			cmds.collect({case c: L0C_Command => c.getSiteToLabwareModelList}).flatten.toMap
		
		val sHeader = tableFile.toStringWithLabware(mapSiteToLabel.toMap, mapSiteToLabwareModel)
		val sCmds = cmds.mkString("\n")
		val fos = new java.io.FileOutputStream(sFilename)
		writeLines(fos, sHeader)
		writeLines(fos, sCmds);
		fos.close();
		sCmds
	}
	
	private def writeLines(output: java.io.FileOutputStream, s: String) {
		val as = s.split("\r?\n")
		for (sLine <- as if !s.isEmpty) {
			val bytes = sLine.map(_.asInstanceOf[Byte]).toArray
			output.write(bytes)
			output.write("\r\n".getBytes())
		}
	}
	
	private def encode(n: Int): Char = ('0' + n).asInstanceOf[Char]
	private def hex(n: Int): Char = Integer.toString(n, 16).toUpperCase.apply(0)
	
	protected def encodeHasTips(list: Iterable[HasTip]): Int =
		list.foldLeft(0) { (sum, x) => sum | (1 << x.tip.index) }
	protected def encodeTips(list: Iterable[Tip]): Int =
		list.foldLeft(0) { (sum, tip) => sum | (1 << tip.index) }

	protected def encodeWells(holder: Plate, aiWells: Traversable[Int]): String = {
		val nWellMaskChars = math.ceil(holder.nRows * holder.nCols / 7.0).asInstanceOf[Int]
		val amWells = new Array[Int](nWellMaskChars)
		for (iWell <- aiWells) {
			val iChar = iWell / 7;
			val iWell1 = iWell % 7;
			amWells(iChar) += 1 << iWell1
		}
		val sWellMask = amWells.map(encode).mkString
		val sPlateMask = Array('0', hex(holder.nCols), '0', hex(holder.nRows)).mkString + sWellMask
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
	

	private def aspirate(builder: EvowareScriptBuilder, cmd: AspirateToken): Result[Seq[L0C_Command]] = {
		spirate(builder, cmd.items, "Aspirate")
	}
	
	private def dispense(builder: EvowareScriptBuilder, cmd: DispenseToken): Result[Seq[L0C_Command]] = {
		spirate(builder, cmd.items, "Dispense")
	}
	 
	private def spirate(builder: EvowareScriptBuilder, items: Seq[SpirateTokenItem], sFunc: String): Result[Seq[L0C_Command]] = {
		items match {
			case Seq() => Error(Seq("INTERNAL: items empty"))
			case Seq(twvp0, rest @ _*) =>
				// Get the liquid class
				val sLiquidClass = twvp0.policy
				// Assert that there is only one liquid class
				// FIXME: for debug only:
				if (!rest.forall(twvp => twvp.policy.equals(sLiquidClass))) {
					println("sLiquidClass: " + sLiquidClass)
					rest.foreach(twvp => println(twvp.tip, twvp.policy))
				}
				// ENDFIX
				assert(rest.forall(twvp => twvp.policy.equals(sLiquidClass)))
				
				val idPlate = twvp0.well.idPlate
				
				// Assert that all tips are of the same kind
				// TODO: Readd this error check somehow? -- ellis, 2011-08-25
				//val tipKind = config.getTipKind(twvp0.tip)
				//assert(items.forall(twvp => robot.getTipKind(twvp.tip) eq tipKind))
				
				if (!items.forall(_.well.idPlate eq idPlate))
					return Error(Seq("INTERNAL: all wells must be on the same plate"))
				
				// All tip/well pairs are equidistant or all tips are going to the same well
				val bEquidistant = Utils.equidistant(items)
				val bSameWell = items.forall(_.well eq twvp0.well)
				if (!bEquidistant && !bSameWell)
					return Error(Seq("INTERNAL: not equidistant, "+TipSet.toDebugString(items.map(_.tip))+" -> "+Printer.getWellsDebugString(items.map(_.well))))
				
				spirateChecked(builder, items, sFunc, sLiquidClass)
		}
	}

	private def spirateChecked(builder: EvowareScriptBuilder, items: Seq[SpirateTokenItem], sFunc: String, sLiquidClass: String): Result[Seq[L0C_Command]] = {
		val item0 = items.head
		//val tipKind = robot.getTipKind(item0.tip)
		val idPlate = item0.well.idPlate
		val mTips = encodeTips(items.map(_.tip))
		
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
		
		for {
			plate <- builder.ob.findPlate(idPlate)
			location <- getLocation(idPlate)
			site <- getSite(location)
		} yield {
			val sPlateMask = encodeWells(plate, items.map(_.well.index))
			val iGrid = config.tableFile.mapCarrierToGrid(site.carrier)
			val labwareModel = config.tableFile.configFile.mapNameToLabwareModel(plate.model.id)
			val cmd = L0C_Spirate(
				sFunc, 
				mTips, sLiquidClass,
				asVolumes,
				iGrid, site.iSite,
				sPlateMask,
				site, labwareModel
			)
			
			builder.mapCmdToLabwareInfo(cmd) = List((site, location, labwareModel))
			
			Seq(cmd)
		}
	}
	
	/*
	private def comment(cmd: L1C_Comment): Result[Seq[CmdToken]] = {
		Success(Seq(L0C_Comment(cmd.s)))
	}
	
	private def execute(cmd: L1C_Execute): Result[Seq[CmdToken]] = {
		val nWaitOpts = (cmd.args.bWaitTillDone, cmd.args.bCheckResult) match {
			case (true, true) => 6
			case _ => 2
		}
		val sResultVar = if (cmd.args.bCheckResult) "RESULT" else ""
		Success(Seq(L0C_Execute(cmd.args.cmd, nWaitOpts, sResultVar)))
	}
	
	private def prompt(cmd: L1C_Prompt): Result[Seq[CmdToken]] = {
		Success(Seq(L0C_Prompt(cmd.s)))
	}
	*/
	
	private def clean(builder: EvowareScriptBuilder, cmd: TipsWashToken): Result[Seq[L0C_Command]] = {
		val lPermanent = cmd.tips.filter(_.modelPermanent_?.isDefined)
		val lModel = lPermanent.map(_.modelPermanent_?.get.id).distinct
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
	
	/*def wash(cmd: TipsWashToken): Result[Seq[CmdToken]] = {
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
	
	private def mix(builder: EvowareScriptBuilder, items: Seq[MixTokenItem]): Result[Seq[L0C_Command]] = {
		items match {
			case Seq() => Error(Seq("Empty Tip-Well-Volume list"))
			case Seq(item0, rest @ _*) =>
				// Get the liquid class
				val sLiquidClass = item0.policy
				// Assert that there is only one liquid class
				if (rest.exists(_.policy != sLiquidClass)) {
					items.foreach(item => println(item.policy))
					return Error(Seq("INTERNAL: Liquid class must be the same for all mix items"))
				}
				if (rest.exists(_.count != item0.count))
					return Error(Seq("INTERNAL: Mix count must be the same for all mix items"))
				
				// Assert that all tips are of the same kind and that all wells are on the same holder
				// TODO: re-add this check
				//val tipKind = robot.getTipKind(tw0.tip)
				//val holder = tw0.well.holder
				//assert(cmd.tws.forall(twv => (robot.getTipKind(twv.tip) eq tipKind) && (twv.well.holder eq holder)))
				
				// Assert that tips are spaced at equal distances to each other as the wells are to each other
				def equidistant2(a: MixTokenItem, b: MixTokenItem): Boolean =
					(b.tip.index - a.tip.index) == (b.well.index - a.well.index)
				// Test all adjacent items for equidistance
				def equidistant(tws: Seq[MixTokenItem]): Boolean = tws match {
					case Seq() => true
					case Seq(_) => true
					case Seq(a, b, rest @ _*) =>
						equidistant2(a, b) match {
							case false => false
							case true => equidistant(Seq(b) ++ rest)
						}
				}
				// All tip/well pairs are equidistant or all tips are going to the same well
				assert(equidistant(items) || items.forall(_.well eq item0.well))
				
				mixChecked(builder, items, sLiquidClass)
		}
	}

	private def mixChecked(builder: EvowareScriptBuilder, items: Seq[MixTokenItem], sLiquidClass: String): Result[Seq[L0C_Command]] = {
		val item0 = items.head
		//val tipKind = robot.getTipKind(item0.tip)
		val idPlate = item0.well.idPlate
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
		val sPlateMask = encodeWells(plate, items.map(_.well.index))
			val iGrid = config.tableFile.mapCarrierToGrid(site.carrier)
			val cmd = L0C_Mix(
				mTips, sLiquidClass,
				asVolumes,
				iGrid, site.iSite,
				sPlateMask,
				item0.count
			)
			
			val labwareModel = config.tableFile.configFile.mapNameToLabwareModel(plate.model.id)
			builder.mapCmdToLabwareInfo(cmd) = List((site, location, labwareModel))
			
			Seq(cmd)
		}
	}
	
	/*
	private def tipsGet(c: L1C_TipsGet): Result[Seq[CmdToken]] = {
		val mTips = encodeTips(c.tips.map(_.obj))
		Success(Seq(L0C_GetDITI2(mTips, c.model.id)))
	}
	
	private def tipsDrop(c: L1C_TipsDrop): Result[Seq[CmdToken]] = {
		val mTips = encodeTips(c.tips.map(_.obj))
		for (site <- getSite(c.location)) yield {
			val iGrid = config.tableFile.mapCarrierToGrid(site.carrier)
			Seq(L0C_DropDITI(mTips, iGrid, site.iSite))
		}
	}
	
	private def movePlate(builder: EvowareScriptBuilder, c: L1A_MovePlateArgs): Result[Seq[CmdToken]] = {
		for {
			siteSrc <- getSite(c.locationSrc)
			siteDest <- getSite(c.locationDest)
		} yield {
			val labwareModel = config.tableFile.configFile.mapNameToLabwareModel(c.sPlateModel)
			
			val carrierSrc = siteSrc.carrier
			val iGridSrc = config.tableFile.mapCarrierToGrid(carrierSrc)
			val lVectorSrc = config.tableFile.configFile.mapCarrierToVectors(carrierSrc)

			val carrierDest = siteDest.carrier
			val iGridDest = config.tableFile.mapCarrierToGrid(carrierDest)
			val lVectorDest = config.tableFile.configFile.mapCarrierToVectors(carrierDest)

			val mapClassToValue = Map("Narrow" -> 0, "Wide" -> 1)
			val lVector1: Map[Tuple2[Int, String], List[Vector]] = (lVectorSrc ++ lVectorDest).groupBy(v => (v.iRoma, v.sClass)).filter(_._2.length == 2)
			val lVector2 = lVector1.toList.sortBy(pair => pair._1._1.toString + mapClassToValue.getOrElse(pair._1._2, pair._1._2))
			// TODO: figure out an intermediate path to follow instead (e.g. via a re-grip location)
			if (lVector2.isEmpty) 
				return Error("no common RoMa: "+carrierSrc.sName+" and "+carrierDest.sName)
			val (iRoma, sClass) = lVector2.head._1
			
			val cmd = L0C_Transfer_Rack(
				iRoma,
				sClass,
				//c.sPlateModel,
				//iGridSrc, siteSrc.iSite, siteSrc.carrier.sName,
				//iGridDest, siteDest.iSite, siteDest.carrier.sName,
				labwareModel,
				iGridSrc, siteSrc,
				iGridDest, siteDest,
				c.lidHandling,
				iGridLid = 0,
				iSiteLid = 0,
				sCarrierLid = ""
			)
			
			builder.mapCmdToLabwareInfo(cmd) = List(
				(siteSrc, c.locationSrc, labwareModel),
				(siteDest, c.locationDest, labwareModel)
			)
			
			Seq(cmd)
		}
	}
	
	private def timer(args: L12A_TimerArgs): Result[Seq[CmdToken]] = {
		Success(Seq(
			L0C_StartTimer(1),
			L0C_WaitTimer(1, args.nSeconds)
		))
	}
	
	private def facts(builder: EvowareScriptBuilder, cmd: L1C_EvowareFacts): Result[Seq[CmdToken]] = {
		Success(Seq(L0C_Facts(
			sDevice = cmd.args.sDevice,
			sVariable = cmd.args.sVariable,
			sValue = cmd.args.sValue
		)))
	}
	*/
	
	private def subroutine(builder: EvowareScriptBuilder, cmd: EvowareSubroutineToken): Result[Seq[L0C_Command]] = {
		Success(Seq(L0C_Subroutine(
			cmd.sFilename
		)))
	}
	
	private def getLocation(idPlate: String): Result[String] = {
		tracker.getLocationForCommand(idPlate, nodeCurrent.index)
	}
	
	private def getSite(location: String): Result[CarrierSite] = {
		config.mapLabelToSite.get(location) match {
			case None =>
				println("INTERNAL: missing evoware site for location \""+location+"\"")
				println("config.mapLabelToSite: "+config.mapLabelToSite)
				Error(Seq("INTERNAL: missing evoware site for location \""+location+"\""))
			case Some(site) => Success(site)
		}
	}
	
	/*
	private def addLabware(builder: EvowareScriptBuilder, labwareModel: LabwareModel, location: String): Result[Unit] = {
		val site = config.mapLabelToSite(location)
		val labwareModel = config.tableFile.mapSiteToLabwareModel(site)
		
		//val labwareModel = config.tableFile.configFile.mapNameToLabwareModel(sLabwareModel)
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
	
	private def addLabware(builder: EvowareScriptBuilder, sLabwareModel: String, location: String): Result[Unit] = {
		val labwareModel = config.tableFile.configFile.mapNameToLabwareModel(sLabwareModel)
		addLabware(builder, labwareModel, location)
	}
	
	private def addLabware(builder: EvowareScriptBuilder, plate: Plate, location: String): Result[Unit] = {
		if (plate.model_?.isEmpty)
			//return Error("plate model must be assigned to plate \""+plate+"\"")
			return Success(())
		addLabware(builder, plate.model_?.get.id, location)
	}
	*/
}
