package roboliq.utils0.tecan

object LiquidLevelExtractor {
	val Line = """Line .... : (.*)""".r
	val Detect = """tip .+ : detect +([0-9]+, [0-9]+) .+ \[(.*)\]""".r
	
	def main(args: Array[String]) {
		var bC5 = false
		var sC5 = ""
		var sCmd = ""
		for (line0 <- io.Source.fromFile(args(0)).getLines) {
			// drop first 14 chars
			val line = line0.drop(14).trim
			if (line == "> C5,RPZ0") {
				bC5 = true
			}
			else if (bC5) {
				sC5 = line
				bC5 = false
			}
			else {
				line match {
					case Line(cmd) => sCmd = cmd; //println(sCmd)
					case Detect(well, loc) =>
						println(well, loc)
						println(sC5)
					case _ =>
				}
			}
		}
	}

}