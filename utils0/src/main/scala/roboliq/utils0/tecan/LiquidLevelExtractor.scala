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
				if (sCmd == "Detect Liquid")
					println(sC5)
			}
			else {
				line match {
					case Line(cmd) => sCmd = cmd; //println(sCmd)
					case Detect(well, loc) =>
						println(well, loc)
					case _ =>
				}
			}
		}
	}

}