sealed abstract class Section
case class SectionNone() extends Section
case class Carrier(
	val sName: String,
	val id: Int,
	val nSites: Int
) extends Section

object EvowareFormat {
	def splitSemicolons(sLine: String): Tuple2[Int, List[String]] = {
		val l = sLine.split(";", -1).init.toList
		val sLineKind = l.head
		val nLineKind = sLineKind.toInt
		(nLineKind, l.tail)
	}
}

object CarrierParser {
	import EvowareFormat._
	
	def loadCarrierConfig(): List[Section] = {
		val lsLine = scala.io.Source.fromFile("/home/ellisw/tmp/tecan/carrier.cfg", "ISO-8859-1").getLines.toList
		//val lsLine = sInput.split("\r?\n", -1).toList
		def x2(lsLine: List[String], acc: List[Section]): List[Section] = {
			if (lsLine.isEmpty)
				return acc
			section(lsLine) match {
				case (SectionNone(), lsLine2) => x2(lsLine2, acc)
				case (sec, lsLine2) =>
					//println(sec)
					x2(lsLine2, sec::acc)
			}
		}
		x2(lsLine.drop(4), Nil)
		//val ls14 = sLine14.split(";", -1).tail.init.toList
	}
	
	def section(lsLine: List[String]): Tuple2[Section, List[String]] = {
		val sLine0 = lsLine.head
		val (nLineKind, l) = splitSemicolons(sLine0)
		nLineKind match {
			case 13 =>
				val sName = l.head
				val sId = l(1).split("/").head
				val id = sId.toInt
				val nSites = l(4).toInt
				(Carrier(sName, id, nSites), lsLine.drop(8))
			case _ => (SectionNone(), lsLine.tail)
		}
	}
}

object TableParser {
	import EvowareFormat._

	def parseFile(sections: List[Section], sFilename: String) {
		val lsLine = scala.io.Source.fromFile(sFilename, "ISO-8859-1").getLines.toList.drop(7)
		//println(lsLine.takeWhile(_ != "--{ RPG }--").length)
		val (_, l) = EvowareFormat.splitSemicolons(lsLine(1))
		val rest = parse14(sections, l, lsLine.drop(2))
		println(rest.takeWhile(_ != "--{ RPG }--"))
	}

	def parse14(sections: List[Section], l: List[String], lsLine: List[String]): List[String] = {
		val lCarrier_? = parse14_header(sections, l)
		def step(iGrid: Int, lCarrier_? : List[Option[Carrier]], lsLine: List[String]): List[String] = lCarrier_? match {
			case Nil => lsLine
			case None :: rest => step(iGrid + 1, rest, lsLine.tail)
			case Some(carrier) :: rest =>
				val (n0, l0) = EvowareFormat.splitSemicolons(lsLine(0))
				val (n1, l1) = EvowareFormat.splitSemicolons(lsLine(1))
				assert(n0 == 998 && n1 == 998 && l0(0).toInt == carrier.nSites)
				println(iGrid+": "+carrier)
				for (i <- 0 until carrier.nSites) {
					println("\t"+i+": "+l0(i+1)+", "+l1(i))
				}
				step(iGrid + 1, rest, lsLine.drop(2))
		}
		step(0, lCarrier_?, lsLine)
	}
	
	def parse14_header(sections: List[Section], l: List[String]): List[Option[Carrier]] = {
		val map = sections.collect({case c: Carrier => c.id -> c}).toMap
		l.map(s => {
			val id = s.toInt
			if (id == -1) None
			else map.get(id)
		})
		/*
		for ((item, iGrid) <- l.zipWithIndex) {
			if (item != "-1") {
				val id = item.toInt
				println(iGrid + ": " + map(id))
			}
		}*/
	}
}

object T {
	def test() {
		val sections = CarrierParser.loadCarrierConfig()
		val (_, l) = EvowareFormat.splitSemicolons("14;-1;239;240;130;241;-1;-1;52;-1;242;249;-1;-1;-1;-1;-1;250;243;-1;-1;-1;-1;-1;-1;244;-1;-1;-1;-1;-1;-1;-1;-1;35;-1;-1;-1;-1;-1;-1;234;-1;-1;-1;-1;-1;-1;235;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;246;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;-1;")
		//TableParser.parse14_header(sections, l)
		TableParser.parseFile(sections, "/home/ellisw/src/roboliq/ellis_pcr1_corrected.esc")
	}
}
