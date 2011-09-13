package roboliq.robots.evoware


object EvowareTranslatorHeader {
	private val sDefault =
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
