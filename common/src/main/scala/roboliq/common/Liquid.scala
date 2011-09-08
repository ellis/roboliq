package roboliq.common

object Contaminant extends Enumeration {
	val Cell, DNA, DSMO, Decon, Other = Value
	//type ContaminantLevels = Map[Contaminant.Value, ContaminationLevel.Value]
}

//import Contaminant.ContaminantLevels

/*object ContaminationLevel extends Enumeration {
	val None, Minor, Major = Value
}

class ContaminantLevels(val map: Map[Contaminant.Value, ContaminationLevel.Value]) {
	def +(other: ContaminantLevels): ContaminantLevels = {
		val keys = map.keys ++ other.map.keys
		val mapNew = keys.map(key => {
			val v = (map.get(key), other.map.get(key)) match {
				case Tuple2(Some(l1), Some(l2)) => if (l1 >= l2) l1 else l2
				case Tuple2(Some(l1), None) => l1
				case Tuple2(None, Some(l2)) => l2
				case _ => ContaminationLevel.None // This case will never occur, but it's here to avoid a compiler warning
			}
			key -> v
		}).toMap
		new ContaminantLevels(mapNew)
	}
	
	/*def replaceWith(other: ContaminantLevels): ContaminantLevels = {
		new ContaminantLevels(map ++ other.map)
	}*/
}

object ContaminantLevels {
	def apply() = new ContaminantLevels(Map())
}
*/

class Liquid(
	var sName: String,
	val bFreeDispense: Boolean,
	val washIntensityBeforeAspirate: WashIntensity.Value,
	val bReplaceTipsBeforeAspirate: Boolean,
	/** Contaminants in this liquid */
	val contaminants: Set[Contaminant.Value]
	///** Contaminants which must be cleaned from tips before entering this liquid */
	//val prohibitedTipContaminants: Set[Contaminant.Value]
) {
	//def contaminates: Boolean = contaminantLevels.map.exists(_._2 != ContaminationLevel.None)
	
	def +(other: Liquid): Liquid = {
		if (this eq other)
			this
		else if (this eq Liquid.empty)
			other
		else if (other eq Liquid.empty)
			this
		else {
			assert(sName != other.sName)
			val sName3 = sName+":"+other.sName
			new Liquid(
				sName3,
				bFreeDispense & other.bFreeDispense,
				WashIntensity.max(washIntensityBeforeAspirate, other.washIntensityBeforeAspirate),
				bReplaceTipsBeforeAspirate | other.bReplaceTipsBeforeAspirate,
				contaminants ++ other.contaminants
				//contaminantLevels + other.contaminantLevels
				//prohibitedTipContaminants ++ other.prohibitedTipContaminants
			)
		}
	}
	
	def getName() = if (sName == null) "<unnamed>" else sName
}

object Liquid {
	val empty = new Liquid("", false, WashIntensity.None, false, Set())
}
