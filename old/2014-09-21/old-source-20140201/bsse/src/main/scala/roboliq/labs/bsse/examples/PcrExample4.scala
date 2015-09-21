/*package roboliq.labs.bsse.examples

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common
import roboliq.protocol._
import roboliq.protocol.commands._


class PcrExample4 {
	import LiquidAmountImplicits._

	val l = List(
		new Pcr {
			products := List(
				// NOT1
				new Product { template := refDb("FRP446"); forwardPrimer := refDb("FRO115"); backwardPrimer := refDb("FRO700") },
				new Product { template := refDb("FRP332"); forwardPrimer := refDb("FRO699"); backwardPrimer := refDb("FRO114") },
				// NOT2
				new Product { template := refDb("FRP337"); forwardPrimer := refDb("FRO115"); backwardPrimer := refDb("FRO704") },
				new Product { template := refDb("FRP222"); forwardPrimer := refDb("FRO703"); backwardPrimer := refDb("FRO114") },
				// NOR3_yellow
				new Product { template := refDb("FRP572"); forwardPrimer := refDb("FRO115"); backwardPrimer := refDb("FRO1260") },
				new Product { template := refDb("FRP128"); forwardPrimer := refDb("FRO1259"); backwardPrimer := refDb("FRO1262") },
				new Product { template := refDb("FRP572"); forwardPrimer := refDb("FRO1261"); backwardPrimer := refDb("FRO114") }
			)
			volumes := (30 ul)
			mixSpec := new PcrMixSpec {
				waterLiquid := refDb("water")

				buffer.liquid := refDb("buffer5x")
				buffer.amt0 := (5 x)
				buffer.amt1 := (1 x)
				
				dntp.liquid := refDb("dntp")
				dntp.amt0 := (2 mM)
				dntp.amt1 := (0.2 mM)
				
				template.amt0 := (1 x) // FIXME: dummy value
				template.amt1 := (3 ul)
				
				forwardPrimer.amt0 := (10 uM)
				forwardPrimer.amt1 := (0.5 uM)
				
				backwardPrimer.amt0 := (10 uM)
				backwardPrimer.amt1 := (0.5 uM)
				
				polymerase.liquid := refDb("phus-diluted")
				polymerase.amt0 := (10 x)
				polymerase.amt1 := (1 x)
			}
		}
	)
}

//object ExampleRunner {
object PcrExample4Database {
	import LiquidAmountImplicits._
	
	val lLiquid = List[Liquid](
		Liquid(key = "water", cleanPolicy = "TNL"),
		Liquid(key = "buffer10x", cleanPolicy = ("TNT")),
		Liquid(key = "buffer5x", cleanPolicy = ("TNT")),
		Liquid(key = "dntp", cleanPolicy = ("TNT")),
		Liquid(key = "polymerase", physical = ("Glycerol"), cleanPolicy = ("TNT"), multipipetteThreshold = 1000.ul),
		Liquid(key = "phus-diluted", cleanPolicy = ("TNT"), multipipetteThreshold = 1000.ul),
		Liquid(key = "FRO114", cleanPolicy = ("DDD"), contaminants = ("DNA"), multipipetteThreshold = 1000.ul),
		Liquid(key = "FRO115", cleanPolicy = ("DDD"), contaminants = ("DNA"), multipipetteThreshold = 1000.ul),
		Liquid(key = "FRO699", cleanPolicy = ("DDD"), contaminants = ("DNA"), multipipetteThreshold = 1000.ul),
		Liquid(key = "FRO700", cleanPolicy = ("DDD"), contaminants = ("DNA"), multipipetteThreshold = 1000.ul),
		Liquid(key = "FRO703", cleanPolicy = ("DDD"), contaminants = ("DNA"), multipipetteThreshold = 1000.ul),
		Liquid(key = "FRO704", cleanPolicy = ("DDD"), contaminants = ("DNA"), multipipetteThreshold = 1000.ul),
		Liquid(key = "FRO1259", cleanPolicy = ("DDD"), contaminants = ("DNA"), multipipetteThreshold = 1000.ul),
		Liquid(key = "FRO1260", cleanPolicy = ("DDD"), contaminants = ("DNA"), multipipetteThreshold = 1000.ul),
		Liquid(key = "FRO1261", cleanPolicy = ("DDD"), contaminants = ("DNA"), multipipetteThreshold = 1000.ul),
		Liquid(key = "FRO1262", cleanPolicy = ("DDD"), contaminants = ("DNA"), multipipetteThreshold = 1000.ul),
		Liquid(key = "FRP128", cleanPolicy = ("DDD"), contaminants = ("DNA"), multipipetteThreshold = 1000.ul),
		Liquid(key = "FRP222", cleanPolicy = ("DDD"), contaminants = ("DNA"), multipipetteThreshold = 1000.ul),
		Liquid(key = "FRP332", cleanPolicy = ("DDD"), contaminants = ("DNA"), multipipetteThreshold = 1000.ul),
		Liquid(key = "FRP337", cleanPolicy = ("DDD"), contaminants = ("DNA"), multipipetteThreshold = 1000.ul),
		Liquid(key = "FRP446", cleanPolicy = ("DDD"), contaminants = ("DNA"), multipipetteThreshold = 1000.ul),
		Liquid(key = "FRP572", cleanPolicy = ("DDD"), contaminants = ("DNA"), multipipetteThreshold = 1000.ul)
	)
	val lPlateModel = List[PlateModel](
		new PlateModel { key = "D-BSSE 96 Well PCR Plate"; rows := 8; cols := 12 },
		new PlateModel { key = "D-BSSE 96 Well DWP"; rows := 8; cols := 12 },
		new PlateModel { key = "Tube 50ml"; rows := 1; cols := 1 }
	)
	val lPlate0 = List[Plate](
		//new Plate { key = "T50_water"; model := ("Tube 50ml"); description := ("water") },
		new Plate { key = "P1"; model := ("D-BSSE 96 Well PCR Plate"); description := ("templates, primers, polymerase") },
		new Plate { key = "P2"; model := ("D-BSSE 96 Well DWP"); description := ("buffer, dntp, water") },
		new Plate { key = "P4"; model := ("D-BSSE 96 Well PCR Plate"); description := ("PCR products"); purpose := ("PCR") }
	)
	private implicit def valToTempVal[A](a: A): TempValue[A] = Temp1(a)
	val lWell = List[Well](
		Well(parent = TempKey("P1"), index = Temp1(0), liquid = TempKey("FRP128"), volume = LiquidVolume.ul(100)),
		Well(parent = TempKey("P1"), index = Temp1(1), liquid = TempKey("FRP222"), volume = LiquidVolume.ul(100)),
		Well(parent = TempKey("P1"), index = Temp1(2), liquid = TempKey("FRP332"), volume = LiquidVolume.ul(100)),
		Well(parent = TempKey("P1"), index = Temp1(3), liquid = TempKey("FRP337"), volume = LiquidVolume.ul(100)),
		Well(parent = TempKey("P1"), index = Temp1(4), liquid = TempKey("FRP446"), volume = LiquidVolume.ul(100)),
		Well(parent = TempKey("P1"), index = Temp1(5), liquid = TempKey("FRP572"), volume = LiquidVolume.ul(100)),
		
		Well(parent = TempKey("P1"), index = Temp1(7), liquid = TempKey("phus-diluted")),
		
		Well(parent = TempKey("P1"), index = Temp1(16), liquid = TempKey("FRO114"), volume = LiquidVolume.ul(100)),
		Well(parent = TempKey("P1"), index = Temp1(17), liquid = TempKey("FRO115"), volume = LiquidVolume.ul(100)),
		Well(parent = TempKey("P1"), index = Temp1(18), liquid = TempKey("FRO699"), volume = LiquidVolume.ul(100)),
		Well(parent = TempKey("P1"), index = Temp1(19), liquid = TempKey("FRO700"), volume = LiquidVolume.ul(100)),
		Well(parent = TempKey("P1"), index = Temp1(20), liquid = TempKey("FRO703"), volume = LiquidVolume.ul(100)),
		Well(parent = TempKey("P1"), index = Temp1(21), liquid = TempKey("FRO704"), volume = LiquidVolume.ul(100)),
		
		Well(parent = TempKey("P1"), index = Temp1(24), liquid = TempKey("FRO1259"), volume = LiquidVolume.ul(100)),
		Well(parent = TempKey("P1"), index = Temp1(25), liquid = TempKey("FRO1260"), volume = LiquidVolume.ul(100)),
		Well(parent = TempKey("P1"), index = Temp1(26), liquid = TempKey("FRO1261"), volume = LiquidVolume.ul(100)),
		Well(parent = TempKey("P1"), index = Temp1(27), liquid = TempKey("FRO1262"), volume = LiquidVolume.ul(100)),
		Well(parent = TempKey("P1"), index = Temp1(28), liquid = TempKey("FRO1329"), volume = LiquidVolume.ul(100)),

		Well(parent = TempKey("P2"), index = Temp1(0), liquid = TempKey("dntp"), volume = LiquidVolume.ul(500)),
		Well(parent = TempKey("P2"), index = Temp1(1), liquid = TempKey("buffer5x"), volume = LiquidVolume.ul(500)),
		Well(parent = TempKey("P2"), index = Temp1(7), liquid = TempKey("water"), volume = LiquidVolume.ul(500)),

		Well(parent = TempKey("P4"), index = Temp1(0)),
		Well(parent = TempKey("P4"), index = Temp1(1)),
		Well(parent = TempKey("P4"), index = Temp1(2)),
		Well(parent = TempKey("P4"), index = Temp1(3)),
		Well(parent = TempKey("P4"), index = Temp1(4)),
		Well(parent = TempKey("P4"), index = Temp1(5)),
		Well(parent = TempKey("P4"), index = Temp1(6)),
		Well(parent = TempKey("P4"), index = Temp1(7))
	)
	val mapTables = Map[String, Map[String, Item]](
		"Liquid" -> lLiquid.map(liquid => liquid.key -> liquid).toMap,
		"PlateModel" -> lPlateModel.map(o => o.key -> o).toMap,
		"Plate" -> lPlate0.map(o => o.key -> o).toMap,
		"Well" -> lWell.map(o => o.key -> o).toMap
	)
	val mapClassToTable = Map[String, String](
		classOf[Liquid].getCanonicalName() -> "Liquid"
	)
	
	class TestDatabase extends ItemDatabase {
		val lPlate = lPlate0
		
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
			lWell.filter(_.liquid.getValueKey.filter(_.key equals sLiquidKey).isDefined)
		}
		
		def findWellsByPlateKey(sPlateKey: String): List[Well] = {
			lWell.filter(_.parent.getValueKey.filter(_.key equals sPlateKey).isDefined)
		}
		
		def findPlateByPurpose(sPurpose: String): List[Plate] = {
			val s = (sPurpose)
			lPlate.filter(_.purpose.getValue.filter(_ equals s).isDefined)
		}
	}
	
	val db = new TestDatabase
}

/*
object Main extends App {
	example4
	ExampleRunner.run
}
*/
*/