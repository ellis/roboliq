package roboliq.core

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ArrayBuffer


/**
 * Represents a resource required by a command.
 * 
 * Commands might need the following objects:
 * 
 * - well as a source (needs to know initial contents and amount)
 * - well as a destination
 * - plate as a source
 * - liquid as a source
 * - number of new wells required for a particular purpose
 * - device
 */
sealed abstract class NeedResource
/**
 * Represents a source vessel resource.
 * @param id vessel ID.
 */
case class NeedSrc(id: String) extends NeedResource
/**
 * Represents a destination vessel resource.
 * @param id vessel ID.
 */
case class NeedDest(id: String) extends NeedResource
/**
 * Represents a pipetting tip resource.
 * @param id tip ID.
 */
case class NeedTip(id: String) extends NeedResource
/**
 * Represents a new pool (i.e., a new set of wells)
 * @param idInternal an internal id for command use
 * @param purpose a string identifying the purpose for which the
 * pool will be used
 * @param count the number of wells required
 */
case class NeedPool(idInternal: String, purpose: String, count: Int) extends NeedResource
/**
 * Represents a plate resource.
 * @param id plate ID.
 */
case class NeedPlate(id: String) extends NeedResource
/**
 * Represents a location.
 * @param id location ID
 */
//case class NeedLocation(id: String) extends NeedResource
