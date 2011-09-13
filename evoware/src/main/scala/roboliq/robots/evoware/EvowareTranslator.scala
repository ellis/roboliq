package roboliq.robots.evoware

import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter

import roboliq.common._
import roboliq.compiler._
import roboliq.commands._
import roboliq.commands.move._
import roboliq.commands.pipette._
import roboliq.devices.pipette._
import roboliq.robots.evoware.commands._


class EvowareTranslator(system: EvowareConfig) extends Translator {
	override def addKnowledge(kb: KnowledgeBase) {
		// Do nothing
	}
	
	override def translate(cmd: CommandL1): Result[Seq[Command]] = cmd match {
		case t @ L1C_Aspirate(_) => aspirate(t)
		case t @ L1C_Dispense(_) => dispense(t)
		case t @ L1C_Mix(_) => mix(t.items)
		case c: L1C_Wash => wash(c)
		case c: L1C_TipsGet => tipsGet(c)
		case c: L1C_TipsDrop => tipsDrop(c)
		case c: L1C_MovePlate => movePlate(c.args)
		case c: L1C_Timer => timer(c.args)
		case c: L1C_EvowareFacts => facts(c)
		case c: L1C_SaveCurrentLocation => Success(Seq())
	}

	/*def translate(rs: Seq[CompileFinal]): Result[Seq[Command]] = {
		Success(rs.flatMap(r => {
			translate(r.cmd) match {
				case Error(err) => return Error(err)
				case Success(cmds) => cmds
			}
		}))
	}*/

	//def translateToString(txs: Seq[CompileFinal]): String = translate(txs).right.get.mkString("\n")
	def translateToString(cmds: Seq[CommandL1]): String = translate(cmds).right.get.cmds.mkString("\n")

	/*def translateAndSave(
		cmds: Seq[CommandL1],
		mapLabware: EvowareTranslatorHeader.LabwareMap,
		sFilename: String
	): String = {
		val s = translateToString(cmds)
		val fos = new java.io.FileOutputStream(sFilename)
		writeLines(fos, EvowareTranslatorHeader.getHeader(mapLabware))
		writeLines(fos, s);
		fos.close();
		s
	}*/
	
	def saveWithHeader(
		cmds: Seq[Command],
		sHeader: String,
		mapLabware: EvowareTranslatorHeader.LabwareMap,
		sFilename: String
	): String = {
		val s = cmds.mkString("\n")
		val fos = new java.io.FileOutputStream(sFilename)
		writeLines(fos, EvowareTranslatorHeader.getHeader(sHeader, mapLabware))
		writeLines(fos, s);
		fos.close();
		s
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

	protected def encodeWells(holder: PlateConfigL2, aiWells: Traversable[Int]): String = {
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
	

	private def aspirate(cmd: L1C_Aspirate): Result[Seq[Command]] = {
		spirate(cmd.items, "Aspirate")
	}
	
	private def dispense(cmd: L1C_Dispense): Result[Seq[Command]] = {
		spirate(cmd.items, "Dispense")
	}
	 
	private def spirate(items: Seq[L1A_SpirateItem], sFunc: String): Result[Seq[Command]] = {
		items match {
			case Seq() => Error(Seq("INTERNAL: items empty"))
			case Seq(twvp0, rest @ _*) =>
				// Get the liquid class
				val sLiquidClass = twvp0.policy.id
				// Assert that there is only one liquid class
				assert(rest.forall(twvp => twvp.policy.id.equals(sLiquidClass)))
				
				val holder = twvp0.well.holder
				
				// Assert that all tips are of the same kind
				// TODO: Readd this error check somehow? -- ellis, 2011-08-25
				//val tipKind = system.getTipKind(twvp0.tip)
				//assert(items.forall(twvp => robot.getTipKind(twvp.tip) eq tipKind))
				
				if (!items.forall(_.well.holder eq holder))
					return Error(Seq("INTERNAL: all wells must be on the same plate"))
				
				// Assert that tips are spaced at equal distances to each other as the wells are to each other
				def equidistant2(a: L1A_SpirateItem, b: L1A_SpirateItem): Boolean =
					(b.tip.index - a.tip.index) == (b.well.index - a.well.index)
				// Test all adjacent items for equidistance
				def equidistant(item: Seq[L1A_SpirateItem]): Boolean = item match {
					case Seq() => true
					case Seq(_) => true
					case Seq(a, b, rest @ _*) =>
						equidistant2(a, b) match {
							case false => false
							case true => equidistant(Seq(b) ++ rest)
						}
				}
				// All tip/well pairs are equidistant or all tips are going to the same well
				val bEquidistant = equidistant(items)
				val bSameWell = items.forall(_.well eq twvp0.well)
				if (!bEquidistant && !bSameWell)
					return Error(Seq("INTERNAL: not equidistant, "+TipSet.toDebugString(items.map(_.tip))+" -> "+Command.getWellsDebugString(items.map(_.well))))
				
				spirateChecked(items, sFunc, sLiquidClass)
		}
	}

	private def spirateChecked(items: Seq[L1A_SpirateItem], sFunc: String, sLiquidClass: String): Result[Seq[Command]] = {
		val item0 = items.head
		//val tipKind = robot.getTipKind(item0.tip)
		val holder = item0.well.holder
		val mTips = encodeTips(items.map(_.tip.obj))
		
		// Create a list of volumes for each used tip, leaving the remaining values at 0
		val asVolumes = Array.fill(12)("0")
		val fmt = new java.text.DecimalFormat("#.##")
		for (twv <- items) {
			val iTip = twv.tip.index
			assert(iTip >= 0 && iTip < 12)
			asVolumes(iTip) = "\""+fmt.format(twv.nVolume)+'"'
		}
		//val sVolumes = asVolumes.mkString(",")
		
		val sPlateMask = encodeWells(holder, items.map(_.well.index))
		
		// Find a parent of 'holder' which has an Evoware location (x-grid/y-site)
		system.mapSites.get(item0.location) match {
			case None => return Error(Seq("INTERNAL: missing evoware site for location "+item0.location))
			case Some(site) =>
				Success(Seq(L0C_Spirate(
					sFunc, 
					mTips, sLiquidClass,
					asVolumes,
					site.iGrid, site.iSite,
					sPlateMask
				)))
		}
	}
	
	def wash(cmd: L1C_Wash): Result[Seq[Command]] = {
		system.mapWashProgramArgs.get(cmd.iWashProgram) match {
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
	}
	
	private def mix(items: Seq[L1A_MixItem]): Result[Seq[Command]] = {
		items match {
			case Seq() => Error(Seq("Empty Tip-Well-Volume list"))
			case Seq(item0, rest @ _*) =>
				// Get the liquid class
				val sLiquidClass = item0.policy.id
				// Assert that there is only one liquid class
				if (rest.exists(_.policy.id != sLiquidClass)) {
					items.foreach(item => println(item.policy))
					return Error(Seq("INTERNAL: Liquid class must be the same for all mix items"))
				}
				if (rest.exists(_.nCount != item0.nCount))
					return Error(Seq("INTERNAL: Mix count must be the same for all mix items"))
				
				// Assert that all tips are of the same kind and that all wells are on the same holder
				// TODO: re-add this check
				//val tipKind = robot.getTipKind(tw0.tip)
				//val holder = tw0.well.holder
				//assert(cmd.tws.forall(twv => (robot.getTipKind(twv.tip) eq tipKind) && (twv.well.holder eq holder)))
				
				// Assert that tips are spaced at equal distances to each other as the wells are to each other
				def equidistant2(a: L1A_MixItem, b: L1A_MixItem): Boolean =
					(b.tip.index - a.tip.index) == (b.well.index - a.well.index)
				// Test all adjacent items for equidistance
				def equidistant(tws: Seq[L1A_MixItem]): Boolean = tws match {
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
				
				mixChecked(items, sLiquidClass)
		}
	}

	private def mixChecked(items: Seq[L1A_MixItem], sLiquidClass: String): Result[Seq[Command]] = {
		val item0 = items.head
		//val tipKind = robot.getTipKind(item0.tip)
		val holder = item0.well.holder
		val mTips = encodeTips(items.map(_.tip.obj))
		
		// Create a list of volumes for each used tip, leaving the remaining values at 0
		val asVolumes = Array.fill(12)("0")
		val fmt = new java.text.DecimalFormat("#.##")
		for (item <- items) {
			val iTip = item.tip.index
			assert(iTip >= 0 && iTip < 12)
			asVolumes(iTip) = "\""+fmt.format(item.nVolume)+'"'
		}
		//val sVolumes = asVolumes.mkString(",")
		
		val sPlateMask = encodeWells(holder, items.map(_.well.index))
		
		system.mapSites.get(item0.location) match {
			case None => return Error(Seq("INTERNAL: missing evoware site for location "+item0.location))
			case Some(site) =>
				Success(Seq(L0C_Mix(
					mTips, sLiquidClass,
					asVolumes,
					site.iGrid, site.iSite,
					sPlateMask,
					item0.nCount
				)))
		}
	}
	
	private def tipsGet(c: L1C_TipsGet): Result[Seq[Command]] = {
		val mTips = encodeTips(c.tips.map(_.obj))
		Success(Seq(L0C_GetDITI2(mTips, c.model.id)))
	}
	
	private def tipsDrop(c: L1C_TipsDrop): Result[Seq[Command]] = {
		val mTips = encodeTips(c.tips.map(_.obj))
		system.mapSites.get(c.location) match {
			case None => return Error(Seq("INTERNAL: missing evoware site for location "+c.location))
			case Some(site) =>
				Success(Seq(L0C_DropDITI(mTips, site.iGrid, site.iSite)))
		}
	}
	
	private def movePlate(c: L1A_MovePlateArgs): Result[Seq[Command]] = {
		val siteSrc = getSite(c.locationSrc) match {
			case Error(lsError) => return Error(lsError)
			case Success(site) => site
		}
		val siteDest = getSite(c.locationDest) match {
			case Error(lsError) => return Error(lsError)
			case Success(site) => site
		}
		val carrierSrc = getCarrier(c.locationSrc) match {
			case Error(lsError) => return Error(lsError)
			case Success(carrier) => carrier
		}
		val carrierDest = getCarrier(c.locationDest) match {
			case Error(lsError) => return Error(lsError)
			case Success(carrier) => carrier
		}
		Success(Seq(L0C_Transfer_Rack(
			c.iRoma,
			c.sPlateModel,
			siteSrc.iGrid, siteSrc.iSite, carrierSrc.model.sName,
			siteDest.iGrid, siteDest.iSite, carrierDest.model.sName,
			c.lidHandling,
			iGridLid = 0,
			iSiteLid = 0,
			sCarrierModelLid = ""
		)))
	}
	
	private def getSite(location: String): Result[SiteObj] = {
		system.mapSites.get(location) match {
			case None => Error(Seq("INTERNAL: missing evoware site for location \""+location+"\""))
			case Some(site) => Success(site)
		}
	}
	
	private def getCarrier(location: String): Result[CarrierObj] = {
		system.mapSites.get(location) match {
			case None => Error(Seq("INTERNAL: no carrier declared at location \""+location+"\""))
			case Some(site) => Success(site.carrier)
		}
	}
	
	private def timer(args: L12A_TimerArgs): Result[Seq[Command]] = {
		Success(Seq(
			L0C_StartTimer(1),
			L0C_WaitTimer(1, args.nSeconds)
		))
	}
	
	private def facts(cmd: L1C_EvowareFacts): Result[Seq[Command]] = {
		Success(Seq(L0C_Facts(
			sDevice = cmd.args.sDevice,
			sVariable = cmd.args.sVariable,
			sValue = cmd.args.sValue
		)))
	}
}
