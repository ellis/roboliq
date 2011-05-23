package robot

import scala.collection.mutable


// A Site can be a very general concept
// Sites can stack on top of each other
// Sites should have a function to say whether they accept a particular other site
// Some site attributes should be obtained from parents, such as whether the site is cooled
// Site should say whether it can contain liquid, and if so how much
// Carriers are sites, plates are sites, tubtes are sites, wells are sites, covers are sites, plate carrying arms are sites, etc...
class Site {
	var sName: String = null
	// Kind of site
	var kind: Int = -1
	var parent_? : Option[Site] = None
	val children = new mutable.ArrayBuffer[Site]
	val accepts = new mutable.ArrayBuffer[Int]

	/// Can this site be reached by the pipetting arm?
	var bPipettable_? : Option[Boolean] = None
	var bCooled_? : Option[Boolean] = None
	var nLiquidVolume_? : Option[Double] = None
	
	def isCooled: Boolean = bCooled_? match {
		case Some(b) => b
		case None => parent_? match {
			case Some(parent) => parent.isCooled
			case None => false
		}
	}
	
	def acceptsLiquid: Boolean = nLiquidVolume_?.isDefined
}

class PlateSite {
	
}

object Site {
	val TubeKind = 0
	
	def tube(sName: String, nVolume: Double): Site = {
		val site = new Site()
		site.sName = sName
		site.nLiquidVolume_? = Some(nVolume)
		site
	}
	def plate(sName: String, nRows: Int, nCols: Int, nVolume: Double): Site = {
		val site = new Site()
		site.sName = sName
		site.nLiquidVolume_? = Some(nVolume)
		site
	}
}

// The robot needs a function to say whether it can pipette from a particular site

// Labware types: tube, plate, cover
// Subtypes: there are various characteristics for tubes, plates, and covers which should be referenced here and defined elsewhere
//
