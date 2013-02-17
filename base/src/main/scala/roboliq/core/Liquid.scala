package roboliq.core


/**
 * Enumeration of possible contaminants on a tip; this probably won't be used now, so consider deleting it.
 */
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

/**
 * Represents the intensity of tip cleaning required by a given liquid.
 * 
 * @param enter intensity with which tip must have been washed prior to entering a liquid.
 * @param within intensity with which tip must be washed between pipetteing operations performed in the same [[robolq.core.LiquidGroup]].
 * @param exit intensity with which tip must be washed after entering a liquid. 
 */
case class GroupCleanPolicy(
	val enter: CleanIntensity.Value,
	val within: CleanIntensity.Value,
	val exit: CleanIntensity.Value
)
/**
 * Contains the standard GroupCleanPolicy instantiations.
 */
object GroupCleanPolicy {
	val NNN = new GroupCleanPolicy(CleanIntensity.None, CleanIntensity.None, CleanIntensity.None)
	val TNN = new GroupCleanPolicy(CleanIntensity.Thorough, CleanIntensity.None, CleanIntensity.None)
	val TNL = new GroupCleanPolicy(CleanIntensity.Thorough, CleanIntensity.None, CleanIntensity.Light)
	val TNT = new GroupCleanPolicy(CleanIntensity.Thorough, CleanIntensity.None, CleanIntensity.Thorough)
	val DDD = new GroupCleanPolicy(CleanIntensity.Decontaminate, CleanIntensity.Decontaminate, CleanIntensity.Decontaminate)
	val ThoroughNone = new GroupCleanPolicy(CleanIntensity.Thorough, CleanIntensity.None, CleanIntensity.None)
	val Thorough = new GroupCleanPolicy(CleanIntensity.Thorough, CleanIntensity.None, CleanIntensity.Thorough)
	val Decontaminate = new GroupCleanPolicy(CleanIntensity.Decontaminate, CleanIntensity.Decontaminate, CleanIntensity.Decontaminate)
	
	/**
	 * Return a new policy that takes the maximum sub-intensities of `a` and `b`.
	 */
	def max(a: GroupCleanPolicy, b: GroupCleanPolicy): GroupCleanPolicy = {
		new GroupCleanPolicy(
			CleanIntensity.max(a.enter, b.enter),
			CleanIntensity.max(a.within, b.within),
			CleanIntensity.max(a.exit, b.exit)
		)
	}
}

/**
 * A LiquidGroup is a set of liquids for which a special clean policy can be defined when a tip
 * enters two different liquids in the same group.
 * In particular, this can be used to reduce the cleaning intensity when contamination is not
 * a concern. 
 */
class LiquidGroup(
	val cleanPolicy: GroupCleanPolicy = GroupCleanPolicy.TNT
)

/**
 * Represents a liquid.
 * This is one of the most-used classes for our pipetting routines.
 * 
 * @note Liquid should be better integrated with the newer Substance and VesselContent classes.
 * For example, Liquid could maintain a list of Substances and their ratios and concentrations,
 * more similar to VesselContent.
 * 
 * @see [[roboliq.core.Substance]]
 * @see [[roboliq.core.SubstanceLiquid]]
 * @see [[roboliq.core.VesselContent]]
 */
class Liquid(
	var id: String,
	val sName_? : Option[String],
	val sFamily: String,
	val contaminants: Set[Contaminant.Value],
	val group: LiquidGroup,
	var multipipetteThreshold: Double
) {
	def +(other: Liquid): Liquid = {
		if (this eq other)
			this
		else if (this eq Liquid.empty)
			other
		else if (other eq Liquid.empty)
			this
		else {
			assert(id != other.id)
			val id2 = id+":"+other.id
			val group2 = {
				if (group.eq(other.group)) {
					group
				}
				else {
					new LiquidGroup(GroupCleanPolicy.max(group.cleanPolicy, other.group.cleanPolicy))
				}
			}
			new Liquid(
				id2,
				None,
				sFamily,
				contaminants ++ other.contaminants,
				group2,
				math.min(multipipetteThreshold, other.multipipetteThreshold) 
			)
		}
	}
	
	def setGroup(group: LiquidGroup): Liquid = {
		new Liquid(id, sName_?, sFamily, contaminants, group, multipipetteThreshold)
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
	/** Empty liquid */
	val empty = new Liquid("<EMPTY>", None, "", Set(), new LiquidGroup(), 0.0)
}
