/*package roboliq.robots.evoware


object EvowareTranslatorHeader {
	def getHeader(): String = sDefault
	
	type LabwareMap = Map[Tuple2[Int, Int], LabwareItem]
	
	def getHeader(map: LabwareMap): String = {
		getHeader(sDefault, map)
	}
	
	def getHeader(sOrig: String, map: LabwareMap): String = {
		val lsLine = sOrig.split("\r?\n", -1).toList
		val lsLineNew = processTop(lsLine, map)
		lsLineNew.mkString("\n")
	}

	private def processTop(lsLine: List[String], map: LabwareMap): List[String] = lsLine match {
		case Nil => Nil
		case sLine :: rest =>
			if (sLine.startsWith("999;"))
				sLine :: processTop(rest, map)
			else if (sLine.startsWith("14;"))
				sLine :: process_14(sLine, rest, map)
			else {
				//println("ERROR 1", sLine)
				//Nil
				sLine :: processTop(rest, map)
			}
	}

	private def process_14(sLine14: String, lsLine: List[String], map: LabwareMap): List[String] = {
		val ls14 = sLine14.split(";", -1).tail.init.toList
		process_line(0, ls14, lsLine, map)
	}
	
	private def process_line(
		iGrid: Int,
		ls14: List[String],
		lsLine: List[String],
		map: LabwareMap
	): List[String] = (ls14, lsLine) match {
		case (Nil, _) =>
			lsLine
		case (_, Nil) =>
			println("ERROR 2", ls14)
			Nil
		case ("-1" :: rest14, sLine :: rest) =>
			sLine :: process_line(iGrid + 1, rest14, rest, map)
		case (s14 :: rest14, sLine1 :: sLine2 :: rest) =>
			val fields1A = sLine1.split(";", -1).tail.init
			val sCount = fields1A.head
			val fields1 = fields1A.tail
			val fields2 = sLine2.split(";", -1).tail.init
			if (fields1.size != fields2.size) {
				println("ERROR 3", s14, fields1.toSeq, fields2.toSeq)
				Nil
			}
			else {
				val xs = (fields1 zip fields2).zipWithIndex
				val ys = xs.map(tuple => {
					val ((s1, s2), iSite) = tuple
					val key = (iGrid, iSite)
					map.get(key) match {
						case None => (s1, s2)
						case Some(item) => (item.sType, item.sLabel)
					}
				})
				val (ls1, ls2) = ys.unzip
				ls1.mkString("998;"+sCount+";",";",";") :: ls2.mkString("998;",";",";") :: process_line(iGrid + 1, rest14, rest, map)
			}
	}
}
*/