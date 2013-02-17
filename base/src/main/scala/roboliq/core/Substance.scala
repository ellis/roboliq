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
	@BeanProperty var costPerUnit: java.math.BigDecimal = null
}

/** YAML JavaBean representation of [[roboliq.core.SubstanceOther]]. */
class SubstanceOtherBean extends SubstanceBean {
	@BeanProperty var costPerUnit: java.math.BigDecimal = null
}

/** YAML JavaBean representation of [[roboliq.core.SubstanceLiquid]]. */
class SubstanceLiquidBean extends SubstanceBean {
	@BeanProperty var physical: String = null
	@BeanProperty var cleanPolicy: String = null
	@BeanProperty var costPerUnit: java.math.BigDecimal = null
}

/** Represents a substance. */
sealed abstract class Substance {
	/** ID in database. */
	val id: String
	/** Cost per unit (either liter or mol) of the substance */
	val costPerUnit_? : Option[BigDecimal]
	/**
	 * Whether multipipetting is allowed.
	 * Multipipetting is when a tip aspirates once and distributes to that volume to
	 * multiple destinations.  In our case with our Tecan robot, this wastes more
	 * of the source liquid than single-pipetting, so for expensive liquids we
	 * want to prevent multipipetting.
	 */
	def expensive: Boolean = costPerUnit_?.filter(_ > 0).isDefined
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
 * A solid substance, in contrast to [[roboliq.core.SubstanceLiquid]].
 */
abstract class SubstanceSolid extends Substance

/**
 * Represents a DNA-based substance.
 * @param id ID in database.
 * @param sequence_? optional DNA sequence string.
 * @param allowMultipipette Whether multipipetting is allowed (see [[roboliq.core.Substance]]).
 */
case class SubstanceDna(
	val id: String,
	val sequence_? : Option[String],
	val costPerUnit_? : Option[BigDecimal]
) extends SubstanceSolid

object SubstanceDna {
	/** Convert [[roboliq.core.SubstanceDnaBean]] to [[roboliq.core.SubstanceDna]]. */
	def fromBean(bean: SubstanceDnaBean): Result[SubstanceDna] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
		} yield {
			val sequence = if (bean.sequence != null) Some(bean.sequence) else None
			val costPerUnit_? : Option[BigDecimal] = if (bean.costPerUnit == null) None else Some(bean.costPerUnit)
			new SubstanceDna(id, sequence, costPerUnit_?)
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
	val costPerUnit_? : Option[BigDecimal]
) extends SubstanceSolid

object SubstanceOther {
	/** Convert [[roboliq.core.SubstanceOtherBean]] to [[roboliq.core.SubstanceOther]]. */
	def fromBean(bean: SubstanceOtherBean): Result[SubstanceOther] = {
		for {
			id <- Result.mustBeSet(bean._id, "_id")
		} yield {
			val costPerUnit_? : Option[BigDecimal] = if (bean.costPerUnit == null) None else Some(bean.costPerUnit)
			new SubstanceOther(id, costPerUnit_?)
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
	val cleanPolicy: TipCleanPolicy,
	val costPerUnit_? : Option[BigDecimal]
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
				if (bean.cleanPolicy == null) TipCleanPolicy.Thorough
				else {
					bean.cleanPolicy match {
						case "T" => TipCleanPolicy.Thorough
						case "D" => TipCleanPolicy.Decontaminate
						case "TNL" => TipCleanPolicy.TL
						case _ => return Error("unrecognized `cleanPolicy` value \""+bean.cleanPolicy+"\"")
					}
				}
			}
			val costPerUnit_? : Option[BigDecimal] = if (bean.costPerUnit == null) None else Some(bean.costPerUnit)

			new SubstanceLiquid(id, physicalProperties, cleanPolicy, costPerUnit_?)
		}
	}
}
