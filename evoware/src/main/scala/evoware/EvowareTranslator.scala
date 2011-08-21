package evoware

import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter

import roboliq.common._
import roboliq.compiler._
import roboliq.commands.pipette._
import roboliq.devices.pipette._


abstract class EvowareTranslator(robot: EvowareRobot) {
	def translate(state0: RobotState, cmd: Command): Seq[Command] = cmd match {
		case t @ L1C_Aspirate(_) => aspirate(state0, t)
		case t @ L1C_Dispense(_) => dispense(state0, t)
		case t @ L1C_Mix(_) => mix(state0, t)
		case t @ L1C_Wash(_, _, _) => clean(state0, t)
	}

	def translate(state0: RobotState, rs: Seq[CompileFinal]): Seq[Command] = {
		var state = state0
		rs.flatMap(r => {
			val t0s = translate(state, r.cmd)
			state = r.state1
			t0s
		})
	}

	def translateToString(state0: RobotState, txs: Seq[CompileFinal]): String = translate(state0, txs).mkString("\n")

	def translateAndSave(state0: RobotState, txs: Seq[CompileFinal], sFilename: String): String = {
		val s = translateToString(state0, txs)
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
	

	private def aspirate(state0: RobotState, tok: L1C_Aspirate): Either[String, Command] = {
		spirate(state0, tok.twvs, "Aspirate")
	}
	
	private def dispense(state0: RobotState, tok: L1C_Dispense): Either[String, Command] = {
		spirate(state0, tok.twvs, "Dispense")
	}
	
	private def spirate(state0: RobotState, twvps: Seq[TipWellVolumePolicy], sFunc: String): Either[String, Command] = {
		twvps match {
			case Seq() => Left("INTERNAL: twvps empty")
			case Seq(twvp0, rest @ _*) =>
				// Get the liquid class
				val sLiquidClass = twvp0.policy.sName
				// Assert that there is only one liquid class
				assert(rest.forall(twvp => twvp.policy.sName.equals(sLiquidClass)))
				
				val tipKind = robot.getTipKind(twvp0.tip)
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
				
				spirateChecked(state0, twvps, sFunc, sLiquidClass)
		}
	}

	private def spirateChecked(state0: RobotState, twvs: Seq[TipWellVolumePolicy], sFunc: String, sLiquidClass: String): Either[String, Command] = {
		val twv0 = twvs.head
		val tipKind = robot.getTipKind(twv0.tip)
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
		val siteList = state0.getSiteList(holder).reverse.take(2).toArray
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
	def clean(state0: RobotState, tok: T1_Clean): List[T0_Token]
	
	private def mix(state0: RobotState, tok: T1_Mix): Seq[T0_Token] = {
		val t0 = mix2(state0, tok, robot.getDispenseClass)
		t0 :: Nil
	}
	
	private def mix2(state0: RobotState, tok: T1_Mix, getLiquidClass: ((RobotState, TipWellVolume) => Option[String])): T0_Token = {
		tok.tws match {
			case Seq() => T0_TokenError("Empty Tip-Well-Volume list")
			case Seq(tw0, rest @ _*) =>
				// Get the liquid class
				val sLiquidClass_? = getLiquidClass(state0, new TipWellVolume(tw0.tip, tw0.well, tok.nVolume))
				assert(sLiquidClass_?.isDefined)
				val sLiquidClass = sLiquidClass_?.get
				//println("sLiquidClass = "+sLiquidClass)
				//for (twv <- rest) println(robot.getAspirateClass(twv))
				// Assert that there is only one liquid class
				assert(rest.forall(tw => getLiquidClass(state0, new TipWellVolume(tw.tip, tw.well, tok.nVolume)) == sLiquidClass_?))
				
				val tipKind = robot.getTipKind(tw0.tip)
				val holder = tw0.well.holder
				
				// Assert that all tips are of the same kind and that all wells are on the same holder
				assert(tok.tws.forall(twv => (robot.getTipKind(twv.tip) eq tipKind) && (twv.well.holder eq holder)))
				
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
				assert(equidistant(tok.tws) || tok.tws.forall(_.well eq tw0.well))
				
				mixChecked(state0, tok, sLiquidClass)
		}
	}

	private def mixChecked(state0: RobotState, tok: T1_Mix, sLiquidClass: String): T0_Token = {
		val twv0 = tok.tws.head
		val tipKind = robot.getTipKind(twv0.tip)
		val holder = twv0.well.holder
		val mTips = encodeHasTips(tok.tws)
		
		// Create a list of volumes for each used tip, leaving the remaining values at 0
		val asVolumes = Array.fill(12)("0")
		val fmt = new java.text.DecimalFormat("#.##")
		for (twv <- tok.tws) {
			val iTip = twv.tip.index
			assert(iTip >= 0 && iTip < 12)
			asVolumes(iTip) = "\""+fmt.format(tok.nVolume)+'"'
		}
		//val sVolumes = asVolumes.mkString(",")
		
		val sPlateMask = encodeWells(holder, tok.tws.map(_.well.index))
		
		// Find a parent of 'holder' which has an Evoware location (x-grid/y-site)
		val siteList = state0.getSiteList(holder).reverse.take(2).toArray
		assert(siteList.size == 2)
		val iGrid = siteList(0).index
		val iSite = siteList(1).index
		
		T0_Mix(
			mTips, sLiquidClass,
			asVolumes,
			iGrid, iSite,
			sPlateMask
		)
	}
}
