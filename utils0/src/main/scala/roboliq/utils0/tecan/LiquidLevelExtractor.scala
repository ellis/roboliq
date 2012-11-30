package roboliq.utils0.tecan

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

object LiquidLevelExtractor {
	val Line = """Line .... : (.*)""".r
	val Detect = """tip ([0-9]+) : detect +([0-9]+), ([0-9]+) .+ \[(.*)\]""".r
	
	case class Data(tip: String, row: String, col: String, loc: String, z: String)
	
	// Want to output csv lines: tip,loc,row,col,z
	def main(args: Array[String]) {
		var bDetect = false
		var bC5 = false
		val data_l = new ArrayBuffer[Data]
		val tipToData_m = new HashMap[String, Data]
		var sC5 = ""
		var sCmd = ""
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
			if (line == "> C5,RPZ0") {
				bC5 = true
			}
			else if (bC5) {
				bC5 = false
				sC5 = line
				if (bDetect) {
					//println(sC5)
					val l = sC5.split(",").drop(2)
					for ((z, tip_i) <- l.zipWithIndex) {
						val tip = (tip_i + 1).toString
						tipToData_m.get(tip) match {
							case Some(data0) =>
								val data = data0.copy(z = z)
								tipToData_m += tip -> data
								data_l += data
							case _ =>
								// We're not interested in this tip
						}
					}
				}
			}
			else {
				line match {
					case Line(cmd) =>
						sCmd = cmd;
						
						if (bDetect) {
							data_l.foreach(data => {
								println(List(data.tip, data.row, data.col, data.loc, data.z, "0").mkString("\t"))
							})
						}
						
						bDetect = (sCmd == "Detect Liquid")
						data_l.clear
						
					case Detect(tip, col, row, loc) =>
						tipToData_m += tip -> Data(tip, row, col, loc, null)
						//println(List(tip, row, col, loc).mkString("\t"))
						
					case _ =>
				}
			}
		}
	}

}