package roboliq.core

import scala.collection.mutable.HashMap

/**
 * Holds information about the location of objects over time.
 * 
 * @param map A map from object ID to a list of tuples of (command node index, location ID).
 */
class LocationTracker(val map: Map[String, List[Tuple2[String, String]]]) {
	/**
	 * Get the location of object with ID `id` at command node index `index`.
	 * 
	 * @see [[roboliq.core.CommandNodeBean]]
	 * 
	 * @param id ID of object (currently just for plates and tubes).
	 * @param index command node index.
	 * @return ID of location if possible.
	 */
	def getLocationForCommand(
		id: String,
		index: String
	): Result[String] = {
		LocationTracker.getLocationForCommand(id, index, map)
	}
}

object LocationTracker {
	/**
	 * Get the location of object with ID `id` at command node index `index`.
	 * 
	 * @see [[roboliq.core.CommandNodeBean]]
	 * 
	 * @param id ID of object (currently just for plates and tubes).
	 * @param index command node index.
	 * @param map A map from object ID to a list of tuples of (command node index, location ID).
	 * @return ID of location if possible.
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
					case None => Error("location not yet set for `"+id+"`: index: "+index+", index set: "+l)
					case Some(pair) => Success(pair._2)
				}
		}
	}
}

/**
 * A builder for [[roboliq.core.LocationTracker]].
 */
class LocationBuilder {
	/** A map from object ID to a list of tuples of (command node index, location ID). */
	val map = new HashMap[String, List[Tuple2[String, String]]]
	
	/**
	 * Add location information for object with ID `id` at command node `index`.
	 * 
	 * @param id ID of object (currently just for plates and tubes).
	 * @param index command node index.
	 * @param location ID of location.
	 */
	def addLocation(id: String, index: String, location: String) {
		map.get(id) match {
			case None =>
				map(id) = List(index -> location)
			case Some(l) =>
				map(id) = l ++ List(index -> location)
		}
		println("addLocation: "+id+" = "+map.get(id))
	}
}