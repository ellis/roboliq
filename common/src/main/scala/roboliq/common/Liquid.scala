package roboliq.common

object Contaminant extends Enumeration {
	val Cell, DNA, DSMO, Decon, Other = Value
}

/*
A = none?, light*, thorough?, decon (Same Group, different well)
B = thorough*, decon (different group)
C = thorough*, decon (before next group)

NoneThorough?
NoneDecon?
LightThorough*
LightDecon
-Thorough (because this would be the behavior for non-grouped liquids)
ThoroughDecon?
Decon


Decon before entering from another group (boolean) (default is thorough)
Decon before entering the next group (boolean) (default is thorough)
degree when entering from another well of the same group

Thorough-None-Thorough
Thorough-Light-Thorough*
-Thorough-Thorough-Thorough
-Thorough-Decon-Thorough
Decon-*-Thorough?
Decon-None-Decon?
Decon-Light-Decon?
Decon-Thorough-Decon?
Decon-Decon-Decon


*/

sealed class GroupCleanPolicy(
	val enter: WashIntensity.Value,
	val within: WashIntensity.Value,
	val exit: WashIntensity.Value
)
object GroupCleanPolicy {
	val TNT = new GroupCleanPolicy(WashIntensity.Thorough, WashIntensity.None, WashIntensity.Thorough)
	val DDD = new GroupCleanPolicy(WashIntensity.Decontaminate, WashIntensity.Decontaminate, WashIntensity.Decontaminate)
}

class LiquidGroup(
	//val sGroupId: String,
	val cleanPolicy: GroupCleanPolicy = GroupCleanPolicy.TNT
)

class Liquid(
	var sName: String,
	val sFamily: String,
	val contaminants: Set[Contaminant.Value],
	val group: LiquidGroup
	//val family: LiquidPropertiesFamily,
	//val bFreeDispense: Boolean,
	//val washIntensityBeforeAspirate: WashIntensity.Value,
	//val bReplaceTipsBeforeAspirate: Boolean,
	/** Contaminants in this liquid */
	///** Contaminants which must be cleaned from tips before entering this liquid */
	//val prohibitedTipContaminants: Set[Contaminant.Value]
) {
	//val group = group0_?.getOrElse(new LiquidGroup())
	//val sGroupId = sGroupId0_?.getOrElse(this.hashCode().toString)
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
			val sName2 = sName+":"+other.sName
			val group2 = {
				if (group.eq(other.group)) {
					group
				}
				else {
					new LiquidGroup(group.cleanPolicy)
				}
			}
			new Liquid(
				sName2,
				sFamily,
				contaminants ++ other.contaminants,
				group2
			)
		}
	}
	
	def setGroup(group: LiquidGroup): Liquid = {
		new Liquid(sName, sFamily, contaminants, group)
	}
	
	def getName() = if (sName == null) "<unnamed>" else sName
}

object Liquid {
	val empty = new Liquid("", "", Set(), new LiquidGroup())
}
