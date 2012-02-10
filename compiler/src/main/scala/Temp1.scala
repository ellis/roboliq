package temp

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.protocol._
import roboliq.protocol.commands._


class Test1 {
	class IntToVolumeWrapper(n: Int) {
		def pl: PLiquidVolume = new PLiquidVolume(LiquidVolume.pl(n))
		def ul: PLiquidVolume = new PLiquidVolume(LiquidVolume.ul(n))
		def ml: PLiquidVolume = new PLiquidVolume(LiquidVolume.ml(n))
	}
	
	implicit def intToVolumeWrapper(n: Int): IntToVolumeWrapper = new IntToVolumeWrapper(n)

	val l = List(
		new Pcr {
			products := List(
				new Product { template := refDb("FRP572"); forwardPrimer := refDb("FRO115"); backwardPrimer := refDb("FRO1260") },
				new Product { template := refDb("FRP128"); forwardPrimer := refDb("FRO1259"); backwardPrimer := refDb("FRO1262") },
				new Product { template := refDb("FRP572"); forwardPrimer := refDb("FRO1261"); backwardPrimer := refDb("FRO114") }
			)
			volumes := new PLiquidVolume(LiquidVolume.ul(20))
			mixSpec := new PcrMixSpec {
				waterLiquid := refDb("water")

				buffer.liquid := refDb("buffer5x")
				buffer.amt0 := new PLiquidAmount(LiquidAmountByConc(10))
				buffer.amt1 := new PLiquidAmount(LiquidAmountByConc(1))
				
				dntp.liquid := refDb("dntp")
				dntp.amt0 := new PLiquidAmount(LiquidAmountByConc(2))
				dntp.amt1 := new PLiquidAmount(LiquidAmountByConc(0.2))
				
				polymerase.liquid := refDb("polymerase")
				polymerase.amt0 := new PLiquidAmount(LiquidAmountByConc(5))
				polymerase.amt1 := new PLiquidAmount(LiquidAmountByConc(0.01))
			}
		}
	)
}

/*
object Parsers {
	import scala.util.parsing.combinator._
	class Parser extends JavaTokenParsers {
		def propertyAndValue = ident ~ "=" ~ stringLiteral ^^ { case s ~ _ ~ v => (s, v) }
		def liquidContents = repsep(propertyAndValue, ",")
	}
}
*/

object T {
	val lLiquid = List[Liquid](
		new Liquid { key = "water"; cleanPolicy := new PString("TNL") },
		new Liquid { key = "buffer10x"; cleanPolicy := new PString("TNT") },
		new Liquid { key = "buffer5x"; cleanPolicy := new PString("TNT") },
		new Liquid { key = "dntp"; cleanPolicy := new PString("TNT") },
		new Liquid { key = "polymerase"; physical := new PString("Glycerol"); cleanPolicy := new PString("TNT") },
		new Liquid { key = "FRO114"; cleanPolicy := new PString("DDD"); contaminants := new PString("DNA") },
		new Liquid { key = "FRO115"; cleanPolicy := new PString("DDD"); contaminants := new PString("DNA") },
		new Liquid { key = "FRO1259"; cleanPolicy := new PString("DDD"); contaminants := new PString("DNA") },
		new Liquid { key = "FRO1260"; cleanPolicy := new PString("DDD"); contaminants := new PString("DNA") },
		new Liquid { key = "FRO1261"; cleanPolicy := new PString("DDD"); contaminants := new PString("DNA") },
		new Liquid { key = "FRO1262"; cleanPolicy := new PString("DDD"); contaminants := new PString("DNA") },
		new Liquid { key = "FRP128"; cleanPolicy := new PString("DDD"); contaminants := new PString("DNA") },
		new Liquid { key = "FRP572"; cleanPolicy := new PString("DDD"); contaminants := new PString("DNA") }
	)
	val lPlate = List[Plate](
		new Plate { key = "T50_water"; model := new PString("Tube 50ml"); description := new PString("water") },
		new Plate { key = "P1"; model := new PString("D-BSSE 96 Well PCR Plate"); description := new PString("templates and primers") },
		new Plate { key = "P2"; model := new PString("D-BSSE 96 Well PCR Plate"); description := new PString("buffer and dntp") },
		new Plate { key = "P3"; model := new PString("D-BSSE 96 Well PCR Plate"); description := new PString("polymerase") },
		new Plate { key = "P4"; model := new PString("D-BSSE 96 Well PCR Plate"); description := new PString("PCR products"); purpose := new PString("PCR") }
	)
	val lWell = List[Well](
		Well(parent = TempKey("T50_water"), liquid = TempKey("water")),
		Well(parent = TempKey("P1"), index = Temp1(new PInteger(0)), liquid = TempKey("FRO114")),
		Well(parent = TempKey("P1"), index = Temp1(new PInteger(1)), liquid = TempKey("FRO115")),
		Well(parent = TempKey("P1"), index = Temp1(new PInteger(2)), liquid = TempKey("FRO1259")),
		Well(parent = TempKey("P1"), index = Temp1(new PInteger(3)), liquid = TempKey("FRO1260")),
		Well(parent = TempKey("P1"), index = Temp1(new PInteger(4)), liquid = TempKey("FRO1261")),
		Well(parent = TempKey("P1"), index = Temp1(new PInteger(5)), liquid = TempKey("FRO1262")),
		Well(parent = TempKey("P1"), index = Temp1(new PInteger(6)), liquid = TempKey("FRP128")),
		Well(parent = TempKey("P1"), index = Temp1(new PInteger(7)), liquid = TempKey("FRP572")),
		Well(parent = TempKey("P2"), index = Temp1(new PInteger(0)), liquid = TempKey("buffer5x")),
		Well(parent = TempKey("P2"), index = Temp1(new PInteger(1)), liquid = TempKey("dntp")),
		Well(parent = TempKey("P3"), index = Temp1(new PInteger(0)), liquid = TempKey("polymerase"))
	)
	val mapTables = Map[String, Map[String, Item]](
		"Liquid" -> lLiquid.map(liquid => liquid.key -> liquid).toMap,
		"Plate" -> lPlate.map(o => o.key -> o).toMap,
		"Well" -> lWell.map(o => o.key -> o).toMap
	)
	val mapClassToTable = Map[String, String](
		classOf[Liquid].getCanonicalName() -> "Liquid"
	)
	
	class TestDatabase extends ItemDatabase {
		def lookupItem(pair: Tuple2[String, String]): Option[Item] = {
			val sTable = mapClassToTable.getOrElse(pair._1, pair._1)
			for {
				table <- mapTables.get(sTable)
				obj <- table.get(pair._2)
			} yield obj
		}
		
		def lookup[A](pair: Tuple2[String, String]): Option[A] = {
			lookupItem(pair).map(_.asInstanceOf[A])
		}
		
		def findWellsByLiquid(sLiquidKey: String): List[Well] = {
			lWell.filter(_.liquid.values.exists(_.keyEquals(sLiquidKey)))
		}
		
		def findWellsByPlateKey(sPlateKey: String): List[Well] = {
			lWell.filter(_.parent.getKey.filter(_ equals sPlateKey).isDefined)
		}
		
		def findPlateByPurpose(sPurpose: String): List[Plate] = {
			val s = new PString(sPurpose)
			lPlate.filter(_.purpose.valueEquals(s))
		}
	}
	
	def run {
		val test1 = new Test1
		val db = new TestDatabase
		ItemListData(test1.l, db)
	}
}

object Main extends App {
	T.run
}
