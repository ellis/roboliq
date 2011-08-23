package evoware

import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter

import roboliq.common._
import roboliq.compiler._
import roboliq.commands.pipette._
import roboliq.devices.pipette._


abstract class EvowareTranslator(mapper: EvowareMapper) {
	def translate(cmd: Command): Either[Seq[String], Seq[Command]] = cmd match {
		case t @ L1C_Aspirate(_) => aspirate(t)
		case t @ L1C_Dispense(_) => dispense(t)
		case t @ L1C_Mix(_) => mix(t)
		case t @ L1C_Wash(_, _, _) => clean(t)
	}

	def translate(rs: Seq[CompileFinal]): Seq[Command] = {
		var state = state0
		rs.flatMap(r => {
			val t0s = translate(state, r.cmd)
			state = r.state1
			t0s
		})
	}

	def translateToString(txs: Seq[CompileFinal]): String = translate(txs).mkString("\n")

	def translateAndSave(txs: Seq[CompileFinal], sFilename: String): String = {
		val s = translateToString(txs)
		val fos = new java.io.FileOutputStream(sFilename)
		writeLines(fos, EvowareTranslatorHeader.getHeader())
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

	protected def encodeWells(holder: PlateConfigL1, aiWells: Traversable[Int]): String = {
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
	

	private def aspirate(cmd: L1C_Aspirate): Either[Seq[String], Command] = {
		spirate(cmd.items, "Aspirate")
	}
	
	private def dispense(cmd: L1C_Dispense): Either[Seq[String], Command] = {
		spirate(cmd.items, "Dispense")
	}
	
	private def spirate(twvps: Seq[TipWellVolumePolicy], sFunc: String): Either[Seq[String], Command] = {
		twvps match {
			case Seq() => Left(Seq("INTERNAL: twvps empty"))
			case Seq(twvp0, rest @ _*) =>
				// Get the liquid class
				val sLiquidClass = twvp0.policy.sName
				// Assert that there is only one liquid class
				assert(rest.forall(twvp => twvp.policy.sName.equals(sLiquidClass)))
				
				val tipKind = mapper.getTipKind(twvp0.tip)
				val holder = twvp0.well.holder
				
				// Assert that all tips are of the same kind and that all wells are on the same holder
				assert(twvps.forall(twvp => (robot.getTipKind(twvp.tip) eq tipKind) && (twvp.well.holder eq holder)))
				
				// Assert that tips are spaced at equal distances to each other as the wells are to each other
				def equidistant2(a: TipWellVolume, b: TipWellVolume): Boolean =
					(b.tip.index - a.tip.index) == (b.well.index - a.well.index)
				// Test all adjacent items for equidistance
				def equidistant(twvs: Seq[TipWellVolume]): Boolean = twvs match {
					case Seq() => true
					case Seq(_) => true
					case Seq(a, b, rest @ _*) =>
						equidistant2(a, b) match {
							case false => false
							case true => equidistant(Seq(b) ++ rest)
						}
				}
				// All tip/well pairs are equidistant or all tips are going to the same well
				assert(equidistant(twvps) || twvps.forall(_.well eq twvp0.well))
				
				spirateChecked(twvps, sFunc, sLiquidClass)
		}
	}

	private def spirateChecked(twvs: Seq[TipWellVolumePolicy], sFunc: String, sLiquidClass: String): Either[Seq[String], Command] = {
		val twv0 = twvs.head
		//val tipKind = robot.getTipKind(twv0.tip)
		val holder = twv0.well.holder
		val mTips = encodeHasTips(twvs)
		
		// Create a list of volumes for each used tip, leaving the remaining values at 0
		val asVolumes = Array.fill(12)("0")
		val fmt = new java.text.DecimalFormat("#.##")
		for (twv <- twvs) {
			val iTip = twv.tip.index
			assert(iTip >= 0 && iTip < 12)
			asVolumes(iTip) = "\""+fmt.format(twv.nVolume)+'"'
		}
		//val sVolumes = asVolumes.mkString(",")
		
		val sPlateMask = encodeWells(holder, twvs.map(_.well.index))
		
		// Find a parent of 'holder' which has an Evoware location (x-grid/y-site)
		val siteList = mapper.mapSites.get(holder.).reverse.take(2).toArray
		assert(siteList.size == 2)
		val iGrid = siteList(0).index
		val iSite = siteList(1).index
		
		Right(L0C_Spirate(
			sFunc, 
			mTips, sLiquidClass,
			asVolumes,
			iGrid, iSite,
			sPlateMask
		))
	}
	
	/** Get T0 tokens for cleaning */
	def clean(cmd: L1C_Clean): List[Command]
	
	private def mix(cmd: L1C_Mix): Seq[Command] = {
		val t0 = mix2(cmd, robot.getDispenseClass)
		t0 :: Nil
	}
	
	private def mix2(cmd: L1C_Mix, getLiquidClass: ((RobotState, TipWellVolume) => Option[String])): Command = {
		cmd.tws match {
			case Seq() => CommandError("Empty Tip-Well-Volume list")
			case Seq(tw0, rest @ _*) =>
				// Get the liquid class
				val sLiquidClass_? = getLiquidClass(new TipWellVolume(tw0.tip, tw0.well, cmd.nVolume))
				assert(sLiquidClass_?.isDefined)
				val sLiquidClass = sLiquidClass_?.get
				//println("sLiquidClass = "+sLiquidClass)
				//for (twv <- rest) println(robot.getAspirateClass(twv))
				// Assert that there is only one liquid class
				assert(rest.forall(tw => getLiquidClass(new TipWellVolume(tw.tip, tw.well, cmd.nVolume)) == sLiquidClass_?))
				
				val tipKind = robot.getTipKind(tw0.tip)
				val holder = tw0.well.holder
				
				// Assert that all tips are of the same kind and that all wells are on the same holder
				assert(cmd.tws.forall(twv => (robot.getTipKind(twv.tip) eq tipKind) && (twv.well.holder eq holder)))
				
				// Assert that tips are spaced at equal distances to each other as the wells are to each other
				def equidistant2(a: TipWell, b: TipWell): Boolean =
					(b.tip.index - a.tip.index) == (b.well.index - a.well.index)
				// Test all adjacent items for equidistance
				def equidistant(tws: Seq[TipWell]): Boolean = tws match {
					case Seq() => true
					case Seq(_) => true
					case Seq(a, b, rest @ _*) =>
						equidistant2(a, b) match {
							case false => false
							case true => equidistant(Seq(b) ++ rest)
						}
				}
				// All tip/well pairs are equidistant or all tips are going to the same well
				assert(equidistant(cmd.tws) || cmd.tws.forall(_.well eq tw0.well))
				
				mixChecked(cmd, sLiquidClass)
		}
	}

	private def mixChecked(cmd: L1C_Mix, sLiquidClass: String): Command = {
		val twv0 = cmd.tws.head
		val tipKind = robot.getTipKind(twv0.tip)
		val holder = twv0.well.holder
		val mTips = encodeHasTips(cmd.tws)
		
		// Create a list of volumes for each used tip, leaving the remaining values at 0
		val asVolumes = Array.fill(12)("0")
		val fmt = new java.text.DecimalFormat("#.##")
		for (twv <- cmd.tws) {
			val iTip = twv.tip.index
			assert(iTip >= 0 && iTip < 12)
			asVolumes(iTip) = "\""+fmt.format(cmd.nVolume)+'"'
		}
		//val sVolumes = asVolumes.mkString(",")
		
		val sPlateMask = encodeWells(holder, cmd.tws.map(_.well.index))
		
		// Find a parent of 'holder' which has an Evoware location (x-grid/y-site)
		val siteList = state0.getSiteList(holder).reverse.take(2).toArray
		assert(siteList.size == 2)
		val iGrid = siteList(0).index
		val iSite = siteList(1).index
		
		L0C_Mix(
			mTips, sLiquidClass,
			asVolumes,
			iGrid, iSite,
			sPlateMask
		)
	}
}
