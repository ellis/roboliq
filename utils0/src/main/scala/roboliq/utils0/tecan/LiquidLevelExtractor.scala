package roboliq.utils0.tecan

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.LinkedHashMap
import scala.collection.mutable.HashMap

object LiquidLevelExtractor {
	val Line = """Line .... : (.*)""".r
	val Detect = """tip ([0-9]+) : detect +([0-9]+), ([0-9]+) .+ \[(.*)\]""".r
	val Dispense1 = """tip ([0-9]+) : dispense [0-9.]+.l +([0-9]+), ([0-9]+) .+ \[(.*)\]""".r
	val Dispense2 = """([0-9.]+).l.+""".r
	
	object Expect extends Enumeration {
		val None, C5, Dispense2 = Value
	}
	
	case class WellId(loc: String, row: Int, col: Int)
	
	case class WellData(
		id: WellId,
		volPrev: BigDecimal,
		zPrev: Integer,
		vol: BigDecimal,
		z: Integer
	)
	/*case class TipWell(
		tip: Int,
		wellId: WellId
	)*/
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
	
	// Dispense list
	val d = List(
		10,11,13,14,
		16,18,20,23,
		26,29,32,37,
		41,46,52,58,
		66,74,83,94,
		110,120,130,150
	)
	// Dispense volumes list
	val disp0_l =
		(1 to 96).toList.map(_ => 50) ++
		(0 to 4).toList.flatMap(i => {
			d.slice(i * 4, i * 4 + 4) ++
			d.slice(i * 4, i * 4 + 4) ++
			d.slice(i * 4, i * 4 + 4) ++
			d.slice(i * 4, i * 4 + 4) ++
			d.slice(i * 4, i * 4 + 4) ++
			d.slice(i * 4, i * 4 + 4) ++
			d.slice(i * 4, i * 4 + 4) ++
			d.slice(i * 4, i * 4 + 4)
		}) ++
		(5 to 5).toList.flatMap(i => {
			d.slice(i * 4, i * 4 + 4) ++
			d.slice(i * 4, i * 4 + 4) ++
			d.slice(i * 4, i * 4 + 4) ++
			d.slice(i * 4, i * 4 + 4)
		})
	
	// Want to output csv lines: tip,loc,row,col,z
	def main(args: Array[String]) {
		//println("vol0_l: " + vol0_l)
		var bDetect = false
		val tw_m = new LinkedHashMap[Int, WellId]
		//val tipToData_m = new LinkedHashMap[Int, Data]
		//val wellToData_m = new HashMap[(Int, Int), Data]
		val wellData_m = new HashMap[WellId, WellData]
		var disp_l = disp0_l
		var sC5 = ""
		var sCmd = ""
		var expect = Expect.None
		var wellIdTemp_? : Option[WellId] = None
		
		def dispense(wellId: WellId, vol: BigDecimal) {
			val wd0 = wellData_m.get(wellId) match {
				case Some(wd) => wd
				case None =>
					WellData(
						id = wellId,
						volPrev = 0,
						zPrev = null,
						vol = 0,
						z = null
					)
			}
			val wd = wd0.copy(volPrev = wd0.vol, vol = wd0.vol + vol)
			wellData_m += wellId -> wd
		}
		
		def handleDetectLineForTip(tip: Int, row: Int, col: Int, loc: String) {
			val wellId = WellId(loc, row, col)
			tw_m += tip -> wellId
		}
		
		/*
		def addVol(wellId: (Int, Int), loc: String) {
			val wd0 = wellData_m.get(wellId) match {
				case Some(wd) => wd
				case None =>
					WellData(
						row = wellId._1,
						col = wellId._2,
						loc = loc,
						vol0 = 0,
						z0 = null,
						vol = 0,
						z = null
					)
			}
			val dvol = disp_l.head
			disp_l = disp_l.tail
			val wd = wd0.copy(vol = wd0.vol + dvol)
			wellData_m += wellId -> wd
		}
		*/

		def setZ(wellId: WellId, z: Int) {
			wellData_m.get(wellId) match {
				case Some(wd0) =>
					val wd = wd0.copy(zPrev = wd0.z, z = z)
					wellData_m += wellId -> wd
				case None =>
					println(s"ERROR: missing well data `$wellId`")
			}
		}
		
		def handleTipLevel(tip: Int, z: Int) {
			tw_m.get(tip) match {
				case Some(tw) =>
					val wellId = WellId(tw.loc, tw.row, tw.col)
					setZ(wellId, z)
				case _ =>
					// We're not interested in this tip
			}
		}
		
		println(List("tip", "loc", "row", "col", "z", "vol").mkString("\t"))
		for (line0 <- io.Source.fromFile(args(0)).getLines) {
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
							val wd = wellData_m(wellId)
							println(List(tip, wellId.loc, wellId.row, wellId.col, wd.z, wd.vol).mkString("\t"))
						})
					}
					
					bDetect = (sCmd == "Detect Liquid")
					tw_m.clear
					
				case Dispense1(_, col_s, row_s, loc) =>
					//println("Dispense1")
					val (row, col) = (row_s.toInt, col_s.toInt)
					wellIdTemp_? = Some(WellId(loc, row, col))
					expect = Expect.Dispense2
				
				case Dispense2(vol_s) if expect == Expect.Dispense2 =>
					//println("Dispense2")
					val vol = BigDecimal(vol_s)
					dispense(wellIdTemp_?.get, vol)
					wellIdTemp_? = None
					
				case Detect(tip_s, col_s, row_s, loc) =>
					val (tip, row, col) = (tip_s.toInt, row_s.toInt, col_s.toInt)
					handleDetectLineForTip(tip, row, col, loc)
					//println(List(tip, row, col, loc).mkString("\t"))
				
				case "> C5,RPZ0" if bDetect =>
					expect = Expect.C5
					
				case _ if expect == Expect.C5 =>
					val l = line.split(",").drop(2)
					for ((z, tip_i) <- l.zipWithIndex) {
						val tip = tip_i + 1
						//println("z: ", line, l, tip_i, z)
						handleTipLevel(tip, z.toInt)
					}
					expect = Expect.None
				
				case _ =>
					expect = Expect.None
			}
		}
	}

}