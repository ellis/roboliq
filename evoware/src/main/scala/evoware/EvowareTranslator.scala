package evoware

import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter
import roboliq.parts._
import roboliq.tokens._
import roboliq.robot._


abstract class EvowareTranslator(robot: EvowareRobot) {
	def translate(st: Tuple2[RobotState, T1_Token]): Seq[Tuple2[T0_Token, RobotState]] = st._2 match {
		case t @ T1_Aspirate(_) => aspirate(st._1, t)
		case t @ T1_Dispense(_) => dispense(st._1, t)
		case t @ T1_Clean(_, _) => clean(t)
	}

	def translate(toks: Seq[Tuple2[RobotState, T1_Token]]): Seq[Tuple2[T0_Token, RobotState]] = toks.flatMap(translate)

	def translateToString(toks: Seq[Tuple2[RobotState, T1_Token]]): String = translate(toks).map(_._1).mkString("\n")

	def translateAndSave(cmds: Seq[Tuple2[RobotState, T1_Token]], sFilename: String): String = {
		val s = translateToString(cmds)
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

	protected def encodeWells(holder: WellHolder, aiWells: Traversable[Int]): String = {
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
	//settings: EvowareSettings, state: IRobotState
	//val state = robot.state
	

	private def aspirate(state0: RobotState, tok: T1_Aspirate): Seq[Tuple2[T0_Token, RobotState]] = {
		val tok0 = spirate(state0, tok.twvs, "Aspirate", robot.getAspirateClass)
		val state = tok0 match {
			case T0_Spirate(_, _, _, _, _, _, _) =>
				val builder = new RobotStateBuilder(state0)
				tok.twvs.foreach(builder.aspirate)
				builder.toImmutable
			case _ =>
				state0
		}
		(tok0, state) :: Nil
	}
	
	private def dispense(state0: RobotState, tok: T1_Dispense): Seq[Tuple2[T0_Token, RobotState]] = {
		val tok0 = spirate(state0, tok.twvs, "Aspirate", robot.getDispenseClass)
		val state = tok0 match {
			case T0_Spirate(_, _, _, _, _, _, _) =>
				val builder = new RobotStateBuilder(state0)
				tok.twvs.foreach(builder.aspirate)
				builder.toImmutable
			case _ =>
				state0
		}
		(tok0, state) :: Nil
	}
	
	private def spirate(state0: RobotState, twvs: Seq[TipWellVolume], sFunc: String, getLiquidClass: ((RobotState, TipWellVolume) => Option[String])): T0_Token = {
		twvs match {
			case Seq() => T0_Error("Empty Tip-Well-Volume list")
			case Seq(twv0, rest @ _*) =>
				// Get the liquid class
				val sLiquidClass_? = getLiquidClass(state0, twv0)
				assert(sLiquidClass_?.isDefined)
				val sLiquidClass = sLiquidClass_?.get
				//println("sLiquidClass = "+sLiquidClass)
				//for (twv <- rest) println(robot.getAspirateClass(twv))
				// Assert that there is only one liquid class
				assert(rest.forall(twv => getLiquidClass(state0, twv) == sLiquidClass_?))
				
				val tipKind = robot.getTipKind(twv0.tip)
				val holder = twv0.well.holder
				
				// Assert that all tips are of the same kind and that all wells are on the same holder
				assert(twvs.forall(twv => (robot.getTipKind(twv.tip) eq tipKind) && (twv.well.holder eq holder)))
				
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
				assert(equidistant(twvs) || twvs.forall(_.well eq twv0.well))
				
				spirateChecked(state0, twvs, sFunc, sLiquidClass)
		}
	}

	private def spirateChecked(state0: RobotState, twvs: Seq[TipWellVolume], sFunc: String, sLiquidClass: String): T0_Token = {
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
		val siteList = robot.state.getSiteList(holder).reverse.take(2).toArray
		assert(siteList.size == 2)
		val iGrid = siteList(0).index
		val iSite = siteList(1).index
		
		T0_Spirate(
			sFunc, 
			mTips, sLiquidClass,
			asVolumes,
			iGrid, iSite,
			sPlateMask
		)
	}
	
	/** Get T0 tokens for cleaning */
	def clean(tok: T1_Clean): List[Tuple2[T0_Token, RobotState]]

	// Cursor:
	//  1,1+1: 010810, 0011 0001 0011 0000
	//  1,2+1: 010820, 0011 0010
	//  1,3+1: 010840, 0011 0100
	//  1,4+1: 010880, 0011 1000

	//  1,5+1: 0108@0, 0100 0000
	//  1,6+1: 0108P0, 0101 0000
	//  1,7+1: 0108p0, 0111 0000
	//  1,8+1: 010801, 0011 0000 0011 0001

	//  1,1+3: 010830,     0011 0011
	//  1,4+3: 0108H0,     0100 1000
	//  1,5+3: 0108`0,     0110 0000
	//  1,6+3: 0108{144}0, 1001 0000
	//  1,7+3: 0108p1,     0111 0000 0011 0001
	
	//  1,1+7: 010870,     0011 0111
	//  1,2+7: 0108>0,     0011 1110
	//  1,3+7: 0108L0,     0100 1100
	//  1,4+7: 0108h0,     0110 1000
	//  1,5+7: 0108{160}0, 1010 0000
	
	//  1,1+f: 0108?0,     0011 1111
	//  1,2+f: 0108N0,     0100 1110
	//	1,3+f: 0108l0,     0110 1100
	//  1,4+f: 0108{168}0, 1010 1000
	//  1,5+f: 0108{160}1, 1010 0000 0011 0001

	//  1,1+1f: 0108O0,     0100 1111

	//  1,1+ff: 0108{175}0
	
	// So the meaning of the upper 4 bits is:
	// 0011: wells 5-7 are empty
	// 0100: well 5 is filled
	// 0101: well 6 is filled
	// 0110: wells 5 & 6 are filled
	// 0111: well 7 is filled
	// 1001: wells 6 and 7 are filled
	// 1010: wells 5, 6, 7 are filled
}
