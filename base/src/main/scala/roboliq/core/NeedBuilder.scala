package roboliq.core

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ArrayBuffer


/**
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
case class NeedSrc(id: String) extends NeedResource
case class NeedDest(id: String) extends NeedResource
/**
 * Need a new pool (i.e., a new set of wells)
 * @param idInternal an internal id for command use
 * @param purpose a string identifying the purpose for which the
 * pool will be used
 * @param count the number of wells required
 */
case class NeedPool(idInternal: String, purpose: String, count: Int) extends NeedResource

class NeedBuilder(val ob: ObjBase) {
	
	def needSrc(id: String) {
		
	}
	
	def needDest(id: String) {
		
	}
	
	def need(id: String) {
		
	}
}