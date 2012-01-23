sealed abstract class CarrierObject
case class SectionNone() extends CarrierObject
case class GridObject(
	val sName: String,
	val id: Int,
	val nSites: Int
) extends CarrierObject

case class SiteObject(
	parent: GridObject,
	iGrid: Int,
	iSite: Int,
	sName: String,
	sLabel: String
)

case class HotelObject(
	parent: GridObject,
	iGrid: Int
)

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
	
	def loadCarrierConfig(): List[CarrierObject] = {
		val lsLine = scala.io.Source.fromFile("/home/ellisw/tmp/tecan/carrier.cfg", "ISO-8859-1").getLines.toList
		//val lsLine = sInput.split("\r?\n", -1).toList
		def x2(lsLine: List[String], acc: List[CarrierObject]): List[CarrierObject] = {
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
	
	def section(lsLine: List[String]): Tuple2[CarrierObject, List[String]] = {
		val sLine0 = lsLine.head
		val (nLineKind, l) = splitSemicolons(sLine0)
		nLineKind match {
			case 13 =>
				val sName = l.head
				val sId = l(1).split("/").head
				val id = sId.toInt
				val nSites = l(4).toInt
				(GridObject(sName, id, nSites), lsLine.drop(8))
			case _ => (SectionNone(), lsLine.tail)
		}
	}
}

object TableParser {
	import EvowareFormat._

	def parseFile(sections: List[CarrierObject], sFilename: String) {
		val lsLine = scala.io.Source.fromFile(sFilename, "ISO-8859-1").getLines.toList.drop(7)
		//println(lsLine.takeWhile(_ != "--{ RPG }--").length)
		val (_, l) = EvowareFormat.splitSemicolons(lsLine(1))
		val rest = parse14(sections, l, lsLine.drop(2))
		println(rest.takeWhile(_ != "--{ RPG }--"))
	}

	def parse14(sections: List[CarrierObject], l: List[String], lsLine: List[String]): List[String] = {
		val mapIdToGridObject = sections.collect({case c: GridObject => c.id -> c}).toMap
		val lGridObject_? = parse14_getGridObjects(mapIdToGridObject, l)
		val (lSiteObject, lsLine2) = parse14_getSiteObjects(0, lGridObject_?, lsLine, Nil)
		val (lHotelObject, lsLine3) = parse14_getHotelObjects(mapIdToGridObject, lsLine2)
		lSiteObject.foreach(println)
		lHotelObject.foreach(println)
		lsLine3
	}
	
	def parse14_getGridObjects(
		mapIdToGridObject: Map[Int, GridObject],
		l: List[String]
	): List[Option[GridObject]] = {
		l.map(s => {
			val id = s.toInt
			if (id == -1) None
			else mapIdToGridObject.get(id)
		})
		/*
		for ((item, iGrid) <- l.zipWithIndex) {
			if (item != "-1") {
				val id = item.toInt
				println(iGrid + ": " + map(id))
			}
		}*/
	}
	
	def parse14_getSiteObjects(
		iGrid: Int,
		lGridObject_? : List[Option[GridObject]],
		lsLine: List[String],
		acc: List[SiteObject]
	): Tuple2[List[SiteObject], List[String]] = {
		lGridObject_? match {
			case Nil => (acc, lsLine)
			case None :: rest => parse14_getSiteObjects(iGrid + 1, rest, lsLine.tail, acc)
			case Some(carrier) :: rest =>
				val (n0, l0) = EvowareFormat.splitSemicolons(lsLine(0))
				val (n1, l1) = EvowareFormat.splitSemicolons(lsLine(1))
				assert(n0 == 998 && n1 == 998 && l0(0).toInt == carrier.nSites)
				println(iGrid+": "+carrier)
				val l = (for (iSite <- 0 until carrier.nSites) yield {
					//println("\t"+i+": "+l0(i+1)+", "+l1(i))
					val sName = l0(iSite+1)
					if (sName.isEmpty()) None
					else Some(SiteObject(carrier, iGrid, iSite, sName, l1(iSite)))
				}).toList.flatten
				parse14_getSiteObjects(iGrid + 1, rest, lsLine.drop(2), acc ++ l)
		}
	}
	
	def parse14_getHotelObjects(
		mapIdToGridObject: Map[Int, GridObject],
		lsLine: List[String]
	): Tuple2[List[HotelObject], List[String]] = {
		val (n0, l0) = EvowareFormat.splitSemicolons(lsLine(0))
		assert(n0 == 998)
		val nHotels = l0(0).toInt
		val lHotelObject = lsLine.tail.take(nHotels).map(s => {
			val (n, l) = EvowareFormat.splitSemicolons(s)
			assert(n == 998)
			val id = l(0).toInt
			val iGrid = l(1).toInt
			val parent = mapIdToGridObject(id)
			HotelObject(parent, iGrid)
		})
		(lHotelObject, lsLine.drop(1 + nHotels))
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
