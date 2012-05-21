package roboliq.core

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
	val NNN = new GroupCleanPolicy(WashIntensity.None, WashIntensity.None, WashIntensity.None)
	val TNN = new GroupCleanPolicy(WashIntensity.Thorough, WashIntensity.None, WashIntensity.None)
	val TNL = new GroupCleanPolicy(WashIntensity.Thorough, WashIntensity.None, WashIntensity.Light)
	val TNT = new GroupCleanPolicy(WashIntensity.Thorough, WashIntensity.None, WashIntensity.Thorough)
	val DDD = new GroupCleanPolicy(WashIntensity.Decontaminate, WashIntensity.Decontaminate, WashIntensity.Decontaminate)
	val ThoroughNone = new GroupCleanPolicy(WashIntensity.Thorough, WashIntensity.None, WashIntensity.None)
	val Thorough = new GroupCleanPolicy(WashIntensity.Thorough, WashIntensity.None, WashIntensity.Thorough)
	val Decontaminate = new GroupCleanPolicy(WashIntensity.Decontaminate, WashIntensity.Decontaminate, WashIntensity.Decontaminate)
	
	def max(a: GroupCleanPolicy, b: GroupCleanPolicy): GroupCleanPolicy = {
		new GroupCleanPolicy(
			WashIntensity.max(a.enter, b.enter),
			WashIntensity.max(a.within, b.within),
			WashIntensity.max(a.exit, b.exit)
		)
	}
}

class LiquidGroup(
	//val sGroupId: String,
	val cleanPolicy: GroupCleanPolicy = GroupCleanPolicy.TNT
)

class Liquid(
	var id: String,
	val sFamily: String,
	val contaminants: Set[Contaminant.Value],
	val group: LiquidGroup,
	var multipipetteThreshold: Double
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
			assert(id != other.id)
			val sName2 = id+":"+other.id
			val group2 = {
				if (group.eq(other.group)) {
					group
				}
				else {
					new LiquidGroup(GroupCleanPolicy.max(group.cleanPolicy, other.group.cleanPolicy))
				}
			}
			new Liquid(
				sName2,
				sFamily,
				contaminants ++ other.contaminants,
				group2,
				math.min(multipipetteThreshold, other.multipipetteThreshold) 
			)
		}
	}
	
	def setGroup(group: LiquidGroup): Liquid = {
		new Liquid(id, sFamily, contaminants, group, multipipetteThreshold)
	}
	
	def getName() = if (id == null) "<unnamed>" else id
	
	override def toString = getName()
	override def equals(that: Any): Boolean = {
		that match {
			case b: Liquid => id == b.id
			case _ => assert(false); false
		}
	}
	override def hashCode() = id.hashCode()
}

object Liquid {
	val empty = new Liquid("<EMPTY>", "", Set(), new LiquidGroup(), 0.0)
}
