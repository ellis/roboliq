package evoware

import java.io.File
import java.io.BufferedWriter
import java.io.FileWriter
import roboliq.parts._
import roboliq.tokens._
import roboliq.robot._


object EvowareTranslator {
	def translate(cmds: Seq[T1_Token], robot: EvowareRobot): String = {
		val tr = new EvowareTranslator(robot)
		tr.translate(cmds)
	}
	
	def translateAndSave(cmds: Seq[T1_Token], robot: EvowareRobot, sFilename: String): String = {
		val tr = new EvowareTranslator(robot)
		val s = tr.translate(cmds)
		//val file = new File(sFilename)
		//val output = new BufferedWriter(new FileWriter(file));
		val fos = new java.io.FileOutputStream(sFilename)
		/*output.write(getHeader())
		output.newLine()
		output.write(s)
		output.newLine()*/
		//fos.write(Array('\220'.asInstanceOf[Byte]), 0, 1)
		//fos.write("\220".map(_.asInstanceOf[Byte]).toArray)
		writeLines(fos, getHeader())
		writeLines(fos, s);
		fos.close();
		s
	}
	
	def writeLines(output: java.io.FileOutputStream, s: String) {
		val as = s.split("\r?\n")
		for (sLine <- as if !s.isEmpty) {
			val bytes = sLine.map(_.asInstanceOf[Byte]).toArray
			output.write(bytes)
			output.write("\r\n".getBytes())
		}
	}
	
	private def encode(n: Int): Char = ('0' + n).asInstanceOf[Char]
	private def hex(n: Int): Char = Integer.toString(n, 16).toUpperCase.apply(0)
	
	def encodeWells(holder: WellHolder, aiWells: Traversable[Int]): String = {
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
	
	def getHeader(): String =
"""302F0FE9
20110530_155010 No log in       
                                                                                                                                
No user logged in                                                                                                               
--{ RES }--
V;200
--{ CFG }--
999;219;32;
14;-1;239;240;130;241;-1;-1;52;-1;242;249;-1;-1;-1;-1;-1;250;243;-1;-1;-1;-1;-1;-1;244;-1;-1;-1;-1;-1;-1;-1;-1;35;-1;-1;-1;-1;-1;-1;234;-1;-1;-1;-1;-1;-1;235;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;246;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;
998;0;
998;3;Wash Station Cleaner shallow;Wash Station Waste;Wash Station Cleaner deep;
998;;;;
998;3;Wash Station Cleaner shallow;Wash Station Waste;Wash Station Cleaner deep;
998;;;;
998;3;Trough 100ml;Trough 100ml;Trough 100ml;
998;Labware7;Labware8;Decon;
998;2;Reagent Cooled 8*15ml;Reagent Cooled 8*50ml;
998;Labware5;Labware6;
998;0;
998;0;
998;1;Trough 1000ml;
998;Labware10;
998;0;
998;1;;
998;;
998;2;;;
998;;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;1;;
998;;
998;2;D-BSSE 96 Well PCR Plate;D-BSSE 96 Well PCR Plate;
998;Labware14;Labware15;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;3;;;;
998;;;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;9;;;;;;;MTP Waste;;;
998;;;;;;;Waste;;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;1;D-BSSE 96 Well PCR Plate;
998;Labware12;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;1;D-BSSE 96 Well PCR Plate;
998;Labware13;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;1;;
998;;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;0;
998;5;
998;245;11;
998;93;55;
998;252;54;
998;251;59;
998;253;64;
998;6;
998;4;0;System;
998;0;0;Shelf 32Pos Microplate;
998;0;4;Hotel 5Pos SPE;
998;0;1;Hotel 3Pos DWP;
998;0;2;Hotel 4Pos DWP 1;
998;0;3;Hotel 4Pos DWP 2;
998;0;
998;1;
998;11;
998;55;
998;54;
998;59;
998;64;
996;0;0;
--{ RPG }--"""
}

private class EvowareTranslator(robot: EvowareRobot) {
	//settings: EvowareSettings, state: IRobotState
	val state = robot.state
	
	def translate(cmds: Seq[T1_Token]): String = {
		cmds.map(evowareCmd).flatten.mkString("\n")
	}
	
	private def evowareCmd(tok: T1_Token): List[String] = tok match {
		case t @ T1_Aspirate(_) => aspirate(t)
		case t @ T1_Dispense(_) => dispense(t)
		case t @ T1_Clean(specs) => clean(specs)
	}

	private def aspirate(tok: T1_Aspirate): List[String] = {
		tok.twvs.map(twv => (twv -> robot.getAspirateClass(twv.tip, twv.well))).toMap
		spirates("Aspirate", tok.twvs);
	}
	
	private def dispense(tok: T1_Dispense): List[String] = {
		spirates("Dispense", tok.twvs);
	}
	
	private def spirates(sFunc: String, twvs: Seq[TipWellVolume]): List[String] = {
		if (twvs.isEmpty)
			Nil
		else {
			val holder = twvs.head.well.holder
			assert(twvs.forall(_.well.holder == holder))
			val twvsSorted = twvs.sortWith(_.tip.index < _.tip.index).toList
			val twvssByType = partitionTipKinds(twvsSorted)
			val twvss = twvssByType.flatMap(partitiontwvsByType)
			twvss.flatMap(twvs => spirate(sFunc, twvs))
		}
	}
	
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
	}
	
	private def spirate(sFunc: String, twvs: Seq[TipWellVolume]): List[String] = {
		if (twvs.isEmpty)
			return Nil

		val twv0 = twvs.head 
		val tipKind = robot.getTipKind(twv0.tip)
		val holder = twv0.well.holder
		
		// Indexes of tips we want to use
		val aiTips = twvs.map(_.tip.index)
		// Create mask for all tips being used
		val mTips = aiTips.foldLeft(0) { (sum, i) => sum + (1 << i) }
		
		val sLiquidClass = robot.getLiquidClass(twv0)
		
		// Create a list of volumes for each used tip, leaving the remaining values at 0
		val asVolumes = Array.fill(12)("0")
		val fmt = new java.text.DecimalFormat("#.##")
		for (twv <- twvs) {
			val iTip = twv.tip.index
			assert(iTip >= 0 && iTip < 12)
			asVolumes(iTip) = "\""+fmt.format(twv.nVolume)+'"'
		}
		val sVolumes = asVolumes.mkString(",")
		
		val sPlateMask = EvowareTranslator.encodeWells(holder, twvs.map(_.well.index))
		
		// Find a parent of 'holder' which has an Evoware location (x-grid/y-site)
		def findLoc(part: Part): Option[Tuple2[Int, Int]] = {
			val site_? = state.getSite(part)
			site_? match {
				case None => None
				case Some(site) =>
					settings.grids.get(site.parent) match {
						case Some(iGrid) => Some(iGrid, site.index)
						case None => findLoc(site.parent)
					}
			}
		}
		val (iGrid, iSite) = findLoc(holder).get
		
		(
			sFunc+"("+
			"%d,\"%s\","+
			"%s,"+
			"%d,%d,"+
			"1,"+
			"\"%s\","+
			"0,0);"
		).format(
			mTips, sLiquidClass,
			sVolumes,
			iGrid, iSite,
			sPlateMask
		) :: Nil;
	}
	
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

	private def clean(specs0: Traversable[TipCleanSpec]): List[String] = {
		def sameTipKind(a: TipCleanSpec, b: TipCleanSpec): Boolean = {
			val ka = robot.getTipKind(a.tip)
			val kb = robot.getTipKind(b.tip)
			ka == kb
		}
		val specss = partitionBy(specs0.toList, sameTipKind)
		for (specs <- specss) yield {
			// Indexes of tips we want to use
			val aiTips = specs.map(_.tip.index)
			// Create mask for all tips being used
			val mTips = aiTips.foldLeft(0) { (sum, i) => sum + (1 << i) }
			
			Array(
				mTips,
				iWasteGrid, iWasteSite,
				iCleanerGrid, iCleanerSite,
				'"'+sWasteVolume+'"',
				nWasteDelay,
				'"'+sCleanerVolume+'"',
				nCleanerDelay,
				nAirgapVolume,
				nAirgapSpeed,
				nRetractSpeed,
				(if (bFastWash) 1 else 0),
				0,1000,0).mkString("Wash(", ",", ")")
		}.toList
	}
}
