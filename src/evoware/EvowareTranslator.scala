package evoware

import concrete._


object EvowareTranslator {
	def translate(cmds: List[Token], settings: EvowareSettings): String = {
		val tr = new EvowareTranslator(settings)
		tr.translate(cmds)
	}
}

private class EvowareTranslator(settings: EvowareSettings) {
	def translate(cmds: List[Token]): String = {
		cmds.map(evowareCmd).flatten.mkString("\n")
	}
	
	private def evowareCmd(cmd: Token): List[String] = cmd match {
		case c @ Aspirate(_, _, _, _, _) => aspirate(c) :: Nil
		case c @ Dispense(_, _, _, _, _) => dispense(c) :: Nil
		case _ => Nil
	}

	private def aspirate(cmd: Aspirate): String = {
		spirate("Aspirate", cmd.volumes, cmd.plate, cmd.loc, cmd.wells, cmd.rule);
	}
	
	private def dispense(cmd: Dispense): String = {
		spirate("Dispense", cmd.volumes, cmd.plate, cmd.loc, cmd.wells, cmd.rule);
	}
	
	private def spirate(sFunc: String, volumes: Array[Double], plate: Plate, iLoc: Int, wells: Set[Int], rule: PipettingRule): String = {
		assert(volumes.size <= 12)
		
		// Indexes of tips we want to use
		val tips = volumes.zipWithIndex.filter(_._1 > 0).map(_._2)
		// Create mask for all tips being used
		val mTips = tips.foldLeft(0) { (sum, i) => sum + (1 << i) }
		
		// Create a list of volumes for each used tip, leaving the remaining values at 0
		val anVolumes0 = volumes.filter(_ > 0).toArray
		val anVolumes = new Array[Double](12)
		Array.copy(anVolumes0, 0, anVolumes, 0, anVolumes0.size)
		val formatVolume = new java.text.DecimalFormat("#.##")
		val sVolumes = anVolumes.map(formatVolume.format).mkString(",")
		
		val nWellMaskChars = math.ceil(plate.rows * plate.cols / 7.0).asInstanceOf[Int]
		val amWells = new Array[Int](nWellMaskChars)
		//val acWellMask = ("0" * nWellMaskChars).toArray
		for (iWell <- wells) {
			val iChar = iWell / 7;
			val iWell1 = iWell % 7;
			amWells(iChar) += 1 << iWell1
		}
		//acWellMask.mkString
		val sWellMask = amWells.map(encode).mkString
		val sPlateMask = Array('0', hex(plate.cols), '0', hex(plate.rows)).mkString + sWellMask
		
		val loc = settings.locations(iLoc)
		
		(
			sFunc+"("+
			"%d,\"%s\","+
			"%s,"+
			"%d,%d,"+
			"1,"+
			"\"%s\","+
			"0,0);"
		).format(
			mTips, rule.name,
			sVolumes,
			loc.iGrid, loc.iSite,
			sPlateMask
		);
	}
	
	private def hex(n: Int): Char = Integer.toString(n, 16).toUpperCase.apply(0)
	private def encode(n: Int): Char = ('0' + n).asInstanceOf[Char]
	
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