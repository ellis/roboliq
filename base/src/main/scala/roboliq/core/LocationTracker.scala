package roboliq.core

import scala.collection.mutable.HashMap

class LocationTracker(val map: Map[String, List[Tuple2[String, String]]]) {
	def getLocationForCommand(
		id: String,
		index: String
	): Result[String] = {
		LocationTracker.getLocationForCommand(id, index, map)
	}
}

object LocationTracker {
	/**
	 * @param id ID of plate or tube
	 * @param index command index
	 */
	def getLocationForCommand(
		id: String,
		index: String,
		map: scala.collection.Map[String, List[Tuple2[String, String]]]
	): Result[String] = {
		val lIndex = index.split('.').toList.map(_.toInt)
		map.get(id) match {
			case None => Error("no location set for `"+id+"`")
			case Some(l) =>
				l.find(pair => {
					val lIndex2 = pair._1.split('.').toList.map(_.toInt)
					val n = (lIndex2 zip lIndex).map(pair => pair._1 - pair._2).foldLeft(0) {(acc, n) =>
						if (acc == 0) n else acc }
					n <= 0
				}) match {
					case None => Error("location not yet set for `"+id+"`")
					case Some(pair) => Success(pair._2)
				}
		}
	}
}

class LocationBuilder {
	val map = new HashMap[String, List[Tuple2[String, String]]]
	
	def addLocation(id: String, index: String, location: String) {
		map.get(id) match {
			case None =>
				map(id) = List(index -> location)
			case Some(l) =>
				map(id) = l ::: List(index -> location)
		}
	}
}