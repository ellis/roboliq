package roboliq.core
import scala.reflect.BeanProperty


/**
 * Enumeration of the physical property classes of liquids as relevant to pipetting
 * (currently just Water or Glycerol).
 */
object LiquidPhysicalProperties extends Enumeration {
	val Water, Glycerol = Value
}

/**
 * Base trait YAML JavaBean for a substance
 * @see [[roboliq.core.Substance]]
 */
sealed trait SubstanceBean extends Bean

/** YAML JavaBean representation of [[roboliq.core.SubstanceDna]]. */
class SubstanceDnaBean extends SubstanceBean {
	@BeanProperty var sequence: String = null
	@BeanProperty var expensive: java.lang.Boolean = null
}

/** YAML JavaBean representation of [[roboliq.core.SubstanceOther]]. */
class SubstanceOtherBean extends SubstanceBean {
	@BeanProperty var expensive: java.lang.Boolean = null
}

/** YAML JavaBean representation of [[roboliq.core.SubstanceLiquid]]. */
class SubstanceLiquidBean extends SubstanceBean {
	@BeanProperty var physical: String = null
	@BeanProperty var cleanPolicy: String = null
	@BeanProperty var expensive: java.lang.Boolean = null
}

/** Represents a substance. */
sealed abstract class Substance {
	/** ID in database. */
	val id: String
	/**
	 * Whether multipipetting is allowed.
	 * Multipipetting is when a tip aspirates once and distributes to that volume to
	 * multiple destinations.  In our case with our Tecan robot, this wastes more
	 * of the source liquid than single-pipetting, so for expensive liquids we
	 * want to prevent multipipetting.
	 */
	val expensive: Boolean
}

object Substance {
	/** Convert [[roboliq.core.SubstanceBean]] to [[roboliq.core.Substance]]. */
	def fromBean(bean: SubstanceBean): Result[Substance] = {
		bean match {
			case b: SubstanceDnaBean => SubstanceDna.fromBean(b)
			case b: SubstanceLiquidBean => SubstanceLiquid.fromBean(b)
			case b: SubstanceOtherBean => SubstanceOther.fromBean(b)
		}
	}
}

/**
 * Represents a DNA-based substance.
 * @param id ID in database.
 * @param sequence_? optional DNA sequence string.
 * @param allowMultipipette Whether multipipetting is allowed (see [[roboliq.core.Substance]]).
 */
case class SubstanceDna(
	val id: String,
	val sequence_? : Option[String],
	val expensive: Boolean
) extends Substance

object SubstanceDna {
	/** Convert [[roboliq.core.SubstanceDnaBean]] to [[roboliq.core.SubstanceDna]]. */
	def fromBean(bean: SubstanceDnaBean): Result[SubstanceDna] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
		} yield {
			val sequence = if (bean.sequence != null) Some(bean.sequence) else None
			val allowMultipipette: Boolean = if (bean.expensive == null) true else bean.expensive
			new SubstanceDna(id, sequence, allowMultipipette)
		}
	}
}

/**
 * Represents the catch-all case of a substance which isn't DNA or a liquid.
 * @param id ID in database.
 * @param allowMultipipette Whether multipipetting is allowed (see [[roboliq.core.Substance]]).
 */
case class SubstanceOther(
	val id: String,
	val expensive: Boolean
) extends Substance

object SubstanceOther {
	/** Convert [[roboliq.core.SubstanceOtherBean]] to [[roboliq.core.SubstanceOther]]. */
	def fromBean(bean: SubstanceOtherBean): Result[SubstanceOther] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
		} yield {
			val allowMultipipette: Boolean = if (bean.expensive == null) true else bean.expensive
			new SubstanceOther(id, allowMultipipette)
		}
	}
}

/**
 * Represents a DNA-based substance.
 * @param id ID in database.
 * @param physicalProperties physical properties of the liquid (e.g. water or glycerol).
 * @param cleanPolicy tip cleaning policy when handling this liquid.
 * @param allowMultipipette Whether multipipetting is allowed (see [[roboliq.core.Substance]]).
 */
case class SubstanceLiquid(
	val id: String,
	val physicalProperties: LiquidPhysicalProperties.Value, 
	val cleanPolicy: GroupCleanPolicy,
	val expensive: Boolean
) extends Substance

object SubstanceLiquid {
	/** Convert [[roboliq.core.SubstanceLiquidBean]] to [[roboliq.core.SubstanceLiquid]]. */
	def fromBean(bean: SubstanceLiquidBean): Result[SubstanceLiquid] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
			id <- Result.mustBeSet(bean._id, "_id")
			id <- Result.mustBeSet(bean._id, "_id")
		} yield {
			val physicalProperties = {
				if (bean.physical == null) LiquidPhysicalProperties.Water
				else LiquidPhysicalProperties.withName(bean.physical)
			}
			val cleanPolicy = {
				if (bean.cleanPolicy == null) GroupCleanPolicy.Thorough
				else {
					bean.cleanPolicy match {
						case "T" => GroupCleanPolicy.Thorough
						case "D" => GroupCleanPolicy.Decontaminate
						case "TNL" => GroupCleanPolicy.TNL
						case _ => return Error("unrecognized `cleanPolicy` value \""+bean.cleanPolicy+"\"")
					}
				}
			}
			val allowMultipipette: Boolean = {
				if (bean.expensive == null) true
				else bean.expensive
			}

			new SubstanceLiquid(id, physicalProperties, cleanPolicy, allowMultipipette)
		}
	}
}
