package roboliq.labs.bsse.examples

//import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap

import roboliq.common
import roboliq.protocol._
import roboliq.protocol.commands._


object ExampleRunner {
	
	def run(lItem: List[Item], db: ItemDatabase): Tuple2[common.KnowledgeBase, List[common.Command]] = {
		//val db = new TestDatabase
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
		mapLocFree += "D-BSSE 96 Well DWP" -> List("cover", "cooled4", "cooled5")
		val mapRack = new HashMap[String, List[Int]]
		mapRack += "Tube 50ml" -> (0 until 8).toList
		val lPlateToLocation: List[Tuple2[Plate, String]] = db.lPlate.flatMap(plate => {
			val plateObj = vom.mapKeyToPlateObj(plate.key)
			val plateSetup = vom.kb.getPlateSetup(plateObj)
			plate.model.getValue match {
				case None => None
				case Some(sModel) =>
					val location_? = mapLocFree.get(sModel) match {
						case None =>
							mapRack.get(sModel) match {
								case None => None
								case Some(li) =>
									mapRack(sModel) = li.tail
									Some(li.head.toString)
							}
						case Some(Nil) => None
						case Some(ls) =>
							mapLocFree(sModel) = ls.tail
							Some(ls.head)
					}
					location_? match {
						case None => None
						case Some(location) =>
							plateSetup.location_? = Some(location)
							Some(plate -> location)
					}
			}
		})
		println("lPlateToLocation:")
		lPlateToLocation.foreach(println)
		roboliq.utils.FileUtils.printToFile(new java.io.File("locations.txt")) { p =>
			lPlateToLocation.foreach(p.println)
		}
		
		(vom.kb, cmds)
	}
}