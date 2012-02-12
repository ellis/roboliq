package roboliq.labs.bsse.examples

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common
import roboliq.protocol._
import roboliq.protocol.commands._


class PcrExample4 {
	val l = List(
		new Pcr {
			products := List(
				new Product { template := refDb("FRP572"); forwardPrimer := refDb("FRO115"); backwardPrimer := refDb("FRO1260") },
				new Product { template := refDb("FRP128"); forwardPrimer := refDb("FRO1259"); backwardPrimer := refDb("FRO1262") },
				new Product { template := refDb("FRP572"); forwardPrimer := refDb("FRO1261"); backwardPrimer := refDb("FRO114") }
			)
			volumes := LiquidVolume.ul(20)
			mixSpec := new PcrMixSpec {
				waterLiquid := refDb("water")

				buffer.liquid := refDb("buffer5x")
				buffer.amt0 := LiquidAmountByConc(5)
				buffer.amt1 := LiquidAmountByConc(1)
				
				dntp.liquid := refDb("dntp")
				dntp.amt0 := LiquidAmountByConc(2000) // nM
				dntp.amt1 := LiquidAmountByConc(200) // nM
				
				template.amt0 := LiquidAmountByConc(1) // FIXME: dummy value
				template.amt1 := LiquidAmountByVolume(LiquidVolume.pl(500))
				
				forwardPrimer.amt0 := LiquidAmountByConc(100000) // nM
				forwardPrimer.amt1 := LiquidAmountByConc(500) // nM
				
				backwardPrimer.amt0 := LiquidAmountByConc(100000) // nM
				backwardPrimer.amt1 := LiquidAmountByConc(500) // nM
				
				polymerase.liquid := refDb("polymerase")
				polymerase.amt0 := LiquidAmountByConc(200)
				polymerase.amt1 := LiquidAmountByConc(1)
			}
		}
	)
}

object ExampleRunner {
	val lLiquid = List[Liquid](
		new Liquid { key = "water"; cleanPolicy := "TNL" },
		new Liquid { key = "buffer10x"; cleanPolicy := ("TNT") },
		new Liquid { key = "buffer5x"; cleanPolicy := ("TNT") },
		new Liquid { key = "dntp"; cleanPolicy := ("TNT") },
		new Liquid { key = "polymerase"; physical := ("Glycerol"); cleanPolicy := ("TNT") },
		new Liquid { key = "FRO114"; cleanPolicy := ("DDD"); contaminants := ("DNA") },
		new Liquid { key = "FRO115"; cleanPolicy := ("DDD"); contaminants := ("DNA") },
		new Liquid { key = "FRO1259"; cleanPolicy := ("DDD"); contaminants := ("DNA") },
		new Liquid { key = "FRO1260"; cleanPolicy := ("DDD"); contaminants := ("DNA") },
		new Liquid { key = "FRO1261"; cleanPolicy := ("DDD"); contaminants := ("DNA") },
		new Liquid { key = "FRO1262"; cleanPolicy := ("DDD"); contaminants := ("DNA") },
		new Liquid { key = "FRP128"; cleanPolicy := ("DDD"); contaminants := ("DNA") },
		new Liquid { key = "FRP572"; cleanPolicy := ("DDD"); contaminants := ("DNA") }
	)
	val lPlateModel = List[PlateModel](
		new PlateModel { key = "D-BSSE 96 Well PCR Plate"; rows := 8; cols := 12 },
		new PlateModel { key = "Tube 50ml"; rows := 1; cols := 1 }
	)
	val lPlate = List[Plate](
		new Plate { key = "T50_water"; model := ("Tube 50ml"); description := ("water") },
		new Plate { key = "P1"; model := ("D-BSSE 96 Well PCR Plate"); description := ("templates and primers") },
		new Plate { key = "P2"; model := ("D-BSSE 96 Well PCR Plate"); description := ("buffer and dntp") },
		new Plate { key = "P3"; model := ("D-BSSE 96 Well PCR Plate"); description := ("polymerase") },
		new Plate { key = "P4"; model := ("D-BSSE 96 Well PCR Plate"); description := ("PCR products"); purpose := ("PCR") }
	)
	val lWell = List[Well](
		Well(parent = TempKey("T50_water"), liquid = TempKey("water")),
		Well(parent = TempKey("P1"), index = Temp1(0), liquid = TempKey("FRO114")),
		Well(parent = TempKey("P1"), index = Temp1(1), liquid = TempKey("FRO115")),
		Well(parent = TempKey("P1"), index = Temp1(2), liquid = TempKey("FRO1259")),
		Well(parent = TempKey("P1"), index = Temp1(3), liquid = TempKey("FRO1260")),
		Well(parent = TempKey("P1"), index = Temp1(4), liquid = TempKey("FRO1261")),
		Well(parent = TempKey("P1"), index = Temp1(5), liquid = TempKey("FRO1262")),
		Well(parent = TempKey("P1"), index = Temp1(6), liquid = TempKey("FRP128")),
		Well(parent = TempKey("P1"), index = Temp1(7), liquid = TempKey("FRP572")),
		Well(parent = TempKey("P2"), index = Temp1(0), liquid = TempKey("buffer5x")),
		Well(parent = TempKey("P2"), index = Temp1(1), liquid = TempKey("dntp")),
		Well(parent = TempKey("P3"), index = Temp1(0), liquid = TempKey("polymerase"))
	)
	val mapTables = Map[String, Map[String, Item]](
		"Liquid" -> lLiquid.map(liquid => liquid.key -> liquid).toMap,
		"PlateModel" -> lPlateModel.map(o => o.key -> o).toMap,
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
	
	def run(lItem: List[Item]): Tuple2[common.KnowledgeBase, List[common.Command]] = {
		val db = new TestDatabase
		val ild = ItemListData(lItem, db)
		val vom = ValueToObjectMap(ild)
		val cmds = lItem.collect({case cmd: PCommand => cmd}).flatMap(_.createCommands(vom))
		//println("cmds:")
		//cmds.foreach(cmd => println(cmd.toDebugString))
		
		// This task is platform specific.  In our case: tecan evoware.
		println()
		println("choosePlateLocations:")
		val mapLocFree = new HashMap[String, List[String]]
		mapLocFree += "D-BSSE 96 Well PCR Plate" -> List("cooled1", "cooled2", "cooled3", "cooled4", "cooled5")
		val mapRack = new HashMap[String, List[Int]]
		mapRack += "Tube 50ml" -> (0 until 8).toList
		lPlate.flatMap(plate => {
			val plateObj = vom.mapKeyToPlateObj(plate.key)
			val plateSetup = vom.kb.getPlateSetup(plateObj)
			plate.model.getValue match {
				case None => None
				case Some(sModel) =>
					mapLocFree.get(sModel) match {
						case None =>
							mapRack.get(sModel) match {
								case None => None
								case Some(li) =>
									mapRack(sModel) = li.tail
									plateSetup.location_? = Some(li.head.toString)
									Some(plate -> li.head.toString)
							}
						case Some(Nil) => None
						case Some(ls) =>
							mapLocFree(sModel) = ls.tail
							plateSetup.location_? = Some(ls.toString)
							Some(plate -> ls.head)
					}
			}
		}).foreach(println)
		
		
		(vom.kb, cmds)
	}
}

/*
object Main extends App {
	example4
	ExampleRunner.run
}
*/