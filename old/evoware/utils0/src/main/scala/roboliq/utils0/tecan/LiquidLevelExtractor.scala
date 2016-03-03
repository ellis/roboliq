package roboliq.utils0.tecan

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.HashMap

object LiquidLevelExtractor {
	val StartingProgram = """Starting program "([^"]+)" \(lines .*""".r
	val Line = """Line +([^:]+) +: (.*)""".r
	val DetectTip1 = """tip ([0-9]+) : detect +([0-9]+), *([0-9]+) (.+) \[(.*)\]""".r
	val DetectTip2 = "\"(.+)\"".r
	val DispenseTip1 = """tip ([0-9]+) : dispense [0-9.]+.l +([0-9]+), *([0-9]+) (.+) \[(.*)\]""".r
	val DispenseTip2 = """([0-9.]+).l "([^"]+)".+""".r
	val DetectedVolume = """detected_volume_(.) = ([-0-9]+)""".r
	
	object Expect extends Enumeration {
		val None, C5, Dispense2, DetectTip2 = Value
	}
	
	case class WellId(loc: String, row: Int, col: Int)
	
	case class WellData(
		id: WellId,
		step: Int = 0,
		volPrev: BigDecimal = 0,
		vol: BigDecimal = 0,
		z: Int = 0
	)

	case class Data(
		tip: Int,
		row: Int,
		col: Int,
		loc: String,
		dvol: BigDecimal,
		vol: BigDecimal,
		z: Integer,
		dz: Integer
	)
	
	case class DetectHeader(
		tip: Int,
		row: Int,
		col: Int,
		labwareModel: String,
		loc: String,
		liquidClass: String,
		wellVol: BigDecimal
	)
	
	case class DispenseHeader(
		tip: Int,
		row: Int,
		col: Int,
		labwareModel: String,
		loc: String,
		vol: BigDecimal,
		liquidClass: String
	)
	
	/*
	// Want to output csv lines: tip,loc,row,col,z
	def process(filename: String) {
		var bDetect = false
		val tw_m = new LinkedHashMap[Int, WellId]
		val wellData_m = new HashMap[WellId, WellData]
		val wellZPrev_m = new HashMap[WellId, Int]
		var sC5 = ""
		var sCmd = ""
		var expect = Expect.None
		var wellIdTemp_? : Option[WellId] = None
		
		def getWellData(wellId: WellId): WellData = {
			wellData_m.get(wellId) match {
				case Some(wd) => wd
				case None =>
					val wd = WellData(wellId)
					wellData_m(wellId) = wd
					wd
			}
		}
		
		def dispense(wellId: WellId, vol: BigDecimal) {
			val wd0 = getWellData(wellId)
			val wd = wd0.copy(step = wd0.step + 1, volPrev = wd0.vol, vol = wd0.vol + vol)
			wellData_m += wellId -> wd
		}
		
		def handleDetectLineForTip(tip: Int, row: Int, col: Int, loc: String) {
			val wellId = WellId(loc, row, col)
			tw_m += tip -> wellId
		}
		
		def printLine(tip: Int, wellId: WellId) {
			val wd = getWellData(wellId)
			val dvol = wd.vol - wd.volPrev
			val dz = wd.z - wellZPrev_m.getOrElse(wellId, 0)
			println(List(
					tip, wellId.loc, wellId.row, wellId.col, wd.step, wd.z, wd.vol, dz, dvol
				).mkString("\t"))
			wellZPrev_m += wellId -> wd.z
		}

		def setZ(wellId: WellId, z: Int) {
			val wd0 = getWellData(wellId)
			val wd = wd0.copy(z = z)
			wellData_m += wellId -> wd
		}
		
		def handleTipLevel(tip: Int, z: Int) {
			tw_m.get(tip) match {
				case Some(tw) =>
					val wellId = WellId(tw.loc, tw.row, tw.col)
					setZ(wellId, z)
					printLine(tip, wellId)
				case _ =>
					// We're not interested in this tip
			}
		}
		
		println(List("tip", "loc", "row", "col", "step", "z", "vol", "dz", "dvol").mkString("\t"))
		for (line0 <- io.Source.fromFile(filename).getLines) {
			// drop first 14 chars
			val line = line0.drop(14).trim
			/*line match {
				case Detect(well, loc) =>
					println("hmm", well, loc)
				case _ =>
					if (line.contains(" detect "))
						println("baa", line)
					else
						println(line)
			}*/
			line match {
				case Line(cmd) =>
					sCmd = cmd;
					
					if (bDetect) {
						tw_m.foreach(pair => {
							val (tip, wellId) = pair
							printLine(tip, wellId)
							//wellZPrev_m += wellId -> wd.z
						})
					}
					
					bDetect = (sCmd == "Detect Liquid")
					tw_m.clear
					
				case Dispense1(_, col_s, row_s, loc) =>
					println("Dispense1")
					val (row, col) = (row_s.toInt, col_s.toInt)
					wellIdTemp_? = Some(WellId(loc, row, col))
					expect = Expect.Dispense2
				
				case Dispense2(vol_s) if expect == Expect.Dispense2 =>
					println("Dispense2")
					val vol = BigDecimal(vol_s)
					dispense(wellIdTemp_?.get, vol)
					wellIdTemp_? = None
					expect = Expect.None
					
				case Detect(tip_s, col_s, row_s, loc) =>
					val (tip, row, col) = (tip_s.toInt, row_s.toInt, col_s.toInt)
					handleDetectLineForTip(tip, row, col, loc)
					println(List(tip, row, col, loc).mkString("\t"))
				
				case "> C5,RPZ0" if bDetect =>
					expect = Expect.C5
					
				case _ if expect == Expect.C5 =>
					// NOTE: this check is here because multiple commands may be sent at once without first waiting for a response
					// from the robot.  This makes sure that we only try to process a response message.
					if (line.startsWith("-")) {
						val l = line.split(",").drop(2).toList
						for ((z, tip_i) <- l.zipWithIndex) {
							val tip = tip_i + 1
							println("z: ", line, l, tip_i, z)
							handleTipLevel(tip, z.toInt)
						}
						expect = Expect.None
					}
				
				case _ =>
					expect = Expect.None
			}
		}
	}
	*/
	
	// Want to output csv lines: tip,loc,row,col,z
	def process2(filename: String) {
		var bDetect = false
		//val tw_m = new LinkedHashMap[Int, WellId]
		//val wellData_m = new HashMap[WellId, WellData]
		//val wellZPrev_m = new HashMap[WellId, Int]
		val tipToDetectHeader_m = new LinkedHashMap[Int, DetectHeader]
		val tipToDispenseHeader_m = new LinkedHashMap[Int, DispenseHeader]
		val tipToZ_m = new LinkedHashMap[Int, Int]
		val wellToVolume_m = new LinkedHashMap[WellId, BigDecimal]
		var tipCached = 0
		var sProgram = ""
		var iLine = 0
		var sC5 = ""
		var sCmd = ""
		var expect = Expect.None
		
		println(List("tip", "loc", "row", "col", "z", "vol").mkString("\t"))
		for (line0 <- io.Source.fromFile(filename).getLines) {
			// drop first 14 chars
			val line = line0.drop(14).trim
			line match {
				case StartingProgram(filename) =>
					sProgram = filename
					
				case Line(lineNums, cmd) =>
					if (bDetect) {
						tipToDetectHeader_m.foreach { case (tip, data) =>
							println(List(
									tip, data.loc, data.row, data.col, tipToZ_m(tip), data.wellVol
								).mkString("\t"))
						}
						/*tipToDispenseHeader_m.foreach { case (tip, data) =>
							println(List(
									tip, data.loc, data.row, data.col, tipToZ_m(tip), -1, -1, -1
								).mkString("\t"))
						}*/
					}
					
					tipToDetectHeader_m.clear()
					tipToDispenseHeader_m.clear()
					tipToZ_m.clear()
					tipCached = 0
					bDetect = false

					println("line: "+line)
					val lineNum_l = lineNums.split("/")
					val lineNum = lineNum_l.last
					iLine = lineNum.trim.toInt
					sCmd = cmd
					
				case DetectTip1(tip_s, col_s, row_s, labwareModel, loc) =>
					val (tip, row, col) = (tip_s.toInt, row_s.toInt, col_s.toInt)
					val wellId = WellId(loc, row, col)
					tipToDetectHeader_m(tip) = DetectHeader(tip, row, col, labwareModel, loc, "", wellToVolume_m.getOrElse(wellId, BigDecimal(0)))
					//println(List(tip, row, col, loc).mkString("\t"))
					tipCached = tip
					expect = Expect.DetectTip2
				
				case DetectTip2(liquidClass) if expect == Expect.DetectTip2 =>
					tipToDetectHeader_m(tipCached) = tipToDetectHeader_m(tipCached).copy(liquidClass = liquidClass)
					expect = Expect.None
					bDetect = true
				
				case DispenseTip1(tip_s, col_s, row_s, labwareModel, loc) =>
					println("Dispense1")
					val (tip, row, col) = (tip_s.toInt, row_s.toInt, col_s.toInt)
					tipToDispenseHeader_m(tip) = DispenseHeader(tip, row, col, labwareModel, loc, 0, "")
					tipCached = tip
					expect = Expect.Dispense2
				
				case DispenseTip2(vol_s, liquidClass) if expect == Expect.Dispense2 =>
					println("Dispense2")
					val header = tipToDispenseHeader_m(tipCached)
					val dvol = BigDecimal(vol_s)
					val wellId = WellId(header.loc, header.row, header.col)
					val vol = wellToVolume_m.getOrElse(wellId, BigDecimal(0)) + dvol
					wellToVolume_m(wellId) = vol
					tipToDispenseHeader_m(tipCached) = tipToDispenseHeader_m(tipCached).copy(vol = vol, liquidClass = liquidClass)
					tipToDetectHeader_m(tipCached) = DetectHeader(header.tip, header.row, header.col, header.labwareModel, header.loc, liquidClass, vol)
					expect = Expect.None
					bDetect = true
					
				case "> C5,RPZ0" if bDetect =>
					expect = Expect.C5
					
				case _ if expect == Expect.C5 =>
					// NOTE: this check is here because multiple commands may be sent at once without first waiting for a response
					// from the robot.  This makes sure that we only try to process a response message.
					if (line.startsWith("- C5,0,")) {
						val l = line.split(",").drop(2).toList
						for ((z, tip_i) <- l.zipWithIndex) {
							val tip = tip_i + 1
							//println("z: ", line, l, tip_i, z)
							tipToZ_m(tip) = z.toInt
						}
						expect = Expect.None
					}
				
				case _ =>
					expect = Expect.None
			}
		}
	}
	
	// Want to output csv lines: tip,loc,row,col,z
	def process3(filename: String) {
		
		// HACK: extract the original offset values from the log file instead of hardcoding them!
		val tipToOrigin_m = Map(1 -> 1448, 2 -> 1450, 3 -> 1448, 4 -> 1451)

		var bDetect = false
		//val tw_m = new LinkedHashMap[Int, WellId]
		//val wellData_m = new HashMap[WellId, WellData]
		//val wellZPrev_m = new HashMap[WellId, Int]
		val tipToDetectHeader_m = new LinkedHashMap[Int, DetectHeader]
		val tipToDispenseHeader_m = new LinkedHashMap[Int, DispenseHeader]
		val tipToZDetect_m = new LinkedHashMap[Int, Int]
		val tipToZDispense_m = new LinkedHashMap[Int, Int]
		var tipToZ_m: LinkedHashMap[Int, Int] = tipToZDetect_m
		val wellToVolume_m = new LinkedHashMap[WellId, BigDecimal]
		var tipCached = 0
		var sProgram = ""
		var iLine = 0
		var sC5 = ""
		var sCmd = ""
		var expect = Expect.None
		
		println(List("tip", "loc", "row", "col", "vol", "zAbs", "z").mkString("\t"))
		for (line0 <- io.Source.fromFile(filename, "iso-8859-1").getLines) {
			// drop first 14 chars
			val line = line0.drop(14).trim
			//println("0: "+line)
			line match {
				case StartingProgram(filename) =>
					sProgram = filename
					
				case Line(lineNums, cmd) =>
					if (bDetect) {
						tipToDetectHeader_m.foreach { case (tip, data) =>
							/*println(List(
									tip, data.loc, data.row, data.col, tipToZ_m(tip), data.wellVol
								).mkString("\t"))*/
						}
						/*tipToDispenseHeader_m.foreach { case (tip, data) =>
							println(List(
									tip, data.loc, data.row, data.col, tipToZ_m(tip), -1, -1, -1
								).mkString("\t"))
						}*/
					}
					
					tipToDetectHeader_m.clear()
					tipToDispenseHeader_m.clear()
					tipToZDetect_m.clear()
					tipToZDispense_m.clear()
					tipCached = 0
					bDetect = false

					//println("line: "+line)
					val lineNum_l = lineNums.split("/")
					val lineNum = lineNum_l.last
					iLine = lineNum.trim.toInt
					sCmd = cmd
					
				case DetectTip1(tip_s, col_s, row_s, labwareModel, loc) =>
					//println("1: "+line)
					val (tip, row, col) = (tip_s.toInt, row_s.toInt, col_s.toInt)
					val wellId = WellId(loc, row, col)
					tipToDetectHeader_m(tip) = DetectHeader(tip, row, col, labwareModel, loc, "", wellToVolume_m.getOrElse(wellId, BigDecimal(0)))
					//println(List(tip, row, col, loc).mkString("\t"))
					tipCached = tip
					expect = Expect.DetectTip2
				
				case DetectTip2(liquidClass) if expect == Expect.DetectTip2 =>
					//println("2: "+line)
					tipToDetectHeader_m(tipCached) = tipToDetectHeader_m(tipCached).copy(liquidClass = liquidClass)
					expect = Expect.None
					bDetect = true
				
				case DispenseTip1(tip_s, col_s, row_s, labwareModel, loc) =>
					//println("Dispense1")
					val (tip, row, col) = (tip_s.toInt, row_s.toInt, col_s.toInt)
					tipToDispenseHeader_m(tip) = DispenseHeader(tip, row, col, labwareModel, loc, 0, "")
					tipCached = tip
					expect = Expect.Dispense2
				
				case DispenseTip2(vol_s, liquidClass) if expect == Expect.Dispense2 =>
					//println("Dispense2")
					val header = tipToDispenseHeader_m(tipCached)
					val dvol = BigDecimal(vol_s)
					val wellId = WellId(header.loc, header.row, header.col)
					val vol = wellToVolume_m.getOrElse(wellId, BigDecimal(0)) + dvol
					wellToVolume_m(wellId) = vol
					tipToDispenseHeader_m(tipCached) = tipToDispenseHeader_m(tipCached).copy(vol = vol, liquidClass = liquidClass)
					tipToDetectHeader_m(tipCached) = DetectHeader(header.tip, header.row, header.col, header.labwareModel, header.loc, liquidClass, vol)
					expect = Expect.None
					bDetect = true
				
				case DetectedVolume(tip_s, vol_s) =>
					//println("3: "+line)
					val tip = tip_s.toInt
					val vol = vol_s.toInt
					if (tipToDetectHeader_m.contains(tip)) {
						val header = tipToDetectHeader_m(tip)
						val z = (tipToZDetect_m.get(tip) orElse tipToZDispense_m.get(tip).map(_ + 10)).getOrElse(1000)
						//println(header, vol)
						println(List(
								tip, header.loc, header.row, header.col, vol, z, z - tipToOrigin_m(tip)
							).mkString("\t"))
					}
					
				case "> C5,RPZ0" if bDetect =>
					tipToZ_m = tipToZDispense_m
					expect = Expect.C5
					
				case "> C5,RVZ1" if bDetect =>
					tipToZ_m = tipToZDetect_m
					expect = Expect.C5
					
				case _ if expect == Expect.C5 =>
					// NOTE: this check is here because multiple commands may be sent at once without first waiting for a response
					// from the robot.  This makes sure that we only try to process a response message.
					if (line.startsWith("- C5,0,")) {
						val l = line.split(",").drop(2).toList
						for ((z, tip_i) <- l.zipWithIndex) {
							val tip = tip_i + 1
							//println("z: ", line, l, tip_i, z)
							tipToZ_m(tip) = z.toInt
						}
						expect = Expect.None
					}
				
				case _ =>
					expect = Expect.None
			}
		}
	}

	def main(args: Array[String]) {
		//process3("""C:\ProgramData\Tecan\EVOware\AuditTrail\log\EVO_20150112_131614.LOG""")
		//process3("""C:\Users\localadmin\Desktop\Ellis\bsse-lab\20150116--chao01_dye\EVO_20150116_142604--zlevel_bottom.log""")
		//process3("""C:\Users\localadmin\Desktop\Ellis\bsse-lab\20150116--chao01_dye\EVO_20150116_141247--zlevel_top.log""")
		//process3("""C:\Users\localadmin\Desktop\Ellis\bsse-lab\20150120--chao04_2nd_dispense\EVO_20150120_121617--25_to_60ul.log""")
		//process3("""C:\Users\localadmin\Desktop\Ellis\bsse-lab\20150120--chao04_2nd_dispense\EVO_20150120_125817--zlevel_top.log""")
		//process3("""C:\Users\localadmin\Desktop\Ellis\bsse-lab\20150127--chao04_2nd_dispense\EVO_20150127_111922--layer2.log""")
		//process3("""C:\Users\localadmin\Desktop\Ellis\bsse-lab\20150127--chao04_2nd_dispense\EVO_20150127_120611.LOG""")
		//process3("""/home/ellisw/repo/bsse-lab/20150129--chao07_tripple/EVO_20150129_145522--A_zlevels.log""")
		//process3("""/home/ellisw/repo/bsse-lab/20150129--chao07_tripple/EVO_20150129_150735--B.log""")
		//process3("""/home/ellisw/repo/bsse-lab/20150129--chao07_tripple/EVO_20150129_153959--C.log""")
		//process3("""/home/ellisw/repo/bsse-lab/20150129--chao07_tripple/EVO_20150129_161227--C_zlevels.log""")
		//process3("""/home/ellisw/repo/bsse-lab/20150127--chao04_2nd_dispense/EVO_20150127_111922--layer2.log""")
		process3("""/home/ellisw/repo/bsse-lab/20150127--chao04_2nd_dispense/EVO_20150127_120611--zlevels.log""")
		//process(args(0))
	}
}