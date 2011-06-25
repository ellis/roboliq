package evoware

import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter
import roboliq.parts._
import roboliq.tokens._
import roboliq.robot._


abstract class EvowareTranslator(robot: EvowareRobot) {
	def translate(tok: T1_Token): Seq[T0_Token] = tok match {
		case t @ T1_Aspirate(_) => spirate(t.twvs, "Aspirate", robot.getAspirateClass) :: Nil
		case t @ T1_Dispense(_) => spirate(t.twvs, "Dispense", robot.getDispenseClass) :: Nil
		case t @ T1_Clean(_, _) => clean(t)
	}

	def translate(toks: Seq[T1_Token]): Seq[T0_Token] = toks.flatMap(translate)

	def translateToString(toks: Seq[T1_Token]): String = translate(toks).mkString("\n")

	def translateAndSave(cmds: Seq[T1_Token], sFilename: String): String = {
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
	val state = robot.state
	

	private def spirate(twvs: Seq[TipWellVolume], sFunc: String, getLiquidClass: (TipWellVolume => Option[String])): T0_Token = {
		twvs match {
			case Seq() => T0_Error("Empty Tip-Well-Volume list")
			case Seq(twv0, rest @ _*) =>
				// Get the liquid class
				val sLiquidClass_? = getLiquidClass(twv0)
				assert(sLiquidClass_?.isDefined)
				val sLiquidClass = sLiquidClass_?.get
				//println("sLiquidClass = "+sLiquidClass)
				//for (twv <- rest) println(robot.getAspirateClass(twv))
				// Assert that there is only one liquid class
				assert(rest.forall(twv => getLiquidClass(twv) == sLiquidClass_?))
				
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
				
				spirateChecked(twvs, sFunc, sLiquidClass);
		}
	}

	private def spirateChecked(twvs: Seq[TipWellVolume], sFunc: String, sLiquidClass: String): T0_Token = {
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
		def findLoc(part: Part): Option[Tuple2[Int, Int]] = {
			val site_? = state.getSite(part)
			site_? match {
				case None => None
				case Some(site) =>
					robot.getGridIndex(site.parent) match {
						case Some(iGrid) => Some(iGrid, site.index)
						case None => findLoc(site.parent)
					}
			}
		}
		val (iGrid, iSite) = findLoc(holder).get
		
		T0_Spirate(
			sFunc, 
			mTips, sLiquidClass,
			asVolumes,
			iGrid, iSite,
			sPlateMask
		)
	}
	
	/** Get T0 tokens for cleaning */
	def clean(tok: T1_Clean): List[T0_Token]

	/*
	private def partitionBy[T](list: List[T], fn: (T, T) => Boolean): List[List[T]] = {
		list match {
			case Nil => Nil
			case x :: xs =>
				val (sublist, rest) = list.partition(y => fn(x, y))
				sublist :: partitionBy(rest, fn)
		}
	}
	
	private def partitionTipKinds(twvs: List[TipWellVolume]): List[List[TipWellVolume]] = {
		def sameTipKind(a: TipWellVolume, b: TipWellVolume): Boolean = {
			val ka = robot.getTipKind(a.tip)
			val kb = robot.getTipKind(b.tip)
			ka == kb
		}
		partitionBy(twvs, sameTipKind)
	}*/

	/*
	private def spirate(String sFunc, int mTipsMask, int iGrid, int iSite, int iWell0, String sLiquidClass, double nVolume) {
		// Put nVolume into each position of anVolumes where mTipsMask has a bit set
		final double[] anVolumes = new double[8];
		for (int i = 0; i < 8; i++) {
			final int mask = 1 << i;
			if ((mTipsMask & mask) != 0)
				anVolumes[i] = nVolume;
		}
		
		final int mWellMask = mTipsMask << iWell0;
		final char cCode4 = (char) ('0' + (mWellMask & 0x7f)); // Add the bits for wells 1-7 to byte 4 ('0' = 0011 0000)
		final char cCode5 = (char) ('0' + (mWellMask >> 7)); // Add the bit for well 8 to byte 5
		final String sCode = "0108" + cCode4 + cCode5;
		
		final String s = String.format(sFunc+"("+
				"%d,\"%s\","+
				"%f,%f,%f,%f,%f,%f,%f,%f,0,0,0,0,"+
				"%d,%d,"+
				"1,"+
				"\"%s\","+
				"0,0);",
				mTipsMask, sLiquidClass,
				anVolumes[0], anVolumes[1], anVolumes[2], anVolumes[3], anVolumes[4], anVolumes[5], anVolumes[6], anVolumes[7],
				iGrid, iSite,
				sCode
				);
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
		System.out.println(s);
	}
	*/
}
